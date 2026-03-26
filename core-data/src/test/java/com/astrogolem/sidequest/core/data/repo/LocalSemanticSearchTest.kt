package com.astrogolem.sidequest.core.data.repo

import com.astrogolem.sidequest.core.data.model.SearchFilter
import com.astrogolem.sidequest.core.data.model.SearchResultType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSemanticSearchTest {

    @Test
    fun `semantic query can recover a date without exact phrasing`() {
        val results = semanticSearch(
            query = "passport expiry",
            filter = SearchFilter.ALL,
            documents = listOf(
                SemanticMemoryDocument(
                    id = "date-1",
                    title = "ETA valid until 15 March 2028",
                    body = "Travel authorization valid until 15 March 2028.",
                    resultType = SearchResultType.DATE,
                    sourceLabel = "intel",
                    captureId = "capture-1",
                    qualityWeight = 0.08f,
                ),
                SemanticMemoryDocument(
                    id = "fact-1",
                    title = "Coffee beans",
                    body = "Buy coffee beans",
                    resultType = SearchResultType.TASK,
                    sourceLabel = "candidate",
                    captureId = "capture-2",
                ),
            ),
        )

        assertTrue(results.isNotEmpty())
        assertEquals(SearchResultType.DATE, results.first().resultType)
        assertEquals("date-1", results.first().id)
    }

    @Test
    fun `task filter excludes notes and facts`() {
        val results = semanticSearch(
            query = "buy coffee",
            filter = SearchFilter.TASKS,
            documents = listOf(
                SemanticMemoryDocument(
                    id = "task-1",
                    title = "Buy coffee beans",
                    body = "Need to restock",
                    resultType = SearchResultType.TASK,
                    sourceLabel = "quest",
                    missionId = "mission-1",
                ),
                SemanticMemoryDocument(
                    id = "note-1",
                    title = "Coffee journal",
                    body = "Markdown tasting notes",
                    resultType = SearchResultType.NOTE,
                    sourceLabel = "note",
                    noteId = "note-1",
                ),
            ),
        )

        assertEquals(1, results.size)
        assertEquals("task-1", results.single().id)
    }

    @Test
    fun `notes remain searchable through semantic aliases`() {
        val results = semanticSearch(
            query = "launch markdown document",
            filter = SearchFilter.NOTES,
            documents = listOf(
                SemanticMemoryDocument(
                    id = "note-42",
                    title = "Launch Checklist",
                    body = "# Launch\nShip notes for release",
                    resultType = SearchResultType.NOTE,
                    sourceLabel = "note",
                    noteId = "note-42",
                    qualityWeight = 0.07f,
                ),
            ),
        )

        assertEquals(1, results.size)
        assertEquals("note-42", results.first().id)
        assertEquals(SearchResultType.NOTE, results.first().resultType)
    }
}
