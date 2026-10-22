package com.yxy.naswidget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** NAS 资源告警：CPU / 内存 / 磁盘超过阈值时发本地通知 */
object AlertNotifier {

    private const val CHANNEL_ID = "nas_alert"
    private const val REPEAT_MS = 30 * 60 * 1000L  // 同一组告警 30 分钟内不重复

    fun checkAndNotify(context: Context, stats: NasStats) {
        if (!Prefs.showAlert(context)) return
        if (Prefs.isSilentNow(context)) return
        val usageThreshold = Prefs.alertUsageThreshold(context).coerceIn(1, 100).toDouble()
        val cpuTempThreshold = Prefs.alertCpuTempThreshold(context).coerceIn(1, 120).toDouble()
        val nvmeTempThreshold = Prefs.alertNvmeTempThreshold(context).coerceIn(1, 120).toDouble()
        val alerts = mutableListOf<String>()
        if (stats.cpuPercent >= usageThreshold) alerts.add("CPU ${stats.cpuPercent.toInt()}%")
        if (stats.memPercent >= usageThreshold) alerts.add("内存 ${stats.memPercent.toInt()}%")
        for (d in stats.disks) {
            if (d.usePercent >= usageThreshold) alerts.add("${d.name} ${d.usePercent.toInt()}%")
        }
        val cpuTemp = stats.tempCpu
        if (cpuTemp != null && cpuTemp >= cpuTempThreshold) alerts.add("CPU 温度 ${cpuTemp.toInt()}°")
        val nvmeTemp = stats.tempNvme
        if (nvmeTemp != null && nvmeTemp >= nvmeTempThreshold) alerts.add("NVMe 温度 ${nvmeTemp.toInt()}°")
        if (alerts.isEmpty()) return

        val key = alerts.joinToString("|")
        val last = Prefs.lastAlertKey(context)
        val lastTime = Prefs.lastAlertTime(context)
        if (key == last && System.currentTimeMillis() - lastTime < REPEAT_MS) return
        Prefs.setLastAlert(context, key)

        ensureChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("NAS 告警")
            .setContentText(alerts.joinToString("、"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(1, notif) }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "NAS 资源告警", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }
}
