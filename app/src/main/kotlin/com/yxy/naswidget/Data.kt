package com.yxy.naswidget

import org.json.JSONObject

/** NAS 监控数据模型（对应 monitor_proxy 的 /nas 返回） */
data class DiskStat(val name: String, val used: Long, val total: Long, val usePercent: Double)

/** Docker 容器（来自飞牛 appcgi.dockermgr.containerList） */
data class DockerContainer(val name: String, val state: String, val status: String) {
    val running: Boolean get() = state.equals("running", ignoreCase = true)
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
}
