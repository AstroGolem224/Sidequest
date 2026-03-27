package com.astrogolem.sidequest.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.SearchFilter
import com.astrogolem.sidequest.core.data.model.SearchResultModel
import com.astrogolem.sidequest.core.data.model.SearchResultType
import com.astrogolem.sidequest.core.data.repo.SearchRepository
import com.astrogolem.sidequest.core.ui.components.EmptyStateCard
import com.astrogolem.sidequest.core.ui.components.ErrorStateCard
import com.astrogolem.sidequest.core.ui.components.GlassCard
import com.astrogolem.sidequest.core.ui.components.LoadingStateCard
import com.astrogolem.sidequest.core.ui.components.StateShellAction
import com.astrogolem.sidequest.core.ui.text.UiText
import com.astrogolem.sidequest.core.ui.text.resolve
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
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GlassCard(
                title = stringResource(R.string.search_title),
                subtitle = stringResource(R.string.search_subtitle),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_query_label)) },
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
                Button(
                    onClick = { viewModel.search(query) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(stringResource(R.string.search_action))
                }
                Text(
                    text = state.statusMessage.resolve(context),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        when {
            state.isLoading -> {
                item {
                    LoadingStateCard(
                        title = stringResource(R.string.search_loading_title),
                        subtitle = stringResource(R.string.search_loading_subtitle),
                    )
                }
            }

            state.errorMessage != null -> {
                item {
                    ErrorStateCard(
                        title = stringResource(R.string.search_error_title),
                        subtitle = state.errorMessage?.resolve(context) ?: stringResource(R.string.search_error_subtitle),
                        supportingLines = listOf(stringResource(R.string.search_error_help)),
                        primaryAction = StateShellAction(
                            label = stringResource(R.string.search_retry_action),
                            onClick = {
                                if (state.lastQuery.isNotBlank()) {
                                    viewModel.search(state.lastQuery)
                                }
                            },
                        ),
                        secondaryAction = StateShellAction(
                            label = stringResource(R.string.search_clear_action),
                            onClick = {
                                query = ""
                                viewModel.clearSearch()
                            },
                        ),
                    )
                }
            }

            state.results.isEmpty() -> {
                item {
                    EmptyStateCard(
                        title = stringResource(
                            if (state.hasSearched) {
                                R.string.search_empty_title
                            } else {
                                R.string.search_idle_title
                            },
                        ),
                        subtitle = stringResource(
                            if (state.hasSearched) {
                                R.string.search_empty_subtitle
                            } else {
                                R.string.search_idle_subtitle
                            },
                        ),
                        supportingLines = listOf(
                            if (state.hasSearched) {
                                stringResource(R.string.search_empty_detail_format, state.lastQuery)
                            } else {
                                stringResource(R.string.search_idle_detail)
                            },
                        ),
                        primaryAction = StateShellAction(
                            label = stringResource(
                                if (state.hasSearched) {
                                    R.string.search_retry_action
                                } else {
                                    R.string.search_examples_action
                                },
                            ),
                            onClick = {
                                if (state.hasSearched) {
                                    viewModel.search(state.lastQuery)
                                } else {
                                    query = "passport expiry"
                                    viewModel.search(query)
                                }
                            },
                        ),
                        secondaryAction = if (state.hasSearched) {
                            StateShellAction(
                                label = stringResource(R.string.search_clear_action),
                                onClick = {
                                    query = ""
                                    viewModel.clearSearch()
                                },
                            )
                        } else {
                            null
                        },
                    )
                }
            }

            else -> {
                item {
                    Text(
                        text = stringResource(R.string.search_results_heading),
                        modifier = Modifier.semantics { heading() },
                    )
                }
                items(state.results, key = { it.id }) { result ->
                    GlassCard(
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
                                        Text(stringResource(R.string.search_open_quest))
                                    }
                                    captureId?.let { sourceCaptureId ->
                                        OutlinedButton(onClick = { onOpenCapture(sourceCaptureId) }) {
                                            Text(stringResource(R.string.search_open_source))
                                        }
                                    }
                                }

                                noteId != null -> {
                                    Button(onClick = { onOpenNote(noteId) }) {
                                        Text(stringResource(R.string.search_open_note))
                                    }
                                }

                                captureId != null -> {
                                    Button(onClick = { onOpenCapture(captureId) }) {
                                        Text(
                                            stringResource(
                                                if (result.resultType == SearchResultType.TASK) {
                                                    R.string.search_recover_from_scan
                                                } else {
                                                    R.string.search_open_capture
                                                },
                                            ),
                                        )
                                    }
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
    val statusMessage: UiText = UiText.resource(R.string.search_status_initial),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(
        SearchUiState(
            statusMessage = UiText.resource(R.string.search_status_initial),
        ),
    )
    val state: StateFlow<SearchUiState> = _state

    fun setFilter(filter: SearchFilter, query: String) {
        _state.value = _state.value.copy(filter = filter, errorMessage = null)
        if (query.isNotBlank()) {
            search(query)
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            val trimmed = query.trim()
            if (trimmed.isBlank()) {
                clearSearch()
            } else {
                val filter = _state.value.filter
                _state.value = _state.value.copy(
                    lastQuery = trimmed,
                    hasSearched = true,
                    isLoading = true,
                    errorMessage = null,
                    statusMessage = UiText.resource(R.string.search_status_loading),
                )
                runCatching {
                    searchRepository.search(trimmed, filter)
                }.fold(
                    onSuccess = { results ->
                        _state.value = SearchUiState(
                            results = results,
                            lastQuery = trimmed,
                            hasSearched = true,
                            filter = filter,
                            statusMessage = if (results.isEmpty()) {
                                UiText.resource(R.string.search_status_empty_format, filter.label().lowercase(), trimmed)
                            } else {
                                UiText.resource(R.string.search_status_results_format, results.size, filter.label().lowercase(), trimmed)
                            },
                        )
                    },
                    onFailure = { error ->
                        _state.value = _state.value.copy(
                            results = emptyList(),
                            isLoading = false,
                            errorMessage = if (error.message.isNullOrBlank()) {
                                UiText.resource(R.string.search_error_subtitle)
                            } else {
                                UiText.Dynamic(error.message.orEmpty())
                            },
                            statusMessage = UiText.resource(R.string.search_status_failed),
                        )
                    },
                )
            }
        }
    }

    fun clearSearch() {
        _state.value = SearchUiState(
            results = emptyList(),
            lastQuery = "",
            hasSearched = false,
            filter = _state.value.filter,
            statusMessage = UiText.resource(R.string.search_status_prompt),
        )
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
