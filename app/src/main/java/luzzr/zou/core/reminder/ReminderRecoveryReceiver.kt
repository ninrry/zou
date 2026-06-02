package luzzr.zou.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderRecoveryReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRecoveryCoordinator: ReminderRecoveryCoordinator

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in supportedActions) return
        reminderRecoveryCoordinator.ensureHealthCheckScheduled()
        reminderRecoveryCoordinator.enqueueImmediateRecovery()
    }

    private companion object {
        val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
