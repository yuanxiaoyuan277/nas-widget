package com.yxy.naswidget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** 状态栏常驻监控通知：跟随定时刷新显示 CPU/内存/存储/温度，点击打开 App 主界面 */
object MonitorNotifier {

    private const val CHANNEL_ID = "nas_monitor"
    private const val NOTIF_ID = 100

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "NAS 状态栏监控", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    /** 用已拉取的数据更新常驻通知（关闭开关时取消） */
    fun updateFromStats(context: Context, result: Result<NasStats>) {
        if (!Prefs.showMonitorNotif(context)) {
            cancel(context)
            return
        }
        ensureChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val text = result.fold(
            onSuccess = { s ->
                val parts = mutableListOf(
                    "CPU ${s.cpuPercent.toInt()}%",
                    "内存 ${s.memPercent.toInt()}%",
                    "存储 ${s.storagePercent.toInt()}%"
                )
                if (s.tempCpu != null) parts.add("CPU ${s.tempCpu.toInt()}°")
                if (s.tempNvme != null) parts.add("NVMe ${s.tempNvme.toInt()}°")
                parts.joinToString(" · ")
            },
            onFailure = { "NAS 离线，无法连接监控数据源" }
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("NAS 监控")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_ID, notif) }
    }

    fun cancel(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIF_ID) }
    }
}
