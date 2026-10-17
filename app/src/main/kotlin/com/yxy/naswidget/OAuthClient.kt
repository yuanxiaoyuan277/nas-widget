package com.yxy.naswidget

import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import org.json.JSONObject

/** 飞牛 fnOS OAuth 客户端（public 客户端 PKCE 授权码流程） */
object OAuthClient {

    const val CLIENT_ID = "YJNMPJUGA9"
    const val REDIRECT_URI = "naswidget://oauth/callback"
    const val SCOPE = "trim.user.all trim.file.read trim.file.write trim.download.all trim.appcenter.all trim.photo.all trim.baidu_netdisk.all trim.system.all trim.log.all trim.storage.all trim.docker.all"

    /** 生成 PKCE：返回 (code_verifier, code_challenge) */
    fun generatePkce(): Pair<String, String> {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val verifier = base64Url(bytes)
        val challenge = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
        return verifier to challenge
    }

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    /** 构造授权 URL（带 redirect_uri，授权后跳回 App） */
    fun buildAuthUrl(nasBase: String, codeChallenge: String, state: String, deviceId: String): String {
        val base = nasBase.trimEnd('/')
        val sb = StringBuilder(base).append("/signin?")
        sb.append("client_id=").append(CLIENT_ID)
        sb.append("&redirect_uri=").append(java.net.URLEncoder.encode(REDIRECT_URI, "UTF-8"))
        sb.append("&scope=").append(java.net.URLEncoder.encode(SCOPE, "UTF-8"))
        sb.append("&code_challenge=").append(codeChallenge)
        sb.append("&code_challenge_method=S256")
        sb.append("&device_id=").append(java.net.URLEncoder.encode(deviceId, "UTF-8"))
        sb.append("&state=").append(state)
        return sb.toString()
    }

    data class TokenResult(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Long
    )

    /** 用授权码换 token（第三方 PKCE 换 token 接口，需带 client_id） */
    fun exchangeCode(nasBase: String, code: String, codeVerifier: String): Result<TokenResult> {
        return try {
            val url = URL(nasBase.trimEnd('/') + "/oauthapi/third-part/token")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true

            val body = JSONObject().apply {
                put("client_id", CLIENT_ID)
                put("code", code)
                put("code_verifier", codeVerifier)
                put("device_name", "nas-widget")
                put("device_model", "android")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            val code_ = conn.responseCode
            val resp = conn.inputStream?.bufferedReader()?.use { it.readText() }
                ?: conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()

            if (code_ != 200) return Result.failure(Exception("HTTP $code_: $resp"))

            val root = JSONObject(resp)
            val data = root.optJSONObject("data") ?: root
            val accessToken = data.optString("accessToken").ifEmpty { data.optString("access_token") }
            val refreshToken = data.optString("refreshToken").ifEmpty { data.optString("refresh_token") }
            val expiresIn = data.optLong("expiresIn", data.optLong("expires_in", 0L))
            if (accessToken.isEmpty()) return Result.failure(Exception("响应无 accessToken: $resp"))

            Result.success(TokenResult(accessToken, refreshToken.ifEmpty { null }, expiresIn))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
