package com.astrogolem.sidequest.feature.lobby

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.NoteDetail
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.data.repo.NotesRepository
import com.astrogolem.sidequest.core.ui.components.DestructiveOutlinedButton
import com.astrogolem.sidequest.core.ui.components.EmptyStateCard
import com.astrogolem.sidequest.core.ui.components.InlineSupportText
import com.astrogolem.sidequest.core.ui.components.LoadingStateCard
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.CardStroke
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    val noteLoaded by viewModel.noteLoaded.collectAsStateWithLifecycle()
    val openMissionId by viewModel.openMissionId.collectAsStateWithLifecycle()
    val sourceCaptureId = note?.sourceLabel?.let(::extractAutoNoteCaptureId)
    var previewMode by remember(note?.id) { mutableStateOf(false) }
    var showDangerZone by rememberSaveable(note?.id) { mutableStateOf(false) }
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
        if (!noteLoaded) {
            item {
                LoadingStateCard(
                    title = stringResource(R.string.note_loading_title),
                    subtitle = stringResource(R.string.note_loading_subtitle),
                )
            }
        } else if (currentNote == null) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.note_missing_title),
                    subtitle = stringResource(R.string.note_missing_subtitle),
                )
            }
        } else {
            item {
                ScaffoldCard(
                    title = currentNote.title,
                    subtitle = stringResource(
                        R.string.note_updated_format,
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(currentNote.updatedAt)),
                    ),
                ) {
                    Text(
                        text = when {
                            currentNote.imported -> stringResource(
                                R.string.note_status_imported_format,
                                currentNote.sourceLabel.orEmpty(),
                            )
                            sourceCaptureId != null -> stringResource(R.string.note_status_capture_linked)
                            else -> stringResource(R.string.note_status_local)
                        },
                        color = AccentPrimary,
                    )
                    if (sourceCaptureId != null) {
                        OutlinedButton(
                            onClick = { onOpenCapture(sourceCaptureId) },
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            Text(stringResource(R.string.note_open_capture))
                        }
                    }
                }
            }
            item {
                ScaffoldCard(
                    title = stringResource(R.string.note_editor_title),
                    subtitle = stringResource(R.string.note_editor_subtitle),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = !previewMode,
                            onClick = { previewMode = false },
                            label = { Text(stringResource(R.string.note_mode_edit)) },
                        )
                        FilterChip(
                            selected = previewMode,
                            onClick = { previewMode = true },
                            label = { Text(stringResource(R.string.note_mode_preview)) },
                        )
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.note_field_title)) },
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
                            label = { Text(stringResource(R.string.note_field_markdown)) },
                            shape = noteFieldShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            minLines = 14,
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.save(
                                currentNote.copy(
                                    title = title.trim().ifBlank { currentNote.title },
                                    markdown = markdown,
                                ),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Text(stringResource(R.string.note_save_action))
                    }
                }
            }
            item {
                ScaffoldCard(
                    title = stringResource(R.string.note_handoff_title),
                    subtitle = stringResource(R.string.note_handoff_subtitle),
                ) {
                    InlineSupportText(stringResource(R.string.note_handoff_helper))
                    OutlinedButton(
                        onClick = viewModel::createQuest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Text(stringResource(R.string.note_create_quest))
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showDangerZone = !showDangerZone },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (showDangerZone) {
                                R.string.note_danger_hide
                            } else {
                                R.string.note_danger_show
                            },
                        ),
                    )
                }
            }
            if (showDangerZone) {
                item {
                    ScaffoldCard(
                        title = stringResource(R.string.note_danger_title),
                        subtitle = stringResource(R.string.note_danger_subtitle),
                    ) {
                        DestructiveOutlinedButton(
                            label = stringResource(R.string.note_delete_action),
                            onClick = viewModel::delete,
                            modifier = Modifier.fillMaxWidth(),
                        )
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
        title = stringResource(R.string.note_preview_title),
        subtitle = stringResource(R.string.note_preview_subtitle),
        modifier = modifier.fillMaxWidth(),
    ) {
        val lines = markdown.ifBlank { stringResource(R.string.note_preview_empty) }.lines()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            var inCodeBlock = false
            var index = 0
            while (index < lines.size) {
                val rawLine = lines[index]
                val line = rawLine.trimEnd()
                val tableBlock = parseMarkdownTable(lines, index)
                when {
                    line.startsWith("```") -> {
                        inCodeBlock = !inCodeBlock
                        Text(
                            if (inCodeBlock) {
                                stringResource(R.string.note_preview_code_start)
                            } else {
                                stringResource(R.string.note_preview_code_end)
                            },
                            color = AccentPrimary,
                        )
                        index += 1
                    }
                    inCodeBlock -> {
                        Text(line, fontWeight = FontWeight.Medium)
                        index += 1
                    }
                    tableBlock != null -> {
                        MarkdownTable(tableBlock)
                        index += tableBlock.lineCount
                    }
                    line.startsWith("# ") -> {
                        Text(line.removePrefix("# ").trim(), fontWeight = FontWeight.Bold)
                        index += 1
                    }
                    line.startsWith("## ") -> {
                        Text(line.removePrefix("## ").trim(), fontWeight = FontWeight.SemiBold)
                        index += 1
                    }
                    line.startsWith("- ") || line.startsWith("* ") -> {
                        Text("- ${line.drop(2).trim()}")
                        index += 1
                    }
                    line.startsWith("> ") -> {
                        Text(line.removePrefix("> ").trim(), color = TextSecondary, fontStyle = FontStyle.Italic)
                        index += 1
                    }
                    line.isBlank() -> {
                        Text("")
                        index += 1
                    }
                    else -> {
                        Text(line)
                        index += 1
                    }
                }
            }
        }
    }
}

