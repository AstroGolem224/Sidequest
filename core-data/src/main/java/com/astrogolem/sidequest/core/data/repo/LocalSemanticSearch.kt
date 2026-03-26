package com.astrogolem.sidequest.core.data.repo

import com.astrogolem.sidequest.core.data.model.SearchFilter
import com.astrogolem.sidequest.core.data.model.SearchResultModel
import com.astrogolem.sidequest.core.data.model.SearchResultType
import kotlin.math.max
import kotlin.math.min

internal data class SemanticMemoryDocument(
    val id: String,
    val title: String,
    val body: String,
    val resultType: SearchResultType,
    val sourceLabel: String,
    val captureId: String? = null,
    val missionId: String? = null,
    val noteId: String? = null,
    val createdAt: Long = 0L,
    val qualityWeight: Float = 0f,
)

internal fun semanticSearch(
    query: String,
    filter: SearchFilter,
    documents: List<SemanticMemoryDocument>,
    exactIds: Set<String> = emptySet(),
    nowMillis: Long = System.currentTimeMillis(),
): List<SearchResultModel> {
    val normalizedQuery = normalizeSearchText(query)
    if (normalizedQuery.isBlank()) return emptyList()

    val queryTerms = expandSemanticTerms(tokenizeSearchText(normalizedQuery))
    if (queryTerms.isEmpty()) return emptyList()

    val preferredType = preferredResultType(queryTerms)
    val deduped = linkedMapOf<String, Pair<SearchResultModel, Float>>()

    for (document in documents) {
        if (!matchesFilter(document.resultType, filter)) continue

        val titleNorm = normalizeSearchText(document.title)
        val bodyNorm = normalizeSearchText(document.body)
        val titleTerms = expandSemanticTerms(tokenizeSearchText(titleNorm))
        val bodyTerms = expandSemanticTerms(tokenizeSearchText(bodyNorm))
        val combinedTerms = (titleTerms + bodyTerms).toSet()

        val titleOverlap = overlapScore(queryTerms, titleTerms)
        val bodyOverlap = overlapScore(queryTerms, combinedTerms)
        val phraseBoost = phraseBoost(normalizedQuery, titleNorm, bodyNorm)
        val trigram = max(
            trigramDice(normalizedQuery, titleNorm),
            trigramDice(normalizedQuery, "$titleNorm $bodyNorm"),
        )
        val exactBoost = if (document.id in exactIds) 0.24f else 0f
        val typeBoost = if (preferredType == document.resultType) 0.08f else 0f
        val freshnessBoost = freshnessBoost(document.createdAt, nowMillis)

        val score = (titleOverlap * 0.34f) +
            (bodyOverlap * 0.24f) +
            (phraseBoost * 0.18f) +
            (trigram * 0.12f) +
            exactBoost +
            typeBoost +
            document.qualityWeight +
            freshnessBoost

        if (score < 0.18f && document.id !in exactIds) continue

        val matchLabel = when {
            document.id in exactIds || phraseBoost >= 0.95f -> "Exact"
            trigram >= 0.74f || bodyOverlap >= 0.55f -> "Semantic"
            titleOverlap >= 0.34f -> "Title"
            else -> "Related"
        }

        val result = SearchResultModel(
            id = document.id,
            title = document.title.ifBlank { fallbackTitle(document.resultType) },
            snippet = snippetForSearch(document),
            resultType = document.resultType,
            sourceLabel = document.sourceLabel,
            matchLabel = matchLabel,
            captureId = document.captureId,
            missionId = document.missionId,
            noteId = document.noteId,
        )

        val dedupeKey = when {
            document.missionId != null -> "mission:${document.missionId}"
            document.noteId != null -> "note:${document.noteId}"
            document.captureId != null -> "${document.captureId}:${document.resultType}:${normalizeSearchText(document.title)}"
            else -> "${document.resultType}:${normalizeSearchText(document.title)}"
        }
        val current = deduped[dedupeKey]
        if (current == null || score > current.second) {
            deduped[dedupeKey] = result to score
        }
    }

    return deduped.values
        .sortedByDescending { it.second }
        .map { it.first }
        .take(24)
}

