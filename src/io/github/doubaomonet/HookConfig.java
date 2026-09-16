package io.github.doubaomonet;

import de.robv.android.xposed.XSharedPreferences;

final class HookConfig {
    static final String PACKAGE = "io.github.doubaomonet";
    static final String FILE = "config";

    static final class Snapshot {
        final boolean enabled;
        final boolean syncSystemNav;
        final boolean tintedLetterKeys;
        final boolean highlightFirstCandidate;
        final boolean highlightActionKey;

        Snapshot(
                boolean enabled,
                boolean syncSystemNav,
                boolean tintedLetterKeys,
                boolean highlightFirstCandidate,
                boolean highlightActionKey) {
            this.enabled = enabled;
            this.syncSystemNav = syncSystemNav;
            this.tintedLetterKeys = tintedLetterKeys;
            this.highlightFirstCandidate = highlightFirstCandidate;
            this.highlightActionKey = highlightActionKey;
        }

        String key() {
            int bits = 0;
            if (enabled) bits |= 1;
            if (syncSystemNav) bits |= 1 << 1;
            if (tintedLetterKeys) bits |= 1 << 2;
            if (highlightFirstCandidate) bits |= 1 << 3;
            if (highlightActionKey) bits |= 1 << 4;
            return Integer.toHexString(bits);
        }
    }

    private static XSharedPreferences prefs;

    static synchronized Snapshot load() {
        try {
            if (prefs == null) {
                prefs = new XSharedPreferences(PACKAGE, FILE);
            } else {
                prefs.reload();
            }
            return new Snapshot(
                    prefs.getBoolean("enabled", true),
                    prefs.getBoolean("sync_system_nav", true),
                    prefs.getBoolean("tinted_letter_keys", false),
                    prefs.getBoolean("highlight_first_candidate", true),
                    prefs.getBoolean("highlight_action_key", false));
        } catch (Throwable ignored) {
            return defaults();
        }
    }

    static Snapshot defaults() {
        return new Snapshot(true, true, false, true, false);
    }
}
