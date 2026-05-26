package luzzr.zou.core.reminder

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRecoveryCoordinator @Inject constructor(
    private val workManager: WorkManager,
) {

    fun enqueueImmediateRecovery() {
        workManager.enqueueUniqueWork(
            ReminderConstants.reminderRecoveryWorkName,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderRecoveryWorker>().build(),
        )
    }

    fun ensureHealthCheckScheduled() {
        workManager.enqueueUniquePeriodicWork(
            ReminderConstants.reminderHealthCheckWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ReminderRecoveryWorker>(6, TimeUnit.HOURS).build(),
        )
    }
}
