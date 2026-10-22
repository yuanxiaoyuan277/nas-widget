package com.yxy.naswidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

/** 设置页：NAS 地址、wb2api 积分、外观（深色浅色）、检查更新 */
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    companion object {
        private const val REQ_LOGIN = 1001
    }

    private val refreshOptions = listOf(10 to "10 秒", 15 to "15 秒", 30 to "30 秒", 60 to "60 秒", 300 to "5 分钟")
    private val repeatOptions = listOf(15 to "15 分钟", 30 to "30 分钟", 60 to "60 分钟", 120 to "2 小时")
    private lateinit var spRefresh: Spinner
    private lateinit var spAlertRepeat: Spinner
    private var currentRefreshSec = 30
    private lateinit var etFontSize: EditText
    private lateinit var scrollRoot: ScrollView
    private lateinit var tvLoginStatus: TextView
    private lateinit var btnLogin: Button
    private lateinit var btnOpenNasWeb: Button
    private lateinit var etUrl: EditText
    private lateinit var btnTestUrl: Button
    private lateinit var cbWb2api: CheckBox
    private lateinit var etWb2apiBase: EditText
    private lateinit var etWb2apiKey: EditText
    private lateinit var btnWb2apiDetail: Button
    private lateinit var tvWb2apiDetail: TextView
    private lateinit var btnToggleTheme: Button
    private lateinit var btnWidgetBg: Button
    private lateinit var btnWidgetTap: Button
    private lateinit var tvVersion: TextView
    private lateinit var btnCheckUpdate: Button
    private lateinit var btnAboutCheckUpdate: Button
    private lateinit var tvUpdateStatus: TextView
    private lateinit var btnBackup: Button
    private lateinit var btnRestore: Button
    private lateinit var btnShareBackup: Button
    private lateinit var tvBackupStatus: TextView
    private lateinit var tvAboutVersion: TextView
    private lateinit var tvAboutRepo: TextView
    private lateinit var tvAboutFeedback: TextView
    private lateinit var cbAlert: CheckBox
    private lateinit var cbAlertSound: CheckBox
    private lateinit var cbMonitorNotif: CheckBox
    private lateinit var cbMonCpu: CheckBox
    private lateinit var cbMonMem: CheckBox
    private lateinit var cbMonStorage: CheckBox
    private lateinit var cbMonTemp: CheckBox
    private lateinit var cbMonNet: CheckBox
    private lateinit var cbMonTraffic: CheckBox
    private lateinit var etAlertUsage: EditText
    private lateinit var etAlertCpuTemp: EditText
    private lateinit var etAlertNvmeTemp: EditText
    private lateinit var btnAlertPreset: Button
    private lateinit var cbSilent: CheckBox
    private lateinit var etSilentStart: EditText
    private lateinit var etSilentEnd: EditText
    private lateinit var btnTempSilent: Button
    private lateinit var tvTempSilentStatus: TextView
    private lateinit var btnAlertHistory: Button
    private lateinit var btnTestNotif: Button
    private lateinit var btnReset: Button
    private lateinit var tvBatteryStatus: TextView
    private lateinit var btnBatteryWhitelist: Button
    private var suppressSave = false
    private val saveHandler = Handler(Looper.getMainLooper())
    private val saveRunnable = Runnable { persist() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scrollRoot = view.findViewById(R.id.settings_scroll)
        spRefresh = view.findViewById(R.id.sp_refresh)
        spAlertRepeat = view.findViewById(R.id.sp_alert_repeat)
        etFontSize = view.findViewById(R.id.et_font_size)
        tvLoginStatus = view.findViewById(R.id.tv_login_status)
        btnLogin = view.findViewById(R.id.btn_login)
        btnOpenNasWeb = view.findViewById(R.id.btn_open_nas_web)
        etUrl = view.findViewById(R.id.et_url)
        btnTestUrl = view.findViewById(R.id.btn_test_url)
        cbWb2api = view.findViewById(R.id.cb_wb2api)
        etWb2apiBase = view.findViewById(R.id.et_wb2api_base)
        etWb2apiKey = view.findViewById(R.id.et_wb2api_key)
        btnWb2apiDetail = view.findViewById(R.id.btn_wb2api_detail)
        tvWb2apiDetail = view.findViewById(R.id.tv_wb2api_detail)
        btnToggleTheme = view.findViewById(R.id.btn_toggle_theme)
        btnWidgetBg = view.findViewById(R.id.btn_widget_bg)
        btnWidgetTap = view.findViewById(R.id.btn_widget_tap)
        tvVersion = view.findViewById(R.id.tv_version)
        btnCheckUpdate = view.findViewById(R.id.btn_check_update)
        btnAboutCheckUpdate = view.findViewById(R.id.btn_about_check_update)
        tvUpdateStatus = view.findViewById(R.id.tv_update_status)
        btnBackup = view.findViewById(R.id.btn_backup)
        btnRestore = view.findViewById(R.id.btn_restore)
        btnShareBackup = view.findViewById(R.id.btn_share_backup)
        tvBackupStatus = view.findViewById(R.id.tv_backup_status)
        tvAboutVersion = view.findViewById(R.id.tv_about_version)
        tvAboutRepo = view.findViewById(R.id.tv_about_repo)
        tvAboutFeedback = view.findViewById(R.id.tv_about_feedback)
        cbAlert = view.findViewById(R.id.cb_alert)
        cbAlertSound = view.findViewById(R.id.cb_alert_sound)
        cbMonitorNotif = view.findViewById(R.id.cb_monitor_notif)
        cbMonCpu = view.findViewById(R.id.cb_mon_cpu)
        cbMonMem = view.findViewById(R.id.cb_mon_mem)
        cbMonStorage = view.findViewById(R.id.cb_mon_storage)
        cbMonTemp = view.findViewById(R.id.cb_mon_temp)
        cbMonNet = view.findViewById(R.id.cb_mon_net)
        cbMonTraffic = view.findViewById(R.id.cb_mon_traffic)
        etAlertUsage = view.findViewById(R.id.et_alert_usage)
        etAlertCpuTemp = view.findViewById(R.id.et_alert_cpu_temp)
        etAlertNvmeTemp = view.findViewById(R.id.et_alert_nvme_temp)
        btnAlertPreset = view.findViewById(R.id.btn_alert_preset)
        cbSilent = view.findViewById(R.id.cb_silent)
        etSilentStart = view.findViewById(R.id.et_silent_start)
        etSilentEnd = view.findViewById(R.id.et_silent_end)
        btnTempSilent = view.findViewById(R.id.btn_temp_silent)
        tvTempSilentStatus = view.findViewById(R.id.tv_temp_silent_status)
        btnAlertHistory = view.findViewById(R.id.btn_alert_history)
        btnTestNotif = view.findViewById(R.id.btn_test_notif)
        btnReset = view.findViewById(R.id.btn_reset)
        tvBatteryStatus = view.findViewById(R.id.tv_battery_status)
        btnBatteryWhitelist = view.findViewById(R.id.btn_battery_whitelist)

        btnLogin.setOnClickListener {
            requireActivity().startActivityForResult(Intent(requireContext(), LoginActivity::class.java), REQ_LOGIN)
        }
        btnOpenNasWeb.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Prefs.nasBase(requireContext()))))
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "无法打开浏览器，请检查飞牛地址配置", Toast.LENGTH_SHORT).show()
            }
        }
        btnTestUrl.setOnClickListener { testUrl() }
        btnToggleTheme.setOnClickListener { toggleTheme() }
        btnWidgetBg.setOnClickListener { cycleWidgetBg() }
        btnWidgetTap.setOnClickListener { cycleWidgetTap() }
        btnWb2apiDetail.setOnClickListener { fetchWb2apiDetail() }
        btnCheckUpdate.setOnClickListener { checkUpdate() }
        btnAboutCheckUpdate.setOnClickListener { checkUpdate() }
        btnBackup.setOnClickListener { backupConfig() }
        btnRestore.setOnClickListener { restoreConfig() }
        btnShareBackup.setOnClickListener { shareBackup() }
        btnAlertHistory.setOnClickListener { showAlertHistory() }
        btnAlertPreset.setOnClickListener { showThresholdPreset() }
        btnTempSilent.setOnClickListener { toggleTempSilent() }
        btnTestNotif.setOnClickListener {
            val ok = AlertNotifier.sendTest(requireContext())
            if (ok) Toast.makeText(requireContext(), "已发送测试通知，请下拉通知栏查看", Toast.LENGTH_SHORT).show()
            else Toast.makeText(requireContext(), "通知权限未开启，请在系统设置中允许通知", Toast.LENGTH_LONG).show()
        }
        btnReset.setOnClickListener { confirmReset() }
        btnBatteryWhitelist.setOnClickListener { openBatteryWhitelist() }
        tvAboutRepo.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/yuanxiaoyuan277/nas-widget")))
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "无法打开浏览器", Toast.LENGTH_SHORT).show()
            }
        }
        tvAboutFeedback.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/yuanxiaoyuan277/nas-widget/issues")))
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "无法打开浏览器", Toast.LENGTH_SHORT).show()
            }
        }
        cbWb2api.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        cbAlert.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        cbAlertSound.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        cbMonitorNotif.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        cbMonCpu.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        cbMonMem.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        cbMonStorage.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        cbMonTemp.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        cbMonNet.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        cbMonTraffic.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        cbSilent.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        watchEditText(etUrl)
        watchEditText(etWb2apiBase)
        watchEditText(etWb2apiKey)
        watchEditText(etAlertUsage)
        watchEditText(etAlertCpuTemp)
        watchEditText(etAlertNvmeTemp)
        watchEditText(etSilentStart)
        watchEditText(etSilentEnd)
        spRefresh.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressSave) return
                if (position == refreshOptions.size) {
                    showCustomRefreshDialog()
                } else {
                    currentRefreshSec = refreshOptions[position].first
                    scheduleSave()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        val repeatAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, repeatOptions.map { it.second })
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spAlertRepeat.adapter = repeatAdapter
        spAlertRepeat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!suppressSave) scheduleSave()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        etFontSize.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { if (!suppressSave) scheduleSave() }
        })

        // 输入框默认隐藏 + 显示/隐藏切换
        setupToggle(etUrl, view.findViewById(R.id.btn_toggle_url))
        setupToggle(etWb2apiBase, view.findViewById(R.id.btn_toggle_wb2api_base))
        setupToggle(etWb2apiKey, view.findViewById(R.id.btn_toggle_wb2api_key))

        // 键盘遮挡自动滚动
        setupAutoScroll(etUrl)
        setupAutoScroll(etWb2apiBase)
        setupAutoScroll(etWb2apiKey)
        setupAutoScroll(etAlertUsage)
        setupAutoScroll(etAlertCpuTemp)
        setupAutoScroll(etAlertNvmeTemp)
        setupAutoScroll(etSilentStart)
        setupAutoScroll(etSilentEnd)

        loadPrefs()

        // 返回键即完成（改动已自动保存）
        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishConfig()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_LOGIN && resultCode == Activity.RESULT_OK) {
            updateLoginStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        // 从系统白名单设置返回后刷新状态
        if (::tvBatteryStatus.isInitialized) refreshBatteryStatus()
    }

    private fun updateLoginStatus() {
        val logged = Prefs.isLoggedIn(requireContext())
        tvLoginStatus.text = if (logged) "已登录飞牛账号" else "未登录"
        btnLogin.text = if (logged) "重新登录" else "登录飞牛账号"
    }

    private fun loadPrefs() {
        suppressSave = true
        updateLoginStatus()
        etUrl.setText(Prefs.url(requireContext()))
        val cur = Prefs.refreshSec(requireContext())
        currentRefreshSec = cur
        rebuildRefreshSpinner()
        val repIdx = repeatOptions.indexOfFirst { it.first == Prefs.alertRepeatMin(requireContext()) }
        spAlertRepeat.setSelection(if (repIdx >= 0) repIdx else 1)
        etFontSize.setText(Prefs.fontSize(requireContext()).toString())
        cbWb2api.isChecked = Prefs.showWb2api(requireContext())
        cbAlert.isChecked = Prefs.showAlert(requireContext())
        cbAlertSound.isChecked = Prefs.alertSound(requireContext())
        cbMonitorNotif.isChecked = Prefs.showMonitorNotif(requireContext())
        val monItems = Prefs.monNotifItems(requireContext())
        cbMonCpu.isChecked = "cpu" in monItems
        cbMonMem.isChecked = "mem" in monItems
        cbMonStorage.isChecked = "storage" in monItems
        cbMonTemp.isChecked = "temp" in monItems
        cbMonNet.isChecked = "net" in monItems
        cbMonTraffic.isChecked = "traffic" in monItems
        etAlertUsage.setText(Prefs.alertUsageThreshold(requireContext()).toString())
        etAlertCpuTemp.setText(Prefs.alertCpuTempThreshold(requireContext()).toString())
        etAlertNvmeTemp.setText(Prefs.alertNvmeTempThreshold(requireContext()).toString())
        cbSilent.isChecked = Prefs.silentEnabled(requireContext())
        etSilentStart.setText(Prefs.silentStart(requireContext()).toString())
        etSilentEnd.setText(Prefs.silentEnd(requireContext()).toString())
        etWb2apiBase.setText(Prefs.wb2apiBase(requireContext()))
        etWb2apiKey.setText(Prefs.wb2apiKey(requireContext()))
        tvVersion.text = "v${UpdateChecker.localVersion(requireContext())}"
        tvAboutVersion.text = "v${UpdateChecker.localVersion(requireContext())}"
        updateThemeButton()
        updateWidgetBgButton()
        updateWidgetTapButton()
        updateTempSilentStatus()
        refreshBatteryStatus()
        suppressSave = false
    }

    /** 重建刷新间隔下拉：标准档 + 末尾「自定义…」（或「自定义 X 秒」），并选中当前值 */
    private fun rebuildRefreshSpinner() {
        val labels = refreshOptions.map { it.second }.toMutableList()
        val customLabel = if (refreshOptions.none { it.first == currentRefreshSec })
            "自定义 $currentRefreshSec 秒" else "自定义…"
        labels.add(customLabel)
        val adapter = spRefresh.adapter as? ArrayAdapter<String>
        if (adapter == null) {
            val a = ArrayAdapter(requireContext(), R.layout.spinner_item, labels)
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spRefresh.adapter = a
        } else {
            adapter.clear()
            adapter.addAll(labels)
            adapter.notifyDataSetChanged()
        }
        val prev = suppressSave
        suppressSave = true
        val idx = refreshOptions.indexOfFirst { it.first == currentRefreshSec }
        spRefresh.setSelection(if (idx >= 0) idx else refreshOptions.size)
        suppressSave = prev
    }

    /** 自定义刷新间隔：输入 5~3600 秒，保存后刷新下拉显示 */
    private fun showCustomRefreshDialog() {
        val input = EditText(requireContext()).apply {
            hint = "5~3600 秒"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentRefreshSec.toString())
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("自定义刷新间隔")
            .setMessage("输入 5~3600 之间的秒数（桌面组件与主页刷新按此间隔）")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val v = input.text.toString().trim().toIntOrNull()
                if (v == null || v < 5 || v > 3600) {
                    Toast.makeText(requireContext(), "请输入 5~3600 之间的整数", Toast.LENGTH_SHORT).show()
                    rebuildRefreshSpinner()
                } else {
                    currentRefreshSec = v
                    rebuildRefreshSpinner()
                    scheduleSave()
                }
            }
            .setNegativeButton("取消") { _, _ -> rebuildRefreshSpinner() }
            .show()
    }

    private fun themeLabel(mode: String): String = when (mode) {
        "light" -> "浅色"
        "system" -> "跟随系统"
        else -> "深色"
    }

    private fun updateThemeButton() {
        btnToggleTheme.text = "${themeLabel(Prefs.theme(requireContext()))}（点击切换）"
    }

    private fun toggleTheme() {
        val next = when (Prefs.theme(requireContext())) {
            "dark" -> "light"
            "light" -> "system"
            else -> "dark"
        }
        Prefs.setTheme(requireContext(), next)
        AppCompatDelegate.setDefaultNightMode(Prefs.nightMode(requireContext()))
        updateThemeButton()
    }

    private fun widgetBgLabel(mode: String): String = when (mode) {
        "lighter" -> "更透明"
        "lightest" -> "最透明"
        else -> "标准"
    }

    private fun updateWidgetBgButton() {
        btnWidgetBg.text = "组件背景：${widgetBgLabel(Prefs.widgetBg(requireContext()))}（点击切换）"
    }

    /** 循环切换组件背景透明度：标准 / 更透明 / 最透明，改完立即刷新桌面组件 */
    private fun cycleWidgetBg() {
        val next = when (Prefs.widgetBg(requireContext())) {
            "standard" -> "lighter"
            "lighter" -> "lightest"
            else -> "standard"
        }
        Prefs.setWidgetBg(requireContext(), next)
        updateWidgetBgButton()
        val am = AppWidgetManager.getInstance(requireContext())
        val ids = am.getAppWidgetIds(ComponentName(requireContext(), NasWidgetProvider::class.java))
        if (ids.isNotEmpty()) NasWidgetProvider.updateAll(requireContext(), am, ids)
    }

    private fun widgetTapLabel(mode: String): String = if (mode == "web") "飞牛管理网页" else "App"

    private fun updateWidgetTapButton() {
        btnWidgetTap.text = "点击组件：打开 ${widgetTapLabel(Prefs.widgetTap(requireContext()))}（点击切换）"
    }

    /** 循环切换组件点击行为：打开 App 主界面 / 飞牛管理网页，改完立即刷新桌面组件 */
    private fun cycleWidgetTap() {
        val next = if (Prefs.widgetTap(requireContext()) == "web") "app" else "web"
        Prefs.setWidgetTap(requireContext(), next)
        updateWidgetTapButton()
        val am = AppWidgetManager.getInstance(requireContext())
        val ids = am.getAppWidgetIds(ComponentName(requireContext(), NasWidgetProvider::class.java))
        if (ids.isNotEmpty()) NasWidgetProvider.updateAll(requireContext(), am, ids)
    }

    /** 告警阈值三档预设：均衡（默认）/ 宽松 / 激进 */
    private data class ThresholdPreset(val label: String, val usage: Int, val cpuTemp: Int, val nvmeTemp: Int)
    private val thresholdPresets = listOf(
        ThresholdPreset("均衡 · 使用率90% / CPU85° / NVMe70°", 90, 85, 70),
        ThresholdPreset("宽松 · 使用率95% / CPU90° / NVMe80°", 95, 90, 80),
        ThresholdPreset("激进 · 使用率80% / CPU75° / NVMe60°", 80, 75, 60)
    )

    /** 弹出阈值预设选择：一键把三个阈值切到对应档位并保存 */
    private fun showThresholdPreset() {
        val items = thresholdPresets.map { it.label }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("阈值预设")
            .setItems(items) { _, which ->
                val p = thresholdPresets[which]
                suppressSave = true
                etAlertUsage.setText(p.usage.toString())
                etAlertCpuTemp.setText(p.cpuTemp.toString())
                etAlertNvmeTemp.setText(p.nvmeTemp.toString())
                suppressSave = false
                persist()
                Toast.makeText(requireContext(), "已应用预设：${p.label}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 临时静默：一键开启 1 小时免打扰（到点自动恢复），已开启时再次点击取消 */
    private fun toggleTempSilent() {
        if (Prefs.isTempSilent(requireContext())) {
            Prefs.setSilentUntil(requireContext(), 0L)
            Toast.makeText(requireContext(), "已取消临时静默，恢复推送告警", Toast.LENGTH_SHORT).show()
        } else {
            Prefs.setSilentUntil(requireContext(), System.currentTimeMillis() + 3600_000L)
            Toast.makeText(requireContext(), "已开启临时静默 1 小时，期间不推送告警", Toast.LENGTH_SHORT).show()
        }
        updateTempSilentStatus()
    }

    /** 刷新临时静默状态：未开启 / 静默至 HH:mm */
    private fun updateTempSilentStatus() {
        val until = Prefs.silentUntil(requireContext())
        if (until <= System.currentTimeMillis()) {
            btnTempSilent.text = "临时静默 1 小时"
            tvTempSilentStatus.text = "未开启"
        } else {
            btnTempSilent.text = "取消临时静默"
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = until
            tvTempSilentStatus.text = String.format(
                "静默至 %02d:%02d",
                cal.get(java.util.Calendar.HOUR_OF_DAY),
                cal.get(java.util.Calendar.MINUTE)
            )
        }
    }

    /** 是否已加入系统电池优化白名单 */
    private fun isBatteryWhitelisted(): Boolean {
        val pm = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(requireContext().packageName)
    }

    /** 刷新后台保活状态提示（从系统设置返回后调用） */
    private fun refreshBatteryStatus() {
        if (isBatteryWhitelisted()) {
            tvBatteryStatus.text = "已加入白名单，后台定时刷新与告警不受省电限制"
            btnBatteryWhitelist.isEnabled = false
            btnBatteryWhitelist.text = "已在白名单中"
        } else {
            tvBatteryStatus.text = "未加入白名单，省电策略可能延迟后台刷新与告警"
            btnBatteryWhitelist.isEnabled = true
            btnBatteryWhitelist.text = "加入电池优化白名单"
        }
    }

    /** 跳转系统电池优化白名单：优先直接弹授权对话框，失败则打开白名单列表 */
    private fun openBatteryWhitelist() {
        val ctx = requireContext()
        val pkg = ctx.packageName
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$pkg"))
        try {
            startActivity(direct)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(ctx, "无法打开系统设置，请手动在「设置 - 电池」中添加白名单", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** 测试 NAS 监控接口连通性（用当前输入框里的地址，失败提示错误信息） */
    private fun testUrl() {
        btnTestUrl.isEnabled = false
        btnTestUrl.text = "测试中…"
        val url = etUrl.text.toString().trim().ifEmpty { Prefs.DEFAULT_URL }
        Thread {
            val result = NasDataFetcher.fetch(url)
            requireActivity().runOnUiThread {
                btnTestUrl.isEnabled = true
                btnTestUrl.text = "测试连接"
                result.onSuccess { s ->
                    Toast.makeText(
                        requireContext(),
                        "连接成功 · CPU ${s.cpuPercent.toInt()}% · 内存 ${s.memPercent.toInt()}%",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { e ->
                    Toast.makeText(requireContext(), "连接失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /** 查询 wb2api 各账号积分明细（用当前输入框的 base/key） */
    private fun fetchWb2apiDetail() {
        btnWb2apiDetail.isEnabled = false
        btnWb2apiDetail.text = "查询中…"
        val base = etWb2apiBase.text.toString().trim().ifEmpty { Prefs.DEFAULT_WB2API_BASE }
        val key = etWb2apiKey.text.toString().trim().ifEmpty { Prefs.DEFAULT_WB2API_KEY }
        Thread {
            val result = NasDataFetcher.fetchWb2apiCredits(base, key)
            requireActivity().runOnUiThread {
                btnWb2apiDetail.isEnabled = true
                btnWb2apiDetail.text = "查看积分明细"
                result.onSuccess { s ->
                    val lines = mutableListOf("总计 ${s.totalCredits} · 健康 ${s.healthyCount}/${s.accountCount}")
                    for (a in s.accounts) {
                        var line = "${a.nickname}: ${a.credits}"
                        if (a.cooling) line += "（冷却中）"
                        if (a.disabled) line += "（已禁用）"
                        lines.add(line)
                    }
                    tvWb2apiDetail.text = lines.joinToString("\n")
                }.onFailure { e ->
                    tvWb2apiDetail.text = "查询失败：${e.message}"
                }
            }
        }.start()
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
        Prefs.setUrl(requireContext(), url)
        Prefs.setShowWb2api(requireContext(), cbWb2api.isChecked)
        Prefs.setShowAlert(requireContext(), cbAlert.isChecked)
        Prefs.setAlertSound(requireContext(), cbAlertSound.isChecked)
        Prefs.setShowMonitorNotif(requireContext(), cbMonitorNotif.isChecked)
        val monItems = mutableSetOf<String>()
        if (cbMonCpu.isChecked) monItems.add("cpu")
        if (cbMonMem.isChecked) monItems.add("mem")
        if (cbMonStorage.isChecked) monItems.add("storage")
        if (cbMonTemp.isChecked) monItems.add("temp")
        if (cbMonNet.isChecked) monItems.add("net")
        if (cbMonTraffic.isChecked) monItems.add("traffic")
        Prefs.setMonNotifItems(requireContext(), monItems)
        Prefs.setAlertUsageThreshold(requireContext(), etAlertUsage.text.toString().toIntOrNull()?.coerceIn(1, 100) ?: 90)
        Prefs.setAlertCpuTempThreshold(requireContext(), etAlertCpuTemp.text.toString().toIntOrNull()?.coerceIn(1, 120) ?: 85)
        Prefs.setAlertNvmeTempThreshold(requireContext(), etAlertNvmeTemp.text.toString().toIntOrNull()?.coerceIn(1, 120) ?: 70)
        Prefs.setSilentEnabled(requireContext(), cbSilent.isChecked)
        Prefs.setSilentStart(requireContext(), etSilentStart.text.toString().toIntOrNull()?.coerceIn(0, 23) ?: 23)
        Prefs.setSilentEnd(requireContext(), etSilentEnd.text.toString().toIntOrNull()?.coerceIn(0, 23) ?: 7)
        Prefs.setRefresh(requireContext(), currentRefreshSec)
        val repSel = spAlertRepeat.selectedItemPosition
        Prefs.setAlertRepeatMin(requireContext(), repeatOptions.getOrElse(repSel) { 30 to "30 分钟" }.first)
        val fs = etFontSize.text.toString().toIntOrNull()?.coerceIn(8, 200) ?: 16
        Prefs.setFontSize(requireContext(), fs)
        Prefs.setWb2apiBase(requireContext(), etWb2apiBase.text.toString().trim().ifEmpty { Prefs.DEFAULT_WB2API_BASE })
        Prefs.setWb2apiKey(requireContext(), etWb2apiKey.text.toString().trim().ifEmpty { Prefs.DEFAULT_WB2API_KEY })

        val am = AppWidgetManager.getInstance(requireContext())
        val ids = am.getAppWidgetIds(ComponentName(requireContext(), NasWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            NasWidgetProvider.updateAll(requireContext(), am, ids)
        }

        // 状态栏常驻监控：开启则调度定时器并立即刷新一次通知，关闭则取消
        if (Prefs.showMonitorNotif(requireContext())) {
            AlarmScheduler.schedule(requireContext())
            Thread {
                val r = NasDataFetcher.fetch(Prefs.url(requireContext()))
                MonitorNotifier.updateFromStats(requireContext(), r)
            }.start()
        } else {
            MonitorNotifier.cancel(requireContext())
        }
    }

    private fun finishConfig() {
        saveHandler.removeCallbacks(saveRunnable)
        persist()
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveHandler.removeCallbacksAndMessages(null)
    }

    private fun backupConfig() {
        val json = BackupHelper.export(requireContext())
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("nas-widget-config", json))
        tvBackupStatus.text = "配置已复制到剪贴板"
        Toast.makeText(requireContext(), "配置已复制，可粘贴到微信/记事本保存", Toast.LENGTH_LONG).show()
    }

    private fun restoreConfig() {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(requireContext()).toString()
        if (text.isBlank()) {
            tvBackupStatus.text = "剪贴板为空"
            return
        }
        if (BackupHelper.import(requireContext(), text)) {
            tvBackupStatus.text = "配置已恢复"
            Toast.makeText(requireContext(), "配置已恢复，部分设置重启后生效", Toast.LENGTH_LONG).show()
            loadPrefs()
        } else {
            tvBackupStatus.text = "恢复失败：配置格式无效"
        }
    }

    /** 导出配置为 JSON 文件并分享（走 FileProvider + ACTION_SEND） */
    private fun shareBackup() {
        try {
            val json = BackupHelper.export(requireContext())
            val file = File(requireContext().cacheDir, "nas-widget-config.json")
            file.writeText(json)
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(share, "分享配置"))
            tvBackupStatus.text = "已生成配置文件，选择应用保存/发送"
        } catch (e: Exception) {
            tvBackupStatus.text = "导出失败：${e.message}"
        }
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

    /** 「恢复默认设置」：二次确认后清空全部配置并重启 App */
    private fun confirmReset() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("恢复默认设置")
            .setMessage("将清除所有配置（含飞牛登录、NAS 地址、告警阈值、主题、Docker 勾选等），恢复为初始状态。此操作不可撤销，确定继续？")
            .setPositiveButton("恢复默认") { _, _ ->
                Prefs.clearAll(requireContext())
                MonitorNotifier.cancel(requireContext())
                AlarmScheduler.cancel(requireContext())
                val restart = Intent.makeRestartActivityTask(
                    requireContext().packageManager.getLaunchIntentForPackage(requireContext().packageName)?.component
                )
                requireContext().startActivity(restart)
                Runtime.getRuntime().exit(0)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun checkUpdate() {
        btnCheckUpdate.isEnabled = false
        tvUpdateStatus.text = "检查更新中…"
        Thread {
            val result = UpdateChecker.checkLatest()
            requireActivity().runOnUiThread {
                btnCheckUpdate.isEnabled = true
                result.onSuccess { info ->
                    val local = UpdateChecker.localVersion(requireContext())
                    if (UpdateChecker.isNewer(info.version, local)) {
                        if (UpdateChecker.isIgnored(requireContext(), info.version)) {
                            // 该版本已被忽略：提示并可取消忽略重新查看更新
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("已忽略 v${info.version}")
                                .setMessage("你之前已忽略此版本。是否取消忽略并查看更新？")
                                .setPositiveButton("取消忽略") { _, _ ->
                                    Prefs.setIgnoredVersion(requireContext(), "")
                                    tvUpdateStatus.text = ""
                                    UpdateChecker.showUpdateDialog(requireActivity() as AppCompatActivity, info)
                                }
                                .setNegativeButton("保持忽略") { _, _ ->
                                    tvUpdateStatus.text = "已忽略 v${info.version}"
                                }
                                .show()
                        } else {
                            tvUpdateStatus.text = ""
                            UpdateChecker.showUpdateDialog(requireActivity() as AppCompatActivity, info)
                        }
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
