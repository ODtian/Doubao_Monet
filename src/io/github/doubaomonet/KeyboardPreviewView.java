package io.github.doubaomonet;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.view.View;

import java.io.InputStream;

final class KeyboardPreviewView extends View {
    static final int MODE_NORMAL = 0;
    static final int MODE_COMPOSING = 1;
    static final int MODE_TOOLBOX = 2;
    static final int MODE_ASR = 3;

    private static final float REF_W = 1240f;
    private static final float REF_H = 1072f;
    private static final float KEY_RADIUS = 18f;

    private static final float[][] ROW1 = {
            {17,196,106,148},{139,196,106,148},{261,196,107,148},{384,196,106,148},{506,196,106,148},
            {628,196,106,148},{750,196,106,148},{872,196,107,148},{995,196,106,148},{1117,196,106,148}
    };
    private static final float[][] ROW2 = {
            {78,376,106,148},{200,376,107,148},{323,376,106,148},{445,376,106,148},{567,376,106,148},
            {689,376,106,148},{811,376,107,148},{934,376,106,148},{1056,376,106,148}
    };
    private static final float[][] ROW3 = {
            {200,556,106,148},{322,556,107,148},{445,556,106,148},{567,556,106,148},
            {689,556,106,148},{811,556,107,148},{934,556,106,148}
    };
    private static final float[] SHIFT = {17,556,147,148};
    private static final float[] DELETE = {1076,556,147,148};
    private static final float[][] BOTTOM = {
            {17,736,227,148},{260,736,113,148},{389,736,444,148},{849,736,117,148},{982,736,241,148}
    };

    private static final class Template {
        Bitmap fg;
        Bitmap body;
        Bitmap letter;
        Bitmap function;
        Bitmap action;
        Bitmap candidate;

        void recycle() {
            recycleOne(fg); recycleOne(body); recycleOne(letter);
            recycleOne(function); recycleOne(action); recycleOne(candidate);
            fg = body = letter = function = action = candidate = null;
        }

        private static void recycleOne(Bitmap b) {
            if (b != null && !b.isRecycled()) b.recycle();
        }
    }

    private final SharedPreferences prefs;
    private final Paint layerPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF shadowRect = new RectF();

    private int backgroundTint;
    private int letterTint;
    private int functionTint;
    private int candidateTint;
    private int pressedTint;
    private int shadowStrength;
    private boolean highlightCandidate;
    private boolean highlightAction;
    private int mode = MODE_NORMAL;
    private Template template;

    KeyboardPreviewView(Context context, SharedPreferences prefs) {
        super(context);
        this.prefs = prefs;
        MonetSkin.refreshPalette(getResources());
        reload();
        loadTemplate();
    }

    void reload() {
        backgroundTint = prefs.getInt("background_tint", HookConfig.DEFAULT_BACKGROUND_TINT);
        letterTint = prefs.getInt("letter_tint", HookConfig.DEFAULT_LETTER_TINT);
        functionTint = prefs.getInt("function_tint", HookConfig.DEFAULT_FUNCTION_TINT);
        candidateTint = prefs.getInt("candidate_tint", HookConfig.DEFAULT_CANDIDATE_TINT);
        pressedTint = prefs.getInt("pressed_tint", HookConfig.DEFAULT_PRESSED_TINT);
        shadowStrength = prefs.getInt("key_shadow", HookConfig.DEFAULT_KEY_SHADOW);
        highlightCandidate = prefs.getBoolean("highlight_first_candidate", true);
        highlightAction = prefs.getBoolean("highlight_action_key", false);
        invalidate();
    }

    void setTint(String key, int value) {
        value = Math.max(0, Math.min(100, value));
        if ("background_tint".equals(key)) backgroundTint = value;
        else if ("letter_tint".equals(key)) letterTint = value;
        else if ("function_tint".equals(key)) functionTint = value;
        else if ("candidate_tint".equals(key)) candidateTint = value;
        else if ("pressed_tint".equals(key)) pressedTint = value;
        else if ("key_shadow".equals(key)) shadowStrength = value;
        invalidate();
    }

    void setFlag(String key, boolean value) {
        if ("highlight_first_candidate".equals(key)) highlightCandidate = value;
        else if ("highlight_action_key".equals(key)) highlightAction = value;
        invalidate();
    }

