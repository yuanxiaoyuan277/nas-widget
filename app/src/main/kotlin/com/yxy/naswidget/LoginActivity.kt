package com.yxy.naswidget

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class LoginActivity : AppCompatActivity() {

    private lateinit var etHost: EditText
    private lateinit var btnGo: Button
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(
            if (Prefs.isDarkTheme(this)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etHost = findViewById(R.id.et_host)
        btnGo = findViewById(R.id.btn_go)
        etHost.setText(Prefs.nasBase(this))

        btnGo.setOnClickListener {
            val host = Prefs.normalizeNasBase(etHost.text.toString())
            Prefs.setNasBase(this, host)
            openBrowserForLogin(host)
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** 处理飞牛授权回调（naswidget://oauth/callback?code=xxx&state=xxx） */
    private fun handleIntent(intent: Intent?) {
        if (handled) return
        if (Intent.ACTION_VIEW == intent?.action && intent.data != null) {
            val uri = intent.data!!
            if (uri.toString().startsWith(OAuthClient.REDIRECT_URI)) {
                handled = true
                handleCallback(uri)
            }
        }
    }

    private fun openBrowserForLogin(nasBase: String) {
        val (verifier, challenge) = OAuthClient.generatePkce()
        Prefs.setPendingVerifier(this, verifier)
        val state = java.util.UUID.randomUUID().toString().replace("-", "")
        val deviceId = Prefs.deviceId(this)
        val authUrl = OAuthClient.buildAuthUrl(nasBase, challenge, state, deviceId)

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)))
            Toast.makeText(this, "请在浏览器中完成登录授权，完成后自动跳回", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开浏览器：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleCallback(uri: Uri) {
        val code = uri.getQueryParameter("code")
        if (code.isNullOrEmpty()) {
            Toast.makeText(this, "授权回调缺少 code", Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        val verifier = Prefs.pendingVerifier(this)
        if (verifier.isEmpty()) {
            Toast.makeText(this, "登录状态失效，请重新登录", Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        Prefs.clearPendingVerifier(this)
        exchangeAndFinish(code, verifier)
    }

    private fun exchangeAndFinish(code: String, verifier: String) {
        Toast.makeText(this, "登录中…", Toast.LENGTH_SHORT).show()
        Thread {
            val nasBase = Prefs.nasBase(this)
            val result = OAuthClient.exchangeCode(nasBase, code, verifier)
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
