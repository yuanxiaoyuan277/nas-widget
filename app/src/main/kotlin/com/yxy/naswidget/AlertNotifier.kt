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
    private const val OFFLINE_DELAY_MS = 2 * 60 * 1000L  // 连续拉取失败 2 分钟后才判定离线

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
        if (key == last && System.currentTimeMillis() - lastTime < Prefs.alertRepeatMs(context)) return
        Prefs.setLastAlert(context, key)
        send(context, "NAS 告警", alerts.joinToString("、"))
    }

    /** 拉取成功：若此前处于离线状态，则发「NAS 已恢复」通知并复位 */
    fun markOnline(context: Context) {
        if (Prefs.offlineSince(context) == 0L) return
        Prefs.setOfflineSince(context, 0L)
        if (Prefs.offlineNotified(context)) {
            Prefs.setOfflineNotified(context, false)
            send(context, "NAS 已恢复", "监控数据源连接已恢复正常")
        }
    }

    /** 拉取失败：记录离线起始时间（不受告警开关影响）；连续失败超过 OFFLINE_DELAY_MS 后发「NAS 离线」通知（一次离线只提醒一次） */
    fun markOffline(context: Context) {
        val now = System.currentTimeMillis()
        val since = Prefs.offlineSince(context)
        if (since == 0L) {
            Prefs.setOfflineSince(context, now)
            return
        }
        if (!Prefs.showAlert(context) || Prefs.isSilentNow(context)) return
        if (!Prefs.offlineNotified(context) && now - since >= OFFLINE_DELAY_MS) {
            Prefs.setOfflineNotified(context, true)
            send(context, "NAS 离线", "无法连接监控数据源，请检查 NAS 是否在线")
        }
    }

    /** 对比容器运行快照，检测从上一次刷新以来停止/异常的容器并发通知 */
    fun checkContainerStops(context: Context, containers: List<DockerContainer>) {
        val running = containers.filter { it.running }.map { it.name }.toSet()
        val prev = Prefs.dockerRunningSnapshot(context)
        val prevTime = Prefs.dockerSnapshotTime(context)
        Prefs.setDockerRunningSnapshot(context, running)
        Prefs.setDockerSnapshotTime(context, System.currentTimeMillis())

        // 首次快照无基线，不告警
        if (prevTime == 0L || prev.isEmpty()) return
        val stopped = prev.filter { name ->
            val c = containers.find { it.name == name }
            // 容器被移除，或退出运行态且非「重启中/暂停/已创建」等过渡态
            c == null || (!c.running && c.state.lowercase() !in setOf("restarting", "paused", "created"))
        }
        if (stopped.isEmpty()) return
        if (!Prefs.showAlert(context)) return
        if (Prefs.isSilentNow(context)) return
        send(context, "容器停止", stopped.joinToString("、") + " 已停止")
    }

    /** 发送测试通知（手动触发，不进告警历史），返回通知权限是否已开启 */
    fun sendTest(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        ensureChannel(context)
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("NAS Widget 测试通知")
            .setContentText("通知功能正常，资源超限 / 离线 / 容器停止时会在这里提醒你")
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
        runCatching { NotificationManagerCompat.from(context).notify(2, notif) }
        return true
    }

    /** 发送本地通知（标题 + 内容），点击打开 App 主界面 */
    private fun send(context: Context, title: String, text: String) {
        Prefs.addAlertHistory(context, "${Fmt.time(System.currentTimeMillis())} $title：$text")
        ensureChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(!Prefs.alertSound(context))
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
