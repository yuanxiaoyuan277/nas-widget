package com.yxy.naswidget

import org.json.JSONObject

/** NAS 监控数据模型（对应 monitor_proxy 的 /nas 返回） */
data class DiskStat(val name: String, val used: Long, val total: Long, val usePercent: Double)

/** wb2api（workbuddy2api）积分统计 */
data class Wb2apiStats(
    val totalCredits: Long,
    val accountCount: Int,
    val healthyCount: Int,
    val accounts: List<AccountCredit>
) {
    data class AccountCredit(
        val nickname: String,
        val credits: Long,
        val creditsTotal: Long,
        val cooling: Boolean,
        val disabled: Boolean
    )
}

/** Docker 容器（来自飞牛 appcgi.dockermgr.containerList + containerStats） */
data class DockerContainer(
    val id: String,
    val name: String,
    val state: String,
    val status: String,
    val memBytes: Long? = null,
    val cpuPercent: Double? = null,
    val image: String = ""
) {
    val running: Boolean get() = state.equals("running", ignoreCase = true)

    /** 精确状态标签（飞牛 State 字段可能为 running/exited/stopped/error/created/paused/restarting 等） */
    val stateLabel: String get() = when (state.lowercase()) {
        "running" -> "运行"
        "exited", "stopped", "dead" -> "已停止"
        "error" -> "异常"
        "created" -> "已创建"
        "paused" -> "已暂停"
        "restarting" -> "重启中"
        "" -> "未知"
        else -> state
    }

    /** 内存占用可读文本（如 "78M"），未获取则为空串 */
    val memText: String get() = memBytes?.let { Fmt.bytes(it) } ?: ""

    /** CPU 使用率可读文本（如 "12%"），未获取则为空串 */
    val cpuText: String get() = cpuPercent?.let { String.format("%.0f%%", it.coerceIn(0.0, 100.0)) } ?: ""

    /** 运行时长文本（从 Status 提取 "Up 2 days" 这类，清理掉 "(healthy)" 等后缀） */
    val uptimeText: String get() {
        if (!running) return ""
        val cleaned = status.substringBefore(" (").trim()
        return if (cleaned.startsWith("Up ")) cleaned else ""
    }

    /** 状态 + 详情（如 "运行 · Up 2 days"、"异常 · 137"） */
    val stateText: String get() =
        if (status.isNotEmpty() && !running) "$stateLabel · $status" else stateLabel

    /** 显示用完整详情：运行态拼 CPU + 内存 + 运行时长；非运行态用 stateText */
    val detailText: String get() {
        if (running) {
            val parts = mutableListOf(stateLabel)
            if (cpuText.isNotEmpty()) parts.add(cpuText)
            if (memText.isNotEmpty()) parts.add(memText)
            if (uptimeText.isNotEmpty()) parts.add(uptimeText)
            return parts.joinToString(" · ")
        }
        return stateText
    }
}

data class NasStats(
    val cpuPercent: Double,
    val cores: Int,
    val memPercent: Double,
    val memUsed: Long,
    val memTotal: Long,
    val disks: List<DiskStat>,
    val tempCpu: Double?,
    val tempNvme: Double?,
    val uptimeSec: Long,
    val netDown: Long,
    val netUp: Long,
    val cpuModel: String,
    val osVersion: String,
    val fetchTime: Long
) {
    /** 存储总览：所有磁盘汇总已用字节 */
    val storageUsed: Long get() = disks.sumOf { it.used }
    /** 存储总览：所有磁盘汇总总容量字节 */
    val storageTotal: Long get() = disks.sumOf { it.total }
    /** 存储总览：所有磁盘汇总可用字节 */
    val storageFree: Long get() = (storageTotal - storageUsed).coerceAtLeast(0)
    /** 存储总使用率（0-100） */
    val storagePercent: Double get() =
        if (storageTotal > 0) storageUsed * 100.0 / storageTotal else 0.0
    /** 存储总览可读文本（可用/总量 使用率），无磁盘时返回 "--" */
    val storageText: String get() =
        if (disks.isEmpty()) "--"
        else String.format("%s/%s %.0f%%", Fmt.bytes(storageFree), Fmt.bytes(storageTotal), storagePercent)

    companion object {
        fun parse(json: String): NasStats {
            val root = JSONObject(json)
            val cpu = root.optJSONObject("cpu")
            val mem = root.optJSONObject("mem")
            val diskArr = root.optJSONArray("disk")
            val temps = root.optJSONObject("temps")

            val cpuPercent = cpu?.optDouble("currentLoad", 0.0) ?: 0.0
            val cores = cpu?.optInt("cores", 4) ?: 4
            val memPercent = mem?.optDouble("percent", 0.0) ?: 0.0
            val memUsed = mem?.optLong("used", 0L) ?: 0L
            val memTotal = mem?.optLong("total", 0L) ?: 0L

            val disks = mutableListOf<DiskStat>()
            if (diskArr != null) {
                for (i in 0 until diskArr.length()) {
                    val d = diskArr.optJSONObject(i) ?: continue
                    disks.add(
                        DiskStat(
                            name = d.optString("name", "disk$i"),
                            used = d.optLong("used", 0L),
                            total = d.optLong("size", 0L),
                            usePercent = d.optDouble("use", 0.0)
                        )
                    )
                }
            }

            val tempCpu = temps?.let { if (it.has("cpu")) it.getDouble("cpu") else null }
            val tempNvme = temps?.let { if (it.has("nvme")) it.getDouble("nvme") else null }
            val uptime = root.optLong("uptime", 0L)
            val net = root.optJSONObject("net")
            val netDown = net?.optLong("down", 0L) ?: 0L
            val netUp = net?.optLong("up", 0L) ?: 0L
            val sys = root.optJSONObject("sys")
            val cpuModel = sys?.optString("cpuModel", "") ?: ""
            val osVersion = sys?.optString("osVersion", "") ?: ""

            return NasStats(
                cpuPercent = cpuPercent,
                cores = cores,
                memPercent = memPercent,
                memUsed = memUsed,
                memTotal = memTotal,
                disks = disks,
                tempCpu = tempCpu,
                tempNvme = tempNvme,
                uptimeSec = uptime,
                netDown = netDown,
                netUp = netUp,
                cpuModel = cpuModel,
                osVersion = osVersion,
                fetchTime = System.currentTimeMillis()
            )
        }
    }
}

