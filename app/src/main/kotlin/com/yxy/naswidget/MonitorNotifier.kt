package com.yxy.naswidget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** 状态栏常驻监控通知：跟随定时刷新显示 CPU/内存/存储/温度，点击打开 App 主界面 */
object MonitorNotifier {

    private const val CHANNEL_ID = "nas_monitor"
    private const val NOTIF_ID = 100

    /** 广播 action：通知栏「静默 1 小时 / 取消静默」快捷操作 */
    const val ACTION_SILENT_1H = "com.yxy.naswidget.action.SILENT_1H"

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
                val items = Prefs.monNotifItems(context)
                val parts = mutableListOf<String>()
                if ("cpu" in items) parts.add("CPU ${s.cpuPercent.toInt()}%")
                if ("mem" in items) parts.add("内存 ${s.memPercent.toInt()}%")
                if ("storage" in items) parts.add("存储 ${s.storagePercent.toInt()}%")
                if ("temp" in items) {
                    if (s.tempCpu != null) parts.add("CPU ${s.tempCpu.toInt()}°")
                    if (s.tempNvme != null) parts.add("NVMe ${s.tempNvme.toInt()}°")
                }
                if ("net" in items) parts.add("↓${Fmt.speed(s.netDown)} ↑${Fmt.speed(s.netUp)}")
                if ("traffic" in items) parts.add("流量 ↓${Fmt.bytes(Prefs.trafficDown(context))} ↑${Fmt.bytes(Prefs.trafficUp(context))}")
                val base = if (parts.isEmpty()) "NAS 在线" else parts.joinToString(" · ")
                "$base · 更新 ${Fmt.time(s.fetchTime)}"
            },
            onFailure = { "NAS 离线，无法连接监控数据源" }
        )

        // 「刷新」快捷操作：广播到 UpdateReceiver 立即拉取最新数据（组件+通知一起刷新）
        val refreshPi = PendingIntent.getBroadcast(
            context, 101,
            Intent(context, UpdateReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 「飞牛管理」快捷操作：打开飞牛 NAS 网页管理界面
        val nasPi = PendingIntent.getActivity(
            context, 102,
            Intent(Intent.ACTION_VIEW, Uri.parse(Prefs.nasBase(context))),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 「静默 1 小时 / 取消静默」快捷操作：广播到 UpdateReceiver 切换临时静默
        val silentPi = PendingIntent.getBroadcast(
            context, 103,
            Intent(context, UpdateReceiver::class.java).setAction(ACTION_SILENT_1H),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val silentLabel = if (Prefs.isTempSilent(context)) "取消静默" else "静默1小时"

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("NAS 监控")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_launcher, "刷新", refreshPi)
            .addAction(R.drawable.ic_launcher, "飞牛管理", nasPi)
            .addAction(R.drawable.ic_launcher, silentLabel, silentPi)
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
