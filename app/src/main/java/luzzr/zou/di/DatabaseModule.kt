package luzzr.zou.di

import android.content.Context
import androidx.room.Room
import luzzr.zou.data.local.database.NoteFlowDatabase
import luzzr.zou.data.local.database.NoteFlowMigrations
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
    fun provideNoteFlowDatabase(
        @ApplicationContext context: Context,
    ): NoteFlowDatabase = Room.databaseBuilder(
        context,
        NoteFlowDatabase::class.java,
        "noteflow.db",
    )
        .addMigrations(NoteFlowMigrations.MIGRATION_1_2)
        .addMigrations(NoteFlowMigrations.MIGRATION_2_3)
        .addMigrations(NoteFlowMigrations.MIGRATION_3_4)
        .build()

    @Provides
    fun provideTaskDao(database: NoteFlowDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideHabitDao(database: NoteFlowDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideNoteDao(database: NoteFlowDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideMediaDao(database: NoteFlowDatabase): MediaDao = database.mediaDao()
}
