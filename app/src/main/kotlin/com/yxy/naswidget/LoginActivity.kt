package com.yxy.naswidget

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etHost: EditText
    private lateinit var btnGo: Button
    private lateinit var webView: WebView
    private var codeVerifier = ""
    private var exchanged = false
    private var extractCount = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etHost = findViewById(R.id.et_host)
        btnGo = findViewById(R.id.btn_go)
        webView = findViewById(R.id.webview)

        etHost.setText(Prefs.nasBase(this))

        btnGo.setOnClickListener {
            val host = Prefs.normalizeNasBase(etHost.text.toString())
            Prefs.setNasBase(this, host)
            startLogin(host)
        }
    }

    private fun startLogin(nasBase: String) {
        btnGo.isEnabled = false
        webView.visibility = View.VISIBLE
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        val (verifier, challenge) = OAuthClient.generatePkce()
        codeVerifier = verifier
        val state = java.util.UUID.randomUUID().toString().replace("-", "")
        val deviceId = Prefs.deviceId(this)
        val authUrl = OAuthClient.buildAuthUrl(nasBase, challenge, state, deviceId)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                handleUrl(request.url.toString())

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                handleUrl(url)

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (!exchanged) scheduleExtract()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    Toast.makeText(
                        this@LoginActivity,
                        "无法连接飞牛，请检查地址是否正确",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        webView.loadUrl(authUrl)
    }

    /** 拦截飞牛授权后的跳转（redirect_uri?code=xxx），或返回 false 放行 */
    private fun handleUrl(url: String): Boolean {
        val code = extractCodeFromUrl(url)
        if (code != null && !exchanged) {
            exchanged = true
            exchangeAndFinish(code)
            return true
        }
        return false
    }

    private fun extractCodeFromUrl(url: String): String? {
        return try {
            val code = Uri.parse(url).getQueryParameter("code")
            if (!code.isNullOrEmpty() && code.length in 4..256) code else null
        } catch (_: Exception) {
            null
        }
    }

    /** 授权成功后飞牛页面会显示授权码（<code> 元素），定时轮询提取 */
    private fun scheduleExtract() {
        handler.postDelayed({
            if (exchanged || isFinishing) return@postDelayed
            extractCount++
            if (extractCount > 90) {
                diagnose()
                return@postDelayed
            }
            extractCode { code ->
                if (exchanged) return@extractCode
                if (code.isNotEmpty()) {
                    exchanged = true
                    exchangeAndFinish(code)
                } else {
                    scheduleExtract()
                }
            }
        }, 1000)
    }

    private fun extractCode(callback: (String) -> Unit) {
        val js = "(function(){var cs=document.querySelectorAll('code');for(var i=0;i<cs.length;i++){var t=(cs[i].textContent||'').trim();if(t.length>=4&&t.length<=256)return t;}return '';})()"
        webView.evaluateJavascript(js) { value ->
            val code = value?.trim()?.removeSurrounding("\"")?.trim() ?: ""
            callback(code)
        }
    }

    /** 提取失败时，抓页面文本，帮助定位 */
    private fun diagnose() {
        val js = "(function(){var b=document.body?(document.body.innerText||'').replace(/\\s+/g,' ').trim():'';return b?b.substring(0,80):'[页面空白]';})()"
        webView.evaluateJavascript(js) { value ->
            val info = value?.trim()?.removeSurrounding("\"")?.trim() ?: ""
            Toast.makeText(this, "页面内容：$info", Toast.LENGTH_LONG).show()
        }
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
