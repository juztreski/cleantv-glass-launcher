package io.tvhome.launcher;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TvController {
    private static final String TAG = "GlassTVTuner";
    private static final Pattern CHANNEL_URI =
            Pattern.compile("content://android\\.media\\.tv/channel/\\d+");
    private final Activity activity;
    private final TvView tvView;
    private final TvInputManager inputManager;
    private final SharedPreferences preferences;
    private final Map<String, Integer> inputStates = new HashMap<>();
    private final Set<String> launcherTunedUris = new HashSet<>();
    private final TvInputManager.TvInputCallback inputCallback;
    private Process logcatProcess;
    private volatile boolean previewActive;
    private int savedVolume;
    private int maxVolume;

    static final class Channel {
        final long id;
        final Uri uri;
        final String inputId;
        final String number;
        final String name;

        Channel(long id, String inputId, String number, String name) {
            this.id = id;
            this.uri = TvContract.buildChannelUri(id);
            this.inputId = inputId;
            this.number = number;
            this.name = name;
        }
    }

    TvController(Activity activity, TvView tvView) {
        this.activity = activity;
        this.tvView = tvView;
        this.inputManager = (TvInputManager) activity.getSystemService(Context.TV_INPUT_SERVICE);
        this.preferences = activity.getSharedPreferences("launcher", Context.MODE_PRIVATE);
        inputCallback = new TvInputManager.TvInputCallback() {
                @Override
                public void onInputStateChanged(String inputId, int state) {
                    synchronized (inputStates) {
                        inputStates.put(inputId, state);
                    }
                    activity.runOnUiThread(() -> {
                        if (!activity.isFinishing() && activity instanceof MainActivity) {
                            ((MainActivity) activity).refreshInputState();
                        }
                    });
                }
            };
        if (inputManager != null) {
            inputManager.registerCallback(inputCallback, new Handler(Looper.getMainLooper()));
        }
        tvView.setCallback(new TvView.TvInputCallback() {
            @Override
            public void onVideoAvailable(String inputId) {
                tvView.setVisibility(android.view.View.VISIBLE);
            }

            @Override
            public void onVideoUnavailable(String inputId, int reason) {
                tvView.setVisibility(android.view.View.GONE);
                Log.w(TAG, "Antenna video unavailable (reason " + reason + ")");
            }

            @Override
            public void onConnectionFailed(String inputId) {
                Log.e(TAG, "TV input connection failed: " + inputId);
                release();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).onInputConnectionFailed();
                }
            }

            @Override
            public void onDisconnected(String inputId) {
                Log.e(TAG, "TV input disconnected: " + inputId);
                release();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).onInputConnectionFailed();
                }
            }
        });
    }

    void startLogcat() {
        if (logcatProcess != null) return;
        try {
            logcatProcess = new ProcessBuilder("/system/bin/logcat", "-v", "brief",
                    "TvInputManagerService:I", "*:S").redirectErrorStream(true).start();
            Process process = logcatProcess;
            Thread reader = new Thread(() -> readRetunes(process), "tv-retune-log");
            reader.setDaemon(true);
            reader.start();
        } catch (IOException e) {
            Log.e(TAG, "Could not start TvInputManagerService log reader", e);
        }
    }

    void stopLogcat() {
        Process process = logcatProcess;
        logcatProcess = null;
        if (process != null) process.destroy();
    }

    private void readRetunes(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null && process == logcatProcess) {
                if (!line.contains("onChannelRetuned")) continue;
                Matcher matcher = CHANNEL_URI.matcher(line);
                if (!matcher.find()) continue;
                Uri channelUri = Uri.parse(matcher.group());
                synchronized (launcherTunedUris) {
                    if (launcherTunedUris.contains(channelUri.toString())) continue;
                }
                preferences.edit().putString("last_channel_uri", channelUri.toString()).apply();
                Log.i(TAG, "Saved last antenna channel from TV input service");
            }
        } catch (IOException e) {
            if (process == logcatProcess) Log.e(TAG, "TV retune log reader failed", e);
        }
    }

    List<Channel> getChannels() {
        List<Channel> result = new ArrayList<>();
        ContentResolver resolver = activity.getContentResolver();
        String[] projection = {
                TvContract.Channels._ID,
                TvContract.Channels.COLUMN_INPUT_ID,
                TvContract.Channels.COLUMN_DISPLAY_NUMBER,
                TvContract.Channels.COLUMN_DISPLAY_NAME
        };
        try (Cursor cursor = resolver.query(TvContract.Channels.CONTENT_URI, projection,
                null, null, null)) {
            if (cursor == null) return result;
            int idIndex = cursor.getColumnIndexOrThrow(TvContract.Channels._ID);
            int inputIndex = cursor.getColumnIndexOrThrow(TvContract.Channels.COLUMN_INPUT_ID);
            int numberIndex = cursor.getColumnIndexOrThrow(TvContract.Channels.COLUMN_DISPLAY_NUMBER);
            int nameIndex = cursor.getColumnIndexOrThrow(TvContract.Channels.COLUMN_DISPLAY_NAME);
            while (cursor.moveToNext()) {
                String input = cursor.getString(inputIndex);
                if (input == null || !input.contains(".tuner.TunerInputService")) continue;
                result.add(new Channel(cursor.getLong(idIndex), input,
                        cursor.getString(numberIndex), cursor.getString(nameIndex)));
            }
        } catch (SecurityException e) {
            Log.e(TAG, "READ_TV_LISTINGS is unavailable; cannot read antenna channels", e);
        }
        return result;
    }

    Channel chooseLastOrSmallest() {
        List<Channel> channels = getChannels();
        if (channels.isEmpty()) return null;
        String saved = preferences.getString("last_channel_uri", "");
        for (Channel channel : channels) {
            if (channel.uri.toString().equals(saved)) return channel;
        }
        channels.sort(Comparator.comparingDouble(channel -> channelNumber(channel.number)));
        return channels.get(0);
    }

    private double channelNumber(String value) {
        if (value == null || value.isEmpty()) return Double.MAX_VALUE;
        String normalized = value.replace('-', '.');
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return Double.MAX_VALUE;
        }
    }

    void tune(Channel channel, boolean mute) {
        if (channel == null) return;
        saveVolumeBeforeTune();
        previewActive = true;
        synchronized (launcherTunedUris) {
            launcherTunedUris.add(channel.uri.toString());
        }
        tvView.setVisibility(android.view.View.VISIBLE);
        try {
            tvView.setStreamVolume(mute ? 0f : 1f);
            tvView.tune(channel.inputId, channel.uri);
        } catch (IllegalArgumentException | SecurityException e) {
            Log.e(TAG, "Could not tune antenna channel " + channel.uri, e);
            release();
        }
    }

    void switchToInput(TvInputInfo input) {
        saveVolumeBeforeTune();
        previewActive = true;
        tvView.setVisibility(android.view.View.VISIBLE);
        try {
            tvView.setStreamVolume(1f);
            tvView.tune(input.getId(), null);
        } catch (IllegalArgumentException | SecurityException e) {
            Log.e(TAG, "Could not switch to TV input " + input.getId(), e);
            release();
        }
    }

    private void saveVolumeBeforeTune() {
        if (previewActive) return;
        AudioManager audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        savedVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxVolume = Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
    }

    boolean isActive() {
        return previewActive;
    }

    void release() {
        if (!previewActive) return;
        AudioManager audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        tvView.setStreamVolume((float) savedVolume / maxVolume);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolume, 0);
        tvView.reset();
        tvView.setVisibility(android.view.View.GONE);
        previewActive = false;
    }

    int inputState(String inputId) {
        if (inputManager == null) return TvInputManager.INPUT_STATE_DISCONNECTED;
        synchronized (inputStates) {
            Integer state = inputStates.get(inputId);
            if (state != null) return state;
        }
        try {
            return inputManager.getInputState(inputId);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "TV input is no longer registered: " + inputId, e);
            return TvInputManager.INPUT_STATE_DISCONNECTED;
        }
    }

    void dispose() {
        if (inputManager != null) inputManager.unregisterCallback(inputCallback);
        stopLogcat();
        release();
    }

    List<TvInputInfo> getInputs() {
        if (inputManager == null) return new ArrayList<>();
        try {
            return inputManager.getTvInputList();
        } catch (SecurityException e) {
            Log.e(TAG, "TV input list is not available to this app", e);
            return new ArrayList<>();
        }
    }

    boolean isAntennaInput(TvInputInfo info) {
        return info.getId().contains(".tuner.TunerInputService");
    }

    @SuppressWarnings("deprecation")
    static String displayName(TvInputInfo info, Context context) {
        CharSequence label = info.loadLabel(context);
        return label == null ? info.getId() : label.toString();
    }
}
