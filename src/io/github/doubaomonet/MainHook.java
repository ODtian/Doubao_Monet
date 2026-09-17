package io.github.doubaomonet;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.Window;

import java.lang.reflect.Field;

import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {
    private static final String TARGET = "com.bytedance.android.doubaoime";
    private static final String TAG = "DoubaoMonet";
    private static final Map<AssetManager, Object> BASE_ASSET_ARRAYS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<AssetManager, String> APPLIED_SKINS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile Context APP_CONTEXT;
    private static volatile SparseIntArray COLOR_ROLES;
    private static volatile SparseIntArray DRAWABLE_ROLES;
    private static volatile ClassLoader TARGET_CL;
    private static final Object RESOURCE_ROLE_LOCK = new Object();
    private static volatile BroadcastReceiver APPLY_RECEIVER;

    private static final int C_NAV = 1;
    private static final int C_EXT_SURFACE = 2;
    private static final int C_EXT_CANDIDATE = 3;
    private static final int C_EXT_DELETE = 4;
    private static final int C_EXT_DELETE_PRESSED = 5;
    private static final int C_LETTER = 6;
    private static final int C_PRESSED = 7;
    private static final int C_FUNCTION = 8;
    private static final int C_ACTION = 9;
    private static final int C_PRIMARY_TEXT = 10;
    private static final int C_PRIMARY_ALPHA = 11;
    private static final int C_TEXT = 12;
    private static final int C_TEXT_SECONDARY = 13;
    private static final int C_DIVIDER = 14;
    private static final int C_CANDIDATE_ALPHA_80 = 15;
    private static final int C_CANDIDATE_ALPHA_12 = 16;
    private static final int C_PRIMARY = 17;

    private static final int D_CANDIDATE = 1;
    private static final int D_PRESSED = 2;
    private static final int D_MORE_SEGMENT = 3;
    private static final int D_MORE_PANEL = 4;
    private static final int D_CLIP_SELECTOR = 5;
    private static final int D_CLIP_ITEM = 6;
    private static final int D_CLIP_ITEM_TRANSPARENT = 7;
    private static final int D_CLIP_SEG_TIP = 8;
    private static final int D_CLIP_OPTION = 9;
    private static final int D_CLIP_SEG = 10;
    private static final int D_CROSS_CARD = 11;
    private static final int D_CROSS_CLOSE = 12;
    private static final int D_TOOLBOX_ITEM = 13;
    private static final int D_TOOLBAR_BUTTON = 14;
    private static final int D_TOOLBAR_IDLE = 15;
    private static final int D_TOOLBAR_PILL = 16;
    private static final int D_TAB_SELECTED = 17;
    private static final int D_CLIP_EMPTY = 18;
    private static final int D_COMMON_EMPTY = 19;
    private static final int D_TOOLBAR_TIPS = 20;
    private static final int D_CANDIDATE_TIP = 21;
    private static final int D_IDLE_TIP = 22;
    private static final int D_IDLE_ACTION = 23;
    private static final int D_SETTING_GROUP = 24;
    private static final int D_SETTING_GROUP_6 = 25;
    private static final int D_SETTING_GROUP_8 = 26;
    private static final int D_CLIP_CLEAR_BUTTON = 27;
    private static final int D_CARD_ROUNDED = 28;
    private static final int D_FLOATING_TIP = 29;
    private static final int D_KEYBOARD_IV = 30;
    private static final int D_MORE_DELETE = 31;
    private static final int D_MORE_SEG_CONTAINER = 32;
    private static final int D_ASR_DOWNLOAD = 33;
    private static final int D_ASR_DOWNLOAD_BUTTON = 34;
    private static final int D_ASR_CIRCLE = 35;
    private static final int D_ASR_EDITOR_BUTTON = 36;
    private static final int D_ASR_EDITOR_BUTTON_INVERT = 37;

    private static void info(String message) {
        Log.i(TAG, message);
        XposedBridge.log(TAG + ": " + message);
    }

    private static void error(String message, Throwable t) {
        Log.e(TAG, message, t);
        XposedBridge.log(TAG + ": " + message + ": " + t);
        XposedBridge.log(t);
    }

    private static Context currentAppContext() {
        Context cached = APP_CONTEXT;
        if (cached != null) return cached;
        synchronized (MainHook.class) {
            if (APP_CONTEXT != null) return APP_CONTEXT;
            try {
                Class<?> helper = Class.forName("android.app.AndroidAppHelper");
                Method m = helper.getDeclaredMethod("currentApplication");
                Object app = m.invoke(null);
                if (app instanceof Context) {
                    APP_CONTEXT = ((Context) app).getApplicationContext();
                    if (APP_CONTEXT == null) APP_CONTEXT = (Context) app;
                    return APP_CONTEXT;
                }
            } catch (Throwable ignored) {
            }
            try {
                Class<?> thread = Class.forName("android.app.ActivityThread");
                Method m = thread.getDeclaredMethod("currentApplication");
                Object app = m.invoke(null);
                if (app instanceof Context) {
                    APP_CONTEXT = ((Context) app).getApplicationContext();
                    if (APP_CONTEXT == null) APP_CONTEXT = (Context) app;
                    return APP_CONTEXT;
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    private static void rememberContext(Context context) {
        if (context == null || APP_CONTEXT != null) return;
        Context app = context.getApplicationContext();
        APP_CONTEXT = app != null ? app : context;
    }

    private static void addRole(Resources resources, SparseIntArray map, String type, String name, int role) {
        int id = resources.getIdentifier(name, type, TARGET);
        if (id != 0) map.put(id, role);
    }

    private static void ensureResourceRoles(Resources ignored) {
        if (COLOR_ROLES != null && DRAWABLE_ROLES != null) return;
        synchronized (RESOURCE_ROLE_LOCK) {
            if (COLOR_ROLES != null && DRAWABLE_ROLES != null) return;
            Context app = currentAppContext();
            if (app == null) return;
            Resources resources = app.getResources();
            SparseIntArray colors = new SparseIntArray();
            SparseIntArray drawables = new SparseIntArray();

            String[] navSurface = {
                    "navigation_bar_normal", "navigation_bar_ai_writing",
                    "navigation_bar_asr_activated", "aiwriting_bk_navigation",
                    "aiwriting_bk_navigation_night", "asr_long_press_navigation_normal",
                    "asr_long_press_navigation_press"
            };
            for (String name : navSurface) addRole(resources, colors, "color", name, C_NAV);
            String[] surface = {
                    "system_keyboard_bk", "keyboard_top_start_color", "keyboard_top_end_color",
                    "BGPanelGray", "BGPanelTone", "BGPanelToneiOS", "BGPanelReverse",
                    "ConstBGPanelReverse", "more_candidate_panel_bg",
                    "more_candidate_content_bg"
            };
            for (String name : surface) addRole(resources, colors, "color", name, C_EXT_SURFACE);

            String[] candidate = {
                    "BGPanelTint", "BGPanelTintiOS", "ime_keyboard_candidate_font_bg",
                    "ime_keyboard_candidate_text_bg", "candidate_item_text_bg",
                    "candidate_tip_background", "more_candidate_segment_selected_bg",
                    "more_candidate_syllable_selected_bg", "more_candidate_item_pressed",
                    "ime_keyboard_iv_bg_color", "ime_toolbar_clipboard_bg_color",
                    "ime_toolbar_tips_bg_color", "common_phrase_item_bg"
            };
            for (String name : candidate) addRole(resources, colors, "color", name, C_EXT_CANDIDATE);

            addRole(resources, colors, "color", "clipboard_history_seg_tip_bg_color", C_CANDIDATE_ALPHA_80);
            addRole(resources, colors, "color", "common_phrase_clipboard_item_bg_color_transparent", C_CANDIDATE_ALPHA_12);
            addRole(resources, colors, "color", "cross_device_clipboard_card_bg", C_CANDIDATE_ALPHA_80);
            addRole(resources, colors, "color", "cross_device_clipboard_close_bg", C_EXT_DELETE);
            addRole(resources, colors, "color", "ime_toolbar_clipboard_text_color", C_TEXT);
            addRole(resources, colors, "color", "clipboard_history_word_seg_text_color", C_TEXT_SECONDARY);
            addRole(resources, colors, "color", "cross_device_clipboard_text_primary", C_TEXT);
            addRole(resources, colors, "color", "cross_device_clipboard_text_secondary", C_TEXT_SECONDARY);
            addRole(resources, colors, "color", "ime_toolbar_clipboard_line_long", C_DIVIDER);
            addRole(resources, colors, "color", "ime_toolbar_clipboard_line_short", C_DIVIDER);
            addRole(resources, colors, "color", "cross_device_clipboard_divider", C_DIVIDER);
            String[] primaryText = {
                    "cross_device_clipboard_button_primary", "custom_toolbar_selected_text",
                    "idle_tip_action_text"
            };
            for (String name : primaryText) addRole(resources, colors, "color", name, C_PRIMARY);
            String[] normalText = {
                    "candidate_tip_text", "clipboard_history_clear_page_btn_text",
                    "cross_device_clipboard_button_normal", "idle_tip_text",
                    "ime_keyboard_title_bar_middle_text_color", "ime_toolbar_tips_text_color"
            };
            for (String name : normalText) addRole(resources, colors, "color", name, C_TEXT);
            String[] secondaryText = {
                    "custom_toolbar_text", "ime_73_color", "ime_clipboard_settings_text_color",
                    "toolbar_box_text"
            };
            for (String name : secondaryText) addRole(resources, colors, "color", name, C_TEXT_SECONDARY);


            addRole(resources, colors, "color", "clipboard_history_item_bg_color", C_EXT_CANDIDATE);
            addRole(resources, colors, "color", "cross_device_clipboard_card_bg", C_EXT_CANDIDATE);
            addRole(resources, colors, "color", "ime_toolbar_clipboard_bg_color", C_FUNCTION);
            addRole(resources, colors, "color", "clipboard_history_seg_tip_bg_color", C_FUNCTION);
            addRole(resources, colors, "color", "ime_color_setting_bg", C_EXT_SURFACE);
            addRole(resources, colors, "color", "ime_color_setting_item_bg", C_LETTER);
            addRole(resources, colors, "color", "common_phrase_empty_view_bg_color", C_LETTER);
            addRole(resources, colors, "color", "common_phrase_item_bg", C_LETTER);
            addRole(resources, colors, "color", "ime_toolbar_tips_bg_color", C_LETTER);
            addRole(resources, colors, "color", "idle_tip_bg", C_LETTER);
            addRole(resources, colors, "color", "idle_tip_action_bg", C_FUNCTION);
            addRole(resources, colors, "color", "ime_slide_bg_color", C_FUNCTION);
            addRole(resources, colors, "color", "clipboard_history_clear_page_btn_bg", C_FUNCTION);
            addRole(resources, colors, "color", "asr_download_toast_content_bg", C_LETTER);
            addRole(resources, colors, "color", "asr_download_close_btn_bg", C_FUNCTION);
            addRole(resources, colors, "color", "asr_bk_editor_button_normal", C_FUNCTION);
            addRole(resources, colors, "color", "asr_bk_editor_button_pressed", C_PRESSED);
            addRole(resources, colors, "color", "asr_bk_editor_button_pressed_invert", C_PRESSED);
            addRole(resources, colors, "color", "theme_color", C_PRIMARY);
            addRole(resources, colors, "color", "more_candidate_delete_bg", C_EXT_DELETE);
            addRole(resources, colors, "color", "more_candidate_delete_pressed", C_EXT_DELETE_PRESSED);
            addRole(resources, colors, "color", "ime_key_normal_bg_color", C_LETTER);
            String[] pressed = {
                    "ime_key_normal_press_bg_color", "ime_key_gray_clickable_press_bg_color",
                    "ime_key_gray_un_clickable_press_bg_color", "ime_key_blue_clickable_press_bg_color"
            };
            for (String name : pressed) addRole(resources, colors, "color", name, C_PRESSED);
            addRole(resources, colors, "color", "ime_key_gray_clickable_bg_color", C_FUNCTION);
            addRole(resources, colors, "color", "ime_key_gray_un_clickable_bg_color", C_FUNCTION);
            addRole(resources, colors, "color", "ime_key_blue_clickable_bg_color", C_ACTION);
            addRole(resources, colors, "color", "candidate_item_text_highlighted", C_PRIMARY_TEXT);
            addRole(resources, colors, "color", "ime_keyboard_candidate_text_color", C_PRIMARY_TEXT);
            addRole(resources, colors, "color", "candidate_pinyin_segment_text_highlighted", C_PRIMARY_ALPHA);

            addRole(resources, drawables, "drawable", "bg_candidate_item_highlighted", D_CANDIDATE);
            addRole(resources, drawables, "drawable", "bg_candidate_item_pressed", D_PRESSED);
            addRole(resources, drawables, "drawable", "bg_more_candidate_segment_selected", D_MORE_SEGMENT);
            addRole(resources, drawables, "drawable", "bg_more_candidate_panel", D_MORE_PANEL);
            addRole(resources, drawables, "drawable", "bg_toolbar_clipboard_item_selector", D_CLIP_SELECTOR);
            addRole(resources, drawables, "drawable", "bg_clipboard_history_item", D_CLIP_ITEM);
            addRole(resources, drawables, "drawable", "bg_clipboard_history_item_transparent", D_CLIP_ITEM_TRANSPARENT);
            addRole(resources, drawables, "drawable", "bg_clipboard_history_seg_tips", D_CLIP_SEG_TIP);
            addRole(resources, drawables, "drawable", "bg_clipboard_item_option", D_CLIP_OPTION);
            addRole(resources, drawables, "drawable", "bg_clipboard_item_seg", D_CLIP_SEG);
            addRole(resources, drawables, "drawable", "bg_cross_device_clipboard_card", D_CROSS_CARD);
            addRole(resources, drawables, "drawable", "bg_cross_device_clipboard_close", D_CROSS_CLOSE);
            addRole(resources, drawables, "drawable", "bg_toolbox_item", D_TOOLBOX_ITEM);
            addRole(resources, drawables, "drawable", "bg_toolbar_btn", D_TOOLBAR_BUTTON);
            addRole(resources, drawables, "drawable", "bg_toolbar_idle_button", D_TOOLBAR_IDLE);
            addRole(resources, drawables, "drawable", "bg_custom_toolbar_alignment", D_TOOLBAR_PILL);
            addRole(resources, drawables, "drawable", "tab_left_selected", D_TAB_SELECTED);
            addRole(resources, drawables, "drawable", "bg_clipboard_history_empty_icon", D_CLIP_EMPTY);
            addRole(resources, drawables, "drawable", "bg_common_phrase_empty", D_COMMON_EMPTY);
            addRole(resources, drawables, "drawable", "bg_toolbar_tips", D_TOOLBAR_TIPS);
            addRole(resources, drawables, "drawable", "bg_candidate_tip", D_CANDIDATE_TIP);
            addRole(resources, drawables, "drawable", "bg_idle_tip", D_IDLE_TIP);
            addRole(resources, drawables, "drawable", "bg_idle_tip_action", D_IDLE_ACTION);
            addRole(resources, drawables, "drawable", "bg_setting_group", D_SETTING_GROUP);
            addRole(resources, drawables, "drawable", "bg_setting_group_corner_6", D_SETTING_GROUP_6);
            addRole(resources, drawables, "drawable", "bg_setting_group_gray_corner_8", D_SETTING_GROUP_8);
            addRole(resources, drawables, "drawable", "bg_clipboard_history_clear_page_btn", D_CLIP_CLEAR_BUTTON);
            addRole(resources, drawables, "drawable", "bg_card_rounded", D_CARD_ROUNDED);
            addRole(resources, drawables, "drawable", "bg_floating_keyboard_tip", D_FLOATING_TIP);
            addRole(resources, drawables, "drawable", "bg_keyboard_iv_circle", D_KEYBOARD_IV);
            addRole(resources, drawables, "drawable", "bg_more_candidate_delete", D_MORE_DELETE);
            addRole(resources, drawables, "drawable", "bg_more_candidate_segment_container", D_MORE_SEG_CONTAINER);
            addRole(resources, drawables, "drawable", "bg_shape_asr_download", D_ASR_DOWNLOAD);
            addRole(resources, drawables, "drawable", "bg_shape_asr_download_btn", D_ASR_DOWNLOAD_BUTTON);
            addRole(resources, drawables, "drawable", "bg_shape_circle", D_ASR_CIRCLE);
            addRole(resources, drawables, "drawable", "bg_asr_editor_button", D_ASR_EDITOR_BUTTON);
            addRole(resources, drawables, "drawable", "bg_asr_editor_button_invert", D_ASR_EDITOR_BUTTON_INVERT);

            COLOR_ROLES = colors;
            DRAWABLE_ROLES = drawables;
        }
    }

    private static Drawable overrideDrawable(Resources resources, int id, Context context) {
        ensureResourceRoles(resources);
        int role = DRAWABLE_ROLES == null ? 0 : DRAWABLE_ROLES.get(id, 0);
        int colorRole = COLOR_ROLES == null ? 0 : COLOR_ROLES.get(id, 0);
        if (role == 0 && colorRole == 0) return null;
        HookConfig.Values config = HookConfig.current();
        if (!config.enabled) return null;

        switch (role) {
            case D_CANDIDATE:
                return roundedDrawable(context, MonetSkin.candidateBackground(context, config), 6f);
            case D_PRESSED:
                return roundedDrawable(context, MonetSkin.functionKeyPressedSurface(context, config), 6f);
            case D_MORE_SEGMENT:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.letterKeySurface(context, config), 24f) : null;
            case D_MORE_PANEL:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.keyboardSurface(context, config), 0f) : null;
            case D_CLIP_SELECTOR:
                return config.syncExtendedPanels ? clipboardSelector(context, config) : null;
            case D_CLIP_ITEM:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.candidateBackground(context, config), 10f) : null;
            case D_CLIP_ITEM_TRANSPARENT:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, Color.TRANSPARENT, 10f) : null;
            case D_CLIP_SEG_TIP:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.functionKeySurface(context, config), 8f) : null;
            case D_CLIP_OPTION:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.functionKeySurface(context, config), 30f) : null;
            case D_CLIP_SEG:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.functionKeySurface(context, config), 6f) : null;
            case D_CROSS_CARD:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.candidateBackground(context, config), 14f) : null;
            case D_CROSS_CLOSE:
                return config.syncExtendedPanels
                        ? ovalDrawable(MonetSkin.functionKeySurface(context, config)) : null;
            case D_TOOLBOX_ITEM:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.candidateBackground(context, config), 14f) : null;
            case D_TOOLBAR_BUTTON:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.functionKeySurface(context, config), 16f) : null;
            case D_TOOLBAR_IDLE:
                return config.syncExtendedPanels
                        ? ovalDrawable(MonetSkin.functionKeySurface(context, config)) : null;
            case D_TOOLBAR_PILL:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.functionKeySurface(context, config), 999f) : null;
            case D_TAB_SELECTED:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.candidateBackground(context, config), 999f) : null;
            case D_CLIP_EMPTY:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.candidateBackground(context, config), 24f) : null;
            case D_COMMON_EMPTY:
                return config.syncExtendedPanels ? roundedDrawable(context, MonetSkin.candidateBackground(context, config), 14f) : null;
            case D_TOOLBAR_TIPS:
                return config.syncExtendedPanels ? roundedDrawable(context, MonetSkin.letterKeySurface(context, config), 12f) : null;
            case D_CANDIDATE_TIP:
                return config.syncExtendedPanels ? roundedDrawable(context, MonetSkin.candidateBackground(context, config), 16f) : null;
            case D_IDLE_TIP:
                return config.syncExtendedPanels ? roundedDrawable(context, MonetSkin.letterKeySurface(context, config), 25f) : null;
            case D_IDLE_ACTION:
                return config.syncExtendedPanels ? roundedDrawable(context, MonetSkin.functionKeySurface(context, config), 116f) : null;
            case D_SETTING_GROUP:
                return config.syncExtendedPanels ? roundedDrawable(context, MonetSkin.letterKeySurface(context, config), 16f) : null;
            case D_SETTING_GROUP_6:
                return config.syncExtendedPanels ? roundedDrawable(context, MonetSkin.candidateBackground(context, config), 6f) : null;
            case D_SETTING_GROUP_8:
                return config.syncExtendedPanels ? roundedDrawable(context, MonetSkin.functionKeySurface(context, config), 8f) : null;
            case D_CLIP_CLEAR_BUTTON:
                return config.syncExtendedPanels ? roundedDrawable(context, MonetSkin.functionKeySurface(context, config), 32f) : null;
            case D_CARD_ROUNDED:
                return config.syncExtendedPanels ? roundedDrawable(context, MonetSkin.letterKeySurface(context, config), 16f) : null;
            case D_FLOATING_TIP:
                return config.syncExtendedPanels ? roundedDrawable(context, MonetSkin.keyboardSurface(context, config), 16f) : null;
            case D_KEYBOARD_IV:
                return config.syncExtendedPanels ? ovalDrawable(MonetSkin.candidateBackground(context, config)) : null;
            case D_MORE_DELETE:
                return config.syncExtendedPanels ? roundedSelector(context, MonetSkin.functionKeySurface(context, config), MonetSkin.functionKeyPressedSurface(context, config), 12f) : null;
            case D_MORE_SEG_CONTAINER:
                return config.syncExtendedPanels ? roundedDrawable(context, MonetSkin.functionKeySurface(context, config), 20f) : null;
            case D_ASR_DOWNLOAD:
                return config.syncExtendedPanels ? roundedDrawable(context, MonetSkin.letterKeySurface(context, config), 16f) : null;
            case D_ASR_DOWNLOAD_BUTTON:
                return config.syncExtendedPanels ? roundedSelector(context, MonetSkin.primary(context), MonetSkin.functionKeyPressedSurface(context, config), 999f) : null;
            case D_ASR_CIRCLE:
                return config.syncExtendedPanels ? ovalDrawable(MonetSkin.functionKeySurface(context, config)) : null;
            case D_ASR_EDITOR_BUTTON:
                return config.syncExtendedPanels ? roundedSelector(context, MonetSkin.functionKeySurface(context, config), MonetSkin.functionKeyPressedSurface(context, config), 16f) : null;
            case D_ASR_EDITOR_BUTTON_INVERT:
                return config.syncExtendedPanels ? roundedSelector(context, MonetSkin.primary(context), MonetSkin.functionKeyPressedSurface(context, config), 16f) : null;
            default:
                break;
        }

        // A color resource used as android:background or setBackgroundResource() is loaded
        // as a Drawable. Reuse the same color-role mapping here so XML inflation and code
        // paths see the same Monet value from the first frame.
        Integer color = colorRole != 0 ? resolveColorRole(colorRole, context, config) : null;
        return color != null ? new ColorDrawable(color) : null;
    }

    private static GradientDrawable roundedDrawable(Context context, int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp * context.getResources().getDisplayMetrics().density);
        return drawable;
    }

    private static GradientDrawable ovalDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private static int withAlpha(int color, float alpha) {
        return Color.argb(Math.round(255f * alpha), Color.red(color), Color.green(color), Color.blue(color));
    }

    private static StateListDrawable roundedSelector(Context context, int normal, int pressed, float radiusDp) {
        StateListDrawable selector = new StateListDrawable();
        selector.addState(new int[] {android.R.attr.state_pressed}, roundedDrawable(context, pressed, radiusDp));
        selector.addState(new int[] {}, roundedDrawable(context, normal, radiusDp));
        return selector;
    }

    private static StateListDrawable clipboardSelector(Context context, HookConfig.Values config) {
        StateListDrawable selector = new StateListDrawable();
        Drawable active = roundedDrawable(context, MonetSkin.candidateBackground(context, config), 6f);
        selector.addState(new int[] {android.R.attr.state_pressed}, active);
        selector.addState(new int[] {android.R.attr.state_focused},
                roundedDrawable(context, MonetSkin.candidateBackground(context, config), 6f));
        selector.addState(new int[] {}, roundedDrawable(context, Color.TRANSPARENT, 6f));
        return selector;
    }

    private static Integer overrideColor(Resources resources, int id, Context context) {
        ensureResourceRoles(resources);
        int role = COLOR_ROLES == null ? 0 : COLOR_ROLES.get(id, 0);
        if (role == 0) return null;
        HookConfig.Values config = HookConfig.current();
        if (!config.enabled) return null;
        return resolveColorRole(role, context, config);
    }

    private static Integer resolveColorRole(int role, Context context, HookConfig.Values config) {
        switch (role) {
            case C_NAV:
                return MonetSkin.keyboardSurface(context, config);
            case C_EXT_SURFACE:
                return config.syncExtendedPanels ? MonetSkin.keyboardSurface(context, config) : null;
            case C_EXT_CANDIDATE:
                return config.syncExtendedPanels ? MonetSkin.candidateBackground(context, config) : null;
            case C_EXT_DELETE:
                return config.syncExtendedPanels ? MonetSkin.functionKeySurface(context, config) : null;
            case C_EXT_DELETE_PRESSED:
                return config.syncExtendedPanels ? MonetSkin.functionKeyPressedSurface(context, config) : null;
            case C_LETTER:
                return MonetSkin.letterKeySurface(context, config);
            case C_PRESSED:
                return MonetSkin.functionKeyPressedSurface(context, config);
            case C_FUNCTION:
                return MonetSkin.functionKeySurface(context, config);
            case C_ACTION:
                return config.highlightActionKey ? MonetSkin.primary(context) : MonetSkin.functionKeySurface(context, config);
            case C_PRIMARY_TEXT:
                return config.highlightFirstCandidate ? MonetSkin.primary(context) : null;
            case C_PRIMARY_ALPHA:
                return config.highlightFirstCandidate ? MonetSkin.primaryWithAlpha(context, 0.80f) : null;
            case C_TEXT:
                return MonetSkin.onSurface(context);
            case C_TEXT_SECONDARY:
                return MonetSkin.onSurfaceVariant(context);
            case C_DIVIDER:
                return withAlpha(MonetSkin.outlineVariant(context), 0.55f);
            case C_CANDIDATE_ALPHA_80:
                return config.syncExtendedPanels
                        ? withAlpha(MonetSkin.candidateBackground(context, config), 0.80f) : null;
            case C_CANDIDATE_ALPHA_12:
                return config.syncExtendedPanels
                        ? withAlpha(MonetSkin.candidateBackground(context, config), 0.12f) : null;
            case C_PRIMARY:
                return MonetSkin.primary(context);
            default:
                return null;
        }
    }

    private static void installConfigBridge() {
        try {
            XposedBridge.hookAllMethods(
                    Application.class,
                    "attach",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.args == null || param.args.length < 1 || !(param.args[0] instanceof Context)) return;
                            Context context = (Context) param.args[0];
                            rememberContext(context);
                            HookConfig.Values value = HookConfig.refresh(context);
                            MonetSkin.refreshPalette(context.getResources());
                            registerApplyReceiver(context);
                            info("config loaded key=" + value.key());
                        }
                    });
        } catch (Throwable t) {
            error("config bridge install failed", t);
        }
    }

    private static Object declaredField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static long declaredLongField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.getLong(target);
    }

    private static void reloadDoubaoConfig(ClassLoader cl) throws Exception {
        Class<?> settingsClass = Class.forName(
                "com.bytedance.android.input.common.SettingsConfigNext", false, cl);
        Field singletonField = settingsClass.getDeclaredField("a");
        singletonField.setAccessible(true);
        Object settings = singletonField.get(null);
        Method export = settingsClass.getDeclaredMethod("a", boolean.class);
        export.setAccessible(true);
        Object json = export.invoke(settings, false);
        if (!(json instanceof String)) return;

        Class<?> jniClass = Class.forName(
                "com.bytedance.android.doubaoime.KeyboardJni", false, cl);
        Method get = jniClass.getDeclaredMethod("getKeyboardJni");
        get.setAccessible(true);
        Object jni = get.invoke(null);
        Method reload = jniClass.getDeclaredMethod("ReloadConfig", String.class);
        reload.setAccessible(true);
        reload.invoke(jni, json);
    }

    private static void reloadDoubaoColors(ClassLoader cl) {
        try {
            Class<?> globalsClass = Class.forName(
                    "com.bytedance.android.input.basic.IAppGlobals", false, cl);
            Field singleton = globalsClass.getDeclaredField("a");
            singleton.setAccessible(true);
            Object globals = singleton.get(null);
            Method getScheme = globals.getClass().getDeclaredMethod("A");
            getScheme.setAccessible(true);
            Object value = getScheme.invoke(globals);
            String current = value instanceof String ? (String) value : "";

            Class<?> jniClass = Class.forName(
                    "com.bytedance.android.doubaoime.KeyboardJni", false, cl);
            Method get = jniClass.getDeclaredMethod("getKeyboardJni");
            get.setAccessible(true);
            Object jni = get.invoke(null);
            Method update = jniClass.getDeclaredMethod("updateColorSchemeConf", String.class);
            update.setAccessible(true);

            String alternate = "dark".equals(current) ? "" : "dark";
            update.invoke(jni, alternate);
            update.invoke(jni, current);
            info("native color table reloaded, scheme=" + current);
        } catch (Throwable t) {
            error("native color reload failed", t);
        }
    }

    private static void ensureCurrentSkin(Object view) {
        if (view == null) return;
        try {
            Method assets = view.getClass().getDeclaredMethod("getAssetsMgr");
            assets.setAccessible(true);
            assets.invoke(view);
        } catch (Throwable t) {
            error("skin path refresh failed", t);
        }
    }

    private static void reinitKeyboardView(Object view) {
        if (view == null) return;
        try {
            long nativeId = declaredLongField(view, "mNativeViewId");
            Object windowName = declaredField(view, "mWindowName");
            if (nativeId != 0L && windowName instanceof String) {
                Method rebind = view.getClass().getDeclaredMethod(
                        "SetNativeId", long.class, String.class);
                rebind.setAccessible(true);
                rebind.invoke(view, nativeId, windowName);
            }

            if (view instanceof View) {
                View androidView = (View) view;
                int w = androidView.getWidth();
                int h = androidView.getHeight();
                if (w > 0 && h > 0) {
                    try {
                        Method resize = view.getClass().getDeclaredMethod(
                                "resizeView", int.class, int.class);
                        resize.setAccessible(true);
                        resize.invoke(view, w, h);
                    } catch (Throwable ignored) {
                    }
                }
                androidView.requestLayout();
                androidView.invalidate();
            }
        } catch (Throwable t) {
            error("keyboard hot refresh failed", t);
        }
    }

    private static void hotApplyToCurrentKeyboard(Context context) {
        ClassLoader cl = TARGET_CL;
        if (cl == null) return;
        try {
            Class<?> imeClass = Class.forName(
                    "com.bytedance.android.doubaoime.ImeService", false, cl);
            Method currentInput = imeClass.getDeclaredMethod("c");
            currentInput.setAccessible(true);
            Object inputView = currentInput.invoke(null);
            if (inputView == null) {
                info("keyboard apply deferred until input view exists");
                return;
            }

            Object main;
            try {
                Method q = inputView.getClass().getDeclaredMethod("q");
                q.setAccessible(true);
                main = q.invoke(inputView);
            } catch (Throwable ignored) {
                main = declaredField(inputView, "a");
            }
            Object alternate = null;
            try {
                alternate = declaredField(inputView, "b");
            } catch (Throwable ignored) {
            }

            // getAssetsMgr() passes through our hook and swaps the overlay inside the
            // same AssetManager object already held by native code.
            ensureCurrentSkin(main);
            if (alternate != main) ensureCurrentSkin(alternate);
            reloadDoubaoColors(cl);

            // Doubao's own input-view initialization refreshes the remaining keyboard state.
            Method reloadInput = inputView.getClass().getDeclaredMethod("M");
            reloadInput.setAccessible(true);
            reloadInput.invoke(inputView);

            if (main instanceof View) ((View) main).invalidate();
            if (alternate instanceof View) ((View) alternate).invalidate();
            if (inputView instanceof View) {
                View v = (View) inputView;
                v.requestLayout();
                v.invalidate();
            }
            info("keyboard hot apply completed");
        } catch (Throwable t) {
            error("hot apply failed", t);
        }
    }

    private static void registerApplyReceiver(Context context) {
        if (APPLY_RECEIVER != null) return;
        synchronized (MainHook.class) {
            if (APPLY_RECEIVER != null) return;
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    if (intent == null || !HookConfig.ACTION_APPLY.equals(intent.getAction())) return;
                    HookConfig.Values value = HookConfig.acceptApply(c, intent);
                    MonetSkin.refreshPalette(c.getResources());
                    info("config accepted key=" + value.key());
                    hotApplyToCurrentKeyboard(c);
                }
            };
            IntentFilter filter = new IntentFilter(HookConfig.ACTION_APPLY);
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(receiver, filter);
            }
            APPLY_RECEIVER = receiver;
        }
    }

    private static void installAppSurfaceHooks(ClassLoader cl) {
        try {
            Set<?> resourceDrawableHooks = XposedBridge.hookAllMethods(
                    Resources.class,
                    "getDrawable",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof Resources)
                                    || param.args == null || param.args.length < 1
                                    || !(param.args[0] instanceof Integer)) return;
                            Context context = currentAppContext();
                            if (context == null) return;
                            Drawable value = overrideDrawable(
                                    (Resources) param.thisObject, (Integer) param.args[0], context);
                            if (value != null) param.setResult(value);
                        }
                    });

            Set<?> resourceColorHooks = XposedBridge.hookAllMethods(
                    Resources.class,
                    "getColor",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof Resources)
                                    || param.args == null || param.args.length < 1
                                    || !(param.args[0] instanceof Integer)) return;
                            Context context = currentAppContext();
                            if (context == null) return;
                            Integer value = overrideColor(
                                    (Resources) param.thisObject, (Integer) param.args[0], context);
                            if (value != null) param.setResult(value);
                        }
                    });

            Set<?> resourceColorStateHooks = XposedBridge.hookAllMethods(
                    Resources.class,
                    "getColorStateList",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof Resources)
                                    || param.args == null || param.args.length < 1
                                    || !(param.args[0] instanceof Integer)) return;
                            Context context = currentAppContext();
                            if (context == null) return;
                            Integer value = overrideColor(
                                    (Resources) param.thisObject, (Integer) param.args[0], context);
                            if (value != null) param.setResult(ColorStateList.valueOf(value));
                        }
                    });

            Set<?> typedDrawableHooks = XposedBridge.hookAllMethods(
                    TypedArray.class,
                    "getDrawable",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof TypedArray)
                                    || param.args == null || param.args.length < 1
                                    || !(param.args[0] instanceof Integer)) return;
                            TypedArray array = (TypedArray) param.thisObject;
                            int id = array.getResourceId((Integer) param.args[0], 0);
                            if (id == 0) return;
                            Context context = currentAppContext();
                            if (context == null) return;
                            Drawable value = overrideDrawable(array.getResources(), id, context);
                            if (value != null) param.setResult(value);
                        }
                    });

            Set<?> typedColorHooks = XposedBridge.hookAllMethods(
                    TypedArray.class,
                    "getColor",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof TypedArray)
                                    || param.args == null || param.args.length < 1
                                    || !(param.args[0] instanceof Integer)) return;
                            TypedArray array = (TypedArray) param.thisObject;
                            int id = array.getResourceId((Integer) param.args[0], 0);
                            if (id == 0) return;
                            Context context = currentAppContext();
                            if (context == null) return;
                            Integer value = overrideColor(array.getResources(), id, context);
                            if (value != null) param.setResult(value);
                        }
                    });

            Set<?> typedColorStateHooks = XposedBridge.hookAllMethods(
                    TypedArray.class,
                    "getColorStateList",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof TypedArray)
                                    || param.args == null || param.args.length < 1
                                    || !(param.args[0] instanceof Integer)) return;
                            TypedArray array = (TypedArray) param.thisObject;
                            int id = array.getResourceId((Integer) param.args[0], 0);
                            if (id == 0) return;
                            Context context = currentAppContext();
                            if (context == null) return;
                            Integer value = overrideColor(array.getResources(), id, context);
                            if (value != null) param.setResult(ColorStateList.valueOf(value));
                        }
                    });

            info("resource hooks installed=" + resourceDrawableHooks.size() + "/"
                    + resourceColorHooks.size() + "/" + resourceColorStateHooks.size()
                    + ", typed=" + typedDrawableHooks.size() + "/"
                    + typedColorHooks.size() + "/" + typedColorStateHooks.size());
        } catch (Throwable t) {
            error("resource hook install failed", t);
        }
    }

    private static void applySkinToAssetManager(
            Context context,
            AssetManager assets,
            HookConfig.Values config) throws Exception {
        String key = MonetSkin.paletteKey(context, config);
        String old = APPLIED_SKINS.get(assets);
        if (key.equals(old)) return;

        MonetSkin.Result built = MonetSkin.build(context, assets, config, key);
        Method getApkAssets = AssetManager.class.getDeclaredMethod("getApkAssets");
        getApkAssets.setAccessible(true);

        Object base = BASE_ASSET_ARRAYS.get(assets);
        if (base == null) {
            Object current = getApkAssets.invoke(assets);
            int count = Array.getLength(current);
            Class<?> component = current.getClass().getComponentType();
            Object copy = Array.newInstance(component, count);
            for (int i = 0; i < count; i++) Array.set(copy, i, Array.get(current, i));
            BASE_ASSET_ARRAYS.put(assets, copy);
            base = copy;
        }

        java.lang.reflect.Constructor<AssetManager> ctor = AssetManager.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        AssetManager temp = ctor.newInstance();
        Object cookieObj = XposedHelpers.callMethod(temp, "addAssetPath", built.zip.getAbsolutePath());
        int cookie = cookieObj instanceof Integer ? (Integer) cookieObj : 0;
        if (cookie == 0) throw new IllegalStateException("Could not load Monet skin");
        Object tempArray = getApkAssets.invoke(temp);
        int tempCount = Array.getLength(tempArray);
        if (tempCount == 0) throw new IllegalStateException("Monet skin has no ApkAssets");
        Object overlay = Array.get(tempArray, tempCount - 1);

        int baseCount = Array.getLength(base);
        Class<?> component = base.getClass().getComponentType();
        Object merged = Array.newInstance(component, baseCount + 1);
        for (int i = 0; i < baseCount; i++) Array.set(merged, i, Array.get(base, i));
        Array.set(merged, baseCount, overlay);

        Method setApkAssets = AssetManager.class.getDeclaredMethod(
                "setApkAssets", merged.getClass(), boolean.class);
        setApkAssets.setAccessible(true);
        setApkAssets.invoke(assets, merged, true);
        APPLIED_SKINS.put(assets, key);
        info("skin assets swapped, key=" + built.paletteKey);
    }

    private static void restoreSkinAssetManager(AssetManager assets) throws Exception {
        if ("__base__".equals(APPLIED_SKINS.get(assets))) return;
        Object base = BASE_ASSET_ARRAYS.get(assets);
        if (base == null) return;
        Method setApkAssets = AssetManager.class.getDeclaredMethod(
                "setApkAssets", base.getClass(), boolean.class);
        setApkAssets.setAccessible(true);
        setApkAssets.invoke(assets, base, true);
        APPLIED_SKINS.put(assets, "__base__");
        info("skin assets restored");
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;
        if (!TARGET.equals(lpparam.processName)) return;
        TARGET_CL = lpparam.classLoader;
        info("package loaded, process=" + lpparam.processName);
        installConfigBridge();
        Context initialContext = currentAppContext();
        if (initialContext != null) {
            HookConfig.refresh(initialContext);
            MonetSkin.refreshPalette(initialContext.getResources());
            registerApplyReceiver(initialContext);
        }
        installAppSurfaceHooks(lpparam.classLoader);

        try {
            Class<?> keyboardView = Class.forName(
                    "com.bytedance.android.input.keyboard.KeyboardView",
                    false,
                    lpparam.classLoader);

            Set<?> hooks = XposedBridge.hookAllMethods(
                    keyboardView,
                    "getAssetsMgr",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                Object raw = param.getResult();
                                AssetManager original = raw instanceof AssetManager
                                        ? (AssetManager) raw
                                        : null;

                                Context context = currentAppContext();
                                if (context != null) rememberContext(context);
                                if (context == null) return;
                                if (original == null) original = context.getAssets();

                                HookConfig.Values config = HookConfig.refresh(context);
                                if (config.enabled) {
                                    applySkinToAssetManager(context, original, config);
                                } else {
                                    restoreSkinAssetManager(original);
                                }
                            } catch (Throwable t) {
                                error("hook callback failed", t);
                            }
                        }
                    });
            info("getAssetsMgr hooks installed=" + hooks.size());
        } catch (Throwable t) {
            error("hook install failed", t);
        }
    }
}


