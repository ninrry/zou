package luzzr.zou.di

import luzzr.zou.data.settings.SettingsRepositoryImpl
import luzzr.zou.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        repository: SettingsRepositoryImpl,
    ): SettingsRepository
}
