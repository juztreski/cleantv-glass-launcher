package io.tvhome.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class NetworkWidgets {
    private static final String TAG = "GlassTVData";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    static final class PrayerTimes {
        final String fajr;
        final String dhuhr;
        final String asr;
        final String maghrib;
        final String isha;

        PrayerTimes(JSONObject timings) {
            fajr = cleanTime(timings.optString("Fajr"));
            dhuhr = cleanTime(timings.optString("Dhuhr"));
            asr = cleanTime(timings.optString("Asr"));
            maghrib = cleanTime(timings.optString("Maghrib"));
            isha = cleanTime(timings.optString("Isha"));
        }

        private static String cleanTime(String value) {
            int suffix = value.indexOf(' ');
            return suffix < 0 ? value : value.substring(0, suffix);
        }

        JSONObject toJson() {
            JSONObject value = new JSONObject();
            try {
                value.put("Fajr", fajr);
                value.put("Dhuhr", dhuhr);
                value.put("Asr", asr);
                value.put("Maghrib", maghrib);
                value.put("Isha", isha);
            } catch (org.json.JSONException e) {
                throw new IllegalStateException(e);
            }
            return value;
        }
    }

    static void refreshPrayerTimes(Context context, String latitude, String longitude, String timezone,
                                   Callback<PrayerTimes> callback) {
        SharedPreferences cache = context.getSharedPreferences("widgets", Context.MODE_PRIVATE);
        Date requestDate = new Date();
        java.util.TimeZone prayerTimeZone = java.util.TimeZone.getTimeZone(timezone);
        SimpleDateFormat cacheDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        cacheDateFormat.setTimeZone(prayerTimeZone);
        String today = cacheDateFormat.format(requestDate);
        String location = latitude + "," + longitude;
        String cached = location.equals(cache.getString("prayer_location", ""))
                ? cache.getString("prayer_json", "") : "";
        if (!cached.isEmpty()) {
            try {
                PrayerTimes saved = new PrayerTimes(new JSONObject(cached));
                MAIN.post(() -> callback.onSuccess(saved));
            } catch (org.json.JSONException e) {
                Log.w(TAG, "Ignoring invalid cached prayer times", e);
            }
        }
        if (today.equals(cache.getString("prayer_date", ""))
                && location.equals(cache.getString("prayer_location", ""))) return;

        EXECUTOR.execute(() -> {
            try {
                SimpleDateFormat requestDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                requestDateFormat.setTimeZone(prayerTimeZone);
                JSONObject response = new JSONObject(get(
                        "https://api.aladhan.com/v1/timings/" + requestDateFormat.format(requestDate)
                                + "?latitude=" + latitude + "&longitude=" + longitude + "&method=20"));
                JSONObject timings = response.getJSONObject("data").getJSONObject("timings");
                PrayerTimes result = new PrayerTimes(timings);
                cache.edit().putString("prayer_json", result.toJson().toString())
                        .putString("prayer_date", today).putString("prayer_location", location).apply();
                MAIN.post(() -> callback.onSuccess(result));
            } catch (IOException | org.json.JSONException e) {
                Log.e(TAG, "Prayer-time request failed for coordinates " + location, e);
                if (cached.isEmpty()) {
                    MAIN.post(() -> callback.onError("Jadwal sholat tidak tersedia"));
                }
            }
        });
    }

    private static String get(String endpoint) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "GlassTVLauncher/1.0");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + status + " from " + connection.getURL().getHost());
            }
            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
            }
            return result.toString();
        } finally {
            connection.disconnect();
        }
    }

    private NetworkWidgets() {}
}
