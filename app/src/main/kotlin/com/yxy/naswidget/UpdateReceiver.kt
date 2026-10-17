package com.yxy.naswidget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** 收到定时广播，刷新所有 widget */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val am = AppWidgetManager.getInstance(context)
        val ids = am.getAppWidgetIds(ComponentName(context, NasWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            NasWidgetProvider.updateAll(context, am, ids)
        }
    }
}
