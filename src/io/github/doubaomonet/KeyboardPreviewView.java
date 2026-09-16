package io.github.doubaomonet;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class KeyboardPreviewView extends View {
    private final SharedPreferences prefs;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

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
        int height = dp(238);
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        int body = blend(surfaceContainer, primary, backgroundTint / 100f);
        int letter = blend(surfaceLow, primary, letterTint / 100f);
        int function = blend(surfaceHigh, primary, functionTint / 100f);
        int candidate = blend(surfaceLow, primary, candidateTint / 100f);
        int pressed = blend(surfaceHigh, primary, pressedTint / 100f);
        int action = highlightAction ? primary : function;

        float radius = dp(22);
        rect.set(0, 0, w, h);
        fillRound(canvas, rect, body, radius);

        float navH = dp(27);
        if (syncBottom) {
            rect.set(0, h - navH, w, h);
            fill(canvas, rect, body);
        }

        float pad = dp(10);
        float candidateTop = dp(9);
        float candidateH = dp(38);
        if (highlightCandidate) {
            rect.set(pad, candidateTop, dp(84), candidateTop + candidateH);
            fillRound(canvas, rect, candidate, dp(12));
        }
        drawText(canvas, "你好", dp(24), candidateTop + dp(26),
                highlightCandidate ? primary : onSurface, 15, true);
        drawText(canvas, "你号", dp(100), candidateTop + dp(26), onSurface, 15, false);
        drawText(canvas, "拟好", dp(156), candidateTop + dp(26), onSurfaceVariant, 15, false);

        float gridTop = dp(55);
        float gap = dp(5);
        float keyH = dp(39);
        String[] row1 = {"Q","W","E","R","T","Y","U","I","O","P"};
        String[] row2 = {"A","S","D","F","G","H","J","K","L"};
        String[] row3 = {"⇧","Z","X","C","V","B","N","M","⌫"};
        drawKeyRow(canvas, row1, gridTop, pad, gap, keyH, letter, onSurface, false);
        drawKeyRow(canvas, row2, gridTop + keyH + gap, pad + dp(14), gap, keyH, letter, onSurface, false);
        drawKeyRow(canvas, row3, gridTop + (keyH + gap) * 2, pad, gap, keyH, letter, onSurface, true);

        float bottomY = gridTop + (keyH + gap) * 3;
        float left = pad;
        float bottomKeyH = dp(40);
        float smallW = dp(64);
        rect.set(left, bottomY, left + smallW, bottomY + bottomKeyH);
        fillRound(canvas, rect, function, dp(11));
        centerText(canvas, "123", rect, onSurface, 13, false);

        left += smallW + gap;
        rect.set(left, bottomY, left + dp(39), bottomY + bottomKeyH);
        fillRound(canvas, rect, letter, dp(11));
        centerText(canvas, "，", rect, onSurface, 15, false);

        left += dp(39) + gap;
        float actionW = dp(76);
        float centerW = w - pad - left - actionW - gap;
        rect.set(left, bottomY, left + centerW, bottomY + bottomKeyH);
        fillRound(canvas, rect, letter, dp(11));
        centerText(canvas, "语音", rect, onSurfaceVariant, 12, false);

        left += centerW + gap;
        rect.set(left, bottomY, w - pad, bottomY + bottomKeyH);
        fillRound(canvas, rect, action, dp(11));
        centerText(canvas, "换行", rect, highlightAction ? onPrimary : onSurface, 12, true);

        // tiny pressed-state sample, so this slider has a visible effect in preview
        rect.set(w - dp(27), dp(8), w - dp(9), dp(26));
        fillRound(canvas, rect, pressed, dp(9));
    }

    private void drawKeyRow(Canvas canvas, String[] keys, float y, float left, float gap,
                            float keyH, int letterColor, int textColor, boolean functionEdges) {
        float usable = getWidth() - left * 2 - gap * (keys.length - 1);
        float keyW = usable / keys.length;
        for (int i = 0; i < keys.length; i++) {
            float x = left + i * (keyW + gap);
            int bg = letterColor;
            if (functionEdges && (i == 0 || i == keys.length - 1)) {
                bg = blend(surfaceHigh, primary, functionTint / 100f);
            }
            rect.set(x, y, x + keyW, y + keyH);
            fillRound(canvas, rect, bg, dp(10));
            centerText(canvas, keys[i], rect, textColor, functionEdges && (i == 0 || i == keys.length - 1) ? 16 : 15, false);
        }
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
                          float sp, boolean bold) {
        paint.setColor(color);
        paint.setTextSize(sp(sp));
        paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(text, x, baseline, paint);
    }

    private void centerText(Canvas canvas, String text, RectF r, int color, float sp, boolean bold) {
        paint.setColor(color);
        paint.setTextSize(sp(sp));
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
