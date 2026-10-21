package com.yxy.naswidget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private var currentTab = R.id.nav_home

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
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
            val target = item.itemId
            // 已在这个 tab，不重复切换（避免重复动画）
            if (target == currentTab) {
                return@setOnItemSelectedListener true
            }
            val fragment: Fragment = when (target) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HomeFragment()
            }
            // 主页→设置 向右滑，设置→主页 向左滑
            val goingRight = target == R.id.nav_settings
            val enter = if (goingRight) R.anim.slide_in_right else R.anim.slide_in_left
            val exit = if (goingRight) R.anim.slide_out_left else R.anim.slide_out_right
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(enter, exit, enter, exit)
                .replace(R.id.fragment_container, fragment)
                .commit()
            currentTab = target
            true
        }
    }
}
