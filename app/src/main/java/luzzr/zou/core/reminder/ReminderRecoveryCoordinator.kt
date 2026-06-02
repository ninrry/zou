package luzzr.zou.core.reminder

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
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
            OneTimeWorkRequestBuilder<ReminderRecoveryWorker>()
                .setInitialDelay(IMMEDIATE_RECOVERY_GRACE_SECONDS, TimeUnit.SECONDS)
                .build(),
        )
    }

    fun ensureHealthCheckScheduled() {
        workManager.enqueueUniquePeriodicWork(
            ReminderConstants.reminderHealthCheckWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ReminderRecoveryWorker>(
                HEALTH_CHECK_REPEAT_HOURS,
                TimeUnit.HOURS,
            )
                .setInitialDelay(HEALTH_CHECK_INITIAL_DELAY_MINUTES, TimeUnit.MINUTES)
                .build(),
        )
    }

    private companion object {
        const val HEALTH_CHECK_REPEAT_HOURS = 6L
        const val HEALTH_CHECK_INITIAL_DELAY_MINUTES = 30L
        const val IMMEDIATE_RECOVERY_GRACE_SECONDS = 60L
    }
}
