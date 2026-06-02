package luzzr.zou

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import luzzr.zou.core.reminder.ReminderNotificationManager
import luzzr.zou.core.reminder.ReminderRecoveryCoordinator
import luzzr.zou.di.ApplicationScope
import luzzr.zou.domain.repository.NoteRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltAndroidApp
class ZouApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var reminderRecoveryCoordinator: Lazy<ReminderRecoveryCoordinator>

    @Inject
    lateinit var reminderNotificationManager: ReminderNotificationManager

    @Inject
    lateinit var noteRepository: Lazy<NoteRepository>

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        reminderNotificationManager.ensureChannels()
        applicationScope.launch {
            delay(STARTUP_MAINTENANCE_DELAY_MILLIS)
            runCatching { reminderRecoveryCoordinator.get().ensureHealthCheckScheduled() }
            runCatching { noteRepository.get().cleanupOrphanedMedia() }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private companion object {
        const val STARTUP_MAINTENANCE_DELAY_MILLIS = 15_000L
    }
}
