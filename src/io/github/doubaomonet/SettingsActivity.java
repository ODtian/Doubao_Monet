package io.github.doubaomonet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        setTheme(night ? android.R.style.Theme_Material_NoActionBar : android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(HookConfig.FILE, MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(22), dp(24), dp(36));
        root.setBackgroundColor(systemColor(
                night ? "system_surface_dark" : "system_surface_light",
                night ? 0xff111318 : 0xfff9f9ff));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        addTitle("Doubao Monet");
        addBody("豆包输入法 Material You / Monet 动态配色模块");
        addPalettePreview(night);

        addSection("模块");
        addSwitch(
                "启用模块",
                "关闭后需要重启豆包输入法进程才能完全恢复原始配色。",
                "enabled",
                true);
        addSwitch(
                "统一键盘与底部导航区",
                "让键盘主体、候选栏和最下方收起键盘/地球区域使用同一 Monet surface。",
                "sync_system_nav",
                true);

        addSection("键盘元素");
        addSwitch(
                "普通字母键轻微染色",
                "默认保留接近白色的 Material 键帽；开启后字母键也会带一点壁纸色。",
                "tinted_letter_keys",
                false);
        addSwitch(
                "首候选使用 Monet 强调色",
                "替换豆包固定的 #4F84FF 首候选蓝，并同步拼音高亮色。",
                "highlight_first_candidate",
                true);
        addSwitch(
                "突出搜索 / 确定键",
                "关闭时搜索、确定、换行等动作键统一使用功能键容器色；开启后搜索/确定使用 Monet primary。",
                "highlight_action_key",
                false);

        addSection("应用设置");
        addBody("颜色资源会在键盘重新创建时刷新。修改设置后，收起再重新唤起键盘；若某项仍未刷新，强行停止一次豆包输入法即可。动态色本身会跟随系统壁纸变化。");

        Button reset = new Button(this);
        reset.setAllCaps(false);
        reset.setText("恢复默认设置");
        reset.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            recreate();
        });
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52));
        rp.topMargin = dp(16);
        root.addView(reset, rp);

        TextView footer = new TextView(this);
        footer.setText("测试环境：豆包输入法 1.4.5 · Android 16 · Vector / Xposed compatible");
        footer.setTextSize(12);
        footer.setAlpha(0.58f);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        fp.topMargin = dp(22);
        root.addView(footer, fp);
    }

    private void addTitle(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(30);
        v.setTypeface(v.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(v);
    }

    private void addSection(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(15);
        v.setTypeface(v.getTypeface(), android.graphics.Typeface.BOLD);
        v.setAlpha(0.72f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(26);
        lp.bottomMargin = dp(8);
        root.addView(v, lp);
    }

    private void addBody(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(14);
        v.setLineSpacing(0f, 1.18f);
        v.setAlpha(0.70f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        root.addView(v, lp);
    }

    private void addPalettePreview(boolean night) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        int surface = systemColor(
                night ? "system_surface_container_dark" : "system_surface_container_light",
                night ? 0xff1f2024 : 0xffe7e8ee);
        row.setBackgroundColor(surface);

        View primary = new View(this);
        int pc = systemColor(
                night ? "system_primary_dark" : "system_primary_light",
                night ? 0xffaac7ff : 0xff415f91);
        primary.setBackgroundColor(pc);
        row.addView(primary, new LinearLayout.LayoutParams(dp(42), dp(42)));

        TextView label = new TextView(this);
        label.setText("当前系统动态色\n键盘会直接读取 Android framework Monet palette");
        label.setTextSize(13);
        label.setLineSpacing(0f, 1.15f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = dp(14);
        row.addView(label, lp);

        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = dp(18);
        root.addView(row, rp);
    }

    private void addSwitch(String title, String summary, String key, boolean defaultValue) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(12), dp(10), dp(12));

        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);

        TextView text = new TextView(this);
        text.setText(title);
        text.setTextSize(16);
        line.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(key, defaultValue));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(key, isChecked).apply());
        line.addView(toggle);
        box.addView(line);

        TextView sub = new TextView(this);
        sub.setText(summary);
        sub.setTextSize(12.5f);
        sub.setAlpha(0.62f);
        sub.setLineSpacing(0f, 1.12f);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(4);
        box.addView(sub, sp);

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(4);
        root.addView(box, bp);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
