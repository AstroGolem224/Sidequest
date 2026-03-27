package com.astrogolem.sidequest.core.data.provider

internal fun buildStructuredIntelSystemInstructions(): String {
    return """
        You extract structured intel from Android app captures.
        Inspect the image first. Use OCR as supporting context only.

        Return strict JSON — nothing else:
        {
          "summary": "one plain-language sentence",
          "items": [
            {
              "kind": "TASK" | "DATE" | "REFERENCE" | "FACT",
              "text": "concise extracted line",
              "reasoning": "one short phrase"
            }
          ]
        }

        Kind rules:
        - TASK: real actionable next step in the capture. Clutter/mess scenes may produce one cleanup TASK. Never TASK for code, terminal output, git text, or dev/UI notes.
        - DATE: deadline, appointment, validity window, or explicit time anchor.
        - REFERENCE: ID, invoice, tracking, booking, or similar identifier.
        - FACT: notable searchable context — not actionable.

        Defaults:
        - Prefer 0 items over invented ones.
        - For scene images, infer intent from the scene before reading text.
        - If nothing is actionable, return FACT / DATE / REFERENCE only.
    """.trimIndent()
}

internal fun buildStructuredIntelUserPrompt(request: ProviderExtractionRequest): String {
    val ocrText = request.ocrText.takeIf { it.isNotBlank() } ?: "(none — read image directly)"
    return """
        Analyze this SideQuest capture.
        Mode: ${request.analysisMode.name}
        Hint: ${request.documentHint}
        OCR: $ocrText

        Return only JSON.
    """.trimIndent()
}
