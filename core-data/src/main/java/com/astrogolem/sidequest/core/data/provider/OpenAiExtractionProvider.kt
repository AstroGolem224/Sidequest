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
class OpenAiExtractionProvider @Inject constructor(
    private val securityService: SecurityService,
) : ExtractionProvider {
    override val kind: ProviderKind = ProviderKind.OPENAI

    override suspend fun enhance(request: ProviderExtractionRequest): Result<ProviderExtractionResponse> {
        val apiKey = securityService.getProviderKey(ProviderKind.OPENAI)
            ?: return Result.failure(IllegalStateException("OPENAI is not configured"))

        return runCatching {
            val responsesPayload = JSONObject().apply {
                put("model", "gpt-4.1-mini")
                put("temperature", 0.2)
                put(
                    "instructions",
                    "Return strict JSON with keys summary and normalizedLines. normalizedLines must be an array of concise actionable lines.",
                )
                put(
                    "input",
                    "Document hint: ${request.documentHint}\nOCR text:\n${request.ocrText}\nReturn only JSON.",
                )
            }

            val rawText = postJson(
                url = "https://api.openai.com/v1/responses",
                apiKey = apiKey,
                payload = responsesPayload,
            ).fold(
                onSuccess = { response -> parseResponsesText(response) },
                onFailure = {
                    val chatPayload = JSONObject().apply {
                        put("model", "gpt-4.1-mini")
                        put("temperature", 0.2)
                        put(
                            "messages",
                            JSONArray()
                                .put(
                                    JSONObject().apply {
                                        put("role", "system")
                                        put(
                                            "content",
                                            "Return strict JSON with keys summary and normalizedLines. normalizedLines must be an array of concise actionable lines.",
                                        )
                                    },
                                )
                                .put(
                                    JSONObject().apply {
                                        put("role", "user")
                                        put(
                                            "content",
                                            "Document hint: ${request.documentHint}\nOCR text:\n${request.ocrText}\nReturn only JSON.",
                                        )
                                    },
                                ),
                        )
                    }
                    val fallback = postJson(
                        url = "https://api.openai.com/v1/chat/completions",
                        apiKey = apiKey,
                        payload = chatPayload,
                    ).getOrThrow()
                    parseChatCompletionsText(fallback)
                },
            )

            parseProviderResponse(rawText)
        }
    }

    private fun postJson(
        url: String,
        apiKey: String,
        payload: JSONObject,
    ): Result<String> = runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("OpenAI-Beta", "assistants=v2")
        }

        connection.outputStream.use { it.write(payload.toString().toByteArray()) }
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw IllegalStateException("OpenAI request failed with ${connection.responseCode}: $error")
        }
        stream.bufferedReader().use { it.readText() }
    }

    private fun parseResponsesText(responseText: String): String {
        val root = JSONObject(responseText)
        root.optString("output_text").takeIf { it.isNotBlank() }?.let { return it }

        val output = root.optJSONArray("output") ?: return responseText
        for (index in 0 until output.length()) {
            val item = output.optJSONObject(index) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (contentIndex in 0 until content.length()) {
                val contentItem = content.optJSONObject(contentIndex) ?: continue
                contentItem.optString("text").takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return responseText
    }

    private fun parseChatCompletionsText(responseText: String): String {
        val root = JSONObject(responseText)
        return root.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }

    private fun parseProviderResponse(rawText: String): ProviderExtractionResponse {
        val normalized = rawText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val jsonText = extractJsonObject(normalized)
        val parsed = JSONObject(jsonText)

        return ProviderExtractionResponse(
            summary = parsed.optString("summary").trim(),
            normalizedLines = buildList {
                val lines = parsed.optJSONArray("normalizedLines") ?: JSONArray()
                for (index in 0 until lines.length()) {
                    add(lines.optString(index).trim())
                }
            }.filter { it.isNotBlank() },
        )
    }

    private fun extractJsonObject(text: String): String {
        if (text.startsWith("{") && text.endsWith("}")) return text
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        require(start >= 0 && end > start) { "Provider response did not contain JSON" }
        return text.substring(start, end + 1)
    }
}
