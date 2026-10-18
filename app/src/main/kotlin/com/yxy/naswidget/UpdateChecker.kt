package com.yxy.naswidget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** 应用内更新：检查公网 version.json、下载 APK、跳转安装 */
object UpdateChecker {

    private const val UPDATE_BASE = "https://411223.xyz:44444"
    private const val VERSION_URL = "$UPDATE_BASE/version.json"

    data class UpdateInfo(val version: String, val url: String, val size: Long, val notes: String)

    /** 获取本地版本号 */
    fun localVersion(context: Context): String =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (_: Exception) {
            "0.0.0"
        }

    /** 检查最新版本：读公网 version.json */
    fun checkLatest(): Result<UpdateInfo> {
        return try {
            val conn = URL(VERSION_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "nas-widget-updater")
            val code = conn.responseCode
            if (code != 200) {
                conn.disconnect()
                return Result.failure(Exception("HTTP $code"))
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val root = JSONObject(body)
            val version = root.optString("version", "").removePrefix("v")
            val apk = root.optString("apk", "")
            val notes = root.optString("notes", "")
            if (version.isEmpty() || apk.isEmpty()) {
                return Result.failure(Exception("version.json 格式错误"))
            }
            val url = "$UPDATE_BASE/$apk"
            Result.success(UpdateInfo(version, url, 0L, notes))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 版本比较：newVer > oldVer 返回 true */
    fun isNewer(newVer: String, oldVer: String): Boolean {
        fun toInts(v: String): List<Int> =
            v.split(".").map { it.toIntOrNull() ?: 0 }
        val a = toInts(newVer)
        val b = toInts(oldVer)
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** 下载 APK 到 cache 目录，回调进度(0-100) */
    fun download(url: String, dest: File, onProgress: ((Int) -> Unit)? = null): Result<File> {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 30000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "nas-widget-updater")
            val code = conn.responseCode
            if (code != 200) {
                conn.disconnect()
                return Result.failure(Exception("下载失败 HTTP $code"))
            }
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                dest.outputStream().use { output ->
                    val buf = ByteArray(8192)
                    var read = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress?.invoke((read * 100 / total).toInt())
                    }
                }
            }
            conn.disconnect()
            Result.success(dest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 跳转系统安装器安装 APK */
    fun install(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
