package com.yxy.naswidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SwitchCompat
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val refreshOptions = listOf(10 to "10 秒", 15 to "15 秒", 30 to "30 秒", 60 to "60 秒", 300 to "5 分钟")

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var spRefresh: Spinner
    private lateinit var scrollRoot: ScrollView
    private lateinit var cbCpu: SwitchCompat
    private lateinit var cbMem: SwitchCompat
    private lateinit var cbDisk1: SwitchCompat
    private lateinit var cbDisk2: SwitchCompat
    private lateinit var cbNet: SwitchCompat
    private lateinit var previewContainer: FrameLayout
    private lateinit var cbDocker: SwitchCompat
    private lateinit var dockerListContainer: LinearLayout
    private lateinit var btnRefreshDocker: Button
    private lateinit var etFontSize: EditText
    private lateinit var sbCpuX: SeekBar
    private lateinit var sbCpuY: SeekBar
    private lateinit var sbMemX: SeekBar
    private lateinit var sbMemY: SeekBar
    private lateinit var sbDisk1X: SeekBar
    private lateinit var sbDisk1Y: SeekBar
    private lateinit var sbDisk2X: SeekBar
    private lateinit var sbDisk2Y: SeekBar
    private lateinit var sbDockerX: SeekBar
    private lateinit var sbDockerY: SeekBar
    private lateinit var sbNetX: SeekBar
    private lateinit var sbNetY: SeekBar
    private lateinit var etCpuX: EditText
    private lateinit var etCpuY: EditText
    private lateinit var etMemX: EditText
    private lateinit var etMemY: EditText
    private lateinit var etDisk1X: EditText
    private lateinit var etDisk1Y: EditText
    private lateinit var etDisk2X: EditText
    private lateinit var etDisk2Y: EditText
    private lateinit var etDockerX: EditText
    private lateinit var etDockerY: EditText
    private lateinit var etNetX: EditText
    private lateinit var etNetY: EditText

    private val handler = Handler(Looper.getMainLooper())
    private var dockerContainers: List<DockerContainer> = emptyList()
    private val dockerCheckboxes = mutableListOf<SwitchCompat>()
    private var previewBusy = false
    private var suppressSave = false
    private var autoUpdateChecked = false
    private var dockerLoaded = false
    private val fontSaveRunnable = Runnable { persist() }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(
            if (Prefs.isDarkTheme(this)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        bottomNav = findViewById(R.id.bottom_nav)
        spRefresh = findViewById(R.id.sp_refresh)
        scrollRoot = findViewById(R.id.scroll_root)
        cbCpu = findViewById(R.id.cb_cpu)
        cbMem = findViewById(R.id.cb_mem)
        cbDisk1 = findViewById(R.id.cb_disk1)
        cbDisk2 = findViewById(R.id.cb_disk2)
        cbNet = findViewById(R.id.cb_net)
        previewContainer = findViewById(R.id.preview_container)
        cbDocker = findViewById(R.id.cb_docker)
        dockerListContainer = findViewById(R.id.docker_list_container)
        btnRefreshDocker = findViewById(R.id.btn_refresh_docker)
        etFontSize = findViewById(R.id.et_font_size)
        sbCpuX = findViewById(R.id.sb_cpu_x)
        sbCpuY = findViewById(R.id.sb_cpu_y)
        sbMemX = findViewById(R.id.sb_mem_x)
        sbMemY = findViewById(R.id.sb_mem_y)
        sbDisk1X = findViewById(R.id.sb_disk1_x)
        sbDisk1Y = findViewById(R.id.sb_disk1_y)
        sbDisk2X = findViewById(R.id.sb_disk2_x)
        sbDisk2Y = findViewById(R.id.sb_disk2_y)
        sbDockerX = findViewById(R.id.sb_docker_x)
        sbDockerY = findViewById(R.id.sb_docker_y)
        sbNetX = findViewById(R.id.sb_net_x)
        sbNetY = findViewById(R.id.sb_net_y)
        etCpuX = findViewById(R.id.et_cpu_x)
        etCpuY = findViewById(R.id.et_cpu_y)
        etMemX = findViewById(R.id.et_mem_x)
        etMemY = findViewById(R.id.et_mem_y)
        etDisk1X = findViewById(R.id.et_disk1_x)
        etDisk1Y = findViewById(R.id.et_disk1_y)
        etDisk2X = findViewById(R.id.et_disk2_x)
        etDisk2Y = findViewById(R.id.et_disk2_y)
        etDockerX = findViewById(R.id.et_docker_x)
        etDockerY = findViewById(R.id.et_docker_y)
        etNetX = findViewById(R.id.et_net_x)
        etNetY = findViewById(R.id.et_net_y)

        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        }

        val labels = refreshOptions.map { it.second }
        spRefresh.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)

        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    false
                }
                else -> true
            }
        }

        // 小组件预览
        layoutInflater.inflate(R.layout.nas_widget_preview, previewContainer, true)

        // 控件改动即自动保存
        setupAutoSaveListeners()

        // Docker 配置
        btnRefreshDocker.setOnClickListener { loadDockerContainers() }

        // 强制登录门
        if (!Prefs.isLoggedIn(this)) {
            startActivityForResult(Intent(this, LoginActivity::class.java), REQ_LOGIN)
            return
        }

        resetExtremeOffsets()
        loadPrefs()
        refreshDockerVisibility()
        startPreviewRefresh()
        maybeAutoCheckUpdate()

        // 返回键即完成（改动已自动保存）
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishConfig()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_LOGIN) {
            if (resultCode == RESULT_OK) {
                loadPrefs()
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

    // 启动时自动检查更新，有新版本静默弹出更新弹框
    private fun maybeAutoCheckUpdate() {
        if (autoUpdateChecked) return
        autoUpdateChecked = true
        Thread {
            val result = UpdateChecker.checkLatest()
            runOnUiThread {
                result.onSuccess { info ->
                    val local = UpdateChecker.localVersion(this)
                    if (UpdateChecker.isNewer(info.version, local)) {
                        UpdateChecker.showUpdateDialog(this, info)
                    }
                }
            }
        }.start()
    }

    // 清掉之前拉满的极端偏移值（±80 会把组件顶出卡片），归零归中
    private fun resetExtremeOffsets() {
        for (comp in listOf("cpu", "mem", "disk1", "disk2", "docker", "net")) {
            val x = Prefs.compX(this, comp)
            val y = Prefs.compY(this, comp)
            if (kotlin.math.abs(x) > 20) Prefs.setCompX(this, comp, 0)
            if (kotlin.math.abs(y) > 20) Prefs.setCompY(this, comp, 0)
        }
    }

    companion object {
        private const val REQ_LOGIN = 1001
    }

    private fun loadPrefs() {
        suppressSave = true
        val cur = Prefs.refreshSec(this)
        val idx = refreshOptions.indexOfFirst { it.first == cur }
        spRefresh.setSelection(if (idx >= 0) idx else 2)
        cbCpu.isChecked = Prefs.showCpu(this)
        cbMem.isChecked = Prefs.showMem(this)
        cbDisk1.isChecked = Prefs.showDisk1(this)
        cbDisk2.isChecked = Prefs.showDisk2(this)
        cbNet.isChecked = Prefs.showNet(this)
        cbDocker.isChecked = Prefs.showDocker(this)
        etFontSize.setText(Prefs.fontSize(this).toString())
        sbCpuX.progress = Prefs.compX(this, "cpu") + 50
        sbCpuY.progress = Prefs.compY(this, "cpu") + 50
        sbMemX.progress = Prefs.compX(this, "mem") + 50
        sbMemY.progress = Prefs.compY(this, "mem") + 50
        sbDisk1X.progress = Prefs.compX(this, "disk1") + 50
        sbDisk1Y.progress = Prefs.compY(this, "disk1") + 50
        sbDisk2X.progress = Prefs.compX(this, "disk2") + 50
        sbDisk2Y.progress = Prefs.compY(this, "disk2") + 50
        sbDockerX.progress = Prefs.compX(this, "docker") + 50
        sbDockerY.progress = Prefs.compY(this, "docker") + 50
        sbNetX.progress = Prefs.compX(this, "net") + 50
        sbNetY.progress = Prefs.compY(this, "net") + 50
        etCpuX.setText(Prefs.compX(this, "cpu").toString())
        etCpuY.setText(Prefs.compY(this, "cpu").toString())
        etMemX.setText(Prefs.compX(this, "mem").toString())
        etMemY.setText(Prefs.compY(this, "mem").toString())
        etDisk1X.setText(Prefs.compX(this, "disk1").toString())
        etDisk1Y.setText(Prefs.compY(this, "disk1").toString())
        etDisk2X.setText(Prefs.compX(this, "disk2").toString())
        etDisk2Y.setText(Prefs.compY(this, "disk2").toString())
        etDockerX.setText(Prefs.compX(this, "docker").toString())
        etDockerY.setText(Prefs.compY(this, "docker").toString())
        etNetX.setText(Prefs.compX(this, "net").toString())
        etNetY.setText(Prefs.compY(this, "net").toString())
        suppressSave = false
        if (Prefs.isLoggedIn(this)) {
            loadDockerContainers()
        }
    }

    // === 小组件预览 ===
    private fun startPreviewRefresh() {
        handler.removeCallbacks(previewRunnable)
        handler.post(previewRunnable)
    }

    private val previewRunnable = object : Runnable {
        override fun run() {
            refreshPreview()
            handler.postDelayed(this, 5000)
        }
    }
    private val previewThrottle = Runnable { refreshPreview() }
    private fun schedulePreviewRefresh() {
        handler.removeCallbacks(previewThrottle)
        handler.postDelayed(previewThrottle, 100)
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
        val fs = Prefs.fontSize(this)

        val contentArea = root.findViewById<FrameLayout>(R.id.content_area)
        contentArea.removeAllViews()

        root.findViewById<View>(R.id.row_uptime).visibility = if (showUptime) View.VISIBLE else View.GONE
        root.findViewById<TextView>(R.id.tv_title).setTextSize(TypedValue.COMPLEX_UNIT_PX, (fs + 3).toFloat())
        root.findViewById<TextView>(R.id.tv_uptime).setTextSize(TypedValue.COMPLEX_UNIT_PX, (fs + 2).toFloat())

        var stats: NasStats? = null
        result.onSuccess { s ->
            stats = s
            root.findViewById<TextView>(R.id.tv_uptime).text = "运行 " + Fmt.uptime(s.uptimeSec)
        }.onFailure {
            root.findViewById<TextView>(R.id.tv_uptime).text = "连接失败"
        }

        fun addTo(comp: String, v: View) {
            contentArea.addView(v)
            val density = resources.displayMetrics.density
            v.translationX = Prefs.compX(this@MainActivity, comp) * density * 1.6f
            v.translationY = Prefs.compY(this@MainActivity, comp) * density * 1.6f
        }

        if (showCpu) {
            val item = layoutInflater.inflate(R.layout.item_cpu_preview, contentArea, false)
            stats?.let { s ->
                item.findViewById<ProgressBar>(R.id.pb_cpu).progress = s.cpuPercent.toInt().coerceIn(0, 100)
                item.findViewById<TextView>(R.id.tv_cpu).text = tempPct(s.tempCpu, s.cpuPercent, showTemp)
            } ?: run { item.findViewById<TextView>(R.id.tv_cpu).text = "--%" }
            item.findViewById<TextView>(R.id.tv_cpu_label).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            item.findViewById<TextView>(R.id.tv_cpu).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            addTo("cpu", item)
        }

        if (showMem) {
            val item = layoutInflater.inflate(R.layout.item_mem_preview, contentArea, false)
            stats?.let { s ->
                item.findViewById<ProgressBar>(R.id.pb_mem).progress = s.memPercent.toInt().coerceIn(0, 100)
                val memFree = (s.memTotal - s.memUsed).coerceAtLeast(0)
                item.findViewById<TextView>(R.id.tv_mem).text = String.format("%s %.0f%%", Fmt.bytes(memFree), s.memPercent)
            } ?: run { item.findViewById<TextView>(R.id.tv_mem).text = "--%" }
            item.findViewById<TextView>(R.id.tv_mem_label).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            item.findViewById<TextView>(R.id.tv_mem).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            addTo("mem", item)
        }

        if (showDisk1) {
            val item = layoutInflater.inflate(R.layout.item_disk1_preview, contentArea, false)
            stats?.let { s ->
                if (s.disks.isNotEmpty()) {
                    val d = s.disks[0]
                    item.findViewById<TextView>(R.id.tv_disk1_label).text = d.name
                    item.findViewById<ProgressBar>(R.id.pb_disk1).progress = d.usePercent.toInt().coerceIn(0, 100)
                    item.findViewById<TextView>(R.id.tv_disk1).text = tempPct(s.tempNvme, d.usePercent, showTemp)
                } else {
                    item.findViewById<TextView>(R.id.tv_disk1).text = "--%"
                }
            } ?: run { item.findViewById<TextView>(R.id.tv_disk1).text = "--%" }
            item.findViewById<TextView>(R.id.tv_disk1_label).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            item.findViewById<TextView>(R.id.tv_disk1).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            addTo("disk1", item)
        }

        if (showDisk2) {
            val item = layoutInflater.inflate(R.layout.item_disk2_preview, contentArea, false)
            stats?.let { s ->
                if (s.disks.size > 1) {
                    val d = s.disks[1]
                    item.findViewById<TextView>(R.id.tv_disk2_label).text = d.name
                    item.findViewById<ProgressBar>(R.id.pb_disk2).progress = d.usePercent.toInt().coerceIn(0, 100)
                    item.findViewById<TextView>(R.id.tv_disk2).text = String.format("%.0f%%", d.usePercent)
                } else {
                    item.findViewById<TextView>(R.id.tv_disk2).text = "--%"
                }
            } ?: run { item.findViewById<TextView>(R.id.tv_disk2).text = "--%" }
            item.findViewById<TextView>(R.id.tv_disk2_label).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            item.findViewById<TextView>(R.id.tv_disk2).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            addTo("disk2", item)
        }

        if (Prefs.showNet(this)) {
            val item = layoutInflater.inflate(R.layout.item_net_preview, contentArea, false)
            stats?.let { s ->
                item.findViewById<TextView>(R.id.tv_net).text = "↓${Fmt.speed(s.netDown)} ↑${Fmt.speed(s.netUp)}"
            } ?: run { item.findViewById<TextView>(R.id.tv_net).text = "↓-- ↑--" }
            item.findViewById<TextView>(R.id.tv_net_label).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            item.findViewById<TextView>(R.id.tv_net).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            addTo("net", item)
        }

        if (showDocker) {
            val selected = Prefs.dockerContainers(this)
            val group = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val density = resources.displayMetrics.density
            group.translationX = Prefs.compX(this, "docker") * density * 1.6f
            group.translationY = Prefs.compY(this, "docker") * density * 1.6f
            contentArea.addView(group)
            if (docker == null) {
                group.addView(makeDockerItem("未登录飞牛", false, ""))
            } else {
                docker.onSuccess { list ->
                    if (selected.isNotEmpty()) {
                        val filtered = list.filter { it.name in selected }.sortedBy { it.name }
                        if (filtered.isEmpty()) {
                            group.addView(makeDockerItem("无容器", false, ""))
                        } else {
                            for (c in filtered) {
                                group.addView(makeDockerItem(c))
                            }
                        }
                    }
                }.onFailure {
                    group.addView(makeDockerItem("Docker 获取失败", false, ""))
                }
            }
        }

        root.findViewById<View>(R.id.row_wb2api).visibility = if (showWb2api) View.VISIBLE else View.GONE
        if (showWb2api) {
            val tvWb2api = root.findViewById<TextView>(R.id.tv_wb2api)
            wb2api?.onSuccess { s ->
                tvWb2api.text = "${s.totalCredits} · ${s.healthyCount}/${s.accountCount}"
            }?.onFailure {
                tvWb2api.text = "获取失败"
            }
        }
        root.findViewById<TextView>(R.id.tv_wb2api).setTextSize(TypedValue.COMPLEX_UNIT_PX, (fs + 3).toFloat())
    }

    private fun tempPct(temp: Double?, pct: Double, showTemp: Boolean): String {
        val pctText = String.format("%.0f%%", pct)
        if (!showTemp) return pctText
        val t = temp?.let { String.format("%.0f°", it) } ?: "--°"
        return "$t $pctText"
    }

    private fun makeDockerItem(name: String, running: Boolean, state: String, isError: Boolean = false): View {
        val item = layoutInflater.inflate(R.layout.item_docker_preview, null, false)
        val fs = Prefs.fontSize(this)
        item.findViewById<TextView>(R.id.tv_dot).text = if (running) "●" else "○"
        val dotColor = when {
            running -> 0xFF4CAF50.toInt()
            isError -> 0xFFF44336.toInt()
            else -> 0xFF888888.toInt()
        }
        item.findViewById<TextView>(R.id.tv_dot).setTextColor(dotColor)
        item.findViewById<TextView>(R.id.tv_name).text = name
        item.findViewById<TextView>(R.id.tv_state).text = state
        item.findViewById<TextView>(R.id.tv_name).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
        item.findViewById<TextView>(R.id.tv_state).setTextSize(TypedValue.COMPLEX_UNIT_PX, (fs - 1).toFloat())
        return item
    }

    private fun makeDockerItem(c: DockerContainer): View {
        val item = layoutInflater.inflate(R.layout.item_docker_preview, null, false)
        val fs = Prefs.fontSize(this)
        item.findViewById<TextView>(R.id.tv_dot).text = if (c.running) "●" else "○"
        val dotColor = when {
            c.running -> 0xFF4CAF50.toInt()
            c.state.equals("error", true) -> 0xFFF44336.toInt()
            else -> 0xFF888888.toInt()
        }
        item.findViewById<TextView>(R.id.tv_dot).setTextColor(dotColor)
        item.findViewById<TextView>(R.id.tv_name).text = c.name
        item.findViewById<TextView>(R.id.tv_state).text = c.detailText
        item.findViewById<TextView>(R.id.tv_name).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
        item.findViewById<TextView>(R.id.tv_state).setTextSize(TypedValue.COMPLEX_UNIT_PX, (fs - 1).toFloat())
        return item
    }

    // === Docker 配置 ===
    private fun loadDockerContainers() {
        if (dockerLoaded) return
        dockerLoaded = true
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
                Result.failure(Exception("未登录"))
            }
            runOnUiThread {
                btnRefreshDocker.isEnabled = true
                result.onSuccess { list ->
                    dockerContainers = list
                    buildDockerCheckboxes(list)
                }.onFailure { e ->
                    Toast.makeText(this, "获取 Docker 失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun buildDockerCheckboxes(containers: List<DockerContainer>) {
        dockerListContainer.removeAllViews()
        dockerCheckboxes.clear()
        val selected = Prefs.dockerContainers(this)
        val sorted = containers.sortedBy { it.name }
        for (c in sorted) {
            val item = layoutInflater.inflate(R.layout.item_docker_config, dockerListContainer, false)
            item.findViewById<TextView>(R.id.tv_dot).text = if (c.running) "●" else "○"
            item.findViewById<TextView>(R.id.tv_dot).setTextColor(if (c.running) 0xFF4CAF50.toInt() else 0xFF888888.toInt())
            item.findViewById<TextView>(R.id.tv_name).text = c.name
            item.findViewById<TextView>(R.id.tv_detail).text = c.detailText
            val cb = item.findViewById<SwitchCompat>(R.id.cb_select)
            cb.isChecked = c.name in selected
            cb.tag = c.name
            cb.setOnCheckedChangeListener { _, _ -> if (!suppressSave) persist() }
            dockerCheckboxes.add(cb)
            dockerListContainer.addView(item)
        }
    }

    private fun refreshDockerVisibility() {
        val show = Prefs.showDocker(this)
        dockerListContainer.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun persist() {
        val sel = spRefresh.selectedItemPosition
        Prefs.setRefresh(this, refreshOptions.getOrElse(sel) { 30 to "30 秒" }.first)
        Prefs.setShowCpu(this, cbCpu.isChecked)
        Prefs.setShowMem(this, cbMem.isChecked)
        Prefs.setShowDisk1(this, cbDisk1.isChecked)
        Prefs.setShowDisk2(this, cbDisk2.isChecked)
        Prefs.setShowDocker(this, cbDocker.isChecked)
        Prefs.setShowNet(this, cbNet.isChecked)
        // 字号
        val fs = etFontSize.text.toString().toIntOrNull()?.coerceIn(8, 200) ?: 16
        Prefs.setFontSize(this, fs)
        // Docker 选中容器
        if (dockerCheckboxes.isNotEmpty()) {
            val selected = dockerCheckboxes.filter { it.isChecked }.mapNotNull { it.tag as? String }.toSet()
            Prefs.setDockerContainers(this, selected)
        }
        // 位置偏移
        Prefs.setCompX(this, "cpu", sbCpuX.progress - 50)
        Prefs.setCompY(this, "cpu", sbCpuY.progress - 50)
        Prefs.setCompX(this, "mem", sbMemX.progress - 50)
        Prefs.setCompY(this, "mem", sbMemY.progress - 50)
        Prefs.setCompX(this, "disk1", sbDisk1X.progress - 50)
        Prefs.setCompY(this, "disk1", sbDisk1Y.progress - 50)
        Prefs.setCompX(this, "disk2", sbDisk2X.progress - 50)
        Prefs.setCompY(this, "disk2", sbDisk2Y.progress - 50)
        Prefs.setCompX(this, "docker", sbDockerX.progress - 50)
        Prefs.setCompY(this, "docker", sbDockerY.progress - 50)
        Prefs.setCompX(this, "net", sbNetX.progress - 50)
        Prefs.setCompY(this, "net", sbNetY.progress - 50)

        val am = AppWidgetManager.getInstance(this)
        val ids = am.getAppWidgetIds(ComponentName(this, NasWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            NasWidgetProvider.updateAll(this, am, ids)
        }
        AlarmScheduler.schedule(this)
    }

    private fun finishConfig() {
        persist()
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, result)
            finish()
        } else {
            finish()
        }
    }

    /** 滑轨与输入框双向联动 */
    private fun bindSlider(et: EditText, sb: SeekBar, comp: String, isX: Boolean) {
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressSave) return
                val str = s?.toString() ?: return
                if (str == "-" || str.isEmpty()) return
                val v = str.toIntOrNull()?.coerceIn(-50, 50) ?: return
                if (sb.progress != v + 50) sb.progress = v + 50
                persist()
            }
        })
    }

    private fun setupAutoSaveListeners() {
        bindSlider(etCpuX, sbCpuX, "cpu", true)
        bindSlider(etCpuY, sbCpuY, "cpu", false)
        bindSlider(etMemX, sbMemX, "mem", true)
        bindSlider(etMemY, sbMemY, "mem", false)
        bindSlider(etDisk1X, sbDisk1X, "disk1", true)
        bindSlider(etDisk1Y, sbDisk1Y, "disk1", false)
        bindSlider(etDisk2X, sbDisk2X, "disk2", true)
        bindSlider(etDisk2Y, sbDisk2Y, "disk2", false)
        bindSlider(etDockerX, sbDockerX, "docker", true)
        bindSlider(etDockerY, sbDockerY, "docker", false)
        bindSlider(etNetX, sbNetX, "net", true)
        bindSlider(etNetY, sbNetY, "net", false)

        for (cb in listOf(cbCpu, cbMem, cbDisk1, cbDisk2, cbDocker, cbNet)) {
            cb.setOnCheckedChangeListener { _, _ -> if (!suppressSave) persist() }
        }

        spRefresh.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!suppressSave) persist()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        etFontSize.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!suppressSave) {
                    handler.removeCallbacks(fontSaveRunnable)
                    handler.postDelayed(fontSaveRunnable, 400)
                }
            }
        })

        // 滑块实时同步：拖动即时更新输入框 + 保存 + 刷新预览
        val etMap = mapOf(
            sbCpuX to etCpuX, sbCpuY to etCpuY,
            sbMemX to etMemX, sbMemY to etMemY,
            sbDisk1X to etDisk1X, sbDisk1Y to etDisk1Y,
            sbDisk2X to etDisk2X, sbDisk2Y to etDisk2Y,
            sbDockerX to etDockerX, sbDockerY to etDockerY,
            sbNetX to etNetX, sbNetY to etNetY
        )
        for (sb in etMap.keys) {
            sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (suppressSave) return
                    if (fromUser) {
                        etMap[sb]?.setText((progress - 50).toString())
                        persist()
                        schedulePreviewRefresh()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }
}
