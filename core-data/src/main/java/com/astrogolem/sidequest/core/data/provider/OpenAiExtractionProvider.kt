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
            val connection = (URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
            }

            val payload = JSONObject().apply {
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

            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            val responseText = (if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: throw IllegalStateException("OpenAI request failed with ${connection.responseCode}")
            }).bufferedReader().use { it.readText() }

            val root = JSONObject(responseText)
            val content = root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsed = JSONObject(content)
            ProviderExtractionResponse(
                summary = parsed.optString("summary"),
                normalizedLines = buildList {
                    val lines = parsed.optJSONArray("normalizedLines") ?: JSONArray()
                    for (index in 0 until lines.length()) {
                        add(lines.optString(index))
                    }
                }.filter { it.isNotBlank() },
            )
        }
    }
}
