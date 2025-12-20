package com.euktop.studentalarm

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.euktop.studentalarm.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var frameNameTextView: android.widget.TextView
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomNavigationView: BottomNavigationView

    private var isBottomNavAnimating = false
    private var isBottomNavVisible = true

    // Для запроса разрешения на уведомления (API 33+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Не показываем сообщение о результате
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Проверяем разрешения при запуске приложения
        checkPermissionsOnStartup()

        // Настройка обработки системных окон
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Настройка обратного вызова для жеста "Назад"
        setupBackPressedHandler()

        frameNameTextView = findViewById(R.id.FrameNameTextView)
        bottomNavigationView = binding.bottomNavigation

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val menu = bottomNavigationView.menu
        val menuOrder = mutableListOf<Int>()
        for (i in 0 until menu.size()) {
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

    override fun onResume() {
        super.onResume()
        // При возвращении в приложение проверяем разрешения и отключаем будильники при необходимости
        checkPermissionsAndDisableAlarms()
    }

    private fun checkPermissionsOnStartup() {
        // Показываем диалог со всеми необходимыми разрешениями
        if (!PermissionManager.hasAllAlarmPermissions(this)) {
            PermissionManager.showAllPermissionsDialog(this)
        }

        // Дополнительно запрашиваем разрешение на уведомления для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionManager.hasNotificationPermission(this)) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkPermissionsAndDisableAlarms() {
        // Проверяем, есть ли все необходимые разрешения
        if (!PermissionManager.hasAllAlarmPermissions(this)) {
            // Нет разрешений - отключаем все будильники
            disableAllAlarms()
        }
    }

    private fun disableAllAlarms() {
        lifecycleScope.launch {
            val app = application as AlarmApplication
            val alarms = app.alarmRepository.getAllAlarms().first()

            // Проверяем, есть ли вообще будильники
            if (alarms.isEmpty()) {
                // Нет будильников - ничего не делаем и не показываем сообщение
                return@launch
            }

            var disabledCount = 0

            alarms.forEach { alarm ->
                if (alarm.isEnabled) {
                    // Отключаем будильник в базе данных
                    app.alarmRepository.updateAlarm(alarm.copy(isEnabled = false))
                    // Отменяем запланированный будильник
                    AlarmScheduler.cancelAlarm(this@MainActivity, alarm.id)
                    disabledCount++
                }
            }

            if (disabledCount > 0) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Все будильники отключены из-за отсутствия необходимых разрешений",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PermissionManager.OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (PermissionManager.hasOverlayPermission(this)) {
                    Toast.makeText(this, "Разрешение предоставлено", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Не показываем сообщения о результате запроса разрешений
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
                    binding.bottomNavContainer.visibility = android.view.View.VISIBLE
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
                    binding.bottomNavContainer.visibility = android.view.View.GONE
                    isBottomNavAnimating = false
                    isBottomNavVisible = false
                }
            )
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentDestination = navController.currentDestination?.id

                when (currentDestination) {
                    R.id.alarmsFragment, R.id.timerFragment -> {
                        finish()
                    }
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })
    }

    // Метод для проверки разрешений перед выполнением действия
    fun checkAlarmPermissionsAndExecute(action: () -> Unit) {
        PermissionManager.checkAllPermissionsBeforeAction(this, action)
    }
}