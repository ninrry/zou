package luzzr.zou.core.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import luzzr.zou.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notificationPermissionChecker: NotificationPermissionChecker,
) {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        for (config in ReminderConstants.channelConfigs) {
            val existing = notificationManager.getNotificationChannel(config.id)
            if (existing != null) continue // 已创建，保留用户自定义
            val channel = NotificationChannel(
                config.id,
                config.name,
                config.importance,
            ).apply {
                description = when (config.id) {
                    ReminderConstants.channelTaskStart -> "任务开始时间的提醒"
                    ReminderConstants.channelTaskDue -> "任务截止时间的提醒"
                    ReminderConstants.channelHabitReminder -> "习惯打卡的定时提醒"
                    ReminderConstants.channelRepeat -> "逾期任务的重复提醒"
                    else -> "提醒通知"
                }
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTaskReminder(
        taskId: String,
        taskTitle: String,
        triggerAtMillis: Long,
        reason: ReminderTriggerReason,
        customTitle: String? = null,
        customBody: String? = null,
    ) {
        ensureChannels()
        val notificationId = ReminderConstants.notificationId(taskId)
        val title = resolveNotificationTitle(taskTitle, customTitle)
        val body = resolveNotificationBody(triggerAtMillis, reason, customBody)
        val channelId = channelIdForTaskReason(reason)
        val notification = createBaseBuilder(
            channelId = channelId,
            title = title,
            body = body,
            contentIntent = createTaskDetailPendingIntent(taskId),
            fullScreenIntent = createReminderAlertPendingIntent(
                notificationId = notificationId,
                title = title,
                body = body,
                taskId = taskId,
                habitId = null,
            ),
        ).build()

        notifySafely(notificationId, notification)
    }

    fun showHabitReminder(
        habitId: String,
        habitTitle: String,
        triggerAtMillis: Long,
        reason: ReminderTriggerReason,
        customTitle: String? = null,
        customBody: String? = null,
    ) {
        ensureChannels()
        val notificationId = ReminderConstants.habitNotificationId(habitId)
        val title = resolveNotificationTitle(habitTitle, customTitle)
        val body = resolveNotificationBody(triggerAtMillis, reason, customBody)
        val notification = createBaseBuilder(
            channelId = ReminderConstants.channelHabitReminder,
            title = title,
            body = body,
            contentIntent = createHabitDetailPendingIntent(habitId),
            fullScreenIntent = createReminderAlertPendingIntent(
                notificationId = notificationId,
                title = title,
                body = body,
                taskId = null,
                habitId = habitId,
            ),
        ).build()

        notifySafely(notificationId, notification)
    }

    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun notifySafely(notificationId: Int, notification: Notification) {
        if (!notificationPermissionChecker.canPostNotifications()) return
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (exception: SecurityException) {
            Log.w(TAG, "Notification permission was revoked before notify($notificationId)", exception)
        }
    }

    // ── 频道选择 ──────────────────────────────────────────────

    private fun channelIdForTaskReason(reason: ReminderTriggerReason): String {
        return when (reason) {
            ReminderTriggerReason.REPEAT -> ReminderConstants.channelRepeat
            ReminderTriggerReason.START,
            ReminderTriggerReason.EXACT -> ReminderConstants.channelTaskStart
        }
    }

    // ── Intent 构建 ────────────────────────────────────────────

    internal fun buildTaskDetailIntent(taskId: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderConstants.taskIdExtra, taskId)
        }
    }

    internal fun buildHabitDetailIntent(habitId: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderConstants.habitIdExtra, habitId)
        }
    }

    internal fun buildReminderAlertIntent(
        notificationId: Int,
        title: String,
        body: String,
        taskId: String?,
        habitId: String?,
    ): Intent {
        return Intent(context, ReminderAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(ReminderConstants.alertNotificationIdExtra, notificationId)
            putExtra(ReminderConstants.alertTitleExtra, title)
            putExtra(ReminderConstants.alertBodyExtra, body)
            taskId?.let { putExtra(ReminderConstants.taskIdExtra, it) }
            habitId?.let {
                putExtra(ReminderConstants.habitIdExtra, it)
                putExtra(ReminderConstants.alertIsHabitExtra, true)
            }
        }
    }

    internal fun resolveNotificationTitle(
        fallbackTitle: String,
        customTitle: String?,
    ): String {
        return customTitle?.trim().orEmpty().ifBlank { fallbackTitle }
    }

    internal fun resolveNotificationBody(
        triggerAtMillis: Long,
        reason: ReminderTriggerReason,
        customBody: String?,
    ): String {
        val normalizedCustomBody = customBody?.trim().orEmpty()
        if (normalizedCustomBody.isNotBlank()) {
            return normalizedCustomBody
        }
        val reasonText = when (reason) {
            ReminderTriggerReason.START -> "开始提醒时间已到"
            ReminderTriggerReason.REPEAT -> "项目仍未完成，按提醒间隔再次提醒"
            ReminderTriggerReason.EXACT -> "特别提醒时间已到"
        }
        val timeText = if (triggerAtMillis > 0L) {
            Instant.ofEpochMilli(triggerAtMillis)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
        } else {
            "当前时间"
        }
        return "$reasonText · $timeText"
    }

    // ── Builder ───────────────────────────────────────────────

    private fun createBaseBuilder(
        channelId: String,
        title: String,
        body: String,
        contentIntent: PendingIntent,
        fullScreenIntent: PendingIntent,
    ): NotificationCompat.Builder {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setVibrate(longArrayOf(0, 400, 200, 400, 200, 600))
            .setSound(soundUri)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .setTicker(title)
            .setWhen(System.currentTimeMillis())
            .setChannelId(channelId)
    }

    // ── PendingIntent 工厂 ────────────────────────────────────

    private fun createTaskDetailPendingIntent(taskId: String): PendingIntent {
        return PendingIntent.getActivity(
            context,
            ReminderConstants.notificationId(taskId),
            buildTaskDetailIntent(taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createHabitDetailPendingIntent(habitId: String): PendingIntent {
        return PendingIntent.getActivity(
            context,
            ReminderConstants.habitNotificationId(habitId),
            buildHabitDetailIntent(habitId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createReminderAlertPendingIntent(
        notificationId: Int,
        title: String,
        body: String,
        taskId: String?,
        habitId: String?,
    ): PendingIntent {
        return PendingIntent.getActivity(
            context,
            notificationId * 37,
            buildReminderAlertIntent(
                notificationId = notificationId,
                title = title,
                body = body,
                taskId = taskId,
                habitId = habitId,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val TAG = "ReminderNotificationManager"
    }
}
