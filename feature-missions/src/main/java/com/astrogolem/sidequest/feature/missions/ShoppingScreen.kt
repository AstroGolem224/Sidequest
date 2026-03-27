package com.astrogolem.sidequest.feature.missions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.ShoppingListDetailModel
import com.astrogolem.sidequest.core.data.model.ShoppingListSummary
import com.astrogolem.sidequest.core.data.repo.ShoppingListRepository
import com.astrogolem.sidequest.core.ui.components.EmptyStateCard
import com.astrogolem.sidequest.core.ui.components.LoadingStateCard
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.AccentSecondary
import com.astrogolem.sidequest.core.ui.theme.BgPanel
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private enum class ShoppingCreationMode {
    Manual,
    Voice,
    Photo,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShoppingRoute(
    onOpenList: (String) -> Unit,
    viewModel: ShoppingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedMode by rememberSaveable { mutableStateOf<ShoppingCreationMode?>(null) }
    var manualTitle by remember { mutableStateOf("") }
    var manualItems by remember { mutableStateOf("") }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val transcript = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (transcript.isNotBlank()) {
                viewModel.createVoiceList(transcript)
            } else {
                viewModel.showFeedback("No speech result returned.")
            }
        }
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val pendingUri = state.pendingPhotoUri
        if (success && pendingUri != null) {
            viewModel.createPhotoList(Uri.parse(pendingUri))
        } else {
            viewModel.showFeedback("Photo capture cancelled.")
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createShoppingPhotoUri(context)
            viewModel.setPendingPhotoUri(uri.toString())
            photoLauncher.launch(uri)
        } else {
            viewModel.showFeedback("Camera permission is required for photo shopping lists.")
        }
    }

    LaunchedEffect(state.openListId) {
        state.openListId?.let { listId ->
            onOpenList(listId)
            viewModel.consumeOpenList()
        }
    }

    LaunchedEffect(state.feedback) {
        if (state.feedback != null) {
            delay(1800)
            viewModel.clearFeedback()
        }
    }

    val activeLists = state.lists.filterNot { it.archived }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                ShoppingCreationLauncherCard(
                    selectedMode = selectedMode,
                    onSelectMode = { mode -> selectedMode = mode },
                )
            }

            item {
                Text(
                    text = stringResource(R.string.shopping_active_lists_heading),
                    color = TextSecondary,
                )
            }
            if (activeLists.isEmpty()) {
                item {
                    ScaffoldCard(
                        title = stringResource(R.string.shopping_empty_title),
                        subtitle = if (state.lists.any { it.archived }) {
                            stringResource(R.string.shopping_empty_archived_subtitle)
                        } else {
                            stringResource(R.string.shopping_empty_subtitle)
                        },
                    ) {}
                }
            } else {
                items(activeLists, key = { it.id }) { list ->
                    ShoppingListSummaryCard(
                        summary = list,
                        onOpen = { onOpenList(list.id) },
                    )
                }
            }

            selectedMode?.let { mode ->
                item {
                    ShoppingCreationComposerCard(
                        mode = mode,
                        manualTitle = manualTitle,
                        manualItems = manualItems,
                        onManualTitleChange = { manualTitle = it },
                        onManualItemsChange = { manualItems = it },
                        onManualCreate = {
                            viewModel.createManualList(manualTitle, manualItems)
                            manualTitle = ""
                            manualItems = ""
                        },
                        onVoiceCreate = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(
                                    RecognizerIntent.EXTRA_PROMPT,
                                    context.getString(R.string.shopping_voice_prompt),
                                )
                            }
                            runCatching { voiceLauncher.launch(intent) }
                                .onFailure {
                                    viewModel.showFeedback(
                                        context.getString(R.string.shopping_voice_unavailable_feedback),
                                    )
                                }
                        },
                        onPhotoCreate = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                val uri = createShoppingPhotoUri(context)
                                viewModel.setPendingPhotoUri(uri.toString())
                                photoLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.feedback != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
        ) {
            state.feedback?.let { message ->
                Surface(color = BgPanel.copy(alpha = 0.96f)) {
                    Text(
                        text = message,
                        color = AccentSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShoppingCreationLauncherCard(
    selectedMode: ShoppingCreationMode?,
    onSelectMode: (ShoppingCreationMode) -> Unit,
) {
    ScaffoldCard(
        title = stringResource(R.string.shopping_screen_title),
        subtitle = stringResource(R.string.shopping_screen_subtitle),
    ) {
        Text(
            text = stringResource(R.string.shopping_launcher_helper),
            color = TextSecondary,
        )
        FlowRow(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShoppingCreationMode.entries.forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onSelectMode(mode) },
                    label = {
                        Text(
                            text = stringResource(
                                when (mode) {
                                    ShoppingCreationMode.Manual -> R.string.shopping_mode_manual
                                    ShoppingCreationMode.Voice -> R.string.shopping_mode_voice
                                    ShoppingCreationMode.Photo -> R.string.shopping_mode_photo
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ShoppingCreationComposerCard(
    mode: ShoppingCreationMode,
    manualTitle: String,
    manualItems: String,
    onManualTitleChange: (String) -> Unit,
    onManualItemsChange: (String) -> Unit,
    onManualCreate: () -> Unit,
    onVoiceCreate: () -> Unit,
    onPhotoCreate: () -> Unit,
) {
    ScaffoldCard(
        title = stringResource(
            when (mode) {
                ShoppingCreationMode.Manual -> R.string.shopping_manual_title
                ShoppingCreationMode.Voice -> R.string.shopping_voice_title
                ShoppingCreationMode.Photo -> R.string.shopping_photo_title
            },
        ),
        subtitle = stringResource(
            when (mode) {
                ShoppingCreationMode.Manual -> R.string.shopping_manual_subtitle
                ShoppingCreationMode.Voice -> R.string.shopping_voice_subtitle
                ShoppingCreationMode.Photo -> R.string.shopping_photo_subtitle
            },
        ),
    ) {
        when (mode) {
            ShoppingCreationMode.Manual -> {
                OutlinedTextField(
                    value = manualTitle,
                    onValueChange = onManualTitleChange,
                    label = { Text(stringResource(R.string.shopping_manual_optional_title)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = manualItems,
                    onValueChange = onManualItemsChange,
                    label = { Text(stringResource(R.string.shopping_manual_items_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    minLines = 4,
                )
                Button(
                    onClick = onManualCreate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                ) {
                    Text(stringResource(R.string.shopping_manual_action))
                }
            }

            ShoppingCreationMode.Voice -> {
                Text(
                    text = stringResource(R.string.shopping_voice_helper),
                    color = TextSecondary,
                )
                Button(
                    onClick = onVoiceCreate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                ) {
                    Text(stringResource(R.string.shopping_voice_action))
                }
            }

            ShoppingCreationMode.Photo -> {
                Text(
                    text = stringResource(R.string.shopping_photo_helper),
                    color = TextSecondary,
                )
                Button(
                    onClick = onPhotoCreate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                ) {
                    Text(stringResource(R.string.shopping_photo_action))
                }
            }
        }
    }
}

@Composable
fun ShoppingDetailRoute(
    onOpenCapture: (String) -> Unit,
    onOpenMission: (String) -> Unit,
    viewModel: ShoppingDetailViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val detailLoaded by viewModel.detailLoaded.collectAsStateWithLifecycle()
    val openMissionId by viewModel.openMissionId.collectAsStateWithLifecycle()
    var draftTitle by rememberSaveable(detail?.id) { mutableStateOf("") }

    LaunchedEffect(openMissionId) {
        openMissionId?.let { missionId ->
            onOpenMission(missionId)
            viewModel.consumeOpenMission()
        }
    }

    LaunchedEffect(detail?.id, detail?.title) {
        draftTitle = detail?.title.orEmpty()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        val shoppingDetail = detail
        if (!detailLoaded) {
            item {
                LoadingStateCard(
                    title = stringResource(R.string.shopping_detail_loading_title),
                    subtitle = stringResource(R.string.shopping_detail_loading_subtitle),
                )
            }
        } else if (shoppingDetail == null) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.shopping_detail_missing_title),
                    subtitle = stringResource(R.string.shopping_detail_missing_subtitle),
                )
            }
        } else {
            item {
                ShoppingListHeaderCard(
                    detail = shoppingDetail,
                    draftTitle = draftTitle,
                    onDraftTitleChange = { draftTitle = it },
                    onRename = { viewModel.renameList(draftTitle) },
                    onToggleArchived = { viewModel.setArchived(!shoppingDetail.archived) },
                    onOpenCapture = shoppingDetail.sourceCaptureId?.let { captureId -> { onOpenCapture(captureId) } },
                    onCreateQuest = viewModel::createQuest,
                    onDelete = viewModel::deleteList,
                )
            }
            item {
                ScaffoldCard(
                    title = "Checklist",
                    subtitle = "Simple checkbox list. All items stay in one vertical stack.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        shoppingDetail.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = item.checked,
                                    onCheckedChange = { checked -> viewModel.toggleItem(item.id, checked) },
                                )
                                Text(
                                    text = item.label,
                                    color = if (item.checked) AccentPrimary else TextSecondary,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingListHeaderCard(
    detail: ShoppingListDetailModel,
    draftTitle: String,
    onDraftTitleChange: (String) -> Unit,
    onRename: () -> Unit,
    onToggleArchived: () -> Unit,
    onOpenCapture: (() -> Unit)?,
    onCreateQuest: () -> Unit,
    onDelete: () -> Unit,
) {
    ScaffoldCard(
        title = detail.title,
        subtitle = "${detail.source.name.lowercase()} | ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(detail.createdAt))}",
    ) {
        OutlinedTextField(
            value = draftTitle,
            onValueChange = onDraftTitleChange,
            label = { Text(stringResource(R.string.shopping_detail_title_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = onRename, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.shopping_detail_save_title))
            }
            OutlinedButton(onClick = onToggleArchived, modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(
                        if (detail.archived) {
                            R.string.shopping_detail_reactivate
                        } else {
                            R.string.shopping_detail_archive
                        },
                    ),
                )
            }
        }
        Text(
            text = stringResource(
                R.string.shopping_detail_progress_format,
                detail.items.count { it.checked },
                detail.items.size,
            ),
            color = AccentPrimary,
            modifier = Modifier.padding(top = 12.dp),
        )
        onOpenCapture?.let { openCapture ->
            OutlinedButton(
                onClick = openCapture,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.shopping_detail_open_source))
            }
        }
        Button(
            onClick = onCreateQuest,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(stringResource(R.string.shopping_detail_create_quest))
        }
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(stringResource(R.string.shopping_detail_delete))
        }
    }
}

private fun createShoppingPhotoUri(context: android.content.Context): Uri {
    val file = File(context.cacheDir, "shopping-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
private fun ShoppingListSummaryCard(
    summary: ShoppingListSummary,
    onOpen: () -> Unit,
) {
    val progress = if (summary.itemCount == 0) 0 else summary.checkedCount * 100 / summary.itemCount
    ScaffoldCard(
        title = summary.title,
        subtitle = "${summary.source.name.lowercase()} | ${summary.checkedCount}/${summary.itemCount} checked",
    ) {
        Text(
            text = when {
                summary.checkedCount == summary.itemCount && summary.itemCount > 0 -> stringResource(R.string.shopping_summary_complete)
                summary.checkedCount > 0 -> stringResource(R.string.shopping_summary_progress_format, progress)
                else -> stringResource(R.string.shopping_summary_not_started)
            },
            color = AccentPrimary,
        )
        Button(
            onClick = onOpen,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(stringResource(R.string.shopping_summary_open))
        }
    }
}

data class ShoppingUiState(
    val lists: List<ShoppingListSummary> = emptyList(),
    val feedback: String? = null,
    val openListId: String? = null,
    val pendingPhotoUri: String? = null,
)

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository,
) : ViewModel() {
    private val _feedback = MutableStateFlow<String?>(null)
    private val _openListId = MutableStateFlow<String?>(null)
    private val _pendingPhotoUri = MutableStateFlow<String?>(null)

    val state: StateFlow<ShoppingUiState> =
        combine(
            shoppingListRepository.observeLists(),
            _feedback,
            _openListId,
            _pendingPhotoUri,
        ) { summaries, feedback, openListId, pendingPhotoUri ->
            ShoppingUiState(
                lists = summaries,
                feedback = feedback,
                openListId = openListId,
                pendingPhotoUri = pendingPhotoUri,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState())

    fun createManualList(title: String, rawInput: String) {
        viewModelScope.launch {
            shoppingListRepository.createManualList(title, rawInput)
                .fold(
                    onSuccess = { listId ->
                        _feedback.value = "Shopping list created."
                        _openListId.value = listId
                    },
                    onFailure = { error ->
                        _feedback.value = error.message ?: "Shopping list could not be created."
                    },
                )
        }
    }

    fun createVoiceList(rawInput: String) {
        viewModelScope.launch {
            shoppingListRepository.createVoiceList(rawInput)
                .fold(
                    onSuccess = { listId ->
                        _feedback.value = "Shopping list created from dictation."
                        _openListId.value = listId
                    },
                    onFailure = { error ->
                        _feedback.value = error.message ?: "Voice import failed."
                    },
                )
        }
    }

    fun createPhotoList(imageUri: Uri) {
        viewModelScope.launch {
            shoppingListRepository.createPhotoList(imageUri)
                .fold(
                    onSuccess = { listId ->
                        _feedback.value = "Shopping list created from photo."
                        _openListId.value = listId
                    },
                    onFailure = { error ->
                        _feedback.value = error.message ?: "Photo scan failed."
                    },
                )
            _pendingPhotoUri.value = null
        }
    }

    fun showFeedback(message: String) {
        _feedback.value = message
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    fun consumeOpenList() {
        _openListId.value = null
    }

    fun setPendingPhotoUri(uri: String?) {
        _pendingPhotoUri.value = uri
    }
}

@HiltViewModel
class ShoppingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val shoppingListRepository: ShoppingListRepository,
    private val missionRepository: com.astrogolem.sidequest.core.data.repo.MissionRepository,
) : ViewModel() {
    private val listId: String = checkNotNull(savedStateHandle["listId"])
    private val _openMissionId = MutableStateFlow<String?>(null)
    val openMissionId: StateFlow<String?> = _openMissionId

    val detail: StateFlow<ShoppingListDetailModel?> =
        shoppingListRepository.observeList(listId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val detailLoaded: StateFlow<Boolean> =
        shoppingListRepository.observeList(listId)
            .map { true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleItem(itemId: String, checked: Boolean) {
        viewModelScope.launch {
            shoppingListRepository.toggleItem(itemId, checked)
        }
    }

    fun renameList(title: String) {
        viewModelScope.launch {
            shoppingListRepository.renameList(listId, title)
        }
    }

    fun setArchived(archived: Boolean) {
        viewModelScope.launch {
            shoppingListRepository.setListArchived(listId, archived)
        }
    }

    fun deleteList() {
        viewModelScope.launch {
            shoppingListRepository.deleteList(listId)
        }
    }

    fun createQuest() {
        viewModelScope.launch {
            _openMissionId.value = missionRepository.createQuestFromShoppingList(listId)
        }
    }

    fun consumeOpenMission() {
        _openMissionId.value = null
    }
}
