package io.github.doubaomonet;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Preview geometry follows Doubao IME 1.4.5's bundled skin instead of an
 * arbitrary keyboard mockup. The important source values are:
 *
 * - preview/skin width reference: 355 units
 * - candidate/top area: 57 units
 * - 4 keyboard rows, each 57 units
 * - bottom navigation area: ~49 units on the tested Android 16 device
 * - normal key margins: 2.25,4.75,2.25,4.75
 * - row 2 weights: 15,10,10,10,10,10,10,10,15
 * - row 3 weights: 15 + seven 10s + 15
 * - bottom row weights: 70.6,37.6,133.6,38.6,74.6
 *
 * These are taken from assets/skin/default/layout/input_kbd_pinyin26.xml and
 * style.xml in Doubao IME 1.4.5.
 */
final class KeyboardPreviewView extends View {
    private static final float SKIN_W = 355f;
    private static final float CANDIDATE_H = 57f;
    private static final float ROW_H = 57f;
    private static final float NAV_H = 49f;
    private static final float TOTAL_H = CANDIDATE_H + ROW_H * 4f + NAV_H;

    private static final float KEY_MARGIN_X = 2.25f;
    private static final float KEY_MARGIN_Y = 4.75f;

    private final SharedPreferences prefs;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();

    private final boolean night;
    private final int primary;
    private final int onPrimary;
    private final int surfaceContainer;
    private final int surfaceLow;
    private final int surfaceHigh;
    private final int onSurface;
    private final int onSurfaceVariant;

    private int backgroundTint;
    private int letterTint;
    private int functionTint;
    private int candidateTint;
    private int pressedTint;
    private boolean highlightCandidate;
    private boolean highlightAction;
    private boolean syncBottom;

    KeyboardPreviewView(Context context, SharedPreferences prefs) {
        super(context);
        this.prefs = prefs;
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        primary = systemColor(night ? "system_primary_dark" : "system_primary_light",
                night ? 0xffaac7ff : 0xff415f91);
        onPrimary = systemColor(night ? "system_on_primary_dark" : "system_on_primary_light",
                night ? 0xff0b305f : 0xffffffff);
        surfaceContainer = systemColor(night ? "system_surface_container_dark" : "system_surface_container_light",
                night ? 0xff1f2024 : 0xffe7e8ee);
        surfaceLow = systemColor(night ? "system_surface_container_high_dark" : "system_surface_container_low_light",
                night ? 0xff292a2f : 0xfff3f3f9);
        surfaceHigh = systemColor(night ? "system_surface_container_highest_dark" : "system_surface_container_high_light",
                night ? 0xff34353a : 0xffe1e2e8);
        onSurface = systemColor(night ? "system_on_surface_dark" : "system_on_surface_light",
                night ? 0xffe3e2e8 : 0xff1b1c20);
        onSurfaceVariant = systemColor(night ? "system_on_surface_variant_dark" : "system_on_surface_variant_light",
                night ? 0xffc4c7cf : 0xff44474e);
        reload();
    }

    void reload() {
        backgroundTint = prefs.getInt("background_tint", HookConfig.DEFAULT_BACKGROUND_TINT);
        letterTint = prefs.getInt("letter_tint", HookConfig.DEFAULT_LETTER_TINT);
        functionTint = prefs.getInt("function_tint", HookConfig.DEFAULT_FUNCTION_TINT);
        candidateTint = prefs.getInt("candidate_tint", HookConfig.DEFAULT_CANDIDATE_TINT);
        pressedTint = prefs.getInt("pressed_tint", HookConfig.DEFAULT_PRESSED_TINT);
        highlightCandidate = prefs.getBoolean("highlight_first_candidate", true);
        highlightAction = prefs.getBoolean("highlight_action_key", false);
        syncBottom = prefs.getBoolean("sync_system_nav", true);
        invalidate();
    }

    void setTint(String key, int value) {
        if ("background_tint".equals(key)) backgroundTint = value;
        else if ("letter_tint".equals(key)) letterTint = value;
        else if ("function_tint".equals(key)) functionTint = value;
        else if ("candidate_tint".equals(key)) candidateTint = value;
        else if ("pressed_tint".equals(key)) pressedTint = value;
        invalidate();
    }

