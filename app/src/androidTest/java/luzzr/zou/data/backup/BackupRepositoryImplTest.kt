package luzzr.zou.data.backup

import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import luzzr.zou.core.markdown.MarkdownImageReferenceParser
import luzzr.zou.core.reminder.ReminderScheduler
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.NoteFlowDatabase
import luzzr.zou.data.local.database.entity.TaskEntity
import luzzr.zou.data.settings.ReminderPreferences
import luzzr.zou.domain.repository.SettingsRepository
import java.io.File
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRepositoryImplTest {

    private lateinit var database: NoteFlowDatabase
    private lateinit var repository: BackupRepositoryImpl
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var exportedZip: File
    private lateinit var importZip: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            NoteFlowDatabase::class.java,
        ).allowMainThreadQueries().build()
        settingsRepository = FakeSettingsRepository()
        repository = BackupRepositoryImpl(
            context = context,
            database = database,
            taskDao = database.taskDao(),
            habitDao = database.habitDao(),
            noteDao = database.noteDao(),
            mediaDao = database.mediaDao(),
            settingsRepository = settingsRepository,
            reminderScheduler = FakeReminderScheduler(),
            markdownImageReferenceParser = MarkdownImageReferenceParser(),
            timeProvider = FakeTimeProvider(),
        )
        exportedZip = File(context.cacheDir, "backup-export-test.zip")
        importZip = File(context.cacheDir, "backup-import-test.zip")
        exportedZip.delete()
        importZip.delete()
    }

    @After
    fun tearDown() {
        database.close()
        exportedZip.delete()
        importZip.delete()
    }

    @Test
    fun exportBackupWritesBackupJsonIntoZip() = runBlocking {
        database.taskDao().upsertTask(
            TaskEntity(
                id = "task-1",
                title = "Exported task",
                createdAt = 1L,
                updatedAt = 2L,
            ),
        )

        val result = repository.exportBackup(Uri.fromFile(exportedZip).toString())

        assertTrue(result.success)
        ZipFile(exportedZip).use { zip ->
            assertTrue(zip.getEntry("backup.json") != null)
        }
    }

    @Test
    fun importBackupRejectsNewerVersion() = runBlocking {
        ZipOutputStream(importZip.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write("""{"version":4,"exportedAt":1}""".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        val result = repository.importBackup(Uri.fromFile(importZip).toString())

        assertEquals(false, result.success)
        assertTrue(result.message.contains("版本"))
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val state = MutableStateFlow(ReminderPreferences())

        override fun observeReminderPreferences(): Flow<ReminderPreferences> = state

        override suspend fun getReminderPreferences(): ReminderPreferences = state.value

        override suspend fun updateReminderPreferences(
            transform: (ReminderPreferences) -> ReminderPreferences,
        ) {
            state.value = transform(state.value)
        }

        override suspend fun replaceReminderPreferences(preferences: ReminderPreferences) {
            state.value = preferences
        }
    }

    private class FakeReminderScheduler : ReminderScheduler {
        override suspend fun scheduleTask(taskId: String) = Unit

        override suspend fun cancelTask(taskId: String) = Unit

        override suspend fun scheduleHabit(habitId: String) = Unit

        override suspend fun cancelHabit(habitId: String) = Unit

        override suspend fun rescheduleAllActiveReminders() = Unit
    }

    private class FakeTimeProvider : TimeProvider {
        override fun nowMillis(): Long = 1_763_000_000_000L

        override fun zoneId(): ZoneId = ZoneId.of("Asia/Singapore")
    }
}
