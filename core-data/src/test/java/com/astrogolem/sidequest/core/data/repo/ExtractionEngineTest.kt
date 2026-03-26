package com.astrogolem.sidequest.core.data.repo

import com.astrogolem.sidequest.core.data.model.DocumentType
import com.astrogolem.sidequest.core.data.model.ExtractionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractionEngineTest {

    @Test
    fun `eta style document yields date and reference but no task`() {
        val drafts = buildExtractionDrafts(
            documentType = DocumentType.LETTER,
            ocrText = """
                Your ETA is valid until: 15 MARCH 2028
                ETA reference number: 2020-0000-4659
                included on this email. It only needs to
            """.trimIndent(),
            localLines = listOf(
                "Your ETA is valid until: 15 MARCH 2028",
                "ETA reference number: 2020-0000-4659",
            ),
            providerLines = emptyList(),
        )

        val debugKinds = drafts.joinToString(separator = " | ") { "${it.kind}:${it.body}" }
        assertTrue(debugKinds, drafts.any { it.kind == ExtractionKind.DATE && it.dueAt != null })
        assertTrue(debugKinds, drafts.any { it.kind == ExtractionKind.REFERENCE })
        assertFalse(debugKinds, drafts.any { it.kind == ExtractionKind.TASK })
    }

    @Test
    fun `todo note yields actionable task drafts with reasoning`() {
        val drafts = buildExtractionDrafts(
            documentType = DocumentType.NOTE,
            ocrText = """
                Todo
                Buy coffee beans
                Call dentist tomorrow
                Review launch checklist
            """.trimIndent(),
            localLines = listOf(
                "Buy coffee beans",
                "Call dentist tomorrow",
                "Review launch checklist",
            ),
            providerLines = emptyList(),
        )

        val tasks = drafts.filter { it.kind == ExtractionKind.TASK }
        assertEquals(3, tasks.size)
        assertTrue(tasks.all { it.reasoning.startsWith("task signal:") })
        assertTrue(tasks.any { it.dueAt != null })
    }

    @Test
    fun `receipt document does not invent task candidates`() {
        val drafts = buildExtractionDrafts(
            documentType = DocumentType.RECEIPT,
            ocrText = """
                RECEIPT
                Total 24.90
                Order number 8844-1122
                Card payment approved
            """.trimIndent(),
            localLines = listOf(
                "Total 24.90",
                "Order number 8844-1122",
                "Card payment approved",
            ),
            providerLines = emptyList(),
        )

        assertFalse(drafts.any { it.kind == ExtractionKind.TASK })
        assertTrue(drafts.any { it.kind == ExtractionKind.REFERENCE })
        assertTrue(drafts.any { it.kind == ExtractionKind.FACT })
    }

    @Test
    fun `classification recognizes receipt signals`() {
        val type = classifyDocumentType(
            """
                Receipt
                Total
                Tax
                Card payment
            """.trimIndent(),
        )

        assertEquals(DocumentType.RECEIPT, type)
    }

    @Test
    fun `date parsing handles explicit absolute dates`() {
        val dueAt = extractDueAtFromText("Valid until: 15 March 2028")

        assertNotNull(dueAt)
    }
}
