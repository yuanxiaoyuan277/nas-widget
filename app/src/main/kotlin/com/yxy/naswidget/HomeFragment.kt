package com.yxy.naswidget

import android.app.Activity
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
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SwitchCompat
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        scrollRoot = view.findViewById(R.id.scroll_root)
        cbCpu = view.findViewById(R.id.cb_cpu)
        cbMem = view.findViewById(R.id.cb_mem)
        cbDisk1 = view.findViewById(R.id.cb_disk1)
        cbDisk2 = view.findViewById(R.id.cb_disk2)
        cbNet = view.findViewById(R.id.cb_net)
        previewContainer = view.findViewById(R.id.preview_container)
        cbDocker = view.findViewById(R.id.cb_docker)
        dockerListContainer = view.findViewById(R.id.docker_list_container)
        btnRefreshDocker = view.findViewById(R.id.btn_refresh_docker)
        sbCpuX = view.findViewById(R.id.sb_cpu_x)
        sbCpuY = view.findViewById(R.id.sb_cpu_y)
        sbMemX = view.findViewById(R.id.sb_mem_x)
        sbMemY = view.findViewById(R.id.sb_mem_y)
        sbDisk1X = view.findViewById(R.id.sb_disk1_x)
        sbDisk1Y = view.findViewById(R.id.sb_disk1_y)
        sbDisk2X = view.findViewById(R.id.sb_disk2_x)
        sbDisk2Y = view.findViewById(R.id.sb_disk2_y)
        sbDockerX = view.findViewById(R.id.sb_docker_x)
        sbDockerY = view.findViewById(R.id.sb_docker_y)
        sbNetX = view.findViewById(R.id.sb_net_x)
        sbNetY = view.findViewById(R.id.sb_net_y)
        etCpuX = view.findViewById(R.id.et_cpu_x)
        etCpuY = view.findViewById(R.id.et_cpu_y)
        etMemX = view.findViewById(R.id.et_mem_x)
        etMemY = view.findViewById(R.id.et_mem_y)
        etDisk1X = view.findViewById(R.id.et_disk1_x)
        etDisk1Y = view.findViewById(R.id.et_disk1_y)
        etDisk2X = view.findViewById(R.id.et_disk2_x)
        etDisk2Y = view.findViewById(R.id.et_disk2_y)
        etDockerX = view.findViewById(R.id.et_docker_x)
        etDockerY = view.findViewById(R.id.et_docker_y)
        etNetX = view.findViewById(R.id.et_net_x)
        etNetY = view.findViewById(R.id.et_net_y)

        widgetId = requireActivity().intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            requireActivity().setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        }

        // 小组件预览
        layoutInflater.inflate(R.layout.nas_widget_preview, previewContainer, true)

        // 控件改动即自动保存
        setupAutoSaveListeners()

        // Docker 配置
        btnRefreshDocker.setOnClickListener { loadDockerContainers() }

        // 强制登录门
        if (!Prefs.isLoggedIn(requireContext())) {
            requireActivity().startActivityForResult(Intent(requireContext(), LoginActivity::class.java), REQ_LOGIN)
            return
        }

        resetExtremeOffsets()
        loadPrefs()
        refreshDockerVisibility()
        startPreviewRefresh()
        maybeAutoCheckUpdate()

        // 返回键即完成（改动已自动保存）
        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishConfig()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_LOGIN) {
            if (resultCode == Activity.RESULT_OK) {
                loadPrefs()
                startPreviewRefresh()
            } else {
                requireActivity().finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }

    // show/hide 复用实例时：隐藏停止后台刷新，显示恢复
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            handler.removeCallbacks(previewRunnable)
            handler.removeCallbacks(previewThrottle)
        } else {
            startPreviewRefresh()
        }
    }

    // 启动时自动检查更新，有新版本静默弹出更新弹框
    private fun maybeAutoCheckUpdate() {
        if (autoUpdateChecked) return
        autoUpdateChecked = true
        Thread {
            val result = UpdateChecker.checkLatest()
            requireActivity().runOnUiThread {
                result.onSuccess { info ->
                    val local = UpdateChecker.localVersion(requireContext())
                    if (UpdateChecker.isNewer(info.version, local)) {
                        UpdateChecker.showUpdateDialog(requireActivity() as AppCompatActivity, info)
                    }
                }
            }
        }.start()
    }

    // 清掉之前拉满的极端偏移值（±80 会把组件顶出卡片），归零归中
    private fun resetExtremeOffsets() {
        for (comp in listOf("cpu", "mem", "disk1", "disk2", "docker", "net")) {
            val x = Prefs.compX(requireContext(), comp)
            val y = Prefs.compY(requireContext(), comp)
            if (kotlin.math.abs(x) > 20) Prefs.setCompX(requireContext(), comp, 0)
            if (kotlin.math.abs(y) > 20) Prefs.setCompY(requireContext(), comp, 0)
        }
    }

    companion object {
        private const val REQ_LOGIN = 1001
    }

    private fun loadPrefs() {
        suppressSave = true
        cbCpu.isChecked = Prefs.showCpu(requireContext())
        cbMem.isChecked = Prefs.showMem(requireContext())
        cbDisk1.isChecked = Prefs.showDisk1(requireContext())
        cbDisk2.isChecked = Prefs.showDisk2(requireContext())
        cbNet.isChecked = Prefs.showNet(requireContext())
        cbDocker.isChecked = Prefs.showDocker(requireContext())
        sbCpuX.progress = Prefs.compX(requireContext(), "cpu") + 50
        sbCpuY.progress = Prefs.compY(requireContext(), "cpu") + 50
        sbMemX.progress = Prefs.compX(requireContext(), "mem") + 50
        sbMemY.progress = Prefs.compY(requireContext(), "mem") + 50
        sbDisk1X.progress = Prefs.compX(requireContext(), "disk1") + 50
        sbDisk1Y.progress = Prefs.compY(requireContext(), "disk1") + 50
        sbDisk2X.progress = Prefs.compX(requireContext(), "disk2") + 50
        sbDisk2Y.progress = Prefs.compY(requireContext(), "disk2") + 50
        sbDockerX.progress = Prefs.compX(requireContext(), "docker") + 50
        sbDockerY.progress = Prefs.compY(requireContext(), "docker") + 50
        sbNetX.progress = Prefs.compX(requireContext(), "net") + 50
        sbNetY.progress = Prefs.compY(requireContext(), "net") + 50
        etCpuX.setText(Prefs.compX(requireContext(), "cpu").toString())
        etCpuY.setText(Prefs.compY(requireContext(), "cpu").toString())
        etMemX.setText(Prefs.compX(requireContext(), "mem").toString())
        etMemY.setText(Prefs.compY(requireContext(), "mem").toString())
        etDisk1X.setText(Prefs.compX(requireContext(), "disk1").toString())
        etDisk1Y.setText(Prefs.compY(requireContext(), "disk1").toString())
        etDisk2X.setText(Prefs.compX(requireContext(), "disk2").toString())
        etDisk2Y.setText(Prefs.compY(requireContext(), "disk2").toString())
        etDockerX.setText(Prefs.compX(requireContext(), "docker").toString())
        etDockerY.setText(Prefs.compY(requireContext(), "docker").toString())
        etNetX.setText(Prefs.compX(requireContext(), "net").toString())
        etNetY.setText(Prefs.compY(requireContext(), "net").toString())
        suppressSave = false
        if (Prefs.isLoggedIn(requireContext())) {
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
        val url = Prefs.url(requireContext())
        Thread {
            val result = NasDataFetcher.fetch(url)
            val docker = if (Prefs.showDocker(requireContext())) {
                OAuthClient.getValidAccessToken(requireContext()).getOrNull()?.let {
                    NasDataFetcher.fetchDocker(Prefs.nasBase(requireContext()), it)
                }
            } else null
            val wb2api = if (Prefs.showWb2api(requireContext())) {
                NasDataFetcher.fetchWb2apiCredits(Prefs.wb2apiBase(requireContext()), Prefs.wb2apiKey(requireContext()))
            } else null
            requireActivity().runOnUiThread {
                updatePreviewViews(result, docker, wb2api)
                previewBusy = false
            }
        }.start()
    }

    private fun updatePreviewViews(result: Result<NasStats>, docker: Result<List<DockerContainer>>?, wb2api: Result<Wb2apiStats>?) {
        val root = previewContainer.getChildAt(0) as? View ?: return
        val showCpu = Prefs.showCpu(requireContext())
        val showMem = Prefs.showMem(requireContext())
        val showDisk1 = Prefs.showDisk1(requireContext())
        val showDisk2 = Prefs.showDisk2(requireContext())
        val showTemp = Prefs.showTemp(requireContext())
        val showUptime = Prefs.showUptime(requireContext())
        val showDocker = Prefs.showDocker(requireContext())
        val showWb2api = Prefs.showWb2api(requireContext())
        val fs = Prefs.fontSize(requireContext())

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
            v.translationX = Prefs.compX(requireContext(), comp) * density * 1.6f
            v.translationY = Prefs.compY(requireContext(), comp) * density * 1.6f
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

        if (Prefs.showNet(requireContext())) {
            val item = layoutInflater.inflate(R.layout.item_net_preview, contentArea, false)
            stats?.let { s ->
                item.findViewById<TextView>(R.id.tv_net).text = "↓${Fmt.speed(s.netDown)} ↑${Fmt.speed(s.netUp)}"
            } ?: run { item.findViewById<TextView>(R.id.tv_net).text = "↓-- ↑--" }
            item.findViewById<TextView>(R.id.tv_net_label).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            item.findViewById<TextView>(R.id.tv_net).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            addTo("net", item)
        }

        if (showDocker) {
            val selected = Prefs.dockerContainers(requireContext())
            val group = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            val density = resources.displayMetrics.density
            group.translationX = Prefs.compX(requireContext(), "docker") * density * 1.6f
            group.translationY = Prefs.compY(requireContext(), "docker") * density * 1.6f
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
        val fs = Prefs.fontSize(requireContext())
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
        val fs = Prefs.fontSize(requireContext())
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
        if (!Prefs.isLoggedIn(requireContext())) {
            Toast.makeText(requireContext(), "请先登录飞牛账号", Toast.LENGTH_SHORT).show()
            return
        }
        btnRefreshDocker.isEnabled = false
        Thread {
            val token = OAuthClient.getValidAccessToken(requireContext()).getOrNull()
            val result = if (token != null) {
                NasDataFetcher.fetchDocker(Prefs.nasBase(requireContext()), token)
            } else {
                Result.failure(Exception("未登录"))
            }
            requireActivity().runOnUiThread {
                btnRefreshDocker.isEnabled = true
                result.onSuccess { list ->
                    dockerContainers = list
                    buildDockerCheckboxes(list)
                }.onFailure { e ->
                    Toast.makeText(requireContext(), "获取 Docker 失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun buildDockerCheckboxes(containers: List<DockerContainer>) {
        dockerListContainer.removeAllViews()
        dockerCheckboxes.clear()
        val selected = Prefs.dockerContainers(requireContext())
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
        // Docker 配置区（勾选列表）始终显示，让用户能配置勾选哪些容器
        // showDocker 只控制桌面组件/预览是否显示 Docker 项目，不应隐藏配置列表
        dockerListContainer.visibility = View.VISIBLE
    }

    private fun persist() {
        Prefs.setShowCpu(requireContext(), cbCpu.isChecked)
        Prefs.setShowMem(requireContext(), cbMem.isChecked)
        Prefs.setShowDisk1(requireContext(), cbDisk1.isChecked)
        Prefs.setShowDisk2(requireContext(), cbDisk2.isChecked)
        Prefs.setShowDocker(requireContext(), cbDocker.isChecked)
        Prefs.setShowNet(requireContext(), cbNet.isChecked)
        // Docker 选中容器
        if (dockerCheckboxes.isNotEmpty()) {
            val selected = dockerCheckboxes.filter { it.isChecked }.mapNotNull { it.tag as? String }.toSet()
            Prefs.setDockerContainers(requireContext(), selected)
        }
        // 位置偏移
        Prefs.setCompX(requireContext(), "cpu", sbCpuX.progress - 50)
        Prefs.setCompY(requireContext(), "cpu", sbCpuY.progress - 50)
        Prefs.setCompX(requireContext(), "mem", sbMemX.progress - 50)
        Prefs.setCompY(requireContext(), "mem", sbMemY.progress - 50)
        Prefs.setCompX(requireContext(), "disk1", sbDisk1X.progress - 50)
        Prefs.setCompY(requireContext(), "disk1", sbDisk1Y.progress - 50)
        Prefs.setCompX(requireContext(), "disk2", sbDisk2X.progress - 50)
        Prefs.setCompY(requireContext(), "disk2", sbDisk2Y.progress - 50)
        Prefs.setCompX(requireContext(), "docker", sbDockerX.progress - 50)
        Prefs.setCompY(requireContext(), "docker", sbDockerY.progress - 50)
        Prefs.setCompX(requireContext(), "net", sbNetX.progress - 50)
        Prefs.setCompY(requireContext(), "net", sbNetY.progress - 50)

        val am = AppWidgetManager.getInstance(requireContext())
        val ids = am.getAppWidgetIds(ComponentName(requireContext(), NasWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            NasWidgetProvider.updateAll(requireContext(), am, ids)
        }
        AlarmScheduler.schedule(requireContext())
    }

    private fun finishConfig() {
        persist()
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            requireActivity().setResult(Activity.RESULT_OK, result)
            requireActivity().finish()
        } else {
            requireActivity().finish()
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
