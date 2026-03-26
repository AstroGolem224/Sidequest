package com.astrogolem.sidequest.core.data.provider

import com.astrogolem.sidequest.core.data.model.ProviderKind
import com.astrogolem.sidequest.core.data.model.ExtractionKind

data class ProviderExtractionRequest(
    val ocrText: String,
    val documentHint: String,
    val imageDataUrl: String? = null,
)

data class ProviderExtractionItem(
    val text: String,
    val kind: ExtractionKind,
    val reasoning: String,
)

data class ProviderExtractionResponse(
    val summary: String,
    val items: List<ProviderExtractionItem>,
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
