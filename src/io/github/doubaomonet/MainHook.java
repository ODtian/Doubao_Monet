package io.github.doubaomonet;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
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
        try {
            Class<?> helper = Class.forName("android.app.AndroidAppHelper");
            Method m = helper.getDeclaredMethod("currentApplication");
            Object app = m.invoke(null);
            if (app instanceof Context) return (Context) app;
        } catch (Throwable ignored) {
        }
        try {
            Class<?> thread = Class.forName("android.app.ActivityThread");
            Method m = thread.getDeclaredMethod("currentApplication");
            Object app = m.invoke(null);
            if (app instanceof Context) return (Context) app;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String readBackThemeColor(AssetManager am) {
        try (InputStream in = am.open("skin/default/values/colors.xml");
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
            String xml = new String(out.toByteArray(), StandardCharsets.UTF_8);
            String marker = "name=\"theme_color\"";
            int at = xml.indexOf(marker);
            if (at < 0) return "missing";
            int start = xml.indexOf('>', at);
            int end = start < 0 ? -1 : xml.indexOf("</color>", start + 1);
            if (start < 0 || end < 0) return "parse-failed";
            return xml.substring(start + 1, end).trim();
        } catch (Throwable t) {
            return "read-failed:" + t.getClass().getSimpleName();
        }
    }

    private static Integer overrideColor(Resources resources, int id, Context context) {
        HookConfig.Snapshot config = HookConfig.current();
        if (!config.enabled) return null;
        try {
            String entry = resources.getResourceEntryName(id);
            if ("navigation_bar_normal".equals(entry)) {
                return MonetSkin.keyboardSurface(context);
            }
            if (config.highlightFirstCandidate) {
                if ("candidate_item_text_highlighted".equals(entry)
                        || "ime_keyboard_candidate_text_color".equals(entry)) {
                    return MonetSkin.primary(context);
                }
                if ("candidate_pinyin_segment_text_highlighted".equals(entry)) {
                    return MonetSkin.primaryWithAlpha(context, 0.80f);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void applyImeNavigation(Object service) {
        if (!(service instanceof Context)) return;
        Context context = (Context) service;
        HookConfig.Snapshot config = HookConfig.current();
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
                    HookConfig.refresh();
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
            Class<?> contextCompat = Class.forName("androidx.core.content.ContextCompat", false, cl);
            Set<?> compatHooks = XposedBridge.hookAllMethods(
                    contextCompat,
                    "getColor",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.args == null || param.args.length < 2) return;
                            if (!(param.args[0] instanceof Context) || !(param.args[1] instanceof Integer)) return;
                            Context context = (Context) param.args[0];
                            Integer value = overrideColor(
                                    context.getResources(),
                                    (Integer) param.args[1],
                                    context);
                            if (value != null) param.setResult(value);
                        }
                    });

            Set<?> contextHooks = XposedBridge.hookAllMethods(
                    Context.class,
                    "getColor",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof Context)
                                    || param.args == null
                                    || param.args.length != 1
                                    || !(param.args[0] instanceof Integer)) return;
                            Context context = (Context) param.thisObject;
                            Integer value = overrideColor(
                                    context.getResources(),
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
                            HookConfig.Snapshot config = HookConfig.current();
                            if (!config.enabled || !config.syncSystemNav) return;
                            if (param.args == null || param.args.length != 1) return;
                            if (!(param.args[0] instanceof Integer) || !(param.thisObject instanceof Context)) return;
                            param.args[0] = MonetSkin.navigationSurface((Context) param.thisObject);
                        }
                    });
            info("surface hooks installed="
                    + compatHooks.size() + "/" + contextHooks.size() + "/"
                    + resourceHooks.size() + "/" + navHooks.size());
        } catch (Throwable t) {
            error("surface hook install failed", t);
        }
    }

    private static void installColorStateHooks(ClassLoader cl) {
        try {
            Class<?> keyboardJni = Class.forName(
                    "com.bytedance.android.doubaoime.KeyboardJni", false, cl);
            Set<?> schemeHooks = XposedBridge.hookAllMethods(
                    keyboardJni,
                    "updateColorSchemeConf",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Object value = param.args != null && param.args.length > 0
                                    ? param.args[0]
                                    : null;
                            info("updateColorSchemeConf=" + String.valueOf(value));
                        }
                    });
            Set<?> transparentHooks = XposedBridge.hookAllMethods(
                    keyboardJni,
                    "setTransparent",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Object value = param.args != null && param.args.length > 0
                                    ? param.args[0]
                                    : null;
                            info("setTransparent=" + String.valueOf(value));
                        }
                    });
            info("color state hooks installed="
                    + schemeHooks.size() + "/" + transparentHooks.size());
        } catch (Throwable t) {
            error("color state hook install failed", t);
        }
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;
        info("package loaded, process=" + lpparam.processName);
        HookConfig.refresh();
        installColorStateHooks(lpparam.classLoader);
        installAppSurfaceHooks(lpparam.classLoader);
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
                                HookConfig.Snapshot config = HookConfig.refresh();
                                if (!config.enabled) return;

                                Object raw = param.getResult();
                                AssetManager original = raw instanceof AssetManager
                                        ? (AssetManager) raw
                                        : null;

                                Context context = currentAppContext();
                                AssetManager am = original;
                                if (am == null && context != null) am = context.getAssets();

                                if (context == null || am == null) {
                                    info("getAssetsMgr unusable: original=" + (original != null)
                                            + ", context=" + (context != null));
                                    return;
                                }

                                MonetSkin.Result built = MonetSkin.build(context, am, config);
                                String old = APPLIED.get(am);
                                if (!built.paletteKey.equals(old)) {
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
                                    info("asset readback theme_color=" + readBackThemeColor(am));
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


