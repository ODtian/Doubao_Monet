package io.github.doubaomonet;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout root;
    private boolean night;
    private int surface;
    private int surfaceLow;
    private int surfaceContainer;
    private int onSurface;
    private int onSurfaceVariant;
    private int primary;
    private int primaryContainer;
    private int onPrimaryContainer;
    private int outlineVariant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        setTheme(night
                ? android.R.style.Theme_Material_NoActionBar
                : android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(HookConfig.FILE, MODE_PRIVATE);
        loadColors();
        getWindow().setStatusBarColor(surface);
        getWindow().setNavigationBarColor(surface);
        if (!night) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(surface);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        addHeader();
        addPaletteCard();
        addSectionTitle("外观");
        LinearLayout appearance = newCard();
        addSwitchRow(appearance,
                "启用 Monet 配色",
                "关闭后恢复豆包原始配色，重新创建键盘后生效。",
                "enabled",
                true,
                false);
        addDivider(appearance);
        addSwitchRow(appearance,
                "底部区域跟随键盘",
                "统一候选栏、键盘主体和底部导航区域。",
                "sync_system_nav",
                true,
                false);
        addDivider(appearance);
        addSwitchRow(appearance,
                "扩展面板跟随键盘",
                "覆盖工具面板、顶部工具栏、更多候选等仍使用豆包原始灰色的区域。",
                "sync_extended_panels",
                true,
                false);
        root.addView(appearance, cardParams());

        addSectionTitle("混色强度");
        LinearLayout tintCard = newCard();
        TextView tintHint = bodyText("0% 只使用对应 Material surface；数值越大，越向当前 Monet primary 靠拢。", 12.2f, 0.62f);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.leftMargin = dp(14);
        hintParams.rightMargin = dp(14);
        hintParams.topMargin = dp(10);
        hintParams.bottomMargin = dp(4);
        tintCard.addView(tintHint, hintParams);
        addSliderRow(tintCard, "键盘底板", "background_tint", HookConfig.DEFAULT_BACKGROUND_TINT, 30);
        addDivider(tintCard);
        addSliderRow(tintCard, "普通字母键", "letter_tint", HookConfig.DEFAULT_LETTER_TINT, 30);
        addDivider(tintCard);
        addSliderRow(tintCard, "功能键", "function_tint", HookConfig.DEFAULT_FUNCTION_TINT, 35);
        addDivider(tintCard);
        addSliderRow(tintCard, "候选 / 面板高亮", "candidate_tint", HookConfig.DEFAULT_CANDIDATE_TINT, 30);
        addDivider(tintCard);
        addSliderRow(tintCard, "按下态", "pressed_tint", HookConfig.DEFAULT_PRESSED_TINT, 45);
        root.addView(tintCard, cardParams());

        addSectionTitle("候选与动作键");
        LinearLayout behavior = newCard();
        addSwitchRow(behavior,
                "首候选使用强调色",
                "替换豆包固定蓝色，并让首候选背景更柔和。",
                "highlight_first_candidate",
                true,
                false);
        addDivider(behavior);
        addSwitchRow(behavior,
                "突出搜索 / 确定键",
                "默认与其它功能键同色；开启后使用更明显的强调色。",
                "highlight_action_key",
                false,
                false);
        root.addView(behavior, cardParams());

        addSectionTitle("应用");
        TextView reset = new TextView(this);
        reset.setText("恢复默认设置");
        reset.setTextSize(15);
        reset.setGravity(Gravity.CENTER);
        reset.setTextColor(onPrimaryContainer);
        reset.setBackground(roundRect(primaryContainer, 18));
        reset.setPadding(dp(18), dp(14), dp(18), dp(14));
        reset.setClickable(true);
        reset.setFocusable(true);
        reset.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            recreate();
        });
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        resetParams.topMargin = dp(2);
        root.addView(reset, resetParams);

        TextView footer = bodyText(
                "修改后收起并重新唤起键盘即可刷新；少数界面需要重启一次豆包输入法。\n"
                        + "适配：豆包输入法 1.4.5 · Android 12+ · Vector / Xposed compatible",
                12.2f,
                0.68f);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        footerParams.topMargin = dp(18);
        root.addView(footer, footerParams);
    }

    private void loadColors() {
        surface = systemColor(night ? "system_surface_dark" : "system_surface_light",
                night ? 0xff111318 : 0xfff9f9ff);
        surfaceLow = systemColor(night ? "system_surface_container_low_dark" : "system_surface_container_low_light",
                night ? 0xff1b1c20 : 0xfff3f3f9);
        surfaceContainer = systemColor(night ? "system_surface_container_dark" : "system_surface_container_light",
                night ? 0xff1f2024 : 0xffe7e8ee);
        onSurface = systemColor(night ? "system_on_surface_dark" : "system_on_surface_light",
                night ? 0xffe3e2e8 : 0xff1b1c20);
        onSurfaceVariant = systemColor(night ? "system_on_surface_variant_dark" : "system_on_surface_variant_light",
                night ? 0xffc4c7cf : 0xff44474e);
        primary = systemColor(night ? "system_primary_dark" : "system_primary_light",
                night ? 0xffaac7ff : 0xff415f91);
        primaryContainer = systemColor(night ? "system_primary_container_dark" : "system_primary_container_light",
                night ? 0xff284777 : 0xffd6e3ff);
        onPrimaryContainer = systemColor(night ? "system_on_primary_container_dark" : "system_on_primary_container_light",
                night ? 0xffd6e3ff : 0xff001b3e);
        outlineVariant = systemColor(night ? "system_outline_variant_dark" : "system_outline_variant_light",
                night ? 0xff44474e : 0xffc4c7cf);
    }

    private void addHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("Doubao Monet");
        title.setTextColor(onSurface);
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textBlock.addView(title);

        TextView subtitle = bodyText("让豆包输入法跟随系统 Material You 动态色", 13.5f, 0.78f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(2);
        textBlock.addView(subtitle, subtitleParams);

        row.addView(textBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView version = new TextView(this);
        version.setText("0.3.1");
        version.setTextSize(12);
        version.setTextColor(onPrimaryContainer);
        version.setGravity(Gravity.CENTER);
        version.setPadding(dp(12), dp(7), dp(12), dp(7));
        version.setBackground(roundRect(primaryContainer, 999));
        row.addView(version);

        root.addView(row);
    }

    private void addPaletteCard() {
        LinearLayout card = newCard();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.addView(colorDot(primary));
        LinearLayout.LayoutParams secondDot = new LinearLayout.LayoutParams(dp(30), dp(30));
        secondDot.leftMargin = -dp(6);
        View surfaceDot = colorDot(surfaceContainer);
        dots.addView(surfaceDot, secondDot);
        LinearLayout.LayoutParams thirdDot = new LinearLayout.LayoutParams(dp(30), dp(30));
        thirdDot.leftMargin = -dp(6);
        int functionTint = prefs.getInt("function_tint", HookConfig.DEFAULT_FUNCTION_TINT);
        View functionDot = colorDot(blend(surfaceContainer, primary, functionTint / 100f));
        dots.addView(functionDot, thirdDot);
        card.addView(dots);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        TextView title = bodyText("当前动态色", 15.2f, 1f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textBlock.addView(title);
        TextView sub = bodyText("直接读取 Android framework 的 Monet 色板", 12.5f, 0.68f);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(2);
        textBlock.addView(sub, subParams);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);
        textParams.leftMargin = dp(14);
        card.addView(textBlock, textParams);

        LinearLayout.LayoutParams params = cardParams();
        params.topMargin = dp(18);
        root.addView(card, params);
    }

    private void addSectionTitle(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(13.2f);
        v.setTextColor(onSurfaceVariant);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(24);
        lp.bottomMargin = dp(8);
        root.addView(v, lp);
    }

    private LinearLayout newCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(4), dp(4), dp(4), dp(4));
        card.setBackground(roundRect(surfaceLow, 22));
        card.setElevation(dp(1));
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void addSwitchRow(
            LinearLayout parent,
            String title,
            String summary,
            String key,
            boolean defaultValue,
            boolean disabled) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(10), dp(12));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = bodyText(title, 15.2f, disabled ? 0.45f : 1f);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labels.addView(titleView);
        TextView summaryView = bodyText(summary, 12.2f, disabled ? 0.35f : 0.62f);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = dp(3);
        labels.addView(summaryView, summaryParams);
        row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(this);
        toggle.setShowText(false);
        toggle.setChecked(prefs.getBoolean(key, defaultValue));
        toggle.setEnabled(!disabled);
        tintSwitch(toggle);
        toggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(key, isChecked).apply());
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        toggleParams.leftMargin = dp(8);
        row.addView(toggle, toggleParams);

        row.setOnClickListener(v -> {
            if (toggle.isEnabled()) toggle.toggle();
        });
        parent.addView(row);
    }

    private void addSliderRow(
            LinearLayout parent,
            String title,
            String key,
            int defaultValue,
            int maxValue) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(10), dp(14), dp(10));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = bodyText(title, 14.5f, 1f);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(titleView, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));

        TextView valueView = bodyText("", 13f, 0.72f);
        valueView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        header.addView(valueView, new LinearLayout.LayoutParams(
                dp(52),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(header);

        SeekBar seek = new SeekBar(this);
        seek.setMax(maxValue);
        int initial = Math.max(0, Math.min(maxValue, prefs.getInt(key, defaultValue)));
        seek.setProgress(initial);
        valueView.setText(initial + "%");
        seek.setThumbTintList(ColorStateList.valueOf(primary));
        seek.setProgressTintList(ColorStateList.valueOf(primary));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(withAlpha(onSurfaceVariant, 0.18f)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueView.setText(progress + "%");
                if (fromUser) prefs.edit().putInt(key, progress).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        seekParams.topMargin = dp(2);
        box.addView(seek, seekParams);
        parent.addView(box);
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(withAlpha(outlineVariant, night ? 0.42f : 0.55f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1));
        lp.leftMargin = dp(16);
        lp.rightMargin = dp(16);
        parent.addView(divider, lp);
    }

    private void tintSwitch(Switch toggle) {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] {}
        };
        toggle.setThumbTintList(new ColorStateList(
                states,
                new int[] { primary, onSurfaceVariant }));
        toggle.setTrackTintList(new ColorStateList(
                states,
                new int[] { withAlpha(primary, 0.42f), withAlpha(onSurfaceVariant, 0.20f) }));
    }

    private TextView bodyText(String text, float size, float alpha) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(onSurface);
        v.setAlpha(alpha);
        v.setLineSpacing(0f, 1.12f);
        return v;
    }

    private View colorDot(int color) {
        View v = new View(this);
        v.setBackground(oval(color));
        v.setElevation(dp(2));
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(30), dp(30)));
        return v;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        d.setStroke(dp(1), withAlpha(outlineVariant, 0.65f));
        return d;
    }

    private int systemColor(String name, int fallback) {
        int id = getResources().getIdentifier(name, "color", "android");
        if (id == 0) return fallback;
        try {
            return getColor(id);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}