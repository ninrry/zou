package luzzr.zou.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object ZouMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE tasks ADD COLUMN reminderNotificationTitle TEXT",
            )
            database.execSQL(
                "ALTER TABLE tasks ADD COLUMN reminderNotificationBody TEXT",
            )
            database.execSQL(
                "ALTER TABLE habits ADD COLUMN reminderNotificationTitle TEXT",
            )
            database.execSQL(
                "ALTER TABLE habits ADD COLUMN reminderNotificationBody TEXT",
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE habit_records ADD COLUMN stepProgressJson TEXT",
            )
            database.execSQL(
                "ALTER TABLE habit_records ADD COLUMN durationElapsedSeconds INTEGER NOT NULL DEFAULT 0",
            )
            database.execSQL(
                "ALTER TABLE habit_records ADD COLUMN durationRunningSinceMillis INTEGER",
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE tasks ADD COLUMN startReminderMinuteOfDay INTEGER",
            )
            database.execSQL(
                "ALTER TABLE tasks ADD COLUMN windowEndMinuteOfDay INTEGER",
            )
        }
    }
}
