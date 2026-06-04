package luzzr.zou

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import luzzr.zou.app.ZouApp
import luzzr.zou.core.designsystem.theme.ZouTheme
import luzzr.zou.core.reminder.ReminderConstants
import luzzr.zou.data.settings.ReminderPreferences
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var observeReminderPreferencesUseCase: luzzr.zou.domain.usecase.ObserveReminderPreferencesUseCase

    private var pendingTaskDetailId by mutableStateOf<String?>(null)
    private var pendingHabitDetailId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb(),
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb(),
            ),
        )
        window.isNavigationBarContrastEnforced = false
        handleReminderIntent(intent)
        setContent {
            val preferences by observeReminderPreferencesUseCase()
                .collectAsStateWithLifecycle(initialValue = ReminderPreferences())
            ZouTheme {
                ZouApp(
                    defaultStartDestination = preferences.defaultStartDestination,
                    pendingTaskDetailId = pendingTaskDetailId,
                    pendingHabitDetailId = pendingHabitDetailId,
                    onPendingTaskDetailConsumed = { pendingTaskDetailId = null },
                    onPendingHabitDetailConsumed = { pendingHabitDetailId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminderIntent(intent)
    }

    internal fun handleReminderIntent(intent: Intent?) {
        pendingTaskDetailId = intent?.getStringExtra(ReminderConstants.taskIdExtra)
        pendingHabitDetailId = intent?.getStringExtra(ReminderConstants.habitIdExtra)
    }
}
