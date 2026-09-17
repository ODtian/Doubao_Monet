package io.github.doubaomonet;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import de.robv.android.xposed.XSharedPreferences;

final class HookConfig {
    static final String PACKAGE = "io.github.doubaomonet";
    static final String FILE = "config";
    static final String ACTION_APPLY = "io.github.doubaomonet.ACTION_APPLY";
    private static final String TARGET_FILE = "doubao_monet_bridge";

    static final int DEFAULT_BACKGROUND_TINT = 0;
    static final int DEFAULT_LETTER_TINT = 2;
    static final int DEFAULT_FUNCTION_TINT = 6;
    static final int DEFAULT_CANDIDATE_TINT = 5;
    static final int DEFAULT_PRESSED_TINT = 12;
    static final int DEFAULT_KEY_SHADOW = 0;

    static final class Values {
        final boolean enabled;
        final boolean syncSystemNav;
        final boolean syncExtendedPanels;
        final boolean highlightFirstCandidate;
        final boolean highlightActionKey;
        final int backgroundTint;
        final int letterTint;
        final int functionTint;
        final int candidateTint;
        final int pressedTint;
        final int keyShadow;

        Values(
                boolean enabled,
                boolean syncSystemNav,
                boolean syncExtendedPanels,
                boolean highlightFirstCandidate,
                boolean highlightActionKey,
                int backgroundTint,
                int letterTint,
                int functionTint,
                int candidateTint,
                int pressedTint,
                int keyShadow) {
            this.enabled = enabled;
            this.syncSystemNav = syncSystemNav;
            this.syncExtendedPanels = syncExtendedPanels;
            this.highlightFirstCandidate = highlightFirstCandidate;
            this.highlightActionKey = highlightActionKey;
            this.backgroundTint = clamp(backgroundTint);
            this.letterTint = clamp(letterTint);
            this.functionTint = clamp(functionTint);
            this.candidateTint = clamp(candidateTint);
            this.pressedTint = clamp(pressedTint);
            this.keyShadow = clamp(keyShadow);
        }

        String key() {
            int h = 17;
            h = 31 * h + (enabled ? 1 : 0);
            h = 31 * h + (syncSystemNav ? 1 : 0);
            h = 31 * h + (syncExtendedPanels ? 1 : 0);
            h = 31 * h + (highlightFirstCandidate ? 1 : 0);
            h = 31 * h + (highlightActionKey ? 1 : 0);
            h = 31 * h + backgroundTint;
            h = 31 * h + letterTint;
            h = 31 * h + functionTint;
            h = 31 * h + candidateTint;
            h = 31 * h + pressedTint;
            h = 31 * h + keyShadow;
            return Integer.toHexString(h);
        }
    }

    private static XSharedPreferences xPrefs;
    private static volatile Values cached;

    static Values current() {
        Values value = cached;
        return value != null ? value : defaults();
    }

    static synchronized Values refresh(Context context) {
        Values localValue = null;
        int localRevision = -1;
        if (context != null) {
            SharedPreferences local = context.getSharedPreferences(TARGET_FILE, Context.MODE_PRIVATE);
            if (local.getBoolean("has_config", false)) {
                localRevision = local.getInt("config_rev", 0);
                localValue = valuesFromPrefs(local);
            }
        }

        Values legacyValue = null;
        int legacyRevision = -1;
        try {
            if (xPrefs == null) xPrefs = new XSharedPreferences(PACKAGE, FILE);
            else xPrefs.reload();
            legacyRevision = xPrefs.getInt("config_rev", 0);
            legacyValue = valuesFromXPrefs(xPrefs);
        } catch (Throwable ignored) {
        }

        if (legacyValue != null && legacyRevision > localRevision) {
            cached = legacyValue;
        } else if (localValue != null) {
            cached = localValue;
        } else if (legacyValue != null) {
            cached = legacyValue;
        } else {
            cached = defaults();
        }
        return cached;
    }

    static synchronized Values acceptApply(Context context, Intent intent) {
        Bundle extras = intent != null ? intent.getExtras() : null;
        if (extras == null) return refresh(context);
        writeTargetCopy(context, extras);
        cached = valuesFromBundle(extras);
        return cached;
    }

