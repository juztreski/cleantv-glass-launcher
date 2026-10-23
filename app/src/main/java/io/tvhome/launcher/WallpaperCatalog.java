package io.tvhome.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import java.util.Arrays;
import java.util.List;

final class WallpaperCatalog {
    static final class Preset {
        final String id;
        final String name;

        Preset(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static final List<Preset> PRESETS = Arrays.asList(
            new Preset("senja", "Senja"),
            new Preset("malam", "Malam"),
            new Preset("samudra", "Samudra"),
            new Preset("hutan", "Hutan"),
            new Preset("aurora", "Aurora"),
            new Preset("hitam", "Hitam"),
            new Preset("pemandangan", "Pemandangan"),
            new Preset("perkotaan", "Perkotaan"),
            new Preset("abstrak", "Abstrak"));

    static List<Preset> presets() {
        return PRESETS;
    }

    static String nameOf(String id) {
        for (Preset preset : PRESETS) {
            if (preset.id.equals(id)) return preset.name;
        }
        return PRESETS.get(0).name;
    }

    static boolean isBlack(String id) {
        return "hitam".equals(id);
    }

    static Drawable create(String id) {
        if ("pemandangan".equals(id) || "perkotaan".equals(id) || "abstrak".equals(id)) {
            return new ArtWallpaperDrawable(id);
        }
        return new GradientWallpaperDrawable(themeFor(id));
    }

    static Bitmap thumbnail(String id, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(Math.max(1, width), Math.max(1, height),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable drawable = create(id);
        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private static Theme themeFor(String id) {
        if ("malam".equals(id)) {
            return new Theme(new int[]{0xFF05070F, 0xFF101A33, 0xFF1E3A5F},
                    0x555C9CFF, 0x33486BB3);
        }
        if ("samudra".equals(id)) {
            return new Theme(new int[]{0xFF031C2B, 0xFF064663, 0xFF0B8FA8},
                    0x5560E7FF, 0x3334B7D5);
        }
        if ("hutan".equals(id)) {
            return new Theme(new int[]{0xFF06170F, 0xFF0E3B2A, 0xFF1E6B4E},
                    0x557BE3A8, 0x3332A76B);
        }
        if ("aurora".equals(id)) {
            return new Theme(new int[]{0xFF0A0620, 0xFF14264D, 0xFF3B1F5C},
                    0x555CFFD6, 0x44B06BFF);
        }
        if ("hitam".equals(id)) {
            return new Theme(new int[]{Color.BLACK, Color.BLACK, Color.BLACK},
                    0x00000000, 0x00000000);
        }
        return new Theme(new int[]{0xFF111528, 0xFF3A2547, 0xFFC96A3B},
                0x55FFB46B, 0x448C5CE0);
    }

    private static final class Theme {
        final int[] base;
        final int glowTop;
        final int glowBottom;

        Theme(int[] base, int glowTop, int glowBottom) {
            this.base = base;
            this.glowTop = glowTop;
            this.glowBottom = glowBottom;
        }
    }

    private static final class ArtWallpaperDrawable extends Drawable {
        private final String id;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        ArtWallpaperDrawable(String id) {
            this.id = id;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) return;
            if ("pemandangan".equals(id)) drawLandscape(canvas, bounds);
            else if ("perkotaan".equals(id)) drawCity(canvas, bounds);
            else drawAbstract(canvas, bounds);
        }

        private void drawLandscape(Canvas canvas, Rect bounds) {
            float left = bounds.left;
            float top = bounds.top;
            float width = bounds.width();
            float height = bounds.height();
            float horizon = top + height * 0.70f;

            paint.setShader(new LinearGradient(left, top, left, horizon,
                    new int[]{0xFF0C1530, 0xFF3D2A4C, 0xFFB05B52, 0xFFF0A15C},
                    new float[]{0f, 0.48f, 0.78f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(left, top, left + width, horizon, paint);
            paint.setShader(null);

            float sunX = left + width * 0.73f;
            float sunY = top + height * 0.31f;
            float sunRadius = height * 0.085f;
            paint.setColor(0x33FFD9A0);
            canvas.drawCircle(sunX, sunY, sunRadius * 3.2f, paint);
            paint.setColor(0x66FFE2B4);
            canvas.drawCircle(sunX, sunY, sunRadius * 1.8f, paint);
            paint.setColor(0xFFFFEBCB);
            canvas.drawCircle(sunX, sunY, sunRadius, paint);

            path.reset();
            path.moveTo(left, horizon);
            path.lineTo(left + width * 0.16f, top + height * 0.40f);
            path.lineTo(left + width * 0.31f, top + height * 0.58f);
            path.lineTo(left + width * 0.48f, top + height * 0.34f);
            path.lineTo(left + width * 0.64f, top + height * 0.55f);
            path.lineTo(left + width * 0.82f, top + height * 0.43f);
            path.lineTo(left + width, top + height * 0.58f);
            path.lineTo(left + width, horizon);
            path.close();
            paint.setColor(0xFF353B5C);
            canvas.drawPath(path, paint);

            path.reset();
            path.moveTo(left, horizon);
            path.lineTo(left + width * 0.20f, top + height * 0.55f);
            path.lineTo(left + width * 0.37f, top + height * 0.69f);
            path.lineTo(left + width * 0.58f, top + height * 0.50f);
            path.lineTo(left + width * 0.76f, top + height * 0.66f);
            path.lineTo(left + width * 0.91f, top + height * 0.53f);
            path.lineTo(left + width, top + height * 0.63f);
            path.lineTo(left + width, horizon);
            path.close();
            paint.setColor(0xFF181C2D);
            canvas.drawPath(path, paint);

            paint.setShader(new LinearGradient(left, horizon, left, bounds.bottom,
                    new int[]{0xFF1B2842, 0xFF0A1220, 0xFF05080F},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(left, horizon, left + width, bounds.bottom, paint);
            paint.setShader(null);

            paint.setColor(0x40FFC085);
            canvas.drawOval(new RectF(sunX - width * 0.10f, horizon + height * 0.035f,
                    sunX + width * 0.10f, horizon + height * 0.12f), paint);
            paint.setColor(0x22FFE1B8);
            for (int i = 0; i < 5; i++) {
                float lineY = horizon + height * (0.05f + i * 0.035f);
                float inset = width * (0.08f + i * 0.045f);
                canvas.drawRect(left + inset, lineY, left + width - inset, lineY + height * 0.004f,
                        paint);
            }
            paint.setColor(0xFF04060C);
            canvas.drawRect(left, top + height * 0.94f, left + width, bounds.bottom, paint);
        }

        private void drawCity(Canvas canvas, Rect bounds) {
            float left = bounds.left;
            float top = bounds.top;
            float width = bounds.width();
            float height = bounds.height();

            paint.setShader(new LinearGradient(left, top, left, top + height * 0.84f,
                    new int[]{0xFF060A18, 0xFF171B3D, 0xFF4B2C53, 0xFF93516A},
                    new float[]{0f, 0.45f, 0.78f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(left, top, left + width, top + height * 0.84f, paint);
            paint.setShader(null);

            float moonX = left + width * 0.20f;
            float moonY = top + height * 0.20f;
            float moonRadius = height * 0.055f;
            paint.setColor(0x33DCE6FF);
            canvas.drawCircle(moonX, moonY, moonRadius * 3.4f, paint);
            paint.setColor(0xFFE8EDFF);
            canvas.drawCircle(moonX, moonY, moonRadius, paint);

            drawSkyline(canvas, bounds, 0.82f, 0xFF131A2E, 0.05f, false);
            drawSkyline(canvas, bounds, 0.90f, 0xFF080C16, 0.075f, true);

            paint.setColor(0xFF02040A);
            canvas.drawRect(left, top + height * 0.93f, left + width, bounds.bottom, paint);
        }

        private void drawSkyline(Canvas canvas, Rect bounds, float baselineFraction, int color,
                                 float widthFraction, boolean windows) {
            float left = bounds.left;
            float top = bounds.top;
            float width = bounds.width();
            float height = bounds.height();
            float baseline = top + height * baselineFraction;
            float x = left - width * 0.02f;
            int index = 0;
            while (x < left + width) {
                float buildingWidth = width * (widthFraction + ((index * 37) % 7) * 0.007f);
                float buildingHeight = height * (0.13f + ((index * 29) % 11) * 0.035f);
                paint.setColor(color);
                canvas.drawRect(x, baseline - buildingHeight, x + buildingWidth, baseline, paint);
                if (windows) {
                    paint.setColor(index % 3 == 0 ? 0x8866C8FF : 0x99FFC46B);
                    float cellWidth = buildingWidth * 0.16f;
                    float cellHeight = height * 0.012f;
                    for (float wy = baseline - buildingHeight + height * 0.035f;
                         wy < baseline - height * 0.035f; wy += height * 0.045f) {
                        for (float wx = x + buildingWidth * 0.18f;
                             wx < x + buildingWidth * 0.78f; wx += cellWidth * 2f) {
                            if ((((int) (wy * 3) + (int) (wx * 2) + index) % 4) == 0) continue;
                            canvas.drawRect(wx, wy, wx + cellWidth, wy + cellHeight, paint);
                        }
                    }
                }
                x += buildingWidth + width * 0.006f;
                index++;
            }
        }

        private void drawAbstract(Canvas canvas, Rect bounds) {
            float left = bounds.left;
            float top = bounds.top;
            float width = bounds.width();
            float height = bounds.height();

            paint.setShader(new LinearGradient(left, top, left + width, bounds.bottom,
                    new int[]{0xFF080512, 0xFF1B0D33, 0xFF07202C},
                    new float[]{0f, 0.52f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(bounds, paint);
            paint.setShader(null);

            paint.setShader(new RadialGradient(left + width * 0.78f, top + height * 0.20f,
                    Math.max(width, height) * 0.68f, 0x667B5CFF, 0x00000000,
                    Shader.TileMode.CLAMP));
            canvas.drawRect(bounds, paint);
            paint.setShader(null);

            paint.setColor(0x33FF6FB5);
            canvas.drawCircle(left + width * 0.18f, top + height * 0.74f, height * 0.30f, paint);
            paint.setColor(0x2E42E8E0);
            canvas.drawCircle(left + width * 0.87f, top + height * 0.62f, height * 0.38f, paint);
            paint.setColor(0x228B7CFF);
            canvas.drawCircle(left + width * 0.56f, top + height * 0.30f, height * 0.24f, paint);

            path.reset();
            path.moveTo(left - width * 0.05f, top + height * 0.68f);
            path.cubicTo(left + width * 0.18f, top + height * 0.52f,
                    left + width * 0.36f, top + height * 0.88f,
                    left + width * 0.58f, top + height * 0.68f);
            path.cubicTo(left + width * 0.76f, top + height * 0.52f,
                    left + width * 0.94f, top + height * 0.78f,
                    left + width * 1.06f, top + height * 0.60f);
            path.lineTo(left + width * 1.06f, bounds.bottom);
            path.lineTo(left - width * 0.05f, bounds.bottom);
            path.close();
            paint.setShader(new LinearGradient(left, top + height * 0.5f, left + width,
                    bounds.bottom, new int[]{0x7748C6FF, 0x664B2C8F, 0xAA120A22},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawPath(path, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, height * 0.006f));
            paint.setColor(0x55FFFFFF);
            path.reset();
            path.moveTo(left + width * 0.02f, top + height * 0.40f);
            path.cubicTo(left + width * 0.26f, top + height * 0.24f,
                    left + width * 0.47f, top + height * 0.50f,
                    left + width * 0.72f, top + height * 0.35f);
            path.cubicTo(left + width * 0.86f, top + height * 0.27f,
                    left + width * 0.94f, top + height * 0.31f,
                    left + width * 1.02f, top + height * 0.24f);
            canvas.drawPath(path, paint);
            paint.setColor(0x33FFB0D8);
            path.reset();
            path.moveTo(left + width * 0.04f, top + height * 0.48f);
            path.cubicTo(left + width * 0.28f, top + height * 0.60f,
                    left + width * 0.52f, top + height * 0.30f,
                    left + width * 0.78f, top + height * 0.45f);
            path.cubicTo(left + width * 0.90f, top + height * 0.52f,
                    left + width * 0.97f, top + height * 0.46f,
                    left + width * 1.03f, top + height * 0.50f);
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }

    private static final class GradientWallpaperDrawable extends Drawable {
        private final Theme theme;
        private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint vignettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int cachedWidth;
        private int cachedHeight;

        GradientWallpaperDrawable(Theme theme) {
            this.theme = theme;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) return;
            ensureShaders(bounds.width(), bounds.height());
            canvas.drawRect(bounds, basePaint);
            canvas.drawRect(bounds, glowPaint);
            canvas.drawRect(bounds, vignettePaint);
        }

        private void ensureShaders(int width, int height) {
            if (width == cachedWidth && height == cachedHeight) return;
            cachedWidth = width;
            cachedHeight = height;
            basePaint.setShader(new LinearGradient(0, 0, width, height, theme.base,
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            glowPaint.setShader(new RadialGradient(width * 0.82f, height * 0.12f,
                    Math.max(width, height) * 0.62f, theme.glowTop, 0x00000000,
                    Shader.TileMode.CLAMP));
            vignettePaint.setShader(new RadialGradient(width * 0.38f, height * 0.58f,
                    Math.max(width, height) * 0.8f, 0x00000000, theme.glowBottom,
                    Shader.TileMode.CLAMP));
        }

        @Override
        public void setAlpha(int alpha) {
            basePaint.setAlpha(alpha);
            glowPaint.setAlpha(alpha);
            vignettePaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            basePaint.setColorFilter(colorFilter);
            glowPaint.setColorFilter(colorFilter);
            vignettePaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private WallpaperCatalog() {}
}
