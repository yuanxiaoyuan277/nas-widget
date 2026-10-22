package com.yxy.naswidget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** 收到定时广播，刷新所有 widget 并重新调度下一次 */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.schedule(context)
        val am = AppWidgetManager.getInstance(context)
        val ids = am.getAppWidgetIds(ComponentName(context, NasWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            NasWidgetProvider.updateAll(context, am, ids)
        } else if (Prefs.showMonitorNotif(context)) {
            // 无桌面组件但开启了常驻监控：单独拉取并更新通知
            Thread {
                val result = NasDataFetcher.fetch(Prefs.url(context))
                MonitorNotifier.updateFromStats(context, result)
            }.start()
        }
    }
}
