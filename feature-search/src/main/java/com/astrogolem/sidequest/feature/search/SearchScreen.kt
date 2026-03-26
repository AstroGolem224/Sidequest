package com.astrogolem.sidequest.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.SearchFilter
import com.astrogolem.sidequest.core.data.model.SearchResultModel
import com.astrogolem.sidequest.core.data.model.SearchResultType
import com.astrogolem.sidequest.core.data.repo.SearchRepository
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchRoute(
    onOpenCapture: (String) -> Unit,
    onOpenMission: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScaffoldCard(title = "Memory Search", subtitle = "Local semantic search across scans, notes, quests, dates, references, and facts.") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search query") },
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SearchFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter, query) },
                            label = { Text(filter.label()) },
                        )
                    }
                }
                Button(onClick = { viewModel.search(query) }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Search")
                }
                Text(
                    text = state.statusMessage,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        if (state.results.isEmpty()) {
            item {
                ScaffoldCard(
                    title = if (state.hasSearched) "No matches found" else "Search is idle",
                    subtitle = if (state.hasSearched) {
                        "Try a synonym, switch the filter, or search by date, reference, note heading, or quest title."
                    } else {
                        "Search local memory by intent instead of exact OCR wording."
                    },
                ) {
                    Text(
                        if (state.hasSearched) {
                            "Nothing matched `${state.lastQuery}` in the local archive."
                        } else {
                            "Examples: passport expiry, buy coffee, invoice id, garden routine, launch note."
                        },
                    )
                }
            }
        } else {
            items(state.results, key = { it.id }) { result ->
                ScaffoldCard(
                    title = result.title,
                    subtitle = "${result.matchLabel} | ${result.resultType.label()} | ${result.sourceLabel}",
                ) {
                    Text(result.snippet)
                    FlowRow(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val missionId = result.missionId
                        val noteId = result.noteId
                        val captureId = result.captureId
                        when {
                            missionId != null -> {
                                Button(onClick = { onOpenMission(missionId) }) {
                                    Text("Open quest")
                                }
                                captureId?.let { sourceCaptureId ->
                                    OutlinedButton(onClick = { onOpenCapture(sourceCaptureId) }) {
                                        Text("Source scan")
                                    }
                                }
                            }

                            noteId != null -> {
                                Button(onClick = { onOpenNote(noteId) }) {
                                    Text("Open note")
                                }
                            }

                            captureId != null -> {
                                Button(onClick = { onOpenCapture(captureId) }) {
                                    Text(if (result.resultType == SearchResultType.TASK) "Recover from scan" else "Open capture")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SearchUiState(
    val results: List<SearchResultModel> = emptyList(),
    val lastQuery: String = "",
    val hasSearched: Boolean = false,
    val filter: SearchFilter = SearchFilter.ALL,
    val statusMessage: String = "Search the local archive.",
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    fun setFilter(filter: SearchFilter, query: String) {
        _state.value = _state.value.copy(filter = filter)
        if (query.isNotBlank()) {
            search(query)
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            val trimmed = query.trim()
            if (trimmed.isBlank()) {
                _state.value = SearchUiState(
                    results = emptyList(),
                    lastQuery = "",
                    hasSearched = false,
                    filter = _state.value.filter,
                    statusMessage = "Enter a query to search scans, dates, references, quests, and notes.",
                )
            } else {
                val filter = _state.value.filter
                val results = searchRepository.search(trimmed, filter)
                _state.value = SearchUiState(
                    results = results,
                    lastQuery = trimmed,
                    hasSearched = true,
                    filter = filter,
                    statusMessage = if (results.isEmpty()) {
                        "No ${filter.label().lowercase()} matches for `$trimmed`."
                    } else {
                        "${results.size} ${filter.label().lowercase()} matches for `$trimmed`."
                    },
                )
            }
        }
    }
}

private fun SearchFilter.label(): String {
    return when (this) {
        SearchFilter.ALL -> "All"
        SearchFilter.TASKS -> "Tasks"
        SearchFilter.DATES -> "Dates"
        SearchFilter.REFERENCES -> "References"
        SearchFilter.FACTS -> "Facts"
        SearchFilter.NOTES -> "Notes"
        SearchFilter.SCANS -> "Scans"
    }
}

private fun SearchResultType.label(): String {
    return when (this) {
        SearchResultType.TASK -> "Task"
        SearchResultType.DATE -> "Date"
        SearchResultType.REFERENCE -> "Reference"
        SearchResultType.FACT -> "Fact"
        SearchResultType.NOTE -> "Note"
        SearchResultType.SCAN -> "Scan"
    }
}
