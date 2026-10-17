package com.yxy.naswidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val refreshOptions = listOf(10 to "10 秒", 15 to "15 秒", 30 to "30 秒", 60 to "60 秒", 300 to "5 分钟")

    private lateinit var etUrl: EditText
    private lateinit var spRefresh: Spinner
    private lateinit var scrollRoot: ScrollView
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
    private lateinit var btnToggleTheme: Button
    private lateinit var previewContainer: FrameLayout
    private lateinit var cbDocker: CheckBox
    private lateinit var dockerListContainer: LinearLayout
    private lateinit var btnRefreshDocker: Button
    private lateinit var cbWb2api: CheckBox
    private lateinit var etWb2apiBase: EditText
    private lateinit var etWb2apiKey: EditText

    private val handler = Handler(Looper.getMainLooper())
    private var dockerContainers: List<DockerContainer> = emptyList()
    private val dockerCheckboxes = mutableListOf<CheckBox>()
    private var previewBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        AppCompatDelegate.setDefaultNightMode(
            if (Prefs.isDarkTheme(this)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etUrl = findViewById(R.id.et_url)
        spRefresh = findViewById(R.id.sp_refresh)
        scrollRoot = findViewById(R.id.scroll_root)
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
        btnToggleTheme = findViewById(R.id.btn_toggle_theme)
        previewContainer = findViewById(R.id.preview_container)
        cbDocker = findViewById(R.id.cb_docker)
        dockerListContainer = findViewById(R.id.docker_list_container)
        btnRefreshDocker = findViewById(R.id.btn_refresh_docker)
        cbWb2api = findViewById(R.id.cb_wb2api)
        etWb2apiBase = findViewById(R.id.et_wb2api_base)
        etWb2apiKey = findViewById(R.id.et_wb2api_key)

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
        btnToggleTheme.setOnClickListener { toggleTheme() }
        updateThemeButton()
        findViewById<Button>(R.id.btn_save).setOnClickListener { save() }

        // 输入框获得焦点时自动滚动，避免被键盘遮挡
        setupAutoScroll(etUrl)
        setupAutoScroll(etWb2apiBase)
        setupAutoScroll(etWb2apiKey)

        // 小组件预览
        layoutInflater.inflate(R.layout.nas_widget_preview, previewContainer, true)

        // Docker 配置
        cbDocker.isChecked = Prefs.showDocker(this)
        cbDocker.setOnCheckedChangeListener { _, _ -> refreshDockerVisibility() }
        btnRefreshDocker.setOnClickListener { loadDockerContainers() }

        // 强制登录门
        if (!Prefs.isLoggedIn(this)) {
            startActivityForResult(Intent(this, LoginActivity::class.java), REQ_LOGIN)
            return
        }

        loadPrefs()
        tvVersion.text = "v${UpdateChecker.localVersion(this)}"
        updateLoginStatus()
        refreshDockerVisibility()
        startPreviewRefresh()
    }

    override fun onResume() {
        super.onResume()
        if (Prefs.isLoggedIn(this)) {
            updateLoginStatus()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_LOGIN) {
            if (resultCode == RESULT_OK) {
                loadPrefs()
                tvVersion.text = "v${UpdateChecker.localVersion(this)}"
                updateLoginStatus()
                startPreviewRefresh()
            } else {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val REQ_LOGIN = 1001
    }

    private fun updateLoginStatus() {
        val logged = Prefs.isLoggedIn(this)
        tvLoginStatus.text = if (logged) "已登录飞牛账号" else "未登录"
        btnLogin.text = if (logged) "重新登录" else "登录飞牛账号"
    }

    private fun toggleTheme() {
        val newTheme = if (Prefs.isDarkTheme(this)) "light" else "dark"
        Prefs.setTheme(this, newTheme)
        AppCompatDelegate.setDefaultNightMode(
            if (newTheme == "dark") AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun updateThemeButton() {
        btnToggleTheme.text = if (Prefs.isDarkTheme(this)) "深色" else "浅色"
    }

    /** 输入框获得焦点时滚动 ScrollView，让输入框露出来（避免键盘遮挡） */
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
        cbDocker.isChecked = Prefs.showDocker(this)
        cbWb2api.isChecked = Prefs.showWb2api(this)
        etWb2apiBase.setText(Prefs.wb2apiBase(this))
        etWb2apiKey.setText(Prefs.wb2apiKey(this))
        if (Prefs.isLoggedIn(this)) {
            loadDockerContainers()
        }
    }

    // === 小组件预览 ===
    private fun startPreviewRefresh() {
        handler.removeCallbacksAndMessages(null)
        handler.post(object : Runnable {
            override fun run() {
                refreshPreview()
                handler.postDelayed(this, 5000)
            }
        })
    }

    private fun refreshPreview() {
        if (previewBusy) return
        previewBusy = true
        val url = Prefs.url(this)
        Thread {
            val result = NasDataFetcher.fetch(url)
            val docker = if (Prefs.showDocker(this)) {
                OAuthClient.getValidAccessToken(this).getOrNull()?.let {
                    NasDataFetcher.fetchDocker(Prefs.nasBase(this), it)
                }
            } else null
            val wb2api = if (Prefs.showWb2api(this)) {
                NasDataFetcher.fetchWb2apiCredits(Prefs.wb2apiBase(this), Prefs.wb2apiKey(this))
            } else null
            runOnUiThread {
                updatePreviewViews(result, docker, wb2api)
                previewBusy = false
            }
        }.start()
    }

    private fun updatePreviewViews(result: Result<NasStats>, docker: Result<List<DockerContainer>>?, wb2api: Result<Wb2apiStats>?) {
        val root = previewContainer.getChildAt(0) as? View ?: return
        val showCpu = Prefs.showCpu(this)
        val showMem = Prefs.showMem(this)
        val showDisk1 = Prefs.showDisk1(this)
        val showDisk2 = Prefs.showDisk2(this)
        val showTemp = Prefs.showTemp(this)
        val showUptime = Prefs.showUptime(this)
        val showDocker = Prefs.showDocker(this)
        val showWb2api = Prefs.showWb2api(this)

        root.findViewById<View>(R.id.row_cpu).visibility = if (showCpu) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.row_mem).visibility = if (showMem) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.row_disk1).visibility = if (showDisk1) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.row_disk2).visibility = if (showDisk2) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.row_temp).visibility = if (showTemp) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.row_uptime).visibility = if (showUptime) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.row_wb2api).visibility = if (showWb2api) View.VISIBLE else View.GONE

        result.onSuccess { s ->
            val t = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(s.fetchTime))
            root.findViewById<TextView>(R.id.tv_time).text = t

            if (showCpu) {
                root.findViewById<ProgressBar>(R.id.pb_cpu).progress = s.cpuPercent.toInt().coerceIn(0, 100)
                root.findViewById<TextView>(R.id.tv_cpu).text = String.format("%.0f%%", s.cpuPercent)
            }
            if (showMem) {
                root.findViewById<ProgressBar>(R.id.pb_mem).progress = s.memPercent.toInt().coerceIn(0, 100)
                root.findViewById<TextView>(R.id.tv_mem).text =
                    String.format("%.0f%% · %s", s.memPercent, Fmt.bytes(s.memUsed))
            }
            if (showDisk1 && s.disks.isNotEmpty()) {
                val d = s.disks[0]
                root.findViewById<TextView>(R.id.tv_disk1_label).text = d.name
                root.findViewById<ProgressBar>(R.id.pb_disk1).progress = d.usePercent.toInt().coerceIn(0, 100)
                root.findViewById<TextView>(R.id.tv_disk1).text = String.format("%.0f%%", d.usePercent)
            }
            if (showDisk2 && s.disks.size > 1) {
                val d = s.disks[1]
                root.findViewById<TextView>(R.id.tv_disk2_label).text = d.name
                root.findViewById<ProgressBar>(R.id.pb_disk2).progress = d.usePercent.toInt().coerceIn(0, 100)
                root.findViewById<TextView>(R.id.tv_disk2).text = String.format("%.0f%%", d.usePercent)
            }
            if (showTemp) {
                root.findViewById<TextView>(R.id.tv_temp_cpu).text =
                    s.tempCpu?.let { String.format("CPU %.0f°", it) } ?: "CPU --°"
                root.findViewById<TextView>(R.id.tv_temp_nvme).text =
                    s.tempNvme?.let { String.format("NVMe %.0f°", it) } ?: "NVMe --°"
            }
            if (showUptime) {
                root.findViewById<TextView>(R.id.tv_uptime).text = Fmt.uptime(s.uptimeSec)
            }
        }.onFailure {
            root.findViewById<TextView>(R.id.tv_time).text = "连接失败"
        }

        // wb2api 积分
        if (showWb2api) {
            val tvWb2api = root.findViewById<TextView>(R.id.tv_wb2api)
            wb2api?.onSuccess { s ->
                tvWb2api.text = "${s.totalCredits} · ${s.healthyCount}/${s.accountCount}健康"
            }?.onFailure {
                tvWb2api.text = "获取失败"
            }
        }

        // Docker 容器预览
        val container = root.findViewById<LinearLayout>(R.id.container_docker)
        container.removeAllViews()
        if (showDocker) {
            val selected = Prefs.dockerContainers(this)
            val list = docker?.getOrNull() ?: emptyList()
            val filtered = if (selected.isEmpty()) list else list.filter { it.name in selected }
            if (docker == null) {
                container.addView(makeDockerItem("未登录飞牛", false, ""))
            } else if (filtered.isEmpty()) {
                container.addView(makeDockerItem("无容器", false, ""))
            } else {
                for (c in filtered) {
                    container.addView(makeDockerItem(c))
                }
            }
        }
    }

    private fun makeDockerItem(name: String, running: Boolean, state: String, isError: Boolean = false): View {
        val item = layoutInflater.inflate(R.layout.item_docker_preview, dockerListContainer, false)
        item.findViewById<TextView>(R.id.tv_dot).text = if (running) "●" else "○"
        val dotColor = when {
            running -> 0xFF4CAF50.toInt()
            isError -> 0xFFF44336.toInt()
            else -> 0xFF888888.toInt()
        }
        item.findViewById<TextView>(R.id.tv_dot).setTextColor(dotColor)
        item.findViewById<TextView>(R.id.tv_name).text = name
        item.findViewById<TextView>(R.id.tv_state).text = state
        return item
    }

    private fun makeDockerItem(c: DockerContainer): View {
        val item = layoutInflater.inflate(R.layout.item_docker_preview, dockerListContainer, false)
        item.findViewById<TextView>(R.id.tv_dot).text = if (c.running) "●" else "○"
        val dotColor = when {
            c.running -> 0xFF4CAF50.toInt()
            c.state.equals("error", true) -> 0xFFF44336.toInt()
            else -> 0xFF888888.toInt()
        }
        item.findViewById<TextView>(R.id.tv_dot).setTextColor(dotColor)
        item.findViewById<TextView>(R.id.tv_name).text = c.name
        item.findViewById<TextView>(R.id.tv_state).text = c.detailText
        return item
    }

    // === Docker 配置 ===
    private fun loadDockerContainers() {
        if (!Prefs.isLoggedIn(this)) {
            Toast.makeText(this, "请先登录飞牛账号", Toast.LENGTH_SHORT).show()
            return
        }
        btnRefreshDocker.isEnabled = false
        Thread {
            val token = OAuthClient.getValidAccessToken(this).getOrNull()
            val result = if (token != null) {
                NasDataFetcher.fetchDocker(Prefs.nasBase(this), token)
            } else {
                Result.failure(Exception("登录已过期，请重新登录"))
            }
            runOnUiThread {
                btnRefreshDocker.isEnabled = true
                result.onSuccess { list ->
                    dockerContainers = list
                    buildDockerCheckboxes()
                }.onFailure { e ->
                    Toast.makeText(this, "获取容器失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun buildDockerCheckboxes() {
        dockerListContainer.removeAllViews()
        dockerCheckboxes.clear()
        val selected = Prefs.dockerContainers(this)
        for (c in dockerContainers.sortedBy { it.name }) {
            val cb = CheckBox(this).apply {
                text = "${c.name}（${c.detailText}）"
                isChecked = selected.contains(c.name)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.app_text_primary))
                textSize = 13f
            }
            dockerListContainer.addView(cb)
            dockerCheckboxes.add(cb)
        }
        refreshDockerVisibility()
    }

    private fun refreshDockerVisibility() {
        val visible = cbDocker.isChecked
        dockerListContainer.visibility = if (visible) View.VISIBLE else View.GONE
        btnRefreshDocker.visibility = if (visible) View.VISIBLE else View.GONE
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

        // Docker 配置
        Prefs.setShowDocker(this, cbDocker.isChecked)
        val selected = dockerCheckboxes.filter { it.isChecked }
            .map { it.text.toString().substringBefore("（") }.toSet()
        Prefs.setDockerContainers(this, selected)

        // wb2api 积分配置
        Prefs.setShowWb2api(this, cbWb2api.isChecked)
        Prefs.setWb2apiBase(this, etWb2apiBase.text.toString().trim().ifEmpty { Prefs.DEFAULT_WB2API_BASE })
        Prefs.setWb2apiKey(this, etWb2apiKey.text.toString().trim().ifEmpty { Prefs.DEFAULT_WB2API_KEY })

        val am = AppWidgetManager.getInstance(this)
        val ids = am.getAppWidgetIds(ComponentName(this, NasWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            NasWidgetProvider.updateAll(this, am, ids)
        }
        AlarmScheduler.schedule(this)

        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, result)
            finish()
        } else {
            Toast.makeText(this, "已保存，小组件已更新", Toast.LENGTH_SHORT).show()
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
                        showUpdateDialog(info)
                    } else {
                        tvUpdateStatus.text = "已是最新版本 v$local"
                    }
                }.onFailure { e ->
                    tvUpdateStatus.text = "检查失败：${e.message}"
                }
            }
        }.start()
    }

    /** 弹框：显示新版本号 + 更新内容，用户选择是否更新 */
    private fun showUpdateDialog(info: UpdateChecker.UpdateInfo) {
        val notes = info.notes.trim().ifEmpty { "（无更新说明）" }
        val message = "新版本 v${info.version}\n\n$notes"
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("发现新版本")
            .setMessage(message)
            .setPositiveButton("立即更新") { _, _ -> downloadAndInstall(info) }
            .setNegativeButton("暂不更新", null)
            .setCancelable(true)
            .show()
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