internal fun normalizeSearchText(value: String): String {
    return value
        .lowercase()
        .replace(Regex("""[`'’"]"""), "")
        .replace(Regex("""[^\p{L}\p{N}\s:/.-]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

internal fun tokenizeSearchText(value: String): Set<String> {
    return value
        .split(Regex("""[\s:/._-]+"""))
        .asSequence()
        .map { it.trim() }
        .filter { it.length >= 2 }
        .map(::stemToken)
        .filter { it.length >= 2 }
        .toSet()
}

internal fun expandSemanticTerms(tokens: Set<String>): Set<String> {
    return buildSet {
        tokens.forEach { token ->
            add(token)
            SearchSynonyms[token]?.forEach(::add)
        }
    }
}

private fun stemToken(token: String): String {
    val normalized = token.lowercase()
    return when {
        normalized.length > 5 && normalized.endsWith("ing") -> normalized.dropLast(3)
        normalized.length > 4 && normalized.endsWith("ed") -> normalized.dropLast(2)
        normalized.length > 4 && normalized.endsWith("es") -> normalized.dropLast(2)
        normalized.length > 3 && normalized.endsWith("s") -> normalized.dropLast(1)
        else -> normalized
    }
}

private fun preferredResultType(queryTerms: Set<String>): SearchResultType? {
    return when {
        queryTerms.any { it in setOf("date", "due", "deadline", "expiry", "expire", "valid", "until", "when") } -> SearchResultType.DATE
        queryTerms.any { it in setOf("ref", "reference", "id", "number", "code", "passport", "eta") } -> SearchResultType.REFERENCE
        queryTerms.any { it in setOf("fact", "info", "detail", "note", "memory", "intel") } -> SearchResultType.FACT
        queryTerms.any { it in setOf("markdown", "note", "journal", "document") } -> SearchResultType.NOTE
        queryTerms.any { it in setOf("scan", "capture", "photo", "image", "receipt", "letter") } -> SearchResultType.SCAN
        queryTerms.any { it in setOf("task", "todo", "quest", "call", "send", "buy", "review", "plan") } -> SearchResultType.TASK
        else -> null
    }
}

private fun matchesFilter(type: SearchResultType, filter: SearchFilter): Boolean {
    return when (filter) {
        SearchFilter.ALL -> true
        SearchFilter.TASKS -> type == SearchResultType.TASK
        SearchFilter.DATES -> type == SearchResultType.DATE
        SearchFilter.REFERENCES -> type == SearchResultType.REFERENCE
        SearchFilter.FACTS -> type == SearchResultType.FACT
        SearchFilter.NOTES -> type == SearchResultType.NOTE
        SearchFilter.SCANS -> type == SearchResultType.SCAN
    }
}

private fun overlapScore(queryTerms: Set<String>, docTerms: Set<String>): Float {
    if (queryTerms.isEmpty() || docTerms.isEmpty()) return 0f
    return queryTerms.intersect(docTerms).size.toFloat() / queryTerms.size.toFloat()
}

private fun phraseBoost(query: String, title: String, body: String): Float {
    return when {
        query.isBlank() -> 0f
        title.contains(query) -> 1f
        body.contains(query) -> 0.82f
        query.split(' ').size > 1 && query.split(' ').all { it in body || it in title } -> 0.55f
        else -> 0f
    }
}

private fun trigramDice(left: String, right: String): Float {
    val leftGrams = trigrams(left)
    val rightGrams = trigrams(right)
    if (leftGrams.isEmpty() || rightGrams.isEmpty()) return 0f
    val overlap = leftGrams.intersect(rightGrams).size.toFloat()
    return (2f * overlap) / (leftGrams.size + rightGrams.size).toFloat()
}

private fun trigrams(value: String): Set<String> {
    if (value.length < 3) return if (value.isBlank()) emptySet() else setOf(value)
    val normalized = value.replace(" ", "_")
    return buildSet {
        for (index in 0..normalized.length - 3) {
            add(normalized.substring(index, index + 3))
        }
    }
}

private fun freshnessBoost(createdAt: Long, nowMillis: Long): Float {
    if (createdAt <= 0L) return 0f
    val ageDays = ((nowMillis - createdAt) / (24f * 60f * 60f * 1000f)).coerceAtLeast(0f)
    return when {
        ageDays <= 1f -> 0.05f
        ageDays <= 7f -> 0.03f
        ageDays <= 30f -> 0.015f
        else -> 0f
    }
}

private fun snippetForSearch(document: SemanticMemoryDocument): String {
    val base = when {
        document.body.isNotBlank() -> document.body
        document.title.isNotBlank() -> document.title
        else -> fallbackTitle(document.resultType)
    }.trim()
    return base.take(180)
}

private fun fallbackTitle(type: SearchResultType): String {
    return when (type) {
        SearchResultType.TASK -> "Untitled task"
        SearchResultType.DATE -> "Detected date"
        SearchResultType.REFERENCE -> "Detected reference"
        SearchResultType.FACT -> "Detected fact"
        SearchResultType.NOTE -> "Untitled note"
        SearchResultType.SCAN -> "Saved scan"
    }
}

private val SearchSynonyms = mapOf(
    "buy" to setOf("purchase", "shop", "shopping", "grocery"),
    "grocery" to setOf("shopping", "buy", "list"),
    "invoice" to setOf("receipt", "bill", "payment"),
    "receipt" to setOf("invoice", "bill", "payment"),
    "due" to setOf("deadline", "expiry", "expire", "valid", "until"),
    "expiry" to setOf("due", "expire", "valid", "until"),
    "passport" to setOf("travel", "reference", "document", "eta"),
    "eta" to setOf("reference", "travel", "document"),
    "task" to setOf("todo", "quest", "action"),
    "todo" to setOf("task", "quest", "action"),
    "note" to setOf("memo", "markdown", "document"),
    "call" to setOf("phone", "contact"),
    "email" to setOf("mail", "send"),
    "garden" to setOf("outdoor", "yard"),
    "sport" to setOf("fitness", "exercise", "workout"),
    "meditation" to setOf("mindful", "breathing", "focus"),
)
