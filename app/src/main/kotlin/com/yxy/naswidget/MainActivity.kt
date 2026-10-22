package com.yxy.naswidget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private var currentTab = R.id.nav_home
    private var homeFragment: HomeFragment? = null
    private var settingsFragment: SettingsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(Prefs.nightMode(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)

        // 恢复缓存的 Fragment 实例（recreate 后 Fragment 管理器已恢复它们）
        homeFragment = supportFragmentManager.findFragmentByTag("home") as? HomeFragment
        settingsFragment = supportFragmentManager.findFragmentByTag("settings") as? SettingsFragment

        if (savedInstanceState == null) {
            // 首次启动：add 主页
            homeFragment = HomeFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment!!, "home")
                .commit()
            currentTab = R.id.nav_home
        } else {
            // recreate（主题切换等）：恢复之前的 tab
            currentTab = savedInstanceState.getInt("state_current_tab", R.id.nav_home)
        }

        bottomNav.selectedItemId = currentTab

        bottomNav.setOnItemSelectedListener { item ->
            val target = item.itemId
            if (target == currentTab) {
                return@setOnItemSelectedListener true
            }
            switchFragment(target)
            currentTab = target
            true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("state_current_tab", currentTab)
    }

    /** show/hide 复用实例（不重 inflate），手动做 enter 滑入 + exit 滑出动画 */
    private fun switchFragment(tabId: Int) {
        val home = homeFragment ?: HomeFragment().also { homeFragment = it }
        val settings = settingsFragment ?: SettingsFragment().also { settingsFragment = it }
        val goingRight = tabId == R.id.nav_settings
        val enterFrag = if (tabId == R.id.nav_home) home else settings
        val exitFrag = if (tabId == R.id.nav_home) settings else home

        // 首次 add（同步，确保 view 准备好）
        if (!enterFrag.isAdded) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, enterFrag, if (tabId == R.id.nav_home) "home" else "settings")
                .commitNow()
        }

        val enterView = enterFrag.view ?: run {
            supportFragmentManager.beginTransaction().hide(exitFrag).show(enterFrag).commit()
            return
        }
        val exitView = exitFrag.view

        // show enter（同步，确保 view 立即 VISIBLE，避免 hidden 状态下宽度为 0 导致动画失效）
        supportFragmentManager.beginTransaction().show(enterFrag).commitNow()

        // exit 滑出：用 exitView 自身宽度（exit 已 layout，宽度正常）
        exitView?.let { v ->
            val exitTo = if (goingRight) -v.width.toFloat() else v.width.toFloat()
            v.animate()
                .translationX(exitTo)
                .setDuration(650)
                .withEndAction { supportFragmentManager.beginTransaction().hide(exitFrag).commit() }
                .start()
        }

        // enter 滑入：post 到 layout 完成后执行，避免首次 add 时 width 还是 0
        enterView.post {
            val from = if (goingRight) enterView.width.toFloat() else -enterView.width.toFloat()
            enterView.translationX = from
            enterView.animate()
                .translationX(0f)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }
}