    void setFlag(String key, boolean value) {
        if ("highlight_first_candidate".equals(key)) highlightCandidate = value;
        else if ("highlight_action_key".equals(key)) highlightAction = value;
        else if ("sync_system_nav".equals(key)) syncBottom = value;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desiredHeight = Math.round(width * TOTAL_H / SKIN_W);
        setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float sx = w / SKIN_W;
        float sy = h / TOTAL_H;

        int body = blend(surfaceContainer, primary, backgroundTint / 100f);
        int letter = blend(surfaceLow, primary, letterTint / 100f);
        int function = blend(surfaceHigh, primary, functionTint / 100f);
        int candidate = blend(surfaceLow, primary, candidateTint / 100f);
        int pressed = blend(surfaceHigh, primary, pressedTint / 100f);
        int action = highlightAction ? primary : function;

        // Real keyboard body + bottom navigation region.
        rect.set(0f, 0f, w, h);
        fill(canvas, rect, body);
        if (!syncBottom) {
            rect.set(0f, sy * (TOTAL_H - NAV_H), w, h);
            fill(canvas, rect, surfaceContainer);
        }

        drawCandidateBar(canvas, sx, sy, candidate);

        float row1 = CANDIDATE_H;
        drawWeightedKeyRow(
                canvas,
                new String[] {"Q","W","E","R","T","Y","U","I","O","P"},
                new String[] {"1","2","3","4","5","6","7","8","9","0"},
                new float[] {10,10,10,10,10,10,10,10,10,10},
                row1,
                letter,
                function,
                sx,
                sy,
                0);

        float row2 = row1 + ROW_H;
        drawWeightedKeyRow(
                canvas,
                new String[] {"A","S","D","F","G","H","J","K","L"},
                new String[] {"-","/","：","；","（","）","～","“","”"},
                new float[] {15,10,10,10,10,10,10,10,15},
                row2,
                letter,
                function,
                sx,
                sy,
                0);

        float row3 = row2 + ROW_H;
        drawWeightedKeyRow(
                canvas,
                new String[] {"⇧","Z","X","C","V","B","N","M","⌫"},
                new String[] {"","@",".","#","、","？","！","…",""},
                new float[] {15,10,10,10,10,10,10,10,15},
                row3,
                letter,
                function,
                sx,
                sy,
                1);

        float row4 = row3 + ROW_H;
        drawBottomRow(canvas, row4, letter, function, action, sx, sy);
        drawNavigation(canvas, sx, sy, body);

        // Small pressed-state sample in the upper-right candidate region. It is deliberately
        // subtle but gives the pressed slider an immediate visible reference.
        rect.set(w - 20f * sx, 8f * sy, w - 7f * sx, 21f * sy);
        fillRound(canvas, rect, pressed, 6f * sx);
    }

    private void drawCandidateBar(Canvas canvas, float sx, float sy, int candidate) {
        float cy = 0f;
        float h = CANDIDATE_H * sy;

        if (highlightCandidate) {
            rect.set(5f * sx, 8f * sy, 62f * sx, 49f * sy);
            fillRound(canvas, rect, candidate, 8f * sx);
        }

        drawText(canvas, "你好", 17f * sx, 35f * sy,
                highlightCandidate ? primary : onSurface, 17f * sx, true);
        drawText(canvas, "你号", 76f * sx, 35f * sy, onSurface, 17f * sx, false);
        drawText(canvas, "拟好", 134f * sx, 35f * sy, onSurface, 17f * sx, false);
        drawText(canvas, "有点", 191f * sx, 35f * sy, onSurfaceVariant, 16f * sx, false);

        // candidate close divider + X, matching the typing candidate bar structure
        paint.setStrokeWidth(Math.max(1f, 0.65f * sx));
        paint.setColor(withAlpha(onSurfaceVariant, 0.32f));
        canvas.drawLine(326f * sx, 11f * sy, 326f * sx, 46f * sy, paint);
        paint.setStrokeWidth(Math.max(1.4f, 1.1f * sx));
        paint.setColor(onSurfaceVariant);
        canvas.drawLine(338f * sx, 20f * sy, 347f * sx, 29f * sy, paint);
        canvas.drawLine(347f * sx, 20f * sy, 338f * sx, 29f * sy, paint);
    }

    private void drawWeightedKeyRow(
            Canvas canvas,
            String[] labels,
            String[] secondary,
            float[] weights,
            float topSkin,
            int letterColor,
            int functionColor,
            float sx,
            float sy,
            int edgeMode) {
        float totalWeight = 0f;
        for (float weight : weights) totalWeight += weight;

        float x = 0f;
        for (int i = 0; i < labels.length; i++) {
            float cellW = SKIN_W * weights[i] / totalWeight;
            float ml = KEY_MARGIN_X;
            float mr = KEY_MARGIN_X;
            boolean functionKey = false;

            if (edgeMode == 1 && i == 0) {
                // cls_button_left_big: 2.25,4.75,8,4.75
                ml = 2.25f;
                mr = 8f;
                functionKey = true;
            } else if (edgeMode == 1 && i == labels.length - 1) {
                // cls_button_right_big: 8,4.75,2.25,4.75
                ml = 8f;
                mr = 2.25f;
                functionKey = true;
            }

            rect.set(
                    (x + ml) * sx,
                    (topSkin + KEY_MARGIN_Y) * sy,
                    (x + cellW - mr) * sx,
                    (topSkin + ROW_H - KEY_MARGIN_Y) * sy);
            fillRound(canvas, rect, functionKey ? functionColor : letterColor, 6f * sx);

            if (functionKey) {
                centerText(canvas, labels[i], rect, onSurface, 18f * sx, false);
            } else {
                centerText(canvas, labels[i], rect, onSurface, 19f * sx, false);
                if (secondary != null && i < secondary.length && secondary[i] != null
                        && !secondary[i].isEmpty()) {
                    drawText(
                            canvas,
                            secondary[i],
                            rect.left + 3.7f * sx,
                            rect.top + 9.5f * sy,
                            onSurfaceVariant,
                            7.5f * sx,
                            false);
                }
            }
            x += cellW;
        }
    }

