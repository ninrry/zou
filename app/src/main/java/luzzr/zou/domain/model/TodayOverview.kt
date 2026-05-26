package luzzr.zou.domain.model

data class TodayOverview(
    val summary: TodaySummary,
    val tasks: List<TodayTaskItem>,
    val habits: List<TodayHabitItem>,
    val recentNotes: List<RecentNoteItem>,
    val isCompletelyEmpty: Boolean,
)

data class TodaySummary(
    val pendingTaskCount: Int,
    val dueHabitCount: Int,
    val completedCount: Int,
)

data class TodayTaskItem(
    val task: Task,
)

data class TodayHabitItem(
    val habit: Habit,
    val todayStatus: HabitTodayStatus,
)

data class RecentNoteItem(
    val note: Note,
)
