package com.astrogolem.sidequest.core.data.repo

import com.astrogolem.sidequest.core.data.model.DocumentType
import com.astrogolem.sidequest.core.data.model.ExtractionKind
import com.astrogolem.sidequest.core.data.provider.ProviderExtractionItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal data class ExtractionDraft(
    val title: String,
    val body: String,
    val confidence: Float,
    val dueAt: Long?,
    val kind: ExtractionKind,
    val reasoning: String,
    val score: Float,
)

internal fun buildExtractionDrafts(
    documentType: DocumentType,
    ocrText: String,
    localLines: List<String>,
    providerItems: List<ProviderExtractionItem>,
): List<ExtractionDraft> {
    val lines = buildMergedLines(ocrText, localLines)
    val reserved = mutableSetOf<String>()
    val providerDrafts = buildProviderDrafts(providerItems, reserved)
    val references = extractReferenceDrafts(lines, reserved)
    val tasks = extractTaskDrafts(lines, documentType, reserved)
    val dates = extractDateDrafts(lines, reserved)
    val facts = extractFactDrafts(lines, documentType, reserved)

    return (providerDrafts + tasks + dates + references + facts)
        .sortedWith(
            compareByDescending<ExtractionDraft> { extractionPriority(it.kind) }
                .thenByDescending { it.score },
        )
        .take(8)
}

internal fun classifyDocumentType(text: String): DocumentType {
    val lowered = text.lowercase()
    return when {
        lowered.isBlank() -> DocumentType.SCENE
        Regex("""\b(total|subtotal|tax|receipt|card payment|change due|summe)\b""").containsMatchIn(lowered) ->
            DocumentType.RECEIPT
        Regex("""\b(dear|regards|sincerely|kind regards|mit freundlichen)\b""").containsMatchIn(lowered) ->
            DocumentType.LETTER
        Regex("""\b(todo|to do|task|checklist|next step|follow up)\b""").containsMatchIn(lowered) ->
            DocumentType.NOTE
        Regex("""\b(whiteboard|brainstorm|agenda|meeting notes|sprint)\b""").containsMatchIn(lowered) ->
            DocumentType.WHITEBOARD
        Regex("""\b(click|tap|settings|notification|android|screen|screenshot)\b""").containsMatchIn(lowered) ->
            DocumentType.SCREEN
        else -> DocumentType.UNKNOWN
    }
}

