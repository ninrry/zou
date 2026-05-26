package luzzr.zou.core.hyperos

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * HyperOS / MIUI 专属的工具方法集合。
 *
 * 澎湃OS 有 3 层后台限制可能影响提醒准时性：
 * 1. PowerKeeper（应用智能省电）— 默认"智能限制"，需设为"无限制"
 * 2. 自启动权限 — 默认关闭，BOOT_COMPLETED 收不到
 * 3. 后台弹出页面权限 — 默认禁止，全屏弹窗被拦截
 *
 * 部分设置项可通过 Android 公开 API 自动检测，部分需用户手动确认。
 */
object XiaomiPowerKeeper {

    private const val PREFS_NAME = "hyperos_optimization"
    private const val KEY_OPTIMIZATION_DONE = "optimization_done"

    /** 可自动检测的项目 */
    data class OptimizeStatus(
        val batteryOptOk: Boolean,     // 省电策略 → 无限制
        val exactAlarmOk: Boolean,     // 精确闹钟权限
        val autoStartOk: Boolean,      // 自启动（无法检测，默认 false）
        val lockScreenOk: Boolean,     // 锁屏通知（无法检测，默认 false）
    ) {
        /** 可检测项是否全部通过 */
        val detectableAllOk: Boolean get() = batteryOptOk && exactAlarmOk
    }

    // ── 检测 ──────────────────────────────────────────────────

    /** 是否运行在 HyperOS 或 MIUI 上 */
    fun isHyperOS(): Boolean {
        val isXiaomiBrand = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
            Build.MANUFACTURER.equals("Redmi", ignoreCase = true) ||
            Build.MANUFACTURER.equals("POCO", ignoreCase = true)
        if (!isXiaomiBrand) return false
        val miuiVersion = getSystemProperty("ro.miui.ui.version.name")
        val hyperOsVersion = getSystemProperty("ro.mi.os.version.name")
        return miuiVersion != null || hyperOsVersion != null
    }

    /** 检测当前所有可读的优化项状态 */
    fun checkStatus(context: Context): OptimizeStatus {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryOptOk = pm.isIgnoringBatteryOptimizations(context.packageName)

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exactAlarmOk = am.canScheduleExactAlarms()

        return OptimizeStatus(
            batteryOptOk = batteryOptOk,
            exactAlarmOk = exactAlarmOk,
            autoStartOk = false,
            lockScreenOk = false,
        )
    }

    /** 用户是否已完成优化引导 */
    fun isOptimizationDone(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_OPTIMIZATION_DONE, false)
    }

    /** 标记优化引导已完成 */
    fun markOptimizationDone(context: Context) {
        prefs(context).edit().putBoolean(KEY_OPTIMIZATION_DONE, true).apply()
    }

    /** 重置优化引导状态（调试用） */
    fun resetOptimization(context: Context) {
        prefs(context).edit().remove(KEY_OPTIMIZATION_DONE).apply()
    }

    // ── 设置页跳转 ────────────────────────────────────────────

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openAutoStartSettings(context: Context) {
        val intent = Intent().apply {
            action = "android.settings.APPLICATION_DETAILS_SETTINGS"
            data = android.net.Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openNotificationChannelSettings(context: Context) {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, "task_start")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    // ── 内部 ──────────────────────────────────────────────────

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            method.invoke(null, key) as? String
        } catch (_: Exception) {
            null
        }
    }
}
