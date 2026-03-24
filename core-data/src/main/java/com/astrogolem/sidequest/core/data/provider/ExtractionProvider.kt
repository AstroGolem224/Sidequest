package com.astrogolem.sidequest.core.data.provider

import com.astrogolem.sidequest.core.data.model.ProviderKind

data class ProviderExtractionRequest(
    val ocrText: String,
    val documentHint: String,
)

data class ProviderExtractionResponse(
    val summary: String,
    val normalizedLines: List<String>,
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
