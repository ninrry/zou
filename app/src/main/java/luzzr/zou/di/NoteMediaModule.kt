package luzzr.zou.di

import luzzr.zou.data.local.media.LocalNoteImageStorage
import luzzr.zou.data.local.media.NoteImageStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NoteMediaModule {

    @Binds
    @Singleton
    abstract fun bindNoteImageStorage(
        storage: LocalNoteImageStorage,
    ): NoteImageStorage
}
