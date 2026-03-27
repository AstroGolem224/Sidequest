package com.astrogolem.sidequest.feature.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.NoteDetail
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.data.repo.NotesRepository
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun NoteDetailRoute(
    onOpenMission: (String) -> Unit,
    onOpenCapture: (String) -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel(),
) {
    val noteFieldShape = RoundedCornerShape(24.dp)
    val note by viewModel.note.collectAsStateWithLifecycle()
    val openMissionId by viewModel.openMissionId.collectAsStateWithLifecycle()
    val sourceCaptureId = note?.sourceLabel?.let(::extractAutoNoteCaptureId)
    var previewMode by remember(note?.id) { mutableStateOf(false) }
    var title by remember(note?.id) { mutableStateOf("") }
    var markdown by remember(note?.id) { mutableStateOf("") }

    LaunchedEffect(openMissionId) {
        openMissionId?.let { missionId ->
            onOpenMission(missionId)
            viewModel.consumeOpenMission()
        }
    }

    LaunchedEffect(note?.id, note?.title, note?.markdown) {
        title = note?.title.orEmpty()
        markdown = note?.markdown.orEmpty()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        val currentNote = note
        if (currentNote == null) {
            item {
                ScaffoldCard(
                    title = "Note missing",
                    subtitle = "This note was deleted or is no longer available on this device.",
                ) {}
            }
        } else {
            item {
                ScaffoldCard(
                    title = currentNote.title,
                    subtitle = "Updated ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(currentNote.updatedAt))}",
                ) {
                    Text(
                        text = when {
                            currentNote.imported -> "Imported markdown${currentNote.sourceLabel?.let { " • $it" }.orEmpty()}"
                            sourceCaptureId != null -> "Capture-linked markdown note"
                            else -> "Local markdown note"
                        },
                        color = AccentPrimary,
                    )
                    if (sourceCaptureId != null) {
                        OutlinedButton(
                            onClick = { onOpenCapture(sourceCaptureId) },
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            Text("Open Source Capture")
                        }
                    }
                }
            }
            item {
                ScaffoldCard(
                    title = "Note Editor",
                    subtitle = "Write markdown, then switch to preview to inspect the rendered structure.",
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = !previewMode,
                            onClick = { previewMode = false },
                            label = { Text("Edit") },
                        )
                        FilterChip(
                            selected = previewMode,
                            onClick = { previewMode = true },
                            label = { Text("Preview") },
                        )
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        shape = noteFieldShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                    if (previewMode) {
                        MarkdownPreviewCard(markdown = markdown, modifier = Modifier.padding(top = 12.dp))
                    } else {
                        OutlinedTextField(
                            value = markdown,
                            onValueChange = { markdown = it },
                            label = { Text("Markdown") },
                            shape = noteFieldShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            minLines = 14,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                viewModel.save(
                                    currentNote.copy(
                                        title = title.trim().ifBlank { currentNote.title },
                                        markdown = markdown,
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Save")
                        }
                        OutlinedButton(
                            onClick = viewModel::delete,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Delete")
                        }
                    }
                    Button(
                        onClick = viewModel::createQuest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                    ) {
                        Text("Create Quest")
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownPreviewCard(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    ScaffoldCard(
        title = "Preview",
        subtitle = "Simple markdown rendering for headings, bullets, quotes, and code fences.",
        modifier = modifier.fillMaxWidth(),
    ) {
        val lines = markdown.ifBlank { "_empty note_" }.lines()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            var inCodeBlock = false
            lines.forEach { rawLine ->
                val line = rawLine.trimEnd()
                when {
                    line.startsWith("```") -> {
                        inCodeBlock = !inCodeBlock
                        Text(if (inCodeBlock) "code block" else "end code block", color = AccentPrimary)
                    }
                    inCodeBlock -> Text(line, fontWeight = FontWeight.Medium)
                    line.startsWith("# ") -> Text(line.removePrefix("# ").trim(), fontWeight = FontWeight.Bold)
                    line.startsWith("## ") -> Text(line.removePrefix("## ").trim(), fontWeight = FontWeight.SemiBold)
                    line.startsWith("- ") || line.startsWith("* ") -> Text("• ${line.drop(2).trim()}")
                    line.startsWith("> ") -> Text(line.removePrefix("> ").trim(), color = TextSecondary)
                    line.isBlank() -> Text("")
                    else -> Text(line)
                }
            }
        }
    }
}

private fun extractAutoNoteCaptureId(sourceLabel: String?): String? {
    if (sourceLabel.isNullOrBlank()) return null
    return Regex("""^capture:([^:]+):auto-note$""")
        .find(sourceLabel)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { it.isNotBlank() }
}

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val notesRepository: NotesRepository,
    private val missionRepository: MissionRepository,
) : ViewModel() {
    private val noteId: String = checkNotNull(savedStateHandle["noteId"])
    private val _openMissionId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val openMissionId: StateFlow<String?> = _openMissionId

    val note: StateFlow<NoteDetail?> =
        notesRepository.observeNote(noteId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(note: NoteDetail) {
        viewModelScope.launch {
            notesRepository.saveNote(note)
        }
    }

    fun delete() {
        viewModelScope.launch {
            notesRepository.deleteNote(noteId)
        }
    }

    fun createQuest() {
        viewModelScope.launch {
            _openMissionId.value = missionRepository.createQuestFromNote(noteId)
        }
    }

    fun consumeOpenMission() {
        _openMissionId.value = null
    }
}
