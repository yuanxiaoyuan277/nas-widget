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
import androidx.fragment.app.Fragment
import java.io.File

/** 设置页：NAS 地址、wb2api 积分、外观（深色浅色）、检查更新 */
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    companion object {
        private const val REQ_LOGIN = 1001
    }

    private val refreshOptions = listOf(10 to "10 秒", 15 to "15 秒", 30 to "30 秒", 60 to "60 秒", 300 to "5 分钟")
    private lateinit var spRefresh: Spinner
    private lateinit var etFontSize: EditText
    private lateinit var scrollRoot: ScrollView
    private lateinit var tvLoginStatus: TextView
    private lateinit var btnLogin: Button
    private lateinit var etUrl: EditText
    private lateinit var btnTestUrl: Button
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
    private lateinit var tvAboutVersion: TextView
    private lateinit var tvAboutRepo: TextView
    private lateinit var cbAlert: CheckBox
    private lateinit var etAlertUsage: EditText
    private lateinit var etAlertCpuTemp: EditText
    private lateinit var etAlertNvmeTemp: EditText
    private var suppressSave = false
    private val saveHandler = Handler(Looper.getMainLooper())
    private val saveRunnable = Runnable { persist() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scrollRoot = view.findViewById(R.id.settings_scroll)
        spRefresh = view.findViewById(R.id.sp_refresh)
        etFontSize = view.findViewById(R.id.et_font_size)
        tvLoginStatus = view.findViewById(R.id.tv_login_status)
        btnLogin = view.findViewById(R.id.btn_login)
        etUrl = view.findViewById(R.id.et_url)
        btnTestUrl = view.findViewById(R.id.btn_test_url)
        cbWb2api = view.findViewById(R.id.cb_wb2api)
        etWb2apiBase = view.findViewById(R.id.et_wb2api_base)
        etWb2apiKey = view.findViewById(R.id.et_wb2api_key)
        btnToggleTheme = view.findViewById(R.id.btn_toggle_theme)
        tvVersion = view.findViewById(R.id.tv_version)
        btnCheckUpdate = view.findViewById(R.id.btn_check_update)
        tvUpdateStatus = view.findViewById(R.id.tv_update_status)
        btnBackup = view.findViewById(R.id.btn_backup)
        btnRestore = view.findViewById(R.id.btn_restore)
        tvBackupStatus = view.findViewById(R.id.tv_backup_status)
        tvAboutVersion = view.findViewById(R.id.tv_about_version)
        tvAboutRepo = view.findViewById(R.id.tv_about_repo)
        cbAlert = view.findViewById(R.id.cb_alert)
        etAlertUsage = view.findViewById(R.id.et_alert_usage)
        etAlertCpuTemp = view.findViewById(R.id.et_alert_cpu_temp)
        etAlertNvmeTemp = view.findViewById(R.id.et_alert_nvme_temp)

        btnLogin.setOnClickListener {
            requireActivity().startActivityForResult(Intent(requireContext(), LoginActivity::class.java), REQ_LOGIN)
        }
        btnTestUrl.setOnClickListener { testUrl() }
        btnToggleTheme.setOnClickListener { toggleTheme() }
        btnCheckUpdate.setOnClickListener { checkUpdate() }
        btnBackup.setOnClickListener { backupConfig() }
        btnRestore.setOnClickListener { restoreConfig() }
        tvAboutRepo.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/yuanxiaoyuan277/nas-widget")))
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "无法打开浏览器", Toast.LENGTH_SHORT).show()
            }
        }
        cbWb2api.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        cbAlert.setOnCheckedChangeListener { _, _ -> if (!suppressSave) scheduleSave() }
        watchEditText(etUrl)
        watchEditText(etWb2apiBase)
        watchEditText(etWb2apiKey)
        watchEditText(etAlertUsage)
        watchEditText(etAlertCpuTemp)
        watchEditText(etAlertNvmeTemp)
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, refreshOptions.map { it.second })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRefresh.adapter = adapter
        spRefresh.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
        val idx = refreshOptions.indexOfFirst { it.first == cur }
        spRefresh.setSelection(if (idx >= 0) idx else 2)
        etFontSize.setText(Prefs.fontSize(requireContext()).toString())
        cbWb2api.isChecked = Prefs.showWb2api(requireContext())
        cbAlert.isChecked = Prefs.showAlert(requireContext())
        etAlertUsage.setText(Prefs.alertUsageThreshold(requireContext()).toString())
        etAlertCpuTemp.setText(Prefs.alertCpuTempThreshold(requireContext()).toString())
        etAlertNvmeTemp.setText(Prefs.alertNvmeTempThreshold(requireContext()).toString())
        etWb2apiBase.setText(Prefs.wb2apiBase(requireContext()))
        etWb2apiKey.setText(Prefs.wb2apiKey(requireContext()))
        tvVersion.text = "v${UpdateChecker.localVersion(requireContext())}"
        tvAboutVersion.text = "v${UpdateChecker.localVersion(requireContext())}"
        updateThemeButton()
        suppressSave = false
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
        Prefs.setAlertUsageThreshold(requireContext(), etAlertUsage.text.toString().toIntOrNull()?.coerceIn(1, 100) ?: 90)
        Prefs.setAlertCpuTempThreshold(requireContext(), etAlertCpuTemp.text.toString().toIntOrNull()?.coerceIn(1, 120) ?: 85)
        Prefs.setAlertNvmeTempThreshold(requireContext(), etAlertNvmeTemp.text.toString().toIntOrNull()?.coerceIn(1, 120) ?: 70)
        val sel = spRefresh.selectedItemPosition
        Prefs.setRefresh(requireContext(), refreshOptions.getOrElse(sel) { 30 to "30 秒" }.first)
        val fs = etFontSize.text.toString().toIntOrNull()?.coerceIn(8, 200) ?: 16
        Prefs.setFontSize(requireContext(), fs)
        Prefs.setWb2apiBase(requireContext(), etWb2apiBase.text.toString().trim().ifEmpty { Prefs.DEFAULT_WB2API_BASE })
        Prefs.setWb2apiKey(requireContext(), etWb2apiKey.text.toString().trim().ifEmpty { Prefs.DEFAULT_WB2API_KEY })

        val am = AppWidgetManager.getInstance(requireContext())
        val ids = am.getAppWidgetIds(ComponentName(requireContext(), NasWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            NasWidgetProvider.updateAll(requireContext(), am, ids)
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

    private fun checkUpdate() {
        btnCheckUpdate.isEnabled = false
        tvUpdateStatus.text = "检查更新中…"
        Thread {
            val result = UpdateChecker.checkLatest()
            requireActivity().runOnUiThread {
                btnCheckUpdate.isEnabled = true
                result.onSuccess { info ->
                    val local = UpdateChecker.localVersion(requireContext())
                    if (UpdateChecker.isNewer(info.version, local) &&
                        !UpdateChecker.isIgnored(requireContext(), info.version)
                    ) {
                        tvUpdateStatus.text = ""
                        UpdateChecker.showUpdateDialog(requireActivity() as AppCompatActivity, info)
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
