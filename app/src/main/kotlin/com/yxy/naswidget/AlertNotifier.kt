package com.yxy.naswidget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** NAS 资源告警：CPU / 内存 / 磁盘超过阈值时发本地通知 */
object AlertNotifier {

    private const val CHANNEL_ID = "nas_alert"
    private const val THRESHOLD = 90.0
    private const val REPEAT_MS = 30 * 60 * 1000L  // 同一组告警 30 分钟内不重复

    fun checkAndNotify(context: Context, stats: NasStats) {
        val alerts = mutableListOf<String>()
        if (stats.cpuPercent >= THRESHOLD) alerts.add("CPU ${stats.cpuPercent.toInt()}%")
        if (stats.memPercent >= THRESHOLD) alerts.add("内存 ${stats.memPercent.toInt()}%")
        for (d in stats.disks) {
            if (d.usePercent >= THRESHOLD) alerts.add("${d.name} ${d.usePercent.toInt()}%")
        }
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
            .setContentTitle("NAS 资源告警")
            .setContentText(alerts.joinToString("、") + " 使用率超过 ${THRESHOLD.toInt()}%")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
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
