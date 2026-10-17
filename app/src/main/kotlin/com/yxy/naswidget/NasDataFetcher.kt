package com.yxy.naswidget

import java.net.HttpURLConnection
import java.net.URL

/** 从 monitor_proxy 拉取 NAS 数据 */
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
}
