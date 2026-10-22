package com.yxy.naswidget

import android.content.Context
import android.content.SharedPreferences

/** 配置存储（全局一份，所有 widget 实例共享） */
object Prefs {
    private const val NAME = "nas_widget_prefs"
    private const val KEY_URL = "nas_url"
    private const val KEY_REFRESH = "refresh_sec"
    private const val KEY_CPU = "show_cpu"
    private const val KEY_MEM = "show_mem"
    private const val KEY_DISK1 = "show_disk1"
    private const val KEY_DISK2 = "show_disk2"
    private const val KEY_DISK3 = "show_disk3"
    private const val KEY_TEMP = "show_temp"
    private const val KEY_UPTIME = "show_uptime"
    private const val KEY_GLASS_DARK = "glass_dark"
    private const val KEY_SHOW_DOCKER = "show_docker"
    private const val KEY_DOCKER_CONTAINERS = "docker_containers"
    private const val KEY_NAS_BASE = "nas_base"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_PENDING_VERIFIER = "pending_verifier"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_TOKEN_EXPIRES = "token_expires_at"
    private const val KEY_THEME = "theme"
    private const val KEY_IGNORED_VERSION = "ignored_version"
    private const val KEY_SHOW_WB2API = "show_wb2api"
    private const val KEY_WB2API_BASE = "wb2api_base"
    private const val KEY_WB2API_KEY = "wb2api_key"
    private const val KEY_FONT_SIZE = "font_size"
    private const val KEY_CPU_SIDE = "cpu_side"
    private const val KEY_MEM_SIDE = "mem_side"
    private const val KEY_DISK1_SIDE = "disk1_side"
    private const val KEY_DISK2_SIDE = "disk2_side"
    private const val KEY_DOCKER_SIDE = "docker_side"
    private const val KEY_SHOW_NET = "show_net"
    private const val KEY_NET_SIDE = "net_side"
    private const val KEY_SHOW_STORAGE = "show_storage"
    private const val KEY_SHOW_TEMP_ITEM = "show_temp_item"
    private const val KEY_LAST_ALERT_KEY = "last_alert_key"
    private const val KEY_LAST_ALERT_TIME = "last_alert_time"
    private const val KEY_ALERT = "show_alert"
    private const val KEY_ALERT_USAGE = "alert_usage_threshold"
    private const val KEY_ALERT_CPU_TEMP = "alert_cpu_temp_threshold"
    private const val KEY_ALERT_NVME_TEMP = "alert_nvme_temp_threshold"
    private const val KEY_SILENT_ENABLED = "silent_enabled"
    private const val KEY_SILENT_START = "silent_start"
    private const val KEY_SILENT_END = "silent_end"
    private const val KEY_OFFLINE_SINCE = "offline_since"
    private const val KEY_OFFLINE_NOTIFIED = "offline_notified"
    private const val KEY_DOCKER_RUNNING_SNAPSHOT = "docker_running_snapshot"
    private const val KEY_DOCKER_SNAPSHOT_TIME = "docker_snapshot_time"
    private const val KEY_ALERT_HISTORY = "alert_history"

    const val DEFAULT_URL = "http://192.168.2.3:19254/nas"
    const val DEFAULT_NAS_BASE = "http://192.168.2.3:1223"
    const val DEFAULT_WB2API_BASE = "http://192.168.2.3:7863"
    const val DEFAULT_WB2API_KEY = "yxyhhr.."

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun url(ctx: Context) = sp(ctx).getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
    fun setUrl(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_URL, v).apply()

    fun refreshSec(ctx: Context) = sp(ctx).getInt(KEY_REFRESH, 30)
    fun setRefresh(ctx: Context, v: Int) = sp(ctx).edit().putInt(KEY_REFRESH, v).apply()

    fun showCpu(ctx: Context) = sp(ctx).getBoolean(KEY_CPU, true)
    fun setShowCpu(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_CPU, v).apply()

    fun showMem(ctx: Context) = sp(ctx).getBoolean(KEY_MEM, true)
    fun setShowMem(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_MEM, v).apply()

    fun showDisk1(ctx: Context) = sp(ctx).getBoolean(KEY_DISK1, true)
    fun setShowDisk1(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_DISK1, v).apply()

    fun showDisk2(ctx: Context) = sp(ctx).getBoolean(KEY_DISK2, true)
    fun setShowDisk2(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_DISK2, v).apply()

    fun showDisk3(ctx: Context) = sp(ctx).getBoolean(KEY_DISK3, false)
    fun setShowDisk3(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_DISK3, v).apply()

