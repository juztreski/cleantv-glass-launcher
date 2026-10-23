package io.tvhome.launcher;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String TAG = "GlassTV";
    private static final String PREFS = "launcher";
    private static final String KEY_WALLPAPER_KIND = "wallpaper_kind";
    private static final String KEY_WALLPAPER_VALUE = "wallpaper_value";
    private static final String KEY_LOCATION_NAME = "location_name";
    private static final String KEY_LOCATION_LAT = "location_lat";
    private static final String KEY_LOCATION_LON = "location_lon";
    private static final String KEY_LOCATION_TIMEZONE = "location_timezone";
    private static final String KEY_SCREENSAVER_AUTO = "screensaver_auto";
    private static final String KEY_SCREENSAVER_TIMEOUT = "screensaver_timeout";
    private static final String KEY_VERSE_AUTO = "verse_auto";
    private static final String KEY_VERSE_ID = "verse_id";
    private static final String KEY_DOCK_PREVIEW = "dock_preview";
    private static final String KEY_DOCK_FAVORITES = "dock_favorites";
    private static final String KEY_DOCK_COUNT = "dock_count";
    private static final String KEY_GREETING_NAME = "greeting_name";
    private static final int MAX_DOCK_APPS = 6;
    private static final int REQUEST_IMAGE = 41;
    private static final int REQUEST_LOCATION = 42;
    private static final int PILLS = 0;
    private static final int DOCK = 1;
    private static final int WATCH = 2;
    private static final int CHANNELS = 3;
    private static final int GRID = 4;
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Tile> dockTiles = new ArrayList<>();
    private final List<Tile> watchTiles = new ArrayList<>();
    private final List<Tile> channelTiles = new ArrayList<>();
    private final List<Tile> gridTiles = new ArrayList<>();
    private final List<Tile> allApps = new ArrayList<>();
    private final List<View> pillViews = new ArrayList<>();
    private final List<PreviewPoster> posters = new ArrayList<>();
    private final Map<String, PreviewInfo> previewCache = new HashMap<>();
    private final List<String> prayerNames = Arrays.asList("Subuh", "Zuhur", "Asar", "Magrib", "Isya");
    private final List<String> prayerKeys = Arrays.asList("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha");
    private FrameLayout root;
    private View homePage;
    private View homeScrim;
    private FrameLayout posterLayer;
    private ImageView posterImage;
    private ImageView wallpaperImage;
    private TextView posterTitle;
    private TvView tvView;
    private TvController tvController;
    private View prayerWidget;
    private LinearLayout watchWidget;
    private LinearLayout channelsWidget;
    private RecyclerView dock;
    private RecyclerView watch;
    private RecyclerView channels;
    private RecyclerView grid;
    private ScrollView homeScroll;
    private TileAdapter dockAdapter;
    private TileAdapter watchAdapter;
    private TileAdapter channelsAdapter;
    private TileAdapter gridAdapter;
    private LinearLayout pillRow;
    private View glassHighlight;
    private TextView prayerEyebrow;
    private TextView nextPrayer;
    private TextView prayerRow;
    private TextView prayerStatus;
    private TextView clockText;
    private TextView dateText;
    private TextView greetingText;
    private TextView focusedDockTitle;
    private TextView gridLabel;
    private TextView watchTitle;
    private TextView watchEmpty;
    private TextView channelsEmpty;
    private TextView verseEyebrow;
    private TextView verseArabic;
    private TextView verseTranslation;
    private TextView verseSource;
    private FrameLayout dockPreviewLayer;
    private VideoView dockPreviewVideo;
    private ImageView dockPreviewImage;
    private TextView wifiPill;
    private TextView inputPill;
    private TextView quickPill;
    private android.widget.PopupWindow inputPopup;
    private android.widget.PopupWindow menuPopup;
    private AlertDialog wallpaperDialog;
    private Dialog choiceDialog;
    private Dialog wallpaperGalleryDialog;
    private Dialog favoritesDialog;
    private Dialog nameDialog;
    private View screensaverLayer;
    private TextView screensaverLocation;
    private TextView screensaverClock;
    private TextView screensaverDate;
    private TextView screensaverNext;
    private TextView screensaverCountdown;
    private TextView screensaverPrayerRow;
    private Typeface outfitTypeface;
    private int gridColumns;
    private int currentSection = DOCK;
    private int currentPosition;
    private int posterPosition;
    private int wallpaperGeneration;
    private int locationGeneration;
    private View highlightedTile;
    private boolean centerPressed;
    private boolean centerLongTriggered;
    private boolean externalInputVisible;
    private boolean screensaverVisible;
    private NetworkWidgets.PrayerTimes prayerTimes;
    private volatile boolean previewProviderBlocked;
    private Runnable dockPreviewRunnable;
    private int dockPreviewGeneration;
    private final Runnable watchRefreshRunnable = this::loadWatchNext;

    private final Runnable prayerClockRunnable = new Runnable() {
        @Override
        public void run() {
            renderPrayerTimes();
            renderClock();
            renderVerse();
            handler.postDelayed(this, 60_000L);
        }
    };
    private final Runnable posterNextRunnable = new Runnable() {
        @Override
        public void run() {
            if (posters.isEmpty() || posterLayer.getVisibility() != View.VISIBLE) return;
            posterPosition = (posterPosition + 1) % posters.size();
            showPoster(posterPosition);
            handler.postDelayed(this, 6_000L);
        }
    };
    private final Runnable centerLongPressRunnable = () -> {
        if (centerPressed && !posters.isEmpty() && posterLayer.getVisibility() == View.VISIBLE) {
            centerLongTriggered = true;
            launchIntentUri(posters.get(posterPosition).intentUri,
                    posters.get(posterPosition).packageName);
        }
    };
    private final Runnable screensaverTimeoutRunnable = () -> {
        if (screensaverVisible || externalInputVisible) return;
        if ((menuPopup != null && menuPopup.isShowing())
                || (inputPopup != null && inputPopup.isShowing())
                || (wallpaperDialog != null && wallpaperDialog.isShowing())
                || (choiceDialog != null && choiceDialog.isShowing())
                || (wallpaperGalleryDialog != null && wallpaperGalleryDialog.isShowing())
                || (favoritesDialog != null && favoritesDialog.isShowing())
                || (nameDialog != null && nameDialog.isShowing())) {
            scheduleScreensaver();
            return;
        }
        showScreensaver();
    };
    private final Runnable screensaverTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!screensaverVisible) return;
            renderScreensaver();
            handler.postDelayed(this, 1_000L);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gridColumns = getResources().getInteger(R.integer.grid_columns);
        if (gridColumns < 1) throw new IllegalStateException("Grid column count must be positive");
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        buildLayout();
        loadApps();
        bindAdapters();
        renderVerse();
        root.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                root.post(MainActivity.this::restoreFocus);
            }
        });
        if (featureEnabled("prayer_times")) refreshPrayerTimes();
        handler.post(prayerClockRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (wallpaperImage != null && !externalInputVisible) {
            wallpaperImage.setVisibility(View.VISIBLE);
        }
        updateWifiPill();
        updateInputPill();
        previewCache.clear();
        loadWatchNext();
        handler.removeCallbacks(watchRefreshRunnable);
        handler.postDelayed(watchRefreshRunnable, 900L);
        scheduleScreensaver();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(posterNextRunnable);
        handler.removeCallbacks(watchRefreshRunnable);
        cancelScreensaverTimer();
        if (screensaverVisible) hideScreensaver();
        cancelScreensaverTimer();
        if (tvController != null) {
            releaseTuner();
            tvController.stopLogcat();
        }
        hidePoster();
        hideDockPreview();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (tvController != null) tvController.dispose();
        super.onDestroy();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (!screensaverVisible) resetScreensaverTimer();
    }

    private void buildLayout() {
        try {
            outfitTypeface = Typeface.createFromAsset(getAssets(), "fonts/Outfit-Variable.ttf");
        } catch (RuntimeException e) {
            Log.w(TAG, "Outfit font unavailable; using the system sans-serif font", e);
        }
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        tvView = new TvView(this);
        tvView.setVisibility(View.GONE);
        tvController = new TvController(this, tvView);

        wallpaperImage = new ImageView(this);
        wallpaperImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(wallpaperImage, new FrameLayout.LayoutParams(-1, -1));

        buildDockPreviewLayer();

        homeScrim = new View(this);
        root.addView(homeScrim, new FrameLayout.LayoutParams(-1, -1));
        applyWallpaper();

        LinearLayout page = new LinearLayout(this);
        homePage = page;
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(50), dp(20), dp(50), 0);
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(-1, dp(228));
        page.addView(top, topParams);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        top.addView(left, new LinearLayout.LayoutParams(0, -1, 1f));
        greetingText = text("Selamat datang", 17, 0xFFBFC8D5, false);
        softShadow(greetingText);
        left.addView(greetingText, new LinearLayout.LayoutParams(-1, dp(27)));
        clockText = text("--:--", 58, Color.WHITE, false);
        softShadow(clockText);
        if (outfitTypeface != null) {
            clockText.setTypeface(Typeface.create(outfitTypeface, Typeface.NORMAL));
            if (android.os.Build.VERSION.SDK_INT >= 26) clockText.setFontVariationSettings("'wght' 300");
        } else {
            clockText.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        }
        left.addView(clockText, new LinearLayout.LayoutParams(-1, dp(68)));
        dateText = text("", 15, 0xFFBFC8D5, false);
        softShadow(dateText);
        left.addView(dateText, new LinearLayout.LayoutParams(-1, dp(26)));
        buildPrayerWidget(left);
        prayerWidget.setVisibility(featureEnabled("prayer_times") ? View.VISIBLE : View.GONE);
        buildSystemWidgets(top);
        pillRow.setVisibility(featureEnabled("system_pills") ? View.VISIBLE : View.GONE);

        homeScroll = new ScrollView(this);
        homeScroll.setFillViewport(true);
        homeScroll.setVerticalScrollBarEnabled(false);
        homeScroll.setClipToPadding(false);
        page.addView(homeScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        homeScroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        View spacer = new View(this);
        content.addView(spacer, new LinearLayout.LayoutParams(-1, dp(105)));
        focusedDockTitle = text("", 30, Color.WHITE, true);
        focusedDockTitle.setShadowLayer(dp(22), 0, 0, 0x668BBFFF);
        content.addView(focusedDockTitle, new LinearLayout.LayoutParams(-1, dp(46)));

        LinearLayout dockWidget = new LinearLayout(this);
        dockWidget.setOrientation(LinearLayout.VERTICAL);
        content.addView(dockWidget);
        dock = new RecyclerView(this);
        dock.setClipToPadding(false);
        dock.setPadding(dp(8), dp(8), dp(8), dp(8));
        dock.setBackground(glassBackground(0xFF8BBFFF));
        dock.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        dock.setNestedScrollingEnabled(false);
        LinearLayout.LayoutParams dockParams = new LinearLayout.LayoutParams(-1, dp(122));
        dockWidget.addView(dock, dockParams);

        watchTitle = sectionTitle("Lanjutkan menonton");
        watchTitle.setVisibility(View.GONE);
        LinearLayout.LayoutParams watchTitleParams = new LinearLayout.LayoutParams(-1, dp(46));
        watchTitleParams.topMargin = dp(18);
        content.addView(watchTitle, watchTitleParams);
        watch = horizontalRow(154);
        watch.setVisibility(View.GONE);
        content.addView(watch, new LinearLayout.LayoutParams(-1, dp(154)));

        gridLabel = sectionTitle("Aplikasi");
        LinearLayout.LayoutParams gridTitleParams = new LinearLayout.LayoutParams(-1, dp(46));
        gridTitleParams.topMargin = dp(22);
        content.addView(gridLabel, gridTitleParams);
        grid = new RecyclerView(this);
        grid.setClipToPadding(false);
        grid.setPadding(dp(4), dp(6), dp(4), dp(24));
        grid.setLayoutManager(new GridLayoutManager(this, gridColumns));
        grid.setNestedScrollingEnabled(false);
        content.addView(grid, new LinearLayout.LayoutParams(-1, -2));

        glassHighlight = new View(this);
        glassHighlight.setVisibility(View.INVISIBLE);
        glassHighlight.setClickable(false);
        glassHighlight.setFocusable(false);
        root.addView(glassHighlight, new FrameLayout.LayoutParams(dp(120), dp(120)));

        buildScreensaverLayer();

        setContentView(root);
        renderClock();
    }

    private void buildDockPreviewLayer() {
        FrameLayout layer = new FrameLayout(this);
        layer.setBackgroundColor(Color.BLACK);
        layer.setVisibility(View.GONE);
        dockPreviewLayer = layer;

        dockPreviewVideo = new VideoView(this);
        dockPreviewVideo.setFocusable(false);
        dockPreviewVideo.setClickable(false);
        dockPreviewVideo.setBackgroundColor(Color.BLACK);
        dockPreviewVideo.setOnPreparedListener(mediaPlayer -> {
            mediaPlayer.setVolume(0f, 0f);
            mediaPlayer.setLooping(true);
        });
        dockPreviewVideo.setOnErrorListener((mediaPlayer, what, extra) -> {
            dockPreviewVideo.setVisibility(View.GONE);
            if (dockPreviewImage.getDrawable() != null) {
                dockPreviewImage.setVisibility(View.VISIBLE);
            } else {
                dockPreviewLayer.setVisibility(View.GONE);
            }
            return true;
        });
        layer.addView(dockPreviewVideo, new FrameLayout.LayoutParams(-1, -1));

        dockPreviewImage = new ImageView(this);
        dockPreviewImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        dockPreviewImage.setVisibility(View.GONE);
        dockPreviewImage.setFocusable(false);
        dockPreviewImage.setClickable(false);
        layer.addView(dockPreviewImage, new FrameLayout.LayoutParams(-1, -1));

        root.addView(layer, new FrameLayout.LayoutParams(-1, -1));
    }

    private void buildPrayerWidget(LinearLayout parent) {
        LinearLayout prayer = new LinearLayout(this);
        prayerWidget = prayer;
        prayer.setOrientation(LinearLayout.VERTICAL);
        prayer.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(prayer, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView eyebrow = text(locationName().toUpperCase(new Locale("id", "ID"))
                + "  ·  JADWAL SHOLAT", 12, 0xFFC0CBD9, true);
        eyebrow.setLetterSpacing(0.05f);
        eyebrow.setMaxLines(1);
        eyebrow.setEllipsize(android.text.TextUtils.TruncateAt.END);
        softShadow(eyebrow);
        prayerEyebrow = eyebrow;
        prayer.addView(eyebrow, new LinearLayout.LayoutParams(-1, dp(24)));
        nextPrayer = text("Memuat jadwal…", 19, Color.WHITE, true);
        softShadow(nextPrayer);
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(-1, dp(32));
        nextParams.topMargin = dp(4);
        prayer.addView(nextPrayer, nextParams);
        prayerRow = text("Subuh  --:--  ·  Zuhur  --:--  ·  Asar  --:--  ·  Magrib  --:--  ·  Isya  --:--",
                12, 0xFFE0E6EF, false);
        softShadow(prayerRow);
        prayerRow.setMaxLines(1);
        prayer.addView(prayerRow, new LinearLayout.LayoutParams(-1, dp(24)));
        prayerStatus = text("", 13, 0xFFFFB6A8, false);
        softShadow(prayerStatus);
        prayer.addView(prayerStatus, new LinearLayout.LayoutParams(-1, dp(20)));
    }

    private void buildSystemWidgets(LinearLayout parent) {
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(dp(430), -1);
        rightParams.leftMargin = dp(22);
        parent.addView(right, rightParams);

        pillRow = new LinearLayout(this);
        pillRow.setGravity(Gravity.CENTER_VERTICAL);
        right.addView(pillRow, new LinearLayout.LayoutParams(-1, dp(56)));
        inputPill = addPill("○  Inputs", 0);
        wifiPill = addPill("Wi-Fi", 1);
        quickPill = addPill("Atur", 2);
        addPill("Setelan", 3);

        LinearLayout verse = new LinearLayout(this);
        verse.setOrientation(LinearLayout.VERTICAL);
        verse.setGravity(Gravity.CENTER_VERTICAL);
        verse.setPadding(dp(14), dp(9), dp(14), dp(8));
        verse.setBackground(glassBackground(0xFF9AC8FF));
        LinearLayout.LayoutParams verseParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        verseParams.topMargin = dp(10);
        right.addView(verse, verseParams);
        verse.setVisibility(featureEnabled("verse_widget") ? View.VISIBLE : View.GONE);

        verseEyebrow = text("AYAT HARI INI", 10, 0xFFC0CBD9, true);
        verseEyebrow.setLetterSpacing(0.12f);
        verseEyebrow.setMaxLines(1);
        verse.addView(verseEyebrow, new LinearLayout.LayoutParams(-1, dp(16)));

        verseArabic = text("", 16, Color.WHITE, false);
        verseArabic.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        verseArabic.setTextDirection(View.TEXT_DIRECTION_RTL);
        verseArabic.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        verseArabic.setMaxLines(2);
        verseArabic.setEllipsize(android.text.TextUtils.TruncateAt.END);
        verse.addView(verseArabic, new LinearLayout.LayoutParams(-1, dp(38)));

        verseTranslation = text("", 11, 0xFFE0E6EF, false);
        verseTranslation.setMaxLines(3);
        verseTranslation.setEllipsize(android.text.TextUtils.TruncateAt.END);
        verseTranslation.setLineSpacing(dp(1), 1f);
        verse.addView(verseTranslation, new LinearLayout.LayoutParams(-1, 0, 1f));

        verseSource = text("", 10, 0xFF9FB0C4, false);
        verseSource.setMaxLines(1);
        verseSource.setEllipsize(android.text.TextUtils.TruncateAt.END);
        verse.addView(verseSource, new LinearLayout.LayoutParams(-1, dp(16)));
    }

    private TextView addPill(String label, int index) {
        TextView pill = text(label, 13, Color.WHITE, true);
        pill.setGravity(Gravity.CENTER);
        pill.setMaxLines(1);
        pill.setEllipsize(android.text.TextUtils.TruncateAt.END);
        pill.setBackground(glassBackground(0xFF9AC8FF));
        pill.setFocusable(true);
        pill.setFocusableInTouchMode(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        pillRow.addView(pill, params);
        pillViews.add(pill);
        pill.setTag(R.id.focus_section, PILLS);
        pill.setTag(R.id.focus_position, index);
        pill.setOnFocusChangeListener((view, focused) -> onTileFocus(view, focused, 0xFF8BBFFF));
        pill.setOnClickListener(view -> activatePill(index));
        return pill;
    }

    private RecyclerView horizontalRow(int heightDp) {
        RecyclerView row = new RecyclerView(this);
        row.setClipToPadding(false);
        row.setPadding(dp(4), dp(4), dp(4), dp(4));
        row.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        row.setNestedScrollingEnabled(false);
        return row;
    }

    private void bindAdapters() {
        dockAdapter = new TileAdapter(DOCK, dockTiles);
        watchAdapter = new TileAdapter(WATCH, watchTiles);
        channelsAdapter = new TileAdapter(CHANNELS, channelTiles);
        gridAdapter = new TileAdapter(GRID, gridTiles);
        dock.setAdapter(dockAdapter);
        watch.setAdapter(watchAdapter);
        grid.setAdapter(gridAdapter);
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(query, 0);
        Set<String> hidden = new HashSet<>();
        Collections.addAll(hidden, getResources().getStringArray(R.array.hidden_packages));
        allApps.clear();
        for (ResolveInfo resolve : found) {
            String packageName = resolve.activityInfo.packageName;
            if (packageName.equals(getPackageName()) || hidden.contains(packageName)) continue;
            Drawable banner = loadBanner(pm, resolve.activityInfo.applicationInfo);
            Tile tile = Tile.app(packageName, resolve.loadLabel(pm).toString(), banner,
                    resolve.activityInfo.name, accentOf(banner));
            allApps.add(tile);
        }
        allApps.sort(Comparator.comparing(tile -> tile.title.toLowerCase(Locale.ROOT)));
        rebuildDockAndGrid();
    }

    private List<String> favoritePackages() {
        String stored = prefs().getString(KEY_DOCK_FAVORITES, null);
        List<String> packages = new ArrayList<>();
        if (stored != null) {
            for (String value : stored.split(",")) {
                String packageName = value.trim();
                if (!packageName.isEmpty() && !packages.contains(packageName)) {
                    packages.add(packageName);
                }
            }
        }
        if (packages.isEmpty()) {
            Collections.addAll(packages,
                    getResources().getStringArray(R.array.dock_packages));
        }
        return packages;
    }

    private int dockCount() {
        int fallback = Math.min(MAX_DOCK_APPS,
                getResources().getStringArray(R.array.dock_packages).length);
        int value = prefs().getInt(KEY_DOCK_COUNT, fallback);
        return Math.max(1, Math.min(MAX_DOCK_APPS, value));
    }

    private void fillDock(List<String> packages, int limit, Set<String> included) {
        for (String value : packages) {
            if (dockTiles.size() >= limit) break;
            if (value == null) continue;
            String packageName = value.trim();
            if (packageName.isEmpty() || included.contains(packageName)) continue;
            if ("__tv__".equals(packageName)) {
                if (featureEnabled("tv_dock_tile")) {
                    dockTiles.add(Tile.tv());
                    included.add(packageName);
                }
                continue;
            }
            Tile tile = appTile(packageName);
            if (tile != null) {
                dockTiles.add(tile);
                included.add(packageName);
            }
        }
    }

    private Tile appTile(String packageName) {
        for (Tile tile : allApps) {
            if (tile.packageName.equals(packageName)) return tile;
        }
        return null;
    }

    private void rebuildDockAndGrid() {
        dockTiles.clear();
        int count = dockCount();
        Set<String> included = new HashSet<>();
        fillDock(favoritePackages(), count, included);
        if (dockTiles.isEmpty()) {
            List<String> defaults = new ArrayList<>();
            Collections.addAll(defaults,
                    getResources().getStringArray(R.array.dock_packages));
            fillDock(defaults, count, included);
        }
        gridTiles.clear();
        for (Tile tile : allApps) {
            if (!included.contains(tile.packageName)) gridTiles.add(tile);
        }
        if (dockAdapter != null) dockAdapter.notifyDataSetChanged();
        if (gridAdapter != null) gridAdapter.notifyDataSetChanged();
        if (currentSection == DOCK && currentPosition >= dockTiles.size()) {
            currentPosition = Math.max(0, dockTiles.size() - 1);
        }
        if (currentSection == GRID && currentPosition >= gridTiles.size()) {
            currentPosition = Math.max(0, gridTiles.size() - 1);
        }
    }

    private void saveFavoritePackages(List<String> packages) {
        StringBuilder value = new StringBuilder();
        for (String packageName : packages) {
            if (value.length() > 0) value.append(',');
            value.append(packageName);
        }
        prefs().edit().putString(KEY_DOCK_FAVORITES, value.toString()).apply();
    }

    private Drawable loadBanner(PackageManager pm, ApplicationInfo app) {
        Drawable banner = pm.getApplicationBanner(app);
        return banner != null ? banner : pm.getApplicationIcon(app);
    }

    private void loadFavoriteChannels() {
        if (!featureEnabled("favorite_channels")) return;
        String[] configured = getResources().getStringArray(R.array.favorite_channels);
        List<TvController.Channel> available = tvController.getChannels();
        for (String item : configured) {
            String[] parts = item.split("\\|", 2);
            if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                Log.w(TAG, "Ignoring malformed favorite channel entry: " + item);
                continue;
            }
            TvController.Channel match = null;
            for (TvController.Channel channel : available) {
                if (parts[0].trim().equals(channel.number)) {
                    match = channel;
                    break;
                }
            }
            channelTiles.add(Tile.channel(parts[0].trim(), parts[1].trim(), match));
        }
        channelsEmpty.setVisibility(channelTiles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadWatchNext() {
        if (android.os.Build.VERSION.SDK_INT < 26 || !featureEnabled("watch_next")) return;
        IO.execute(() -> {
            List<WatchCandidate> candidates = new ArrayList<>();
            String[] projection = {
                    TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME,
                    TvContract.WatchNextPrograms.COLUMN_TITLE,
                    TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
                    TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS,
                    TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI,
                    TvContract.WatchNextPrograms.COLUMN_INTENT_URI,
                    TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS,
                    TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE,
                    TvContract.WatchNextPrograms.COLUMN_CONTENT_ID
            };
            try (android.database.Cursor cursor = getContentResolver().query(
                    TvContract.WatchNextPrograms.CONTENT_URI, projection, null, null,
                    TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS + " DESC")) {
                if (cursor != null) {
                    int packageIndex = cursor.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME);
                    int titleIndex = cursor.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_TITLE);
                    int positionIndex = cursor.getColumnIndex(
                            TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS);
                    int durationIndex = cursor.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS);
                    int posterIndex = cursor.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI);
                    int intentIndex = cursor.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_INTENT_URI);
                    int engagementIndex = cursor.getColumnIndex(
                            TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS);
                    int typeIndex = cursor.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE);
                    int contentIndex = cursor.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_CONTENT_ID);
                    while (cursor.moveToNext()) {
                        String packageName = cursor.getString(packageIndex);
                        if (packageName == null || !isInstalled(packageName)) continue;
                        String appName = packageName;
                        try {
                            appName = getPackageManager().getApplicationLabel(
                                    getPackageManager().getApplicationInfo(packageName, 0)).toString();
                        } catch (PackageManager.NameNotFoundException ignored) {
                            Log.w(TAG, "Watch Next package disappeared: " + packageName);
                        }
                        String title = cursor.getString(titleIndex);
                        long position = cursor.getLong(positionIndex);
                        long duration = cursor.getLong(durationIndex);
                        int type = typeIndex >= 0 ? cursor.getInt(typeIndex) : -1;
                        long engagement = engagementIndex >= 0 ? cursor.getLong(engagementIndex) : 0L;
                        String contentId = contentIndex >= 0 ? cursor.getString(contentIndex) : null;
                        candidates.add(new WatchCandidate(
                                Tile.watch(packageName, appName, title, cursor.getString(posterIndex),
                                        cursor.getString(intentIndex), position, duration),
                                engagement, position, duration, type, contentId));
                    }
                }
            } catch (SecurityException | IllegalArgumentException e) {
                Log.e(TAG, "READ_TV_LISTINGS denied for Watch Next", e);
            }

            Comparator<WatchCandidate> newestFirst = (left, right) ->
                    Long.compare(right.engagement, left.engagement);
            List<WatchCandidate> ongoing = new ArrayList<>();
            List<WatchCandidate> others = new ArrayList<>();
            for (WatchCandidate candidate : candidates) {
                if (candidate.type == TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE
                        && candidate.position > 0 && candidate.duration > 0) {
                    ongoing.add(candidate);
                } else {
                    others.add(candidate);
                }
            }
            ongoing.sort(newestFirst);
            others.sort(newestFirst);
            ongoing.addAll(others);

            List<Tile> results = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (WatchCandidate candidate : ongoing) {
                if (results.size() >= 5) break;
                String key = candidate.contentId;
                if (key == null || key.isEmpty()) {
                    key = candidate.tile.packageName + "|" + candidate.tile.title
                            + "|" + candidate.tile.artUri;
                }
                if (!seen.add(key)) continue;
                results.add(candidate.tile);
            }
            handler.post(() -> {
                watchTiles.clear();
                watchTiles.addAll(results);
                watchAdapter.notifyDataSetChanged();
                boolean hasItems = !results.isEmpty();
                watchTitle.setVisibility(hasItems ? View.VISIBLE : View.GONE);
                watch.setVisibility(hasItems ? View.VISIBLE : View.GONE);
                if (!hasItems && currentSection == WATCH && !dockTiles.isEmpty()) {
                    focusAt(DOCK, Math.min(currentPosition, dockTiles.size() - 1));
                } else if (hasItems && currentSection == WATCH
                        && currentPosition >= watchTiles.size()) {
                    focusAt(WATCH, watchTiles.size() - 1);
                }
            });
        });
    }

    private boolean isInstalled(String packageName) {
        try {
            getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void loadPreviewPrograms(String packageName) {
        posters.clear();
        posterPosition = 0;
        if (!featureEnabled("preview_programs") || previewProviderBlocked
                || android.os.Build.VERSION.SDK_INT < 26) {
            hidePoster();
            return;
        }
        IO.execute(() -> {
            List<PreviewPoster> results = new ArrayList<>();
            String[] projection = {
                    TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME,
                    TvContract.PreviewPrograms.COLUMN_TITLE,
                    TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI,
                    TvContract.PreviewPrograms.COLUMN_INTENT_URI
            };
            try (android.database.Cursor cursor = getContentResolver().query(
                    TvContract.PreviewPrograms.CONTENT_URI, projection, null, null,
                    TvContract.PreviewPrograms.COLUMN_WEIGHT + " DESC")) {
                if (cursor != null) {
                    int packageIndex = cursor.getColumnIndex(TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME);
                    int titleIndex = cursor.getColumnIndex(TvContract.PreviewPrograms.COLUMN_TITLE);
                    int posterIndex = cursor.getColumnIndex(TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI);
                    int intentIndex = cursor.getColumnIndex(TvContract.PreviewPrograms.COLUMN_INTENT_URI);
                    while (cursor.moveToNext() && results.size() < 20) {
                        if (!packageName.equals(cursor.getString(packageIndex))) continue;
                        String art = cursor.getString(posterIndex);
                        if (art == null || art.isEmpty()) continue;
                        results.add(new PreviewPoster(packageName, cursor.getString(titleIndex),
                                art, cursor.getString(intentIndex)));
                    }
                }
            } catch (SecurityException e) {
                previewProviderBlocked = true;
                Log.e(TAG, "READ_TV_LISTINGS denied for preview programs", e);
            } catch (IllegalArgumentException e) {
                previewProviderBlocked = true;
                Log.e(TAG, "Preview-program provider query failed", e);
            }
            handler.post(() -> {
                if (currentSection == DOCK || currentSection == GRID) {
                    Tile focused = currentTile();
                    if (focused != null && packageName.equals(focused.packageName)) {
                        posters.clear();
                        posters.addAll(results);
                        if (posters.isEmpty()) hidePoster();
                        else {
                            posterPosition = 0;
                            showPoster(0);
                            handler.removeCallbacks(posterNextRunnable);
                            handler.postDelayed(posterNextRunnable, 6_000L);
                        }
                    }
                }
            });
        });
    }

    private void restoreFocus() {
        SharedPreferences prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        int section = prefs.getInt("focus_section", DOCK);
        int position = prefs.getInt("focus_position", 0);
        if (!sectionAvailable(section)) section = DOCK;
        currentSection = section;
        focusAt(section, position);
    }

    private void focusAt(int section, int position) {
        if (!sectionAvailable(section)) return;
        int target = Math.max(0, Math.min(position, sectionCount(section) - 1));
        currentSection = section;
        currentPosition = target;
        if (section == PILLS) {
            View targetView = pillViews.get(target);
            targetView.requestFocus();
            if (targetView.hasFocus()) onTileFocus(targetView, true, 0xFF8BBFFF);
            return;
        }
        RecyclerView list = recycler(section);
        if (section == DOCK) {
            homeScroll.post(() -> {
                homeScroll.scrollTo(0, 0);
                list.scrollToPosition(target);
                list.post(() -> {
                    RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(target);
                    if (holder != null) {
                        holder.itemView.requestFocus();
                        onTileFocus(holder.itemView, true,
                                (Integer) holder.itemView.getTag(R.id.focus_accent));
                    }
                });
            });
            return;
        }
        if (section == GRID) {
            homeScroll.post(() -> {
                homeScroll.scrollTo(0, gridLabel.getTop());
                list.scrollToPosition(target);
                list.post(() -> {
                    RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(target);
                    if (holder != null) {
                        holder.itemView.requestFocus();
                        homeScroll.requestChildFocus(list, holder.itemView);
                        onTileFocus(holder.itemView, true, (Integer) holder.itemView.getTag(R.id.focus_accent));
                    }
                });
            });
            return;
        }
        if (section == WATCH) {
            homeScroll.post(() -> {
                homeScroll.scrollTo(0, watchTitle.getTop());
                list.scrollToPosition(target);
                list.post(() -> {
                    RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(target);
                    if (holder != null) {
                        holder.itemView.requestFocus();
                        homeScroll.requestChildFocus(list, holder.itemView);
                        onTileFocus(holder.itemView, true, (Integer) holder.itemView.getTag(R.id.focus_accent));
                    }
                });
            });
            return;
        }
        list.scrollToPosition(target);
        list.post(() -> {
            RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(target);
            if (holder != null) {
                holder.itemView.requestFocus();
                onTileFocus(holder.itemView, true, (Integer) holder.itemView.getTag(R.id.focus_accent));
            }
        });
    }

    private boolean sectionAvailable(int section) {
        return sectionCount(section) > 0;
    }

    private int sectionCount(int section) {
        switch (section) {
            case PILLS: return pillViews.size();
            case DOCK: return dockTiles.size();
            case WATCH: return watchTiles.size();
            case CHANNELS: return channelTiles.size();
            case GRID: return gridTiles.size();
            default: return 0;
        }
    }

    private RecyclerView recycler(int section) {
        switch (section) {
            case DOCK: return dock;
            case WATCH: return watch;
            case CHANNELS: return channels;
            case GRID: return grid;
            default: throw new IllegalArgumentException("Section has no RecyclerView: " + section);
        }
    }

    private List<Integer> availableSections() {
        List<Integer> result = new ArrayList<>();
        for (int section : new int[]{PILLS, DOCK, WATCH, GRID}) {
            if (sectionAvailable(section)) result.add(section);
        }
        return result;
    }

    private void moveVertical(boolean down) {
        if (currentSection == GRID) {
            int target = currentPosition + (down ? gridColumns : -gridColumns);
            if (target >= 0 && target < gridTiles.size()) {
                focusAt(GRID, target);
                return;
            }
            if (down) return;
        }
        List<Integer> sections = availableSections();
        int sectionIndex = sections.indexOf(currentSection);
        int nextIndex = sectionIndex + (down ? 1 : -1);
        if (nextIndex < 0 || nextIndex >= sections.size()) return;
        int targetSection = sections.get(nextIndex);
        int targetPosition = nearestPosition(currentSection, currentPosition, targetSection);
        focusAt(targetSection, targetPosition);
    }

    private int nearestPosition(int sourceSection, int sourcePosition, int targetSection) {
        View source = itemView(sourceSection, sourcePosition);
        int count = sectionCount(targetSection);
        if (source == null || count <= 1) return 0;
        int[] sourceLocation = new int[2];
        int[] sourceRowLocation = new int[2];
        source.getLocationOnScreen(sourceLocation);
        rowView(sourceSection).getLocationOnScreen(sourceRowLocation);
        float fraction = (sourceLocation[0] + source.getWidth() / 2f - sourceRowLocation[0])
                / Math.max(1f, rowView(sourceSection).getWidth());
        if (targetSection == GRID) {
            return Math.max(0, Math.min(count - 1,
                    Math.round(fraction * (gridColumns - 1))));
        }
        return Math.max(0, Math.min(count - 1, Math.round(fraction * (count - 1))));
    }

    private View itemView(int section, int position) {
        if (section == PILLS) return pillViews.get(position);
        return recycler(section).findViewHolderForAdapterPosition(position) == null ? null
                : recycler(section).findViewHolderForAdapterPosition(position).itemView;
    }

    private View rowView(int section) {
        return section == PILLS ? pillRow : recycler(section);
    }

    private void onTileFocus(View view, boolean focused, int accent) {
        if (!focused) {
            view.animate().scaleX(1f).scaleY(1f).setDuration(150L).start();
            setTileLabelVisibility(view, false);
            if (highlightedTile == view) highlightedTile = null;
            return;
        }
        Object sectionTag = view.getTag(R.id.focus_section);
        Object positionTag = view.getTag(R.id.focus_position);
        if (!(sectionTag instanceof Integer) || !(positionTag instanceof Integer)) return;
        if (highlightedTile != null && highlightedTile != view) {
            setTileLabelVisibility(highlightedTile, false);
            highlightedTile.animate().scaleX(1f).scaleY(1f).setDuration(150L).start();
        }
        highlightedTile = view;
        setTileLabelVisibility(view, true);
        currentSection = (Integer) sectionTag;
        currentPosition = (Integer) positionTag;
        if (currentSection != PILLS) {
            view.animate().scaleX(1.045f).scaleY(1.045f).setDuration(180L).start();
        }
        getSharedPreferences("launcher", MODE_PRIVATE).edit()
                .putInt("focus_section", currentSection)
                .putInt("focus_position", currentPosition).apply();
        updateHighlight(view, accent);
        Tile focusedTile = currentTile();
        if (currentSection == DOCK && focusedTile != null) {
            focusedDockTitle.setText(focusedTile.title);
            focusedDockTitle.setShadowLayer(dp(22), 0, 0, withAlpha(focusedTile.accent, 0x99));
            scheduleDockPreview(focusedTile);
        } else {
            focusedDockTitle.setText("");
            hideDockPreview();
        }
        hidePoster();
    }

    private void setTileLabelVisibility(View tileView, boolean visible) {
        Object sectionTag = tileView.getTag(R.id.focus_section);
        if (!(sectionTag instanceof Integer)) return;
        int section = (Integer) sectionTag;
        if ((section == DOCK || section == GRID) && tileView instanceof LinearLayout) {
            View label = ((LinearLayout) tileView).getChildAt(1);
            if (label != null) label.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void updateHighlight(View target, int accent) {
        int[] targetPosition = new int[2];
        int[] rootPosition = new int[2];
        target.getLocationOnScreen(targetPosition);
        root.getLocationOnScreen(rootPosition);
        ViewGroup.LayoutParams params = glassHighlight.getLayoutParams();
        params.width = target.getWidth();
        params.height = target.getHeight();
        glassHighlight.setLayoutParams(params);
        glassHighlight.setBackground(glassDrawable(accent));
        glassHighlight.setVisibility(View.VISIBLE);
        glassHighlight.animate().cancel();
        glassHighlight.animate().translationX(targetPosition[0] - rootPosition[0])
                .translationY(targetPosition[1] - rootPosition[1]).setDuration(240L)
                .setInterpolator(new OvershootInterpolator(0.7f)).start();
        glassHighlight.bringToFront();
    }

    private GradientDrawable glassBackground(int accent) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x3FFFFFFF, 0x20FFFFFF, 0x0AFFFFFF});
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), withAlpha(accent, 0x66));
        return drawable;
    }

    private GradientDrawable glassDrawable(int accent) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x45FFFFFF, 0x22FFFFFF, 0x0AFFFFFF});
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(2), withAlpha(accent, 0xE0));
        return drawable;
    }

    private GradientDrawable glassFocusedBackground(int accent) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x664A7DB8, 0x4430598C, 0x22203F63});
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(2), withAlpha(accent, 0xE8));
        return drawable;
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int accentOf(Drawable drawable) {
        if (drawable == null) return 0xFF86BFFF;
        Bitmap bitmap = Bitmap.createBitmap(12, 12, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, 12, 12);
        drawable.draw(canvas);
        long red = 0;
        long green = 0;
        long blue = 0;
        int samples = 0;
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 12; x++) {
                int color = bitmap.getPixel(x, y);
                if (Color.alpha(color) < 80) continue;
                red += Color.red(color);
                green += Color.green(color);
                blue += Color.blue(color);
                samples++;
            }
        }
        bitmap.recycle();
        return samples == 0 ? 0xFF86BFFF
                : Color.rgb((int) (red / samples), (int) (green / samples), (int) (blue / samples));
    }

    private void activatePill(int position) {
        if (position == 0) openInputMenu();
        else if (position == 1) {
            try {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Wi-Fi settings activity is unavailable", e);
            }
        } else if (position == 2) {
            openQuickSettingsMenu();
        } else {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Settings activity is unavailable", e);
            }
        }
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private String locationName() {
        return prefs().getString(KEY_LOCATION_NAME, getString(R.string.prayer_location));
    }

    private String locationLatitude() {
        return prefs().getString(KEY_LOCATION_LAT, getString(R.string.prayer_latitude));
    }

    private String locationLongitude() {
        return prefs().getString(KEY_LOCATION_LON, getString(R.string.prayer_longitude));
    }

    private String locationTimezone() {
        return prefs().getString(KEY_LOCATION_TIMEZONE, getString(R.string.prayer_timezone));
    }

    private String wallpaperKind() {
        String stored = prefs().getString(KEY_WALLPAPER_KIND, null);
        if (stored != null) return stored;
        return getString(R.string.wallpaper_uri).isEmpty() ? "preset" : "image";
    }

    private String wallpaperValue() {
        String stored = prefs().getString(KEY_WALLPAPER_VALUE, null);
        if (stored != null) return stored;
        String legacy = getString(R.string.wallpaper_uri);
        return legacy.isEmpty() ? "senja" : legacy;
    }

    private void saveWallpaper(String kind, String value) {
        prefs().edit().putString(KEY_WALLPAPER_KIND, kind)
                .putString(KEY_WALLPAPER_VALUE, value).apply();
    }

    private void applyWallpaper() {
        if (wallpaperImage == null) return;
        int generation = ++wallpaperGeneration;
        String kind = wallpaperKind();
        String value = wallpaperValue();
        wallpaperImage.animate().cancel();
        wallpaperImage.setImageDrawable(null);
        wallpaperImage.setBackgroundColor(Color.BLACK);
        if ("preset".equals(kind)) {
            wallpaperImage.setScaleType(ImageView.ScaleType.FIT_XY);
            wallpaperImage.setBackground(WallpaperCatalog.create(value));
        } else {
            wallpaperImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (!value.isEmpty()) {
                loadImage(value, new ImageCallback() {
                    @Override
                    public void onImage(Bitmap bitmap) {
                        if (generation != wallpaperGeneration) return;
                        wallpaperImage.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onError(String message) {
                        if (generation != wallpaperGeneration) return;
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        updateHomeScrim();
        if (!externalInputVisible) wallpaperImage.setVisibility(View.VISIBLE);
    }

    private void updateHomeScrim() {
        if (homeScrim == null) return;
        boolean hasWallpaper = !("preset".equals(wallpaperKind())
                && WallpaperCatalog.isBlack(wallpaperValue()));
        boolean previewVisible = dockPreviewLayer != null
                && dockPreviewLayer.getVisibility() == View.VISIBLE;
        int[] colors;
        if (previewVisible) {
            colors = new int[]{0xB3050812, 0xA6050812, 0xE6000000};
        } else {
            colors = hasWallpaper
                    ? new int[]{0x40050812, 0x3D050812, 0xC9000000}
                    : new int[]{0x66050812, 0x65050812, 0xD9000000};
        }
        homeScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                colors));
    }

    private void openQuickSettingsMenu() {
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        labels.add("Lokasi");
        actions.add(this::openLocationMenu);
        labels.add("Aplikasi favorit");
        actions.add(this::openFavoritesMenu);
        labels.add("Jumlah dock  " + dockCount() + " aplikasi");
        actions.add(this::openDockCountMenu);
        labels.add("Wallpaper");
        actions.add(this::openWallpaperMenu);
        labels.add("Screensaver");
        actions.add(this::openScreensaverMenu);
        labels.add("Ayat & Hadis");
        actions.add(this::openVerseMenu);
        labels.add("Nama sapaan");
        actions.add(this::openGreetingNameDialog);
        boolean preview = prefs().getBoolean(KEY_DOCK_PREVIEW, true);
        labels.add("Preview dock  " + (preview ? "Ya" : "Tidak"));
        actions.add(() -> {
            boolean next = !prefs().getBoolean(KEY_DOCK_PREVIEW, true);
            prefs().edit().putBoolean(KEY_DOCK_PREVIEW, next).apply();
            if (next) {
                Tile tile = currentTile();
                if (currentSection == DOCK && tile != null) scheduleDockPreview(tile);
            } else {
                hideDockPreview();
            }
            Toast.makeText(this, next ? "Preview dock aktif" : "Preview dock dimatikan",
                    Toast.LENGTH_SHORT).show();
        });
        showChoicePopup(quickPill, labels, actions, 360);
    }

    private void showChoicePopup(View anchor, List<String> labels, List<Runnable> actions, int widthDp) {
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(10), dp(8), dp(10), dp(8));
        for (int i = 0; i < labels.size(); i++) {
            final Runnable action = actions.get(i);
            TextView choice = text(labels.get(i), 15, Color.WHITE, true);
            choice.setPadding(dp(12), 0, dp(12), 0);
            choice.setFocusable(true);
            choice.setBackground(glassBackground(0xFF9AC8FF));
            choice.setOnFocusChangeListener((view, focused) -> view.setBackground(focused
                    ? glassFocusedBackground(0xFFC4E0FF)
                    : glassBackground(0xFF9AC8FF)));
            LinearLayout.LayoutParams choiceParams = new LinearLayout.LayoutParams(-1, dp(46));
            choiceParams.setMargins(0, dp(2), 0, dp(2));
            menu.addView(choice, choiceParams);
            choice.setOnClickListener(view -> {
                PopupWindow active = menuPopup;
                if (active != null) active.dismiss();
                handler.postDelayed(action, 90L);
            });
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(menu, new ScrollView.LayoutParams(-1, -2));
        PopupWindow popup = new PopupWindow(scroll, dp(widthDp),
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        menuPopup = popup;
        popup.setOnDismissListener(() -> {
            if (menuPopup == popup) menuPopup = null;
            resetScreensaverTimer();
        });
        popup.setBackgroundDrawable(glassBackground(0xFF9AC8FF));
        popup.setOutsideTouchable(true);
        popup.setElevation(dp(14));
        popup.showAsDropDown(anchor, 0, dp(4));
        if (menu.getChildCount() > 0) menu.getChildAt(0).requestFocus();
    }

    private void showChoiceDialog(String title, List<String> labels, List<Runnable> actions,
                                  int widthDp, int heightDp) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.setBackground(glassBackground(0xFF9AC8FF));

        TextView heading = text(title, 18, Color.WHITE, true);
        heading.setPadding(dp(12), 0, dp(12), dp(6));
        panel.addView(heading, new LinearLayout.LayoutParams(-1, dp(38)));

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        final TextView[] first = new TextView[1];
        for (int i = 0; i < labels.size(); i++) {
            final Runnable action = actions.get(i);
            TextView choice = text(labels.get(i), 15, Color.WHITE, true);
            choice.setPadding(dp(12), 0, dp(12), 0);
            choice.setFocusable(true);
            choice.setBackground(glassBackground(0xFF9AC8FF));
            choice.setOnFocusChangeListener((view, focused) -> view.setBackground(focused
                    ? glassFocusedBackground(0xFFC4E0FF)
                    : glassBackground(0xFF9AC8FF)));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(48));
            rowParams.setMargins(0, dp(2), 0, dp(2));
            menu.addView(choice, rowParams);
            choice.setOnClickListener(view -> {
                Dialog active = choiceDialog;
                if (active != null) active.dismiss();
                handler.postDelayed(action, 90L);
            });
            applyCenterClick(choice);
            if (first[0] == null) first[0] = choice;
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(menu, new ScrollView.LayoutParams(-1, -2));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, dp(heightDp)));

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(panel, new ViewGroup.LayoutParams(
                dp(widthDp), ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setCanceledOnTouchOutside(true);
        choiceDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            if (choiceDialog == dialog) choiceDialog = null;
            resetScreensaverTimer();
        });
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.62f;
            window.setAttributes(attributes);
        }
        dialog.show();
        if (window != null) {
            window.setLayout(dp(widthDp), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (first[0] != null) first[0].requestFocus();
    }

    private void openLocationMenu() {
        String[] entries = getResources().getStringArray(R.array.location_presets);
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        String currentLat = locationLatitude();
        String currentLon = locationLongitude();
        for (String entry : entries) {
            String[] parts = entry.split("\\|", 4);
            if (parts.length != 4) continue;
            final String label = parts[0].trim();
            final String latitude = parts[1].trim();
            final String longitude = parts[2].trim();
            final String timezone = parts[3].trim();
            boolean selected = sameCoordinate(currentLat, latitude)
                    && sameCoordinate(currentLon, longitude);
            labels.add((selected ? "●  " : "○  ") + label);
            actions.add(() -> applyLocation(label, latitude, longitude, timezone));
        }
        labels.add("Lokasi perangkat");
        actions.add(this::useDeviceLocation);
        showChoicePopup(quickPill, labels, actions, 400);
    }

    private boolean sameCoordinate(String first, String second) {
        try {
            return Math.abs(Double.parseDouble(first) - Double.parseDouble(second)) < 0.001;
        } catch (NumberFormatException e) {
            return first.equals(second);
        }
    }

    private void applyLocation(String name, String latitude, String longitude, String timezone) {
        prefs().edit().putString(KEY_LOCATION_NAME, name)
                .putString(KEY_LOCATION_LAT, latitude)
                .putString(KEY_LOCATION_LON, longitude)
                .putString(KEY_LOCATION_TIMEZONE, timezone).apply();
        if (prayerEyebrow != null) {
            prayerEyebrow.setText(name.toUpperCase(new Locale("id", "ID")) + "  ·  JADWAL SHOLAT");
        }
        if (screensaverLocation != null) {
            screensaverLocation.setText(name.toUpperCase(new Locale("id", "ID")) + "  ·  JADWAL SHOLAT");
        }
        prayerTimes = null;
        if (prayerStatus != null) prayerStatus.setText("Memuat jadwal…");
        if (nextPrayer != null) nextPrayer.setText("Memuat jadwal…");
        refreshPrayerTimes();
        if (screensaverVisible) renderScreensaver();
    }

    private void useDeviceLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION);
            return;
        }
        Location best = null;
        try {
            LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (manager != null) {
                for (String provider : manager.getProviders(true)) {
                    Location candidate = manager.getLastKnownLocation(provider);
                    if (candidate != null && (best == null
                            || candidate.getTime() > best.getTime())) {
                        best = candidate;
                    }
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Location permission denied while reading device location", e);
        }
        if (best == null) {
            Toast.makeText(this, "Lokasi perangkat belum tersedia. Pilih kota dari daftar.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String latitude = String.format(Locale.US, "%.4f", best.getLatitude());
        String longitude = String.format(Locale.US, "%.4f", best.getLongitude());
        applyLocation("Lokasi perangkat (" + latitude + ", " + longitude + ")",
                latitude, longitude, TimeZone.getDefault().getID());
    }

    private void openFavoritesMenu() {
        if (allApps.isEmpty()) {
            Toast.makeText(this, "Belum ada aplikasi yang terpasang", Toast.LENGTH_LONG).show();
            return;
        }
        final List<String> selected = new ArrayList<>();
        for (String packageName : favoritePackages()) {
            if (appTile(packageName) != null && !selected.contains(packageName)) {
                selected.add(packageName);
            }
        }

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        panel.setBackground(glassBackground(0xFF9AC8FF));

        TextView heading = text("Aplikasi favorit", 20, Color.WHITE, true);
        heading.setPadding(dp(6), 0, dp(6), 0);
        panel.addView(heading, new LinearLayout.LayoutParams(-1, dp(36)));

        TextView hint = text("Pilih maksimal 6. Urutan centang menjadi urutan dock.",
                12, 0xFFC0CBD9, false);
        hint.setPadding(dp(6), 0, dp(6), 0);
        panel.addView(hint, new LinearLayout.LayoutParams(-1, dp(26)));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        final List<TextView> rows = new ArrayList<>();
        for (final Tile tile : allApps) {
            final TextView row = text("", 15, Color.WHITE, true);
            row.setPadding(dp(14), 0, dp(14), 0);
            row.setFocusable(true);
            row.setBackground(glassBackground(0xFF9AC8FF));
            row.setOnFocusChangeListener((view, focused) -> view.setBackground(focused
                    ? glassFocusedBackground(0xFFC4E0FF)
                    : glassBackground(0xFF9AC8FF)));
            row.setOnClickListener(view -> {
                int index = selected.indexOf(tile.packageName);
                if (index >= 0) {
                    selected.remove(index);
                    refreshFavoriteRows(rows, selected);
                    return;
                }
                if (selected.size() >= MAX_DOCK_APPS) {
                    Toast.makeText(this, "Maksimal 6 aplikasi favorit", Toast.LENGTH_SHORT).show();
                    return;
                }
                selected.add(tile.packageName);
                refreshFavoriteRows(rows, selected);
            });
            applyCenterClick(row);
            rows.add(row);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(46));
            rowParams.setMargins(0, dp(2), 0, dp(2));
            list.addView(row, rowParams);
        }
        refreshFavoriteRows(rows, selected);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, dp(330)));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        buttonParams.topMargin = dp(8);
        TextView save = dialogButton("Simpan");
        buttons.addView(save, buttonParams);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        cancelParams.topMargin = dp(8);
        cancelParams.leftMargin = dp(8);
        TextView cancel = dialogButton("Batal");
        buttons.addView(cancel, cancelParams);
        panel.addView(buttons, new LinearLayout.LayoutParams(-1, dp(54)));

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(panel, new ViewGroup.LayoutParams(
                dp(480), ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setCanceledOnTouchOutside(true);
        favoritesDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            if (favoritesDialog == dialog) favoritesDialog = null;
            resetScreensaverTimer();
        });

        save.setOnClickListener(view -> {
            if (selected.isEmpty()) {
                Toast.makeText(this, "Pilih minimal satu aplikasi", Toast.LENGTH_SHORT).show();
                return;
            }
            saveFavoritePackages(selected);
            rebuildDockAndGrid();
            dialog.dismiss();
            Toast.makeText(this, "Aplikasi favorit diperbarui", Toast.LENGTH_SHORT).show();
        });
        cancel.setOnClickListener(view -> dialog.dismiss());

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.66f;
            window.setAttributes(attributes);
        }
        dialog.show();
        if (window != null) {
            window.setLayout(dp(480), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (!rows.isEmpty()) rows.get(0).requestFocus();
    }

    private void refreshFavoriteRows(List<TextView> rows, List<String> selected) {
        for (int i = 0; i < rows.size() && i < allApps.size(); i++) {
            Tile tile = allApps.get(i);
            int order = selected.indexOf(tile.packageName);
            String prefix = order >= 0 ? (order + 1) + ".  ●  " : "○  ";
            rows.get(i).setText(prefix + tile.title);
        }
    }

    private TextView dialogButton(String label) {
        TextView button = text(label, 15, Color.WHITE, true);
        button.setGravity(Gravity.CENTER);
        button.setFocusable(true);
        button.setBackground(glassBackground(0xFF9AC8FF));
        button.setOnFocusChangeListener((view, focused) -> view.setBackground(focused
                ? glassFocusedBackground(0xFFC4E0FF)
                : glassBackground(0xFF9AC8FF)));
        applyCenterClick(button);
        return button;
    }

    private void openDockCountMenu() {
        int current = dockCount();
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (int i = 1; i <= MAX_DOCK_APPS; i++) {
            final int count = i;
            labels.add((i == current ? "●  " : "○  ") + i + " aplikasi");
            actions.add(() -> {
                prefs().edit().putInt(KEY_DOCK_COUNT, count).apply();
                rebuildDockAndGrid();
                Toast.makeText(this, "Jumlah dock: " + count + " aplikasi",
                        Toast.LENGTH_SHORT).show();
            });
        }
        showChoicePopup(quickPill, labels, actions, 320);
    }

    private void openGreetingNameDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(true);
        input.setHint("Contoh: Budi");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(24)});
        String current = prefs().getString(KEY_GREETING_NAME, "");
        input.setText(current);
        input.setSelection(input.length());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nama setelah sapaan")
                .setMessage("Kosongkan untuk sapaan tanpa nama.")
                .setView(input)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();
        nameDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            if (nameDialog == dialog) nameDialog = null;
            resetScreensaverTimer();
        });
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    prefs().edit().putString(KEY_GREETING_NAME,
                            input.getText().toString().trim()).apply();
                    renderClock();
                    dialog.dismiss();
                }));
        dialog.show();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        input.requestFocus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_LOCATION) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            useDeviceLocation();
        } else {
            Toast.makeText(this, "Izin lokasi ditolak. Pilih kota dari daftar.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openWallpaperMenu() {
        List<WallpaperCatalog.Preset> presets = WallpaperCatalog.presets();
        String kind = wallpaperKind();
        String value = wallpaperValue();

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        panel.setBackground(glassBackground(0xFF9AC8FF));

        TextView heading = text("Pilih wallpaper", 20, Color.WHITE, true);
        heading.setPadding(dp(6), 0, dp(6), dp(4));
        panel.addView(heading, new LinearLayout.LayoutParams(-1, dp(38)));

        RecyclerView gallery = new RecyclerView(this);
        gallery.setClipToPadding(false);
        gallery.setPadding(dp(2), dp(2), dp(2), dp(2));
        gallery.setLayoutManager(new GridLayoutManager(this, 3));
        gallery.setNestedScrollingEnabled(false);
        gallery.setAdapter(new WallpaperAdapter(presets, kind, value));
        int rows = (presets.size() + 2) / 3;
        panel.addView(gallery, new LinearLayout.LayoutParams(-1, dp(128 * rows)));

        TextView deviceChoice = text("Gambar dari perangkat", 15, Color.WHITE, true);
        deviceChoice.setPadding(dp(14), 0, dp(14), 0);
        deviceChoice.setFocusable(true);
        deviceChoice.setBackground(glassBackground(0xFF9AC8FF));
        deviceChoice.setOnFocusChangeListener((view, focused) -> view.setBackground(focused
                ? glassFocusedBackground(0xFFC4E0FF) : glassBackground(0xFF9AC8FF)));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(46));
        actionParams.topMargin = dp(6);
        panel.addView(deviceChoice, actionParams);
        deviceChoice.setOnClickListener(view -> {
            Dialog active = wallpaperGalleryDialog;
            if (active != null) active.dismiss();
            handler.postDelayed(this::pickWallpaperImage, 90L);
        });
        applyCenterClick(deviceChoice);

        TextView urlChoice = text("URL gambar", 15, Color.WHITE, true);
        urlChoice.setPadding(dp(14), 0, dp(14), 0);
        urlChoice.setFocusable(true);
        urlChoice.setBackground(glassBackground(0xFF9AC8FF));
        urlChoice.setOnFocusChangeListener((view, focused) -> view.setBackground(focused
                ? glassFocusedBackground(0xFFC4E0FF) : glassBackground(0xFF9AC8FF)));
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(-1, dp(46));
        urlParams.topMargin = dp(4);
        panel.addView(urlChoice, urlParams);
        urlChoice.setOnClickListener(view -> {
            Dialog active = wallpaperGalleryDialog;
            if (active != null) active.dismiss();
            handler.postDelayed(this::openWallpaperUrlDialog, 90L);
        });
        applyCenterClick(urlChoice);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(panel, new ViewGroup.LayoutParams(
                dp(760), ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setCanceledOnTouchOutside(true);
        wallpaperGalleryDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            if (wallpaperGalleryDialog == dialog) wallpaperGalleryDialog = null;
            resetScreensaverTimer();
        });
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.66f;
            window.setAttributes(attributes);
        }
        dialog.show();
        if (window != null) {
            window.setLayout(dp(760), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        gallery.post(() -> {
            if (gallery.getChildAt(0) != null) gallery.getChildAt(0).requestFocus();
        });
    }

    private final class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.Holder> {
        private final List<WallpaperCatalog.Preset> presets;
        private final String kind;
        private final String value;
        private final Bitmap[] thumbnails;

        WallpaperAdapter(List<WallpaperCatalog.Preset> presets, String kind, String value) {
            this.presets = presets;
            this.kind = kind;
            this.value = value;
            thumbnails = new Bitmap[presets.size()];
            for (int i = 0; i < presets.size(); i++) {
                thumbnails[i] = WallpaperCatalog.thumbnail(presets.get(i).id, dp(210), dp(96));
            }
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(MainActivity.this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setFocusable(true);
            card.setFocusableInTouchMode(true);
            card.setPadding(dp(5), dp(5), dp(5), dp(4));
            card.setBackground(glassBackground(0xFF9AC8FF));

            ImageView image = new ImageView(MainActivity.this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(image, new LinearLayout.LayoutParams(-1, dp(92)));

            TextView label = text("", 12, Color.WHITE, true);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(1);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            card.addView(label, new LinearLayout.LayoutParams(-1, dp(22)));
            applyCenterClick(card);
            return new Holder(card, image, label);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            WallpaperCatalog.Preset preset = presets.get(position);
            boolean selected = "preset".equals(kind) && preset.id.equals(value);
            holder.image.setImageBitmap(thumbnails[position]);
            holder.label.setText((selected ? "●  " : "") + preset.name);
            holder.itemView.setOnFocusChangeListener((view, focused) -> view.setBackground(focused
                    ? glassFocusedBackground(0xFFC4E0FF) : glassBackground(0xFF9AC8FF)));
            holder.itemView.setOnClickListener(view -> {
                saveWallpaper("preset", preset.id);
                applyWallpaper();
                Dialog active = wallpaperGalleryDialog;
                if (active != null) active.dismiss();
            });
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, dp(128));
            params.setMargins(dp(4), dp(2), dp(4), dp(2));
            holder.itemView.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            return presets.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            final TextView label;

            Holder(View itemView, ImageView image, TextView label) {
                super(itemView);
                this.image = image;
                this.label = label;
            }
        }
    }

    private void pickWallpaperImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try {
            startActivityForResult(intent, REQUEST_IMAGE);
        } catch (ActivityNotFoundException e) {
            intent.setAction(Intent.ACTION_GET_CONTENT);
            try {
                startActivityForResult(intent, REQUEST_IMAGE);
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, "Pemilih gambar tidak tersedia di TV ini.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMAGE || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        importWallpaper(uri);
    }

    private void importWallpaper(Uri source) {
        IO.execute(() -> {
            File target = new File(getFilesDir(), "wallpaper-import.img");
            try (InputStream input = getContentResolver().openInputStream(source);
                 OutputStream output = new FileOutputStream(target)) {
                if (input == null) throw new IOException("No image data for " + source);
                byte[] buffer = new byte[8_192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                handler.post(() -> {
                    saveWallpaper("image", Uri.fromFile(target).toString());
                    applyWallpaper();
                });
            } catch (IOException | SecurityException e) {
                Log.e(TAG, "Wallpaper image could not be imported: " + source, e);
                handler.post(() -> Toast.makeText(this, "Gambar tidak dapat dibaca",
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void openWallpaperUrlDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(true);
        input.setHint("https://contoh.com/wallpaper.jpg");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        if ("image".equals(wallpaperKind()) && wallpaperValue().startsWith("http")) {
            input.setText(wallpaperValue());
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("URL gambar wallpaper")
                .setView(input)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();
        wallpaperDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            if (wallpaperDialog == dialog) wallpaperDialog = null;
            resetScreensaverTimer();
        });
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String value = input.getText().toString().trim();
                    if (!value.toLowerCase(Locale.ROOT).startsWith("https://")) {
                        input.setError("Gunakan URL https://");
                        return;
                    }
                    saveWallpaper("image", value);
                    applyWallpaper();
                    dialog.dismiss();
                }));
        dialog.show();
        dialog.getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        input.requestFocus();
    }

    private void openScreensaverMenu() {
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        labels.add("Mulai sekarang");
        actions.add(this::showScreensaver);
        final boolean auto = screensaverAutoEnabled();
        labels.add("Aktif otomatis  " + (auto ? "Ya" : "Tidak"));
        actions.add(() -> {
            boolean next = !screensaverAutoEnabled();
            prefs().edit().putBoolean(KEY_SCREENSAVER_AUTO, next).apply();
            if (next) resetScreensaverTimer();
            else cancelScreensaverTimer();
            Toast.makeText(this, next ? "Screensaver otomatis aktif" : "Screensaver otomatis dijeda",
                    Toast.LENGTH_SHORT).show();
        });
        labels.add("Waktu tunggu  " + Math.max(1, screensaverTimeoutSeconds() / 60) + " menit");
        actions.add(this::openScreensaverTimeoutMenu);
        showChoicePopup(quickPill, labels, actions, 340);
    }

    private void openScreensaverTimeoutMenu() {
        final int[] minutes = {1, 2, 5, 10};
        int current = Math.max(1, screensaverTimeoutSeconds() / 60);
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (int value : minutes) {
            labels.add((value == current ? "●  " : "○  ") + value + " menit");
            actions.add(() -> {
                prefs().edit().putInt(KEY_SCREENSAVER_TIMEOUT, value * 60).apply();
                resetScreensaverTimer();
                Toast.makeText(this, "Waktu tunggu screensaver " + value + " menit",
                        Toast.LENGTH_SHORT).show();
            });
        }
        showChoicePopup(quickPill, labels, actions, 300);
    }

    private void openVerseMenu() {
        List<VerseCatalog.Entry> entries = VerseCatalog.entries();
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        final boolean auto = prefs().getBoolean(KEY_VERSE_AUTO, true);
        String selectedId = prefs().getString(KEY_VERSE_ID, "");
        labels.add((auto ? "●  " : "○  ") + "Otomatis harian");
        actions.add(() -> {
            prefs().edit().putBoolean(KEY_VERSE_AUTO, true).apply();
            renderVerse();
        });
        for (VerseCatalog.Entry entry : entries) {
            boolean selected = !auto && entry.id.equals(selectedId);
            labels.add((selected ? "●  " : "○  ") + entry.typeName() + " · " + entry.source);
            actions.add(() -> {
                prefs().edit().putBoolean(KEY_VERSE_AUTO, false)
                        .putString(KEY_VERSE_ID, entry.id).apply();
                renderVerse();
            });
        }
        showChoiceDialog("Ayat & Hadis", labels, actions, 460, 500);
    }

    private boolean screensaverAutoEnabled() {
        return prefs().getBoolean(KEY_SCREENSAVER_AUTO, true);
    }

    private int screensaverTimeoutSeconds() {
        return Math.max(30, prefs().getInt(KEY_SCREENSAVER_TIMEOUT,
                getResources().getInteger(R.integer.screensaver_timeout_seconds)));
    }

    private void scheduleScreensaver() {
        cancelScreensaverTimer();
        if (!featureEnabled("screensaver") || !screensaverAutoEnabled()
                || screensaverVisible || externalInputVisible) return;
        handler.postDelayed(screensaverTimeoutRunnable, screensaverTimeoutSeconds() * 1_000L);
    }

    private void resetScreensaverTimer() {
        cancelScreensaverTimer();
        scheduleScreensaver();
    }

    private void cancelScreensaverTimer() {
        handler.removeCallbacks(screensaverTimeoutRunnable);
    }

    private void buildScreensaverLayer() {
        FrameLayout layer = new FrameLayout(this);
        layer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x99050812, 0xCC050812, 0xE8050812}));
        layer.setVisibility(View.GONE);
        screensaverLayer = layer;

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(90), dp(50), dp(90), dp(30));
        layer.addView(content, new FrameLayout.LayoutParams(-1, -1));

        screensaverLocation = text("", 16, 0xFFC0CBD9, true);
        screensaverLocation.setLetterSpacing(0.1f);
        screensaverLocation.setMaxLines(1);
        screensaverLocation.setEllipsize(android.text.TextUtils.TruncateAt.END);
        content.addView(screensaverLocation, new LinearLayout.LayoutParams(-1, dp(26)));

        screensaverClock = text("--:--:--", 112, Color.WHITE, false);
        if (outfitTypeface != null) {
            screensaverClock.setTypeface(Typeface.create(outfitTypeface, Typeface.NORMAL));
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                screensaverClock.setFontVariationSettings("'wght' 250");
            }
        } else {
            screensaverClock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        }
        content.addView(screensaverClock, new LinearLayout.LayoutParams(-1, dp(146)));

        screensaverDate = text("", 22, 0xFFDFE7F2, false);
        content.addView(screensaverDate, new LinearLayout.LayoutParams(-1, dp(34)));

        View spacer = new View(this);
        content.addView(spacer, new LinearLayout.LayoutParams(-1, dp(30)));

        screensaverNext = text("", 34, Color.WHITE, true);
        content.addView(screensaverNext, new LinearLayout.LayoutParams(-1, dp(52)));
        screensaverCountdown = text("", 21, 0xFFBFC8D5, false);
        content.addView(screensaverCountdown, new LinearLayout.LayoutParams(-1, dp(34)));
        screensaverPrayerRow = text("", 14, 0xFFE0E6EF, false);
        screensaverPrayerRow.setMaxLines(1);
        content.addView(screensaverPrayerRow, new LinearLayout.LayoutParams(-1, dp(28)));

        TextView hint = text("Tekan tombol apa saja untuk kembali", 14, 0xFF8F9BAA, false);
        hint.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(-1, dp(24),
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        hintParams.bottomMargin = dp(28);
        layer.addView(hint, hintParams);

        root.addView(layer, new FrameLayout.LayoutParams(-1, -1));
    }

    private void showScreensaver() {
        if (screensaverVisible || externalInputVisible || screensaverLayer == null) return;
        if (!featureEnabled("screensaver")) return;
        hidePoster();
        hideDockPreview();
        releaseTuner();
        screensaverVisible = true;
        cancelScreensaverTimer();
        homePage.setVisibility(View.GONE);
        homeScrim.setVisibility(View.GONE);
        glassHighlight.setVisibility(View.GONE);
        wallpaperImage.setVisibility(View.VISIBLE);
        screensaverLayer.setVisibility(View.VISIBLE);
        screensaverLayer.bringToFront();
        renderScreensaver();
        handler.removeCallbacks(screensaverTickRunnable);
        handler.post(screensaverTickRunnable);
    }

    private void hideScreensaver() {
        if (!screensaverVisible) return;
        screensaverVisible = false;
        handler.removeCallbacks(screensaverTickRunnable);
        screensaverLayer.setVisibility(View.GONE);
        showHomeUi();
        scheduleScreensaver();
    }

    private void renderScreensaver() {
        if (!screensaverVisible || screensaverClock == null) return;
        Date now = new Date();
        Locale locale = new Locale("id", "ID");
        screensaverClock.setText(new SimpleDateFormat("HH:mm:ss", locale).format(now));
        screensaverDate.setText(new SimpleDateFormat("EEEE, d MMMM yyyy", locale).format(now));
        screensaverLocation.setText(locationName().toUpperCase(locale) + "  ·  JADWAL SHOLAT");
        NextPrayerInfo info = nextPrayerInfo();
        if (info == null) {
            screensaverNext.setText("Jadwal sholat belum tersedia");
            screensaverCountdown.setText(prayerStatus == null ? "" : prayerStatus.getText());
            screensaverPrayerRow.setText("Subuh  --:--  ·  Zuhur  --:--  ·  Asar  --:--  ·  Magrib  --:--  ·  Isya  --:--");
            return;
        }
        screensaverNext.setText(info.name + "  " + info.time);
        long remaining = Math.max(0L, info.timeMillis - now.getTime());
        screensaverCountdown.setText(formatRemaining(remaining, true) + " lagi");
        screensaverPrayerRow.setText(prayerRowText(prayerTimes, info.index));
    }

    private String formatRemaining(long millis, boolean withSeconds) {
        long totalSeconds = millis / 1_000L;
        long hours = totalSeconds / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (withSeconds) return hours + " j " + minutes + " m " + seconds + " d";
        return hours + " j " + minutes + " m";
    }

    private NextPrayerInfo nextPrayerInfo() {
        if (prayerTimes == null) return null;
        String[] values = {prayerTimes.fajr, prayerTimes.dhuhr, prayerTimes.asr,
                prayerTimes.maghrib, prayerTimes.isha};
        long now = System.currentTimeMillis();
        TimeZone timeZone = TimeZone.getTimeZone(locationTimezone());
        int next = -1;
        long nextTime = Long.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            long time = parseToday(values[i], timeZone);
            if (time > now && time < nextTime) {
                next = i;
                nextTime = time;
            }
        }
        if (next < 0) {
            next = 0;
            nextTime = parseToday(values[0], timeZone) + 24L * 60L * 60L * 1_000L;
        }
        return new NextPrayerInfo(prayerNames.get(next), values[next], nextTime, next);
    }

    private CharSequence prayerRowText(NetworkWidgets.PrayerTimes times, int nextIndex) {
        String[] values = {times.fajr, times.dhuhr, times.asr, times.maghrib, times.isha};
        SpannableStringBuilder row = new SpannableStringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) row.append("   ·   ");
            int start = row.length();
            row.append(prayerNames.get(i)).append(" ").append(values[i]);
            if (i == nextIndex) {
                row.setSpan(new StyleSpan(Typeface.BOLD), start, row.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return row;
    }

    private void refreshPrayerTimes() {
        if (!featureEnabled("prayer_times")) return;
        final int generation = ++locationGeneration;
        final String latitude = locationLatitude();
        final String longitude = locationLongitude();
        NetworkWidgets.refreshPrayerTimes(this, latitude, longitude, locationTimezone(),
                new NetworkWidgets.Callback<NetworkWidgets.PrayerTimes>() {
                    @Override
                    public void onSuccess(NetworkWidgets.PrayerTimes result) {
                        if (generation != locationGeneration) return;
                        prayerTimes = result;
                        prayerStatus.setText("");
                        renderPrayerTimes();
                    }

                    @Override
                    public void onError(String message) {
                        if (generation != locationGeneration) return;
                        prayerStatus.setText(message);
                        if (screensaverVisible) renderScreensaver();
                    }
                });
    }

    private void openInputMenu() {
        List<TvInputInfo> inputs = new ArrayList<>();
        TvInputInfo antennaInput = null;
        for (TvInputInfo input : tvController.getInputs()) {
            if (input.isPassthroughInput()) {
                inputs.add(input);
            } else if (tvController.isAntennaInput(input)) {
                String label = TvController.displayName(input, this).toLowerCase(Locale.ROOT);
                if (antennaInput == null || label.contains("dtv")) antennaInput = input;
            }
        }
        if (antennaInput != null) {
            inputs.add(antennaInput);
        }
        if (inputs.isEmpty()) {
            Toast.makeText(this, "Daftar input TV tidak tersedia", Toast.LENGTH_LONG).show();
            return;
        }
        List<TvController.Channel> availableChannels = tvController.getChannels();
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(10), dp(8), dp(10), dp(8));
        android.widget.PopupWindow popup = new android.widget.PopupWindow(menu, dp(300),
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        inputPopup = popup;
        popup.setOnDismissListener(() -> inputPopup = null);
        popup.setBackgroundDrawable(glassBackground(0xFF9AC8FF));
        popup.setOutsideTouchable(true);
        popup.setElevation(dp(14));
        for (int i = 0; i < inputs.size(); i++) {
            TvInputInfo input = inputs.get(i);
            boolean connected = tvController.inputState(input.getId()) == TvInputManager.INPUT_STATE_CONNECTED;
            TextView choice = text((connected ? "●  " : "○  ")
                    + displayInputName(input), 15,
                    connected ? 0xFF9FE3B0 : Color.WHITE, true);
            choice.setPadding(dp(12), 0, dp(12), 0);
            choice.setFocusable(true);
            choice.setBackground(glassBackground(connected ? 0xFF9FE3B0 : 0xFF9AC8FF));
            LinearLayout.LayoutParams choiceParams = new LinearLayout.LayoutParams(-1, dp(48));
            choiceParams.setMargins(0, dp(2), 0, dp(2));
            menu.addView(choice, choiceParams);
            TvInputInfo selectedInput = input;
            choice.setOnClickListener(view -> {
                popup.dismiss();
                selectInput(selectedInput, availableChannels);
            });
        }
        popup.showAsDropDown(inputPill, 0, dp(4));
        menu.getChildAt(0).requestFocus();
    }

    private String displayInputName(TvInputInfo input) {
        String name = TvController.displayName(input, this);
        if (tvController.isAntennaInput(input)) return "Live TV";
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.contains("composite") || normalized.contains("komposit")) return "AV";
        return name;
    }

    private void selectInput(TvInputInfo input, List<TvController.Channel> channelsForInput) {
        for (TvController.Channel channel : channelsForInput) {
            if (channel.inputId.equals(input.getId())) {
                tuneChannel(channel, false);
                return;
            }
        }
        if (tvController.isAntennaInput(input)) {
            TvController.Channel channel = tvController.chooseLastOrSmallest();
            if (channel == null) {
                Toast.makeText(this, "Belum ada channel antena. Pindai channel di aplikasi TV.",
                        Toast.LENGTH_LONG).show();
            } else {
                showSelectedInput();
                tuneChannel(channel, false);
            }
            return;
        }
        if (input.isPassthroughInput()) {
            showSelectedInput();
            tvController.switchToInput(input);
            if (!tvController.isActive()) showHomeUi();
            return;
        }
        try {
            Intent setup = input.createSetupIntent();
            if (setup != null) startActivity(setup);
            else Toast.makeText(this, "Input ini tidak menyediakan channel TIF yang dapat disetel.",
                    Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.e(TAG, "Could not open setup for TV input " + input.getId(), e);
            Toast.makeText(this, "Tidak dapat membuka input ini", Toast.LENGTH_LONG).show();
        }
    }

    private void updateInputPill() {
        if (inputPill == null || tvController == null) return;
        boolean activeHdmi = false;
        for (TvInputInfo input : tvController.getInputs()) {
            if (input.getId().toLowerCase(Locale.ROOT).contains("hdmi")
                    && tvController.inputState(input.getId()) == TvInputManager.INPUT_STATE_CONNECTED) {
                activeHdmi = true;
                break;
            }
        }
        inputPill.setText(activeHdmi ? "●  Inputs" : "○  Inputs");
        inputPill.setTextColor(activeHdmi ? 0xFF9FE3B0 : Color.WHITE);
    }

    void refreshInputState() {
        updateInputPill();
    }

    @SuppressWarnings("deprecation")
    private void updateWifiPill() {
        if (wifiPill == null) return;
        String ssid = "";
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo info = wifi == null ? null : wifi.getConnectionInfo();
            if (info != null) ssid = info.getSSID();
        } catch (SecurityException e) {
            Log.w(TAG, "SSID access unavailable; showing generic Wi-Fi status", e);
        }
        if (ssid == null || ssid.isEmpty() || "<unknown ssid>".equalsIgnoreCase(ssid)) {
            wifiPill.setText("Wi-Fi");
        } else {
            wifiPill.setText("Wi-Fi  " + ssid.replace("\"", ""));
        }
    }

    private void renderClock() {
        if (clockText == null) return;
        Date now = new Date();
        Locale locale = new Locale("id", "ID");
        clockText.setText(new SimpleDateFormat("HH:mm", locale).format(now));
        dateText.setText(new SimpleDateFormat("EEEE, d MMMM yyyy", locale).format(now));
        int hour = Integer.parseInt(new SimpleDateFormat("H", Locale.US).format(now));
        String greeting = hour < 5 ? "Selamat malam"
                : hour < 11 ? "Selamat pagi"
                : hour < 15 ? "Selamat siang" : hour < 18 ? "Selamat sore" : "Selamat malam";
        String name = prefs().getString(KEY_GREETING_NAME, "").trim();
        greetingText.setText(name.isEmpty() ? greeting : greeting + ", " + name);
    }

    private void renderVerse() {
        if (verseArabic == null || !featureEnabled("verse_widget")) return;
        List<VerseCatalog.Entry> entries = VerseCatalog.entries();
        if (entries.isEmpty()) return;
        boolean auto = prefs().getBoolean(KEY_VERSE_AUTO, true);
        VerseCatalog.Entry entry = null;
        if (!auto) {
            entry = VerseCatalog.byId(prefs().getString(KEY_VERSE_ID, ""));
        }
        if (entry == null) {
            auto = true;
            int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            entry = entries.get(day % entries.size());
        }
        verseEyebrow.setText(entry.typeName().toUpperCase(new Locale("id", "ID"))
                + (auto ? " HARI INI" : " PILIHAN"));
        verseArabic.setText(entry.arabic);
        verseTranslation.setText(entry.translation);
        verseSource.setText(entry.source);
    }

    private void renderPrayerTimes() {
        if (prayerTimes == null || prayerRow == null) return;
        NextPrayerInfo info = nextPrayerInfo();
        if (info == null) return;
        long minutes = Math.max(0L, (info.timeMillis - System.currentTimeMillis()) / 60_000L);
        String remaining = (minutes / 60L) + " j " + (minutes % 60L) + " m lagi";
        nextPrayer.setText(info.name + "  " + info.time + "  ·  " + remaining);
        prayerRow.setText(prayerRowText(prayerTimes, info.index));
        if (screensaverVisible) renderScreensaver();
    }

    private long parseToday(String value, java.util.TimeZone timeZone) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            format.setTimeZone(timeZone);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            dateFormat.setTimeZone(timeZone);
            return format.parse(dateFormat.format(new Date())
                    + " " + value).getTime();
        } catch (ParseException e) {
            Log.e(TAG, "Invalid prayer time: " + value, e);
            return Long.MAX_VALUE;
        }
    }

    private void tuneChannel(TvController.Channel channel, boolean mute) {
        tvController.tune(channel, mute);
        if (wallpaperImage != null && tvController.isActive()) wallpaperImage.setVisibility(View.GONE);
    }

    private void showSelectedInput() {
        externalInputVisible = true;
        cancelScreensaverTimer();
        hideDockPreview();
        homePage.setVisibility(View.GONE);
        homeScrim.setVisibility(View.GONE);
        wallpaperImage.setVisibility(View.GONE);
        glassHighlight.setVisibility(View.GONE);
        if (tvView.getParent() == null) {
            root.addView(tvView, 0, new FrameLayout.LayoutParams(-1, -1));
        }
        tvView.setVisibility(View.VISIBLE);
        tvView.bringToFront();
    }

    private void showHomeUi() {
        externalInputVisible = false;
        tvView.setVisibility(View.GONE);
        if (tvView.getParent() == root) root.removeView(tvView);
        homeScrim.setVisibility(View.VISIBLE);
        homePage.setVisibility(View.VISIBLE);
        homePage.bringToFront();
        wallpaperImage.setVisibility(View.VISIBLE);
        updateHomeScrim();
        resetScreensaverTimer();
        if (glassHighlight != null) {
            glassHighlight.setVisibility(View.INVISIBLE);
            root.post(() -> {
                View focused = getCurrentFocus();
                if (focused != null) {
                    Object accent = focused.getTag(R.id.focus_accent);
                    updateHighlight(focused, accent instanceof Integer ? (Integer) accent : 0xFF8BBFFF);
                }
            });
        }
    }

    void onInputConnectionFailed() {
        runOnUiThread(() -> {
            if (externalInputVisible) showHomeUi();
        });
    }

    private void releaseTuner() {
        tvController.release();
        if (externalInputVisible) showHomeUi();
    }

    private boolean featureEnabled(String name) {
        for (String entry : getResources().getStringArray(R.array.feature_switches)) {
            String[] pair = entry.split("=", 2);
            if (pair.length == 2 && pair[0].trim().equals(name)) {
                return Boolean.parseBoolean(pair[1].trim());
            }
        }
        return false;
    }

    private Tile currentTile() {
        List<Tile> items = currentSection == DOCK ? dockTiles
                : currentSection == WATCH ? watchTiles
                : currentSection == GRID ? gridTiles : Collections.emptyList();
        return currentPosition >= 0 && currentPosition < items.size() ? items.get(currentPosition) : null;
    }

    private void showPoster(int position) {
        if (position < 0 || position >= posters.size()) return;
        PreviewPoster poster = posters.get(position);
        posterTitle.setText(poster.title);
        posterLayer.setVisibility(View.VISIBLE);
        posterImage.animate().cancel();
        posterImage.setAlpha(0f);
        loadImage(poster.artUri, bitmap -> {
            if (posterLayer.getVisibility() != View.VISIBLE || !poster.equals(posters.get(posterPosition))) return;
            posterImage.setImageBitmap(bitmap);
            posterImage.animate().alpha(1f).setDuration(450L).start();
        });
    }

    private void loadImage(String rawUri, ImageCallback callback) {
        IO.execute(() -> {
            try {
                Uri uri = Uri.parse(rawUri);
                byte[] data;
                if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                    HttpURLConnection connection = (HttpURLConnection) new URL(rawUri).openConnection();
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    try (InputStream input = connection.getInputStream()) {
                        data = readBytes(input);
                    } finally {
                        connection.disconnect();
                    }
                } else {
                    try (InputStream input = getContentResolver().openInputStream(uri)) {
                        data = input == null ? null : readBytes(input);
                    }
                }
                if (data == null || data.length == 0) throw new IOException("No image data for " + uri);
                Bitmap bitmap = decodeScaled(data);
                if (bitmap == null) throw new IOException("Image could not be decoded: " + uri);
                handler.post(() -> callback.onImage(bitmap));
            } catch (IOException | SecurityException e) {
                Log.e(TAG, "Image could not be loaded: " + rawUri, e);
                handler.post(() -> callback.onError("Wallpaper tidak dapat dimuat"));
            }
        });
    }

    private byte[] readBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8_192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private Bitmap decodeScaled(byte[] data) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
        int targetWidth = Math.max(1, getResources().getDisplayMetrics().widthPixels);
        int targetHeight = Math.max(1, getResources().getDisplayMetrics().heightPixels);
        int sample = 1;
        while (bounds.outWidth / (sample * 2) > targetWidth
                || bounds.outHeight / (sample * 2) > targetHeight) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    private void hidePoster() {
        handler.removeCallbacks(posterNextRunnable);
        posters.clear();
        if (posterLayer != null) posterLayer.setVisibility(View.GONE);
    }

    private boolean dockPreviewEnabled() {
        return featureEnabled("dock_preview_video")
                && prefs().getBoolean(KEY_DOCK_PREVIEW, true);
    }

    private void scheduleDockPreview(Tile tile) {
        hideDockPreview();
        if (!dockPreviewEnabled() || tile == null || tile.packageName == null
                || "__tv__".equals(tile.packageName)) return;
        final String packageName = tile.packageName;
        final int generation = dockPreviewGeneration;
        dockPreviewRunnable = () -> loadDockPreview(packageName, generation);
        handler.postDelayed(dockPreviewRunnable, 550L);
    }

    private void hideDockPreview() {
        handler.removeCallbacks(dockPreviewRunnable);
        dockPreviewRunnable = null;
        dockPreviewGeneration++;
        if (dockPreviewVideo != null) {
            dockPreviewVideo.stopPlayback();
            dockPreviewVideo.setVisibility(View.GONE);
        }
        if (dockPreviewImage != null) {
            dockPreviewImage.setImageDrawable(null);
            dockPreviewImage.setVisibility(View.GONE);
        }
        if (dockPreviewLayer != null) dockPreviewLayer.setVisibility(View.GONE);
        updateHomeScrim();
    }

    private void loadDockPreview(String packageName, int generation) {
        PreviewInfo cached = previewCache.get(packageName);
        if (cached != null) {
            showDockPreview(cached, generation);
            return;
        }
        IO.execute(() -> {
            PreviewInfo result = queryDockPreview(packageName);
            handler.post(() -> {
                previewCache.put(packageName, result);
                showDockPreview(result, generation);
            });
        });
    }

    private PreviewInfo queryDockPreview(String packageName) {
        String videoUri = null;
        String posterUri = null;
        String[] projection = {
                TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME,
                TvContract.PreviewPrograms.COLUMN_PREVIEW_VIDEO_URI,
                TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI
        };
        try (android.database.Cursor cursor = getContentResolver().query(
                TvContract.PreviewPrograms.CONTENT_URI, projection,
                null, null,
                TvContract.PreviewPrograms.COLUMN_WEIGHT + " DESC")) {
            if (cursor != null) {
                int packageIndex = cursor.getColumnIndex(
                        TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME);
                int videoIndex = cursor.getColumnIndex(
                        TvContract.PreviewPrograms.COLUMN_PREVIEW_VIDEO_URI);
                int posterIndex = cursor.getColumnIndex(
                        TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI);
                while (cursor.moveToNext()) {
                    if (packageIndex < 0 || !packageName.equals(cursor.getString(packageIndex))) {
                        continue;
                    }
                    if (videoUri == null && videoIndex >= 0) {
                        videoUri = cursor.getString(videoIndex);
                    }
                    if (posterUri == null && posterIndex >= 0) {
                        posterUri = cursor.getString(posterIndex);
                    }
                    if (videoUri != null && posterUri != null) break;
                }
            }
        } catch (SecurityException | IllegalArgumentException e) {
            Log.w(TAG, "Preview program query failed for " + packageName, e);
        }
        if (videoUri == null && posterUri == null) {
            String[] watchProjection = {
                    TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME,
                    TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI,
                    TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI
            };
            try (android.database.Cursor cursor = getContentResolver().query(
                    TvContract.WatchNextPrograms.CONTENT_URI, watchProjection,
                    null, null,
                    TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS + " DESC")) {
                if (cursor != null) {
                    int packageIndex = cursor.getColumnIndex(
                            TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME);
                    int posterIndex = cursor.getColumnIndex(
                            TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI);
                    int thumbnailIndex = cursor.getColumnIndex(
                            TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI);
                    while (cursor.moveToNext()) {
                        if (packageIndex < 0 || !packageName.equals(cursor.getString(packageIndex))) {
                            continue;
                        }
                        if (posterIndex >= 0) posterUri = cursor.getString(posterIndex);
                        if (posterUri == null && thumbnailIndex >= 0) {
                            posterUri = cursor.getString(thumbnailIndex);
                        }
                        break;
                    }
                }
            } catch (SecurityException | IllegalArgumentException e) {
                Log.w(TAG, "Watch Next poster query failed for " + packageName, e);
            }
        }
        return new PreviewInfo(videoUri, posterUri);
    }

    private void showDockPreview(PreviewInfo info, int generation) {
        if (generation != dockPreviewGeneration) return;
        if (info == null || (info.videoUri == null && info.posterUri == null)) {
            dockPreviewLayer.setVisibility(View.GONE);
            updateHomeScrim();
            return;
        }
        if (info.posterUri != null) {
            loadImage(info.posterUri, new ImageCallback() {
                @Override
                public void onImage(Bitmap bitmap) {
                    if (generation != dockPreviewGeneration) return;
                    dockPreviewImage.setImageBitmap(bitmap);
                    if (dockPreviewVideo.getVisibility() != View.VISIBLE) {
                        dockPreviewLayer.setVisibility(View.VISIBLE);
                        dockPreviewImage.setVisibility(View.VISIBLE);
                        updateHomeScrim();
                    }
                }
            });
        }
        if (info.videoUri != null) {
            dockPreviewImage.setVisibility(View.GONE);
            dockPreviewLayer.setVisibility(View.VISIBLE);
            dockPreviewVideo.setVisibility(View.VISIBLE);
            dockPreviewVideo.setVideoURI(Uri.parse(info.videoUri));
            dockPreviewVideo.start();
            updateHomeScrim();
        }
    }

    private void activateFocused() {
        hideDockPreview();
        if (currentSection == PILLS) {
            activatePill(currentPosition);
            return;
        }
        Tile tile = currentTileForSection();
        if (tile == null) return;
        if (currentSection == WATCH) {
            openContinueWatching(tile);
        } else if (currentSection == CHANNELS) {
            if (tile.channel == null) {
                Toast.makeText(this, "Channel belum tersedia. Periksa nomor di config.xml atau pindai channel.",
                        Toast.LENGTH_LONG).show();
            } else {
                tuneChannel(tile.channel, false);
            }
        } else if ("__tv__".equals(tile.packageName)) {
            openLiveTv();
        } else {
            launchApp(tile.packageName);
        }
    }

    private Tile currentTileForSection() {
        List<Tile> items;
        if (currentSection == DOCK) items = dockTiles;
        else if (currentSection == WATCH) items = watchTiles;
        else if (currentSection == CHANNELS) items = channelTiles;
        else if (currentSection == GRID) items = gridTiles;
        else return null;
        return currentPosition >= 0 && currentPosition < items.size() ? items.get(currentPosition) : null;
    }

    private void openLiveTv() {
        releaseTuner();
        Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage("com.mediatek.wwtv.tvcenter");
        if (intent == null) intent = getPackageManager().getLaunchIntentForPackage("com.mediatek.wwtv.tvcenter");
        if (intent == null) {
            Toast.makeText(this, "Aplikasi Live TV tidak tersedia", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
    }

    private void launchApp(String packageName) {
        releaseTuner();
        Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(packageName);
        if (intent == null) intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            Toast.makeText(this, "Aplikasi tidak menyediakan halaman untuk dibuka", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
    }

    private void openContinueWatching(Tile tile) {
        releaseTuner();
        String packageName = tile.packageName;
        if (packageName != null) {
            Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(packageName);
            if (intent == null) intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                startActivity(intent);
                return;
            }
        }
        launchIntentUri(tile.intentUri, packageName);
    }

    private void launchIntentUri(String uri, String fallbackPackage) {
        releaseTuner();
        if (uri != null && !uri.isEmpty()) {
            try {
                Intent intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
                startActivity(intent);
                return;
            } catch (URISyntaxException | ActivityNotFoundException e) {
                Log.e(TAG, "Could not open TV program intent URI", e);
            }
        }
        if (fallbackPackage != null) launchApp(fallbackPackage);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (screensaverVisible) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) hideScreensaver();
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) resetScreensaverTimer();
        if (externalInputVisible && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            releaseTuner();
            return true;
        }
        if (inputPopup != null && inputPopup.isShowing()) return super.dispatchKeyEvent(event);
        if (menuPopup != null && menuPopup.isShowing()) return super.dispatchKeyEvent(event);
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (!centerPressed) {
                    centerPressed = true;
                    centerLongTriggered = false;
                    handler.postDelayed(centerLongPressRunnable, 600L);
                }
                return true;
            }
            if (event.getAction() == KeyEvent.ACTION_UP) {
                handler.removeCallbacks(centerLongPressRunnable);
                if (centerPressed && !centerLongTriggered) activateFocused();
                centerPressed = false;
                return true;
            }
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                int delta = keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1;
                int next = currentPosition + delta;
                boolean crossesGridRow = currentSection == GRID && next >= 0
                        && next < gridTiles.size() && next / gridColumns != currentPosition / gridColumns;
                if (next >= 0 && next < sectionCount(currentSection) && !crossesGridRow) {
                    focusAt(currentSection, next);
                }
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                moveVertical(false);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                moveVertical(true);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private TextView sectionTitle(String value) {
        TextView title = text(value, 22, Color.WHITE, true);
        softShadow(title);
        return title;
    }

    private void softShadow(TextView view) {
        view.setShadowLayer(dp(8), 0f, dp(2), 0xE6000000);
    }

    private void applyCenterClick(View view) {
        view.setOnKeyListener((target, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP
                    && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == KeyEvent.KEYCODE_ENTER)) {
                target.performClick();
                return true;
            }
            return false;
        });
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        if (outfitTypeface != null) {
            view.setTypeface(Typeface.create(outfitTypeface, bold ? Typeface.BOLD : Typeface.NORMAL));
        } else if (bold) {
            view.setTypeface(view.getTypeface(), Typeface.BOLD);
        }
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class TileAdapter extends RecyclerView.Adapter<TileAdapter.Holder> {
        private final int section;
        private final List<Tile> items;

        TileAdapter(int section, List<Tile> items) {
            this.section = section;
            this.items = items;
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(MainActivity.this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setFocusable(true);
            card.setFocusableInTouchMode(true);
            int horizontalPadding = section == WATCH ? dp(8) : dp(7);
            card.setPadding(horizontalPadding, dp(6), horizontalPadding, dp(5));
            card.setBackgroundColor(Color.TRANSPARENT);

            ImageView image = new ImageView(MainActivity.this);
            image.setScaleType(section == WATCH ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
            if (section == CHANNELS) image.setVisibility(View.GONE);
            card.addView(image, new LinearLayout.LayoutParams(
                    section == WATCH ? dp(172) : section == CHANNELS ? 0 : dp(76),
                    section == WATCH ? dp(96) : section == CHANNELS ? 0 : dp(76)));
            ProgressBar progress = new ProgressBar(MainActivity.this, null,
                    android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100);
            progress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF80BFFF));
            progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0x66FFFFFF));
            if (section == WATCH) {
                card.addView(progress, new LinearLayout.LayoutParams(dp(172), dp(4)));
            } else {
                progress.setVisibility(View.GONE);
            }

            TextView label = text("", section == WATCH ? 13 : 13, Color.WHITE, true);
            softShadow(label);
            label.setGravity(section == WATCH ? Gravity.CENTER_VERTICAL : Gravity.CENTER);
            label.setMaxLines(1);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            card.addView(label, new LinearLayout.LayoutParams(-1,
                    dp(section == WATCH || section == CHANNELS ? 24 : 22)));
            TextView subtitle = text("", 11, 0xFFC0CBD9, false);
            softShadow(subtitle);
            subtitle.setGravity(Gravity.CENTER);
            subtitle.setMaxLines(1);
            subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
            if (section == WATCH || section == CHANNELS) {
                card.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(18)));
            }
            card.setOnFocusChangeListener((view, focused) -> {
                int accent = view.getTag(R.id.focus_accent) instanceof Integer
                        ? (Integer) view.getTag(R.id.focus_accent) : 0xFF86BFFF;
                view.setAlpha(focused ? 1f : section == WATCH ? 0.74f : 1f);
                label.setVisibility(focused || section == WATCH || section == CHANNELS
                        ? View.VISIBLE : View.INVISIBLE);
                if (focused) onTileFocus(view, true, accent);
            });
            card.setOnClickListener(view -> {
                int position = (Integer) view.getTag(R.id.focus_position);
                focusAt(section, position);
                activateFocused();
            });
            return new Holder(card, image, progress, label, subtitle);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            Tile tile = items.get(position);
            holder.itemView.setTag(R.id.focus_section, section);
            holder.itemView.setTag(R.id.focus_position, position);
            holder.itemView.setTag(R.id.focus_accent, tile.accent);
            holder.label.setText(section == CHANNELS ? tile.channelNumber : tile.title);
            holder.subtitle.setText(section == CHANNELS ? tile.title : section == WATCH ? tile.subtitle : "");
            holder.subtitle.setVisibility(section == WATCH || section == CHANNELS ? View.VISIBLE : View.GONE);
            holder.label.setVisibility(section == WATCH || section == CHANNELS || holder.itemView.hasFocus()
                    ? View.VISIBLE : View.INVISIBLE);
            holder.progress.setVisibility(section == WATCH ? View.VISIBLE : View.GONE);
            if (section == WATCH) {
                holder.itemView.setAlpha(holder.itemView.hasFocus() ? 1f : 0.74f);
                int percent = tile.duration > 0 ? (int) Math.min(100L, tile.playbackPosition * 100L / tile.duration) : 0;
                holder.progress.setProgress(percent);
            }
            if (tile.icon != null) {
                holder.image.setImageDrawable(tile.icon);
            } else if (tile.artUri != null && !tile.artUri.isEmpty()) {
                holder.image.setImageDrawable(null);
                loadImage(tile.artUri, bitmap -> {
                    if (holder.getBindingAdapterPosition() == position) holder.image.setImageBitmap(bitmap);
                });
            } else {
                holder.image.setImageResource(R.drawable.app_icon);
            }
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    section == GRID ? -1 : section == WATCH ? dp(190) : section == CHANNELS ? dp(176) : 0,
                    dp(section == GRID ? 132 : section == WATCH ? 154 : section == CHANNELS ? 78 : 128));
            if (section == DOCK) {
                int available = getResources().getDisplayMetrics().widthPixels - dp(100) - dp(16);
                params.width = available / Math.max(1, Math.min(6, dockTiles.size())) - dp(8);
            }
            params.setMargins(dp(4), dp(3), dp(4), dp(3));
            holder.itemView.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            final ProgressBar progress;
            final TextView label;
            final TextView subtitle;

            Holder(View itemView, ImageView image, ProgressBar progress, TextView label, TextView subtitle) {
                super(itemView);
                this.image = image;
                this.progress = progress;
                this.label = label;
                this.subtitle = subtitle;
            }
        }
    }

    private interface ImageCallback {
        void onImage(Bitmap bitmap);

        default void onError(String message) {
        }
    }

    private static final class NextPrayerInfo {
        final String name;
        final String time;
        final long timeMillis;
        final int index;

        NextPrayerInfo(String name, String time, long timeMillis, int index) {
            this.name = name;
            this.time = time;
            this.timeMillis = timeMillis;
            this.index = index;
        }
    }

    private static final class PreviewInfo {
        final String videoUri;
        final String posterUri;

        PreviewInfo(String videoUri, String posterUri) {
            this.videoUri = videoUri == null || videoUri.isEmpty() ? null : videoUri;
            this.posterUri = posterUri == null || posterUri.isEmpty() ? null : posterUri;
        }
    }

    private static final class PreviewPoster {
        final String packageName;
        final String title;
        final String artUri;
        final String intentUri;

        PreviewPoster(String packageName, String title, String artUri, String intentUri) {
            this.packageName = packageName;
            this.title = title == null ? "" : title;
            this.artUri = artUri;
            this.intentUri = intentUri;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof PreviewPoster)) return false;
            PreviewPoster value = (PreviewPoster) other;
            return artUri.equals(value.artUri);
        }
    }

    private static final class WatchCandidate {
        final Tile tile;
        final long engagement;
        final long position;
        final long duration;
        final int type;
        final String contentId;

        WatchCandidate(Tile tile, long engagement, long position, long duration,
                       int type, String contentId) {
            this.tile = tile;
            this.engagement = engagement;
            this.position = position;
            this.duration = duration;
            this.type = type;
            this.contentId = contentId;
        }
    }

    private static final class Tile {
        final int type;
        final String packageName;
        final String title;
        final String subtitle;
        final String artUri;
        final String intentUri;
        final String activityName;
        final String channelNumber;
        final TvController.Channel channel;
        final Drawable icon;
        final int accent;
        final long playbackPosition;
        final long duration;

        private Tile(int type, String packageName, String title, String subtitle, String artUri,
                     String intentUri, String activityName, String channelNumber,
                     TvController.Channel channel, Drawable icon, int accent,
                     long playbackPosition, long duration) {
            this.type = type;
            this.packageName = packageName;
            this.title = title == null ? "" : title;
            this.subtitle = subtitle == null ? "" : subtitle;
            this.artUri = artUri;
            this.intentUri = intentUri;
            this.activityName = activityName;
            this.channelNumber = channelNumber == null ? "" : channelNumber;
            this.channel = channel;
            this.icon = icon;
            this.accent = accent;
            this.playbackPosition = playbackPosition;
            this.duration = duration;
        }

        static Tile app(String packageName, String label, Drawable banner, String activityName, int accent) {
            return new Tile(0, packageName, label, "", null, null, activityName, null, null,
                    banner, accent, 0L, 0L);
        }

        static Tile tv() {
            return new Tile(0, "__tv__", "TV", "", null, null, null, null, null,
                    null, 0xFF78B8FF, 0L, 0L);
        }

        static Tile watch(String packageName, String appName, String title, String artUri, String intentUri,
                          long playbackPosition, long duration) {
            return new Tile(1, packageName, title, appName, artUri, intentUri, null, null,
                    null, null, 0xFF78B8FF, playbackPosition, duration);
        }

        static Tile channel(String number, String name, TvController.Channel channel) {
            return new Tile(2, null, name, "", null, null, null, number, channel, null,
                    0xFF78B8FF, 0L, 0L);
        }
    }
}
