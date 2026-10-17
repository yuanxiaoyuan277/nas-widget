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
    private const val KEY_SHOW_WB2API = "show_wb2api"
    private const val KEY_WB2API_BASE = "wb2api_base"
    private const val KEY_WB2API_KEY = "wb2api_key"

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

    // === 主题（浅色/深色） ===
    fun theme(ctx: Context): String = sp(ctx).getString(KEY_THEME, "dark") ?: "dark"
    fun setTheme(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_THEME, v).apply()
    fun isDarkTheme(ctx: Context): Boolean = theme(ctx) != "light"

    // === wb2api 积分 ===
    fun showWb2api(ctx: Context) = sp(ctx).getBoolean(KEY_SHOW_WB2API, false)
    fun setShowWb2api(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_SHOW_WB2API, v).apply()
    fun wb2apiBase(ctx: Context) = sp(ctx).getString(KEY_WB2API_BASE, DEFAULT_WB2API_BASE) ?: DEFAULT_WB2API_BASE
    fun setWb2apiBase(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_WB2API_BASE, v).apply()
    fun wb2apiKey(ctx: Context) = sp(ctx).getString(KEY_WB2API_KEY, DEFAULT_WB2API_KEY) ?: DEFAULT_WB2API_KEY
    fun setWb2apiKey(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_WB2API_KEY, v).apply()
}
