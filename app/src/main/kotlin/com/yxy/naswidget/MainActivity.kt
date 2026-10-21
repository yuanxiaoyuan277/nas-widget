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
            if (target == currentTab) {
                return@setOnItemSelectedListener true
            }
            switchFragment(target)
            currentTab = target
            true
        }
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
                .add(R.id.fragment_container, enterFrag)
                .commitNow()
        }

        val enterView = enterFrag.view ?: run {
            supportFragmentManager.beginTransaction().hide(exitFrag).show(enterFrag).commit()
            return
        }
        val exitView = exitFrag.view

        val w = enterView.width.toFloat()
        val enterFrom = if (goingRight) w else -w
        val exitTo = if (goingRight) -w else w

        // show enter（立即），hide exit（等 exit 滑出动画结束）
        supportFragmentManager.beginTransaction().show(enterFrag).commit()

        exitView?.animate()
            ?.translationX(exitTo)
            ?.setDuration(650)
            ?.withEndAction { supportFragmentManager.beginTransaction().hide(exitFrag).commit() }
            ?.start()

        enterView.translationX = enterFrom
        enterView.animate()
            .translationX(0f)
            .setDuration(650)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }
}
