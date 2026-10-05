package org.wordguard;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class GuardService extends AccessibilityService {

    private static final Set<String> BROWSERS = Set.of(
            "com.android.chrome", "com.chrome.beta", "com.brave.browser", "com.brave.browser_beta",
            "org.mozilla.firefox", "org.mozilla.fenix", "org.mozilla.focus", "org.mozilla.klar",
            "com.microsoft.emmx", "com.opera.browser", "com.opera.mini.native", "com.opera.gx",
            "com.duckduckgo.mobile.android", "com.sec.android.app.sbrowser", "com.kiwibrowser.browser",
            "com.vivaldi.browser", "org.chromium.chrome", "com.google.android.googlequicksearchbox",
            "com.ecosia.android", "org.torproject.torbrowser", "mark.via.gp", "com.nothing.browser");

    private static final Set<String> YOUTUBE = Set.of(
            "com.google.android.youtube", "app.morphe.android.youtube", "app.revanced.android.youtube",
            "com.vanced.android.youtube", "org.schabi.newpipe", "com.github.libretube",
            // Android TV / Google TV
            "com.google.android.youtube.tv", "com.google.android.youtube.tvkids",
            "com.teamsmart.videomanager.tv", "com.liskovsoft.smarttubetv.beta");

    private static final Set<String> SETTINGS = Set.of(
            "com.android.settings", "com.google.android.packageinstaller", "com.android.packageinstaller",
            "com.google.android.permissioncontroller", "com.android.permissioncontroller",
            "com.android.tv.settings", "com.google.android.tv.settings");

    private static final String TAG = "WordGuard";

    private static final String APP_NAME = "Word Guard";
    private static final String[] PROTECTED_ACTIONS = {
            "uninstall", "force stop", "clear storage", "clear data", "use word guard",
            "turn off", "stop word guard", "disable"};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Pattern pattern;
    private long cooldownUntil;
    private String pendingPackage;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
            (p, key) -> reload();

    private final Runnable check = () -> {
        String pkg = pendingPackage;
        pendingPackage = null;
        if (pkg != null) inspect(pkg);
    };

    @Override
    protected void onServiceConnected() {
        reload();
        Words.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener);
    }

    private void reload() {
        pattern = Words.compile(Words.load(this));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        if (debug() && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "window: " + pkg + " / " + event.getClassName());
        }
        if (!BROWSERS.contains(pkg) && !YOUTUBE.contains(pkg) && !isSettings(pkg)) return;
        // Coalesce bursts of events into one check
        pendingPackage = pkg;
        handler.removeCallbacks(check);
        handler.postDelayed(check, 120);
    }

    private void inspect(String pkg) {
        if (SystemClock.uptimeMillis() < cooldownUntil) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null || !pkg.equals(root.getPackageName().toString())) return;

        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        collect(root, nodes, 0);
        if (debug()) dump(pkg, nodes);

        if (isSettings(pkg)) {
            if (isProtectedScreen(nodes)) block("Word Guard is protected");
        } else if (BROWSERS.contains(pkg)) {
            for (AccessibilityNodeInfo n : nodes) {
                if (isAddressOrSearch(n) && matches(n.getText())) {
                    block("Blocked");
                    return;
                }
            }
        } else if (YOUTUBE.contains(pkg)) {
            for (AccessibilityNodeInfo n : nodes) {
                if (matches(n.getText()) || matches(n.getContentDescription())) {
                    block("Blocked");
                    return;
                }
            }
        }
    }

    // TV makers ship their own settings apps under various names
    private static boolean isSettings(String pkg) {
        return SETTINGS.contains(pkg) || pkg.endsWith(".settings") || pkg.contains("tvsettings");
    }

    // Enable with: adb shell setprop log.tag.WordGuard DEBUG
    private static boolean debug() {
        return Log.isLoggable(TAG, Log.DEBUG);
    }

    private static void dump(String pkg, List<AccessibilityNodeInfo> nodes) {
        StringBuilder sb = new StringBuilder("dump " + pkg + " (" + nodes.size() + " nodes)");
        for (AccessibilityNodeInfo n : nodes) {
            String t = textOf(n).trim();
            if (t.isEmpty()) continue;
            sb.append("\n  [").append(n.getClassName()).append(" ").append(n.getViewIdResourceName())
                    .append("] ").append(t);
        }
        for (String line : sb.toString().split("\n")) Log.d(TAG, line);
    }

    private static void collect(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> out, int depth) {
        if (node == null || depth > 60 || out.size() > 3000) return;
        out.add(node);
        for (int i = 0; i < node.getChildCount(); i++) collect(node.getChild(i), out, depth + 1);
    }

    private static boolean isAddressOrSearch(AccessibilityNodeInfo n) {
        if (n.getText() == null) return false;
        String id = n.getViewIdResourceName();
        if (id != null) {
            id = id.toLowerCase();
            if (id.contains("url") || id.contains("omnibox") || id.contains("location_bar")
                    || id.contains("search") || id.contains("omnibar") || id.contains("address")) {
                return true;
            }
        }
        CharSequence cls = n.getClassName();
        return cls != null && cls.toString().endsWith("EditText");
    }

    private boolean isProtectedScreen(List<AccessibilityNodeInfo> nodes) {
        boolean mentionsApp = false;
        boolean hasAction = false;
        boolean privateDns = false;
        for (AccessibilityNodeInfo n : nodes) {
            String t = textOf(n);
            if (t.isEmpty()) continue;
            String lower = t.toLowerCase();
            if (lower.contains(APP_NAME.toLowerCase())) mentionsApp = true;
            for (String a : PROTECTED_ACTIONS) if (lower.contains(a)) hasAction = true;
            if (lower.contains("private dns provider hostname")) privateDns = true;
        }
        return privateDns || (mentionsApp && hasAction);
    }

    private static String textOf(AccessibilityNodeInfo n) {
        CharSequence t = n.getText();
        CharSequence d = n.getContentDescription();
        return (t == null ? "" : t) + " " + (d == null ? "" : d);
    }

    private boolean matches(CharSequence text) {
        return pattern != null && text != null && text.length() > 0 && pattern.matcher(text).find();
    }

    private void block(String message) {
        cooldownUntil = SystemClock.uptimeMillis() + 1200;
        performGlobalAction(GLOBAL_ACTION_BACK);
        handler.postDelayed(() -> performGlobalAction(GLOBAL_ACTION_BACK), 250);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        Words.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener);
        super.onDestroy();
    }
}
