package org.wordguard;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Stores the word list and PIN, and builds the matcher. */
final class Words {
    private static final String PREFS = "wordguard";
    private static final String KEY_WORDS = "words";
    private static final String KEY_PIN = "pin_hash";

    private Words() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String load(Context c) {
        String saved = prefs(c).getString(KEY_WORDS, null);
        return saved != null ? saved : readDefaults(c);
    }

    static void save(Context c, String text) {
        prefs(c).edit().putString(KEY_WORDS, text).apply();
    }

    private static String readDefaults(Context c) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                c.getResources().openRawResource(R.raw.default_words), StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) sb.append(line).append('\n');
        } catch (IOException ignored) {
        }
        return sb.toString();
    }

    /** "word" or "two words" match as whole words, ignoring case; "/regex/" is used as-is. */
    static Pattern compile(String text) {
        List<String> parts = new ArrayList<>();
        for (String line : text.split("\n")) {
            String p = line.trim();
            if (p.isEmpty() || p.startsWith("#")) continue;
            String source;
            if (p.length() > 2 && p.startsWith("/") && p.endsWith("/")) {
                source = p.substring(1, p.length() - 1);
            } else {
                String escaped = Pattern.quote(p).replaceAll("\\s+", "\\\\E\\\\s+\\\\Q");
                source = "(?<![\\p{L}\\p{N}])" + escaped + "(?![\\p{L}\\p{N}])";
            }
            try {
                Pattern.compile(source);
                parts.add(source);
            } catch (PatternSyntaxException ignored) {
                // Skip a broken /regex/ instead of disabling the whole list
            }
        }
        if (parts.isEmpty()) return null;
        return Pattern.compile(String.join("|", parts), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    static boolean hasPin(Context c) {
        return prefs(c).contains(KEY_PIN);
    }

    static void setPin(Context c, String pin) {
        prefs(c).edit().putString(KEY_PIN, hash(pin)).apply();
    }

    static boolean checkPin(Context c, String pin) {
        return hash(pin).equals(prefs(c).getString(KEY_PIN, ""));
    }

    private static String hash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                    .digest(("wordguard:" + s).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