    static Bundle bundleFromPrefs(SharedPreferences p) {
        Bundle out = new Bundle();
        out.putBoolean("enabled", p.getBoolean("enabled", true));
        out.putBoolean("sync_extended_panels", p.getBoolean("sync_extended_panels", true));
        out.putBoolean("highlight_first_candidate", p.getBoolean("highlight_first_candidate", true));
        out.putBoolean("highlight_action_key", p.getBoolean("highlight_action_key", false));
        out.putInt("background_tint", p.getInt("background_tint", DEFAULT_BACKGROUND_TINT));
        out.putInt("letter_tint", p.getInt("letter_tint", DEFAULT_LETTER_TINT));
        out.putInt("function_tint", p.getInt("function_tint", DEFAULT_FUNCTION_TINT));
        out.putInt("candidate_tint", p.getInt("candidate_tint", DEFAULT_CANDIDATE_TINT));
        out.putInt("pressed_tint", p.getInt("pressed_tint", DEFAULT_PRESSED_TINT));
        out.putInt("key_shadow", p.getInt("key_shadow", DEFAULT_KEY_SHADOW));
        out.putInt("config_rev", p.getInt("config_rev", 0));
        return out;
    }

    static Values defaults() {
        return new Values(
                true,
                true,
                true,
                true,
                false,
                DEFAULT_BACKGROUND_TINT,
                DEFAULT_LETTER_TINT,
                DEFAULT_FUNCTION_TINT,
                DEFAULT_CANDIDATE_TINT,
                DEFAULT_PRESSED_TINT,
                DEFAULT_KEY_SHADOW);
    }

    private static Values valuesFromXPrefs(XSharedPreferences p) {
        return new Values(
                p.getBoolean("enabled", true),
                true,
                p.getBoolean("sync_extended_panels", true),
                p.getBoolean("highlight_first_candidate", true),
                p.getBoolean("highlight_action_key", false),
                p.getInt("background_tint", DEFAULT_BACKGROUND_TINT),
                p.getInt("letter_tint", DEFAULT_LETTER_TINT),
                p.getInt("function_tint", DEFAULT_FUNCTION_TINT),
                p.getInt("candidate_tint", DEFAULT_CANDIDATE_TINT),
                p.getInt("pressed_tint", DEFAULT_PRESSED_TINT),
                p.getInt("key_shadow", DEFAULT_KEY_SHADOW));
    }

    private static void writeTargetCopy(Context context, Bundle b) {
        if (context == null || b == null) return;
        try {
            SharedPreferences.Editor e = context.getSharedPreferences(TARGET_FILE, Context.MODE_PRIVATE).edit();
            e.putBoolean("has_config", true);
            e.putBoolean("enabled", b.getBoolean("enabled", true));
            e.putBoolean("sync_extended_panels", b.getBoolean("sync_extended_panels", true));
            e.putBoolean("highlight_first_candidate", b.getBoolean("highlight_first_candidate", true));
            e.putBoolean("highlight_action_key", b.getBoolean("highlight_action_key", false));
            e.putInt("background_tint", b.getInt("background_tint", DEFAULT_BACKGROUND_TINT));
            e.putInt("letter_tint", b.getInt("letter_tint", DEFAULT_LETTER_TINT));
            e.putInt("function_tint", b.getInt("function_tint", DEFAULT_FUNCTION_TINT));
            e.putInt("candidate_tint", b.getInt("candidate_tint", DEFAULT_CANDIDATE_TINT));
            e.putInt("pressed_tint", b.getInt("pressed_tint", DEFAULT_PRESSED_TINT));
            e.putInt("key_shadow", b.getInt("key_shadow", DEFAULT_KEY_SHADOW));
            e.putInt("config_rev", b.getInt("config_rev", 0));
            e.commit();
        } catch (Throwable ignored) {
        }
    }

    private static Values valuesFromBundle(Bundle b) {
        return new Values(
                b.getBoolean("enabled", true),
                true,
                b.getBoolean("sync_extended_panels", true),
                b.getBoolean("highlight_first_candidate", true),
                b.getBoolean("highlight_action_key", false),
                b.getInt("background_tint", DEFAULT_BACKGROUND_TINT),
                b.getInt("letter_tint", DEFAULT_LETTER_TINT),
                b.getInt("function_tint", DEFAULT_FUNCTION_TINT),
                b.getInt("candidate_tint", DEFAULT_CANDIDATE_TINT),
                b.getInt("pressed_tint", DEFAULT_PRESSED_TINT),
                b.getInt("key_shadow", DEFAULT_KEY_SHADOW));
    }

    private static Values valuesFromPrefs(SharedPreferences p) {
        return new Values(
                p.getBoolean("enabled", true),
                true,
                p.getBoolean("sync_extended_panels", true),
                p.getBoolean("highlight_first_candidate", true),
                p.getBoolean("highlight_action_key", false),
                p.getInt("background_tint", DEFAULT_BACKGROUND_TINT),
                p.getInt("letter_tint", DEFAULT_LETTER_TINT),
                p.getInt("function_tint", DEFAULT_FUNCTION_TINT),
                p.getInt("candidate_tint", DEFAULT_CANDIDATE_TINT),
                p.getInt("pressed_tint", DEFAULT_PRESSED_TINT),
                p.getInt("key_shadow", DEFAULT_KEY_SHADOW));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
