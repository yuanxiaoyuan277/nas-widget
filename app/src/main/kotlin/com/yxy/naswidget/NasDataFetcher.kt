package com.yxy.naswidget

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/** 从 monitor_proxy / 飞牛 API 拉取 NAS 数据 */
object NasDataFetcher {

    fun fetch(url: String): Result<NasStats> {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code != 200) {
                conn.disconnect()
                return Result.failure(Exception("HTTP $code"))
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            Result.success(NasStats.parse(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 从飞牛 appcgi API 拉取 Docker 容器列表（含运行中+停止） */
    fun fetchDocker(nasBase: String, accessToken: String): Result<List<DockerContainer>> {
        return try {
            val url = URL(nasBase.trimEnd('/') + "/ogh/ac/h/appcgi.dockermgr.containerList")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Trim-MC-token", accessToken)
            conn.doOutput = true

            val body = JSONObject().apply {
                put("req", "appcgi.dockermgr.containerList")
                put("reqid", java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                put("all", true)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            val code = conn.responseCode
            val resp = conn.inputStream?.bufferedReader()?.use { it.readText() }
                ?: conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()

            if (code != 200) return Result.failure(Exception("HTTP $code: $resp"))

            val root = JSONObject(resp)
            // 飞牛返回 {result:"succ", data:[...]} 或直接 data 数组
            val arr = root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("containers")
            val list = mutableListOf<DockerContainer>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val c = arr.optJSONObject(i) ?: continue
                    val names = c.optJSONArray("Names")
                    val rawName = if (names != null && names.length() > 0)
                        names.optString(0) else c.optString("Name", "")
                    val name = rawName.removePrefix("/")
                    val state = c.optString("State", c.optString("state", ""))
                    val status = c.optString("Status", c.optString("status", ""))
                    if (name.isNotEmpty()) {
                        list.add(DockerContainer(name, state, status))
                    }
                }
            }
            if (list.isEmpty() && !root.optString("result", "").equals("succ", true)) {
                return Result.failure(Exception("响应异常: $resp"))
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
