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
    val results by viewModel.results.collectAsStateWithLifecycle()
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
            }
        }
        items(results, key = { it.id }) { result ->
            ScaffoldCard(title = result.title, subtitle = "${result.matchLabel} | ${result.sourceLabel} | ${result.captureId.take(6)}") {
                Text(result.snippet)
                Button(onClick = { onOpenCapture(result.captureId) }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Open capture")
                }
            }
        }
    }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {
    private val _results = MutableStateFlow<List<SearchResultModel>>(emptyList())
    val results: StateFlow<List<SearchResultModel>> = _results

    fun search(query: String) {
        viewModelScope.launch {
            _results.value = if (query.isBlank()) emptyList() else searchRepository.search(query)
        }
    }
}
