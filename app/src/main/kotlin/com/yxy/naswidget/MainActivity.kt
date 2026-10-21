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
        AppCompatDelegate.setDefaultNightMode(
            if (Prefs.isDarkTheme(this)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav.selectedItemId = R.id.nav_home
        currentTab = R.id.nav_home

        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment!!, "home")
                .commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
            val target = item.itemId
            // 已在这个 tab，不重复切换
            if (target == currentTab) {
                return@setOnItemSelectedListener true
            }
            switchFragment(target)
            currentTab = target
            true
        }
    }

    /** 用 show/hide 复用 Fragment 实例，避免每次切换重新 inflate 布局导致卡顿 */
    private fun switchFragment(tabId: Int) {
        val home = homeFragment ?: HomeFragment().also { homeFragment = it }
        val settings = settingsFragment ?: SettingsFragment().also { settingsFragment = it }

        val goingRight = tabId == R.id.nav_settings
        val enter = if (goingRight) R.anim.slide_in_right else R.anim.slide_in_left
        val exit = if (goingRight) R.anim.slide_out_left else R.anim.slide_out_right

        val tx = supportFragmentManager.beginTransaction()
        if (tabId == R.id.nav_home) {
            // 主页：确保 home 已 add，隐藏 settings，显示 home
            if (!home.isAdded) tx.add(R.id.fragment_container, home, "home")
            if (settings.isAdded) tx.hide(settings)
            tx.show(home)
        } else {
            // 设置：确保 settings 已 add，隐藏 home，显示 settings
            if (!settings.isAdded) tx.add(R.id.fragment_container, settings, "settings")
            tx.hide(home)
            if (settings.isAdded) tx.show(settings)
        }
        tx.setCustomAnimations(enter, exit, enter, exit)
        tx.commit()
    }
}
