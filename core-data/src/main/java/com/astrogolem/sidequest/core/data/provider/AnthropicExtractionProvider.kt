package com.astrogolem.sidequest.core.data.provider

import com.astrogolem.sidequest.core.data.model.ProviderKind
import com.astrogolem.sidequest.core.data.repo.SecurityService
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class AnthropicExtractionProvider @Inject constructor(
    private val securityService: SecurityService,
) : ExtractionProvider {
    override val kind: ProviderKind = ProviderKind.ANTHROPIC

    override suspend fun enhance(request: ProviderExtractionRequest): Result<ProviderExtractionResponse> {
        val apiKey = securityService.getProviderKey(ProviderKind.ANTHROPIC)
            ?.trim()
            ?: return Result.failure(IllegalStateException("ANTHROPIC is not configured"))

        return runCatching {
            val payload = JSONObject().apply {
                put("model", "claude-sonnet-4-20250514")
                put("max_tokens", 1000)
                put("system", buildStructuredIntelSystemInstructions())
                put(
                    "messages",
                    JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", buildContent(request))
                        },
                    ),
                )
            }

            val response = postJson(
                url = "https://api.anthropic.com/v1/messages",
                apiKey = apiKey,
                payload = payload,
            )
            parseProviderResponse(parseMessagesText(response))
        }
    }

    private fun buildContent(request: ProviderExtractionRequest): JSONArray {
        return JSONArray().apply {
            request.imageDataUrl?.let { imageDataUrl ->
                val parsed = parseDataUrl(imageDataUrl)
                put(
                    JSONObject().apply {
                        put("type", "image")
                        put(
                            "source",
                            JSONObject().apply {
                                put("type", "base64")
                                put("media_type", parsed.mediaType)
                                put("data", parsed.base64Data)
                            },
                        )
                    },
                )
            }
            put(
                JSONObject().apply {
                    put("type", "text")
                    put("text", buildStructuredIntelUserPrompt(request))
                },
            )
        }
    }

    private fun postJson(
        url: String,
        apiKey: String,
        payload: JSONObject,
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            setRequestProperty("content-type", "application/json")
            setRequestProperty("accept", "application/json")
        }

        connection.outputStream.use { it.write(payload.toString().toByteArray()) }
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw IllegalStateException("Anthropic request failed with ${connection.responseCode}: $error")
        }
        return stream.bufferedReader().use { it.readText() }
    }

    private fun parseMessagesText(responseText: String): String {
        val root = JSONObject(responseText)
        val content = root.optJSONArray("content") ?: return responseText
        val textParts = buildList {
            for (index in 0 until content.length()) {
                val item = content.optJSONObject(index) ?: continue
                if (item.optString("type") == "text") {
                    item.optString("text").takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }
        return textParts.joinToString("\n").ifBlank { responseText }
    }

    private fun parseProviderResponse(rawText: String): ProviderExtractionResponse {
        return parseStructuredIntelResponse(rawText, defaultReasoning = "anthropic insight")
    }

    private fun parseDataUrl(dataUrl: String): ParsedDataUrl {
        val match = Regex("""^data:([^;]+);base64,(.+)$""").find(dataUrl.trim())
            ?: error("Unsupported image data URL")
        return ParsedDataUrl(
            mediaType = match.groupValues[1],
            base64Data = match.groupValues[2],
        )
    }

    private data class ParsedDataUrl(
        val mediaType: String,
        val base64Data: String,
    )
}
