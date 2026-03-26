package com.astrogolem.sidequest.core.data.provider

import com.astrogolem.sidequest.core.data.model.ExtractionKind
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
            ?.trim()
            ?: return Result.failure(IllegalStateException("OPENAI is not configured"))

        return runCatching {
            val userContent = JSONArray().apply {
                put(
                    JSONObject().apply {
                        put("type", "input_text")
                        put(
                            "text",
                            buildPrompt(request),
                        )
                    },
                )
                request.imageDataUrl?.let { imageDataUrl ->
                    put(
                        JSONObject().apply {
                            put("type", "input_image")
                            put("image_url", imageDataUrl)
                            put("detail", "low")
                        },
                    )
                }
            }
            val responsesPayload = JSONObject().apply {
                put("model", "gpt-4.1-mini")
                put(
                    "instructions",
                    buildSystemInstructions(),
                )
                put("input", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", userContent)
                }))
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
                        put(
                            "messages",
                            JSONArray()
                                .put(
                                    JSONObject().apply {
                                        put("role", "system")
                                        put("content", buildSystemInstructions())
                                    },
                                )
                                .put(
                                    JSONObject().apply {
                                        put("role", "user")
                                        put("content", JSONArray().apply {
                                            put(
                                                JSONObject().apply {
                                                    put("type", "text")
                                                    put("text", buildPrompt(request))
                                                },
                                            )
                                            request.imageDataUrl?.let { imageDataUrl ->
                                                put(
                                                    JSONObject().apply {
                                                        put("type", "image_url")
                                                        put(
                                                            "image_url",
                                                            JSONObject().apply {
                                                                put("url", imageDataUrl)
                                                                put("detail", "low")
                                                            },
                                                        )
                                                    },
                                                )
                                            }
                                        })
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
            items = parseProviderItems(parsed),
        )
    }

    private fun parseProviderItems(parsed: JSONObject): List<ProviderExtractionItem> {
        val items = parsed.optJSONArray("items")
        if (items != null) {
            return buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val text = item.optString("text").trim()
                    if (text.isBlank()) continue
                    val kind = item.optString("kind")
                        .trim()
                        .uppercase()
                        .takeIf { it.isNotBlank() }
                        ?.let { runCatching { ExtractionKind.valueOf(it) }.getOrNull() }
                        ?: inferKindFallback(text)
                    add(
                        ProviderExtractionItem(
                            text = text,
                            kind = kind,
                            reasoning = item.optString("reasoning").trim().ifBlank { "provider insight" },
                        ),
                    )
                }
            }
        }

        val normalizedLines = parsed.optJSONArray("normalizedLines") ?: JSONArray()
        return buildList {
            for (index in 0 until normalizedLines.length()) {
                val text = normalizedLines.optString(index).trim()
                if (text.isBlank()) continue
                add(
                    ProviderExtractionItem(
                        text = text,
                        kind = inferKindFallback(text),
                        reasoning = "provider normalized line",
                    ),
                )
            }
        }
    }

    private fun extractJsonObject(text: String): String {
        if (text.startsWith("{") && text.endsWith("}")) return text
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        require(start >= 0 && end > start) { "Provider response did not contain JSON" }
        return text.substring(start, end + 1)
    }

    private fun buildPrompt(request: ProviderExtractionRequest): String {
        return buildString {
            appendLine("Analyze this Sidequest capture.")
            appendLine("Document hint: ${request.documentHint}")
            if (request.ocrText.isNotBlank()) {
                appendLine("OCR text:")
                appendLine(request.ocrText)
            } else {
                appendLine("OCR text is empty or unreliable. Use the image directly.")
            }
            appendLine("Return only JSON.")
        }
    }

    private fun buildSystemInstructions(): String {
        return """
            You are helping a local-first Android productivity app turn a capture into structured intel.
            Inspect the image when available and use OCR text as supporting context.
            Return strict JSON with this shape:
            {
              "summary": "short plain-language summary",
              "items": [
                {
                  "kind": "TASK" | "DATE" | "REFERENCE" | "FACT",
                  "text": "single concise extracted line",
                  "reasoning": "why this belongs to that kind"
                }
              ]
            }
            Rules:
            - Only emit TASK when the capture contains a real actionable next step.
            - Use DATE for deadlines, validity windows, appointments, and explicit time anchors.
            - Use REFERENCE for IDs, codes, invoice numbers, tracking numbers, booking refs, and similar identifiers.
            - Use FACT for notable searchable context that is not actionable.
            - Keep items concise and deduplicated.
            - Prefer 0 tasks over inventing action.
        """.trimIndent()
    }

    private fun inferKindFallback(text: String): ExtractionKind {
        val lowered = text.lowercase()
        return when {
            Regex("""\b(reference|ref|invoice|order|tracking|booking|confirmation|ticket|number|nr|code|id)\b""").containsMatchIn(lowered) ->
                ExtractionKind.REFERENCE
            Regex("""\b\d{1,2}[./-]\d{1,2}([./-]\d{2,4})?\b""").containsMatchIn(text) ||
                Regex("""\b(january|february|march|april|may|june|july|august|september|october|november|december|today|tomorrow|heute|morgen)\b""")
                    .containsMatchIn(lowered) ->
                ExtractionKind.DATE
            Regex("""\b(add|book|buy|call|check|email|fix|follow|pay|plan|review|schedule|send|set|todo|update|anrufen|bezahlen|kaufen|planen|prüfen|prufen)\b""")
                .containsMatchIn(lowered) ->
                ExtractionKind.TASK
            else -> ExtractionKind.FACT
        }
    }
}
