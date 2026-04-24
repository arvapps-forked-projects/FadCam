package com.fadcam.network;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import com.fadcam.FLog;
import com.fadcam.SharedPreferencesManager;

import org.osmdroid.util.GeoPoint;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherService {
    private static final String TAG = "WeatherService";
    private static WeatherService instance;

    private static final String WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast";

    private final OkHttpClient httpClient;
    private final SharedPreferencesManager prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String currentWeather = "";
    private String currentWind = "";
    private long lastFetchTime = 0;
    private static final long CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(10);

    private WeatherService(Context context) {
        this.prefs = SharedPreferencesManager.getInstance(context.getApplicationContext());
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        FLog.d(TAG, "WeatherService initialized");
    }

    public static synchronized WeatherService getInstance(Context context) {
        if (instance == null) {
            instance = new WeatherService(context.getApplicationContext());
        }
        return instance;
    }

    public void fetchWeather(GeoPoint location, WeatherCallback callback) {
        if (location == null || callback == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!currentWeather.isEmpty() && (now - lastFetchTime) < CACHE_DURATION_MS) {
            FLog.d(TAG, "Weather cache hit: " + currentWeather + ", " + currentWind + " (age=" + ((now - lastFetchTime)/1000) + "s)");
            callback.onWeatherReady(currentWeather, currentWind);
            return;
        }
        FLog.d(TAG, "Weather cache miss, fetching from API...");

        String url = String.format(Locale.US,
                "%s?latitude=%f&longitude=%f&current=temperature_2m,weather_code,wind_speed_10m",
                WEATHER_API_URL, location.getLatitude(), location.getLongitude());

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                FLog.e(TAG, "Weather API fetch failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (!response.isSuccessful()) {
                    FLog.e(TAG, "Weather API error: " + response.code());
                    return;
                }

                String body = response.body() != null ? response.body().string() : "";
                parseWeatherResponse(body);
                mainHandler.post(() -> callback.onWeatherReady(currentWeather, currentWind));
            }
        });
    }

    private void parseWeatherResponse(String json) {
        if (json == null || json.isEmpty()) return;

        try {
            org.json.JSONObject root = new org.json.JSONObject(json);
            org.json.JSONObject current = root.optJSONObject("current");
            if (current == null) return;

            double temp = current.optDouble("temperature_2m", 0);
            double wind = current.optDouble("wind_speed_10m", 0);
            int code = current.optInt("weather_code", 0);

            String weatherDesc = getWeatherDescription(code);
            currentWeather = String.format(Locale.US, "%.0f°C %s", temp, weatherDesc);
            currentWind = String.format(Locale.US, "%.0f km/h", wind);
            lastFetchTime = System.currentTimeMillis();

            FLog.d(TAG, "Weather: " + currentWeather + ", Wind: " + currentWind);
        } catch (Exception e) {
            FLog.e(TAG, "Error parsing weather JSON", e);
        }
    }

    private String getWeatherDescription(int code) {
        if (code == 0) return "Clear";
        if (code <= 3) return "Cloudy";
        if (code <= 49) return "Fog";
        if (code <= 59) return "Drizzle";
        if (code <= 69) return "Rain";
        if (code <= 79) return "Snow";
        if (code <= 84) return "Rain Showers";
        if (code <= 94) return "Snow Showers";
        if (code <= 99) return "Thunderstorm";
        return "Unknown";
    }

    public String getCurrentWeather() {
        return currentWeather;
    }

    public String getCurrentWind() {
        return currentWind;
    }

    public interface WeatherCallback {
        void onWeatherReady(String weather, String wind);
    }
}