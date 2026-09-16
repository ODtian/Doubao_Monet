package io.github.doubaomonet;

import de.robv.android.xposed.XSharedPreferences;

final class HookConfig {
    static final String PACKAGE = "io.github.doubaomonet";
    static final String FILE = "config";

    static final int DEFAULT_BACKGROUND_TINT = 0;
    static final int DEFAULT_LETTER_TINT = 2;
    static final int DEFAULT_FUNCTION_TINT = 6;
    static final int DEFAULT_CANDIDATE_TINT = 5;
    static final int DEFAULT_PRESSED_TINT = 12;

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
                int pressedTint) {
            this.enabled = enabled;
            this.syncSystemNav = syncSystemNav;
            this.syncExtendedPanels = syncExtendedPanels;
            this.highlightFirstCandidate = highlightFirstCandidate;
            this.highlightActionKey = highlightActionKey;
            this.backgroundTint = clamp(backgroundTint, 0, 30);
            this.letterTint = clamp(letterTint, 0, 30);
            this.functionTint = clamp(functionTint, 0, 35);
            this.candidateTint = clamp(candidateTint, 0, 30);
            this.pressedTint = clamp(pressedTint, 0, 45);
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
            return Integer.toHexString(h);
        }
    }

    private static XSharedPreferences prefs;
    private static volatile Values cached;

    static Values current() {
        Values value = cached;
        return value != null ? value : refresh();
    }

    static synchronized Values refresh() {
        try {
            if (prefs == null) {
                prefs = new XSharedPreferences(PACKAGE, FILE);
            } else {
                prefs.reload();
            }
            cached = new Values(
                    prefs.getBoolean("enabled", true),
                    prefs.getBoolean("sync_system_nav", true),
                    prefs.getBoolean("sync_extended_panels", true),
                    prefs.getBoolean("highlight_first_candidate", true),
                    prefs.getBoolean("highlight_action_key", false),
                    prefs.getInt("background_tint", DEFAULT_BACKGROUND_TINT),
                    prefs.getInt("letter_tint", DEFAULT_LETTER_TINT),
                    prefs.getInt("function_tint", DEFAULT_FUNCTION_TINT),
                    prefs.getInt("candidate_tint", DEFAULT_CANDIDATE_TINT),
                    prefs.getInt("pressed_tint", DEFAULT_PRESSED_TINT));
        } catch (Throwable ignored) {
            cached = defaults();
        }
        return cached;
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
                DEFAULT_PRESSED_TINT);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
