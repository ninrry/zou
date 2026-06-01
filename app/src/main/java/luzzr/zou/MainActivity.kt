package luzzr.zou

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import luzzr.zou.app.ZouApp
import luzzr.zou.core.designsystem.theme.ZouTheme
import luzzr.zou.core.reminder.ReminderConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject
import androidx.compose.runtime.collectAsState

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        handleReminderIntent(intent)
        setContent {
            val preferences by observeReminderPreferencesUseCase().collectAsState(initial = null)
            ZouTheme {
                StartupPermissionCoordinator()
                if (preferences != null) {
                    ZouApp(
                        defaultStartDestination = preferences!!.defaultStartDestination,
                        pendingTaskDetailId = pendingTaskDetailId,
                        pendingHabitDetailId = pendingHabitDetailId,
                        onPendingTaskDetailConsumed = { pendingTaskDetailId = null },
                        onPendingHabitDetailConsumed = { pendingHabitDetailId = null },
                    )
                } else {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .background(luzzr.zou.core.designsystem.theme.MonetColorTokens.canvas)
                    )
                }
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

@Composable
private fun StartupPermissionCoordinator() {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }
    var notificationRequested by rememberSaveable { mutableStateOf(false) }
    var exactAlarmRequested by rememberSaveable { mutableStateOf(false) }
    var batteryOptimizationRequested by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationRequested &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationRequested = true
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!notificationRequested) {
            notificationRequested = true
        }
    }

    LaunchedEffect(notificationRequested) {
        if (!exactAlarmRequested) {
            exactAlarmRequested = true
            context.createExactAlarmIntent()?.let { intent ->
                delay(300)
                context.startActivity(intent)
            }
        }
    }

    LaunchedEffect(exactAlarmRequested) {
        if (!batteryOptimizationRequested) {
            batteryOptimizationRequested = true
            context.createBatteryOptimizationIntent()?.let { intent ->
                delay(300)
                context.startActivity(intent)
            }
        }
    }
}

private fun android.content.Context.createExactAlarmIntent(): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val alarmManager = getSystemService(AlarmManager::class.java) ?: return null
    if (alarmManager.canScheduleExactAlarms()) return null
    return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:$packageName")
    }
}

private fun android.content.Context.createBatteryOptimizationIntent(): Intent? {
    val powerManager = getSystemService(PowerManager::class.java) ?: return null
    if (powerManager.isIgnoringBatteryOptimizations(packageName)) return null
    return Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:$packageName"),
    )
}
