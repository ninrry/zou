package luzzr.zou.di

import luzzr.zou.data.repository.NoteRepositoryImpl
import luzzr.zou.domain.repository.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NoteRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        repository: NoteRepositoryImpl,
    ): NoteRepository
}
