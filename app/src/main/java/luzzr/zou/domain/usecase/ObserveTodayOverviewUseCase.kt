package luzzr.zou.domain.usecase

import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.model.HabitTodayStatus
import luzzr.zou.domain.model.RecentNoteItem
import luzzr.zou.domain.model.TaskStatus
import luzzr.zou.domain.model.TodayHabitItem
import luzzr.zou.domain.model.TodayOverview
import luzzr.zou.domain.model.TodaySummary
import luzzr.zou.domain.model.TodayTaskItem
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveTodayOverviewUseCase @Inject constructor(
    private val observeTasksUseCase: ObserveTasksUseCase,
    private val observeHabitsUseCase: ObserveHabitsUseCase,
    private val observeNotesUseCase: ObserveNotesUseCase,
    private val habitScheduleEvaluator: HabitScheduleEvaluator,
    private val timeProvider: TimeProvider,
) {

    operator fun invoke(): Flow<TodayOverview> {
        return todayTickerFlow()
            .distinctUntilChanged()
            .flatMapLatest { today ->
                combine(
                    observeTasksUseCase(includeCompleted = true),
                    observeHabitsUseCase(recordDate = today.toEpochDay(), includeDeleted = false),
                    observeNotesUseCase(),
                ) { tasks, habits, notes ->
                    val activeTasks = tasks
                        .filter { it.status != TaskStatus.COMPLETED }
                        .map(::TodayTaskItem)

                    val completedTasksToday = tasks.count { task ->
                        task.status == TaskStatus.COMPLETED && task.updatedAt.isOn(today)
                    }

                    val todayHabits = habits
                        .map { habit ->
                            val todayStatus = habitScheduleEvaluator.getTodayStatus(
                                habit = habit,
                                date = today,
                                todayRecord = habit.todayRecord,
                            )
                            TodayHabitItem(
                                habit = habit,
                                todayStatus = todayStatus,
                            )
                        }
                        .filter { item ->
                            item.todayStatus == HabitTodayStatus.DUE ||
                                item.todayStatus == HabitTodayStatus.COMPLETED
                        }
                        .sortedWith(
                            compareBy<TodayHabitItem>(
                                { item ->
                                    when (item.todayStatus) {
                                        HabitTodayStatus.DUE -> 0
                                        HabitTodayStatus.COMPLETED -> 1
                                        HabitTodayStatus.NOT_DUE -> 2
                                        HabitTodayStatus.DELETED -> 3
                                    }
                                },
                                { item -> -item.habit.updatedAt },
                            ),
                        )

                    val dueHabitCount = todayHabits.count { it.todayStatus == HabitTodayStatus.DUE }
                    val completedHabitCount = todayHabits.count { it.todayStatus == HabitTodayStatus.COMPLETED }
                    val recentNotes = notes
                        .take(3)
                        .map(::RecentNoteItem)

                    TodayOverview(
                        summary = TodaySummary(
                            pendingTaskCount = activeTasks.size,
                            dueHabitCount = dueHabitCount,
                            completedCount = completedTasksToday + completedHabitCount,
                        ),
                        tasks = activeTasks,
                        habits = todayHabits,
                        recentNotes = recentNotes,
                        isCompletelyEmpty = activeTasks.isEmpty() &&
                            todayHabits.isEmpty() &&
                            recentNotes.isEmpty(),
                    )
                }
            }
    }

    private fun currentDate() = Instant.ofEpochMilli(timeProvider.nowMillis())
        .atZone(timeProvider.zoneId())
        .toLocalDate()

    private fun todayTickerFlow(): Flow<java.time.LocalDate> = flow {
        emit(currentDate())
        while (true) {
            delay(60_000L)
            emit(currentDate())
        }
    }

    private fun Long.isOn(date: java.time.LocalDate): Boolean {
        return Instant.ofEpochMilli(this)
            .atZone(timeProvider.zoneId())
            .toLocalDate() == date
    }
}
