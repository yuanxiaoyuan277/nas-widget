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
    val memBytes: Long? = null
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

    /** 运行时长文本（从 Status 提取 "Up 2 days" 这类，清理掉 "(healthy)" 等后缀） */
    val uptimeText: String get() {
        if (!running) return ""
        val cleaned = status.substringBefore(" (").trim()
        return if (cleaned.startsWith("Up ")) cleaned else ""
    }

    /** 状态 + 详情（如 "运行 · Up 2 days"、"异常 · 137"） */
    val stateText: String get() =
        if (status.isNotEmpty() && !running) "$stateLabel · $status" else stateLabel

    /** 显示用完整详情：运行态拼 内存 + 运行时长；非运行态用 stateText */
    val detailText: String get() {
        if (running) {
            val parts = mutableListOf(stateLabel)
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

/** 工具：字节转可读、秒转时长 */
object Fmt {
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
