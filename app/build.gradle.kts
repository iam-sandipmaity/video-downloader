import org.gradle.api.tasks.Copy
import java.util.Properties
import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import java.util.zip.ZipInputStream

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val signingProps = Properties().apply {
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) {
        propsFile.inputStream().use(::load)
    }
}

fun signingValue(name: String): String? {
    val envValue = System.getenv(name)?.takeIf { it.isNotBlank() }
    if (envValue != null) return envValue
    val gradleValue = project.findProperty(name) as String?
    if (!gradleValue.isNullOrBlank()) return gradleValue
    return signingProps.getProperty(name)?.takeIf { it.isNotBlank() }
}

fun stringProperty(name: String, fallback: String): String {
    return (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() } ?: fallback
}

val generatedChangelogAssetsDir = layout.buildDirectory.dir("generated/changelogAssets")
val syncBundledChangelog by tasks.registering(Copy::class) {
    from(rootProject.file("CHANGELOG.md"))
    into(generatedChangelogAssetsDir.map { it.dir("changelog") })
}
val stableVersionCode = stringProperty("APP_VERSION_CODE", "1").toInt()
val stableVersionName = stringProperty("APP_VERSION_NAME", "0.1.0")
val nightlyVersionCode = stringProperty("NIGHTLY_VERSION_CODE", stableVersionCode.toString()).toInt()
val nightlyVersionName = stringProperty("NIGHTLY_VERSION_NAME", stableVersionName)
val requestedReleaseChannel = stringProperty(
    "APP_RELEASE_CHANNEL",
    if (gradle.startParameter.taskNames.any { it.contains("Nightly", ignoreCase = true) }) {
        "nightly"
    } else {
        "stable"
    },
)
val isNightlyBuild = requestedReleaseChannel.equals("nightly", ignoreCase = true)
val selectedVersionCode = if (isNightlyBuild) nightlyVersionCode else stableVersionCode
val selectedVersionName = if (isNightlyBuild) nightlyVersionName else stableVersionName
val androidxCoreVersion = "1.18.0"

configurations.configureEach {
    resolutionStrategy.force(
        "androidx.core:core:$androidxCoreVersion",
        "androidx.core:core-ktx:$androidxCoreVersion",
    )
}

