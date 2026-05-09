package luzzr.zou

import android.app.Application
import luzzr.zou.core.reminder.ReminderNotificationManager
import luzzr.zou.core.reminder.ReminderRecoveryCoordinator
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import luzzr.zou.di.ApplicationScope
import luzzr.zou.domain.repository.NoteRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class ZouApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var reminderRecoveryCoordinator: ReminderRecoveryCoordinator

    @Inject
    lateinit var reminderNotificationManager: ReminderNotificationManager

    @Inject
    lateinit var noteRepository: NoteRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        reminderNotificationManager.ensureChannels()
        reminderRecoveryCoordinator.ensureHealthCheckScheduled()
        applicationScope.launch {
            runCatching { noteRepository.cleanupOrphanedMedia() }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
