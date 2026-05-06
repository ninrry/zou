package luzzr.zou.feature.habits

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import luzzr.zou.core.designsystem.theme.ZouHabitAccent
import luzzr.zou.core.ui.GlassSurface
import luzzr.zou.core.ui.ModuleFab
import luzzr.zou.core.ui.ZouEmptyStateCard
import luzzr.zou.core.ui.ZouMetaChip
import luzzr.zou.core.ui.ZouStaggeredReveal
import luzzr.zou.core.ui.noteFlowButtonColors
import luzzr.zou.core.ui.noteFlowPressScale
import luzzr.zou.core.ui.rememberPressInteractionSource

@Composable
fun HabitsRoute(
    onCreateHabit: () -> Unit,
    onOpenHabit: (String) -> Unit,
    onEditHabit: (String) -> Unit,
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    HabitsScreen(
        uiState = uiState,
        onCreateHabit = onCreateHabit,
        onOpenHabit = onOpenHabit,
        onEditHabit = onEditHabit,
        onQuickCheckHabit = viewModel::onQuickCheckHabit,
        onRestoreHabit = viewModel::onRestoreHabit,
    )
}

@Composable
fun HabitsScreen(
    uiState: HabitsUiState,
    onCreateHabit: () -> Unit,
    onOpenHabit: (String) -> Unit,
    onEditHabit: (String) -> Unit,
    onQuickCheckHabit: (String) -> Unit,
    onRestoreHabit: (String) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ModuleFab(
                accentColor = ZouHabitAccent,
                contentDescription = "新建习惯",
                icon = Icons.Default.Add,
                testTag = "habits_fab",
                onClick = onCreateHabit,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (uiState.activeHabits.isEmpty() && uiState.deletedHabits.isEmpty()) {
                item {
                    ZouStaggeredReveal(revealKey = "habits_empty", index = 0) {
                        ZouEmptyStateCard(
                            title = uiState.emptyTitle,
                            description = uiState.emptyDescription,
                            accentColor = ZouHabitAccent,
                        )
                    }
                }
            } else {
                items(uiState.activeHabits, key = { it.id }) { habit ->
                    HabitCard(
                        item = habit,
                        onOpenHabit = onOpenHabit,
                        onEditHabit = onEditHabit,
                        onQuickCheckHabit = onQuickCheckHabit,
                        onRestoreHabit = onRestoreHabit,
                    )
                }
                if (uiState.deletedHabits.isNotEmpty()) {
                    item {
                        ZouStaggeredReveal(revealKey = "habits_deleted_header", index = 0) {
                            Text(
                                text = "已删除习惯",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    items(uiState.deletedHabits, key = { it.id }) { habit ->
                        HabitCard(
                            item = habit,
                            onOpenHabit = onOpenHabit,
                            onEditHabit = onEditHabit,
                            onQuickCheckHabit = onQuickCheckHabit,
                            onRestoreHabit = onRestoreHabit,
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(112.dp))
            }
        }
    }
}

@Composable
private fun HabitCard(
    item: HabitCardUiModel,
    onOpenHabit: (String) -> Unit,
    onEditHabit: (String) -> Unit,
    onQuickCheckHabit: (String) -> Unit,
    onRestoreHabit: (String) -> Unit,
) {
    val interactionSource = rememberPressInteractionSource()
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_card_${item.id}")
            .noteFlowPressScale(interactionSource = interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = item.canOpenDetail,
                onClick = { onOpenHabit(item.id) },
                onLongClick = { onEditHabit(item.id) },
            ),
        accentColor = ZouHabitAccent,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = item.title,
                modifier = Modifier.testTag("habit_open_${item.title}"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ZouMetaChip(text = item.frequencyText)
                ZouMetaChip(text = item.modeText, accentColor = ZouHabitAccent)
                ZouMetaChip(text = item.statusText)
            }
            Text(
                text = item.supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!item.quickActionLabel.isNullOrBlank()) {
                Button(
                    modifier = Modifier.testTag("habit_quick_action_${item.id}"),
                    enabled = item.quickActionEnabled || !item.canQuickCheck,
                    onClick = {
                        if (item.canQuickCheck) {
                            onQuickCheckHabit(item.id)
                        } else {
                            onOpenHabit(item.id)
                        }
                    },
                    colors = noteFlowButtonColors(ZouHabitAccent),
                ) {
                    Text(item.quickActionLabel)
                }
            }
            if (item.canRestore) {
                Button(
                    modifier = Modifier.testTag("habit_restore_${item.title}"),
                    onClick = { onRestoreHabit(item.id) },
                    colors = noteFlowButtonColors(ZouHabitAccent),
                ) {
                    Text("恢复习惯")
                }
            }
        }
    }
}
