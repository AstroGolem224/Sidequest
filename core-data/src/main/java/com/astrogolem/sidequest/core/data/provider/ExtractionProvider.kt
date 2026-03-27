package com.astrogolem.sidequest.core.data.provider

import com.astrogolem.sidequest.core.data.model.ProviderKind
import com.astrogolem.sidequest.core.data.model.ExtractionKind

enum class ProviderAnalysisMode { IMAGE_FIRST, OCR_FIRST, IMAGE_PLUS_OCR }

data class ProviderExtractionRequest(
    val ocrText: String,
    val documentHint: String,
    val imageDataUrl: String? = null,
    val analysisMode: ProviderAnalysisMode = ProviderAnalysisMode.OCR_FIRST,
)

data class ProviderExtractionItem(
    val text: String,
    val kind: ExtractionKind,
    val reasoning: String,
)

data class ProviderGeneratedNote(
    val filename: String,
    val content: String,
)

data class ProviderExtractionResponse(
    val summary: String,
    val items: List<ProviderExtractionItem>,
    val type: String? = null,
    val note: ProviderGeneratedNote? = null,
)

interface ExtractionProvider {
    val kind: ProviderKind
    suspend fun enhance(request: ProviderExtractionRequest): Result<ProviderExtractionResponse>
}

class UnconfiguredExtractionProvider(
    override val kind: ProviderKind,
) : ExtractionProvider {
    override suspend fun enhance(request: ProviderExtractionRequest): Result<ProviderExtractionResponse> {
        return Result.failure(IllegalStateException("${kind.name} is not configured"))
    }
}
