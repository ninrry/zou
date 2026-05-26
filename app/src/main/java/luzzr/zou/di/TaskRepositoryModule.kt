package luzzr.zou.di

import luzzr.zou.data.repository.TaskRepositoryImpl
import luzzr.zou.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TaskRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        repository: TaskRepositoryImpl,
    ): TaskRepository
}
