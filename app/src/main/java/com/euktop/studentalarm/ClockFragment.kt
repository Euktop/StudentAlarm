// app/src/main/java/com/euktop/studentalarm/ClockFragment.kt
package com.euktop.studentalarm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.euktop.studentalarm.databinding.FragmentClockBinding
import com.euktop.studentalarm.viewmodel.ClockViewModel
import com.euktop.studentalarm.viewmodel.WeatherViewModel
import com.euktop.studentalarm.viewmodel.ViewModelFactory
import com.euktop.studentalarm.viewmodel.WeatherState
import com.euktop.studentalarm.weather.WeatherIconStorage
import kotlinx.coroutines.launch

class ClockFragment : Fragment() {

    private var _binding: FragmentClockBinding? = null
    private val binding get() = _binding!!

    private lateinit var clockViewModel: ClockViewModel
    private lateinit var weatherViewModel: WeatherViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModels()
        setupObservers()
        setupClickListeners()

        // Запускаем часы
        clockViewModel.startClock()

        // Загружаем погоду
        weatherViewModel.loadWeather()
    }

    private fun initViewModels() {
        // Для ClockViewModel не нужны параметры
        clockViewModel = ViewModelProvider(this)[ClockViewModel::class.java]

        // Для WeatherViewModel нужна фабрика
        val app = requireActivity().application as AlarmApplication
        val factory = ViewModelFactory(app.alarmRepository, requireContext())
        weatherViewModel = ViewModelProvider(this, factory)[WeatherViewModel::class.java]
    }

    private fun setupObservers() {
        // Наблюдаем за часами
        clockViewModel.currentTime.observe(viewLifecycleOwner) { time ->
            binding.tvTime.text = time
        }

        clockViewModel.currentDate.observe(viewLifecycleOwner) { date ->
            binding.tvDate.text = date
        }

        clockViewModel.currentDay.observe(viewLifecycleOwner) { day ->
            binding.tvDayOfWeek.text = day
        }

        clockViewModel.milliseconds.observe(viewLifecycleOwner) { millis ->
            // Можно добавить миллисекунды если нужно
            // binding.tvMilliseconds.text = millis
        }

        // Наблюдаем за погодой
        weatherViewModel.weatherState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is WeatherState.Loading -> {
                    showWeatherLoading()
                }
                is WeatherState.Success -> {
                    hideWeatherLoading()
                    displayWeather(state.weather)
                }
                is WeatherState.Error -> {
                    hideWeatherLoading()
                    showWeatherError(state.message)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnRefreshWeather.setOnClickListener {
            weatherViewModel.refreshWeather()
        }
    }

    private fun showWeatherLoading() {
        binding.weatherProgressBar.visibility = View.VISIBLE
        binding.weatherCard.alpha = 0.6f
    }

    private fun hideWeatherLoading() {
        binding.weatherProgressBar.visibility = View.GONE
        binding.weatherCard.alpha = 1.0f
    }

    private fun displayWeather(weather: com.euktop.studentalarm.data.repository.WeatherUI) {
        binding.tvWeatherCity.text = weather.city
        binding.tvWeatherTemp.text = weather.temperature
        binding.tvWeatherDescription.text = weather.description

        if (weather.lastUpdate.isNotEmpty()) {
            binding.tvLastUpdate.text =
                requireContext().getString(R.string.weather_last_updated, weather.lastUpdate)
        } else {
            binding.tvLastUpdate.text = ""
        }

        // Загружаем иконку через Glide
        loadWeatherIconWithGlide(weather.iconCode)
    }

    private fun loadWeatherIconWithGlide(iconCode: String) {
        if (iconCode.isNotEmpty()) {
            val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
            Glide.with(this)
                .load(iconUrl)
                .into(binding.ivWeatherIcon)
        }
    }

    private fun showWeatherError(message: String) {
        binding.tvWeatherDescription.text = message
        binding.tvWeatherDescription.setTextColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.error)
        )

        // Показываем последние кешированные данные, если есть
        weatherViewModel.weatherState.value?.let { state ->
            if (state is WeatherState.Success) {
                displayWeather(state.weather)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        clockViewModel.stopClock()
    }

    override fun onResume() {
        super.onResume()
        clockViewModel.startClock()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}