package com.euktop.studentalarm

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.euktop.studentalarm.weather.WeatherIconStorage
import com.euktop.studentalarm.weather.WeatherResponse
import com.euktop.studentalarm.weather.WeatherRetrofitClient
import com.google.android.gms.location.*
import kotlinx.coroutines.*

class ClockFragment : Fragment() {

    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvDayOfWeek: TextView
    private lateinit var tvWeatherCity: TextView
    private lateinit var tvWeatherTemp: TextView
    private lateinit var tvWeatherDescription: TextView
    private lateinit var tvLastUpdate: TextView
    private lateinit var weatherProgressBar: ProgressBar
    private lateinit var weatherCard: View
    private lateinit var btnRefreshWeather: Button
    private lateinit var ivWeatherIcon: android.widget.ImageView

    private val clockHandler = Handler(Looper.getMainLooper())
    private var isClockUpdating = false
    private var lastClockUpdateTime = 0L

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var lastLatitude: Double = 0.0
    private var lastLongitude: Double = 0.0

    private val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    private val dateFormat = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault())
    private val dayFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
    private val millisFormat = java.text.SimpleDateFormat("SSS", java.util.Locale.getDefault())
    private val updateTimeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

    private var cachedWeatherData: WeatherResponse? = null
    private var lastWeatherUpdateTime: Long = 0
    private var cachedLocationName: String? = null
    private var lastErrorMessage: String = ""
    private var hasRequestedPermission = false
    private var wasPermissionDeniedForever = false

    private lateinit var sharedPreferences: SharedPreferences

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasRequestedPermission = true

        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            wasPermissionDeniedForever = false
            attemptWeatherRefresh()
        } else {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) &&
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                wasPermissionDeniedForever = true
                lastErrorMessage = requireContext().getString(R.string.location_permission_denied_forever)
            } else {
                lastErrorMessage = requireContext().getString(R.string.location_permission_denied)
            }
            showWeatherError(lastErrorMessage)
            displayCachedWeather()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ClockFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_clock, container, false)

        tvTime = view.findViewById(R.id.tvTime)
        tvDate = view.findViewById(R.id.tvDate)
        tvDayOfWeek = view.findViewById(R.id.tvDayOfWeek)

        tvWeatherCity = view.findViewById(R.id.tvWeatherCity)
        tvWeatherTemp = view.findViewById(R.id.tvWeatherTemp)
        tvWeatherDescription = view.findViewById(R.id.tvWeatherDescription)
        tvLastUpdate = view.findViewById(R.id.tvLastUpdate)
        weatherProgressBar = view.findViewById(R.id.weatherProgressBar)
        weatherCard = view.findViewById(R.id.weatherCard)
        btnRefreshWeather = view.findViewById(R.id.btnRefreshWeather)
        ivWeatherIcon = view.findViewById(R.id.ivWeatherIcon)

        btnRefreshWeather.setOnClickListener {
            attemptWeatherRefresh()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    lastLatitude = location.latitude
                    lastLongitude = location.longitude

                    if (isNetworkAvailable()) {
                        fetchWeather(location.latitude, location.longitude)
                    } else {
                        lastErrorMessage = requireContext().getString(R.string.no_internet)
                        showWeatherError(lastErrorMessage)
                        hideWeatherProgress()
                        displayCachedWeather()
                    }
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    hideWeatherProgress()
                    lastErrorMessage = requireContext().getString(R.string.location_not_determined)
                    showWeatherError(lastErrorMessage)
                    displayCachedWeather()
                }
            }
        }

        sharedPreferences = requireContext().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        loadWeatherFromStorage()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startClockUpdates()
        displayCachedWeather()
    }

    private fun attemptWeatherRefresh() {
        if (wasPermissionDeniedForever) {
            lastErrorMessage = requireContext().getString(R.string.location_permission_denied_forever)
            showWeatherError(lastErrorMessage)
            displayCachedWeather()
            return
        }

        if (!hasLocationPermissions()) {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
            return
        }

        if (!isNetworkAvailable()) {
            lastErrorMessage = requireContext().getString(R.string.no_internet)
            showWeatherError(lastErrorMessage)
            displayCachedWeather()
            return
        }

        showWeatherProgress()
        requestWeather()
    }

    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            networkInfo.isConnected
        }
    }

    private suspend fun getExactLocationName(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), java.util.Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                addresses?.firstOrNull()?.let { address ->
                    when {
                        address.locality != null -> address.locality
                        address.subLocality != null -> address.subLocality
                        address.featureName != null -> address.featureName
                        address.adminArea != null -> address.adminArea
                        else -> null
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun requestWeather() {
        if (lastLatitude != 0.0 && lastLongitude != 0.0) {
            fetchWeather(lastLatitude, lastLongitude)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    lastLatitude = location.latitude
                    lastLongitude = location.longitude
                    fetchWeather(location.latitude, location.longitude)
                } else {
                    requestLocationUpdate()
                }
            }
            .addOnFailureListener {
                requestLocationUpdate()
            }
    }

    private fun requestLocationUpdate() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000
        ).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()

        if (!hasLocationPermissions()) {
            hideWeatherProgress()
            lastErrorMessage = requireContext().getString(R.string.location_permission_denied)
            showWeatherError(lastErrorMessage)
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun fetchWeather(latitude: Double, longitude: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = WeatherRetrofitClient.weatherApiService
                    .getWeatherByCoordinates(latitude, longitude)

                val response = call.execute()

                if (response.isSuccessful && response.body() != null) {
                    val weatherData = response.body()!!

                    cachedWeatherData = weatherData
                    lastWeatherUpdateTime = System.currentTimeMillis()
                    lastErrorMessage = ""

                    val locationName = getExactLocationName(latitude, longitude)
                    cachedLocationName = locationName

                    // Сохраняем иконку погоды в кэш
                    weatherData.weather.firstOrNull()?.icon?.let { iconCode ->
                        WeatherIconStorage.loadOrDownloadIcon(requireContext(), iconCode)
                    }

                    saveWeatherToStorage()

                    withContext(Dispatchers.Main) {
                        displayWeather(weatherData, locationName)
                    }

                } else {
                    val errorMessage = when (response.code()) {
                        401 -> requireContext().getString(R.string.weather_api_key_invalid)
                        404 -> requireContext().getString(R.string.weather_location_not_found)
                        429 -> requireContext().getString(R.string.weather_too_many_requests)
                        500 -> requireContext().getString(R.string.weather_server_error)
                        else -> "${requireContext().getString(R.string.error)}: ${response.code()}"
                    }

                    lastErrorMessage = errorMessage

                    withContext(Dispatchers.Main) {
                        showWeatherError(errorMessage)
                        hideWeatherProgress()
                        displayCachedWeather()
                    }
                }
            } catch (e: Exception) {
                lastErrorMessage = "${requireContext().getString(R.string.weather_network_error)}: ${e.message}"

                withContext(Dispatchers.Main) {
                    showWeatherError(lastErrorMessage)
                    hideWeatherProgress()
                    displayCachedWeather()
                }
            }
        }
    }

    private fun displayWeather(weatherData: WeatherResponse, locationName: String?) {
        hideWeatherProgress()

        val displayCity = when {
            locationName != null -> locationName
            weatherData.cityName.isNotEmpty() -> weatherData.cityName
            else -> requireContext().getString(R.string.city_undefined)
        }

        tvWeatherCity.text = displayCity
        tvWeatherTemp.text = "${weatherData.main.temperature.toInt()}°C"
        tvWeatherDescription.text = weatherData.weather.firstOrNull()?.description
            ?.replaceFirstChar { it.uppercase() } ?: ""

        tvWeatherDescription.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

        if (lastWeatherUpdateTime > 0) {
            val time = updateTimeFormat.format(java.util.Date(lastWeatherUpdateTime))
            tvLastUpdate.text = requireContext().getString(R.string.weather_last_updated, time)
        } else {
            tvLastUpdate.text = ""
        }

        // Загружаем иконку из кэша или сети
        weatherData.weather.firstOrNull()?.icon?.let { iconCode ->
            loadWeatherIcon(iconCode)
        }
    }

    private fun displayCachedWeather() {
        cachedWeatherData?.let { weatherData ->
            val displayCity = when {
                cachedLocationName != null -> cachedLocationName!!
                weatherData.cityName.isNotEmpty() -> weatherData.cityName
                lastErrorMessage.isNotEmpty() -> "${requireContext().getString(R.string.city_undefined)} ($lastErrorMessage)"
                else -> requireContext().getString(R.string.city_undefined)
            }

            tvWeatherCity.text = displayCity
            tvWeatherTemp.text = "${weatherData.main.temperature.toInt()}°C"
            tvWeatherDescription.text = weatherData.weather.firstOrNull()?.description
                ?.replaceFirstChar { it.uppercase() } ?: ""

            tvWeatherDescription.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

            if (lastWeatherUpdateTime > 0) {
                val time = updateTimeFormat.format(java.util.Date(lastWeatherUpdateTime))
                tvLastUpdate.text = requireContext().getString(R.string.weather_last_updated, time)
            } else {
                tvLastUpdate.text = ""
            }

            // Загружаем сохраненную иконку
            weatherData.weather.firstOrNull()?.icon?.let { iconCode ->
                loadWeatherIcon(iconCode)
            }
        } ?: run {
            tvWeatherCity.text = if (lastErrorMessage.isNotEmpty()) {
                "${requireContext().getString(R.string.city_undefined)} ($lastErrorMessage)"
            } else {
                requireContext().getString(R.string.city_undefined)
            }
            tvWeatherTemp.text = "--°C"
            tvWeatherDescription.text = ""
            tvLastUpdate.text = ""

            // Очищаем иконку
            ivWeatherIcon.setImageDrawable(null)
        }
    }

    private fun loadWeatherIcon(iconCode: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = WeatherIconStorage.loadIcon(requireContext(), iconCode)
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        ivWeatherIcon.setImageBitmap(bitmap)
                    } else {
                        // Если нет в кэше, загружаем через Glide
                        val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
                        Glide.with(this@ClockFragment)
                            .load(iconUrl)
                            .into(ivWeatherIcon)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
                    Glide.with(this@ClockFragment)
                        .load(iconUrl)
                        .into(ivWeatherIcon)
                }
            }
        }
    }

    private fun showWeatherError(message: String) {
        tvWeatherDescription.text = message
        tvWeatherDescription.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
    }

    private fun showWeatherProgress() {
        weatherProgressBar.visibility = View.VISIBLE
        weatherCard.alpha = 0.6f
    }

    private fun hideWeatherProgress() {
        weatherProgressBar.visibility = View.GONE
        weatherCard.alpha = 1.0f
    }

    private fun loadWeatherFromStorage() {
        lastLatitude = sharedPreferences.getFloat("last_lat", 0f).toDouble()
        lastLongitude = sharedPreferences.getFloat("last_lon", 0f).toDouble()
        lastWeatherUpdateTime = sharedPreferences.getLong("last_update", 0)
        cachedLocationName = sharedPreferences.getString("location_name", null)

        val city = sharedPreferences.getString("weather_city", null)
        val temp = sharedPreferences.getFloat("weather_temp", -1000f)
        val humidity = sharedPreferences.getInt("weather_humidity", 0)
        val desc = sharedPreferences.getString("weather_desc", "")
        val icon = sharedPreferences.getString("weather_icon", "")

        if (city != null && temp > -1000f) {
            cachedWeatherData = WeatherResponse(
                cityName = city,
                main = com.euktop.studentalarm.weather.Main(
                    temperature = temp.toDouble(),
                    humidity = humidity,
                    feelsLike = 0.0,
                    pressure = 1013
                ),
                weather = listOf(com.euktop.studentalarm.weather.Weather(desc ?: "", icon ?: ""))
            )
        }
    }

    private fun saveWeatherToStorage() {
        sharedPreferences.edit().apply {
            putFloat("last_lat", lastLatitude.toFloat())
            putFloat("last_lon", lastLongitude.toFloat())
            putLong("last_update", lastWeatherUpdateTime)
            cachedLocationName?.let { putString("location_name", it) }
            cachedWeatherData?.let { weather ->
                putString("weather_city", weather.cityName)
                putFloat("weather_temp", weather.main.temperature.toFloat())
                putInt("weather_humidity", weather.main.humidity)
                weather.weather.firstOrNull()?.let { w ->
                    putString("weather_desc", w.description)
                    putString("weather_icon", w.icon)
                }
            }
            apply()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isClockUpdating) {
            startClockUpdates()
        }
        loadWeatherFromStorage()
        displayCachedWeather()

        wasPermissionDeniedForever = !hasLocationPermissions() &&
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) &&
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    override fun onPause() {
        super.onPause()
        stopClockUpdates()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun startClockUpdates() {
        if (isClockUpdating) return
        isClockUpdating = true
        lastClockUpdateTime = System.currentTimeMillis()

        val updateRunnable = object : Runnable {
            override fun run() {
                if (!isClockUpdating || !isAdded) return
                updateClock()
                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - lastClockUpdateTime
                lastClockUpdateTime = currentTime
                val targetDelay = 10L
                val adjustedDelay = maxOf(1L, targetDelay - elapsed)
                clockHandler.postDelayed(this, adjustedDelay)
            }
        }
        clockHandler.post(updateRunnable)
    }

    private fun stopClockUpdates() {
        isClockUpdating = false
        clockHandler.removeCallbacksAndMessages(null)
    }

    private fun updateClock() {
        val now = System.currentTimeMillis()
        val timeStr = timeFormat.format(now)
        val millisStr = millisFormat.format(now)
        val fullTimeStr = "$timeStr.$millisStr"
        val dateStr = dateFormat.format(now)
        val dayStr = dayFormat.format(now)
        tvTime.text = fullTimeStr
        tvDate.text = dateStr
        tvDayOfWeek.text = dayStr
    }
}