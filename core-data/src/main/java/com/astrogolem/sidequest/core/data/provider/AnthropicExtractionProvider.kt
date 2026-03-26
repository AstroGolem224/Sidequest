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
                put("system", buildSystemInstructions())
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
                    put("text", buildPrompt(request))
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
                            reasoning = item.optString("reasoning").trim().ifBlank { "anthropic insight" },
                        ),
                    )
                }
            }
        }
        return emptyList()
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
            appendLine("Analysis mode: ${request.analysisMode.name}")
            appendLine("Document hint: ${request.documentHint}")
            if (request.ocrText.isNotBlank()) {
                appendLine("OCR text:")
                appendLine(request.ocrText)
            } else {
                appendLine("OCR text is empty or unreliable. Use the image directly.")
            }
            appendLine("Decide from the image first whether the user likely needs reading help, cleanup help, a reminder, or only memory storage.")
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
            - If the image shows clutter, mess, or a reset opportunity, you may emit one TASK that frames the likely cleanup quest.
            - If the image is mostly a scene and not a document, focus on likely intent and scene-based action before OCR.
            - If no reading is needed, still emit scene-based TASK or FACT items when useful.
            - Never emit TASK for developer notes, build/test instructions, terminal output, git workflow text, or UI review copy about the app itself.
            - If the capture is informational but not actionable, return FACT, DATE, or REFERENCE only.
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