    fun showTemp(ctx: Context) = sp(ctx).getBoolean(KEY_TEMP, true)
    fun setShowTemp(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_TEMP, v).apply()

    fun showUptime(ctx: Context) = sp(ctx).getBoolean(KEY_UPTIME, true)
    fun setShowUptime(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_UPTIME, v).apply()

    fun glassDark(ctx: Context) = sp(ctx).getBoolean(KEY_GLASS_DARK, true)
    fun setGlassDark(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_GLASS_DARK, v).apply()

    // === Docker 显示配置 ===
    fun showDocker(ctx: Context) = sp(ctx).getBoolean(KEY_SHOW_DOCKER, false)
    fun setShowDocker(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_SHOW_DOCKER, v).apply()

    fun dockerContainers(ctx: Context): Set<String> =
        sp(ctx).getString(KEY_DOCKER_CONTAINERS, "")?.split(",")
            ?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()

    fun setDockerContainers(ctx: Context, names: Set<String>) =
        sp(ctx).edit().putString(KEY_DOCKER_CONTAINERS, names.joinToString(",")).apply()

    // === Docker 容器排序方式（status=运行中优先/name=名称/cpu=CPU降序/mem=内存降序） ===
    private const val KEY_DOCKER_SORT = "docker_sort"
    fun dockerSort(ctx: Context): String = sp(ctx).getString(KEY_DOCKER_SORT, "status") ?: "status"
    fun setDockerSort(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_DOCKER_SORT, v).apply()

    // === 飞牛 NAS Web 地址（登录用） ===
    fun nasBase(ctx: Context) = sp(ctx).getString(KEY_NAS_BASE, DEFAULT_NAS_BASE) ?: DEFAULT_NAS_BASE
    fun setNasBase(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_NAS_BASE, v).apply()