/** Docker 容器排序：运行中优先，其次按名称（组件/预览/配置列表统一顺序） */
val dockerContainerOrder = Comparator<DockerContainer> { a, b ->
    val r = b.running.compareTo(a.running)
    if (r != 0) r else a.name.compareTo(b.name)
}

/** Docker 容器排序比较器：status=运行中优先(默认)/name=名称/cpu=CPU降序/mem=内存降序 */
fun dockerComparator(sort: String): Comparator<DockerContainer> = when (sort) {
    "name" -> compareBy { it.name }
    "cpu" -> compareByDescending { it.cpuPercent ?: -1.0 }
    "mem" -> compareByDescending { it.memBytes ?: -1L }
    else -> dockerContainerOrder
}

/** 工具：字节转可读、秒转时长 */
object Fmt {
    /** 超阈值警示色（亮红，玻璃/深色底上均可见） */
    val ALERT_COLOR = 0xFFFF5252.toInt()

    fun bytes(b: Long): String {
        val gb = 1024.0 * 1024 * 1024
        val mb = 1024.0 * 1024
        return when {
            b >= gb -> String.format("%.1fG", b / gb)
            b >= mb -> String.format("%.0fM", b / mb)
            else -> String.format("%.0fK", b / 1024.0)
        }
    }

    fun speed(bps: Long): String {
        val kb = 1024.0
        val mb = 1024.0 * 1024
        val gb = 1024.0 * 1024 * 1024
        return when {
            bps >= gb -> String.format("%.1fG/s", bps / gb)
            bps >= mb -> String.format("%.1fM/s", bps / mb)
            bps >= kb -> String.format("%.0fK/s", bps / kb)
            else -> "${bps}B/s"
        }
    }

    fun uptime(sec: Long): String {
        val d = sec / 86400
        val h = (sec % 86400) / 3600
        val m = (sec % 3600) / 60
        return when {
            d > 0 -> "${d}天${h}时"
            h > 0 -> "${h}时${m}分"
            else -> "${m}分"
        }
    }

    /** 离线时长可读文本（从离线起始时间戳算起） */
    fun offlineDuration(sinceMillis: Long): String {
        if (sinceMillis <= 0) return "连接失败"
        val mins = (System.currentTimeMillis() - sinceMillis) / 60000
        return when {
            mins < 1 -> "离线 <1 分钟"
            mins < 60 -> "已离线 ${mins} 分钟"
            else -> "已离线 ${mins / 60} 时 ${mins % 60} 分"
        }
    }

    /** 使用率分级颜色：<60% 绿、60~85% 黄、≥85% 红（监控项进度条配色） */
    fun levelColor(pct: Double): Int = when {
        pct >= 85 -> 0xFFF44336.toInt()
        pct >= 60 -> 0xFFFFB74D.toInt()
        else -> 0xFF58D68D.toInt()
    }

    /** 时间戳毫秒 → 本地 HH:mm:ss（用于「最后更新时间」） */
    fun time(millis: Long): String {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = millis
        return String.format(
            "%02d:%02d:%02d",
            cal.get(java.util.Calendar.HOUR_OF_DAY),
            cal.get(java.util.Calendar.MINUTE),
            cal.get(java.util.Calendar.SECOND)
        )
    }
}
