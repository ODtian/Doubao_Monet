package io.github.doubaomonet;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class SettingsActivity extends Activity {
    private static final String TARGET_PACKAGE = "com.bytedance.android.doubaoime";
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
    private KeyboardPreviewView preview;
    private TextView applyStatus;
    private boolean dirty;

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
        scroll.setClipToPadding(false);
        scroll.setClipChildren(false);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setClipChildren(false);
        root.setClipToPadding(false);
        root.setPadding(dp(22), dp(18), dp(22), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        addHeader();
        addPaletteCard();
        addPreviewCard();
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
                "扩展面板跟随键盘",
                "覆盖工具面板、顶部工具栏、更多候选等仍使用豆包原始灰色的区域。",
                "sync_extended_panels",
                true,
                false);
        root.addView(appearance, cardParams());

        addSectionTitle("混色强度");
        LinearLayout tintCard = newCard();
        TextView tintHint = bodyText("所有混色项统一为 0–100%。0% 只使用对应 Material surface；100% 完全使用当前 Monet primary。", 12.2f, 0.62f);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.leftMargin = dp(14);
        hintParams.rightMargin = dp(14);
        hintParams.topMargin = dp(10);
        hintParams.bottomMargin = dp(4);
        tintCard.addView(tintHint, hintParams);
        addSliderRow(tintCard, "键盘底板", "background_tint", HookConfig.DEFAULT_BACKGROUND_TINT, 100);
        addDivider(tintCard);
        addSliderRow(tintCard, "普通字母键", "letter_tint", HookConfig.DEFAULT_LETTER_TINT, 100);
        addDivider(tintCard);
        addSliderRow(tintCard, "功能键", "function_tint", HookConfig.DEFAULT_FUNCTION_TINT, 100);
        addDivider(tintCard);
        addSliderRow(tintCard, "候选 / 面板高亮", "candidate_tint", HookConfig.DEFAULT_CANDIDATE_TINT, 100);
        addDivider(tintCard);
        addSliderRow(tintCard, "按下态", "pressed_tint", HookConfig.DEFAULT_PRESSED_TINT, 100);
        root.addView(tintCard, cardParams());

        addSectionTitle("键帽细节");
        LinearLayout keyDetail = newCard();
        TextView shadowHint = bodyText("按键阴影 0% 表示关闭；100% 等于豆包原始阴影强度。", 12.2f, 0.62f);
        LinearLayout.LayoutParams shadowHintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        shadowHintParams.leftMargin = dp(14);
        shadowHintParams.rightMargin = dp(14);
        shadowHintParams.topMargin = dp(10);
        shadowHintParams.bottomMargin = dp(4);
        keyDetail.addView(shadowHint, shadowHintParams);
        addSliderRow(keyDetail, "按键阴影", "key_shadow", HookConfig.DEFAULT_KEY_SHADOW, 100);
        root.addView(keyDetail, cardParams());

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
        LinearLayout applyCard = newCard();
        applyStatus = bodyText("当前设置已保存；点击下方按钮可直接刷新当前键盘。", 12.4f, 0.68f);
        applyStatus.setPadding(dp(14), dp(10), dp(14), dp(4));
        applyCard.addView(applyStatus);

        TextView restart = actionButton("应用到当前键盘", true);
        restart.setOnClickListener(v -> applyToKeyboard());
        LinearLayout.LayoutParams restartParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        restartParams.leftMargin = dp(10);
        restartParams.rightMargin = dp(10);
        restartParams.topMargin = dp(8);
        applyCard.addView(restart, restartParams);

        TextView reset = actionButton("恢复默认设置", false);
        reset.setOnClickListener(v -> {
            int revision = prefs.getInt("config_rev", 0);
            prefs.edit().clear().putInt("config_rev", revision).commit();
            applyToKeyboard();
            recreate();
        });
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        resetParams.leftMargin = dp(10);
        resetParams.rightMargin = dp(10);
        resetParams.topMargin = dp(8);
        resetParams.bottomMargin = dp(10);
        applyCard.addView(reset, resetParams);
        root.addView(applyCard, cardParams());

        TextView footer = bodyText(
                "拖动时预览立即更新；松手或输入数字后写入设置。实际豆包键盘请点“应用到当前键盘”。\n"
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
        version.setText("0.3.3");
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
        HookConfig.Values config = new HookConfig.Values(
                true,
                true,
                prefs.getBoolean("sync_extended_panels", true),
                prefs.getBoolean("highlight_first_candidate", true),
                prefs.getBoolean("highlight_action_key", false),
                prefs.getInt("background_tint", HookConfig.DEFAULT_BACKGROUND_TINT),
                prefs.getInt("letter_tint", HookConfig.DEFAULT_LETTER_TINT),
                prefs.getInt("function_tint", HookConfig.DEFAULT_FUNCTION_TINT),
                prefs.getInt("candidate_tint", HookConfig.DEFAULT_CANDIDATE_TINT),
                prefs.getInt("pressed_tint", HookConfig.DEFAULT_PRESSED_TINT),
                prefs.getInt("key_shadow", HookConfig.DEFAULT_KEY_SHADOW));
        dots.addView(colorDot(MonetSkin.primary(this)));
        LinearLayout.LayoutParams secondDot = new LinearLayout.LayoutParams(dp(30), dp(30));
        secondDot.leftMargin = -dp(6);
        dots.addView(colorDot(MonetSkin.keyboardSurface(this, config)), secondDot);
        LinearLayout.LayoutParams thirdDot = new LinearLayout.LayoutParams(dp(30), dp(30));
        thirdDot.leftMargin = -dp(6);
        dots.addView(colorDot(MonetSkin.functionKeySurface(this, config)), thirdDot);
        card.addView(dots);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        TextView title = bodyText("当前动态色", 15.2f, 1f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textBlock.addView(title);
        TextView sub = bodyText("从左到右：Monet primary、当前键盘底板、当前功能键；与实际注入使用同一取色链路。", 12.5f, 0.68f);
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

    private void addPreviewCard() {
        addSectionTitle("实时预览");
        LinearLayout card = newCard();
        preview = new KeyboardPreviewView(this, prefs);

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setGravity(Gravity.CENTER);
        final String[] modeLabels = {"普通", "输入中", "工具", "语音"};
        final TextView[] modeButtons = new TextView[modeLabels.length];
        for (int i = 0; i < modeLabels.length; i++) {
            final int selected = i;
            TextView b = new TextView(this);
            b.setText(modeLabels[i]);
            b.setTextSize(12.5f);
            b.setGravity(Gravity.CENTER);
            b.setPadding(dp(8), dp(8), dp(8), dp(8));
            b.setTextColor(i == 0 ? onPrimaryContainer : onSurfaceVariant);
            b.setBackground(roundRect(i == 0 ? primaryContainer : surfaceContainer, 12));
            b.setOnClickListener(v -> {
                preview.setMode(selected);
                for (int j = 0; j < modeButtons.length; j++) {
                    modeButtons[j].setTextColor(j == selected ? onPrimaryContainer : onSurfaceVariant);
                    modeButtons[j].setBackground(roundRect(j == selected ? primaryContainer : surfaceContainer, 12));
                }
            });
            modeButtons[i] = b;
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(40), 1f);
            if (i > 0) bp.leftMargin = dp(6);
            modeRow.addView(b, bp);
        }
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        modeParams.leftMargin = dp(10);
        modeParams.rightMargin = dp(10);
        modeParams.topMargin = dp(10);
        card.addView(modeRow, modeParams);

        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        previewParams.leftMargin = dp(8);
        previewParams.rightMargin = dp(8);
        previewParams.topMargin = dp(8);
        card.addView(preview, previewParams);

        TextView hint = bodyText("预览可切换普通、拼音输入中、工具面板和语音状态；配色跟随滑杆实时变化。", 12.2f, 0.62f);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.leftMargin = dp(14);
        hintParams.rightMargin = dp(14);
        hintParams.topMargin = dp(8);
        hintParams.bottomMargin = dp(10);
        card.addView(hint, hintParams);
        root.addView(card, cardParams());
    }

    private TextView tickLabel(String text) {
        TextView v = bodyText(text, 10.5f, 0.48f);
        v.setText(text);
        return v;
    }

    private TextView actionButton(String text, boolean primaryButton) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(14.5f);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(16), dp(13), dp(16), dp(13));
        if (primaryButton) {
            v.setTextColor(onPrimaryContainer);
            v.setBackground(roundRect(primaryContainer, 16));
        } else {
            v.setTextColor(onSurface);
            v.setBackground(roundRect(surfaceContainer, 16));
        }
        v.setClickable(true);
        v.setFocusable(true);
        return v;
    }

    private void markDirty() {
        dirty = true;
        if (applyStatus != null) {
            applyStatus.setText("有未应用更改 · 预览已更新，实际键盘仍是旧配置。需要点下方按钮刷新。");
            applyStatus.setTextColor(primary);
            applyStatus.setAlpha(1f);
        }
    }

    private void applyToKeyboard() {
        View focused = getCurrentFocus();
        if (focused != null) focused.clearFocus();

        int rev = prefs.getInt("config_rev", 0) + 1;
        prefs.edit().putInt("config_rev", rev).commit();

        Intent apply = new Intent(HookConfig.ACTION_APPLY);
        apply.setPackage(TARGET_PACKAGE);
        apply.putExtras(HookConfig.bundleFromPrefs(prefs));
        apply.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        sendBroadcast(apply);

        dirty = false;
        if (applyStatus != null) {
            applyStatus.setText("设置已发送到豆包输入法；当前键盘会原位刷新。");
            applyStatus.setTextColor(onSurfaceVariant);
            applyStatus.setAlpha(0.78f);
        }
        Toast.makeText(this, "设置已应用", Toast.LENGTH_SHORT).show();
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
        card.setElevation(dp(2));
        card.setClipChildren(false);
        card.setClipToPadding(false);
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        lp.bottomMargin = dp(2);
        return lp;
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
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(key, isChecked).commit();
            if (preview != null) preview.setFlag(key, isChecked);
            markDirty();
        });
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

    private HookConfig.Values configForSlider(String key, int value) {
        int background = prefs.getInt("background_tint", HookConfig.DEFAULT_BACKGROUND_TINT);
        int letter = prefs.getInt("letter_tint", HookConfig.DEFAULT_LETTER_TINT);
        int function = prefs.getInt("function_tint", HookConfig.DEFAULT_FUNCTION_TINT);
        int candidate = prefs.getInt("candidate_tint", HookConfig.DEFAULT_CANDIDATE_TINT);
        int pressed = prefs.getInt("pressed_tint", HookConfig.DEFAULT_PRESSED_TINT);
        int shadow = prefs.getInt("key_shadow", HookConfig.DEFAULT_KEY_SHADOW);
        if ("background_tint".equals(key)) background = value;
        else if ("letter_tint".equals(key)) letter = value;
        else if ("function_tint".equals(key)) function = value;
        else if ("candidate_tint".equals(key)) candidate = value;
        else if ("pressed_tint".equals(key)) pressed = value;
        else if ("key_shadow".equals(key)) shadow = value;
        return new HookConfig.Values(
                prefs.getBoolean("enabled", true),
                true,
                prefs.getBoolean("sync_extended_panels", true),
                prefs.getBoolean("highlight_first_candidate", true),
                prefs.getBoolean("highlight_action_key", false),
                background, letter, function, candidate, pressed, shadow);
    }

    private int sliderColor(String key, int value) {
        HookConfig.Values c = configForSlider(key, value);
        if ("background_tint".equals(key)) return MonetSkin.keyboardSurface(this, c);
        if ("letter_tint".equals(key)) return MonetSkin.letterKeySurface(this, c);
        if ("function_tint".equals(key)) return MonetSkin.functionKeySurface(this, c);
        if ("candidate_tint".equals(key)) return MonetSkin.candidateBackground(this, c);
        if ("pressed_tint".equals(key)) return MonetSkin.functionKeyPressedSurface(this, c);
        return onSurfaceVariant;
    }

    private void tintSlider(SeekBar seek, String key, int value) {
        int c = sliderColor(key, value);
        seek.setThumbTintList(ColorStateList.valueOf(c));
        seek.setProgressTintList(ColorStateList.valueOf(c));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(withAlpha(onSurfaceVariant, 0.18f)));
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
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        EditText number = new EditText(this);
        number.setSingleLine(true);
        number.setInputType(InputType.TYPE_CLASS_NUMBER);
        number.setImeOptions(EditorInfo.IME_ACTION_DONE);
        number.setTextSize(13f);
        number.setTextColor(onSurface);
        number.setGravity(Gravity.CENTER);
        number.setSelectAllOnFocus(true);
        number.setPadding(dp(6), dp(5), dp(6), dp(5));
        number.setBackground(roundRect(surfaceContainer, 10));
        LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(dp(52), dp(38));
        header.addView(number, numberParams);

        TextView percent = bodyText("%", 12.5f, 0.62f);
        percent.setGravity(Gravity.CENTER);
        header.addView(percent, new LinearLayout.LayoutParams(dp(22), dp(38)));
        box.addView(header);

        SeekBar seek = new SeekBar(this);
        seek.setMax(maxValue);
        seek.setKeyProgressIncrement(1);
        int initial = Math.max(0, Math.min(maxValue, prefs.getInt(key, defaultValue)));
        seek.setProgress(initial);
        number.setText(String.valueOf(initial));
        tintSlider(seek, key, initial);

        final boolean[] updating = {false};
        Runnable commitNumber = () -> {
            if (updating[0]) return;
            int value;
            try {
                value = Integer.parseInt(number.getText().toString().trim());
            } catch (Throwable ignored) {
                value = seek.getProgress();
            }
            value = Math.max(0, Math.min(maxValue, value));
            updating[0] = true;
            seek.setProgress(value);
            number.setText(String.valueOf(value));
            number.setSelection(number.length());
            updating[0] = false;
            prefs.edit().putInt(key, value).commit();
            tintSlider(seek, key, value);
            if (preview != null) preview.setTint(key, value);
            markDirty();
        };

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!updating[0]) {
                    updating[0] = true;
                    number.setText(String.valueOf(progress));
                    number.setSelection(number.length());
                    updating[0] = false;
                }
                tintSlider(seekBar, key, progress);
                if (preview != null) preview.setTint(key, progress);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int value = seekBar.getProgress();
                prefs.edit().putInt(key, value).commit();
                tintSlider(seekBar, key, value);
                if (preview != null) preview.setTint(key, value);
                markDirty();
            }
        });

        number.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commitNumber.run();
                number.clearFocus();
                return true;
            }
            return false;
        });
        number.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) commitNumber.run();
        });

        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        seekParams.topMargin = dp(1);
        box.addView(seek, seekParams);

        LinearLayout ticks = new LinearLayout(this);
        ticks.setOrientation(LinearLayout.HORIZONTAL);
        ticks.addView(tickLabel("0"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView mid = tickLabel(String.valueOf(maxValue / 2));
        mid.setGravity(Gravity.CENTER);
        ticks.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView max = tickLabel(String.valueOf(maxValue));
        max.setGravity(Gravity.END);
        ticks.addView(max, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams ticksParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ticksParams.leftMargin = dp(14);
        ticksParams.rightMargin = dp(14);
        ticksParams.topMargin = -dp(5);
        box.addView(ticks, ticksParams);
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