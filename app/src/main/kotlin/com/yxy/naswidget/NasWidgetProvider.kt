package com.yxy.naswidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.util.TypedValue
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
                val docker = if (Prefs.showDocker(context)) {
                    OAuthClient.getValidAccessToken(context).getOrNull()?.let {
                        NasDataFetcher.fetchDocker(Prefs.nasBase(context), it)
                    }
                } else null
                val wb2api = if (Prefs.showWb2api(context)) {
                    NasDataFetcher.fetchWb2apiCredits(Prefs.wb2apiBase(context), Prefs.wb2apiKey(context))
                } else null
                for (id in appWidgetIds) {
                    val views = buildViews(context, result, docker, wb2api)
                    appWidgetManager.updateAppWidget(id, views)
                }
                result.onSuccess { s ->
                    AlertNotifier.checkAndNotify(context, s)
                    AlertNotifier.markOnline(context)
                }.onFailure {
                    AlertNotifier.markOffline(context)
                }
                // 容器停止检测（仅在桌面组件开启 Docker 显示时会拉取容器列表）
                docker?.onSuccess { containers ->
                    AlertNotifier.checkContainerStops(context, containers)
                }
            }.start()
        }

        private fun buildViews(
            context: Context,
            result: Result<NasStats>,
            docker: Result<List<DockerContainer>>?,
            wb2api: Result<Wb2apiStats>?
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.nas_widget)
            // 点击组件任意位置打开 App 主界面
            val clickPi = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, clickPi)
            // 顶部刷新按钮：广播到 UpdateReceiver 立即刷新（requestCode 100 避免与整卡点击冲突）
            val refreshPi = PendingIntent.getBroadcast(
                context, 100,
                Intent(context, UpdateReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPi)
            // 关键：reapply 时旧 child 会被累积，必须每次清空两列再 addView，否则重复渲染
            views.removeAllViews(R.id.content_area)
            val showCpu = Prefs.showCpu(context)
            val showMem = Prefs.showMem(context)
            val showDisk1 = Prefs.showDisk1(context)
            val showDisk2 = Prefs.showDisk2(context)
            val showTemp = Prefs.showTemp(context)
            val showUptime = Prefs.showUptime(context)
            val showDocker = Prefs.showDocker(context)
            val showWb2api = Prefs.showWb2api(context)
            val showTempItem = Prefs.showTempItem(context)
            val fs = Prefs.fontSize(context)
            val usageThr = Prefs.alertUsageThreshold(context).coerceIn(1, 100)
            val cpuTempThr = Prefs.alertCpuTempThreshold(context).coerceIn(1, 120)
            val nvmeTempThr = Prefs.alertNvmeTempThreshold(context).coerceIn(1, 120)

            // 顶部
            views.setViewVisibility(R.id.row_uptime, if (showUptime) View.VISIBLE else View.GONE)
            views.setTextViewTextSize(R.id.tv_title, TypedValue.COMPLEX_UNIT_PX, (fs + 3).toFloat())
            views.setTextViewTextSize(R.id.tv_uptime, TypedValue.COMPLEX_UNIT_PX, (fs + 2).toFloat())

            var stats: NasStats? = null
            result.onSuccess { s ->
                stats = s
                views.setTextViewText(R.id.tv_uptime, "运行 " + Fmt.uptime(s.uptimeSec) + " · " + Fmt.time(s.fetchTime))
            }.onFailure {
                views.setTextViewText(R.id.tv_uptime, "连接失败")
            }

            fun addTo(comp: String, rootId: Int, item: RemoteViews) {
                views.addView(R.id.content_area, item)
                val density = context.resources.displayMetrics.density
                views.setFloat(rootId, "setTranslationX", Prefs.compX(context, comp) * density * 1.6f)
                views.setFloat(rootId, "setTranslationY", Prefs.compY(context, comp) * density * 1.6f)
            }

            // CPU
            if (showCpu) {
                val item = RemoteViews(context.packageName, R.layout.item_cpu)
                stats?.let { s ->
                    item.setProgressBar(R.id.pb_cpu, 100, s.cpuPercent.toInt().coerceIn(0, 100), false)
                    item.setColorStateList(R.id.pb_cpu, "setProgressTintList", ColorStateList.valueOf(Fmt.levelColor(s.cpuPercent)))
                    item.setTextViewText(R.id.tv_cpu, tempPct(s.tempCpu, s.cpuPercent, showTemp))
                    if (s.cpuPercent >= usageThr || (s.tempCpu?.let { it >= cpuTempThr } == true)) {
                        item.setTextColor(R.id.tv_cpu, Fmt.ALERT_COLOR)
                    }
                } ?: item.setTextViewText(R.id.tv_cpu, "--%")
                item.setTextViewTextSize(R.id.tv_cpu_label, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                item.setTextViewTextSize(R.id.tv_cpu, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                addTo("cpu", R.id.item_cpu_root, item)
            }

            // 内存
            if (showMem) {
                val item = RemoteViews(context.packageName, R.layout.item_mem)
                stats?.let { s ->
                    item.setProgressBar(R.id.pb_mem, 100, s.memPercent.toInt().coerceIn(0, 100), false)
                    item.setColorStateList(R.id.pb_mem, "setProgressTintList", ColorStateList.valueOf(Fmt.levelColor(s.memPercent)))
                    val memFree = (s.memTotal - s.memUsed).coerceAtLeast(0)
                    item.setTextViewText(R.id.tv_mem, String.format("%s/%s %.0f%%", Fmt.bytes(memFree), Fmt.bytes(s.memTotal), s.memPercent))
                    if (s.memPercent >= usageThr) {
                        item.setTextColor(R.id.tv_mem, Fmt.ALERT_COLOR)
                    }
                } ?: item.setTextViewText(R.id.tv_mem, "--%")
                item.setTextViewTextSize(R.id.tv_mem_label, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                item.setTextViewTextSize(R.id.tv_mem, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                addTo("mem", R.id.item_mem_root, item)
            }

            // NVMe
            if (showDisk1) {
                val item = RemoteViews(context.packageName, R.layout.item_disk1)
                stats?.let { s ->
                    if (s.disks.isNotEmpty()) {
                        val d = s.disks[0]
                        item.setTextViewText(R.id.tv_disk1_label, d.name)
                        item.setProgressBar(R.id.pb_disk1, 100, d.usePercent.toInt().coerceIn(0, 100), false)
                        item.setColorStateList(R.id.pb_disk1, "setProgressTintList", ColorStateList.valueOf(Fmt.levelColor(d.usePercent)))
                        item.setTextViewText(R.id.tv_disk1, diskText(s.tempNvme, d.usePercent, d.total - d.used, showTemp))
                        if (d.usePercent >= usageThr || (s.tempNvme?.let { it >= nvmeTempThr } == true)) {
                            item.setTextColor(R.id.tv_disk1, Fmt.ALERT_COLOR)
                        }
                    } else {
                        item.setTextViewText(R.id.tv_disk1, "--%")
                    }
                } ?: item.setTextViewText(R.id.tv_disk1, "--%")
                item.setTextViewTextSize(R.id.tv_disk1_label, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                item.setTextViewTextSize(R.id.tv_disk1, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                addTo("disk1", R.id.item_disk1_root, item)
            }

            // 黑盘
            if (showDisk2) {
                val item = RemoteViews(context.packageName, R.layout.item_disk2)
                stats?.let { s ->
                    if (s.disks.size > 1) {
                        val d = s.disks[1]
                        item.setTextViewText(R.id.tv_disk2_label, d.name)
                        item.setProgressBar(R.id.pb_disk2, 100, d.usePercent.toInt().coerceIn(0, 100), false)
                        item.setColorStateList(R.id.pb_disk2, "setProgressTintList", ColorStateList.valueOf(Fmt.levelColor(d.usePercent)))
                        item.setTextViewText(R.id.tv_disk2, diskText(null, d.usePercent, d.total - d.used, false))
                        if (d.usePercent >= usageThr) {
                            item.setTextColor(R.id.tv_disk2, Fmt.ALERT_COLOR)
                        }
                    } else {
                        item.setTextViewText(R.id.tv_disk2, "--%")
                    }
                } ?: item.setTextViewText(R.id.tv_disk2, "--%")
                item.setTextViewTextSize(R.id.tv_disk2_label, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                item.setTextViewTextSize(R.id.tv_disk2, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                addTo("disk2", R.id.item_disk2_root, item)
            }

            // 存储总览（所有磁盘汇总）
            if (Prefs.showStorage(context)) {
                val item = RemoteViews(context.packageName, R.layout.item_storage)
                stats?.let { s ->
                    item.setProgressBar(R.id.pb_storage, 100, s.storagePercent.toInt().coerceIn(0, 100), false)
                    item.setColorStateList(R.id.pb_storage, "setProgressTintList", ColorStateList.valueOf(Fmt.levelColor(s.storagePercent)))
                    item.setTextViewText(R.id.tv_storage, s.storageText)
                    if (s.storagePercent >= usageThr) {
                        item.setTextColor(R.id.tv_storage, Fmt.ALERT_COLOR)
                    }
                } ?: item.setTextViewText(R.id.tv_storage, "--%")
                item.setTextViewTextSize(R.id.tv_storage_label, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                item.setTextViewTextSize(R.id.tv_storage, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                addTo("storage", R.id.item_storage_root, item)
            }

            // 网络
            if (Prefs.showNet(context)) {
                val item = RemoteViews(context.packageName, R.layout.item_net)
                stats?.let { s ->
                    item.setTextViewText(R.id.tv_net, "↓" + Fmt.speed(s.netDown) + " ↑" + Fmt.speed(s.netUp))
                } ?: item.setTextViewText(R.id.tv_net, "↓-- ↑--")
                item.setTextViewTextSize(R.id.tv_net_label, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                item.setTextViewTextSize(R.id.tv_net, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                addTo("net", R.id.item_net_root, item)
            }

            // 温度（CPU + NVMe 独立成一行）
            if (showTempItem) {
                val item = RemoteViews(context.packageName, R.layout.item_temp)
                stats?.let { s ->
                    val cpuT = s.tempCpu?.let { String.format("%.0f°", it) } ?: "--°"
                    val nvmeT = s.tempNvme?.let { String.format("%.0f°", it) } ?: "--°"
                    item.setTextViewText(R.id.tv_temp, "CPU $cpuT · NVMe $nvmeT")
                    if ((s.tempCpu?.let { it >= cpuTempThr } == true) || (s.tempNvme?.let { it >= nvmeTempThr } == true)) {
                        item.setTextColor(R.id.tv_temp, Fmt.ALERT_COLOR)
                    }
                } ?: item.setTextViewText(R.id.tv_temp, "CPU --° · NVMe --°")
                item.setTextViewTextSize(R.id.tv_temp_label, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                item.setTextViewTextSize(R.id.tv_temp, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
                addTo("temp", R.id.item_temp_root, item)
            }

            // Docker（容器垂直排列，整体按 docker 的 x/y 定位）
            if (showDocker) {
                val selected = Prefs.dockerContainers(context)
                val group = RemoteViews(context.packageName, R.layout.docker_group)
                val density = context.resources.displayMetrics.density
                group.setFloat(R.id.docker_group_root, "setTranslationX", Prefs.compX(context, "docker") * density * 1.6f)
                group.setFloat(R.id.docker_group_root, "setTranslationY", Prefs.compY(context, "docker") * density * 1.6f)
                group.setViewVisibility(R.id.tv_docker_summary, View.GONE)
                if (docker == null) {
                    group.addView(R.id.docker_list, makeDockerItem(context, "未登录飞牛", false, "", fs))
                } else {
                    docker.onSuccess { containers ->
                        if (selected.isNotEmpty()) {
                            val running = containers.count { it.running }
                            group.setTextViewText(R.id.tv_docker_summary, "$running 运行 / ${containers.size} 总计")
                            group.setTextViewTextSize(R.id.tv_docker_summary, TypedValue.COMPLEX_UNIT_PX, (fs - 2).toFloat())
                            group.setViewVisibility(R.id.tv_docker_summary, View.VISIBLE)
                            val filtered = containers.filter { it.name in selected }.sortedWith(dockerContainerOrder)
                            if (filtered.isEmpty()) {
                                group.addView(R.id.docker_list, makeDockerItem(context, "无容器", false, "", fs))
                            } else {
                                for (c in filtered) {
                                    group.addView(R.id.docker_list, makeDockerItem(context, c, fs))
                                }
                            }
                        }
                    }.onFailure {
                        group.addView(R.id.docker_list, makeDockerItem(context, "Docker 获取失败", false, "", fs))
                    }
                }
                views.addView(R.id.content_area, group)
            }

            // 底部积分
            views.setViewVisibility(R.id.row_wb2api, if (showWb2api) View.VISIBLE else View.GONE)
            if (showWb2api) {
                wb2api?.onSuccess { s ->
                    views.setTextViewText(R.id.tv_wb2api, "${s.totalCredits} · ${s.healthyCount}/${s.accountCount}")
                }?.onFailure {
                    views.setTextViewText(R.id.tv_wb2api, "获取失败")
                }
            }
            views.setTextViewTextSize(R.id.tv_wb2api, TypedValue.COMPLEX_UNIT_PX, (fs + 3).toFloat())

            return views
        }

        private fun makeDockerItem(context: Context, name: String, running: Boolean, state: String, fs: Int): RemoteViews {
            val item = RemoteViews(context.packageName, R.layout.item_docker)
            item.setTextViewText(R.id.tv_name, name)
            item.setTextViewText(R.id.tv_state, state)
            item.setTextViewText(R.id.tv_dot, if (running) "●" else "○")
            item.setTextColor(R.id.tv_dot, if (running) 0xFF4CAF50.toInt() else 0xFF888888.toInt())
            item.setTextViewTextSize(R.id.tv_name, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            item.setTextViewTextSize(R.id.tv_state, TypedValue.COMPLEX_UNIT_PX, (fs - 1).toFloat())
            return item
        }

        private fun makeDockerItem(context: Context, c: DockerContainer, fs: Int): RemoteViews {
            val item = RemoteViews(context.packageName, R.layout.item_docker)
            item.setTextViewText(R.id.tv_name, c.name)
            item.setTextViewText(R.id.tv_state, c.detailText)
            val dotColor = when {
                c.running -> 0xFF4CAF50.toInt()
                c.state.equals("error", true) -> 0xFFF44336.toInt()
                else -> 0xFF888888.toInt()
            }
            item.setTextViewText(R.id.tv_dot, if (c.running) "●" else "○")
            item.setTextColor(R.id.tv_dot, dotColor)
            item.setTextViewTextSize(R.id.tv_name, TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            item.setTextViewTextSize(R.id.tv_state, TypedValue.COMPLEX_UNIT_PX, (fs - 1).toFloat())
            return item
        }

        private fun tempPct(temp: Double?, pct: Double, showTemp: Boolean): String {
            val pctText = String.format("%.0f%%", pct)
            if (!showTemp) return pctText
            val t = temp?.let { String.format("%.0f°", it) } ?: "--°"
            return "$t $pctText"
        }

        /** 磁盘值：可用空间 + 使用率（NVMe 附加温度） */
        private fun diskText(temp: Double?, pct: Double, free: Long, showTemp: Boolean): String {
            val freeText = Fmt.bytes(free)
            val pctText = String.format("%.0f%%", pct)
            if (!showTemp) return "$freeText $pctText"
            val t = temp?.let { String.format("%.0f°", it) } ?: "--°"
            return "$freeText $t $pctText"
        }
    }
}
