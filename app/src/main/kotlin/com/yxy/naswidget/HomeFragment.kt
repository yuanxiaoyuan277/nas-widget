package com.yxy.naswidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private lateinit var scrollRoot: ScrollView
    private lateinit var swipeRefresh: SwipeRefreshLayout
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
    private lateinit var btnOpenNasWeb: Button
    private lateinit var btnCopySummary: Button
    private lateinit var btnShareSummary: Button
    private lateinit var btnReport: Button
    private lateinit var cbDocker: SwitchCompat
    private lateinit var dockerListContainer: LinearLayout
    private lateinit var btnRefreshDocker: Button
    private lateinit var btnSelectAll: Button
    private lateinit var btnSelectNone: Button
    private lateinit var btnDockerSort: Button
    private lateinit var btnDockerStartAll: Button
    private lateinit var btnDockerStopAll: Button
    private lateinit var btnDockerRestartAll: Button
    private lateinit var etDockerSearch: EditText
    private lateinit var btnDockerFilter: Button
    private lateinit var tvDockerCount: TextView
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
    private lateinit var tvSysUrl: TextView
    private lateinit var tvSysNet: TextView
    private lateinit var tvSysTemp: TextView
    private lateinit var tvSysStorage: TextView
    private lateinit var tvSysPeak: TextView
    private lateinit var tvSysTraffic: TextView
    private lateinit var tvSysAlertHistory: TextView
    private lateinit var btnClearPeak: TextView
    private lateinit var tvSysRefresh: TextView
    private lateinit var sparklineTrend: SparklineView
    private lateinit var tvTrendLabel: TextView
    private lateinit var cardConfig: LinearLayout
    private lateinit var tvConfigTitle: TextView
    private var configCollapsed = false

    private val handler = Handler(Looper.getMainLooper())
    private var dockerContainers: List<DockerContainer> = emptyList()
    private val dockerCheckboxes = mutableListOf<SwitchCompat>()
    private var previewBusy = false
    private var lastStats: NasStats? = null
    private val cpuHistory = mutableListOf<Float>()
    private val memHistory = mutableListOf<Float>()
    private val cpuTempHistory = mutableListOf<Float>()
    private val nvmeTempHistory = mutableListOf<Float>()
    private val netDownHistory = mutableListOf<Float>()
    private val netUpHistory = mutableListOf<Float>()
    /** 折线图视图模式：0=资源(CPU/内存) 1=温度(CPU/NVMe) 2=网络(下行/上行) */
    private var sparklineMode = 0
    private var suppressSave = false
    private var autoUpdateChecked = false
    private val refreshSecOptions = listOf(10, 15, 30, 60, 300)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        scrollRoot = view.findViewById(R.id.scroll_root)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeColors(requireContext().getColor(R.color.app_primary))
        swipeRefresh.setOnRefreshListener {
            refreshPreview()
            if (Prefs.isLoggedIn(requireContext())) loadDockerContainers()
            handler.postDelayed({ swipeRefresh.isRefreshing = false }, 800)
        }
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
        btnOpenNasWeb = view.findViewById(R.id.btn_open_nas_web)
        btnCopySummary = view.findViewById(R.id.btn_copy_summary)
        btnShareSummary = view.findViewById(R.id.btn_share_summary)
        btnReport = view.findViewById(R.id.btn_report)
        cbDocker = view.findViewById(R.id.cb_docker)
        dockerListContainer = view.findViewById(R.id.docker_list_container)
        btnRefreshDocker = view.findViewById(R.id.btn_refresh_docker)
        btnSelectAll = view.findViewById(R.id.btn_select_all)
        btnSelectNone = view.findViewById(R.id.btn_select_none)
        btnDockerSort = view.findViewById(R.id.btn_docker_sort)
        btnDockerStartAll = view.findViewById(R.id.btn_docker_start_all)
        btnDockerStopAll = view.findViewById(R.id.btn_docker_stop_all)
        btnDockerRestartAll = view.findViewById(R.id.btn_docker_restart_all)
        etDockerSearch = view.findViewById(R.id.et_docker_search)
        btnDockerFilter = view.findViewById(R.id.btn_docker_filter)
        tvDockerCount = view.findViewById(R.id.tv_docker_count)
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
        tvSysUrl = view.findViewById(R.id.tv_sys_url)
        tvSysNet = view.findViewById(R.id.tv_sys_net)
        tvSysTemp = view.findViewById(R.id.tv_sys_temp)
        tvSysStorage = view.findViewById(R.id.tv_sys_storage)
        tvSysPeak = view.findViewById(R.id.tv_sys_peak)
        tvSysTraffic = view.findViewById(R.id.tv_sys_traffic)
        tvSysAlertHistory = view.findViewById(R.id.tv_sys_alert_history)
        btnClearPeak = view.findViewById(R.id.btn_clear_peak)
        tvSysRefresh = view.findViewById(R.id.tv_sys_refresh)
        sparklineTrend = view.findViewById(R.id.sparkline_trend)
        tvTrendLabel = view.findViewById(R.id.tv_trend_label)
        // 点击折线图循环切换视图：资源 / 温度 / 网络
        sparklineTrend.setOnClickListener {
            sparklineMode = (sparklineMode + 1) % 3
            updateSparkline()
        }
        // 「组件配置」卡片折叠/展开：点击标题切换滑块区显隐，减少长页面滚动
        cardConfig = view.findViewById(R.id.card_config)
        tvConfigTitle = view.findViewById(R.id.tv_config_title)
        configCollapsed = Prefs.configCollapsed(requireContext())
        applyConfigCollapsed()
        tvConfigTitle.setOnClickListener {
            configCollapsed = !configCollapsed
            Prefs.setConfigCollapsed(requireContext(), configCollapsed)
            applyConfigCollapsed()
        }

        widgetId = requireActivity().intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            requireActivity().setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        }

        // 小组件预览
        layoutInflater.inflate(R.layout.nas_widget_preview, previewContainer, true)
        btnRefreshPreview.setOnClickListener { refreshPreview() }
        btnOpenNasWeb.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Prefs.nasBase(requireContext()))))
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "无法打开浏览器，请检查飞牛地址配置", Toast.LENGTH_SHORT).show()
            }
        }
        btnCopySummary.setOnClickListener { copySummary() }
        btnShareSummary.setOnClickListener { shareSummary() }
        btnReport.setOnClickListener { showReportDialog() }
        // 预览顶部的刷新图标与桌面组件一致，点击同样立即刷新
        previewContainer.findViewById<View>(R.id.btn_refresh)?.setOnClickListener { refreshPreview() }

        // 系统信息卡片：点击单项数值复制到剪贴板
        bindCopyRow(
            "CPU" to tvSysCpu,
            "系统" to tvSysOs,
            "更新" to tvSysTime,
            "数据源" to tvSysUrl,
            "网络" to tvSysNet
        )
        // 「内存」「温度」「存储」「峰值」行点击弹出明细
        tvSysMem.setOnClickListener { showMemDetail() }
        tvSysTemp.setOnClickListener { showTempDetail() }
        tvSysStorage.setOnClickListener { showDiskDetail() }
        tvSysPeak.setOnClickListener { showPeakDetail() }
        tvSysTraffic.setOnClickListener { showTrafficDetail() }
        tvSysAlertHistory.setOnClickListener { showAlertHistory() }
        // 「运行」行点击弹出已运行时长 + 本次启动时间
        tvSysUptime.setOnClickListener { showUptimeDetail() }
        // 「清零」按钮：一键清零今日峰值记录
        btnClearPeak.setOnClickListener { clearPeaks() }
        // 「刷新间隔」行点击循环切换
        tvSysRefresh.setOnClickListener { cycleRefreshInterval() }

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
        btnDockerFilter.text = dockerFilterLabel(Prefs.dockerFilter(requireContext()))
        btnDockerFilter.setOnClickListener {
            val modes = listOf("all", "running", "stopped", "selected")
            val cur = Prefs.dockerFilter(requireContext())
            val idx = modes.indexOf(cur)
            val next = modes[(idx + 1 + modes.size) % modes.size]
            Prefs.setDockerFilter(requireContext(), next)
            btnDockerFilter.text = dockerFilterLabel(next)
            applyDockerFilter()
        }
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
        btnDockerStartAll.setOnClickListener { batchContainerAction(ContainerAction.START) }
        btnDockerStopAll.setOnClickListener { batchContainerAction(ContainerAction.STOP) }
        btnDockerRestartAll.setOnClickListener { batchContainerAction(ContainerAction.RESTART) }

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
        tvSysRefresh.text = refreshSecLabel(Prefs.refreshSec(requireContext()))
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
            AlertNotifier.markOnline(requireContext())
            Prefs.recordPeaks(requireContext(), s.cpuPercent, s.memPercent, s.tempCpu, s.tempNvme)
            Prefs.recordNetPeak(requireContext(), s.netDown, s.netUp)
            Prefs.recordNetTraffic(requireContext(), s.netDown, s.netUp)
            cpuHistory.add(s.cpuPercent.toFloat())
            if (cpuHistory.size > 60) cpuHistory.removeAt(0)
            memHistory.add(s.memPercent.toFloat())
            if (memHistory.size > 60) memHistory.removeAt(0)
            s.tempCpu?.let { cpuTempHistory.add(it.toFloat()); if (cpuTempHistory.size > 60) cpuTempHistory.removeAt(0) }
            s.tempNvme?.let { nvmeTempHistory.add(it.toFloat()); if (nvmeTempHistory.size > 60) nvmeTempHistory.removeAt(0) }
            netDownHistory.add(s.netDown.toFloat())
            if (netDownHistory.size > 60) netDownHistory.removeAt(0)
            netUpHistory.add(s.netUp.toFloat())
            if (netUpHistory.size > 60) netUpHistory.removeAt(0)
            updateSparkline()
            root.findViewById<TextView>(R.id.tv_uptime).text = "运行 " + Fmt.uptime(s.uptimeSec) + " · " + Fmt.time(s.fetchTime)
            // 状态徽标：任一指标超阈值 → 告警
            val overUsage = s.cpuPercent >= usageThr || s.memPercent >= usageThr ||
                s.disks.any { it.usePercent >= usageThr }
            val overTemp = (s.tempCpu?.let { it >= cpuTempThr } == true) ||
                (s.tempNvme?.let { it >= nvmeTempThr } == true)
            val badge = root.findViewById<TextView>(R.id.tv_status_badge)
            if (overUsage || overTemp) {
                badge.text = "告警"
                badge.visibility = View.VISIBLE
            } else {
                badge.visibility = View.GONE
            }
        }.onFailure {
            AlertNotifier.markOffline(requireContext())
            root.findViewById<TextView>(R.id.tv_uptime).text = Fmt.offlineDuration(Prefs.offlineSince(requireContext()))
            val badge = root.findViewById<TextView>(R.id.tv_status_badge)
            badge.text = "离线"
            badge.visibility = View.VISIBLE
        }

        // 系统信息卡片（CPU 型号+核心数 / 系统版本 / 运行时长 / 温度）
        val defaultTextColor = requireContext().getColor(R.color.app_text_primary)
        fun tintSys(tv: TextView, over: Boolean) {
            tv.setTextColor(if (over) Fmt.ALERT_COLOR else defaultTextColor)
        }
        tvSysCpu.text = stats?.let { s ->
            val model = s.cpuModel.takeIf { it.isNotEmpty() }
            val pct = String.format("%.0f%%", s.cpuPercent)
            if (model != null) "$model · ${s.cores}核 · $pct" else "${s.cores}核 · $pct"
        } ?: "--"
        tintSys(tvSysCpu, (stats?.cpuPercent ?: 0.0) >= usageThr)
        tvSysMem.text = stats?.let { s ->
            val free = (s.memTotal - s.memUsed).coerceAtLeast(0)
            String.format("%s/%s %.0f%%", Fmt.bytes(free), Fmt.bytes(s.memTotal), s.memPercent)
        } ?: "--"
        tintSys(tvSysMem, (stats?.memPercent ?: 0.0) >= usageThr)
        tvSysOs.text = stats?.osVersion?.takeIf { it.isNotEmpty() } ?: "--"
        tvSysUptime.text = stats?.let { Fmt.uptime(it.uptimeSec) } ?: "--"
        tvSysTime.text = stats?.let { s ->
            if (s.latencyMs > 0) "${Fmt.time(s.fetchTime)} · ${s.latencyMs}ms" else Fmt.time(s.fetchTime)
        } ?: "--"
        tvSysUrl.text = Prefs.url(requireContext())
        tvSysNet.text = stats?.let { "↓${Fmt.speed(it.netDown)} ↑${Fmt.speed(it.netUp)}" } ?: "--"
        tvSysTraffic.text = trafficText()
        tvSysAlertHistory.text = alertHistoryLabel()
        tvSysTemp.text = stats?.let { s ->
            val cpuT = s.tempCpu?.let { String.format("%.0f°", it) } ?: "--°"
            val nvmeT = s.tempNvme?.let { String.format("%.0f°", it) } ?: "--°"
            "CPU $cpuT · NVMe $nvmeT"
        } ?: "--"
        val overTemp = stats?.let { s ->
            (s.tempCpu?.let { it >= cpuTempThr } == true) || (s.tempNvme?.let { it >= nvmeTempThr } == true)
        } ?: false
        tintSys(tvSysTemp, overTemp)
        tvSysStorage.text = stats?.storageText ?: "--"
        tintSys(tvSysStorage, (stats?.storagePercent ?: 0.0) >= usageThr)
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

    /** 生成当前监控摘要文本（CPU/内存/存储/温度/网络/运行时长），无数据返回 null */
    private fun summaryText(): String? {
        val s = lastStats ?: return null
        val lines = mutableListOf<String>()
        lines.add("NAS 监控")
        lines.add("CPU ${s.cpuPercent.toInt()}% · ${s.cores}核${s.cpuModel.takeIf { it.isNotEmpty() }?.let { " · $it" } ?: ""}")
        lines.add("内存 ${Fmt.bytes((s.memTotal - s.memUsed).coerceAtLeast(0))}/${Fmt.bytes(s.memTotal)} ${s.memPercent.toInt()}%")
        if (s.disks.isNotEmpty()) {
            lines.add("存储 ${s.storageText}")
            for (d in s.disks.sortedByDescending { it.usePercent }) {
                lines.add("  ${d.name} ${Fmt.bytes(d.total - d.used)}/${Fmt.bytes(d.total)} ${d.usePercent.toInt()}%")
            }
        }
        val tempLine = when {
            s.tempCpu != null && s.tempNvme != null -> "温度 CPU ${s.tempCpu.toInt()}° · NVMe ${s.tempNvme.toInt()}°"
            s.tempCpu != null -> "温度 CPU ${s.tempCpu.toInt()}°"
            s.tempNvme != null -> "温度 NVMe ${s.tempNvme.toInt()}°"
            else -> null
        }
        tempLine?.let { lines.add(it) }
        lines.add("网络 ↓${Fmt.speed(s.netDown)} ↑${Fmt.speed(s.netUp)}")
        val tDown = Prefs.trafficDown(requireContext())
        val tUp = Prefs.trafficUp(requireContext())
        if (tDown > 0L || tUp > 0L) lines.add("今日流量 ↓${Fmt.bytes(tDown)} ↑${Fmt.bytes(tUp)}")
        if (s.osVersion.isNotEmpty()) lines.add("系统 ${s.osVersion}")
        lines.add("运行 ${Fmt.uptime(s.uptimeSec)}")
        if (s.latencyMs > 0) lines.add("数据源延迟 ${s.latencyMs}ms")
        return lines.joinToString("\n")
    }

    /** 复制当前监控摘要到剪贴板 */
    private fun copySummary() {
        val text = summaryText()
        if (text == null) {
            Toast.makeText(requireContext(), "暂无数据，请稍候刷新", Toast.LENGTH_SHORT).show()
            return
        }
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("nas-summary", text))
        Toast.makeText(requireContext(), "监控摘要已复制", Toast.LENGTH_SHORT).show()
    }

    /** 更新折线图：按当前视图模式（资源/温度/网络）重建序列并刷新图例标签 */
    private fun updateSparkline() {
        val label: String
        val series: List<SparklineView.Series>
        when (sparklineMode) {
            0 -> {
                label = "CPU / 内存趋势（点击切换）"
                series = listOf(
                    SparklineView.Series(0xFF5E9EFF.toInt(), cpuHistory.toList(), 100f),
                    SparklineView.Series(0xFF58D68D.toInt(), memHistory.toList(), 100f)
                )
            }
            1 -> {
                label = "温度趋势（点击切换）"
                series = listOf(
                    SparklineView.Series(0xFFFF7043.toInt(), cpuTempHistory.toList(), 100f),
                    SparklineView.Series(0xFFBA68C8.toInt(), nvmeTempHistory.toList(), 100f)
                )
            }
            else -> {
                label = "网络速率趋势（点击切换）"
                series = listOf(
                    SparklineView.Series(0xFF58D68D.toInt(), netDownHistory.toList(), 0f),
                    SparklineView.Series(0xFFFFB74D.toInt(), netUpHistory.toList(), 0f)
                )
            }
        }
        tvTrendLabel.text = label
        sparklineTrend.setSeries(series)
    }

    /** 给系统信息卡片的各项数值绑定「点击复制」：点一下把「标签: 值」复制到剪贴板 */
    private fun bindCopyRow(vararg pairs: Pair<String, TextView>) {
        for ((label, tv) in pairs) {
            tv.setOnClickListener {
                val v = tv.text.toString()
                if (v.isEmpty() || v == "--") return@setOnClickListener
                val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText(label, "$label: $v"))
                Toast.makeText(requireContext(), "已复制 $label", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 查看内存明细：点「内存」行弹出已用/可用/总量/使用率/告警阈值 */
    private fun showMemDetail() {
        val s = lastStats
        if (s == null) {
            Toast.makeText(requireContext(), "暂无数据，请稍候刷新", Toast.LENGTH_SHORT).show()
            return
        }
        val used = s.memUsed
        val total = s.memTotal
        val free = (total - used).coerceAtLeast(0)
        val thr = Prefs.alertUsageThreshold(requireContext())
        val lines = mutableListOf(
            String.format("已用：%s", Fmt.bytes(used)),
            String.format("可用：%s", Fmt.bytes(free)),
            String.format("总量：%s", Fmt.bytes(total)),
            String.format("使用率：%.0f%%（告警阈值 %d%%）", s.memPercent, thr)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("内存明细")
            .setMessage(lines.joinToString("\n"))
            .setPositiveButton("关闭", null)
            .show()
    }

    /** 查看温度明细：点「温度」行弹出 CPU / NVMe 温度与各自告警阈值 */
    private fun showTempDetail() {
        val s = lastStats
        if (s == null) {
            Toast.makeText(requireContext(), "暂无数据，请稍候刷新", Toast.LENGTH_SHORT).show()
            return
        }
        val cpuThr = Prefs.alertCpuTempThreshold(requireContext())
        val nvmeThr = Prefs.alertNvmeTempThreshold(requireContext())
        val cpuT = s.tempCpu?.let { String.format("%.0f°C（阈值 %d°C）", it, cpuThr) } ?: "未获取"
        val nvmeT = s.tempNvme?.let { String.format("%.0f°C（阈值 %d°C）", it, nvmeThr) } ?: "未获取"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("温度明细")
            .setMessage("CPU 温度：$cpuT\nNVMe 温度：$nvmeT")
            .setPositiveButton("关闭", null)
            .show()
    }

    /** 查看各磁盘明细：点「存储」行弹出每块盘的可用/总量/使用率（按已用率降序，最满的盘排最前） */
    private fun showDiskDetail() {
        val s = lastStats
        if (s == null || s.disks.isEmpty()) {
            Toast.makeText(requireContext(), "暂无磁盘数据，请稍候刷新", Toast.LENGTH_SHORT).show()
            return
        }
        val lines = mutableListOf<String>()
        lines.add(String.format("总览：已用 %s / 总量 %s（使用率 %.0f%%）", Fmt.bytes(s.storageUsed), Fmt.bytes(s.storageTotal), s.storagePercent))
        for (d in s.disks.sortedByDescending { it.usePercent }) {
            lines.add(String.format("%s：可用 %s / %s（已用 %.0f%%）", d.name, Fmt.bytes(d.total - d.used), Fmt.bytes(d.total), d.usePercent))
        }
        val text = lines.joinToString("\n")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("磁盘明细")
            .setMessage(text)
            .setPositiveButton("复制") { _, _ ->
                val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("磁盘明细", text))
                Toast.makeText(requireContext(), "磁盘明细已复制", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    /** 查看运行时长明细：点「运行」行弹出已运行时长 + 本次启动时间，可复制 */
    private fun showUptimeDetail() {
        val s = lastStats
        if (s == null) {
            Toast.makeText(requireContext(), "暂无数据，请稍候刷新", Toast.LENGTH_SHORT).show()
            return
        }
        val bootMillis = s.fetchTime - s.uptimeSec * 1000L
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val bootStr = fmt.format(java.util.Date(bootMillis))
        val uptimeStr = Fmt.uptime(s.uptimeSec)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("运行时长")
            .setMessage("已运行：$uptimeStr\n本次启动：$bootStr")
            .setPositiveButton("复制") { _, _ ->
                val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("uptime", "运行 $uptimeStr · 自 $bootStr 启动"))
                Toast.makeText(requireContext(), "运行时长已复制", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    /** 查看今日峰值明细：点「峰值」行弹出各指标峰值 + 告警阈值参考 */
    private fun showPeakDetail() {
        val cpu = Prefs.peakCpu(requireContext())
        val mem = Prefs.peakMem(requireContext())
        val cpuT = Prefs.peakCpuTemp(requireContext())
        val nvmeT = Prefs.peakNvmeTemp(requireContext())
        val netDown = Prefs.peakNetDown(requireContext())
        val netUp = Prefs.peakNetUp(requireContext())
        val usageThr = Prefs.alertUsageThreshold(requireContext())
        val cpuTempThr = Prefs.alertCpuTempThreshold(requireContext())
        val nvmeTempThr = Prefs.alertNvmeTempThreshold(requireContext())
        val lines = mutableListOf<String>()
        lines.add(if (cpu > 0f) String.format("CPU：%.0f%%（阈值 %d%%）", cpu, usageThr) else "CPU：暂无")
        lines.add(if (mem > 0f) String.format("内存：%.0f%%（阈值 %d%%）", mem, usageThr) else "内存：暂无")
        lines.add(if (cpuT != null) String.format("CPU 温度：%.0f°（阈值 %d°）", cpuT, cpuTempThr) else "CPU 温度：暂无")
        lines.add(if (nvmeT != null) String.format("NVMe 温度：%.0f°（阈值 %d°）", nvmeT, nvmeTempThr) else "NVMe 温度：暂无")
        lines.add(if (netDown > 0L || netUp > 0L) String.format("网络：↓%s ↑%s", Fmt.speed(netDown), Fmt.speed(netUp)) else "网络：暂无")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("今日峰值")
            .setMessage(lines.joinToString("\n"))
            .setPositiveButton("关闭", null)
            .show()
    }

    /** 查看今日流量明细：累计下行/上行 + 峰值速率，可一键清零累计流量 */
    private fun showTrafficDetail() {
        val down = Prefs.trafficDown(requireContext())
        val up = Prefs.trafficUp(requireContext())
        val netDown = Prefs.peakNetDown(requireContext())
        val netUp = Prefs.peakNetUp(requireContext())
        val lines = listOf(
            "今日累计：↓${Fmt.bytes(down)} ↑${Fmt.bytes(up)}",
            "峰值速率：↓${Fmt.speed(netDown)} ↑${Fmt.speed(netUp)}"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("今日流量")
            .setMessage(lines.joinToString("\n"))
            .setPositiveButton("清零") { _, _ ->
                Prefs.clearTraffic(requireContext())
                tvSysTraffic.text = trafficText()
                Toast.makeText(requireContext(), "今日流量已清零", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    /** 今日累计流量可读文本（无数据时为 --） */
    private fun trafficText(): String {
        val down = Prefs.trafficDown(requireContext())
        val up = Prefs.trafficUp(requireContext())
        return if (down <= 0L && up <= 0L) "--" else "↓${Fmt.bytes(down)} ↑${Fmt.bytes(up)}"
    }

    /** 告警历史入口可读文本：有记录显示条数，无记录显示「无」 */
    private fun alertHistoryLabel(): String {
        val n = Prefs.alertHistory(requireContext()).size
        return if (n > 0) "$n 条 ›" else "无 ›"
    }

    /** 弹出告警历史弹窗（最近 20 条，可清空 / 分享） */
    private fun showAlertHistory() {
        val history = Prefs.alertHistory(requireContext())
        val message = if (history.isEmpty()) "暂无告警记录" else history.joinToString("\n")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("告警历史")
            .setMessage(message)
            .setNeutralButton("分享") { _, _ -> shareAlertHistory(history) }
            .setPositiveButton("清空") { _, _ ->
                Prefs.clearAlertHistory(requireContext())
                tvSysAlertHistory.text = alertHistoryLabel()
                Toast.makeText(requireContext(), "告警历史已清空", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    /** 分享告警历史（走系统分享，可发到微信/QQ 等） */
    private fun shareAlertHistory(history: List<String>) {
        if (history.isEmpty()) {
            Toast.makeText(requireContext(), "暂无告警记录", Toast.LENGTH_SHORT).show()
            return
        }
        val text = "NAS 告警历史（最近 ${history.size} 条）\n" + history.joinToString("\n")
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        requireContext().startActivity(Intent.createChooser(send, "分享告警历史"))
    }

    /** 清零今日峰值记录（CPU/内存/温度/网络），并立即刷新显示 */
    private fun clearPeaks() {
        Prefs.clearPeaks(requireContext())
        tvSysPeak.text = peakText()
        Toast.makeText(requireContext(), "今日峰值已清零", Toast.LENGTH_SHORT).show()
    }

    /** 刷新间隔可读文本（10/15/30/60 秒、5 分钟） */
    private fun refreshSecLabel(sec: Int): String = when (sec) {
        10 -> "10 秒"
        15 -> "15 秒"
        30 -> "30 秒"
        60 -> "60 秒"
        300 -> "5 分钟"
        else -> "$sec 秒"
    }

    /** 点击「刷新间隔」行：循环切换刷新间隔，即时生效并重排刷新调度 */
    private fun cycleRefreshInterval() {
        val cur = Prefs.refreshSec(requireContext())
        val idx = refreshSecOptions.indexOf(cur)
        val next = refreshSecOptions[(idx + 1 + refreshSecOptions.size) % refreshSecOptions.size]
        Prefs.setRefresh(requireContext(), next)
        tvSysRefresh.text = refreshSecLabel(next)
        AlarmScheduler.schedule(requireContext())
        Toast.makeText(requireContext(), "刷新间隔已设为 ${refreshSecLabel(next)}", Toast.LENGTH_SHORT).show()
    }

    /** 分享当前监控摘要（走系统分享，可发到微信/QQ 等） */
    private fun shareSummary() {
        val text = summaryText()
        if (text == null) {
            Toast.makeText(requireContext(), "暂无数据，请稍候刷新", Toast.LENGTH_SHORT).show()
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        requireContext().startActivity(Intent.createChooser(send, "分享监控摘要"))
    }

    /** 生成完整诊断报告：NAS 指标 + Docker 容器状态 + 今日流量/峰值 + 告警历史 */
    private fun reportText(): String {
        val sb = StringBuilder()
        sb.append("NAS 诊断报告\n")
        sb.append("══════════════\n")
        val s = lastStats
        if (s != null) {
            sb.append("【系统指标】\n")
            sb.append("CPU：${s.cpuPercent.toInt()}% · ${s.cores}核${s.cpuModel.takeIf { it.isNotEmpty() }?.let { " · $it" } ?: ""}\n")
            sb.append("内存：${Fmt.bytes((s.memTotal - s.memUsed).coerceAtLeast(0))}/${Fmt.bytes(s.memTotal)} ${s.memPercent.toInt()}%\n")
            if (s.disks.isNotEmpty()) sb.append("存储：${s.storageText}\n")
            val tempLine = when {
                s.tempCpu != null && s.tempNvme != null -> "温度：CPU ${s.tempCpu.toInt()}° · NVMe ${s.tempNvme.toInt()}°"
                s.tempCpu != null -> "温度：CPU ${s.tempCpu.toInt()}°"
                s.tempNvme != null -> "温度：NVMe ${s.tempNvme.toInt()}°"
                else -> null
            }
            tempLine?.let { sb.append("$it\n") }
            sb.append("网络：↓${Fmt.speed(s.netDown)} ↑${Fmt.speed(s.netUp)}\n")
            if (s.osVersion.isNotEmpty()) sb.append("系统：${s.osVersion}\n")
            sb.append("运行：${Fmt.uptime(s.uptimeSec)}\n")
            if (s.latencyMs > 0) sb.append("数据源延迟：${s.latencyMs}ms\n")
        } else {
            sb.append("【系统指标】暂无数据\n")
        }
        val tDown = Prefs.trafficDown(requireContext())
        val tUp = Prefs.trafficUp(requireContext())
        if (tDown > 0L || tUp > 0L) sb.append("\n【今日流量】↓${Fmt.bytes(tDown)} ↑${Fmt.bytes(tUp)}\n")
        val peak = peakText()
        if (peak != "--") sb.append("\n【今日峰值】$peak\n")
        if (dockerContainers.isNotEmpty()) {
            val running = dockerContainers.count { it.running }
            sb.append("\n【Docker 容器】${running} 运行 / ${dockerContainers.size} 总计\n")
            for (c in dockerContainers.sortedWith(dockerComparator(Prefs.dockerSort(requireContext())))) {
                sb.append("- ${c.name}：${if (c.running) c.detailText else c.stateText}\n")
            }
        }
        val history = Prefs.alertHistory(requireContext())
        if (history.isNotEmpty()) {
            sb.append("\n【告警历史】最近 ${history.size} 条\n")
            for (h in history) sb.append("- $h\n")
        }
        return sb.toString().trimEnd()
    }

    /** 弹出诊断报告：完整整合 NAS 指标 + Docker + 流量/峰值 + 告警，可复制/分享 */
    private fun showReportDialog() {
        val report = reportText()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("诊断报告")
            .setMessage(report)
            .setPositiveButton("复制") { _, _ ->
                val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("nas-report", report))
                Toast.makeText(requireContext(), "诊断报告已复制", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("分享") { _, _ ->
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, report)
                }
                requireContext().startActivity(Intent.createChooser(send, "分享诊断报告"))
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun peakText(): String {
        val cpu = Prefs.peakCpu(requireContext())
        val mem = Prefs.peakMem(requireContext())
        val cpuT = Prefs.peakCpuTemp(requireContext())
        val nvmeT = Prefs.peakNvmeTemp(requireContext())
        val netDown = Prefs.peakNetDown(requireContext())
        val netUp = Prefs.peakNetUp(requireContext())
        if (cpu <= 0f && mem <= 0f && cpuT == null && nvmeT == null && netDown <= 0L && netUp <= 0L) return "--"
        val parts = mutableListOf<String>()
        if (cpu > 0f) parts.add(String.format("CPU %.0f%%", cpu))
        if (mem > 0f) parts.add(String.format("内存 %.0f%%", mem))
        val tempMax = listOfNotNull(cpuT, nvmeT).maxOrNull()
        if (tempMax != null) parts.add(String.format("温度 %.0f°", tempMax))
        if (netDown > 0L || netUp > 0L) parts.add(String.format("网络 ↓%s ↑%s", Fmt.speed(netDown), Fmt.speed(netUp)))
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

    private fun dockerFilterLabel(mode: String): String = when (mode) {
        "running" -> "运行中"
        "stopped" -> "已停止"
        "selected" -> "已勾选"
        else -> "全部"
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
            item.findViewById<TextView>(R.id.tv_detail).text =
                listOfNotNull(c.image.takeIf { it.isNotEmpty() }, c.detailText.takeIf { it.isNotEmpty() })
                    .joinToString(" · ").ifEmpty { "--" }
            item.findViewById<View>(R.id.ll_info).setOnClickListener { showContainerDetail(c) }
            val cb = item.findViewById<SwitchCompat>(R.id.cb_select)
            cb.isChecked = c.name in selected
            cb.tag = c.name
            cb.setOnCheckedChangeListener { _, _ ->
                if (!suppressSave) {
                    persist()
                    applyDockerFilter()
                    updateDockerCount()
                }
            }
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
        updateDockerCount()
        applyDockerFilter()
    }

    /** 更新 Docker 统计头：运行/总计 + 已勾选数量（无容器时提示刷新） */
    private fun updateDockerCount() {
        val total = dockerContainers.size
        val running = dockerContainers.count { it.running }
        val checked = dockerCheckboxes.count { it.isChecked }
        tvDockerCount.text = if (total == 0) "暂无容器，点击「刷新」加载"
        else "$running 运行 / $total 总计 · 已勾选 $checked"
    }

    /** 点击容器行弹出详情（状态/详情/CPU/内存/运行时长） */
    private fun showContainerDetail(c: DockerContainer) {
        val lines = mutableListOf<String>()
        lines.add("状态：${c.stateLabel}")
        if (c.id.isNotEmpty()) lines.add("ID：${c.id.take(12)}")
        if (c.image.isNotEmpty()) lines.add("镜像：${c.image}")
        if (c.portText.isNotEmpty()) lines.add("端口：${c.portText}")
        if (c.status.isNotEmpty()) lines.add("详情：${c.status}")
        lines.add("CPU：${c.cpuText.ifEmpty { "--" }}")
        lines.add("内存：${c.memText.ifEmpty { "--" }}")
        if (c.uptimeText.isNotEmpty()) lines.add("运行时长：${c.uptimeText}")
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(c.name)
            .setMessage(lines.joinToString("\n"))
            .setPositiveButton("关闭", null)
        if (c.id.isNotEmpty()) {
            builder.setNegativeButton("复制 ID") { _, _ ->
                val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("container-id", c.id))
                Toast.makeText(requireContext(), "容器 ID 已复制", Toast.LENGTH_SHORT).show()
            }
        }
        if (c.publicPorts.isNotEmpty()) {
            builder.setNeutralButton("打开 Web") { _, _ -> openContainerWeb(c) }
        }
        builder.show()
    }

    /** 打开容器对外服务：优先 443 用 https、其余 http，多端口时让用户选一个 */
    private fun openContainerWeb(c: DockerContainer) {
        val ports = c.publicPorts
        if (ports.isEmpty()) return
        val base = Prefs.nasBase(requireContext())
        val uri = runCatching { Uri.parse(base) }.getOrNull()
        val host = uri?.let {
            val scheme = it.scheme ?: "http"
            val h = it.host
            if (h == null) null else "$scheme://$h"
        }
        if (host == null) {
            Toast.makeText(requireContext(), "无法解析 NAS 地址，请在设置页确认", Toast.LENGTH_SHORT).show()
            return
        }
        fun urlOf(port: Int): String {
            val scheme = if (port == 443) "https" else host.substringBefore("://")
            return "$scheme://${host.substringAfter("://")}:$port"
        }
        if (ports.size == 1) {
            openUrl(urlOf(ports[0]))
        } else {
            val items = ports.map { p -> urlOf(p) }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择要打开的端口")
                .setItems(items) { _, which -> openUrl(items[which]) }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    /** 用系统浏览器打开 URL，失败时提示 */
    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "无法打开浏览器", Toast.LENGTH_SHORT).show()
        }
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

    /** 批量操作：全部启动 / 全部停止 / 全部重启（二次确认后顺序执行） */
    private fun batchContainerAction(action: ContainerAction) {
        if (!Prefs.isLoggedIn(requireContext())) {
            Toast.makeText(requireContext(), "请先登录飞牛账号", Toast.LENGTH_SHORT).show()
            return
        }
        val targets = when (action) {
            ContainerAction.START -> dockerContainers.filter { !it.running && it.id.isNotEmpty() }
            ContainerAction.STOP, ContainerAction.RESTART -> dockerContainers.filter { it.running && it.id.isNotEmpty() }
        }
        if (targets.isEmpty()) {
            val label = if (action == ContainerAction.START) "没有需要启动的容器" else "没有运行中的容器"
            Toast.makeText(requireContext(), label, Toast.LENGTH_SHORT).show()
            return
        }
        val verb = actionVerb(action)
        val selected = Prefs.dockerContainers(requireContext())
        val selectedTargets = targets.filter { it.name in selected }
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle("批量$verb")
            .setMessage("将对以下 ${targets.size} 个容器执行「$verb」：\n${targets.joinToString("、") { it.name }}")
            .setPositiveButton("全部$verb") { _, _ -> runBatch(action, targets) }
            .setNegativeButton("取消", null)
        if (selectedTargets.isNotEmpty()) {
            builder.setNeutralButton("仅已勾选(${selectedTargets.size})") { _, _ -> runBatch(action, selectedTargets) }
        }
        builder.show()
    }

    /** 容器操作动词：启动/停止/重启 */
    private fun actionVerb(action: ContainerAction): String = when (action) {
        ContainerAction.START -> "启动"
        ContainerAction.STOP -> "停止"
        ContainerAction.RESTART -> "重启"
    }

    /** 执行批量操作：顺序对每个容器调飞牛 API，完成后刷新列表与预览 */
    private fun runBatch(action: ContainerAction, targets: List<DockerContainer>) {
        val verb = actionVerb(action)
        Toast.makeText(requireContext(), "正在批量$verb…", Toast.LENGTH_SHORT).show()
        Thread {
            val token = OAuthClient.getValidAccessToken(requireContext()).getOrNull()
            var ok = 0
            var fail = 0
            if (token != null) {
                for (c in targets) {
                    val r = NasDataFetcher.containerAction(Prefs.nasBase(requireContext()), token, c.id, action)
                    if (r.isSuccess) ok++ else fail++
                }
            } else {
                fail = targets.size
            }
            val finalOk = ok
            val finalFail = fail
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "批量${verb}完成：成功 $finalOk，失败 $finalFail", Toast.LENGTH_LONG).show()
                loadDockerContainers()
                schedulePreviewRefresh()
            }
        }.start()
    }

    /** 按搜索框关键字 + 状态筛选（全部/运行中/已停止）过滤容器列表（只隐藏 item，不重建，避免丢失勾选状态） */
    private fun applyDockerFilter() {
        val query = etDockerSearch.text.toString().trim().lowercase()
        val filter = Prefs.dockerFilter(requireContext())
        val matchNames = when (filter) {
            "running" -> dockerContainers.filter { it.running }.map { it.name }.toSet()
            "stopped" -> dockerContainers.filter { !it.running }.map { it.name }.toSet()
            "selected" -> dockerCheckboxes.filter { it.isChecked }.mapNotNull { it.tag as? String }.toSet()
            else -> null
        }
        val nameToContainer = dockerContainers.associateBy { it.name }
        for (cb in dockerCheckboxes) {
            val name = cb.tag as? String ?: ""
            val c = nameToContainer[name]
            val haystack = if (c != null && c.image.isNotEmpty()) "$name ${c.image}".lowercase() else name.lowercase()
            val nameMatch = query.isEmpty() || haystack.contains(query)
            val filterMatch = matchNames == null || name in matchNames
            (cb.parent as? View)?.visibility = if (nameMatch && filterMatch) View.VISIBLE else View.GONE
        }
    }

    private fun refreshDockerVisibility() {
        // Docker 配置区（勾选列表）始终显示，让用户能配置勾选哪些容器
        // showDocker 只控制桌面组件/预览是否显示 Docker 项目，不应隐藏配置列表
        dockerListContainer.visibility = View.VISIBLE
    }

    /** 折叠/展开「组件配置」区：隐藏/显示标题之外的所有滑块块，并更新标题文案 */
    private fun applyConfigCollapsed() {
        for (i in 0 until cardConfig.childCount) {
            val child = cardConfig.getChildAt(i)
            if (child !== tvConfigTitle) {
                child.visibility = if (configCollapsed) View.GONE else View.VISIBLE
            }
        }
        tvConfigTitle.text = if (configCollapsed) "组件配置（点击展开）" else "组件配置（点击折叠）"
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
