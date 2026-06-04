package luzzr.zou.core.reminder

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import luzzr.zou.core.designsystem.theme.ZouTheme
import luzzr.zou.core.designsystem.theme.ZouDesignTokens
import luzzr.zou.MainActivity
import luzzr.zou.core.ui.GlassLevel
import luzzr.zou.core.ui.GlassSurface
import luzzr.zou.core.ui.LayoutTokens
import luzzr.zou.core.ui.noteFlowButtonColors
import luzzr.zou.core.ui.noteFlowOutlinedButtonColors

class ReminderAlertActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var uiState by mutableStateOf(ReminderAlertUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handleWindowFlags()
        uiState = intent.toUiState()

        setContent {
            ZouTheme {
                val designTokens = ZouDesignTokens.colors
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(designTokens.surfaceFloating.copy(alpha = 0.96f))
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(LayoutTokens.Space20),
                    contentAlignment = Alignment.Center,
                ) {
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        level = GlassLevel.Strong,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(LayoutTokens.Space24),
                            verticalArrangement = Arrangement.spacedBy(LayoutTokens.Space16),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = uiState.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = uiState.body,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    stopAlertEffects()
                                    cancelNotification()
                                    startActivity(buildOpenDetailIntent())
                                    finish()
                                },
                                colors = noteFlowButtonColors(),
                            ) {
                                Text(if (uiState.isHabit) "打开习惯" else "打开任务")
                            }
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    stopAlertEffects()
                                    cancelNotification()
                                    finish()
                                },
                                colors = noteFlowOutlinedButtonColors(),
                            ) {
                                Text("关闭提醒")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startAlertEffects()
    }

    override fun onStop() {
        stopAlertEffects()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        uiState = intent.toUiState()
        stopAlertEffects()
        startAlertEffects()
    }

    private fun handleWindowFlags() {
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
        )
    }

    private fun startAlertEffects() {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone?.stop()
        ringtone = RingtoneManager.getRingtone(this, alarmUri)?.apply {
            isLooping = true
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            play()
        }

        vibrator?.cancel()
        vibrator = resolveVibrator().also { targetVibrator ->
            targetVibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 600), 0),
            )
        }
    }

    private fun stopAlertEffects() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun cancelNotification() {
        getSystemService(NotificationManager::class.java)
            ?.cancel(intent.getIntExtra(ReminderConstants.alertNotificationIdExtra, 0))
    }

    private fun buildOpenDetailIntent(): Intent {
        return Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            uiState.taskId?.let { putExtra(ReminderConstants.taskIdExtra, it) }
            uiState.habitId?.let { putExtra(ReminderConstants.habitIdExtra, it) }
        }
    }

    private fun resolveVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun Intent.toUiState(): ReminderAlertUiState {
        return ReminderAlertUiState(
            title = getStringExtra(ReminderConstants.alertTitleExtra).orEmpty().ifBlank { "提醒" },
            body = getStringExtra(ReminderConstants.alertBodyExtra).orEmpty()
                .ifBlank { "时间到了，请立即处理。" },
            taskId = getStringExtra(ReminderConstants.taskIdExtra),
            habitId = getStringExtra(ReminderConstants.habitIdExtra),
            isHabit = getBooleanExtra(ReminderConstants.alertIsHabitExtra, false),
        )
    }
}

private data class ReminderAlertUiState(
    val title: String = "提醒",
    val body: String = "时间到了，请立即处理。",
    val taskId: String? = null,
    val habitId: String? = null,
    val isHabit: Boolean = false,
)