android {
    namespace = "com.localdownloader"
    compileSdk = 36

    val internalDebugStoreFile = signingValue("INTERNAL_DEBUG_STORE_FILE")
    val internalDebugStorePassword = signingValue("INTERNAL_DEBUG_STORE_PASSWORD")
    val internalDebugKeyAlias = signingValue("INTERNAL_DEBUG_KEY_ALIAS")
    val internalDebugKeyPassword = signingValue("INTERNAL_DEBUG_KEY_PASSWORD")
    val hasInternalDebugSigning = listOf(
        internalDebugStoreFile,
        internalDebugStorePassword,
        internalDebugKeyAlias,
        internalDebugKeyPassword,
    ).all { !it.isNullOrBlank() }

    val releaseStoreFile = signingValue("RELEASE_STORE_FILE")
    val releaseStorePassword = signingValue("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = signingValue("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = signingValue("RELEASE_KEY_PASSWORD")
    val hasReleaseSigning = listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

    defaultConfig {
        applicationId = "com.localdownloader"
        minSdk = 26
        targetSdk = 36
        versionCode = selectedVersionCode
        versionName = selectedVersionName
        buildConfigField("String", "APP_RELEASE_CHANNEL", "\"stable\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("standard") {
            dimension = "distribution"
            buildConfigField("boolean", "YTDLP_AUTO_UPDATE_DEFAULT", "true")
        }
        create("repoSafe") {
            dimension = "distribution"
            buildConfigField("boolean", "YTDLP_AUTO_UPDATE_DEFAULT", "false")
        }
    }

    signingConfigs {
        create("internalDebugStable") {
            if (hasInternalDebugSigning) {
                storeFile = file(requireNotNull(internalDebugStoreFile))
                storePassword = internalDebugStorePassword
                keyAlias = internalDebugKeyAlias
                keyPassword = internalDebugKeyPassword
            }
        }
        create("releaseStable") {
            if (hasReleaseSigning) {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("releaseStable")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            if (hasInternalDebugSigning) {
                signingConfig = signingConfigs.getByName("internalDebugStable")
            }
        }
        create("nightly") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".nightly"
            matchingFallbacks += listOf("debug")
            resValue("string", "app_name", "Nightly - $nightlyVersionName")
            buildConfigField("String", "APP_RELEASE_CHANNEL", "\"nightly\"")
            if (hasInternalDebugSigning) {
                signingConfig = signingConfigs.getByName("internalDebugStable")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    sourceSets.getByName("main").assets.directories.add(
        generatedChangelogAssetsDir.get().asFile.absolutePath,
    )
}

val downloadFfmpegRuntimeTask by tasks.registering {
    val outputDir = file("src/main/jniLibs/arm64-v8a")
    val targetSo = File(outputDir, "libffmpeg.so")
    val targetZipSo = File(outputDir, "libffmpeg.zip.so")

    inputs.property("repo", "iam-sandipmaity/video-downloader-packages")
    outputs.file(targetSo)

    doLast {
        if (targetSo.exists()) {
            println("FFmpeg binaries already exist in jniLibs. Skipping download.")
            return@doLast
        }
        val repo = "iam-sandipmaity/video-downloader-packages"
        val abi = "arm64-v8a"
        println("Fetching latest FFmpeg release from $repo...")
        
        val releasesUrl = URL("https://api.github.com/repos/$repo/releases")
        val releasesConn = releasesUrl.openConnection() as HttpURLConnection
        releasesConn.setRequestProperty("User-Agent", "gradle-build")
        val githubToken = System.getenv("GITHUB_TOKEN") ?: System.getenv("GH_TOKEN")
        if (githubToken != null) {
            releasesConn.setRequestProperty("Authorization", "Bearer $githubToken")
        }
        val responseText = releasesConn.inputStream.bufferedReader().readText()
        
        val regex = Regex("""https://github\.com/[^"]+?ffmpeg-signed-$abi\.apk""")
        val downloadUrl = regex.find(responseText)?.value
            ?: throw GradleException("Could not find signed FFmpeg APK download URL in releases JSON")
            
        println("Downloading FFmpeg APK from $downloadUrl...")
        val tempApk = File(temporaryDir, "ffmpeg-temp.apk")
        URL(downloadUrl).openStream().use { input ->
            tempApk.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        println("Extracting libffmpeg.so and libffmpeg.zip.so...")
        outputDir.mkdirs()
        
        ZipInputStream(tempApk.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "lib/$abi/libffmpeg.so") {
                    targetSo.outputStream().use { zip.copyTo(it) }
                } else if (entry.name == "lib/$abi/libffmpeg.zip.so") {
                    targetZipSo.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        
        if (!targetSo.exists()) {
            throw GradleException("Failed to extract libffmpeg.so from downloaded APK")
        }

        // Set executable permission so Android extracts with correct mode
        targetSo.setExecutable(true, false)
        println("Set executable permission on libffmpeg.so")

        println("Successfully extracted and placed FFmpeg binaries to $outputDir")
    }
}

val downloadPythonRuntimeTask by tasks.registering {
    val outputDir = file("src/main/jniLibs/arm64-v8a")
    val targetPythonSo = File(outputDir, "libpython.so")
    val targetPython311So = File(outputDir, "libpython3.11.so")
    val targetPythonZipSo = File(outputDir, "libpython.zip.so")

    inputs.property("repo", "iam-sandipmaity/video-downloader-packages")
    outputs.file(targetPythonSo)

    doLast {
        if (targetPythonSo.exists()) {
            println("Python runtime binaries already exist in jniLibs. Skipping download.")
            return@doLast
        }
        val repo = "iam-sandipmaity/video-downloader-packages"
        val abi = "arm64-v8a"
        println("Fetching latest Python runtime release from $repo...")
        
        val releasesUrl = URL("https://api.github.com/repos/$repo/releases")
        val releasesConn = releasesUrl.openConnection() as HttpURLConnection
        releasesConn.setRequestProperty("User-Agent", "gradle-build")
        val githubToken = System.getenv("GITHUB_TOKEN") ?: System.getenv("GH_TOKEN")
        if (githubToken != null) {
            releasesConn.setRequestProperty("Authorization", "Bearer $githubToken")
        }
        val responseText = releasesConn.inputStream.bufferedReader().readText()
        
        val regex = Regex("""https://github\.com/[^"]+?python-signed-$abi\.apk""")
        val downloadUrl = regex.find(responseText)?.value
            ?: throw GradleException("Could not find signed Python APK download URL in releases JSON")
            
        println("Downloading Python runtime APK from $downloadUrl...")
        val tempApk = File(temporaryDir, "python-temp.apk")
        URL(downloadUrl).openStream().use { input ->
            tempApk.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        println("Extracting libpython.so, libpython3.11.so, and libpython.zip.so...")
        outputDir.mkdirs()
        
        ZipInputStream(tempApk.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "lib/$abi/libpython.so" -> {
                        targetPythonSo.outputStream().use { zip.copyTo(it) }
                        println("  Extracted libpython.so")
                    }
                    "lib/$abi/libpython3.11.so" -> {
                        targetPython311So.outputStream().use { zip.copyTo(it) }
                        println("  Extracted libpython3.11.so")
                    }
                    "lib/$abi/libpython.zip.so" -> {
                        targetPythonZipSo.outputStream().use { zip.copyTo(it) }
                        println("  Extracted libpython.zip.so")
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        
        if (!targetPythonSo.exists()) {
            throw GradleException("Failed to extract libpython.so from downloaded Python APK")
        }

        // Set executable permission so Android extracts with correct mode
        targetPythonSo.setExecutable(true, false)
        targetPython311So.setExecutable(true, false)
        println("Set executable permission on libpython.so and libpython3.11.so")

        println("Successfully extracted and placed Python runtime binaries to $outputDir")
    }
}

val downloadQuickJsRuntimeTask by tasks.registering {
    val outputDir = file("src/main/jniLibs/arm64-v8a")
    val targetQjsSo = File(outputDir, "libqjs.so")

    inputs.property("repo", "iam-sandipmaity/video-downloader-packages")
    outputs.file(targetQjsSo)

    doLast {
        if (targetQjsSo.exists()) {
            println("QuickJS runtime binary already exists in jniLibs. Skipping download.")
            return@doLast
        }
        val repo = "iam-sandipmaity/video-downloader-packages"
        val abi = "arm64-v8a"
        println("Fetching latest QuickJS release from $repo...")
        
        val releasesUrl = URL("https://api.github.com/repos/$repo/releases")
        val releasesConn = releasesUrl.openConnection() as HttpURLConnection
        releasesConn.setRequestProperty("User-Agent", "gradle-build")
        val githubToken = System.getenv("GITHUB_TOKEN") ?: System.getenv("GH_TOKEN")
        if (githubToken != null) {
            releasesConn.setRequestProperty("Authorization", "Bearer $githubToken")
        }
        val responseText = releasesConn.inputStream.bufferedReader().readText()
        
        val regex = Regex("""https://github\.com/[^"]+?quickjs-signed-$abi\.apk""")
        val downloadUrl = regex.find(responseText)?.value
            ?: throw GradleException("Could not find signed QuickJS APK download URL in releases JSON")
            
        println("Downloading QuickJS APK from $downloadUrl...")
        val tempApk = File(temporaryDir, "quickjs-temp.apk")
        URL(downloadUrl).openStream().use { input ->
            tempApk.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        println("Extracting libqjs.so...")
        outputDir.mkdirs()
        
        ZipInputStream(tempApk.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "lib/$abi/libqjs.so") {
                    targetQjsSo.outputStream().use { zip.copyTo(it) }
                    println("  Extracted libqjs.so")
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        
        if (!targetQjsSo.exists()) {
            throw GradleException("Failed to extract libqjs.so from downloaded QuickJS APK")
        }

        // Set executable permission so Android extracts with correct mode
        targetQjsSo.setExecutable(true, false)
        println("Set executable permission on libqjs.so")

        println("Successfully extracted and placed QuickJS binary to $outputDir")
    }
}

val downloadYtDlpAssetTask by tasks.registering {
    val outputDir = file("src/main/assets/yt-dlp")
    val targetYtDlp = File(outputDir, "yt-dlp")

    outputs.file(targetYtDlp)

    doLast {
        if (targetYtDlp.exists()) {
            println("yt-dlp asset already exists. Skipping download.")
            return@doLast
        }
        println("Downloading latest yt-dlp from yt-dlp/yt-dlp releases...")
        
        val jsonUrl = URL("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest")
        val connection = jsonUrl.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "gradle-build")
        val githubToken = System.getenv("GITHUB_TOKEN") ?: System.getenv("GH_TOKEN")
        if (githubToken != null) {
            connection.setRequestProperty("Authorization", "Bearer $githubToken")
        }
        val responseText = connection.inputStream.bufferedReader().readText()
        
        val downloadUrl = Regex("\"browser_download_url\"\\s*:\\s*\"([^\"]+/yt-dlp)\"(?!\\.)")
            .find(responseText)?.groupValues?.get(1)
            ?: throw GradleException("Could not find yt-dlp download URL in latest release JSON")
            
        println("Downloading yt-dlp from $downloadUrl...")
        outputDir.mkdirs()
        
        URL(downloadUrl).openStream().use { input ->
            targetYtDlp.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        if (!targetYtDlp.exists()) {
            throw GradleException("Failed to download yt-dlp")
        }
        println("Successfully downloaded yt-dlp (${targetYtDlp.length()} bytes) to $outputDir")
    }
}

tasks.named("preBuild") {
    dependsOn(syncBundledChangelog)
    dependsOn(downloadFfmpegRuntimeTask)
    dependsOn(downloadPythonRuntimeTask)
    dependsOn(downloadQuickJsRuntimeTask)
    dependsOn(downloadYtDlpAssetTask)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.05.00")

    implementation("androidx.core:core-ktx:$androidxCoreVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.media:media:1.8.0")
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-ui:1.10.1")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
    implementation("org.tukaani:xz:1.12")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.hilt:hilt-work:1.3.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    ksp("androidx.hilt:hilt-compiler:1.3.0")

    implementation("androidx.datastore:datastore-preferences:1.2.1")

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.3.21")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
