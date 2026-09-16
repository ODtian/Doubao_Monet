package io.github.doubaomonet;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class MonetSkin {
    static final class Result {
        final File zip;
        final String paletteKey;

        Result(File zip, String paletteKey) {
            this.zip = zip;
            this.paletteKey = paletteKey;
        }
    }

    static Result build(Context context, AssetManager source, HookConfig.Snapshot config) throws Exception {
        Palette p = Palette.read(context.getResources());
        String key = "v4_" + p.key() + "_" + config.key();
        File dir = new File(context.getCodeCacheDir(), "doubao_monet");
        if (!dir.exists() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IllegalStateException("Cannot create " + dir);
        }

        File out = new File(dir, "skin_" + key + ".zip");
        if (!out.isFile()) {
            String light = readUtf8(source, "skin/default/values/colors.xml");
            String dark = readUtf8(source, "skin/default/values/dark_colors.xml");
            String style = readUtf8(source, "skin/default/style.xml");
            light = patch(light, lightMap(p, config));
            dark = patch(dark, darkMap(p, config));
            if (!config.highlightActionKey) {
                style = style
                        .replace("actiontextcolor=\"@color/action_button_text\"", "actiontextcolor=\"@color/keybutton_text\"")
                        .replace("actionpushedtextcolor=\"@color/action_button_pushedtext\"", "actionpushedtextcolor=\"@color/keybutton_text\"")
                        .replace("actionbkcolor=\"@color/theme_color\"", "actionbkcolor=\"@color/keybutton_func_bk\"")
                        .replace("actionpushedbkcolor=\"@color/action_button_pushedbk\"", "actionpushedbkcolor=\"@color/keybutton_func_pushed_bk\"");
            }
            writeZip(out, light, dark, style);
        }
        return new Result(out, key);
    }

    static int keyboardSurface(Context context) {
        Palette p = Palette.read(context.getResources());
        return isNight(context.getResources()) ? p.surfaceContainerDark : p.surfaceContainer;
    }

    static int navigationSurface(Context context) {
        return keyboardSurface(context);
    }

    static int primary(Context context) {
        Palette p = Palette.read(context.getResources());
        return isNight(context.getResources()) ? p.primaryDark : p.primary;
    }

    static int primaryWithAlpha(Context context, float alpha) {
        int color = primary(context);
        return Color.argb(
                Math.round(255f * alpha),
                Color.red(color),
                Color.green(color),
                Color.blue(color));
    }

    static boolean useLightNavigationIcons(Context context) {
        int c = navigationSurface(context);
        double y = 0.2126 * Color.red(c) + 0.7152 * Color.green(c) + 0.0722 * Color.blue(c);
        return y > 150.0;
    }

    private static boolean isNight(Resources r) {
        return (r.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private static Map<String, Integer> lightMap(Palette p, HookConfig.Snapshot config) {
        Map<String, Integer> m = new LinkedHashMap<>();

        put(m, p.primary,
                "theme_color",
                "highlight_color",
                "keyboard_current_english",
                "setting_button_icon_selected",
                "symbol_option_selectedtext",
                "trans_cursor");

        // Candidate text must remain high contrast. Accent is expressed through selection state,
        // not by tinting all candidate glyphs.
        put(m, p.onSurface,
                "first_cand_text_color",
                "canditem_text",
                "keybutton_text",
                "title_text");
        put(m, p.onSurfaceVariant,
                "first_cand_pinyin_text_color",
                "canditem_pinyin_text",
                "keybutton_text_2",
                "keybutton_text_3",
                "toolbar_idle_button_text",
                "symbol_option_text");

        put(m, p.surfaceContainer,
                "default_bk",
                "toolbar_idle_button_bk",
                "morecands_list_bk",
                "trans_container_bk");
        put(m, p.surfaceHigh,
                "morecands_filter_container_bk",
                "setting_button_bk",
                "morecands_button_bk");
        int letterKey = config.tintedLetterKeys ? p.surfaceLow : p.surfaceLowest;
        put(m, letterKey,
                "keybutton_bk",
                "keybutton_back");

        put(m, p.secondaryContainer,
                "keybutton_func_bk",
                "toolbar_idle_button_pushedbk",
                "setting_button_pushedbk",
                "symbol_option_pushedbk");
        put(m, p.surfaceHigh,
                "keybutton_pushed_bk",
                "canditem_pushedbk");
        put(m, p.primaryContainer,
                "keybutton_func_pushed_bk",
                "action_button_pushedbk");
        m.put("canditem_selectedbk", config.highlightFirstCandidate ? p.primaryContainer : p.surfaceHigh);

        put(m, p.outlineVariant,
                "canditem_split",
                "morecands_list_border",
                "trans_container_border");

        put(m, p.onPrimary, "action_button_text");
        put(m, p.onPrimaryContainer, "action_button_pushedtext");
        put(m, p.surfaceHigh,
                "trans_action_button_bk",
                "trans_action_button_pushedbk");

        put(m, Color.TRANSPARENT,
                "keybutton_func_border",
                "keybutton_white_shadow",
                "keybutton_gray_shadow");
        return m;
    }

    private static Map<String, Integer> darkMap(Palette p, HookConfig.Snapshot config) {
        Map<String, Integer> m = new LinkedHashMap<>();

        put(m, p.primaryDark,
                "theme_color",
                "highlight_color",
                "keyboard_current_english",
                "setting_button_icon_selected",
                "symbol_option_selectedtext",
                "trans_cursor");

        put(m, p.onSurfaceDark,
                "first_cand_text_color",
                "canditem_text",
                "keybutton_text",
                "title_text");
        put(m, p.onSurfaceVariantDark,
                "first_cand_pinyin_text_color",
                "canditem_pinyin_text",
                "keybutton_text_2",
                "keybutton_text_3",
                "toolbar_idle_button_text",
                "symbol_option_text");

        put(m, p.surfaceContainerDark,
                "default_bk",
                "toolbar_idle_button_bk",
                "morecands_list_bk",
                "trans_container_bk");
        put(m, p.surfaceHighDark,
                "morecands_filter_container_bk",
                "setting_button_bk",
                "morecands_button_bk");
        int letterKey = config.tintedLetterKeys ? p.surfaceHighDark : p.surfaceHighestDark;
        put(m, letterKey,
                "keybutton_bk",
                "keybutton_back");

        put(m, p.secondaryContainerDark,
                "keybutton_func_bk",
                "toolbar_idle_button_pushedbk",
                "setting_button_pushedbk",
                "symbol_option_pushedbk");
        put(m, p.surfaceHighDark,
                "keybutton_pushed_bk",
                "canditem_pushedbk");
        put(m, p.primaryContainerDark,
                "keybutton_func_pushed_bk",
                "action_button_pushedbk");
        m.put("canditem_selectedbk", config.highlightFirstCandidate ? p.primaryContainerDark : p.surfaceHighDark);

        put(m, p.outlineVariantDark,
                "canditem_split",
                "morecands_list_border",
                "trans_container_border");

        put(m, p.onPrimaryDark, "action_button_text");
        put(m, p.onPrimaryContainerDark, "action_button_pushedtext");
        put(m, p.surfaceHighDark,
                "trans_action_button_bk",
                "trans_action_button_pushedbk");

        put(m, Color.TRANSPARENT,
                "keybutton_func_border",
                "keybutton_white_shadow",
                "keybutton_gray_shadow");
        return m;
    }

    private static void put(Map<String, Integer> map, int value, String... names) {
        for (String name : names) map.put(name, value);
    }

    private static String patch(String xml, Map<String, Integer> map) {
        String out = xml;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            String name = java.util.regex.Pattern.quote(e.getKey());
            String rx = "(<color\\s+name=\\\"" + name + "\\\">)(.*?)(</color>)";
            out = out.replaceAll(
                    rx,
                    "$1" + java.util.regex.Matcher.quoteReplacement(rgba(e.getValue())) + "$3");
        }
        return out;
    }

    private static String rgba(int c) {
        float a = Color.alpha(c) / 255.0f;
        String alpha = a >= 0.999f
                ? "1"
                : String.format(Locale.US, "%.3f", a)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
        return String.format(
                Locale.US,
                "rgba(%d, %d, %d, %s)",
                Color.red(c),
                Color.green(c),
                Color.blue(c),
                alpha);
    }

    private static String readUtf8(AssetManager am, String path) throws Exception {
        try (InputStream in = am.open(path); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void writeZip(File out, String light, String dark, String style) throws Exception {
        File tmp = new File(out.getParentFile(), out.getName() + ".tmp");
        try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(tmp))) {
            add(z, "assets/skin/default/values/colors.xml", light);
            add(z, "assets/skin/default/values/dark_colors.xml", dark);
            add(z, "assets/skin/default/style.xml", style);
        }
        if (out.exists() && !out.delete()) {
            throw new IllegalStateException("Cannot replace " + out);
        }
        if (!tmp.renameTo(out)) {
            throw new IllegalStateException("Cannot move " + tmp + " to " + out);
        }
    }

    private static void add(ZipOutputStream z, String name, String text) throws Exception {
        z.putNextEntry(new ZipEntry(name));
        z.write(text.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }

    static final class Palette {
        final int primary;
        final int onPrimary;
        final int primaryContainer;
        final int onPrimaryContainer;
        final int secondaryContainer;
        final int onSecondaryContainer;
        final int surfaceContainer;
        final int surfaceLow;
        final int surfaceLowest;
        final int surfaceHigh;
        final int surfaceHighest;
        final int onSurface;
        final int onSurfaceVariant;
        final int outlineVariant;

        final int primaryDark;
        final int onPrimaryDark;
        final int primaryContainerDark;
        final int onPrimaryContainerDark;
        final int secondaryContainerDark;
        final int onSecondaryContainerDark;
        final int surfaceContainerDark;
        final int surfaceLowDark;
        final int surfaceLowestDark;
        final int surfaceHighDark;
        final int surfaceHighestDark;
        final int onSurfaceDark;
        final int onSurfaceVariantDark;
        final int outlineVariantDark;

        Palette(
                int primary,
                int onPrimary,
                int primaryContainer,
                int onPrimaryContainer,
                int secondaryContainer,
                int onSecondaryContainer,
                int surfaceContainer,
                int surfaceLow,
                int surfaceLowest,
                int surfaceHigh,
                int surfaceHighest,
                int onSurface,
                int onSurfaceVariant,
                int outlineVariant,
                int primaryDark,
                int onPrimaryDark,
                int primaryContainerDark,
                int onPrimaryContainerDark,
                int secondaryContainerDark,
                int onSecondaryContainerDark,
                int surfaceContainerDark,
                int surfaceLowDark,
                int surfaceLowestDark,
                int surfaceHighDark,
                int surfaceHighestDark,
                int onSurfaceDark,
                int onSurfaceVariantDark,
                int outlineVariantDark) {
            this.primary = primary;
            this.onPrimary = onPrimary;
            this.primaryContainer = primaryContainer;
            this.onPrimaryContainer = onPrimaryContainer;
            this.secondaryContainer = secondaryContainer;
            this.onSecondaryContainer = onSecondaryContainer;
            this.surfaceContainer = surfaceContainer;
            this.surfaceLow = surfaceLow;
            this.surfaceLowest = surfaceLowest;
            this.surfaceHigh = surfaceHigh;
            this.surfaceHighest = surfaceHighest;
            this.onSurface = onSurface;
            this.onSurfaceVariant = onSurfaceVariant;
            this.outlineVariant = outlineVariant;
            this.primaryDark = primaryDark;
            this.onPrimaryDark = onPrimaryDark;
            this.primaryContainerDark = primaryContainerDark;
            this.onPrimaryContainerDark = onPrimaryContainerDark;
            this.secondaryContainerDark = secondaryContainerDark;
            this.onSecondaryContainerDark = onSecondaryContainerDark;
            this.surfaceContainerDark = surfaceContainerDark;
            this.surfaceLowDark = surfaceLowDark;
            this.surfaceLowestDark = surfaceLowestDark;
            this.surfaceHighDark = surfaceHighDark;
            this.surfaceHighestDark = surfaceHighestDark;
            this.onSurfaceDark = onSurfaceDark;
            this.onSurfaceVariantDark = onSurfaceVariantDark;
            this.outlineVariantDark = outlineVariantDark;
        }

        static Palette read(Resources r) {
            return new Palette(
                    sys(r, 0xff415f91, "system_primary_light", "system_accent1_600"),
                    sys(r, 0xffffffff, "system_on_primary_light", "system_accent1_0"),
                    sys(r, 0xffd6e3ff, "system_primary_container_light", "system_accent1_100"),
                    sys(r, 0xff001b3e, "system_on_primary_container_light", "system_accent1_900"),
                    sys(r, 0xffdae2f5, "system_secondary_container_light", "system_accent2_100"),
                    sys(r, 0xff131c2b, "system_on_secondary_container_light", "system_accent2_900"),
                    sys(r, 0xffe7e8ee, "system_surface_container_light", "system_neutral1_50"),
                    sys(r, 0xfff3f3f9, "system_surface_container_low_light", "system_neutral1_20"),
                    sys(r, 0xffffffff, "system_surface_container_lowest_light", "system_neutral1_0"),
                    sys(r, 0xffe1e2e8, "system_surface_container_high_light", "system_neutral1_100"),
                    sys(r, 0xffdbdce2, "system_surface_container_highest_light", "system_neutral1_200"),
                    sys(r, 0xff1b1c20, "system_on_surface_light", "system_neutral1_900"),
                    sys(r, 0xff44474e, "system_on_surface_variant_light", "system_neutral2_700"),
                    sys(r, 0xffc4c7cf, "system_outline_variant_light", "system_neutral2_200"),
                    sys(r, 0xffaac7ff, "system_primary_dark", "system_accent1_200"),
                    sys(r, 0xff0b305f, "system_on_primary_dark", "system_accent1_800"),
                    sys(r, 0xff284777, "system_primary_container_dark", "system_accent1_700"),
                    sys(r, 0xffd6e3ff, "system_on_primary_container_dark", "system_accent1_100"),
                    sys(r, 0xff3a4863, "system_secondary_container_dark", "system_accent2_700"),
                    sys(r, 0xffdae2f5, "system_on_secondary_container_dark", "system_accent2_100"),
                    sys(r, 0xff1f2024, "system_surface_container_dark", "system_neutral1_900"),
                    sys(r, 0xff1b1c20, "system_surface_container_low_dark", "system_neutral1_900"),
                    sys(r, 0xff111318, "system_surface_container_lowest_dark", "system_neutral1_1000"),
                    sys(r, 0xff292a2f, "system_surface_container_high_dark", "system_neutral1_800"),
                    sys(r, 0xff34353a, "system_surface_container_highest_dark", "system_neutral1_700"),
                    sys(r, 0xffe3e2e8, "system_on_surface_dark", "system_neutral1_100"),
                    sys(r, 0xffc4c7cf, "system_on_surface_variant_dark", "system_neutral2_200"),
                    sys(r, 0xff44474e, "system_outline_variant_dark", "system_neutral2_700"));
        }

        String key() {
            int h = 17;
            int[] v = {
                    primary, onPrimary, primaryContainer, onPrimaryContainer,
                    secondaryContainer, onSecondaryContainer,
                    surfaceContainer, surfaceLow, surfaceLowest, surfaceHigh, surfaceHighest,
                    onSurface, onSurfaceVariant, outlineVariant,
                    primaryDark, onPrimaryDark, primaryContainerDark, onPrimaryContainerDark,
                    secondaryContainerDark, onSecondaryContainerDark,
                    surfaceContainerDark, surfaceLowDark, surfaceLowestDark,
                    surfaceHighDark, surfaceHighestDark,
                    onSurfaceDark, onSurfaceVariantDark, outlineVariantDark
            };
            for (int x : v) h = 31 * h + x;
            return Integer.toHexString(h);
        }

        private static int sys(Resources r, int fallback, String... names) {
            for (String name : names) {
                int id = r.getIdentifier(name, "color", "android");
                if (id != 0) {
                    try {
                        return r.getColor(id, null);
                    } catch (Throwable ignored) {
                    }
                }
            }
            return fallback;
        }
    }
}
