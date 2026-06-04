package luzzr.zou.data.backup

import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import luzzr.zou.core.markdown.MarkdownImageReferenceParser
import luzzr.zou.core.reminder.ReminderScheduler
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.ZouDatabase
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRepositoryImplTest {

    private lateinit var database: ZouDatabase
    private lateinit var repository: BackupRepositoryImpl
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var reminderScheduler: FakeReminderScheduler
    private lateinit var exportedZip: File
    private lateinit var importZip: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            ZouDatabase::class.java,
        ).allowMainThreadQueries().build()
        settingsRepository = FakeSettingsRepository()
        reminderScheduler = FakeReminderScheduler()
        repository = BackupRepositoryImpl(
            context = context,
            database = database,
            taskDao = database.taskDao(),
            habitDao = database.habitDao(),
            noteDao = database.noteDao(),
            mediaDao = database.mediaDao(),
            settingsRepository = settingsRepository,
            reminderScheduler = reminderScheduler,
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

    @Test
    fun importBackupKeepsCommittedDataAndWarnsWhenSettingsRestoreFails() = runBlocking {
        writeValidImportBackup()
        settingsRepository.failOnReplace = true

        val result = repository.importBackup(Uri.fromFile(importZip).toString())

        assertTrue(result.success)
        assertTrue(result.warnings.any { it.contains("设置恢复失败") })
        assertTrue(database.taskDao().getAllTaskEntities().any { it.id == IMPORTED_TASK_ID })
    }

    @Test
    fun importBackupKeepsCommittedDataAndWarnsWhenReminderRescheduleFails() = runBlocking {
        writeValidImportBackup()
        reminderScheduler.failOnReschedule = true

        val result = repository.importBackup(Uri.fromFile(importZip).toString())

        assertTrue(result.success)
        assertTrue(result.warnings.any { it.contains("提醒重排失败") })
        assertTrue(database.taskDao().getAllTaskEntities().any { it.id == IMPORTED_TASK_ID })
    }

    @Test
    fun importBackupPropagatesCancellationAfterDatabaseCommit() = runBlocking {
        writeValidImportBackup()
        settingsRepository.cancelOnReplace = true

        val failure = runCatching {
            repository.importBackup(Uri.fromFile(importZip).toString())
        }.exceptionOrNull()

        assertTrue(failure is CancellationException)
        assertTrue(database.taskDao().getAllTaskEntities().any { it.id == IMPORTED_TASK_ID })
        assertTrue(
            !File(
                InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "backup_import_${FakeTimeProvider.NOW_MILLIS}",
            ).exists(),
        )
    }

    private fun writeValidImportBackup() {
        ZipOutputStream(importZip.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(
                """
                {
                  "version": 3,
                  "exportedAt": 10,
                  "tasks": [{
                    "id": "$IMPORTED_TASK_ID",
                    "title": "Imported task",
                    "priority": "NORMAL",
                    "isUrgent": false,
                    "status": "ACTIVE",
                    "completionRule": "MANUAL",
                    "createdAt": 1,
                    "updatedAt": 2
                  }],
                  "settings": {
                    "defaultStartDestination": "tasks",
                    "settingsUpdatedAt": 2
                  }
                }
                """.trimIndent().toByteArray(Charsets.UTF_8),
            )
            zip.closeEntry()
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val state = MutableStateFlow(ReminderPreferences())
        var failOnReplace = false
        var cancelOnReplace = false

        override fun observeReminderPreferences(): Flow<ReminderPreferences> = state

        override suspend fun getReminderPreferences(): ReminderPreferences = state.value

        override suspend fun updateReminderPreferences(
            transform: (ReminderPreferences) -> ReminderPreferences,
        ) {
            state.value = transform(state.value)
        }

        override suspend fun replaceReminderPreferences(preferences: ReminderPreferences) {
            if (cancelOnReplace) {
                throw CancellationException("settings write cancelled")
            }
            check(!failOnReplace) { "settings write failed" }
            state.value = preferences
        }
    }

    private class FakeReminderScheduler : ReminderScheduler {
        var failOnReschedule = false

        override suspend fun scheduleTask(taskId: String) = Unit

        override suspend fun cancelTask(taskId: String) = Unit

        override suspend fun scheduleHabit(habitId: String) = Unit

        override suspend fun cancelHabit(habitId: String) = Unit

        override suspend fun rescheduleAllActiveReminders() {
            check(!failOnReschedule) { "reminder reschedule failed" }
        }
    }

    private class FakeTimeProvider : TimeProvider {
        override fun nowMillis(): Long = NOW_MILLIS

        override fun zoneId(): ZoneId = ZoneId.of("Asia/Singapore")

        companion object {
            const val NOW_MILLIS = 1_763_000_000_000L
        }
    }

    private companion object {
        const val IMPORTED_TASK_ID = "imported-task"
    }
}
