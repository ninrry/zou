package luzzr.zou.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import luzzr.zou.core.reminder.HabitReminderTimeCodec
import luzzr.zou.core.reminder.ReminderDispatchQueue
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.NoteFlowDatabase
import luzzr.zou.domain.model.Habit
import luzzr.zou.domain.model.HabitCheckInMode
import luzzr.zou.domain.model.HabitFrequencyType
import luzzr.zou.domain.model.HabitRecordStatus
import luzzr.zou.domain.model.HabitStep
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitRepositoryImplTest {

    private lateinit var database: NoteFlowDatabase
    private lateinit var repository: HabitRepositoryImpl
    private lateinit var reminderDispatchQueue: FakeReminderDispatchQueue
    private val timeProvider = FixedTimeProvider()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            NoteFlowDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        reminderDispatchQueue = FakeReminderDispatchQueue()
        repository = HabitRepositoryImpl(
            habitDao = database.habitDao(),
            reminderDispatchQueue = reminderDispatchQueue,
            habitReminderTimeCodec = HabitReminderTimeCodec(),
            timeProvider = timeProvider,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savesWeeklyHabitAndRestoresStepsFromDatabase() = runBlocking {
        val habitId = UUID.randomUUID().toString()
        repository.saveHabit(
            habit = habit(
                id = habitId,
                title = "Workout",
                frequencyType = HabitFrequencyType.WEEKLY,
                selectedWeekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                checkInMode = HabitCheckInMode.STEPS,
            ),
            steps = listOf(
                HabitStep(id = UUID.randomUUID().toString(), title = "Warm up", sortOrder = 0),
                HabitStep(id = UUID.randomUUID().toString(), title = "Stretch", sortOrder = 1),
            ),
        )

        val storedHabit = repository.getHabit(habitId, todayEpochDay())

        assertEquals(HabitFrequencyType.WEEKLY, storedHabit?.frequencyType)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), storedHabit?.selectedWeekdays)
        assertEquals(listOf("Warm up", "Stretch"), storedHabit?.steps?.map { it.title })
    }

    @Test
    fun softDeleteAndRestoreControlsActivityList() = runBlocking {
        val habitId = UUID.randomUUID().toString()
        repository.saveHabit(
            habit = habit(id = habitId, title = "Meditate"),
            steps = emptyList(),
        )

        repository.softDeleteHabit(habitId)

        assertTrue(repository.observeHabits(todayEpochDay(), includeDeleted = false).first().isEmpty())
        val deletedHabit = repository.observeHabits(todayEpochDay(), includeDeleted = true)
            .first()
            .single()
        assertTrue(deletedHabit.isDeleted)

        repository.restoreHabit(habitId)

        val restoredHabit = repository.observeHabits(todayEpochDay(), includeDeleted = false)
            .first()
            .single()
        assertFalse(restoredHabit.isDeleted)
    }

    @Test
    fun hardDeleteRemovesHabitStepsAndRecords() = runBlocking {
        val habitId = UUID.randomUUID().toString()
        repository.saveHabit(
            habit = habit(
                id = habitId,
                title = "Journal",
                checkInMode = HabitCheckInMode.STEPS,
            ),
            steps = listOf(
                HabitStep(id = UUID.randomUUID().toString(), title = "Open notebook", sortOrder = 0),
            ),
        )
        repository.completeStepsHabit(habitId, todayEpochDay())

        repository.hardDeleteHabit(habitId)

        assertTrue(database.habitDao().getHabitWithSteps(habitId, includeDeleted = true) == null)
        assertTrue(database.habitDao().getHabitRecordForDate(habitId, todayEpochDay()) == null)
    }

    @Test
    fun completeDurationHabitWritesCompletedRecord() = runBlocking {
        val habitId = UUID.randomUUID().toString()
        repository.saveHabit(
            habit = habit(
                id = habitId,
                title = "Run",
                checkInMode = HabitCheckInMode.DURATION,
                targetDurationMinutes = 30,
            ),
            steps = emptyList(),
        )

        repository.completeDurationHabit(habitId, todayEpochDay(), durationMinutes = 45)

        val storedHabit = repository.getHabit(habitId, todayEpochDay())
        assertEquals(HabitRecordStatus.COMPLETED, storedHabit?.todayRecord?.status)
        assertEquals(45, storedHabit?.todayRecord?.durationMinutes)
    }

    @Test
    fun completeStepsHabitWritesCompletedRecord() = runBlocking {
        val habitId = UUID.randomUUID().toString()
        repository.saveHabit(
            habit = habit(
                id = habitId,
                title = "Morning routine",
                checkInMode = HabitCheckInMode.STEPS,
            ),
            steps = listOf(
                HabitStep(id = UUID.randomUUID().toString(), title = "Drink water", sortOrder = 0),
            ),
        )

        repository.completeStepsHabit(habitId, todayEpochDay())

        val storedHabit = repository.getHabit(habitId, todayEpochDay())
        assertEquals(HabitRecordStatus.COMPLETED, storedHabit?.todayRecord?.status)
    }

    private fun habit(
        id: String = UUID.randomUUID().toString(),
        title: String,
        frequencyType: HabitFrequencyType = HabitFrequencyType.DAILY,
        selectedWeekdays: Set<DayOfWeek> = emptySet(),
        checkInMode: HabitCheckInMode = HabitCheckInMode.CHECK,
        targetDurationMinutes: Int? = null,
    ): Habit {
        return Habit(
            id = id,
            title = title,
            frequencyType = frequencyType,
            selectedWeekdays = selectedWeekdays,
            checkInMode = checkInMode,
            targetDurationMinutes = targetDurationMinutes,
        )
    }

    private fun todayEpochDay(): Long {
        return LocalDate.of(2026, 3, 9).toEpochDay()
    }

    private class FixedTimeProvider : TimeProvider {
        private val zoneId = ZoneId.of("Asia/Singapore")

        override fun nowMillis(): Long {
            return LocalDateTime.of(2026, 3, 9, 8, 0)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        }

        override fun zoneId(): ZoneId = zoneId
    }

    private class FakeReminderDispatchQueue : ReminderDispatchQueue {
        override fun scheduleTask(taskId: String) = Unit

        override fun cancelTask(taskId: String) = Unit

        override fun scheduleHabit(habitId: String) = Unit

        override fun cancelHabit(habitId: String) = Unit

        override fun rescheduleAllActiveReminders() = Unit
    }
}
