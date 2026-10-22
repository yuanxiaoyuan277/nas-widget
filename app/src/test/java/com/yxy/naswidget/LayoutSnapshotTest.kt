package com.yxy.naswidget

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * 布局截图测试：用 Paparazzi 在 JVM 里渲染关键布局，输出 PNG 供人工审查。
 * 在 GitHub Actions 云端跑（recordPaparazziDebug），截图上传 artifact 后下载查看。
 */
class LayoutSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "com.yxy.naswidget:Theme.NasWidget"
    )

    @Test
    fun settingsLight() {
        paparazzi.snapshot(paparazzi.inflate(R.layout.fragment_settings), "settings-light")
    }

    @Test
    fun homeLight() {
        paparazzi.snapshot(paparazzi.inflate(R.layout.fragment_home), "home-light")
    }

    @Test
    fun updateDialogLight() {
        paparazzi.snapshot(paparazzi.inflate(R.layout.dialog_update), "update-dialog-light")
    }
}
