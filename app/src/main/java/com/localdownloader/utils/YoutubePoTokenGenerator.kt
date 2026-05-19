package com.localdownloader.utils

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object YoutubePoTokenGenerator {
    const val SAMPLE_VIDEO_ID = "aqz-KE-bpKQ"
    const val SAMPLE_VIDEO_URL = "https://www.youtube.com/watch?v=$SAMPLE_VIDEO_ID"

    data class GeneratedTokens(
        val visitorData: String,
        val dataSyncId: String,
        val gvsToken: String,
        val playerToken: String,
        val subsToken: String,
    )

    suspend fun generate(
        context: Context,
        visitorData: String,
        dataSyncId: String,
        videoId: String = SAMPLE_VIDEO_ID,
    ): GeneratedTokens {
        val generator = BotGuardWebView.initialize(context.applicationContext)
        return try {
            val gvsToken = generator.generatePoToken(visitorData)
            val playerToken = generator.generatePoToken(videoId)
            GeneratedTokens(
                visitorData = visitorData,
                dataSyncId = dataSyncId,
                gvsToken = gvsToken,
                playerToken = playerToken,
                subsToken = playerToken,
            )
        } finally {
            generator.close()
        }
    }

    private class BotGuardWebView private constructor(
        private val context: Context,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val mainHandler = Handler(Looper.getMainLooper())
        private val webView = WebView(context)
        private val poTokenContinuations = linkedMapOf<String, kotlin.coroutines.Continuation<String>>()
        private var initializationContinuation: kotlin.coroutines.Continuation<BotGuardWebView>? = null

        init {
            webView.settings.apply {
                javaScriptEnabled = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = false
                }
                userAgentString = USER_AGENT
                blockNetworkLoads = true
            }
            webView.addJavascriptInterface(this, JS_INTERFACE)
        }

        suspend fun generatePoToken(identifier: String): String {
            return suspendCancellableCoroutine { continuation ->
                poTokenContinuations[identifier] = continuation
                continuation.invokeOnCancellation {
                    poTokenContinuations.remove(identifier)
                }
                mainHandler.post {
                    webView.evaluateJavascript(
                        """
                        try {
                            identifier = ${Json.encodeToString(identifier)}
                            u8Identifier = ${YoutubePoTokenJavascriptUtil.stringToU8(identifier)}
                            poTokenU8 = obtainPoToken(webPoSignalOutput, integrityToken, u8Identifier)
                            poTokenU8String = ""
                            for (i = 0; i < poTokenU8.length; i++) {
                                if (i != 0) poTokenU8String += ","
                                poTokenU8String += poTokenU8[i]
                            }
                            $JS_INTERFACE.onObtainPoTokenResult(identifier, poTokenU8String)
                        } catch (error) {
                            $JS_INTERFACE.onObtainPoTokenError(identifier, error + "\n" + error.stack)
                        }
                        """.trimIndent(),
                        null,
                    )
                }
            }
        }

        fun close() {
            mainHandler.post {
                webView.clearHistory()
                webView.clearCache(true)
                webView.loadUrl("about:blank")
                webView.onPause()
                webView.removeAllViews()
                webView.destroy()
            }
        }

        private fun loadHtmlAndObtainBotguard() {
            scope.launch {
                runCatching {
                    context.assets.open("po_token.html").bufferedReader().use { it.readText() }
                }.onSuccess { html ->
                    withContext(Dispatchers.Main) {
                        webView.loadDataWithBaseURL(
                            "https://www.youtube.com",
                            html.replaceFirst(
                                "</script>",
                                "\n$JS_INTERFACE.downloadAndRunBotguard()</script>",
                            ),
                            "text/html",
                            "utf-8",
                            null,
                        )
                    }
                }.onFailure(::failInitialization)
            }
        }

        @JavascriptInterface
        fun downloadAndRunBotguard() {
            scope.launch {
                runCatching {
                    val responseBody = makeBotguardServiceRequest(
                        url = "https://www.youtube.com/api/jnn/v1/Create",
                        data = listOf(REQUEST_KEY),
                    )
                    YoutubePoTokenJavascriptUtil.parseChallengeData(responseBody)
                }.onSuccess { parsedChallengeData ->
                    withContext(Dispatchers.Main) {
                        webView.evaluateJavascript(
                            """
                            try {
                                data = $parsedChallengeData
                                runBotGuard(data).then(function (result) {
                                    this.webPoSignalOutput = result.webPoSignalOutput
                                    $JS_INTERFACE.onRunBotguardResult(result.botguardResponse)
                                }, function (error) {
                                    $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                                })
                            } catch (error) {
                                $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                            }
                            """.trimIndent(),
                            null,
                        )
                    }
                }.onFailure(::failInitialization)
            }
        }

        @JavascriptInterface
        fun onRunBotguardResult(botguardResponse: String) {
            scope.launch {
                runCatching {
                    val response = makeBotguardServiceRequest(
                        url = "https://www.youtube.com/api/jnn/v1/GenerateIT",
                        data = listOf(REQUEST_KEY, botguardResponse),
                    )
                    YoutubePoTokenJavascriptUtil.parseIntegrityTokenData(response)
                }.onSuccess { (integrityToken, _) ->
                    withContext(Dispatchers.Main) {
                        webView.evaluateJavascript(
                            "this.integrityToken = $integrityToken",
                        ) {
                            initializationContinuation?.resume(this@BotGuardWebView)
                            initializationContinuation = null
                        }
                    }
                }.onFailure(::failInitialization)
            }
        }

        @JavascriptInterface
        fun onJsInitializationError(error: String) {
            failInitialization(IllegalStateException(error))
        }

        @JavascriptInterface
        fun onObtainPoTokenError(identifier: String, error: String) {
            poTokenContinuations.remove(identifier)?.resumeWithException(IllegalStateException(error))
        }

        @JavascriptInterface
        fun onObtainPoTokenResult(identifier: String, poTokenU8: String) {
            runCatching {
                YoutubePoTokenJavascriptUtil.u8ToBase64(poTokenU8)
            }.onSuccess { poToken ->
                poTokenContinuations.remove(identifier)?.resume(poToken)
            }.onFailure { error ->
                poTokenContinuations.remove(identifier)?.resumeWithException(error)
            }
        }

        private fun failInitialization(error: Throwable) {
            runCatching {
                initializationContinuation?.resumeWithException(error)
            }
            initializationContinuation = null
            close()
        }

        private fun makeBotguardServiceRequest(url: String, data: List<String>): String {
            val connection = URL(url).openConnection() as HttpURLConnection
            return try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Content-Type", "application/json+protobuf")
                connection.setRequestProperty("x-goog-api-key", GOOGLE_API_KEY)
                connection.setRequestProperty("x-user-agent", "grpc-web-javascript/0.1")
                connection.outputStream.bufferedWriter().use { writer ->
                    writer.write(Json.encodeToString(data))
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                } ?: throw IllegalStateException("BotGuard request failed without a response body ($responseCode)")
                val body = BufferedReader(InputStreamReader(stream)).use { it.readText() }
                if (responseCode !in 200..299) {
                    throw IllegalStateException("BotGuard request failed ($responseCode): ${body.take(240)}")
                }
                body
            } finally {
                connection.disconnect()
            }
        }

        companion object {
            suspend fun initialize(context: Context): BotGuardWebView {
                return withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine { continuation ->
                        val instance = BotGuardWebView(context)
                        instance.initializationContinuation = continuation
                        continuation.invokeOnCancellation {
                            instance.close()
                        }
                        instance.loadHtmlAndObtainBotguard()
                    }
                }
            }
        }
    }

    // These BotGuard constants currently mirror LibreTube's implementation:
    // GOOGLE_API_KEY:
    // https://github.com/libre-tube/LibreTube/blob/73e9a0fd9bf7f5ee1d6acb22d2fc4d8353bcc815/app/src/main/java/com/github/libretube/api/ExternalApi.kt#L23
    // REQUEST_KEY:
    // https://github.com/libre-tube/LibreTube/blob/73e9a0fd9bf7f5ee1d6acb22d2fc4d8353bcc815/app/src/main/java/com/github/libretube/api/poToken/PoTokenWebView.kt#L268
    // Maintenance check:
    // Invoke-RestMethod -Uri 'https://raw.githubusercontent.com/libre-tube/LibreTube/main/app/src/main/java/com/github/libretube/api/ExternalApi.kt' | Select-String -Pattern 'GOOGLE_API_KEY'; Invoke-RestMethod -Uri 'https://raw.githubusercontent.com/libre-tube/LibreTube/main/app/src/main/java/com/github/libretube/api/poToken/PoTokenWebView.kt' | Select-String -Pattern 'REQUEST_KEY'
    private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
    private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
    private const val JS_INTERFACE = "LocalDownloaderPoTokenBridge"
}
