package io.github.doubaomonet;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.os.Build;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

import java.lang.reflect.Method;
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
    private static final Map<AssetManager, String> APPLIED =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile Context APP_CONTEXT;
    private static volatile SparseIntArray COLOR_ROLES;
    private static volatile SparseIntArray DRAWABLE_ROLES;
    private static final Object RESOURCE_ROLE_LOCK = new Object();

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

    private static final int D_CANDIDATE = 1;
    private static final int D_PRESSED = 2;
    private static final int D_MORE_SEGMENT = 3;
    private static final int D_MORE_PANEL = 4;

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

    private static void ensureResourceRoles(Resources resources) {
        if (COLOR_ROLES != null && DRAWABLE_ROLES != null) return;
        synchronized (RESOURCE_ROLE_LOCK) {
            if (COLOR_ROLES != null && DRAWABLE_ROLES != null) return;
            SparseIntArray colors = new SparseIntArray();
            SparseIntArray drawables = new SparseIntArray();

            addRole(resources, colors, "color", "navigation_bar_normal", C_NAV);
            String[] extSurface = {
                    "system_keyboard_bk", "keyboard_top_start_color", "keyboard_top_end_color",
                    "BGPanelGray", "BGPanelTone", "BGPanelToneiOS", "BGPanelReverse",
                    "ConstBGPanelReverse", "navigation_bar_ai_writing", "aiwriting_bk_navigation",
                    "asr_long_press_navigation_normal", "more_candidate_panel_bg",
                    "more_candidate_content_bg"
            };
            for (String name : extSurface) addRole(resources, colors, "color", name, C_EXT_SURFACE);

            String[] extCandidate = {
                    "BGPanelTint", "BGPanelTintiOS", "ime_keyboard_candidate_font_bg",
                    "ime_keyboard_candidate_text_bg", "candidate_item_text_bg",
                    "candidate_tip_background", "more_candidate_segment_selected_bg",
                    "more_candidate_syllable_selected_bg", "more_candidate_item_pressed",
                    "ime_keyboard_iv_bg_color", "ime_toolbar_clipboard_bg_color",
                    "ime_toolbar_tips_bg_color", "clipboard_history_item_bg_color",
                    "common_phrase_item_bg", "cross_device_clipboard_card_bg",
                    "cross_device_clipboard_close_bg"
            };
            for (String name : extCandidate) addRole(resources, colors, "color", name, C_EXT_CANDIDATE);
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

            COLOR_ROLES = colors;
            DRAWABLE_ROLES = drawables;
        }
    }

    private static Drawable overrideDrawable(Resources resources, int id, Context context) {
        ensureResourceRoles(resources);
        int role = DRAWABLE_ROLES.get(id, 0);
        if (role == 0) return null;
        HookConfig.Values config = HookConfig.current();
        if (!config.enabled) return null;
        switch (role) {
            case D_CANDIDATE:
                return roundedDrawable(context, MonetSkin.candidateBackground(context, config), 6f);
            case D_PRESSED:
                return roundedDrawable(context, MonetSkin.functionKeyPressedSurface(context, config), 6f);
            case D_MORE_SEGMENT:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.candidateBackground(context, config), 24f)
                        : null;
            case D_MORE_PANEL:
                return config.syncExtendedPanels
                        ? roundedDrawable(context, MonetSkin.keyboardSurface(context, config), 0f)
                        : null;
            default:
                return null;
        }
    }

    private static boolean isKnownDoubaoGray(int color) {
        return color == 0xffe0e2e6
                || color == 0xffebebeb
                || color == 0xffeeeeee
                || color == 0xffededed
                || color == 0xfff3f3f4
                || color == 0xfff8f8f8;
    }

    private static void recolorKnownGrayTree(View root, HookConfig.Values config) {
        if (root == null || !config.enabled || !config.syncExtendedPanels) return;
        try {
            Drawable background = root.getBackground();
            if (background instanceof ColorDrawable) {
                int old = ((ColorDrawable) background).getColor();
                if (isKnownDoubaoGray(old)) {
                    root.setBackgroundColor(MonetSkin.keyboardSurface(root.getContext(), config));
                }
            }
            if (root instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) root;
                for (int i = 0; i < group.getChildCount(); i++) {
                    recolorKnownGrayTree(group.getChildAt(i), config);
                }
            }
        } catch (Throwable t) {
            error("gray tree recolor failed", t);
        }
    }

    private static void installExtendedPanelViewHooks(ClassLoader cl) {
        try {
            Class<?> toolbox = Class.forName(
                    "com.bytedance.common_biz.tool_box.ToolboxKeyboardView", false, cl);
            Set<?> toolboxHooks = XposedBridge.hookAllMethods(
                    toolbox,
                    "onAttachedToWindow",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof View)) return;
                            final View view = (View) param.thisObject;
                            view.post(() -> recolorKnownGrayTree(view, HookConfig.current()));
                        }
                    });

            Class<?> inputRoot = Class.forName(
                    "com.bytedance.android.input.keyboard.areacontrol.InputViewRoot", false, cl);
            XC_MethodHook rootCallback = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!(param.thisObject instanceof View)) return;
                    final View view = (View) param.thisObject;
                    view.post(() -> recolorKnownGrayTree(view, HookConfig.current()));
                }
            };
            Set<?> rootV = XposedBridge.hookAllMethods(inputRoot, "V", rootCallback);
            Set<?> rootX2 = XposedBridge.hookAllMethods(inputRoot, "x2", rootCallback);
            Set<?> rootAttached = XposedBridge.hookAllMethods(inputRoot, "onAttachedToWindow", rootCallback);
            info("extended view hooks installed="
                    + toolboxHooks.size() + "/" + rootV.size() + "/"
                    + rootX2.size() + "/" + rootAttached.size());
        } catch (Throwable t) {
            error("extended view hook install failed", t);
        }
    }

    private static GradientDrawable roundedDrawable(Context context, int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp * context.getResources().getDisplayMetrics().density);
        return drawable;
    }

    private static Integer overrideColor(Resources resources, int id, Context context) {
        ensureResourceRoles(resources);
        int role = COLOR_ROLES.get(id, 0);
        if (role == 0) return null;
        HookConfig.Values config = HookConfig.current();
        if (!config.enabled) return null;
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
            default:
                return null;
        }
    }

    private static void applyImeNavigation(Object service) {
        if (!(service instanceof Context)) return;
        Context context = (Context) service;
        HookConfig.Values config = HookConfig.current();
        if (!config.enabled || !config.syncSystemNav) return;

        try {
            Object dialog = XposedHelpers.callMethod(service, "getWindow");
            if (dialog == null) return;
            Object windowObject = XposedHelpers.callMethod(dialog, "getWindow");
            if (!(windowObject instanceof Window)) return;

            Window window = (Window) windowObject;
            int navSurface = MonetSkin.navigationSurface(context);
            window.setNavigationBarColor(navSurface);
            window.setNavigationBarDividerColor(navSurface);
            if (Build.VERSION.SDK_INT >= 29) {
                window.setNavigationBarContrastEnforced(false);
            }

            View decor = window.getDecorView();
            if (decor == null) return;
            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController controller = decor.getWindowInsetsController();
                if (controller != null) {
                    int mask = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                    controller.setSystemBarsAppearance(
                            MonetSkin.useLightNavigationIcons(context) ? mask : 0,
                            mask);
                }
            } else if (Build.VERSION.SDK_INT >= 26) {
                int flags = decor.getSystemUiVisibility();
                if (MonetSkin.useLightNavigationIcons(context)) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                decor.setSystemUiVisibility(flags);
            }
        } catch (Throwable t) {
            error("IME navigation color failed", t);
        }
    }

    private static void installImeWindowHooks() {
        try {
            Class<?> ims = Class.forName("android.inputmethodservice.InputMethodService");
            XC_MethodHook callback = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject instanceof Context) {
                        Context context = (Context) param.thisObject;
                        rememberContext(context);
                        HookConfig.refresh();
                        MonetSkin.refreshPalette(context.getResources());
                    }
                    applyImeNavigation(param.thisObject);
                }
            };
            Set<?> shown = XposedBridge.hookAllMethods(ims, "onWindowShown", callback);
            Set<?> started = XposedBridge.hookAllMethods(ims, "onStartInputView", callback);
            Set<?> created = XposedBridge.hookAllMethods(ims, "onCreate", callback);
            info("IME navigation hooks installed="
                    + shown.size() + "/" + started.size() + "/" + created.size());
        } catch (Throwable t) {
            error("IME navigation hook install failed", t);
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
                                    || param.args == null
                                    || param.args.length < 1
                                    || !(param.args[0] instanceof Integer)) return;
                            Context context = currentAppContext();
                            if (context == null) return;
                            Drawable value = overrideDrawable(
                                    (Resources) param.thisObject,
                                    (Integer) param.args[0],
                                    context);
                            if (value != null) param.setResult(value);
                        }
                    });

            Set<?> resourceHooks = XposedBridge.hookAllMethods(
                    Resources.class,
                    "getColor",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof Resources)
                                    || param.args == null
                                    || param.args.length < 1
                                    || !(param.args[0] instanceof Integer)) return;
                            Context context = currentAppContext();
                            if (context == null) return;
                            Integer value = overrideColor(
                                    (Resources) param.thisObject,
                                    (Integer) param.args[0],
                                    context);
                            if (value != null) param.setResult(value);
                        }
                    });

            Class<?> imeService = Class.forName(
                    "com.bytedance.android.doubaoime.ImeService", false, cl);
            Set<?> navHooks = XposedBridge.hookAllMethods(
                    imeService,
                    "o",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            HookConfig.Values config = HookConfig.current();
                            if (!config.enabled || !config.syncSystemNav) return;
                            if (param.args == null || param.args.length != 1) return;
                            if (!(param.args[0] instanceof Integer) || !(param.thisObject instanceof Context)) return;
                            Context context = (Context) param.thisObject;
                            rememberContext(context);
                            param.args[0] = MonetSkin.navigationSurface(context);
                        }
                    });
            info("surface hooks installed="
                    + resourceDrawableHooks.size() + "/" + resourceHooks.size() + "/" + navHooks.size());
        } catch (Throwable t) {
            error("surface hook install failed", t);
        }
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;
        if (!TARGET.equals(lpparam.processName)) return;
        info("package loaded, process=" + lpparam.processName);
        HookConfig.refresh();
        Context initialContext = currentAppContext();
        if (initialContext != null) MonetSkin.refreshPalette(initialContext.getResources());
        installAppSurfaceHooks(lpparam.classLoader);
        installExtendedPanelViewHooks(lpparam.classLoader);
        installImeWindowHooks();

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
                                HookConfig.Values config = HookConfig.current();
                                if (!config.enabled) return;

                                Object raw = param.getResult();
                                AssetManager original = raw instanceof AssetManager
                                        ? (AssetManager) raw
                                        : null;

                                Context context = currentAppContext();
                                if (context != null) rememberContext(context);
                                AssetManager am = original;
                                if (am == null && context != null) am = context.getAssets();

                                if (context == null || am == null) {
                                    info("getAssetsMgr unusable: original=" + (original != null)
                                            + ", context=" + (context != null));
                                    return;
                                }

                                String desiredKey = MonetSkin.paletteKey(context, config);
                                String old = APPLIED.get(am);
                                if (!desiredKey.equals(old)) {
                                    MonetSkin.Result built = MonetSkin.build(context, am, config, desiredKey);
                                    Object cookieObj = XposedHelpers.callMethod(
                                            am,
                                            "addAssetPath",
                                            built.zip.getAbsolutePath());
                                    int cookie = cookieObj instanceof Integer
                                            ? (Integer) cookieObj
                                            : 0;
                                    if (cookie == 0) {
                                        info("addAssetPath returned 0 for " + built.zip);
                                        return;
                                    }
                                    APPLIED.put(am, built.paletteKey);
                                    info("skin asset added, cookie=" + cookie
                                            + ", key=" + built.paletteKey);
                                }

                                if (original == null) {
                                    param.setResult(am);
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


