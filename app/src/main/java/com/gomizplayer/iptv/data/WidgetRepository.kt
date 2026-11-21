package com.gomizplayer.iptv.data

import com.gomizplayer.iptv.core.net.HttpClient
import com.gomizplayer.iptv.core.store.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class OpenMeteoResponse(
    val current_weather: CurrentWeather? = null,
    val timezone: String? = null
)
@Serializable
private data class CurrentWeather(
    val temperature: Double? = null,
    val weathercode: Int? = null,
    val is_day: Int? = null,
    val windspeed: Double? = null,
    val winddirection: Int? = null
)
@Serializable
private data class GeocodingResponse(val results: List<GeocodingResult> = emptyList())
@Serializable
private data class GeocodingResult(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
    val country: String? = null
)
@Serializable
private data class IpWhoIs(
    val success: Boolean? = null,
    val city: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
@Serializable
private data class AladhanResponse(val code: Int = 200, val data: AladhanData? = null)
@Serializable
private data class AladhanData(val timings: Map<String, String>? = null)

data class WidgetSnapshot(
    val loading: Boolean = false,
    val error: String? = null,
    val city: String = "",
    val country: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val tempC: Double? = null,
    val weatherCode: Int? = null,
    val isDay: Boolean = true,
    val windSpeed: Double? = null,
    val windDir: Int? = null,
    val desc: String = "",
    /** Raw "HH:mm" timings keyed by lowercase prayer name */
    val prayers: Map<String, String> = emptyMap(),
    val fetchedAt: Long = 0L
)

/**
 * Weather + prayer times widget data source.
 * Free APIs, no keys: Open-Meteo (weather + geocoding), Aladhan (prayer), ipwho.is (geolocation).
 * Mirrors the Windows renderer widget logic.
 */
class WidgetRepository(private val store: Store) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    private val _state = MutableStateFlow(WidgetSnapshot(loading = true))
    val state: StateFlow<WidgetSnapshot> = _state.asStateFlow()

    companion object {
        const val REFRESH_MS = 10 * 60 * 1000L

        fun create(store: Store) = WidgetRepository(store)
    }

    suspend fun refresh(force: Boolean = false) = withContext(Dispatchers.IO) {
        val current = _state.value
        if (!force && current.fetchedAt > 0 && System.currentTimeMillis() - current.fetchedAt < REFRESH_MS) return@withContext

        val settings = store.data.value.settings
        var location: Pair<Double, Double>? = null
        var city = settings.weatherCity.trim()
        var country = settings.weatherCountry.trim()

        when {
            settings.weatherLat != 0.0 && settings.weatherLon != 0.0 -> location = settings.weatherLat to settings.weatherLon
            city.isNotBlank() -> {
                try {
                    val encoded = java.net.URLEncoder.encode(city, "UTF-8")
                    val bytes = HttpClient.fetchBytes(
                        "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json",
                        emptyMap(),
                        512 * 1024
                    )
                    val geo = json.decodeFromString(GeocodingResponse.serializer(), bytes.toString(Charsets.UTF_8))
                    val hit = geo.results.firstOrNull()
                    if (hit != null) {
                        location = hit.latitude to hit.longitude
                        city = hit.name ?: city
                        country = hit.country ?: country
                        store.patchSettings(
                            store.data.value.settings.copy(
                                weatherLat = hit.latitude,
                                weatherLon = hit.longitude,
                                weatherCity = city,
                                weatherCountry = country
                            )
                        )
                    }
                } catch (_: Exception) { /* fall back to auto */ }
            }
            else -> {
                try {
                    val bytes = HttpClient.fetchBytes("https://ipwho.is/", emptyMap(), 512 * 1024)
                    val geo = json.decodeFromString(IpWhoIs.serializer(), bytes.toString(Charsets.UTF_8))
                    if (geo.latitude != null && geo.longitude != null && geo.success != false) {
                        location = geo.latitude to geo.longitude
                        city = geo.city ?: ""
                        country = geo.country ?: ""
                    }
                } catch (_: Exception) { /* offline tolerated */ }
            }
        }

        if (location == null) {
            _state.value = current.copy(loading = false, error = "no_location")
            return@withContext
        }

        val (lat, lon) = location
        var tempC: Double? = null
        var weatherCode: Int? = null
        var isDay = true
        var windSpeed: Double? = null
        var windDir: Int? = null
        var error: String? = null

        try {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&timezone=auto"
            val bytes = HttpClient.fetchBytes(url, emptyMap(), 1 * 1024 * 1024)
            val parsed = json.decodeFromString(OpenMeteoResponse.serializer(), bytes.toString(Charsets.UTF_8))
            parsed.current_weather?.let { cw ->
                tempC = cw.temperature
                weatherCode = cw.weathercode
                isDay = cw.is_day != 0
                windSpeed = cw.windspeed
                windDir = cw.winddirection
            }
        } catch (e: Exception) {
            error = e.message ?: "weather_error"
        }

        var prayers: Map<String, String> = emptyMap()
        try {
            val cal = java.util.Calendar.getInstance()
            val dd = "%02d".format(cal.get(java.util.Calendar.DAY_OF_MONTH))
            val mm = "%02d".format(cal.get(java.util.Calendar.MONTH) + 1)
            val yyyy = cal.get(java.util.Calendar.YEAR)
            val method = store.data.value.settings.prayerMethod
            val school = store.data.value.settings.prayerSchool
            val url = "https://api.aladhan.com/v1/timings/$dd-$mm-$yyyy?latitude=$lat&longitude=$lon&method=$method&school=$school"
            val bytes = HttpClient.fetchBytes(url, emptyMap(), 2 * 1024 * 1024)
            val parsed = json.decodeFromString(AladhanResponse.serializer(), bytes.toString(Charsets.UTF_8))
            val timings = parsed.data?.timings.orEmpty()
            prayers = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
                .mapNotNull { name ->
                    timings[name]?.let { name.lowercase() to it.trim().substringBefore(' ') }
                }
                .toMap()
        } catch (e: Exception) {
            error = error ?: (e.message ?: "prayer_error")
        }

        _state.value = WidgetSnapshot(
            loading = false,
            error = error,
            city = city,
            country = country,
            lat = lat,
            lon = lon,
            tempC = tempC,
            weatherCode = weatherCode,
            isDay = isDay,
            windSpeed = windSpeed,
            windDir = windDir,
            desc = weatherDescription(weatherCode),
            prayers = prayers,
            fetchedAt = System.currentTimeMillis()
        )
    }

    /** Ordered prayer entries for display: (label, time "HH:mm"). */
    fun prayerList(): List<Pair<String, String>> {
        val p = _state.value.prayers
        if (p.isEmpty()) return emptyList()
        val order = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
        return order.mapNotNull { key -> p[key]?.let { key to it } }
    }

    /** (name, time) of the next upcoming prayer today, or tomorrow's Fajr if all passed. */
    fun nextPrayer(nowMinutes: Int): Pair<String, String>? {
        val list = prayerList()
        if (list.isEmpty()) return null
        for ((name, time) in list) {
            val t = toMinutes(time) ?: continue
            if (t > nowMinutes) return name to time
        }
        // all passed → tomorrow's fajr
        return list.firstOrNull { it.first == "fajr" }?.let { it.first to it.second }
    }

    fun toMinutes(time: String): Int? = try {
        val parts = time.trim().split(":")
        if (parts.size < 2) null else parts[0].toInt() * 60 + parts[1].toInt()
    } catch (_: Exception) { null }
}

fun weatherDescription(code: Int?): String = when (code) {
    null -> ""
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    56, 57 -> "Freezing drizzle"
    61, 63, 65 -> "Rain"
    66, 67 -> "Freezing rain"
    71, 73, 75 -> "Snow"
    77 -> "Snow grains"
    80, 81, 82 -> "Showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm with hail"
    else -> "Unknown"
}

fun weatherEmoji(code: Int?, isDay: Boolean): String {
    // Day/night-aware symbols — never show a sun at night.
    return when (code) {
        null -> if (isDay) "☀️" else "🌙"
        0 -> if (isDay) "☀️" else "🌙"                       // clear
        1 -> if (isDay) "🌤️" else "🌙"                       // mainly clear
        2 -> if (isDay) "⛅" else "🌙☁️"                      // partly cloudy — was showing sun at night
        3 -> "☁️"                                             // overcast
        45, 48 -> "🌫️"                                        // fog
        in 51..67, in 80..82 -> "🌧️"                         // rain
        in 71..86 -> "🌨️"                                     // snow
        in 95..99 -> "⛈️"                                     // thunderstorm
        else -> if (isDay) "⛅" else "🌙☁️"
    }
}