    private void drawBottomRow(
            Canvas canvas,
            float topSkin,
            int letterColor,
            int functionColor,
            int actionColor,
            float sx,
            float sy) {
        // Exact adaptive width weights used by the normal 26-key bottom row.
        float[] weights = {70.6f, 37.6f, 133.6f, 38.6f, 74.6f};
        float sum = 0f;
        for (float value : weights) sum += value; // 355.0

        float x = 0f;
        for (int i = 0; i < weights.length; i++) {
            float cellW = SKIN_W * weights[i] / sum;
            int bg;
            if (i == 0 || i == 4) bg = i == 4 ? actionColor : functionColor;
            else bg = letterColor;

            rect.set(
                    (x + KEY_MARGIN_X) * sx,
                    (topSkin + KEY_MARGIN_Y) * sy,
                    (x + cellW - KEY_MARGIN_X) * sx,
                    (topSkin + ROW_H - KEY_MARGIN_Y) * sy);
            fillRound(canvas, rect, bg, 6f * sx);

            switch (i) {
                case 0:
                    centerText(canvas, "123", rect, onSurface, 15f * sx, false);
                    break;
                case 1:
                    centerText(canvas, "，", rect, onSurface, 19f * sx, false);
                    drawText(canvas, "。", rect.left + 4f * sx, rect.top + 9f * sy,
                            onSurfaceVariant, 7f * sx, false);
                    break;
                case 2:
                    drawVoiceWave(canvas, rect, onSurfaceVariant, sx, sy);
                    break;
                case 3:
                    drawCnEn(canvas, rect, sx, sy);
                    break;
                case 4:
                    centerText(canvas, "换行", rect,
                            highlightAction ? onPrimary : onSurface, 14f * sx, false);
                    break;
                default:
                    break;
            }
            x += cellW;
        }
    }

    private void drawVoiceWave(Canvas canvas, RectF key, int color, float sx, float sy) {
        float cx = key.centerX();
        float cy = key.centerY();
        float[] heights = {7f, 14f, 20f, 13f, 7f};
        paint.setColor(color);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(1.4f, 2.2f * sx));
        for (int i = 0; i < heights.length; i++) {
            float x = cx + (i - 2) * 5f * sx;
            float half = heights[i] * sy / 2f;
            canvas.drawLine(x, cy - half, x, cy + half, paint);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawCnEn(Canvas canvas, RectF key, float sx, float sy) {
        float cx = key.centerX();
        float cy = key.centerY();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(13f * sx);
        paint.setColor(onSurface);
        canvas.drawText("中", cx - 4f * sx, cy - 2f * sy, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(9f * sx);
        paint.setColor(onSurfaceVariant);
        canvas.drawText("英", cx + 7f * sx, cy + 8f * sy, paint);
    }

    private void drawNavigation(Canvas canvas, float sx, float sy, int body) {
        float top = (TOTAL_H - NAV_H) * sy;
        int nav = syncBottom ? body : surfaceContainer;
        rect.set(0f, top, getWidth(), getHeight());
        fill(canvas, rect, nav);

        // down chevron
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.4f, 1.8f * sx));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(onSurfaceVariant);
        path.reset();
        path.moveTo(27f * sx, top + 21f * sy);
        path.lineTo(33f * sx, top + 27f * sy);
        path.lineTo(39f * sx, top + 21f * sy);
        canvas.drawPath(path, paint);

        // globe-like icon on the right
        float cx = 324f * sx;
        float cy = top + 24f * sy;
        float r = 9f * sx;
        canvas.drawCircle(cx, cy, r, paint);
        canvas.drawOval(new RectF(cx - r * 0.45f, cy - r, cx + r * 0.45f, cy + r), paint);
        canvas.drawLine(cx - r, cy, cx + r, cy, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void fill(Canvas canvas, RectF r, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRect(r, paint);
    }

    private void fillRound(Canvas canvas, RectF r, int color, float radius) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRoundRect(r, radius, radius, paint);
    }

    private void drawText(Canvas canvas, String text, float x, float baseline, int color,
                          float px, boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(px);
        paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(text, x, baseline, paint);
    }

    private void centerText(Canvas canvas, String text, RectF r, int color, float px, boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(px);
        paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float y = r.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, r.centerX(), y, paint);
    }

    private int systemColor(String name, int fallback) {
        int id = getResources().getIdentifier(name, "color", "android");
        if (id == 0) return fallback;
        try {
            return getResources().getColor(id, null);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private int blend(int base, int tint, float amount) {
        float a = Math.max(0f, Math.min(1f, amount));
        return Color.rgb(
                Math.round(Color.red(base) * (1f - a) + Color.red(tint) * a),
                Math.round(Color.green(base) * (1f - a) + Color.green(tint) * a),
                Math.round(Color.blue(base) * (1f - a) + Color.blue(tint) * a));
    }

    private int withAlpha(int color, float alpha) {
        return Color.argb(
                Math.round(255f * alpha),
                Color.red(color),
                Color.green(color),
                Color.blue(color));
    }
}