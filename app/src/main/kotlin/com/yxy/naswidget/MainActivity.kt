package com.yxy.naswidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val refreshOptions = listOf(10 to "10 秒", 15 to "15 秒", 30 to "30 秒", 60 to "60 秒", 300 to "5 分钟")

    private lateinit var etUrl: EditText
    private lateinit var spRefresh: Spinner
    private lateinit var cbCpu: CheckBox
    private lateinit var cbMem: CheckBox
    private lateinit var cbDisk1: CheckBox
    private lateinit var cbDisk2: CheckBox
    private lateinit var cbTemp: CheckBox
    private lateinit var cbUptime: CheckBox
    private lateinit var tvVersion: TextView
    private lateinit var tvUpdateStatus: TextView
    private lateinit var btnCheckUpdate: Button
    private lateinit var tvLoginStatus: TextView
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etUrl = findViewById(R.id.et_url)
        spRefresh = findViewById(R.id.sp_refresh)
        cbCpu = findViewById(R.id.cb_cpu)
        cbMem = findViewById(R.id.cb_mem)
        cbDisk1 = findViewById(R.id.cb_disk1)
        cbDisk2 = findViewById(R.id.cb_disk2)
        cbTemp = findViewById(R.id.cb_temp)
        cbUptime = findViewById(R.id.cb_uptime)
        tvVersion = findViewById(R.id.tv_version)
        tvUpdateStatus = findViewById(R.id.tv_update_status)
        btnCheckUpdate = findViewById(R.id.btn_check_update)
        tvLoginStatus = findViewById(R.id.tv_login_status)
        btnLogin = findViewById(R.id.btn_login)

        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
        }

        val labels = refreshOptions.map { it.second }
        spRefresh.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)

        btnCheckUpdate.setOnClickListener { checkUpdate() }
        btnLogin.setOnClickListener {
            startActivityForResult(Intent(this, LoginActivity::class.java), REQ_LOGIN)
        }
        findViewById<Button>(R.id.btn_save).setOnClickListener { save() }

        // 强制登录门：未登录直接跳登录页
        if (!Prefs.isLoggedIn(this)) {
            startActivityForResult(Intent(this, LoginActivity::class.java), REQ_LOGIN)
            return
        }

        loadPrefs()
        tvVersion.text = "v${UpdateChecker.localVersion(this)}"
        updateLoginStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_LOGIN) {
            if (resultCode == RESULT_OK) {
                loadPrefs()
                tvVersion.text = "v${UpdateChecker.localVersion(this)}"
                updateLoginStatus()
            } else {
                finish()
            }
        }
    }

    companion object {
        private const val REQ_LOGIN = 1001
    }

    private fun updateLoginStatus() {
        val logged = Prefs.isLoggedIn(this)
        tvLoginStatus.text = if (logged) "已登录飞牛账号" else "未登录"
        btnLogin.text = if (logged) "重新登录" else "登录飞牛账号"
    }

    private fun loadPrefs() {
        etUrl.setText(Prefs.url(this))
        val cur = Prefs.refreshSec(this)
        val idx = refreshOptions.indexOfFirst { it.first == cur }
        spRefresh.setSelection(if (idx >= 0) idx else 2)
        cbCpu.isChecked = Prefs.showCpu(this)
        cbMem.isChecked = Prefs.showMem(this)
        cbDisk1.isChecked = Prefs.showDisk1(this)
        cbDisk2.isChecked = Prefs.showDisk2(this)
        cbTemp.isChecked = Prefs.showTemp(this)
        cbUptime.isChecked = Prefs.showUptime(this)
    }

    private fun save() {
        val url = etUrl.text.toString().trim().ifEmpty { Prefs.DEFAULT_URL }
        Prefs.setUrl(this, url)
        val sel = spRefresh.selectedItemPosition
        Prefs.setRefresh(this, refreshOptions.getOrElse(sel) { 30 to "30 秒" }.first)
        Prefs.setShowCpu(this, cbCpu.isChecked)
        Prefs.setShowMem(this, cbMem.isChecked)
        Prefs.setShowDisk1(this, cbDisk1.isChecked)
        Prefs.setShowDisk2(this, cbDisk2.isChecked)
        Prefs.setShowTemp(this, cbTemp.isChecked)
        Prefs.setShowUptime(this, cbUptime.isChecked)

        val am = AppWidgetManager.getInstance(this)
        val ids = am.getAppWidgetIds(ComponentName(this, NasWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            NasWidgetProvider.updateAll(this, am, ids)
        }
        AlarmScheduler.schedule(this)

        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, result)
        }
        finish()
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
                        tvUpdateStatus.text = "发现新版本 v${info.version}，开始下载…"
                        downloadAndInstall(info)
                    } else {
                        tvUpdateStatus.text = "已是最新版本 v$local"
                    }
                }.onFailure { e ->
                    tvUpdateStatus.text = "检查失败：${e.message}"
                }
            }
        }.start()
    }

    private fun downloadAndInstall(info: UpdateChecker.UpdateInfo) {
        Thread {
            val dest = File(cacheDir, "nas-widget-update.apk")
            val result = UpdateChecker.download(info.url, dest) { p ->
                runOnUiThread { tvUpdateStatus.text = "下载中 $p%" }
            }
            runOnUiThread {
                result.onSuccess {
                    tvUpdateStatus.text = "下载完成，准备安装…"
                    UpdateChecker.install(this, it)
                }.onFailure { e ->
                    tvUpdateStatus.text = "下载失败：${e.message}"
                }
            }
        }.start()
    }
}