private data class MarkdownTableBlock(
    val headers: List<String>,
    val rows: List<List<String>>,
    val lineCount: Int,
)

private fun parseMarkdownTable(lines: List<String>, startIndex: Int): MarkdownTableBlock? {
    if (startIndex + 1 >= lines.size) return null
    val headerLine = lines[startIndex].trim()
    val separatorLine = lines[startIndex + 1].trim()
    if (!headerLine.contains('|') || !separatorLine.contains('|')) return null

    val headers = splitMarkdownTableRow(headerLine)
    val separatorCells = splitMarkdownTableRow(separatorLine)
    if (headers.isEmpty() || headers.size != separatorCells.size) return null
    if (!separatorCells.all { it.matches(Regex("""^:?-{3,}:?$""")) }) return null

    val rows = mutableListOf<List<String>>()
    var index = startIndex + 2
    while (index < lines.size) {
        val line = lines[index].trim()
        if (!line.contains('|')) break
        val cells = splitMarkdownTableRow(line)
        if (cells.isEmpty()) break
        rows += cells
        index += 1
    }

    return MarkdownTableBlock(
        headers = headers,
        rows = rows,
        lineCount = index - startIndex,
    )
}

private fun splitMarkdownTableRow(line: String): List<String> {
    return line
        .trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map { it.trim() }
}

@Composable
private fun MarkdownTable(
    table: MarkdownTableBlock,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CardStroke.copy(alpha = 0.7f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MarkdownTableRow(cells = table.headers, header = true)
            table.rows.forEach { row ->
                MarkdownTableRow(cells = row, header = false)
            }
        }
    }
}

@Composable
private fun MarkdownTableRow(
    cells: List<String>,
    header: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        cells.forEach { cell ->
            Text(
                text = cell.ifBlank { "-" },
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                color = if (header) AccentPrimary else TextSecondary,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
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
    val noteLoaded: StateFlow<Boolean> =
        notesRepository.observeNote(noteId)
            .map { true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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
