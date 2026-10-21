package com.yxy.naswidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File

/** 设置页：NAS 地址、wb2api 积分、外观（深色浅色）、检查更新 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val REQ_LOGIN = 1001
    }

    private lateinit var scrollRoot: ScrollView
    private lateinit var tvLoginStatus: TextView
    private lateinit var btnLogin: Button
    private lateinit var etUrl: EditText
    private lateinit var cbWb2api: CheckBox
    private lateinit var etWb2apiBase: EditText
    private lateinit var etWb2apiKey: EditText
    private lateinit var btnToggleTheme: Button
    private lateinit var tvVersion: TextView
    private lateinit var btnCheckUpdate: Button
    private lateinit var tvUpdateStatus: TextView
    private lateinit var btnBackup: Button
    private lateinit var btnRestore: Button
    private lateinit var tvBackupStatus: TextView
    private lateinit var bottomNav: BottomNavigationView
    private var suppressSave = false
    private val saveHandler = Handler(Looper.getMainLooper())
    private val saveRunnable = Runnable { persist() }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(
            if (Prefs.isDarkTheme(this)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        scrollRoot = findViewById(R.id.settings_scroll)
        tvLoginStatus = findViewById(R.id.tv_login_status)
        btnLogin = findViewById(R.id.btn_login)
        etUrl = findViewById(R.id.et_url)
        cbWb2api = findViewById(R.id.cb_wb2api)
        etWb2apiBase = findViewById(R.id.et_wb2api_base)
        etWb2apiKey = findViewById(R.id.et_wb2api_key)
        btnToggleTheme = findViewById(R.id.btn_toggle_theme)
        tvVersion = findViewById(R.id.tv_version)
        btnCheckUpdate = findViewById(R.id.btn_check_update)
        tvUpdateStatus = findViewById(R.id.tv_update_status)
        btnBackup = findViewById(R.id.btn_backup)
        btnRestore = findViewById(R.id.btn_restore)
        tvBackupStatus = findViewById(R.id.tv_backup_status)
        bottomNav = findViewById(R.id.bottom_nav)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finishConfig() }
        bottomNav.selectedItemId = R.id.nav_settings
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    true
                }
                else -> true
            }
        }
        btnLogin.setOnClickListener {
            startActivityForResult(Intent(this, LoginActivity::class.java), REQ_LOGIN)
        }
        btnToggleTheme.setOnClickListener { toggleTheme() }
        btnCheckUpdate.setOnClickListener { checkUpdate() }
        btnBackup.setOnClickListener { backupConfig() }
        btnRestore.setOnClickListener { restoreConfig() }
        cbWb2api.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        watchEditText(etUrl)
        watchEditText(etWb2apiBase)
        watchEditText(etWb2apiKey)

        // 输入框默认隐藏 + 显示/隐藏切换
        setupToggle(etUrl, findViewById(R.id.btn_toggle_url))
        setupToggle(etWb2apiBase, findViewById(R.id.btn_toggle_wb2api_base))
        setupToggle(etWb2apiKey, findViewById(R.id.btn_toggle_wb2api_key))

        // 键盘遮挡自动滚动
        setupAutoScroll(etUrl)
        setupAutoScroll(etWb2apiBase)
        setupAutoScroll(etWb2apiKey)

        loadPrefs()

        // 返回键即完成（改动已自动保存）
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishConfig()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_LOGIN && resultCode == RESULT_OK) {
            updateLoginStatus()
        }
    }

    private fun updateLoginStatus() {
        val logged = Prefs.isLoggedIn(this)
        tvLoginStatus.text = if (logged) "已登录飞牛账号" else "未登录"
        btnLogin.text = if (logged) "重新登录" else "登录飞牛账号"
    }

    private fun loadPrefs() {
        suppressSave = true
        updateLoginStatus()
        etUrl.setText(Prefs.url(this))
        cbWb2api.isChecked = Prefs.showWb2api(this)
        etWb2apiBase.setText(Prefs.wb2apiBase(this))
        etWb2apiKey.setText(Prefs.wb2apiKey(this))
        tvVersion.text = "v${UpdateChecker.localVersion(this)}"
        updateThemeButton()
        suppressSave = false
    }

    private fun updateThemeButton() {
        btnToggleTheme.text = if (Prefs.isDarkTheme(this)) "深色" else "浅色"
    }

    private fun toggleTheme() {
        val newTheme = if (Prefs.isDarkTheme(this)) "light" else "dark"
        Prefs.setTheme(this, newTheme)
        AppCompatDelegate.setDefaultNightMode(
            if (newTheme == "dark") AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /** 输入框内容明文/密文切换（默认隐藏） */
    private fun setupToggle(editText: EditText, toggleBtn: TextView) {
        editText.transformationMethod = PasswordTransformationMethod.getInstance()
        toggleBtn.text = "显示"
        toggleBtn.setOnClickListener {
            val hidden = editText.transformationMethod is PasswordTransformationMethod
            if (hidden) {
                editText.transformationMethod = null
                toggleBtn.text = "隐藏"
            } else {
                editText.transformationMethod = PasswordTransformationMethod.getInstance()
                toggleBtn.text = "显示"
            }
            editText.setSelection(editText.text.length)
        }
    }

    /** 输入框获得焦点时滚动 ScrollView，避免键盘遮挡 */
    private fun setupAutoScroll(editText: EditText) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollRoot.postDelayed({
                    var y = 0
                    var v: View? = editText
                    while (v != null && v !== scrollRoot) {
                        y += v.top
                        v = v.parent as? View
                    }
                    scrollRoot.smoothScrollTo(0, maxOf(0, y - scrollRoot.height / 3))
                }, 200)
            }
        }
    }

    private fun watchEditText(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { if (!suppressSave) scheduleSave() }
        })
    }

    private fun scheduleSave() {
        saveHandler.removeCallbacks(saveRunnable)
        saveHandler.postDelayed(saveRunnable, 400)
    }

    private fun persist() {
        val url = etUrl.text.toString().trim().ifEmpty { Prefs.DEFAULT_URL }
        Prefs.setUrl(this, url)
        Prefs.setShowWb2api(this, cbWb2api.isChecked)
        Prefs.setWb2apiBase(this, etWb2apiBase.text.toString().trim().ifEmpty { Prefs.DEFAULT_WB2API_BASE })
        Prefs.setWb2apiKey(this, etWb2apiKey.text.toString().trim().ifEmpty { Prefs.DEFAULT_WB2API_KEY })

        val am = AppWidgetManager.getInstance(this)
        val ids = am.getAppWidgetIds(ComponentName(this, NasWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            NasWidgetProvider.updateAll(this, am, ids)
        }
    }

    private fun finishConfig() {
        saveHandler.removeCallbacks(saveRunnable)
        persist()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveHandler.removeCallbacksAndMessages(null)
    }

    private fun backupConfig() {
        val json = BackupHelper.export(this)
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("nas-widget-config", json))
        tvBackupStatus.text = "配置已复制到剪贴板"
        Toast.makeText(this, "配置已复制，可粘贴到微信/记事本保存", Toast.LENGTH_LONG).show()
    }

    private fun restoreConfig() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(this).toString()
        if (text.isBlank()) {
            tvBackupStatus.text = "剪贴板为空"
            return
        }
        if (BackupHelper.import(this, text)) {
            tvBackupStatus.text = "配置已恢复"
            Toast.makeText(this, "配置已恢复，部分设置重启后生效", Toast.LENGTH_LONG).show()
            loadPrefs()
        } else {
            tvBackupStatus.text = "恢复失败：配置格式无效"
        }
    }

    private fun checkUpdate() {
        btnCheckUpdate.isEnabled = false
        tvUpdateStatus.text = "检查更新中…"
        Thread {
            val result = UpdateChecker.checkLatest()
            runOnUiThread {
                btnCheckUpdate.isEnabled = true
                result.onSuccess { info ->
                    val local = UpdateChecker.localVersion(this)
                    if (UpdateChecker.isNewer(info.version, local)) {
                        tvUpdateStatus.text = ""
                        UpdateChecker.showUpdateDialog(this, info)
                    } else {
                        tvUpdateStatus.text = "已是最新版本 v$local"
                    }
                }.onFailure { e ->
                    tvUpdateStatus.text = "检查失败：${e.message}"
                }
            }
        }.start()
    }
}
