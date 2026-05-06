package luzzr.zou.di

import android.content.Context
import androidx.room.Room
import luzzr.zou.data.local.database.ZouDatabase
import luzzr.zou.data.local.database.ZouMigrations
import luzzr.zou.data.local.database.dao.HabitDao
import luzzr.zou.data.local.database.dao.MediaDao
import luzzr.zou.data.local.database.dao.NoteDao
import luzzr.zou.data.local.database.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideZouDatabase(
        @ApplicationContext context: Context,
    ): ZouDatabase = Room.databaseBuilder(
        context,
        ZouDatabase::class.java,
        "noteflow.db",
    )
        .addMigrations(ZouMigrations.MIGRATION_1_2)
        .addMigrations(ZouMigrations.MIGRATION_2_3)
        .addMigrations(ZouMigrations.MIGRATION_3_4)
        .build()

    @Provides
    fun provideTaskDao(database: ZouDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideHabitDao(database: ZouDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideNoteDao(database: ZouDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideMediaDao(database: ZouDatabase): MediaDao = database.mediaDao()
}
