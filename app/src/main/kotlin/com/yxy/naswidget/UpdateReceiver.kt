package com.yxy.naswidget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** 收到定时广播，刷新所有 widget 并重新调度下一次 */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 通知栏「静默 1 小时 / 取消静默」快捷操作：切换临时静默后刷新通知文案
        if (intent.action == MonitorNotifier.ACTION_SILENT_1H) {
            if (Prefs.isTempSilent(context)) {
                Prefs.setSilentUntil(context, 0L)
                Toast.makeText(context, "已取消静默，恢复告警推送", Toast.LENGTH_SHORT).show()
            } else {
                Prefs.setSilentUntil(context, System.currentTimeMillis() + 3600_000L)
                Toast.makeText(context, "已静默 1 小时，期间不推送告警", Toast.LENGTH_SHORT).show()
            }
            Thread {
                val result = NasDataFetcher.fetch(Prefs.url(context))
                result.onSuccess { s -> Prefs.recordNetTraffic(context, s.netDown, s.netUp) }
                MonitorNotifier.updateFromStats(context, result)
            }.start()
            return
        }

        AlarmScheduler.schedule(context)
        val am = AppWidgetManager.getInstance(context)
        val ids = am.getAppWidgetIds(ComponentName(context, NasWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            NasWidgetProvider.updateAll(context, am, ids)
        } else if (Prefs.showMonitorNotif(context)) {
            // 无桌面组件但开启了常驻监控：单独拉取并更新通知
            Thread {
                val result = NasDataFetcher.fetch(Prefs.url(context))
                result.onSuccess { s -> Prefs.recordNetTraffic(context, s.netDown, s.netUp) }
                MonitorNotifier.updateFromStats(context, result)
            }.start()
        }
    }
}
