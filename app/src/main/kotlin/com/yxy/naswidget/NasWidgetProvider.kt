package com.yxy.naswidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.view.View
import android.widget.RemoteViews

class NasWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        AlarmScheduler.schedule(context)
        updateAll(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val am = AppWidgetManager.getInstance(context)
        val remain = am.getAppWidgetIds(android.content.ComponentName(context, NasWidgetProvider::class.java))
        if (remain.isEmpty()) {
            AlarmScheduler.cancel(context)
        }
    }

    companion object {

        fun updateAll(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val url = Prefs.url(context)
            Thread {
                val result = NasDataFetcher.fetch(url)
                for (id in appWidgetIds) {
                    val views = buildViews(context, result)
                    appWidgetManager.updateAppWidget(id, views)
                }
            }.start()
        }

        private fun buildViews(context: Context, result: Result<NasStats>): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.nas_widget)
            val showCpu = Prefs.showCpu(context)
            val showMem = Prefs.showMem(context)
            val showDisk1 = Prefs.showDisk1(context)
            val showDisk2 = Prefs.showDisk2(context)
            val showTemp = Prefs.showTemp(context)
            val showUptime = Prefs.showUptime(context)

            // 默认隐藏，按配置显示
            views.setViewVisibility(R.id.row_cpu, if (showCpu) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.row_mem, if (showMem) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.row_disk1, if (showDisk1) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.row_disk2, if (showDisk2) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.row_temp, if (showTemp) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.row_uptime, if (showUptime) View.VISIBLE else View.GONE)

            result.onSuccess { s ->
                val t = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(s.fetchTime))
                views.setTextViewText(R.id.tv_time, t)

                if (showCpu) {
                    views.setProgressBar(R.id.pb_cpu, 100, s.cpuPercent.toInt().coerceIn(0, 100), false)
                    views.setTextViewText(R.id.tv_cpu, String.format("%.0f%%", s.cpuPercent))
                }
                if (showMem) {
                    views.setProgressBar(R.id.pb_mem, 100, s.memPercent.toInt().coerceIn(0, 100), false)
                    views.setTextViewText(
                        R.id.tv_mem,
                        String.format("%.0f%% · %s", s.memPercent, Fmt.bytes(s.memUsed))
                    )
                }
                if (showDisk1 && s.disks.isNotEmpty()) {
                    val d = s.disks[0]
                    views.setTextViewText(R.id.tv_disk1_label, d.name)
                    views.setProgressBar(R.id.pb_disk1, 100, d.usePercent.toInt().coerceIn(0, 100), false)
                    views.setTextViewText(R.id.tv_disk1, String.format("%.0f%%", d.usePercent))
                }
                if (showDisk2 && s.disks.size > 1) {
                    val d = s.disks[1]
                    views.setTextViewText(R.id.tv_disk2_label, d.name)
                    views.setProgressBar(R.id.pb_disk2, 100, d.usePercent.toInt().coerceIn(0, 100), false)
                    views.setTextViewText(R.id.tv_disk2, String.format("%.0f%%", d.usePercent))
                }
                if (showTemp) {
                    val cpuT = s.tempCpu?.let { String.format("CPU %.0f°", it) } ?: "CPU --°"
                    val nvmeT = s.tempNvme?.let { String.format("NVMe %.0f°", it) } ?: "NVMe --°"
                    views.setTextViewText(R.id.tv_temp_cpu, cpuT)
                    views.setTextViewText(R.id.tv_temp_nvme, nvmeT)
                }
                if (showUptime) {
                    views.setTextViewText(R.id.tv_uptime, Fmt.uptime(s.uptimeSec))
                }
            }.onFailure { e ->
                views.setTextViewText(R.id.tv_time, "连接失败")
                if (showCpu) views.setTextViewText(R.id.tv_cpu, "--%")
                if (showMem) views.setTextViewText(R.id.tv_mem, "--%")
                if (showDisk1) views.setTextViewText(R.id.tv_disk1, "--%")
                if (showDisk2) views.setTextViewText(R.id.tv_disk2, "--%")
            }

            return views
        }
    }
}