internal fun extractDueAtFromText(value: String): Long? {
    val trimmed = value.trim()
    val lowered = trimmed.lowercase(Locale.ROOT)
    val now = Calendar.getInstance()

    if (Regex("""\btoday\b|\bheute\b""").containsMatchIn(lowered)) {
        return now.apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    if (Regex("""\btomorrow\b|\bmorgen\b""").containsMatchIn(lowered)) {
        return now.apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val patterns = listOf(
        "d MMMM yyyy",
        "d MMM yyyy",
        "dd MMMM yyyy",
        "dd MMM yyyy",
        "d.M.yyyy",
        "dd.MM.yyyy",
        "d/M/yyyy",
        "dd/MM/yyyy",
        "d-M-yyyy",
        "dd-MM-yyyy",
        "M/d/yyyy",
        "MM/dd/yyyy",
    )

    val normalized = trimmed
        .replace(Regex("""\b(until|valid until|due|by|am|bis|eta|date)\b""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""^[^\p{L}\p{N}]+"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()

    for (pattern in patterns) {
        parseDateCandidate(normalized, pattern)?.let { return it }
    }

    Regex(
        """\b\d{1,2}\s+(january|february|march|april|may|june|july|august|september|october|november|december)\s+\d{4}\b""",
        RegexOption.IGNORE_CASE,
    )
        .find(trimmed)
        ?.value
        ?.let { token ->
            for (pattern in patterns) {
                parseDateCandidate(token, pattern)?.let { return it }
            }
        }

    Regex("""\b\d{1,2}[./-]\d{1,2}([./-]\d{2,4})?\b""")
        .find(trimmed)
        ?.value
        ?.let { token ->
            for (pattern in patterns) {
                parseDateCandidate(token, pattern)?.let { return it }
            }
        }

    return null
}

internal fun inferExtractionKindFromText(body: String): ExtractionKind {
    val normalized = normalizeLine(body)
    return when {
        isReferenceLike(body, normalized) -> ExtractionKind.REFERENCE
        extractDueAtFromText(body) != null -> ExtractionKind.DATE
        isActionableLine(body, normalized, DocumentType.UNKNOWN, fromProvider = false) -> ExtractionKind.TASK
        else -> ExtractionKind.FACT
    }
}

private data class MergedLine(
    val cleaned: String,
    val normalized: String,
)

private fun buildMergedLines(
    ocrText: String,
    localLines: List<String>,
): List<MergedLine> {
    return buildRawCandidateLines(ocrText, localLines)
        .asSequence()
        .map { line ->
            val cleaned = cleanCandidateLine(line)
            MergedLine(
                cleaned = cleaned,
                normalized = normalizeLine(cleaned),
            )
        }
        .filter { isUsefulCandidateLine(it.cleaned, it.normalized) }
        .groupBy { it.normalized }
        .values
        .map { dupes -> dupes.maxBy { it.cleaned.length } }
}

private fun buildProviderDrafts(
    providerItems: List<ProviderExtractionItem>,
    reserved: MutableSet<String>,
): List<ExtractionDraft> {
    return providerItems.asSequence()
        .mapNotNull { item ->
            val cleaned = cleanCandidateLine(item.text)
            val normalized = normalizeLine(cleaned)
            if (!isUsefulCandidateLine(cleaned, normalized)) return@mapNotNull null
            if (item.kind == ExtractionKind.TASK && looksLikeProviderMetaTask(cleaned, item.reasoning)) return@mapNotNull null
            if (!reserved.add(normalized)) return@mapNotNull null
            val score = when (item.kind) {
                ExtractionKind.TASK -> 0.9f
                ExtractionKind.DATE -> 0.84f
                ExtractionKind.REFERENCE -> 0.8f
                ExtractionKind.FACT -> 0.72f
            }
            ExtractionDraft(
                title = deriveCandidateTitle(cleaned),
                body = cleaned,
                confidence = score,
                dueAt = extractDueAtFromText(cleaned),
                kind = item.kind,
                reasoning = item.reasoning.ifBlank { "provider insight" },
                score = score,
            )
        }
        .toList()
}

private fun extractReferenceDrafts(
    lines: List<MergedLine>,
    reserved: MutableSet<String>,
): List<ExtractionDraft> {
    return lines.asSequence()
        .filter { isReferenceLike(it.cleaned, it.normalized) }
        .mapNotNull { line ->
            if (!reserved.add(line.normalized)) return@mapNotNull null
            val score = 0.74f
            ExtractionDraft(
                title = deriveCandidateTitle(line.cleaned),
                body = line.cleaned,
                confidence = score,
                dueAt = null,
                kind = ExtractionKind.REFERENCE,
                reasoning = "reference signal: labeled identifier or code-like value",
                score = score,
            )
        }
        .take(3)
        .toList()
}

private fun extractDateDrafts(
    lines: List<MergedLine>,
    reserved: MutableSet<String>,
): List<ExtractionDraft> {
    return lines.asSequence()
        .mapNotNull { line ->
            val dueAt = extractDueAtFromText(line.cleaned) ?: return@mapNotNull null
            val score = when {
                Regex("""\b(due|until|valid|deadline|appointment|eta|date|bis)\b""", RegexOption.IGNORE_CASE).containsMatchIn(line.cleaned) -> 0.86f
                else -> 0.72f
            }
            if (!reserved.add(line.normalized)) return@mapNotNull null
            ExtractionDraft(
                title = deriveCandidateTitle(line.cleaned),
                body = line.cleaned,
                confidence = score,
                dueAt = dueAt,
                kind = ExtractionKind.DATE,
                reasoning = "date signal: parsed calendar value from OCR text",
                score = score,
            )
        }
        .take(3)
        .toList()
}

private fun extractTaskDrafts(
    lines: List<MergedLine>,
    documentType: DocumentType,
    reserved: MutableSet<String>,
): List<ExtractionDraft> {
    return lines.asSequence()
        .mapNotNull { line ->
            if (!isActionableLine(line.cleaned, line.normalized, documentType, fromProvider = false)) return@mapNotNull null
            if (!reserved.add(line.normalized)) return@mapNotNull null
            val score = taskScore(line.cleaned, line.normalized, documentType, fromProvider = false)
            val confidence = when {
                score >= 0.86f -> 0.92f
                score >= 0.74f -> 0.8f
                else -> 0.64f
            }
            ExtractionDraft(
                title = deriveCandidateTitle(line.cleaned),
                body = line.cleaned,
                confidence = confidence,
                dueAt = extractDueAtFromText(line.cleaned),
                kind = ExtractionKind.TASK,
                reasoning = taskReasoning(line.cleaned, line.normalized, documentType, fromProvider = false),
                score = score,
            )
        }
        .sortedByDescending { it.score }
        .take(5)
        .toList()
}

private fun extractFactDrafts(
    lines: List<MergedLine>,
    documentType: DocumentType,
    reserved: MutableSet<String>,
): List<ExtractionDraft> {
    return lines.asSequence()
        .filterNot { it.normalized in reserved }
        .filter { looksLikeUsefulFact(it.cleaned, it.normalized, documentType) }
        .map { line ->
            val score = 0.58f
            ExtractionDraft(
                title = deriveCandidateTitle(line.cleaned),
                body = line.cleaned,
                confidence = score,
                dueAt = null,
                kind = ExtractionKind.FACT,
                reasoning = "fact signal: retained as searchable context, not an action",
                score = score,
            )
        }
        .sortedByDescending { it.score }
        .take(4)
        .toList()
}

private fun extractionPriority(kind: ExtractionKind): Int {
    return when (kind) {
        ExtractionKind.TASK -> 4
        ExtractionKind.DATE -> 3
        ExtractionKind.REFERENCE -> 2
        ExtractionKind.FACT -> 1
    }
}

private fun buildRawCandidateLines(ocrText: String, localLines: List<String>): List<String> {
    val fromText = ocrText
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()
    return (fromText + localLines)
        .flatMap { entry ->
            entry.split(Regex("[\\n•]+"))
                .map(String::trim)
                .filter(String::isNotBlank)
        }
}

private fun cleanCandidateLine(value: String): String {
    return value
        .replace(Regex("\\s+"), " ")
        .replace(Regex("^[\\-*•\\d.)\\s]+"), "")
        .trim()
}

private fun normalizeLine(value: String): String {
    return value.lowercase()
        .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun isUsefulCandidateLine(cleaned: String, normalized: String): Boolean {
    if (normalized.length < 4) return false
    if (normalized.all { it.isDigit() || it == ' ' }) return false
    if (looksLikeMetaOrCodeLine(cleaned, normalized)) return false
    val alphaNumericCount = cleaned.count { it.isLetterOrDigit() }
    if (alphaNumericCount < 4) return false
    val uniqueAlphaNumeric = cleaned.filter { it.isLetterOrDigit() }.lowercase().toSet().size
    if (uniqueAlphaNumeric <= 1) return false
    return true
}

private fun isReferenceLike(cleaned: String, normalized: String): Boolean {
    val lowered = cleaned.lowercase()
    return Regex("""\b(reference|ref|invoice|order|tracking|booking|confirmation|ticket|number|nr|code|id)\b""")
        .containsMatchIn(lowered) ||
        Regex("""\b[A-Z0-9]{3,}(-[A-Z0-9]{2,})+\b""").containsMatchIn(cleaned) ||
        Regex("""\b\d{4,}[- ]\d{3,}[- ]\d{2,}\b""").containsMatchIn(cleaned) ||
        (normalized.split(' ').size <= 6 && cleaned.count(Char::isDigit) >= 4 && cleaned.any { it == '-' || it == '/' })
}

private fun isActionableLine(
    cleaned: String,
    normalized: String,
    documentType: DocumentType,
    fromProvider: Boolean,
): Boolean {
    val words = normalized.split(' ').filter { it.isNotBlank() }
    val hasActionHint = ActionHints.any { Regex("""\b$it\b""").containsMatchIn(normalized) }
    val startsWithVerbLikeToken = words.firstOrNull() in VerbLikeLeadingTokens
    val hasCheckboxOrDirective = cleaned.startsWith("[") ||
        cleaned.startsWith("(") ||
        cleaned.startsWith("todo", ignoreCase = true) ||
        cleaned.startsWith("fix", ignoreCase = true) ||
        cleaned.startsWith("check", ignoreCase = true) ||
        cleaned.startsWith("review", ignoreCase = true)
    val looksLikeStatement = normalized.startsWith("the ") ||
        normalized.startsWith("this ") ||
        normalized.startsWith("that ") ||
        normalized.startsWith("wir ") ||
        normalized.startsWith("meanwhile ")
    val noteLikeDocument = documentType in setOf(DocumentType.NOTE, DocumentType.WHITEBOARD, DocumentType.SCREEN)
    val allowedByDocType = when (documentType) {
        DocumentType.RECEIPT -> false
        DocumentType.LETTER -> startsWithVerbLikeToken || hasCheckboxOrDirective || (fromProvider && hasActionHint)
        DocumentType.SCENE -> false
        else -> true
    }

    if (!allowedByDocType) return false
    if (looksLikeStatement) return false
    if (cleaned.length !in 8..120) return false
    if (words.size !in 2..16) return false
    if (cleaned.count(Char::isDigit) > cleaned.count(Char::isLetter) && !hasActionHint) return false

    return hasActionHint || startsWithVerbLikeToken || hasCheckboxOrDirective || (fromProvider && noteLikeDocument)
}

private fun taskScore(
    cleaned: String,
    normalized: String,
    documentType: DocumentType,
    fromProvider: Boolean,
): Float {
    var score = 0.52f
    if (fromProvider) score += 0.1f
    if (ActionHints.any { Regex("""\b$it\b""").containsMatchIn(normalized) }) score += 0.16f
    if (normalized.split(' ').firstOrNull() in VerbLikeLeadingTokens) score += 0.1f
    if (documentType in setOf(DocumentType.NOTE, DocumentType.WHITEBOARD, DocumentType.SCREEN)) score += 0.1f
    if (extractDueAtFromText(cleaned) != null) score += 0.06f
    return score.coerceIn(0.5f, 0.94f)
}

private fun taskReasoning(
    cleaned: String,
    normalized: String,
    documentType: DocumentType,
    fromProvider: Boolean,
): String {
    val reasons = mutableListOf<String>()
    if (fromProvider) reasons += "provider rewrite"
    if (ActionHints.any { Regex("""\b$it\b""").containsMatchIn(normalized) }) reasons += "action phrase"
    if (normalized.split(' ').firstOrNull() in VerbLikeLeadingTokens) reasons += "leading verb"
    if (documentType in setOf(DocumentType.NOTE, DocumentType.WHITEBOARD, DocumentType.SCREEN)) reasons += "task-friendly document"
    if (extractDueAtFromText(cleaned) != null) reasons += "timing clue"
    return "task signal: " + reasons.ifEmpty { listOf("manual review needed") }.joinToString(", ")
}

private fun looksLikeUsefulFact(
    cleaned: String,
    normalized: String,
    documentType: DocumentType,
): Boolean {
    if (normalized.length < 10) return false
    if (documentType == DocumentType.SCENE) return false
    if (isReferenceLike(cleaned, normalized)) return false
    if (extractDueAtFromText(cleaned) != null) return false
    if (isActionableLine(cleaned, normalized, documentType, fromProvider = false)) return false
    return normalized.split(' ').size in 3..18
}

private fun deriveCandidateTitle(cleaned: String): String {
    val compact = cleaned.trim().removeSuffix(".")
    if (compact.length <= 64) return compact
    val words = compact.split(' ')
    return buildString {
        for (word in words) {
            if (isNotEmpty() && length + word.length + 1 > 64) break
            if (isNotEmpty()) append(' ')
            append(word)
        }
    }.ifBlank { compact.take(64) }
}

private fun parseDateCandidate(value: String, pattern: String): Long? {
    val locales = listOf(Locale.ENGLISH, Locale.GERMAN)
    return locales.firstNotNullOfOrNull { locale ->
        val parser = SimpleDateFormat(pattern, locale).apply { isLenient = false }
        runCatching {
            parser.parse(value)?.let { parsed ->
                Calendar.getInstance().apply {
                    time = parsed
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
        }.getOrNull()
    }
}

private fun looksLikeMetaOrCodeLine(cleaned: String, normalized: String): Boolean {
    val lowered = cleaned.lowercase()
    if (MetaLinePhrases.any { it in lowered }) return true
    if (Regex("""\b[a-z0-9_]+\.(kt|java|xml|gradle|md|json)\b""").containsMatchIn(lowered)) return true
    if (Regex("""(:app:|testdebugunittest|assembledebug|gradlew|repositoryimpl|viewmodel|build successful|selectobject|adb exec-out)""").containsMatchIn(lowered)) return true
    if (Regex("""[\\/].+\.[a-z]{2,6}\b""").containsMatchIn(cleaned)) return true
    if (normalized.count { it == ' ' } <= 1 && normalized.any(Char::isDigit)) return true
    return false
}

private fun looksLikeProviderMetaTask(cleaned: String, reasoning: String): Boolean {
    val combined = "${cleaned.lowercase()} ${reasoning.lowercase()}"
    return Regex("""\b(test|testing|build|gradle|compile|commit|push|branch|repo|repository|apk|adb|debug|screenshot|import image|open settings|device)\b""")
        .containsMatchIn(combined)
}

private val ActionHints = setOf(
    "add",
    "book",
    "buy",
    "call",
    "check",
    "email",
    "fix",
    "follow",
    "need",
    "pay",
    "plan",
    "review",
    "schedule",
    "send",
    "set",
    "todo",
    "update",
    "anrufen",
    "bezahlen",
    "kaufen",
    "planen",
    "prüfen",
    "prufen",
)

private val VerbLikeLeadingTokens = setOf(
    "add",
    "book",
    "buy",
    "call",
    "check",
    "email",
    "fix",
    "follow",
    "pay",
    "plan",
    "review",
    "schedule",
    "send",
    "set",
    "update",
    "anrufen",
    "bezahlen",
    "kaufen",
    "planen",
    "prüfen",
    "prufen",
)

private val MetaLinePhrases = setOf(
    "nächster schritt",
    "nächster sinnvoller schritt",
    "bearbeitet",
    "ausgeführt",
    "validated with",
    "build successful",
    "import image",
    "mission control",
    "sidequest app",
)
