package com.astrogolem.sidequest.core.data.provider

import com.astrogolem.sidequest.core.data.model.ExtractionKind
import org.json.JSONArray
import org.json.JSONObject

internal fun buildStructuredIntelSystemInstructions(): String {
    return """
        You extract structured intel from Android app captures.
        Inspect the image first. Use OCR as supporting context only.

        ---

        ## STEP 1 - Detect document flags

        Scan the capture for any of these inline tags:
        - `#D` - date mentioned on this line
        - `#P` - person or people
        - `#T` - topic, project, or activity title
        - `#Q` - question (answer it if possible from context or general knowledge)
        - `#A` - action item
        - `#DD` - due date

        If any flag is found, treat the capture as a photographed note.
        Read the note completely, not just the flagged lines.
        Produce a precise summary of what the note means.
        Interpret the whole note before deciding what belongs in summary, items, and markdown.

        If any flag is found, set `"type": "NOTE"` and populate the `"note"` field.
        If no flags are found, omit `"type"` and `"note"` entirely.

        ---

        ## STEP 2 - Extract intel

        Return strict JSON - nothing else:

        {
          "summary": "one plain-language sentence",
          "type": "NOTE",
          "items": [
            {
              "kind": "TASK" | "DATE" | "REFERENCE" | "FACT",
              "text": "concise extracted line",
              "reasoning": "one short phrase"
            }
          ],
          "note": {
            "filename": "YYYY-MM-DD_HH-MM_topic-slug.md",
            "content": "# [Topic]\n\n## Summary\n...\n\n## Topics\n- topic | related context\n\n## People\n- person | related topic | context\n\n## Dates\n- date | related topic | meaning\n\n## Questions & Answers\n- Q: ...\n  A: ...\n\n## Action Items\n| Action | Related Topic | Owner | Due |\n| --- | --- | --- | --- |\n| ... | ... | ... | ... |\n\n## Due Dates\n- due date | related action | topic"
          }
        }

        Kind rules:
        - TASK: real actionable next step in the capture. Clutter/mess scenes may produce one cleanup TASK. Note-derived action items should become TASK items. Never TASK for code, terminal output, git text, or dev/UI notes.
        - DATE: deadline, appointment, validity window, or explicit time anchor.
        - REFERENCE: ID, invoice, tracking, booking, or similar identifier.
        - FACT: notable searchable context - not actionable.

        Defaults:
        - Prefer 0 items over invented ones.
        - For scene images, infer intent from the scene before reading text.
        - If nothing is actionable, return FACT / DATE / REFERENCE only.
        - If a note contains `#Q`, answer the question from the document first. If the document does not answer it, use general knowledge and keep the answer concise.
        - If a note contains people or dates, list them together with the topics they relate to.
        - For note captures, the markdown must be useful as a real saved note, not a placeholder shell.
        - Action items inside the markdown must be rendered as a markdown table.
    """.trimIndent()
}

internal fun buildStructuredIntelUserPrompt(request: ProviderExtractionRequest): String {
    val ocrText = request.ocrText.takeIf { it.isNotBlank() } ?: "(none - read image directly)"
    return """
        Analyze this SideQuest capture.
        Mode: ${request.analysisMode.name}
        Hint: ${request.documentHint}
        OCR: $ocrText

        Return only JSON.
    """.trimIndent()
}

internal fun parseStructuredIntelResponse(
    rawText: String,
    defaultReasoning: String,
): ProviderExtractionResponse {
    val normalized = rawText
        .trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    val jsonText = extractJsonObject(normalized)
    val parsed = JSONObject(jsonText)
    val type = parsed.optString("type").trim().takeIf { it.isNotBlank() }
    val note = parsed.optJSONObject("note")
        ?.let { noteObject ->
            val filename = noteObject.optString("filename").trim()
            val content = noteObject.optString("content").trim()
            if (filename.isBlank() || content.isBlank()) {
                null
            } else {
                ProviderGeneratedNote(filename = filename, content = content)
            }
        }
        ?.takeIf { type.equals("NOTE", ignoreCase = true) }

    return ProviderExtractionResponse(
        summary = parsed.optString("summary").trim(),
        items = parseProviderItems(parsed, defaultReasoning),
        type = type,
        note = note,
    )
}

private fun parseProviderItems(
    parsed: JSONObject,
    defaultReasoning: String,
): List<ProviderExtractionItem> {
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
                        reasoning = item.optString("reasoning").trim().ifBlank { defaultReasoning },
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
                    reasoning = defaultReasoning,
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
