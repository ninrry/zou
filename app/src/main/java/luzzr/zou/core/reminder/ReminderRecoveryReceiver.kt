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
        reminderRecoveryCoordinator.ensureHealthCheckScheduled()
        reminderRecoveryCoordinator.enqueueImmediateRecovery()
    }
}
