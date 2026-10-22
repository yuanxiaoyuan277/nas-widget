package com.yxy.naswidget

import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * 布局截图测试：用 Paparazzi 在 JVM 里渲染关键布局，输出 PNG 供人工审查。
 * 在 GitHub Actions 云端跑（recordPaparazziDebug），截图回传仓库后下载查看。
 */
class LayoutSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "Theme.NasWidget"
    )

    @Test
    fun settingsLight() {
        val view: View = paparazzi.inflate(R.layout.fragment_settings)
        // 填充动态内容，让截图接近真实
        val spinner = view.findViewById<Spinner>(R.id.sp_refresh)
        spinner.adapter = ArrayAdapter(
            paparazzi.context,
            R.layout.spinner_item,
            listOf("10 秒", "15 秒", "30 秒", "60 秒", "5 分钟")
        )
        spinner.setSelection(2)
        view.findViewById<EditText>(R.id.et_font_size).setText("16")
        paparazzi.snapshot(view, "settings-light")
    }

    @Test
    fun homeLight() {
        val view: View = paparazzi.inflate(R.layout.fragment_home)
        view.findViewById<TextView>(R.id.tv_sys_cpu).text = "Intel Core i5-8265U @ 1.60GHz"
        view.findViewById<TextView>(R.id.tv_sys_os).text = "fnOS 1.2.0800"
        view.findViewById<TextView>(R.id.tv_sys_uptime).text = "12天3时"
        paparazzi.snapshot(view, "home-light")
    }

    @Test
    fun updateDialogLight() {
        val view: View = paparazzi.inflate(R.layout.dialog_update)
        view.findViewById<TextView>(R.id.tv_update_version).text = "v1.11.19"
        view.findViewById<TextView>(R.id.tv_update_notes).text = "•  刷新间隔下拉框加箭头指示\n•  修复切换主题后首次切主页动画"
        paparazzi.snapshot(view, "update-dialog-light")
    }
}
