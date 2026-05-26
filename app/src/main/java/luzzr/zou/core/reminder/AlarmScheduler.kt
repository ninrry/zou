package luzzr.zou.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleAlarm(
        taskId: String,
        habitId: String?,
        triggerAtMillis: Long,
        type: AlarmType,
    ) {
        val intent = Intent(context, AlarmReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_TRIGGER_AT, triggerAtMillis)
            putExtra(EXTRA_ALARM_TYPE, type.name)
        }
        val requestCode = if (habitId != null) ("habit_" + habitId).hashCode() else ("task_" + taskId).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                // 没有精确闹钟权限时，用普通闹钟（精度降低但仍可用）
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    fun cancelAlarm(taskId: String, habitId: String?) {
        val requestCode = if (habitId != null) ("habit_" + habitId).hashCode() else ("task_" + taskId).hashCode()
        val intent = Intent(context, AlarmReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    companion object {
        const val EXTRA_TASK_ID = "alarm_task_id"
        const val EXTRA_HABIT_ID = "alarm_habit_id"
        const val EXTRA_TRIGGER_AT = "alarm_trigger_at"
        const val EXTRA_ALARM_TYPE = "alarm_type"
    }
}

enum class AlarmType {
    TASK,
    HABIT,
}