    /** 规范化飞牛地址：补 scheme、去尾部斜杠 */
    fun normalizeNasBase(input: String): String {
        var s = input.trim()
        if (s.isEmpty()) return DEFAULT_NAS_BASE
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "http://$s"
        }
        return s.trimEnd('/')
    }

    /** OAuth device_id（首次生成后保持稳定） */
    fun deviceId(ctx: Context): String {
        val existing = sp(ctx).getString(KEY_DEVICE_ID, "") ?: ""
        if (existing.isNotEmpty()) return existing
        val id = java.util.UUID.randomUUID().toString().replace("-", "")
        sp(ctx).edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }

    /** 待换 token 的 code_verifier（跨浏览器授权期间保存） */
    fun setPendingVerifier(ctx: Context, v: String) =
        sp(ctx).edit().putString(KEY_PENDING_VERIFIER, v).apply()
    fun pendingVerifier(ctx: Context): String = sp(ctx).getString(KEY_PENDING_VERIFIER, "") ?: ""
    fun clearPendingVerifier(ctx: Context) =
        sp(ctx).edit().remove(KEY_PENDING_VERIFIER).apply()

    // === 登录 token ===
    fun accessToken(ctx: Context) = sp(ctx).getString(KEY_ACCESS_TOKEN, "") ?: ""
    fun refreshToken(ctx: Context) = sp(ctx).getString(KEY_REFRESH_TOKEN, "") ?: ""
    fun tokenExpiresAt(ctx: Context) = sp(ctx).getLong(KEY_TOKEN_EXPIRES, 0L)

    fun saveToken(ctx: Context, access: String, refresh: String?, expiresIn: Long) {
        val expiresAt = if (expiresIn > 0) System.currentTimeMillis() / 1000 + expiresIn else 0L
        sp(ctx).edit()
            .putString(KEY_ACCESS_TOKEN, access)
            .putString(KEY_REFRESH_TOKEN, refresh ?: "")
            .putLong(KEY_TOKEN_EXPIRES, expiresAt)
            .apply()
    }

    fun isLoggedIn(ctx: Context): Boolean = accessToken(ctx).isNotEmpty()

    fun clearToken(ctx: Context) = sp(ctx).edit()
        .remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).remove(KEY_TOKEN_EXPIRES).apply()

    // === 主题（浅色/深色/跟随系统） ===
    fun theme(ctx: Context): String = sp(ctx).getString(KEY_THEME, "dark") ?: "dark"
    fun setTheme(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_THEME, v).apply()

    /** 当前是否深色（「跟随系统」时按系统实际深浅解析） */
    fun isDarkTheme(ctx: Context): Boolean = when (theme(ctx)) {
        "light" -> false
        "system" -> (ctx.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        else -> true
    }

    /** AppCompat 夜间模式常量（「跟随系统」交给系统决定） */
    fun nightMode(ctx: Context): Int = when (theme(ctx)) {
        "light" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        "system" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
    }

    // === 更新弹窗「忽略此版本」 ===
    fun ignoredVersion(ctx: Context): String = sp(ctx).getString(KEY_IGNORED_VERSION, "") ?: ""
    fun setIgnoredVersion(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_IGNORED_VERSION, v).apply()

    // === wb2api 积分 ===
    fun showWb2api(ctx: Context) = sp(ctx).getBoolean(KEY_SHOW_WB2API, false)
    fun setShowWb2api(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_SHOW_WB2API, v).apply()
    fun wb2apiBase(ctx: Context) = sp(ctx).getString(KEY_WB2API_BASE, DEFAULT_WB2API_BASE) ?: DEFAULT_WB2API_BASE
    fun setWb2apiBase(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_WB2API_BASE, v).apply()
    fun wb2apiKey(ctx: Context) = sp(ctx).getString(KEY_WB2API_KEY, DEFAULT_WB2API_KEY) ?: DEFAULT_WB2API_KEY
    fun setWb2apiKey(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_WB2API_KEY, v).apply()

    // === 字号（全局，px） ===
    fun fontSize(ctx: Context) = sp(ctx).getInt(KEY_FONT_SIZE, 16)
    fun setFontSize(ctx: Context, v: Int) = sp(ctx).edit().putInt(KEY_FONT_SIZE, v).apply()

    // === 组件位置（x=水平偏移, y=垂直偏移, 0=居中, 范围 -50~+50） ===
    fun compX(ctx: Context, comp: String) = sp(ctx).getInt("${comp}_x", 0)
    fun setCompX(ctx: Context, comp: String, v: Int) = sp(ctx).edit().putInt("${comp}_x", v).apply()
    fun compY(ctx: Context, comp: String) = sp(ctx).getInt("${comp}_y", 0)
    fun setCompY(ctx: Context, comp: String, v: Int) = sp(ctx).edit().putInt("${comp}_y", v).apply()

    fun showNet(ctx: Context) = sp(ctx).getBoolean(KEY_SHOW_NET, false)
    fun setShowNet(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_SHOW_NET, v).apply()

    // === 存储总览显示项（所有磁盘汇总） ===
    fun showStorage(ctx: Context) = sp(ctx).getBoolean(KEY_SHOW_STORAGE, false)
    fun setShowStorage(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_SHOW_STORAGE, v).apply()

    // === 独立温度显示项（CPU + NVMe 温度成一行） ===
    fun showTempItem(ctx: Context) = sp(ctx).getBoolean(KEY_SHOW_TEMP_ITEM, false)
    fun setShowTempItem(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_SHOW_TEMP_ITEM, v).apply()

    fun lastAlertKey(ctx: Context) = sp(ctx).getString(KEY_LAST_ALERT_KEY, "") ?: ""
    fun lastAlertTime(ctx: Context) = sp(ctx).getLong(KEY_LAST_ALERT_TIME, 0L)
    fun setLastAlert(ctx: Context, key: String) = sp(ctx).edit()
        .putString(KEY_LAST_ALERT_KEY, key)
        .putLong(KEY_LAST_ALERT_TIME, System.currentTimeMillis())
        .apply()

    // === 资源告警通知开关 ===
    fun showAlert(ctx: Context) = sp(ctx).getBoolean(KEY_ALERT, true)
    fun setShowAlert(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_ALERT, v).apply()

    // === 状态栏常驻监控通知开关 ===
    private const val KEY_MONITOR_NOTIF = "monitor_notif"
    fun showMonitorNotif(ctx: Context) = sp(ctx).getBoolean(KEY_MONITOR_NOTIF, false)
    fun setShowMonitorNotif(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_MONITOR_NOTIF, v).apply()

    // === 常驻监控通知显示项（cpu/mem/storage/temp/net 子集） ===
    private const val KEY_MON_ITEMS = "mon_notif_items"
    private val MON_ITEM_DEFAULTS = listOf("cpu", "mem", "storage", "temp")
    fun monNotifItems(ctx: Context): Set<String> =
        sp(ctx).getString(KEY_MON_ITEMS, MON_ITEM_DEFAULTS.joinToString(","))?.split(",")
            ?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: MON_ITEM_DEFAULTS.toSet()
    fun setMonNotifItems(ctx: Context, items: Set<String>) =
        sp(ctx).edit().putString(KEY_MON_ITEMS, items.joinToString(",")).apply()

    // === 资源告警阈值（可配置，默认：使用率 90%、CPU 温度 85°、NVMe 温度 70°） ===
    fun alertUsageThreshold(ctx: Context) = sp(ctx).getInt(KEY_ALERT_USAGE, 90)
    fun setAlertUsageThreshold(ctx: Context, v: Int) = sp(ctx).edit().putInt(KEY_ALERT_USAGE, v).apply()
    fun alertCpuTempThreshold(ctx: Context) = sp(ctx).getInt(KEY_ALERT_CPU_TEMP, 85)
    fun setAlertCpuTempThreshold(ctx: Context, v: Int) = sp(ctx).edit().putInt(KEY_ALERT_CPU_TEMP, v).apply()
    fun alertNvmeTempThreshold(ctx: Context) = sp(ctx).getInt(KEY_ALERT_NVME_TEMP, 70)
    fun setAlertNvmeTempThreshold(ctx: Context, v: Int) = sp(ctx).edit().putInt(KEY_ALERT_NVME_TEMP, v).apply()

    // === 告警静默时段（夜间免打扰） ===
    fun silentEnabled(ctx: Context) = sp(ctx).getBoolean(KEY_SILENT_ENABLED, false)
    fun setSilentEnabled(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_SILENT_ENABLED, v).apply()
    fun silentStart(ctx: Context) = sp(ctx).getInt(KEY_SILENT_START, 23)
    fun setSilentStart(ctx: Context, v: Int) = sp(ctx).edit().putInt(KEY_SILENT_START, v).apply()
    fun silentEnd(ctx: Context) = sp(ctx).getInt(KEY_SILENT_END, 7)
    fun setSilentEnd(ctx: Context, v: Int) = sp(ctx).edit().putInt(KEY_SILENT_END, v).apply()

    /** 当前是否处于静默时段（跨天窗口，如 23:00~07:00） */
    fun isSilentNow(ctx: Context): Boolean {
        if (!silentEnabled(ctx)) return false
        val start = silentStart(ctx)
        val end = silentEnd(ctx)
        if (start == end) return false
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return if (start < end) hour in start until end
        else hour >= start || hour < end
    }

    // === NAS 离线状态跟踪（离线/恢复告警） ===
    fun offlineSince(ctx: Context): Long = sp(ctx).getLong(KEY_OFFLINE_SINCE, 0L)
    fun setOfflineSince(ctx: Context, v: Long) = sp(ctx).edit().putLong(KEY_OFFLINE_SINCE, v).apply()
    fun offlineNotified(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_OFFLINE_NOTIFIED, false)
    fun setOfflineNotified(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_OFFLINE_NOTIFIED, v).apply()

    // === Docker 容器运行状态快照（检测容器意外停止） ===
    fun dockerRunningSnapshot(ctx: Context): Set<String> =
        sp(ctx).getString(KEY_DOCKER_RUNNING_SNAPSHOT, "")?.split(",")
            ?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
    fun setDockerRunningSnapshot(ctx: Context, names: Set<String>) =
        sp(ctx).edit().putString(KEY_DOCKER_RUNNING_SNAPSHOT, names.joinToString(",")).apply()
    fun dockerSnapshotTime(ctx: Context): Long = sp(ctx).getLong(KEY_DOCKER_SNAPSHOT_TIME, 0L)
    fun setDockerSnapshotTime(ctx: Context, t: Long) = sp(ctx).edit().putLong(KEY_DOCKER_SNAPSHOT_TIME, t).apply()

    // === 告警历史（最近告警记录，最多 20 条，每条一行「HH:mm:ss 标题：内容」） ===
    fun alertHistory(ctx: Context): List<String> =
        sp(ctx).getString(KEY_ALERT_HISTORY, "")?.split("\n")?.filter { it.isNotEmpty() } ?: emptyList()
    fun addAlertHistory(ctx: Context, entry: String) {
        val updated = (listOf(entry) + alertHistory(ctx)).take(20)
        sp(ctx).edit().putString(KEY_ALERT_HISTORY, updated.joinToString("\n")).apply()
    }
    fun clearAlertHistory(ctx: Context) = sp(ctx).edit().remove(KEY_ALERT_HISTORY).apply()

    // === 今日峰值（CPU/内存/温度今日最高，跨天自动重置） ===
    private const val KEY_PEAK_DATE = "peak_date"
    private const val KEY_PEAK_CPU = "peak_cpu"
    private const val KEY_PEAK_MEM = "peak_mem"
    private const val KEY_PEAK_CPU_TEMP = "peak_cpu_temp"
    private const val KEY_PEAK_NVME_TEMP = "peak_nvme_temp"

    private fun todayKey(): String {
        val c = java.util.Calendar.getInstance()
        return String.format(
            "%04d%02d%02d",
            c.get(java.util.Calendar.YEAR),
            c.get(java.util.Calendar.MONTH) + 1,
            c.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    /** 记录今日峰值（跨天自动清零重记） */
    fun recordPeaks(ctx: Context, cpu: Double, mem: Double, cpuTemp: Double?, nvmeTemp: Double?) {
        val today = todayKey()
        val sp = sp(ctx)
        val e = sp.edit()
        if (sp.getString(KEY_PEAK_DATE, "") != today) {
            e.putString(KEY_PEAK_DATE, today)
            e.putFloat(KEY_PEAK_CPU, cpu.toFloat())
            e.putFloat(KEY_PEAK_MEM, mem.toFloat())
            if (cpuTemp != null) e.putFloat(KEY_PEAK_CPU_TEMP, cpuTemp.toFloat()) else e.remove(KEY_PEAK_CPU_TEMP)
            if (nvmeTemp != null) e.putFloat(KEY_PEAK_NVME_TEMP, nvmeTemp.toFloat()) else e.remove(KEY_PEAK_NVME_TEMP)
        } else {
            val pc = sp.getFloat(KEY_PEAK_CPU, 0f)
            val pm = sp.getFloat(KEY_PEAK_MEM, 0f)
            val pct = sp.getFloat(KEY_PEAK_CPU_TEMP, -1f)
            val pnt = sp.getFloat(KEY_PEAK_NVME_TEMP, -1f)
            if (cpu > pc) e.putFloat(KEY_PEAK_CPU, cpu.toFloat())
            if (mem > pm) e.putFloat(KEY_PEAK_MEM, mem.toFloat())
            if (cpuTemp != null && cpuTemp > pct) e.putFloat(KEY_PEAK_CPU_TEMP, cpuTemp.toFloat())
            if (nvmeTemp != null && nvmeTemp > pnt) e.putFloat(KEY_PEAK_NVME_TEMP, nvmeTemp.toFloat())
        }
        e.apply()
    }

    fun peakCpu(ctx: Context) = sp(ctx).getFloat(KEY_PEAK_CPU, 0f)
    fun peakMem(ctx: Context) = sp(ctx).getFloat(KEY_PEAK_MEM, 0f)
    fun peakCpuTemp(ctx: Context): Float? = sp(ctx).getFloat(KEY_PEAK_CPU_TEMP, -1f).takeIf { it >= 0 }
    fun peakNvmeTemp(ctx: Context): Float? = sp(ctx).getFloat(KEY_PEAK_NVME_TEMP, -1f).takeIf { it >= 0 }

    // === 今日网络峰值（下行/上行速率最高值，跨天自动重置） ===
    private const val KEY_PEAK_NET_DATE = "peak_net_date"
    private const val KEY_PEAK_NET_DOWN = "peak_net_down"
    private const val KEY_PEAK_NET_UP = "peak_net_up"

    fun recordNetPeak(ctx: Context, down: Long, up: Long) {
        val today = todayKey()
        val sp = sp(ctx)
        val e = sp.edit()
        if (sp.getString(KEY_PEAK_NET_DATE, "") != today) {
            e.putString(KEY_PEAK_NET_DATE, today)
            e.putLong(KEY_PEAK_NET_DOWN, down)
            e.putLong(KEY_PEAK_NET_UP, up)
        } else {
            if (down > sp.getLong(KEY_PEAK_NET_DOWN, 0L)) e.putLong(KEY_PEAK_NET_DOWN, down)
            if (up > sp.getLong(KEY_PEAK_NET_UP, 0L)) e.putLong(KEY_PEAK_NET_UP, up)
        }
        e.apply()
    }

    fun peakNetDown(ctx: Context): Long = sp(ctx).getLong(KEY_PEAK_NET_DOWN, 0L)
    fun peakNetUp(ctx: Context): Long = sp(ctx).getLong(KEY_PEAK_NET_UP, 0L)

    /** 清零今日所有峰值记录（CPU/内存/温度/网络），下次刷新重新累积 */
    fun clearPeaks(ctx: Context) = sp(ctx).edit()
        .remove(KEY_PEAK_DATE).remove(KEY_PEAK_CPU).remove(KEY_PEAK_MEM)
        .remove(KEY_PEAK_CPU_TEMP).remove(KEY_PEAK_NVME_TEMP)
        .remove(KEY_PEAK_NET_DATE).remove(KEY_PEAK_NET_DOWN).remove(KEY_PEAK_NET_UP)
        .apply()

    /** 恢复默认设置：清空全部配置（含飞牛登录 token、NAS 地址、告警阈值、主题等），回到初始状态 */
    fun clearAll(ctx: Context) = sp(ctx).edit().clear().apply()
}
