package com.astrogolem.sidequest.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.astrogolem.sidequest.core.data.model.SearchResultModel
import com.astrogolem.sidequest.core.data.repo.SearchRepository
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun SearchRoute(
    onOpenCapture: (String) -> Unit,
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
            ScaffoldCard(title = "Memory Search", subtitle = "FTS search over OCR text and normalized knowledge nodes.") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search query") },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                        "Try a shorter phrase, a date, or a reference value from the source document."
                    } else {
                        "Search scans, OCR text, facts, dates, and references once they have been processed."
                    },
                ) {
                    Text(
                        if (state.hasSearched) {
                            "Nothing matched `${state.lastQuery}` in the local archive."
                        } else {
                            "Examples: invoice number, due date, receipt total, passport reference, whiteboard topic."
                        },
                    )
                }
            }
        } else {
            items(state.results, key = { it.id }) { result ->
                ScaffoldCard(title = result.title, subtitle = "${result.matchLabel} | ${result.sourceLabel} | ${result.captureId.take(6)}") {
                    Text(result.snippet)
                    Button(onClick = { onOpenCapture(result.captureId) }, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Open capture")
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
    val statusMessage: String = "Search the local archive.",
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    fun search(query: String) {
        viewModelScope.launch {
            val trimmed = query.trim()
            if (trimmed.isBlank()) {
                _state.value = SearchUiState(
                    results = emptyList(),
                    lastQuery = "",
                    hasSearched = false,
                    statusMessage = "Enter a query to search scans, dates, references, and facts.",
                )
            } else {
                val results = searchRepository.search(trimmed)
                _state.value = SearchUiState(
                    results = results,
                    lastQuery = trimmed,
                    hasSearched = true,
                    statusMessage = if (results.isEmpty()) {
                        "No local matches for `$trimmed`."
                    } else {
                        "${results.size} matches found for `$trimmed`."
                    },
                )
            }
        }
    }
}
