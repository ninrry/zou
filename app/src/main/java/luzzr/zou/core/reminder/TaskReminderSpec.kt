package luzzr.zou.core.reminder

import java.time.LocalDate

data class TaskReminderSpec(
    val taskId: String,
    val title: String,
    val isCompleted: Boolean,
    val isDeleted: Boolean,
    val archived: Boolean,
    val startReminderMinuteOfDay: Int?,
    val windowEndMinuteOfDay: Int?,
    val dueAt: Long?,
    val repeatIntervalMinutes: Int?,
    val exactReminderTimes: List<Long>,
    val allDay: Boolean,
    /** 提醒激活开始日期（含），null 表示无限制 */
    val reminderActiveFrom: LocalDate? = null,
    /** 提醒激活结束日期（含），null 表示无限制 */
    val reminderActiveTo: LocalDate? = null,
) {
    val hasReminderConfiguration: Boolean
        get() = startReminderMinuteOfDay != null ||
            repeatIntervalMinutes != null ||
            exactReminderTimes.isNotEmpty()
}