    void setMode(int value) {
        int next = Math.max(MODE_NORMAL, Math.min(MODE_ASR, value));
        if (next == mode) return;
        mode = next;
        loadTemplate();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desired = Math.round(width * REF_H / REF_W);
        setMeasuredDimension(width, resolveSize(desired, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (template == null || getWidth() <= 0 || getHeight() <= 0) return;

        HookConfig.Values c = new HookConfig.Values(
                true, true, true, highlightCandidate, highlightAction,
                backgroundTint, letterTint, functionTint, candidateTint, pressedTint, shadowStrength);
        int body = MonetSkin.keyboardSurface(getContext(), c);
        int letter = MonetSkin.letterKeySurface(getContext(), c);
        int function = MonetSkin.functionKeySurface(getContext(), c);
        int candidate = highlightCandidate
                ? MonetSkin.candidateBackground(getContext(), c)
                : body;
        int action = highlightAction ? MonetSkin.primary(getContext()) : function;

        float sx = getWidth() / REF_W;
        float sy = getHeight() / REF_H;
        drawMask(canvas, template.body, body, sx, sy);
        if (mode != MODE_TOOLBOX) drawKeyShadows(canvas, sx, sy);
        drawMask(canvas, template.letter, letter, sx, sy);
        drawMask(canvas, template.function, function, sx, sy);
        drawMask(canvas, template.action, action, sx, sy);
        drawMask(canvas, template.candidate, candidate, sx, sy);
        drawBitmap(canvas, template.fg, sx, sy, null);
    }

    private void drawMask(Canvas canvas, Bitmap bitmap, int color, float sx, float sy) {
        if (bitmap == null) return;
        layerPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        drawBitmap(canvas, bitmap, sx, sy, layerPaint);
        layerPaint.setColorFilter(null);
    }

    private void drawBitmap(Canvas canvas, Bitmap bitmap, float sx, float sy, Paint paint) {
        if (bitmap == null) return;
        int save = canvas.save();
        canvas.scale(sx, sy);
        canvas.drawBitmap(bitmap, 0f, 0f, paint);
        canvas.restoreToCount(save);
    }

    private void drawKeyShadows(Canvas canvas, float sx, float sy) {
        if (shadowStrength <= 0) return;
        int alpha = Math.round(46f * shadowStrength / 100f);
        shadowPaint.setColor(Color.argb(alpha, 0, 0, 0));
        for (float[] r : ROW1) shadow(canvas, r, sx, sy);
        for (float[] r : ROW2) shadow(canvas, r, sx, sy);
        for (float[] r : ROW3) shadow(canvas, r, sx, sy);
        shadow(canvas, SHIFT, sx, sy);
        shadow(canvas, DELETE, sx, sy);
        for (float[] r : BOTTOM) shadow(canvas, r, sx, sy);
    }

    private void shadow(Canvas canvas, float[] r, float sx, float sy) {
        float dx = 0f;
        float dy = 4f;
        shadowRect.set(
                (r[0] + dx) * sx,
                (r[1] + dy) * sy,
                (r[0] + r[2] + dx) * sx,
                (r[1] + r[3] + dy) * sy);
        canvas.drawRoundRect(shadowRect, KEY_RADIUS * sx, KEY_RADIUS * sy, shadowPaint);
    }

    private void loadTemplate() {
        if (template != null) template.recycle();
        String name;
        if (mode == MODE_COMPOSING) name = "composing";
        else if (mode == MODE_TOOLBOX) name = "tools";
        else if (mode == MODE_ASR) name = "voice";
        else name = "normal";

        Template t = new Template();
        t.fg = load("preview_" + name + "_fg.png");
        t.body = load("preview_" + name + "_body.png");
        t.letter = load("preview_" + name + "_letter.png");
        t.function = load("preview_" + name + "_function.png");
        t.action = load("preview_" + name + "_action.png");
        t.candidate = load("preview_" + name + "_candidate.png");
        template = t;
    }

    private Bitmap load(String path) {
        try (InputStream in = getContext().getAssets().open(path)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeStream(in, null, options);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (template != null) {
            template.recycle();
            template = null;
        }
    }
}
