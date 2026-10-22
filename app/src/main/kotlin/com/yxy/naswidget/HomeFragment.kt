package com.yxy.naswidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private lateinit var scrollRoot: ScrollView
    private lateinit var cbCpu: SwitchCompat
    private lateinit var cbMem: SwitchCompat
    private lateinit var cbDisk1: SwitchCompat
    private lateinit var cbDisk2: SwitchCompat
    private lateinit var cbDisk3: SwitchCompat
    private lateinit var cbNet: SwitchCompat
    private lateinit var cbStorage: SwitchCompat
    private lateinit var cbTempItem: SwitchCompat
    private lateinit var previewContainer: FrameLayout
    private lateinit var btnRefreshPreview: Button
    private lateinit var btnCopySummary: Button
    private lateinit var cbDocker: SwitchCompat
    private lateinit var dockerListContainer: LinearLayout
    private lateinit var btnRefreshDocker: Button
    private lateinit var btnSelectAll: Button
    private lateinit var btnSelectNone: Button
    private lateinit var btnDockerSort: Button
    private lateinit var etDockerSearch: EditText
    private lateinit var cbRunningOnly: CheckBox
    private lateinit var sbCpuX: SeekBar
    private lateinit var sbCpuY: SeekBar
    private lateinit var sbMemX: SeekBar
    private lateinit var sbMemY: SeekBar
    private lateinit var sbDisk1X: SeekBar
    private lateinit var sbDisk1Y: SeekBar
    private lateinit var sbDisk2X: SeekBar
    private lateinit var sbDisk2Y: SeekBar
    private lateinit var sbDisk3X: SeekBar
    private lateinit var sbDisk3Y: SeekBar
    private lateinit var sbDockerX: SeekBar
    private lateinit var sbDockerY: SeekBar
    private lateinit var sbNetX: SeekBar
    private lateinit var sbNetY: SeekBar
    private lateinit var sbStorageX: SeekBar
    private lateinit var sbStorageY: SeekBar
    private lateinit var sbTempX: SeekBar
    private lateinit var sbTempY: SeekBar
    private lateinit var etCpuX: EditText
    private lateinit var etCpuY: EditText
    private lateinit var etMemX: EditText
    private lateinit var etMemY: EditText
    private lateinit var etDisk1X: EditText
    private lateinit var etDisk1Y: EditText
    private lateinit var etDisk2X: EditText
    private lateinit var etDisk2Y: EditText
    private lateinit var etDisk3X: EditText
    private lateinit var etDisk3Y: EditText
    private lateinit var etDockerX: EditText
    private lateinit var etDockerY: EditText
    private lateinit var etNetX: EditText
    private lateinit var etNetY: EditText
    private lateinit var etStorageX: EditText
    private lateinit var etStorageY: EditText
    private lateinit var etTempX: EditText
    private lateinit var etTempY: EditText
    private lateinit var tvSysCpu: TextView
    private lateinit var tvSysMem: TextView
    private lateinit var tvSysOs: TextView
    private lateinit var tvSysUptime: TextView
    private lateinit var tvSysTime: TextView
    private lateinit var tvSysNet: TextView
    private lateinit var tvSysTemp: TextView
    private lateinit var tvSysStorage: TextView
    private lateinit var tvSysPeak: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var dockerContainers: List<DockerContainer> = emptyList()
    private val dockerCheckboxes = mutableListOf<SwitchCompat>()
    private var previewBusy = false
    private var lastStats: NasStats? = null
    private var suppressSave = false
    private var autoUpdateChecked = false

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
        cbDisk3 = view.findViewById(R.id.cb_disk3)
        cbNet = view.findViewById(R.id.cb_net)
        cbStorage = view.findViewById(R.id.cb_storage)
        cbTempItem = view.findViewById(R.id.cb_temp)
        previewContainer = view.findViewById(R.id.preview_container)
        btnRefreshPreview = view.findViewById(R.id.btn_refresh_preview)
        btnCopySummary = view.findViewById(R.id.btn_copy_summary)
        cbDocker = view.findViewById(R.id.cb_docker)
        dockerListContainer = view.findViewById(R.id.docker_list_container)
        btnRefreshDocker = view.findViewById(R.id.btn_refresh_docker)
        btnSelectAll = view.findViewById(R.id.btn_select_all)
        btnSelectNone = view.findViewById(R.id.btn_select_none)
        btnDockerSort = view.findViewById(R.id.btn_docker_sort)
        etDockerSearch = view.findViewById(R.id.et_docker_search)
        cbRunningOnly = view.findViewById(R.id.cb_docker_running_only)
        sbCpuX = view.findViewById(R.id.sb_cpu_x)
        sbCpuY = view.findViewById(R.id.sb_cpu_y)
        sbMemX = view.findViewById(R.id.sb_mem_x)
        sbMemY = view.findViewById(R.id.sb_mem_y)
        sbDisk1X = view.findViewById(R.id.sb_disk1_x)
        sbDisk1Y = view.findViewById(R.id.sb_disk1_y)
        sbDisk2X = view.findViewById(R.id.sb_disk2_x)
        sbDisk2Y = view.findViewById(R.id.sb_disk2_y)
        sbDisk3X = view.findViewById(R.id.sb_disk3_x)
        sbDisk3Y = view.findViewById(R.id.sb_disk3_y)
        sbDockerX = view.findViewById(R.id.sb_docker_x)
        sbDockerY = view.findViewById(R.id.sb_docker_y)
        sbNetX = view.findViewById(R.id.sb_net_x)
        sbNetY = view.findViewById(R.id.sb_net_y)
        sbStorageX = view.findViewById(R.id.sb_storage_x)
        sbStorageY = view.findViewById(R.id.sb_storage_y)
        sbTempX = view.findViewById(R.id.sb_temp_x)
        sbTempY = view.findViewById(R.id.sb_temp_y)
        etCpuX = view.findViewById(R.id.et_cpu_x)
        etCpuY = view.findViewById(R.id.et_cpu_y)
        etMemX = view.findViewById(R.id.et_mem_x)
        etMemY = view.findViewById(R.id.et_mem_y)
        etDisk1X = view.findViewById(R.id.et_disk1_x)
        etDisk1Y = view.findViewById(R.id.et_disk1_y)
        etDisk2X = view.findViewById(R.id.et_disk2_x)
        etDisk2Y = view.findViewById(R.id.et_disk2_y)
        etDisk3X = view.findViewById(R.id.et_disk3_x)
        etDisk3Y = view.findViewById(R.id.et_disk3_y)
        etDockerX = view.findViewById(R.id.et_docker_x)
        etDockerY = view.findViewById(R.id.et_docker_y)
        etNetX = view.findViewById(R.id.et_net_x)
        etNetY = view.findViewById(R.id.et_net_y)
        etStorageX = view.findViewById(R.id.et_storage_x)
        etStorageY = view.findViewById(R.id.et_storage_y)
        etTempX = view.findViewById(R.id.et_temp_x)
        etTempY = view.findViewById(R.id.et_temp_y)
        tvSysCpu = view.findViewById(R.id.tv_sys_cpu)
        tvSysMem = view.findViewById(R.id.tv_sys_mem)
        tvSysOs = view.findViewById(R.id.tv_sys_os)
        tvSysUptime = view.findViewById(R.id.tv_sys_uptime)
        tvSysTime = view.findViewById(R.id.tv_sys_time)
        tvSysNet = view.findViewById(R.id.tv_sys_net)
        tvSysTemp = view.findViewById(R.id.tv_sys_temp)
        tvSysStorage = view.findViewById(R.id.tv_sys_storage)
        tvSysPeak = view.findViewById(R.id.tv_sys_peak)

        widgetId = requireActivity().intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            requireActivity().setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        }

        // 小组件预览
        layoutInflater.inflate(R.layout.nas_widget_preview, previewContainer, true)
        btnRefreshPreview.setOnClickListener { refreshPreview() }
        btnCopySummary.setOnClickListener { copySummary() }
        // 预览顶部的刷新图标与桌面组件一致，点击同样立即刷新
        previewContainer.findViewById<View>(R.id.btn_refresh)?.setOnClickListener { refreshPreview() }

        // 控件改动即自动保存
        setupAutoSaveListeners()

        // Docker 配置
        btnRefreshDocker.setOnClickListener { loadDockerContainers() }
        btnSelectAll.setOnClickListener {
            if (dockerCheckboxes.isEmpty()) {
                Toast.makeText(requireContext(), "请先加载容器列表", Toast.LENGTH_SHORT).show()
            } else {
                suppressSave = true
                for (cb in dockerCheckboxes) cb.isChecked = true
                suppressSave = false
                persist()
            }
        }
        btnSelectNone.setOnClickListener {
            if (dockerCheckboxes.isEmpty()) {
                Toast.makeText(requireContext(), "请先加载容器列表", Toast.LENGTH_SHORT).show()
            } else {
                suppressSave = true
                for (cb in dockerCheckboxes) cb.isChecked = false
                suppressSave = false
                persist()
            }
        }
        etDockerSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyDockerFilter() }
        })
        cbRunningOnly.setOnCheckedChangeListener { _, _ -> applyDockerFilter() }
        btnDockerSort.text = dockerSortLabel(Prefs.dockerSort(requireContext()))
        btnDockerSort.setOnClickListener {
            val modes = listOf("status", "name", "cpu", "mem")
            val cur = Prefs.dockerSort(requireContext())
            val idx = modes.indexOf(cur)
            val next = modes[(idx + 1 + modes.size) % modes.size]
            Prefs.setDockerSort(requireContext(), next)
            btnDockerSort.text = dockerSortLabel(next)
            buildDockerCheckboxes(dockerContainers)
            schedulePreviewRefresh()
        }

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
                    if (UpdateChecker.isNewer(info.version, local) &&
                        !UpdateChecker.isIgnored(requireContext(), info.version)
                    ) {
                        UpdateChecker.showUpdateDialog(requireActivity() as AppCompatActivity, info)
                    }
                }
            }
        }.start()
    }

    // 清掉之前拉满的极端偏移值（±80 会把组件顶出卡片），归零归中
    private fun resetExtremeOffsets() {
        for (comp in listOf("cpu", "mem", "disk1", "disk2", "disk3", "docker", "net", "storage", "temp")) {
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
        cbDisk3.isChecked = Prefs.showDisk3(requireContext())
        cbNet.isChecked = Prefs.showNet(requireContext())
        cbStorage.isChecked = Prefs.showStorage(requireContext())
        cbTempItem.isChecked = Prefs.showTempItem(requireContext())
        cbDocker.isChecked = Prefs.showDocker(requireContext())
        sbCpuX.progress = Prefs.compX(requireContext(), "cpu") + 50
        sbCpuY.progress = Prefs.compY(requireContext(), "cpu") + 50
        sbMemX.progress = Prefs.compX(requireContext(), "mem") + 50
        sbMemY.progress = Prefs.compY(requireContext(), "mem") + 50
        sbDisk1X.progress = Prefs.compX(requireContext(), "disk1") + 50
        sbDisk1Y.progress = Prefs.compY(requireContext(), "disk1") + 50
        sbDisk2X.progress = Prefs.compX(requireContext(), "disk2") + 50
        sbDisk2Y.progress = Prefs.compY(requireContext(), "disk2") + 50
        sbDisk3X.progress = Prefs.compX(requireContext(), "disk3") + 50
        sbDisk3Y.progress = Prefs.compY(requireContext(), "disk3") + 50
        sbDockerX.progress = Prefs.compX(requireContext(), "docker") + 50
        sbDockerY.progress = Prefs.compY(requireContext(), "docker") + 50
        sbNetX.progress = Prefs.compX(requireContext(), "net") + 50
        sbNetY.progress = Prefs.compY(requireContext(), "net") + 50
        sbStorageX.progress = Prefs.compX(requireContext(), "storage") + 50
        sbStorageY.progress = Prefs.compY(requireContext(), "storage") + 50
        sbTempX.progress = Prefs.compX(requireContext(), "temp") + 50
        sbTempY.progress = Prefs.compY(requireContext(), "temp") + 50
        etCpuX.setText(Prefs.compX(requireContext(), "cpu").toString())
        etCpuY.setText(Prefs.compY(requireContext(), "cpu").toString())
        etMemX.setText(Prefs.compX(requireContext(), "mem").toString())
        etMemY.setText(Prefs.compY(requireContext(), "mem").toString())
        etDisk1X.setText(Prefs.compX(requireContext(), "disk1").toString())
        etDisk1Y.setText(Prefs.compY(requireContext(), "disk1").toString())
        etDisk2X.setText(Prefs.compX(requireContext(), "disk2").toString())
        etDisk2Y.setText(Prefs.compY(requireContext(), "disk2").toString())
        etDisk3X.setText(Prefs.compX(requireContext(), "disk3").toString())
        etDisk3Y.setText(Prefs.compY(requireContext(), "disk3").toString())
        etDockerX.setText(Prefs.compX(requireContext(), "docker").toString())
        etDockerY.setText(Prefs.compY(requireContext(), "docker").toString())
        etNetX.setText(Prefs.compX(requireContext(), "net").toString())
        etNetY.setText(Prefs.compY(requireContext(), "net").toString())
        etStorageX.setText(Prefs.compX(requireContext(), "storage").toString())
        etStorageY.setText(Prefs.compY(requireContext(), "storage").toString())
        etTempX.setText(Prefs.compX(requireContext(), "temp").toString())
        etTempY.setText(Prefs.compY(requireContext(), "temp").toString())
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
        val usageThr = Prefs.alertUsageThreshold(requireContext()).coerceIn(1, 100)
        val cpuTempThr = Prefs.alertCpuTempThreshold(requireContext()).coerceIn(1, 120)
        val nvmeTempThr = Prefs.alertNvmeTempThreshold(requireContext()).coerceIn(1, 120)

        val contentArea = root.findViewById<FrameLayout>(R.id.content_area)
        contentArea.removeAllViews()

        root.findViewById<View>(R.id.row_uptime).visibility = if (showUptime) View.VISIBLE else View.GONE
        root.findViewById<TextView>(R.id.tv_title).setTextSize(TypedValue.COMPLEX_UNIT_PX, (fs + 3).toFloat())
        root.findViewById<TextView>(R.id.tv_uptime).setTextSize(TypedValue.COMPLEX_UNIT_PX, (fs + 2).toFloat())

        var stats: NasStats? = null
        result.onSuccess { s ->
            stats = s
            lastStats = s
            Prefs.recordPeaks(requireContext(), s.cpuPercent, s.memPercent, s.tempCpu, s.tempNvme)
            root.findViewById<TextView>(R.id.tv_uptime).text = "运行 " + Fmt.uptime(s.uptimeSec) + " · " + Fmt.time(s.fetchTime)
        }.onFailure {
            root.findViewById<TextView>(R.id.tv_uptime).text = "连接失败"
        }

        // 系统信息卡片（CPU 型号+核心数 / 系统版本 / 运行时长 / 温度）
        tvSysCpu.text = stats?.let { s ->
            s.cpuModel.takeIf { it.isNotEmpty() }?.let { "$it · ${s.cores}核" } ?: "--"
        } ?: "--"
        tvSysMem.text = stats?.let { s ->
            val free = (s.memTotal - s.memUsed).coerceAtLeast(0)
            String.format("%s/%s %.0f%%", Fmt.bytes(free), Fmt.bytes(s.memTotal), s.memPercent)
        } ?: "--"
        tvSysOs.text = stats?.osVersion?.takeIf { it.isNotEmpty() } ?: "--"
        tvSysUptime.text = stats?.let { Fmt.uptime(it.uptimeSec) } ?: "--"
        tvSysTime.text = stats?.let { Fmt.time(it.fetchTime) } ?: "--"
        tvSysNet.text = stats?.let { "↓${Fmt.speed(it.netDown)} ↑${Fmt.speed(it.netUp)}" } ?: "--"
        tvSysTemp.text = stats?.let { s ->
            val cpuT = s.tempCpu?.let { String.format("%.0f°", it) } ?: "--°"
            val nvmeT = s.tempNvme?.let { String.format("%.0f°", it) } ?: "--°"
            "CPU $cpuT · NVMe $nvmeT"
        } ?: "--"
        tvSysStorage.text = stats?.storageText ?: "--"
        tvSysPeak.text = peakText()

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
                item.findViewById<ProgressBar>(R.id.pb_cpu).progressTintList = ColorStateList.valueOf(Fmt.levelColor(s.cpuPercent))
                item.findViewById<TextView>(R.id.tv_cpu).text = tempPct(s.tempCpu, s.cpuPercent, showTemp)
                if (s.cpuPercent >= usageThr || (s.tempCpu?.let { it >= cpuTempThr } == true)) {
                    item.findViewById<TextView>(R.id.tv_cpu).setTextColor(Fmt.ALERT_COLOR)
                }
            } ?: run { item.findViewById<TextView>(R.id.tv_cpu).text = "--%" }
            item.findViewById<TextView>(R.id.tv_cpu_label).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            item.findViewById<TextView>(R.id.tv_cpu).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            addTo("cpu", item)
        }

        if (showMem) {
            val item = layoutInflater.inflate(R.layout.item_mem_preview, contentArea, false)
            stats?.let { s ->
                item.findViewById<ProgressBar>(R.id.pb_mem).progress = s.memPercent.toInt().coerceIn(0, 100)
                item.findViewById<ProgressBar>(R.id.pb_mem).progressTintList = ColorStateList.valueOf(Fmt.levelColor(s.memPercent))
                val memFree = (s.memTotal - s.memUsed).coerceAtLeast(0)
                item.findViewById<TextView>(R.id.tv_mem).text = String.format("%s/%s %.0f%%", Fmt.bytes(memFree), Fmt.bytes(s.memTotal), s.memPercent)
                if (s.memPercent >= usageThr) {
                    item.findViewById<TextView>(R.id.tv_mem).setTextColor(Fmt.ALERT_COLOR)
                }
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
                    item.findViewById<ProgressBar>(R.id.pb_disk1).progressTintList = ColorStateList.valueOf(Fmt.levelColor(d.usePercent))
                    item.findViewById<TextView>(R.id.tv_disk1).text = diskText(s.tempNvme, d.usePercent, d.total - d.used, showTemp)
                    if (d.usePercent >= usageThr || (s.tempNvme?.let { it >= nvmeTempThr } == true)) {
                        item.findViewById<TextView>(R.id.tv_disk1).setTextColor(Fmt.ALERT_COLOR)
                    }
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
                    item.findViewById<ProgressBar>(R.id.pb_disk2).progressTintList = ColorStateList.valueOf(Fmt.levelColor(d.usePercent))
                    item.findViewById<TextView>(R.id.tv_disk2).text = diskText(null, d.usePercent, d.total - d.used, false)
                    if (d.usePercent >= usageThr) {
                        item.findViewById<TextView>(R.id.tv_disk2).setTextColor(Fmt.ALERT_COLOR)
                    }
                } else {
                    item.findViewById<TextView>(R.id.tv_disk2).text = "--%"
                }
            } ?: run { item.findViewById<TextView>(R.id.tv_disk2).text = "--%" }
            item.findViewById<TextView>(R.id.tv_disk2_label).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            item.findViewById<TextView>(R.id.tv_disk2).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            addTo("disk2", item)
        }

        if (Prefs.showDisk3(requireContext())) {
            val item = layoutInflater.inflate(R.layout.item_disk3_preview, contentArea, false)
            stats?.let { s ->
                if (s.disks.size > 2) {
                    val d = s.disks[2]
                    item.findViewById<TextView>(R.id.tv_disk3_label).text = d.name
                    item.findViewById<ProgressBar>(R.id.pb_disk3).progress = d.usePercent.toInt().coerceIn(0, 100)
                    item.findViewById<ProgressBar>(R.id.pb_disk3).progressTintList = ColorStateList.valueOf(Fmt.levelColor(d.usePercent))
                    item.findViewById<TextView>(R.id.tv_disk3).text = diskText(null, d.usePercent, d.total - d.used, false)
                    if (d.usePercent >= usageThr) {
                        item.findViewById<TextView>(R.id.tv_disk3).setTextColor(Fmt.ALERT_COLOR)
                    }
                } else {
                    item.findViewById<TextView>(R.id.tv_disk3).text = "--%"
                }
            } ?: run { item.findViewById<TextView>(R.id.tv_disk3).text = "--%" }
            item.findViewById<TextView>(R.id.tv_disk3_label).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            item.findViewById<TextView>(R.id.tv_disk3).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            addTo("disk3", item)
        }

        if (Prefs.showStorage(requireContext())) {
            val item = layoutInflater.inflate(R.layout.item_storage_preview, contentArea, false)
            stats?.let { s ->
                item.findViewById<ProgressBar>(R.id.pb_storage).progress = s.storagePercent.toInt().coerceIn(0, 100)
                item.findViewById<ProgressBar>(R.id.pb_storage).progressTintList = ColorStateList.valueOf(Fmt.levelColor(s.storagePercent))
                item.findViewById<TextView>(R.id.tv_storage).text = s.storageText
                if (s.storagePercent >= usageThr) {
                    item.findViewById<TextView>(R.id.tv_storage).setTextColor(Fmt.ALERT_COLOR)
                }
            } ?: run { item.findViewById<TextView>(R.id.tv_storage).text = "--%" }
            item.findViewById<TextView>(R.id.tv_storage_label).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            item.findViewById<TextView>(R.id.tv_storage).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            addTo("storage", item)
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

        if (Prefs.showTempItem(requireContext())) {
            val item = layoutInflater.inflate(R.layout.item_temp_preview, contentArea, false)
            stats?.let { s ->
                val cpuT = s.tempCpu?.let { String.format("%.0f°", it) } ?: "--°"
                val nvmeT = s.tempNvme?.let { String.format("%.0f°", it) } ?: "--°"
                item.findViewById<TextView>(R.id.tv_temp).text = "CPU $cpuT · NVMe $nvmeT"
                if ((s.tempCpu?.let { it >= cpuTempThr } == true) || (s.tempNvme?.let { it >= nvmeTempThr } == true)) {
                    item.findViewById<TextView>(R.id.tv_temp).setTextColor(Fmt.ALERT_COLOR)
                }
            } ?: run { item.findViewById<TextView>(R.id.tv_temp).text = "CPU --° · NVMe --°" }
            item.findViewById<TextView>(R.id.tv_temp_label).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            item.findViewById<TextView>(R.id.tv_temp).setTextSize(TypedValue.COMPLEX_UNIT_PX, fs.toFloat())
            addTo("temp", item)
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
                        val summary = TextView(requireContext()).apply {
                            text = "${list.count { it.running }} 运行 / ${list.size} 总计"
                            setTextColor(requireContext().getColor(R.color.text_secondary))
                            setTextSize(TypedValue.COMPLEX_UNIT_PX, (fs - 2).toFloat())
                        }
                        group.addView(summary)
                        val filtered = list.filter { it.name in selected }.sortedWith(dockerComparator(Prefs.dockerSort(requireContext())))
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

    /** 复制当前监控摘要到剪贴板（CPU/内存/存储/温度/网络/运行时长） */
    private fun copySummary() {
        val s = lastStats
        if (s == null) {
            Toast.makeText(requireContext(), "暂无数据，请稍候刷新", Toast.LENGTH_SHORT).show()
            return
        }
        val lines = mutableListOf<String>()
        lines.add("NAS 监控")
        lines.add("CPU ${s.cpuPercent.toInt()}% · ${s.cores}核${s.cpuModel.takeIf { it.isNotEmpty() }?.let { " · $it" } ?: ""}")
        lines.add("内存 ${Fmt.bytes((s.memTotal - s.memUsed).coerceAtLeast(0))}/${Fmt.bytes(s.memTotal)} ${s.memPercent.toInt()}%")
        if (s.disks.isNotEmpty()) lines.add("存储 ${s.storageText}")
        val tempLine = when {
            s.tempCpu != null && s.tempNvme != null -> "温度 CPU ${s.tempCpu.toInt()}° · NVMe ${s.tempNvme.toInt()}°"
            s.tempCpu != null -> "温度 CPU ${s.tempCpu.toInt()}°"
            s.tempNvme != null -> "温度 NVMe ${s.tempNvme.toInt()}°"
            else -> null
        }
        tempLine?.let { lines.add(it) }
        lines.add("网络 ↓${Fmt.speed(s.netDown)} ↑${Fmt.speed(s.netUp)}")
        lines.add("运行 ${Fmt.uptime(s.uptimeSec)}")
        val text = lines.joinToString("\n")
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("nas-summary", text))
        Toast.makeText(requireContext(), "监控摘要已复制", Toast.LENGTH_SHORT).show()
    }

    private fun peakText(): String {
        val cpu = Prefs.peakCpu(requireContext())
        val mem = Prefs.peakMem(requireContext())
        val cpuT = Prefs.peakCpuTemp(requireContext())
        val nvmeT = Prefs.peakNvmeTemp(requireContext())
        if (cpu <= 0f && mem <= 0f && cpuT == null && nvmeT == null) return "--"
        val parts = mutableListOf<String>()
        if (cpu > 0f) parts.add(String.format("CPU %.0f%%", cpu))
        if (mem > 0f) parts.add(String.format("内存 %.0f%%", mem))
        val tempMax = listOfNotNull(cpuT, nvmeT).maxOrNull()
        if (tempMax != null) parts.add(String.format("温度 %.0f°", tempMax))
        return parts.joinToString(" · ")
    }

    private fun tempPct(temp: Double?, pct: Double, showTemp: Boolean): String {
        val pctText = String.format("%.0f%%", pct)
        if (!showTemp) return pctText
        val t = temp?.let { String.format("%.0f°", it) } ?: "--°"
        return "$t $pctText"
    }

    /** 磁盘值：可用空间 + 使用率（NVMe 附加温度） */
    private fun diskText(temp: Double?, pct: Double, free: Long, showTemp: Boolean): String {
        val freeText = Fmt.bytes(free)
        val pctText = String.format("%.0f%%", pct)
        if (!showTemp) return "$freeText $pctText"
        val t = temp?.let { String.format("%.0f°", it) } ?: "--°"
        return "$freeText $t $pctText"
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

    private fun dockerSortLabel(mode: String): String = when (mode) {
        "name" -> "名称"
        "cpu" -> "CPU"
        "mem" -> "内存"
        else -> "运行中优先"
    }

    private fun buildDockerCheckboxes(containers: List<DockerContainer>) {
        dockerListContainer.removeAllViews()
        dockerCheckboxes.clear()
        val selected = Prefs.dockerContainers(requireContext())
        val sorted = containers.sortedWith(dockerComparator(Prefs.dockerSort(requireContext())))
        for (c in sorted) {
            val item = layoutInflater.inflate(R.layout.item_docker_config, dockerListContainer, false)
            item.findViewById<TextView>(R.id.tv_dot).text = if (c.running) "●" else "○"
            item.findViewById<TextView>(R.id.tv_dot).setTextColor(if (c.running) 0xFF4CAF50.toInt() else 0xFF888888.toInt())
            item.findViewById<TextView>(R.id.tv_name).text = c.name
            item.findViewById<TextView>(R.id.tv_detail).text = c.detailText
            item.findViewById<View>(R.id.ll_info).setOnClickListener { showContainerDetail(c) }
            val cb = item.findViewById<SwitchCompat>(R.id.cb_select)
            cb.isChecked = c.name in selected
            cb.tag = c.name
            cb.setOnCheckedChangeListener { _, _ -> if (!suppressSave) persist() }
            dockerCheckboxes.add(cb)

            // 容器操作按钮（启动/重启/停止）：id 为空则无法操作，隐藏
            val btnStart = item.findViewById<TextView>(R.id.btn_action_start)
            val btnRestart = item.findViewById<TextView>(R.id.btn_action_restart)
            val btnStop = item.findViewById<TextView>(R.id.btn_action_stop)
            val operable = c.id.isNotEmpty()
            btnStart.visibility = if (operable && !c.running) View.VISIBLE else View.GONE
            btnRestart.visibility = if (operable && c.running) View.VISIBLE else View.GONE
            btnStop.visibility = if (operable && c.running) View.VISIBLE else View.GONE
            val actionBtns = listOf(btnStart, btnRestart, btnStop)
            btnStart.setOnClickListener { runContainerAction(c.id, c.name, ContainerAction.START, actionBtns) }
            btnRestart.setOnClickListener { runContainerAction(c.id, c.name, ContainerAction.RESTART, actionBtns) }
            btnStop.setOnClickListener { runContainerAction(c.id, c.name, ContainerAction.STOP, actionBtns) }

            dockerListContainer.addView(item)
        }
        applyDockerFilter()
    }

    /** 点击容器行弹出详情（状态/详情/CPU/内存/运行时长） */
    private fun showContainerDetail(c: DockerContainer) {
        val lines = mutableListOf<String>()
        lines.add("状态：${c.stateLabel}")
        if (c.status.isNotEmpty()) lines.add("详情：${c.status}")
        lines.add("CPU：${c.cpuText.ifEmpty { "--" }}")
        lines.add("内存：${c.memText.ifEmpty { "--" }}")
        if (c.uptimeText.isNotEmpty()) lines.add("运行时长：${c.uptimeText}")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(c.name)
            .setMessage(lines.joinToString("\n"))
            .setPositiveButton("关闭", null)
            .show()
    }

    /** 容器启动/停止/重启：调飞牛 API，成功后刷新列表与预览 */
    private fun runContainerAction(id: String, name: String, action: ContainerAction, buttons: List<View>) {
        if (!Prefs.isLoggedIn(requireContext())) {
            Toast.makeText(requireContext(), "请先登录飞牛账号", Toast.LENGTH_SHORT).show()
            return
        }
        buttons.forEach { it.isEnabled = false }
        Thread {
            val token = OAuthClient.getValidAccessToken(requireContext()).getOrNull()
            val result = if (token != null) {
                NasDataFetcher.containerAction(Prefs.nasBase(requireContext()), token, id, action)
            } else {
                Result.failure(Exception("未登录"))
            }
            requireActivity().runOnUiThread {
                buttons.forEach { it.isEnabled = true }
                val label = when (action) {
                    ContainerAction.START -> "启动"
                    ContainerAction.STOP -> "停止"
                    ContainerAction.RESTART -> "重启"
                }
                result.onSuccess {
                    Toast.makeText(requireContext(), "已$label $name", Toast.LENGTH_SHORT).show()
                    loadDockerContainers()
                    schedulePreviewRefresh()
                }.onFailure { e ->
                    Toast.makeText(requireContext(), "${label}失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    /** 按搜索框关键字 + 「只看运行中」过滤容器列表（只隐藏 item，不重建，避免丢失勾选状态） */
    private fun applyDockerFilter() {
        val query = etDockerSearch.text.toString().trim().lowercase()
        val runningOnly = cbRunningOnly.isChecked
        val runningNames = if (runningOnly) dockerContainers.filter { it.running }.map { it.name }.toSet() else null
        for (cb in dockerCheckboxes) {
            val name = cb.tag as? String ?: ""
            val nameMatch = query.isEmpty() || name.lowercase().contains(query)
            val runningMatch = runningNames == null || name in runningNames
            (cb.parent as? View)?.visibility = if (nameMatch && runningMatch) View.VISIBLE else View.GONE
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
        Prefs.setShowDisk3(requireContext(), cbDisk3.isChecked)
        Prefs.setShowDocker(requireContext(), cbDocker.isChecked)
        Prefs.setShowNet(requireContext(), cbNet.isChecked)
        Prefs.setShowStorage(requireContext(), cbStorage.isChecked)
        Prefs.setShowTempItem(requireContext(), cbTempItem.isChecked)
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
        Prefs.setCompX(requireContext(), "disk3", sbDisk3X.progress - 50)
        Prefs.setCompY(requireContext(), "disk3", sbDisk3Y.progress - 50)
        Prefs.setCompX(requireContext(), "docker", sbDockerX.progress - 50)
        Prefs.setCompY(requireContext(), "docker", sbDockerY.progress - 50)
        Prefs.setCompX(requireContext(), "net", sbNetX.progress - 50)
        Prefs.setCompY(requireContext(), "net", sbNetY.progress - 50)
        Prefs.setCompX(requireContext(), "storage", sbStorageX.progress - 50)
        Prefs.setCompY(requireContext(), "storage", sbStorageY.progress - 50)
        Prefs.setCompX(requireContext(), "temp", sbTempX.progress - 50)
        Prefs.setCompY(requireContext(), "temp", sbTempY.progress - 50)

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
        bindSlider(etDisk3X, sbDisk3X, "disk3", true)
        bindSlider(etDisk3Y, sbDisk3Y, "disk3", false)
        bindSlider(etDockerX, sbDockerX, "docker", true)
        bindSlider(etDockerY, sbDockerY, "docker", false)
        bindSlider(etNetX, sbNetX, "net", true)
        bindSlider(etNetY, sbNetY, "net", false)
        bindSlider(etStorageX, sbStorageX, "storage", true)
        bindSlider(etStorageY, sbStorageY, "storage", false)
        bindSlider(etTempX, sbTempX, "temp", true)
        bindSlider(etTempY, sbTempY, "temp", false)

        for (cb in listOf(cbCpu, cbMem, cbDisk1, cbDisk2, cbDisk3, cbDocker, cbNet, cbStorage, cbTempItem)) {
            cb.setOnCheckedChangeListener { _, _ -> if (!suppressSave) persist() }
        }

        // 滑块实时同步：拖动即时更新输入框 + 保存 + 刷新预览
        val etMap = mapOf(
            sbCpuX to etCpuX, sbCpuY to etCpuY,
            sbMemX to etMemX, sbMemY to etMemY,
            sbDisk1X to etDisk1X, sbDisk1Y to etDisk1Y,
            sbDisk2X to etDisk2X, sbDisk2Y to etDisk2Y,
            sbDisk3X to etDisk3X, sbDisk3Y to etDisk3Y,
            sbDockerX to etDockerX, sbDockerY to etDockerY,
            sbNetX to etNetX, sbNetY to etNetY,
            sbStorageX to etStorageX, sbStorageY to etStorageY,
            sbTempX to etTempX, sbTempY to etTempY
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
