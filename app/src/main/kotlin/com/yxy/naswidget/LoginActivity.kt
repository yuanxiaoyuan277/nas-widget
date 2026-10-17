package com.yxy.naswidget

import android.app.Activity
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var codeVerifier = ""
    private var state = ""
    private var exchanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        val (verifier, challenge) = OAuthClient.generatePkce()
        codeVerifier = verifier
        state = randomState()

        val nasBase = Prefs.nasBase(this)
        val authUrl = OAuthClient.buildAuthUrl(nasBase, challenge, state)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                handleCallback(request.url.toString())

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                handleCallback(url)

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    Toast.makeText(
                        this@LoginActivity,
                        "无法连接飞牛，请确认已连家里 WiFi 或地址可达",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        webView.loadUrl(authUrl)
    }

    private fun randomState(): String =
        java.util.UUID.randomUUID().toString().replace("-", "")

    private fun handleCallback(url: String): Boolean {
        if (!url.startsWith(OAuthClient.REDIRECT_URI)) return false
        if (exchanged) return true
        val pair = OAuthClient.parseCallback(url)
        if (pair == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return true
        }
        val (code, _) = pair
        exchanged = true
        exchangeAndFinish(code)
        return true
    }

    private fun exchangeAndFinish(code: String) {
        Toast.makeText(this, "登录中…", Toast.LENGTH_SHORT).show()
        Thread {
            val nasBase = Prefs.nasBase(this)
            val result = OAuthClient.exchangeCode(nasBase, code, codeVerifier)
            runOnUiThread {
                result.onSuccess { token ->
                    Prefs.saveToken(this, token.accessToken, token.refreshToken, token.expiresIn)
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }.onFailure { e ->
                    Toast.makeText(this, "登录失败：${e.message}", Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        }.start()
    }
}
