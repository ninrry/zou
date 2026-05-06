package luzzr.zou.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
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

@Database(
    entities = [
        TaskEntity::class,
        SubTaskEntity::class,
        HabitEntity::class,
        HabitStepEntity::class,
        HabitRecordEntity::class,
        NoteEntity::class,
        MediaEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class ZouDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun noteDao(): NoteDao
    abstract fun mediaDao(): MediaDao
}
