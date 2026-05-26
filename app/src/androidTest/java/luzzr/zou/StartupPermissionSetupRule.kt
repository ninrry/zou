package luzzr.zou

import android.app.Instrumentation
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

class StartupPermissionSetupRule : ExternalResource() {

    override fun before() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName

        runShellCommand(
            instrumentation = instrumentation,
            command = "pm grant $packageName android.permission.POST_NOTIFICATIONS",
        )
        runShellCommand(
            instrumentation = instrumentation,
            command = "appops set $packageName SCHEDULE_EXACT_ALARM allow",
        )
        runShellCommand(
            instrumentation = instrumentation,
            command = "cmd deviceidle whitelist +$packageName",
        )
    }

    private fun runShellCommand(
        instrumentation: Instrumentation,
        command: String,
    ) {
        ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command),
        ).bufferedReader().use { it.readText() }
        instrumentation.waitForIdleSync()
    }
}
