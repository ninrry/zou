package luzzr.zou.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import luzzr.zou.core.markdown.MarkdownImageReferenceParser
import luzzr.zou.core.reminder.ReminderScheduler
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.data.local.database.ZouDatabase
import luzzr.zou.data.local.database.dao.HabitDao
import luzzr.zou.data.local.database.dao.MediaDao
import luzzr.zou.data.local.database.dao.NoteDao
import luzzr.zou.data.local.database.dao.TaskDao
import luzzr.zou.data.local.database.entity.HabitEntity
import luzzr.zou.data.local.database.entity.HabitRecordEntity
import luzzr.zou.data.local.database.entity.HabitStepEntity
import luzzr.zou.data.local.database.entity.MediaEntity
import luzzr.zou.data.local.database.entity.NoteEntity
import luzzr.zou.data.local.database.entity.SubTaskEntity
import luzzr.zou.data.local.database.entity.TaskEntity
import luzzr.zou.data.settings.ReminderPreferences
import luzzr.zou.domain.repository.BackupOperationResult
import luzzr.zou.domain.repository.BackupRepository
import luzzr.zou.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ZouDatabase,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val noteDao: NoteDao,
    private val mediaDao: MediaDao,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val markdownImageReferenceParser: MarkdownImageReferenceParser,
    private val timeProvider: TimeProvider,
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun exportBackup(destinationUri: String): BackupOperationResult {
        val uri = Uri.parse(destinationUri)
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: return BackupOperationResult(false, "无法创建备份文件。")
        val exportedAt = timeProvider.nowMillis()
        val settings = settingsRepository.getReminderPreferences()
        val payload = database.withTransaction {
            BackupPayload(
                version = BACKUP_VERSION,
                exportedAt = exportedAt,
                tasks = taskDao.getAllTaskEntities().map { it.toPayload() },
                subTasks = taskDao.getAllSubTaskEntities().map { it.toPayload() },
                habits = habitDao.getAllHabits().map { it.toPayload() },
                habitSteps = habitDao.getAllHabitSteps().map { it.toPayload() },
                habitRecords = habitDao.getAllHabitRecords().map { it.toPayload() },
                notes = noteDao.getAllNotes().map { it.toPayload() },
                media = mediaDao.getAllMedia().map { it.toPayload() },
                settings = settings.toPayload(),
            )
        }
        val warnings = mutableListOf<String>()

        outputStream.use { stream ->
            ZipOutputStream(stream).use { zip ->
                zip.putNextEntry(ZipEntry(BACKUP_JSON_NAME))
                zip.write(json.encodeToString(payload).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                payload.media.forEach { media ->
                    val sourceFile = File(media.localPath)
                    if (!sourceFile.exists() || sourceFile.isDirectory) {
                        warnings += "媒体文件缺失：${media.id}"
                        return@forEach
                    }
                    val entryPath = mediaZipPath(
                        ownerType = media.ownerType,
                        ownerId = media.ownerId,
                        fileName = sourceFile.name,
                    )
                    zip.putNextEntry(ZipEntry(entryPath))
                    sourceFile.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        return BackupOperationResult(
            success = true,
            message = "备份导出完成，格式版本 v$BACKUP_VERSION。",
            warnings = warnings,
        )
    }

    override suspend fun importBackup(sourceUri: String): BackupOperationResult {
        val uri = Uri.parse(sourceUri)
        val tempDirectory = File(context.cacheDir, "backup_import_${timeProvider.nowMillis()}").apply { mkdirs() }
        val warnings = mutableListOf<String>()

        return runCatching {
            val payload = extractBackup(uri, tempDirectory)
            if (payload.version > BACKUP_VERSION) {
                return BackupOperationResult(false, "备份版本过新，当前仅支持导入 v$BACKUP_VERSION。")
            }
            if (payload.version < 1) {
                return BackupOperationResult(false, "暂不支持导入该备份版本。")
            }

            database.withTransaction {
                mergeTasks(payload.tasks)
                mergeSubTasks(payload.subTasks)
                mergeHabits(payload.habits)
                mergeHabitSteps(payload.habitSteps)
                mergeHabitRecords(payload.habitRecords)
                mergeNotes(payload.notes)
                mergeMedia(payload.media, tempDirectory, warnings)
                mergeSettings(payload.settings)
            }

            reminderScheduler.rescheduleAllActiveReminders()
            BackupOperationResult(
                success = true,
                message = "备份导入完成，已按 ID 和 updatedAt 合并。",
                warnings = warnings,
            )
        }.getOrElse { throwable ->
            BackupOperationResult(
                success = false,
                message = throwable.message ?: "备份导入失败。",
                warnings = warnings,
            )
        }.also {
            tempDirectory.deleteRecursively()
        }
    }

    private fun extractBackup(
        uri: Uri,
        tempDirectory: File,
    ): BackupPayload {
        var payload: BackupPayload? = null
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取备份文件。" }
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        if (entry.name == BACKUP_JSON_NAME) {
                            payload = json.decodeFromString<BackupPayload>(zip.readBytes().toString(Charsets.UTF_8))
                        } else {
                            val target = File(tempDirectory, entry.name).apply {
                                parentFile?.mkdirs()
                            }
                            target.outputStream().use { output -> zip.copyTo(output) }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return requireNotNull(payload) { "备份文件缺少 backup.json。" }
    }

    private suspend fun mergeTasks(imported: List<TaskPayload>) {
        val localById = taskDao.getAllTaskEntities().associateBy { it.id }
        imported.forEach { item ->
            val local = localById[item.id]
            if (local == null || shouldImport(item.updatedAt, local.updatedAt)) {
                taskDao.upsertTask(item.toEntity())
            }
        }
    }

    private suspend fun mergeSubTasks(imported: List<SubTaskPayload>) {
        val localById = taskDao.getAllSubTaskEntities().associateBy { it.id }
        val merged = imported.filter { item ->
            val local = localById[item.id]
            local == null || shouldImport(item.updatedAt, local.updatedAt)
        }
        if (merged.isNotEmpty()) {
            taskDao.upsertSubTasks(merged.map { it.toEntity() })
        }
    }

    private suspend fun mergeHabits(imported: List<HabitPayload>) {
        val localById = habitDao.getAllHabits().associateBy { it.id }
        imported.forEach { item ->
            val local = localById[item.id]
            if (local == null || shouldImport(item.updatedAt, local.updatedAt)) {
                habitDao.upsertHabit(item.toEntity())
            }
        }
    }

    private suspend fun mergeHabitSteps(imported: List<HabitStepPayload>) {
        val localById = habitDao.getAllHabitSteps().associateBy { it.id }
        val merged = imported.filter { item ->
            val local = localById[item.id]
            local == null || shouldImport(item.updatedAt, local.updatedAt)
        }
        if (merged.isNotEmpty()) {
            habitDao.upsertHabitSteps(merged.map { it.toEntity() })
        }
    }

    private suspend fun mergeHabitRecords(imported: List<HabitRecordPayload>) {
        val localById = habitDao.getAllHabitRecords().associateBy { it.id }
        imported.forEach { item ->
            val local = localById[item.id]
            if (local == null || shouldImport(item.updatedAt, local.updatedAt)) {
                habitDao.upsertHabitRecord(item.toEntity())
            }
        }
    }

    private suspend fun mergeNotes(imported: List<NotePayload>) {
        val localById = noteDao.getAllNotes().associateBy { it.id }
        imported.forEach { item ->
            val local = localById[item.id]
            if (local == null || shouldImport(item.updatedAt, local.updatedAt)) {
                noteDao.upsertNote(item.toEntity())
            }
        }
    }

    private suspend fun mergeMedia(
        imported: List<MediaPayload>,
        tempDirectory: File,
        warnings: MutableList<String>,
    ) {
        val localById = mediaDao.getAllMedia().associateBy { it.id }
        val mergedMedia = mutableListOf<MediaEntity>()
        imported.forEach { item ->
            val local = localById[item.id]
            if (local != null && !shouldImport(item.updatedAt, local.updatedAt)) {
                return@forEach
            }

            val importedPath = item.backupFileName?.let { fileName ->
                File(tempDirectory, mediaZipPath(item.ownerType, item.ownerId, fileName))
            }
            val resolvedPath = when {
                importedPath == null -> item.localPath
                importedPath.exists() -> restoreMediaFile(
                    ownerType = item.ownerType,
                    ownerId = item.ownerId,
                    mediaId = item.id,
                    source = importedPath,
                )
                else -> {
                    warnings += "备份中缺少媒体文件：${item.id}"
                    local?.localPath ?: item.localPath
                }
            }
            mergedMedia += item.toEntity(localPath = resolvedPath)
        }
        if (mergedMedia.isNotEmpty()) {
            mediaDao.upsertMedia(mergedMedia)
        }
        cleanupUnreferencedRestoredMedia()
    }

    private suspend fun mergeSettings(imported: ReminderPreferencesPayload) {
        val local = settingsRepository.getReminderPreferences()
        if (shouldImport(imported.settingsUpdatedAt, local.settingsUpdatedAt)) {
            settingsRepository.replaceReminderPreferences(imported.toDomain())
        }
    }

    private fun shouldImport(importedUpdatedAt: Long, localUpdatedAt: Long): Boolean {
        return importedUpdatedAt > localUpdatedAt
    }

    private fun restoreMediaFile(
        ownerType: String,
        ownerId: String,
        mediaId: String,
        source: File,
    ): String {
        val extension = source.extension.ifBlank { "bin" }
        val ownerDirectory = when (ownerType) {
            "note" -> File(context.filesDir, "media/notes/$ownerId")
            else -> File(context.filesDir, "media/$ownerType/$ownerId")
        }.apply { mkdirs() }
        val target = File(ownerDirectory, "$mediaId.$extension")
        source.inputStream().use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return target.absolutePath
    }

    private suspend fun cleanupUnreferencedRestoredMedia() {
        val notesById = noteDao.getAllNotes().associateBy { it.id }
        mediaDao.getAllMedia()
            .filter { it.ownerType == "note" && !it.isDeleted }
            .forEach { media ->
                val note = notesById[media.ownerId]
                val referenced = note?.contentMarkdown
                    ?.let(markdownImageReferenceParser::extractMediaIds)
                    .orEmpty()
                if (media.id !in referenced) {
                    mediaDao.softDeleteMediaByIds(listOf(media.id), timeProvider.nowMillis())
                }
            }
    }

    private fun mediaZipPath(
        ownerType: String,
        ownerId: String,
        fileName: String,
    ): String = when (ownerType) {
        "note" -> "media/notes/$ownerId/$fileName"
        else -> "media/$ownerType/$ownerId/$fileName"
    }

    private fun TaskEntity.toPayload() = TaskPayload(
        id = id,
        title = title,
        contentMarkdown = contentMarkdown,
        priority = priority,
        isUrgent = isUrgent,
        status = status,
        startRemindAt = startRemindAt,
        remindWindowEndAt = remindWindowEndAt,
        startReminderMinuteOfDay = startReminderMinuteOfDay,
        windowEndMinuteOfDay = windowEndMinuteOfDay,
        dueAt = dueAt,
        repeatIntervalMinutes = repeatIntervalMinutes,
        exactReminderTimesJson = exactReminderTimesJson,
        allDay = allDay,
        completionRule = completionRule,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        tags = tags,
        archived = archived,
        reminderNotificationTitle = reminderNotificationTitle,
        reminderNotificationBody = reminderNotificationBody,
    )

    private fun SubTaskEntity.toPayload() = SubTaskPayload(
        id = id,
        taskId = taskId,
        title = title,
        sortOrder = sortOrder,
        isCompleted = isCompleted,
        completedAt = completedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    private fun HabitEntity.toPayload() = HabitPayload(
        id = id,
        title = title,
        contentMarkdown = contentMarkdown,
        frequencyType = frequencyType,
        frequencyValueJson = frequencyValueJson,
        remindWindowStart = remindWindowStart,
        remindWindowEnd = remindWindowEnd,
        repeatIntervalMinutes = repeatIntervalMinutes,
        exactReminderTimesJson = exactReminderTimesJson,
        checkInMode = checkInMode,
        targetDurationMinutes = targetDurationMinutes,
        streakCountCache = streakCountCache,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        tags = tags,
        archived = archived,
        reminderNotificationTitle = reminderNotificationTitle,
        reminderNotificationBody = reminderNotificationBody,
    )

    private fun HabitStepEntity.toPayload() = HabitStepPayload(
        id = id,
        habitId = habitId,
        title = title,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    private fun HabitRecordEntity.toPayload() = HabitRecordPayload(
        id = id,
        habitId = habitId,
        recordDate = recordDate,
        status = status,
        durationMinutes = durationMinutes,
        stepProgressJson = stepProgressJson,
        durationElapsedSeconds = durationElapsedSeconds,
        durationRunningSinceMillis = durationRunningSinceMillis,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun NoteEntity.toPayload() = NotePayload(
        id = id,
        title = title,
        contentMarkdown = contentMarkdown,
        previewText = previewText,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        tags = tags,
        archived = archived,
    )

    private fun MediaEntity.toPayload() = MediaPayload(
        id = id,
        ownerType = ownerType,
        ownerId = ownerId,
        localPath = localPath,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        backupFileName = File(localPath).name,
    )

    private fun ReminderPreferences.toPayload() = ReminderPreferencesPayload(
        defaultTaskRepeatIntervalMinutes = defaultTaskRepeatIntervalMinutes,
        defaultHabitRepeatIntervalMinutes = defaultHabitRepeatIntervalMinutes,
        showCompletedTasks = showCompletedTasks,
        showOnlyTodayHabits = showOnlyTodayHabits,
        showDeletedHabits = showDeletedHabits,
        settingsUpdatedAt = settingsUpdatedAt,
    )

    private fun TaskPayload.toEntity() = TaskEntity(
        id = id,
        title = title,
        contentMarkdown = contentMarkdown,
        priority = priority,
        isUrgent = isUrgent,
        status = status,
        startRemindAt = startRemindAt,
        remindWindowEndAt = remindWindowEndAt,
        startReminderMinuteOfDay = startReminderMinuteOfDay,
        windowEndMinuteOfDay = windowEndMinuteOfDay,
        dueAt = dueAt,
        repeatIntervalMinutes = repeatIntervalMinutes,
        exactReminderTimesJson = exactReminderTimesJson,
        allDay = allDay,
        completionRule = completionRule,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        tags = tags,
        archived = archived,
        reminderNotificationTitle = reminderNotificationTitle,
        reminderNotificationBody = reminderNotificationBody,
    )

    private fun SubTaskPayload.toEntity() = SubTaskEntity(
        id = id,
        taskId = taskId,
        title = title,
        sortOrder = sortOrder,
        isCompleted = isCompleted,
        completedAt = completedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    private fun HabitPayload.toEntity() = HabitEntity(
        id = id,
        title = title,
        contentMarkdown = contentMarkdown,
        frequencyType = frequencyType,
        frequencyValueJson = frequencyValueJson,
        remindWindowStart = remindWindowStart,
        remindWindowEnd = remindWindowEnd,
        repeatIntervalMinutes = repeatIntervalMinutes,
        exactReminderTimesJson = exactReminderTimesJson,
        checkInMode = checkInMode,
        targetDurationMinutes = targetDurationMinutes,
        streakCountCache = streakCountCache,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        tags = tags,
        archived = archived,
        reminderNotificationTitle = reminderNotificationTitle,
        reminderNotificationBody = reminderNotificationBody,
    )

    private fun HabitStepPayload.toEntity() = HabitStepEntity(
        id = id,
        habitId = habitId,
        title = title,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    private fun HabitRecordPayload.toEntity() = HabitRecordEntity(
        id = id,
        habitId = habitId,
        recordDate = recordDate,
        status = status,
        durationMinutes = durationMinutes,
        stepProgressJson = stepProgressJson,
        durationElapsedSeconds = durationElapsedSeconds,
        durationRunningSinceMillis = durationRunningSinceMillis,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun NotePayload.toEntity() = NoteEntity(
        id = id,
        title = title,
        contentMarkdown = contentMarkdown,
        previewText = previewText,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        tags = tags,
        archived = archived,
    )

    private fun MediaPayload.toEntity(localPath: String) = MediaEntity(
        id = id,
        ownerType = ownerType,
        ownerId = ownerId,
        localPath = localPath,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    private fun ReminderPreferencesPayload.toDomain() = ReminderPreferences(
        defaultTaskRepeatIntervalMinutes = defaultTaskRepeatIntervalMinutes,
        defaultHabitRepeatIntervalMinutes = defaultHabitRepeatIntervalMinutes,
        showCompletedTasks = showCompletedTasks,
        showOnlyTodayHabits = showOnlyTodayHabits,
        showDeletedHabits = showDeletedHabits,
        settingsUpdatedAt = settingsUpdatedAt,
    )

    @Serializable
    private data class BackupPayload(
        val version: Int,
        val exportedAt: Long,
        val tasks: List<TaskPayload> = emptyList(),
        val subTasks: List<SubTaskPayload> = emptyList(),
        val habits: List<HabitPayload> = emptyList(),
        val habitSteps: List<HabitStepPayload> = emptyList(),
        val habitRecords: List<HabitRecordPayload> = emptyList(),
        val notes: List<NotePayload> = emptyList(),
        val media: List<MediaPayload> = emptyList(),
        val settings: ReminderPreferencesPayload = ReminderPreferencesPayload(),
    )

    @Serializable
    private data class TaskPayload(
        val id: String,
        val title: String,
        val contentMarkdown: String? = null,
        val priority: String,
        val isUrgent: Boolean,
        val status: String,
        val startRemindAt: Long? = null,
        val remindWindowEndAt: Long? = null,
        val startReminderMinuteOfDay: Int? = null,
        val windowEndMinuteOfDay: Int? = null,
        val dueAt: Long? = null,
        val repeatIntervalMinutes: Int? = null,
        val exactReminderTimesJson: String? = null,
        val allDay: Boolean = false,
        val completionRule: String,
        val createdAt: Long,
        val updatedAt: Long,
        val isDeleted: Boolean = false,
        val deletedAt: Long? = null,
        val tags: String? = null,
        val archived: Boolean = false,
        val reminderNotificationTitle: String? = null,
        val reminderNotificationBody: String? = null,
    )

    @Serializable
    private data class SubTaskPayload(
        val id: String,
        val taskId: String,
        val title: String,
        val sortOrder: Int,
        val isCompleted: Boolean,
        val completedAt: Long? = null,
        val createdAt: Long,
        val updatedAt: Long,
        val isDeleted: Boolean = false,
    )

    @Serializable
    private data class HabitPayload(
        val id: String,
        val title: String,
        val contentMarkdown: String? = null,
        val frequencyType: String,
        val frequencyValueJson: String? = null,
        val remindWindowStart: String? = null,
        val remindWindowEnd: String? = null,
        val repeatIntervalMinutes: Int? = null,
        val exactReminderTimesJson: String? = null,
        val checkInMode: String,
        val targetDurationMinutes: Int? = null,
        val streakCountCache: Int,
        val createdAt: Long,
        val updatedAt: Long,
        val isDeleted: Boolean = false,
        val deletedAt: Long? = null,
        val tags: String? = null,
        val archived: Boolean = false,
        val reminderNotificationTitle: String? = null,
        val reminderNotificationBody: String? = null,
    )

    @Serializable
    private data class HabitStepPayload(
        val id: String,
        val habitId: String,
        val title: String,
        val sortOrder: Int,
        val createdAt: Long,
        val updatedAt: Long,
        val isDeleted: Boolean = false,
    )

    @Serializable
    private data class HabitRecordPayload(
        val id: String,
        val habitId: String,
        val recordDate: Long,
        val status: String,
        val durationMinutes: Int? = null,
        val stepProgressJson: String? = null,
        val durationElapsedSeconds: Long = 0L,
        val durationRunningSinceMillis: Long? = null,
        val createdAt: Long,
        val updatedAt: Long,
    )

    @Serializable
    private data class NotePayload(
        val id: String,
        val title: String,
        val contentMarkdown: String? = null,
        val previewText: String? = null,
        val createdAt: Long,
        val updatedAt: Long,
        val lastOpenedAt: Long? = null,
        val isDeleted: Boolean = false,
        val deletedAt: Long? = null,
        val tags: String? = null,
        val archived: Boolean = false,
    )

    @Serializable
    private data class MediaPayload(
        val id: String,
        val ownerType: String,
        val ownerId: String,
        val localPath: String,
        val mimeType: String,
        val sizeBytes: Long = 0L,
        val createdAt: Long,
        val updatedAt: Long,
        val isDeleted: Boolean = false,
        val backupFileName: String? = null,
    )

    @Serializable
    private data class ReminderPreferencesPayload(
        val defaultTaskRepeatIntervalMinutes: Int = 60,
        val defaultHabitRepeatIntervalMinutes: Int = 60,
        val showCompletedTasks: Boolean = false,
        val showOnlyTodayHabits: Boolean = false,
        val showDeletedHabits: Boolean = false,
        val settingsUpdatedAt: Long = 0L,
    )

    private companion object {
        const val BACKUP_VERSION = 4
        const val BACKUP_JSON_NAME = "backup.json"
    }
}
