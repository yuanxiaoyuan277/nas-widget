package com.yxy.naswidget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 配置备份/恢复：导出为 JSON，从 JSON 恢复。
 *  只保存 App 设置与用户输入的域名/地址，不保存 token、密钥等敏感凭据。 */
object BackupHelper {

    fun export(context: Context): String {
        val j = JSONObject()
        // 域名/地址
        j.put("url", Prefs.url(context))
        j.put("nas_base", Prefs.nasBase(context))
        j.put("wb2api_base", Prefs.wb2apiBase(context))
        // App 设置
        j.put("refresh_sec", Prefs.refreshSec(context))
        j.put("show_cpu", Prefs.showCpu(context))
        j.put("show_mem", Prefs.showMem(context))
        j.put("show_disk1", Prefs.showDisk1(context))
        j.put("show_disk2", Prefs.showDisk2(context))
        j.put("show_temp", Prefs.showTemp(context))
        j.put("show_uptime", Prefs.showUptime(context))
        j.put("show_docker", Prefs.showDocker(context))
        j.put("show_net", Prefs.showNet(context))
        j.put("show_wb2api", Prefs.showWb2api(context))
        j.put("docker_containers", JSONArray(Prefs.dockerContainers(context).toList()))
        j.put("font_size", Prefs.fontSize(context))
        j.put("cpu_x", Prefs.compX(context, "cpu"))
        j.put("cpu_y", Prefs.compY(context, "cpu"))
        j.put("mem_x", Prefs.compX(context, "mem"))
        j.put("mem_y", Prefs.compY(context, "mem"))
        j.put("disk1_x", Prefs.compX(context, "disk1"))
        j.put("disk1_y", Prefs.compY(context, "disk1"))
        j.put("disk2_x", Prefs.compX(context, "disk2"))
        j.put("disk2_y", Prefs.compY(context, "disk2"))
        j.put("docker_x", Prefs.compX(context, "docker"))
        j.put("docker_y", Prefs.compY(context, "docker"))
        j.put("net_x", Prefs.compX(context, "net"))
        j.put("net_y", Prefs.compY(context, "net"))
        j.put("theme", Prefs.theme(context))
        // 不导出 access_token / refresh_token / wb2api_key 等敏感凭据
        return j.toString()
    }

    fun import(context: Context, jsonStr: String): Boolean {
        return try {
            val j = JSONObject(jsonStr)
            Prefs.setUrl(context, j.optString("url", Prefs.DEFAULT_URL))
            Prefs.setNasBase(context, j.optString("nas_base", Prefs.DEFAULT_NAS_BASE))
            Prefs.setWb2apiBase(context, j.optString("wb2api_base", Prefs.DEFAULT_WB2API_BASE))
            Prefs.setRefresh(context, j.optInt("refresh_sec", 30))
            Prefs.setShowCpu(context, j.optBoolean("show_cpu", true))
            Prefs.setShowMem(context, j.optBoolean("show_mem", true))
            Prefs.setShowDisk1(context, j.optBoolean("show_disk1", true))
            Prefs.setShowDisk2(context, j.optBoolean("show_disk2", true))
            Prefs.setShowTemp(context, j.optBoolean("show_temp", true))
            Prefs.setShowUptime(context, j.optBoolean("show_uptime", true))
            Prefs.setShowDocker(context, j.optBoolean("show_docker", false))
            Prefs.setShowNet(context, j.optBoolean("show_net", false))
            Prefs.setShowWb2api(context, j.optBoolean("show_wb2api", false))
            val arr = j.optJSONArray("docker_containers")
            val containers = mutableSetOf<String>()
            if (arr != null) for (i in 0 until arr.length()) containers.add(arr.optString(i))
            Prefs.setDockerContainers(context, containers)
            Prefs.setFontSize(context, j.optInt("font_size", 16))
            Prefs.setCompX(context, "cpu", j.optInt("cpu_x", 0))
            Prefs.setCompY(context, "cpu", j.optInt("cpu_y", 0))
            Prefs.setCompX(context, "mem", j.optInt("mem_x", 0))
            Prefs.setCompY(context, "mem", j.optInt("mem_y", 0))
            Prefs.setCompX(context, "disk1", j.optInt("disk1_x", 0))
            Prefs.setCompY(context, "disk1", j.optInt("disk1_y", 0))
            Prefs.setCompX(context, "disk2", j.optInt("disk2_x", 0))
            Prefs.setCompY(context, "disk2", j.optInt("disk2_y", 0))
            Prefs.setCompX(context, "docker", j.optInt("docker_x", 0))
            Prefs.setCompY(context, "docker", j.optInt("docker_y", 0))
            Prefs.setCompX(context, "net", j.optInt("net_x", 0))
            Prefs.setCompY(context, "net", j.optInt("net_y", 0))
            Prefs.setTheme(context, j.optString("theme", "dark"))
            // 不恢复 token / wb2api_key，登录与密钥由用户重新输入
            true
        } catch (e: Exception) {
            false
        }
    }
}
