package com.euktop.studentalarm

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import android.widget.TextView
import com.euktop.studentalarm.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.view.get
import androidx.core.view.size

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var frameNameTextView: TextView
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomNavigationView: BottomNavigationView

    private var isBottomNavAnimating = false
    private var isBottomNavVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        frameNameTextView = findViewById(R.id.FrameNameTextView)
        bottomNavigationView = binding.bottomNavigation

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val menu = bottomNavigationView.menu
        val menuOrder = mutableListOf<Int>()
        for (i in 0 until menu.size) {
            menuOrder.add(menu[i].itemId)
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            val currentDestId = navController.currentDestination?.id
            val targetDestId = item.itemId

            if (currentDestId != targetDestId) {
                val currentIndex = menuOrder.indexOf(currentDestId)
                val targetIndex = menuOrder.indexOf(targetDestId)

                val navOptions = if (targetIndex > currentIndex) {
                    NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setEnterAnim(R.anim.slide_in_right)
                        .setExitAnim(R.anim.slide_out_left)
                        .setPopEnterAnim(R.anim.slide_in_left)
                        .setPopExitAnim(R.anim.slide_out_right)
                        .build()
                } else {
                    NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setEnterAnim(R.anim.slide_in_left)
                        .setExitAnim(R.anim.slide_out_right)
                        .setPopEnterAnim(R.anim.slide_in_right)
                        .setPopExitAnim(R.anim.slide_out_left)
                        .build()
                }

                try {
                    navController.navigate(targetDestId, null, navOptions)
                } catch (e: IllegalArgumentException) {
                    navController.navigate(targetDestId, null, navOptions)
                }
            }
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.alarmsFragment -> {
                    frameNameTextView.text = "Будильники"
                    bottomNavigationView.selectedItemId = R.id.alarmsFragment
                    if (!isBottomNavVisible) {
                        animateBottomNavigationContainer(true)
                    }
                }
                R.id.timerFragment -> {
                    frameNameTextView.text = "Таймер"
                    bottomNavigationView.selectedItemId = R.id.timerFragment
                    if (!isBottomNavVisible) {
                        animateBottomNavigationContainer(true)
                    }
                }
                R.id.alarmEditFragment -> {
                    frameNameTextView.text = "Редактирование"
                    if (isBottomNavVisible) {
                        animateBottomNavigationContainer(false)
                    }
                }
                else -> {
                    if (!isBottomNavVisible) {
                        animateBottomNavigationContainer(true)
                    }
                }
            }
        }
    }

    private fun animateBottomNavigationContainer(show: Boolean) {
        if (isBottomNavAnimating) return

        isBottomNavAnimating = true

        val duration = resources.getInteger(R.integer.anim_duration_short).toLong()

        if (show) {
            AnimatorHelper.slideViewFromDirection(
                view = binding.bottomNavContainer,
                direction = AnimatorHelper.Direction.UP,
                duration = duration,
                onStart = {
                    binding.bottomNavContainer.visibility = View.VISIBLE
                },
                onEnd = {
                    isBottomNavAnimating = false
                    isBottomNavVisible = true
                }
            )
        } else {
            AnimatorHelper.slideViewToDirection(
                view = binding.bottomNavContainer,
                direction = AnimatorHelper.Direction.DOWN,
                duration = duration,
                onStart = {
                },
                onEnd = {
                    binding.bottomNavContainer.visibility = View.GONE
                    isBottomNavAnimating = false
                    isBottomNavVisible = false
                }
            )
        }
    }

    override fun onBackPressed() {
        val currentDestination = navController.currentDestination?.id

        when (currentDestination) {
            R.id.alarmsFragment, R.id.timerFragment -> {
                finish()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
}