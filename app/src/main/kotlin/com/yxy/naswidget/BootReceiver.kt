package com.yxy.naswidget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** 开机 / 应用更新后恢复定时刷新 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.schedule(context)
        val am = AppWidgetManager.getInstance(context)
        val ids = am.getAppWidgetIds(ComponentName(context, NasWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            NasWidgetProvider.updateAll(context, am, ids)
        }
    }
}
