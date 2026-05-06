package luzzr.zou.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import luzzr.zou.core.ui.MotionTokens
import luzzr.zou.core.ui.RadialExpansionController
import luzzr.zou.feature.habits.HabitDetailRoute
import luzzr.zou.feature.habits.HabitEditorRoute
import luzzr.zou.feature.habits.HabitRoutes
import luzzr.zou.feature.habits.HabitsRoute
import luzzr.zou.feature.backup.BackupRestoreRoute
import luzzr.zou.feature.notes.NoteDetailRoute
import luzzr.zou.feature.notes.NoteEditorRoute
import luzzr.zou.feature.notes.NoteRoutes
import luzzr.zou.feature.notes.NotesRoute
import luzzr.zou.feature.settings.SettingsRoute
import luzzr.zou.feature.settings.SettingsRoutes
import luzzr.zou.feature.tasks.TaskDetailRoute
import luzzr.zou.feature.tasks.TaskEditorRoute
import luzzr.zou.feature.tasks.TaskRoutes
import luzzr.zou.feature.tasks.TasksRoute
import luzzr.zou.feature.trash.TrashRoute
import luzzr.zou.feature.today.TodayRoute

@Composable
fun NoteFlowNavHost(
    navController: NavHostController,
    selectedTopLevelDestination: TopLevelDestination,
    onSelectedTopLevelDestinationChange: (TopLevelDestination) -> Unit,
    radialExpansionController: RadialExpansionController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = RootRoutes.TopLevelCanvas,
        modifier = modifier,
        enterTransition = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationDepthEnter,
                    easing = MotionTokens.EasingEmphasized,
                ),
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationDepthEnter,
                    easing = MotionTokens.EasingEmphasized,
                ),
                initialOffsetY = { (it * 0.08f).toInt() },
            ) + scaleIn(
                initialScale = 0.98f,
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationDepthEnter,
                    easing = MotionTokens.EasingEmphasized,
                ),
            )
        },
        exitTransition = {
            fadeOut(
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationDepthExit,
                    easing = MotionTokens.EasingStandard,
                ),
            ) + scaleOut(
                targetScale = 0.95f,
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationDepthExit,
                    easing = MotionTokens.EasingStandard,
                ),
            ) + slideOutVertically(
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationDepthExit,
                    easing = MotionTokens.EasingStandard,
                ),
                targetOffsetY = { (it * 0.04f).toInt() },
            )
        },
        popEnterTransition = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationDepthEnter,
                    easing = MotionTokens.EasingEmphasized,
                ),
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationDepthEnter,
                    easing = MotionTokens.EasingEmphasized,
                ),
                initialOffsetY = { -(it * 0.04f).toInt() },
            ) + scaleIn(
                initialScale = 0.985f,
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationDepthEnter,
                    easing = MotionTokens.EasingEmphasized,
                ),
            )
        },
        popExitTransition = {
            fadeOut(
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationDepthExit,
                    easing = MotionTokens.EasingStandard,
                ),
            ) + slideOutVertically(
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationDepthExit,
                    easing = MotionTokens.EasingStandard,
                ),
                targetOffsetY = { (it * 0.06f).toInt() },
            ) + scaleOut(
                targetScale = 0.95f,
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationDepthExit,
                    easing = MotionTokens.EasingStandard,
                ),
            )
        },
    ) {
        composable(RootRoutes.TopLevelCanvas) {
            TopLevelCanvasRoute(
                selectedDestination = selectedTopLevelDestination,
                onDestinationChanged = onSelectedTopLevelDestinationChange,
                onCreateTask = {
                    navController.navigate(TaskRoutes.createRoute)
                },
                onOpenTask = { taskId ->
                    navController.navigate(TaskRoutes.detailRoute(taskId))
                },
                onEditTask = { taskId ->
                    navController.navigate(TaskRoutes.editRoute(taskId))
                },
                onOpenTasks = {
                    onSelectedTopLevelDestinationChange(TopLevelDestination.TASKS)
                },
                onCreateHabit = {
                    navController.navigate(HabitRoutes.createRoute)
                },
                onOpenHabit = { habitId ->
                    navController.navigate(HabitRoutes.detailRoute(habitId))
                },
                onEditHabit = { habitId ->
                    navController.navigate(HabitRoutes.editRoute(habitId))
                },
                onOpenHabits = {
                    onSelectedTopLevelDestinationChange(TopLevelDestination.HABITS)
                },
                onCreateNote = {
                    navController.navigate(NoteRoutes.createRoute)
                },
                onOpenNote = { noteId ->
                    navController.navigate(NoteRoutes.detailRoute(noteId))
                },
                onEditNote = { noteId ->
                    navController.navigate(NoteRoutes.editRoute(noteId))
                },
                onOpenNotes = {
                    onSelectedTopLevelDestinationChange(TopLevelDestination.NOTES)
                },
                onOpenSettings = {
                    navController.navigate(SettingsRoutes.settingsRoute)
                },
            )
        }
        composable(TopLevelDestination.TODAY.route) {
            LaunchedEffect(Unit) {
                onSelectedTopLevelDestinationChange(TopLevelDestination.TODAY)
                navController.navigate(RootRoutes.TopLevelCanvas) {
                    popUpTo(RootRoutes.TopLevelCanvas) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        }
        composable(TopLevelDestination.TASKS.route) {
            LaunchedEffect(Unit) {
                onSelectedTopLevelDestinationChange(TopLevelDestination.TASKS)
                navController.navigate(RootRoutes.TopLevelCanvas) {
                    popUpTo(RootRoutes.TopLevelCanvas) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        }
        composable(TaskRoutes.createRoute) {
            TaskEditorRoute(
                onNavigateBack = {
                    radialExpansionController.collapse(
                        onCollapsed = { navController.navigateUp() },
                    )
                },
            )
        }
        composable(
            route = TaskRoutes.editRoute,
            arguments = listOf(
                navArgument(TaskRoutes.taskIdArg) {
                    type = NavType.StringType
                },
            ),
        ) {
            TaskEditorRoute(
                onNavigateBack = { navController.navigateUp() },
            )
        }
        composable(
            route = TaskRoutes.detailRoute,
            arguments = listOf(
                navArgument(TaskRoutes.taskIdArg) {
                    type = NavType.StringType
                },
            ),
        ) {
            TaskDetailRoute(
                onNavigateBack = { navController.navigateUp() },
                onEditTask = { taskId ->
                    navController.navigate(TaskRoutes.editRoute(taskId))
                },
            )
        }
        composable(TopLevelDestination.HABITS.route) {
            LaunchedEffect(Unit) {
                onSelectedTopLevelDestinationChange(TopLevelDestination.HABITS)
                navController.navigate(RootRoutes.TopLevelCanvas) {
                    popUpTo(RootRoutes.TopLevelCanvas) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        }
        composable(HabitRoutes.createRoute) {
            HabitEditorRoute(
                onNavigateBack = {
                    radialExpansionController.collapse(
                        onCollapsed = { navController.navigateUp() },
                    )
                },
            )
        }
        composable(
            route = HabitRoutes.editRoute,
            arguments = listOf(
                navArgument(HabitRoutes.habitIdArg) {
                    type = NavType.StringType
                },
            ),
        ) {
            HabitEditorRoute(
                onNavigateBack = { navController.navigateUp() },
            )
        }
        composable(
            route = HabitRoutes.detailRoute,
            arguments = listOf(
                navArgument(HabitRoutes.habitIdArg) {
                    type = NavType.StringType
                },
            ),
        ) {
            HabitDetailRoute(
                onNavigateBack = { navController.navigateUp() },
                onEditHabit = { habitId ->
                    navController.navigate(HabitRoutes.editRoute(habitId))
                },
            )
        }
        composable(TopLevelDestination.NOTES.route) {
            LaunchedEffect(Unit) {
                onSelectedTopLevelDestinationChange(TopLevelDestination.NOTES)
                navController.navigate(RootRoutes.TopLevelCanvas) {
                    popUpTo(RootRoutes.TopLevelCanvas) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        }
        composable(NoteRoutes.createRoute) {
            NoteEditorRoute(
                onNavigateBack = {
                    radialExpansionController.collapse(
                        onCollapsed = { navController.navigateUp() },
                    )
                },
            )
        }
        composable(
            route = NoteRoutes.editRoute,
            arguments = listOf(
                navArgument(NoteRoutes.noteIdArg) {
                    type = NavType.StringType
                },
            ),
        ) {
            NoteEditorRoute(
                onNavigateBack = { navController.navigateUp() },
            )
        }
        composable(
            route = NoteRoutes.detailRoute,
            arguments = listOf(
                navArgument(NoteRoutes.noteIdArg) {
                    type = NavType.StringType
                },
            ),
        ) {
            NoteDetailRoute(
                onNavigateBack = { navController.navigateUp() },
                onEditNote = { noteId ->
                    navController.navigate(NoteRoutes.editRoute(noteId))
                },
            )
        }
        composable(SettingsRoutes.settingsRoute) {
            SettingsRoute(
                onNavigateBack = { navController.navigateUp() },
                onOpenTrash = { navController.navigate(SettingsRoutes.trashRoute) },
                onOpenBackup = { navController.navigate(SettingsRoutes.backupRoute) },
            )
        }
        composable(SettingsRoutes.trashRoute) {
            TrashRoute(
                onNavigateBack = { navController.navigateUp() },
            )
        }
        composable(SettingsRoutes.backupRoute) {
            BackupRestoreRoute(
                onNavigateBack = { navController.navigateUp() },
            )
        }
    }
}
