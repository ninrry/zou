package luzzr.zou

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import luzzr.zou.core.designsystem.theme.ZouTheme
import luzzr.zou.domain.model.TrashItemType
import luzzr.zou.feature.backup.BackupRestoreScreen
import luzzr.zou.feature.backup.BackupRestoreUiState
import luzzr.zou.feature.settings.SettingsScreen
import luzzr.zou.feature.settings.SettingsUiState
import luzzr.zou.feature.today.TodayUiEvent
import luzzr.zou.feature.today.TodayScreen
import luzzr.zou.feature.today.TodayTaskCardUiModel
import luzzr.zou.feature.today.TodayHabitCardUiModel
import luzzr.zou.feature.today.TodaySummaryUiModel
import luzzr.zou.feature.today.TodayUiState
import luzzr.zou.feature.trash.TrashItemCardUiModel
import luzzr.zou.feature.trash.TrashScreen
import luzzr.zou.feature.trash.TrashUiState
import luzzr.zou.domain.usecase.HabitQuickActionType
import luzzr.zou.domain.usecase.TaskQuickActionType
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QualityScreenStateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun todayScreenShowsDualColumnEmptyStatesAndQuickCreateActions() {
        composeRule.setContent {
            ZouTheme {
                TodayScreen(
                    uiState = TodayUiState(
                        dateLine = "2026-03-09",
                        summary = TodaySummaryUiModel(),
                        isCompletelyEmpty = true,
                    ),
                    onCreateTask = {},
                    onOpenTask = {},
                    onEditTask = {},
                    onOpenTasks = {},
                    onTaskAction = { _, _ -> },
                    onCreateHabit = {},
                    onOpenHabit = {},
                    onEditHabit = {},
                    onOpenHabits = {},
                    onHabitPrimaryAction = { _, _, _ -> },
                    onHabitSecondaryAction = { _, _ -> },
                    events = emptyFlow<TodayUiEvent>(),
                    onUndo = {},
                    onUndoExpired = {},
                    onOpenSettings = {},
                )
            }
        }

        assertTagExists("today_dual_columns")
        assertTagExists("today_tasks_column")
        assertTagExists("today_habits_column")
        assertTagExists("today_empty_tasks_card")
        assertTagExists("today_empty_habits_card")
        assertTagNotExists("today_empty_state")
        assertTagNotExists("today_empty_create_note")
    }

    @Test
    fun todayScreenShowsDualColumnsWhenTasksOrHabitsExist() {
        composeRule.setContent {
            ZouTheme {
                TodayScreen(
                    uiState = TodayUiState(
                        dateLine = "2026-03-09",
                        summary = TodaySummaryUiModel(
                            pendingTaskCount = 1,
                            dueHabitCount = 1,
                            completedCount = 0,
                        ),
                        tasks = listOf(
                            TodayTaskCardUiModel(
                                id = "task-1",
                                title = "一个很长但不应该把卡片挤坏的任务标题",
                                actionType = TaskQuickActionType.COMPLETE,
                                actionLabel = "完成",
                                actionEnabled = true,
                            ),
                        ),
                        habits = listOf(
                            TodayHabitCardUiModel(
                                id = "habit-1",
                                title = "吃饭",
                                primaryActionType = HabitQuickActionType.CHECK,
                                primaryActionLabel = "打卡",
                                primaryActionEnabled = true,
                            ),
                        ),
                    ),
                    onCreateTask = {},
                    onOpenTask = {},
                    onEditTask = {},
                    onOpenTasks = {},
                    onTaskAction = { _, _ -> },
                    onCreateHabit = {},
                    onOpenHabit = {},
                    onEditHabit = {},
                    onOpenHabits = {},
                    onHabitPrimaryAction = { _, _, _ -> },
                    onHabitSecondaryAction = { _, _ -> },
                    events = emptyFlow<TodayUiEvent>(),
                    onUndo = {},
                    onUndoExpired = {},
                    onOpenSettings = {},
                )
            }
        }

        assertTagExists("today_dual_columns")
        assertTagExists("today_tasks_column")
        assertTagExists("today_habits_column")
        assertTagExists("today_summary_grid")
        assertTagExists("today_task_action_task-1")
        assertTagExists("today_habit_primary_action_habit-1")
        assertTagNotExists("today_empty_state")
        assertTagNotExists("today_view_all_notes")
    }

    @Test
    fun todayScreenKeepsTwoColumnsWhenOneSideIsEmpty() {
        composeRule.setContent {
            ZouTheme {
                TodayScreen(
                    uiState = TodayUiState(
                        dateLine = "2026-03-09",
                        summary = TodaySummaryUiModel(
                            pendingTaskCount = 0,
                            dueHabitCount = 1,
                            completedCount = 0,
                        ),
                        habits = listOf(
                            TodayHabitCardUiModel(
                                id = "habit-1",
                                title = "步行",
                                primaryActionType = HabitQuickActionType.CHECK,
                                primaryActionLabel = "打卡",
                                primaryActionEnabled = true,
                            ),
                        ),
                    ),
                    onCreateTask = {},
                    onOpenTask = {},
                    onEditTask = {},
                    onOpenTasks = {},
                    onTaskAction = { _, _ -> },
                    onCreateHabit = {},
                    onOpenHabit = {},
                    onEditHabit = {},
                    onOpenHabits = {},
                    onHabitPrimaryAction = { _, _, _ -> },
                    onHabitSecondaryAction = { _, _ -> },
                    events = emptyFlow<TodayUiEvent>(),
                    onUndo = {},
                    onUndoExpired = {},
                    onOpenSettings = {},
                )
            }
        }

        assertTagExists("today_dual_columns")
        assertTagExists("today_empty_tasks_card")
        assertTagExists("today_habit_habit-1")
        assertTagNotExists("today_empty_state")
    }

    @Test
    fun settingsScreenShowsActionsAndMessages() {
        composeRule.setContent {
            ZouTheme {
                SettingsScreen(
                    uiState = SettingsUiState(
                        errorMessage = "保存失败",
                        resultMessage = "已保存",
                    ),
                    onNavigateBack = {},
                    onTaskDefaultIntervalChanged = {},
                    onHabitDefaultIntervalChanged = {},
                    onShowCompletedTasksChanged = {},
                    onShowOnlyTodayHabitsChanged = {},
                    onShowDeletedHabitsChanged = {},
                    onDefaultStartDestinationChanged = {},
                    onSaveDefaults = {},
                    onOpenNotificationSettings = {},
                    onOpenTrash = {},
                    onOpenBackup = {},
                )
            }
        }

        assertTagExists("settings_error")
        assertTagExists("settings_result")
        assertTagExists("settings_show_completed_tasks")
        assertTagExists("settings_show_today_habits")
        assertTagExists("settings_show_deleted_habits")
        composeRule.onNodeWithTag("settings_content").performTouchInput { swipeUp() }
        assertTagExists("settings_open_trash")
        assertTagExists("settings_open_backup")
    }

    @Test
    fun trashScreenRendersEmptyAndActionStates() {
        var restored = false
        var deleted = false

        composeRule.setContent {
            ZouTheme {
                TrashScreen(
                    uiState = TrashUiState(
                        items = listOf(
                            TrashItemCardUiModel(
                                id = "trash-1",
                                type = TrashItemType.NOTE,
                                title = "Deleted note",
                                previewText = "Preview",
                                deletedAtText = "2026-03-09 10:00",
                            ),
                        ),
                        isEmpty = false,
                    ),
                    onNavigateBack = {},
                    onRestore = { restored = true },
                    onHardDelete = { deleted = true },
                )
            }
        }

        composeRule.onNodeWithTag("trash_restore_trash-1").performClick()
        composeRule.onNodeWithTag("trash_delete_trash-1").performClick()

        assertTrue(restored)
        assertTrue(deleted)
    }

    @Test
    fun backupScreenShowsWarningsAndActions() {
        var exported = false
        var imported = false

        composeRule.setContent {
            ZouTheme {
                BackupRestoreScreen(
                    uiState = BackupRestoreUiState(
                        resultMessage = "导入成功",
                        warningMessages = listOf("media missing"),
                    ),
                    onNavigateBack = {},
                    onExportBackup = { exported = true },
                    onImportBackup = { imported = true },
                )
            }
        }

        composeRule.onNodeWithTag("backup_export").performClick()
        composeRule.onNodeWithTag("backup_import").performClick()
        assertTagExists("backup_result")
        assertTagExists("backup_warning_0")

        assertTrue(exported)
        assertTrue(imported)
    }

    private fun assertTagExists(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty())
    }

    private fun assertTagNotExists(tag: String) {
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty())
    }
}
