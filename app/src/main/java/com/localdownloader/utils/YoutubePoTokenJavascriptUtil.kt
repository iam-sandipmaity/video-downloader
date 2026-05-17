package com.localdownloader.utils

import android.util.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

internal object YoutubePoTokenJavascriptUtil {
    fun parseChallengeData(rawChallengeData: String): String {
        val scrambled = Json.parseToJsonElement(rawChallengeData).jsonArray
        val challengeData = if (scrambled.size > 1 && scrambled[1].jsonPrimitive.isString) {
            val descrambled = descramble(scrambled[1].jsonPrimitive.content)
            Json.parseToJsonElement(descrambled).jsonArray
        } else {
            scrambled[1].jsonArray
        }

        val privateScriptValue = challengeData[1]
            .takeIf { it !is JsonNull }
            ?.jsonArray
            ?.find { it.jsonPrimitive.isString }
        val privateResourceValue = challengeData[2]
            .takeIf { it !is JsonNull }
            ?.jsonArray
            ?.find { it.jsonPrimitive.isString }

        return Json.encodeToString(
            JsonObject.serializer(),
            JsonObject(
                mapOf(
                    "messageId" to JsonPrimitive(challengeData[0].jsonPrimitive.content),
                    "interpreterJavascript" to JsonObject(
                        mapOf(
                            "privateDoNotAccessOrElseSafeScriptWrappedValue" to (privateScriptValue ?: JsonNull),
                            "privateDoNotAccessOrElseTrustedResourceUrlWrappedValue" to (privateResourceValue ?: JsonNull),
                        ),
                    ),
                    "interpreterHash" to JsonPrimitive(challengeData[3].jsonPrimitive.content),
                    "program" to JsonPrimitive(challengeData[4].jsonPrimitive.content),
                    "globalName" to JsonPrimitive(challengeData[5].jsonPrimitive.content),
                    "clientExperimentsStateBlob" to JsonPrimitive(challengeData[7].jsonPrimitive.content),
                ),
            ),
        )
    }

    fun parseIntegrityTokenData(rawIntegrityTokenData: String): Pair<String, Long> {
        val integrityTokenData = Json.parseToJsonElement(rawIntegrityTokenData).jsonArray
        return base64ToU8(integrityTokenData[0].jsonPrimitive.content) to integrityTokenData[1].jsonPrimitive.long
    }

    fun stringToU8(identifier: String): String {
        return newUint8Array(identifier.toByteArray())
    }

    fun u8ToBase64(poToken: String): String {
        val bytes = poToken.split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { it.toInt().toByte() }
            .toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    private fun descramble(scrambledChallenge: String): String {
        return base64ToByteArray(scrambledChallenge)
            .map { (it + 97).toByte() }
            .toByteArray()
            .decodeToString()
    }

    private fun base64ToU8(base64: String): String {
        return newUint8Array(base64ToByteArray(base64))
    }

    private fun newUint8Array(contents: ByteArray): String {
        return "new Uint8Array([" + contents.joinToString(separator = ",") { it.toUByte().toString() } + "])"
    }

    private fun base64ToByteArray(base64: String): ByteArray {
        val normalized = base64
            .replace('-', '+')
            .replace('_', '/')
            .replace('.', '=')
        return Base64.decode(normalized, Base64.DEFAULT)
    }
}
