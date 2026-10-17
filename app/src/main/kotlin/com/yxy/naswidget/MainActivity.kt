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
import androidx.appcompat.app.AppCompatActivity

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

        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
        }

        // 刷新间隔下拉
        val labels = refreshOptions.map { it.second }
        spRefresh.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)

        loadPrefs()

        findViewById<Button>(R.id.btn_save).setOnClickListener { save() }
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

        // 立即更新 + 重设定时
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
}
