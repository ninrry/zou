package luzzr.zou.core.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

const val PostNotificationsPermission = "android.permission.POST_NOTIFICATIONS"

fun Context.shouldRequestPostNotificationsPermission(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            this,
            PostNotificationsPermission,
        ) != PackageManager.PERMISSION_GRANTED
}
