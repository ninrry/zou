package luzzr.zou.feature.notes

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import luzzr.zou.core.designsystem.theme.ZouNoteAccent
import luzzr.zou.core.markdown.MarkdownRenderer
import luzzr.zou.core.ui.ZouEditorSection
import luzzr.zou.core.ui.ZouEmptyStateCard
import luzzr.zou.core.ui.ZouPageHeader
import luzzr.zou.core.ui.ZouPageScaffold
import luzzr.zou.core.ui.ZouSectionCard
import luzzr.zou.core.ui.ZouBottomActionBar
import luzzr.zou.core.ui.ZouShimmerList
import luzzr.zou.core.ui.LayoutTokens
import luzzr.zou.core.ui.noteFlowButtonColors
import luzzr.zou.core.ui.noteFlowOutlinedButtonColors
import luzzr.zou.core.ui.noteFlowOutlinedTextFieldColors

@Composable
fun NoteEditorRoute(
    onNavigateBack: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { viewModel.onInsertImage(it.toString()) }
    }

    BackHandler(enabled = !uiState.isSaving) {
        if (viewModel.onDiscardClicked()) {
            onNavigateBack()
        }
    }

    NoteEditorScreen(
        uiState = uiState,
        onNavigateBack = {
            if (!uiState.isSaving && viewModel.onDiscardClicked()) {
                onNavigateBack()
            }
        },
        onTitleChanged = viewModel::onTitleChanged,
        onContentChanged = viewModel::onContentChanged,
        onPickImage = {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onSaveClicked = {
            if (viewModel.validateBeforeSave()) {
                viewModel.saveNote(onSaved = onNavigateBack)
            }
        },
        onDeleteClicked = {
            viewModel.onDeleteClicked(onDeleted = onNavigateBack)
        },
    )
}

@Composable
fun NoteEditorScreen(
    uiState: NoteEditorUiState,
    onNavigateBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (TextFieldValue) -> Unit,
    onPickImage: () -> Unit,
    onSaveClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
) {
    var previewVisible by rememberSaveable(uiState.noteId) { mutableStateOf(false) }

    ZouPageScaffold(
        bottomBar = {
            if (!uiState.isLoading && !uiState.hasMissingContent) {
                ZouBottomActionBar(
                    primaryLabel = uiState.saveButtonLabel,
                    primaryAccentColor = ZouNoteAccent,
                    primaryEnabled = !uiState.isSaving,
                    primaryLoading = uiState.isSaving,
                    primaryTestTag = "note_editor_save",
                    secondaryLabel = "放弃",
                    secondaryEnabled = !uiState.isSaving,
                    onSecondaryClick = onNavigateBack,
                    onPrimaryClick = onSaveClicked,
                )
            }
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Top,
            ) {
                ZouShimmerList()
            }
        } else if (uiState.hasMissingContent) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TextButton(onClick = onNavigateBack) {
                    Text("返回")
                }
                ZouEmptyStateCard(
                    title = "笔记不存在",
                    description = uiState.loadErrorMessage ?: "这条笔记可能已经被删除。",
                    accentColor = ZouNoteAccent,
                    actionLabel = "返回列表",
                    actionTestTag = "note_editor_go_back",
                    onActionClick = onNavigateBack,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TextButton(onClick = onNavigateBack, enabled = !uiState.isSaving) {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    Text("返回", modifier = Modifier.padding(start = LayoutTokens.Space8))
                }
                ZouPageHeader(
                    title = uiState.screenTitle,
                    subtitle = "编辑优先，预览按需展开。",
                )

                ZouEditorSection(
                    title = "正文",
                    subtitle = "先写内容，需要时再展开预览。",
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_editor_title_input"),
                        value = uiState.title,
                        onValueChange = onTitleChanged,
                        label = { Text("标题") },
                        singleLine = true,
                        enabled = !uiState.isSaving,
                        isError = uiState.titleError != null,
                        supportingText = { uiState.titleError?.let { Text(it) } },
                        colors = noteFlowOutlinedTextFieldColors(),
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_editor_content_input"),
                        value = uiState.content,
                        onValueChange = onContentChanged,
                        enabled = !uiState.isSaving,
                        label = { Text("正文（Markdown 原文）") },
                        minLines = 12,
                        visualTransformation = NoteImageReferenceVisualTransformation,
                        colors = noteFlowOutlinedTextFieldColors(),
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_editor_insert_image"),
                            onClick = onPickImage,
                            enabled = !uiState.isSaving,
                            colors = noteFlowButtonColors(ZouNoteAccent),
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null)
                            Text(text = "插入图片", modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { previewVisible = !previewVisible },
                            enabled = !uiState.isSaving,
                            colors = noteFlowOutlinedButtonColors(),
                        ) {
                            Text(if (previewVisible) "收起实时预览" else "展开实时预览")
                        }
                    }
                }

                if (previewVisible) {
                    ZouSectionCard(
                        title = "实时预览",
                        subtitle = "只读渲染，不影响原文。",
                        modifier = Modifier.testTag("note_markdown_preview"),
                    ) {
                        if (uiState.content.text.isBlank()) {
                            Text(
                                text = "输入 Markdown 后会在这里预览。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            MarkdownRenderer(
                                markdown = uiState.content.text,
                                mediaLookup = uiState.images.associate { it.mediaId to it.localPath },
                            )
                        }
                    }
                }

                if (uiState.canDelete) {
                    ZouEditorSection(
                        title = "危险操作",
                        subtitle = "删除后会进入回收站，不影响底部主动作区。",
                    ) {
                        HorizontalDivider()
                        TextButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_editor_delete"),
                            onClick = onDeleteClicked,
                            enabled = !uiState.isSaving,
                        ) {
                            Text(
                                text = "软删除笔记",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                uiState.saveErrorMessage?.let {
                    Text(
                        text = it,
                        modifier = Modifier.testTag("note_editor_save_error"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
