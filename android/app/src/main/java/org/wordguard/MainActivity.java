package org.wordguard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

public class MainActivity extends Activity {

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad * 2, pad, pad);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Words.hasPin(this)) showUnlock();
        else showSetPin();
    }

    private void showSetPin() {
        root.removeAllViews();
        title("Set a PIN");
        note("Have your accountability partner choose this PIN. It's needed to change the word list.");
        EditText pin = pinField("New PIN");
        EditText again = pinField("Repeat PIN");
        button("Save PIN", v -> {
            String p = pin.getText().toString();
            if (p.length() < 4) toast("Use at least 4 digits");
            else if (!p.equals(again.getText().toString())) toast("PINs don't match");
            else {
                Words.setPin(this, p);
                showEditor();
            }
        });
        statusAndSettings();
    }

    private void showUnlock() {
        root.removeAllViews();
        title("Word Guard");
        statusAndSettings();
        EditText pin = pinField("PIN to edit words");
        button("Unlock", v -> {
            if (Words.checkPin(this, pin.getText().toString())) showEditor();
            else toast("Wrong PIN");
        });
    }

    private void showEditor() {
        root.removeAllViews();
        title("Blocked words");
        note("One per line. Whole words, any capitalization. Write /regex/ for a pattern.");
        EditText words = new EditText(this);
        words.setText(Words.load(this));
        words.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        words.setGravity(Gravity.TOP);
        words.setMinLines(12);
        root.addView(words);
        button("Save", v -> {
            Words.save(this, words.getText().toString());
            toast("Saved");
            showUnlock();
        });
        if (Words.isPaused(this)) {
            button("Resume blocking", v -> pause(0, "Blocking resumed"));
        } else {
            button("Pause blocking for 1 hour", v -> pause(System.currentTimeMillis() + 3_600_000L, "Paused for 1 hour"));
            button("Turn off blocking", v -> pause(Long.MAX_VALUE, "Blocking is off"));
        }
        button("Change PIN", v -> showSetPin());
        button("Lock", v -> showUnlock());
    }

    private void pause(long until, String message) {
        Words.pauseUntil(this, until);
        toast(message);
        showEditor();
    }

    private void statusAndSettings() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean on = enabled != null && enabled.contains(getPackageName());
        long until = Words.pausedUntil(this);
        if (!on) note("Status: OFF — turn on Word Guard in Accessibility settings.");
        else if (until == Long.MAX_VALUE) note("Status: TURNED OFF — unlock with the PIN to resume.");
        else if (Words.isPaused(this)) note("Status: PAUSED until "
                + DateFormat.getTimeFormat(this).format(new Date(until)) + ".");
        else note("Status: ON — blocking is active.");
        if (!on) {
            button("Open Accessibility settings",
                    v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        }
    }

    private void title(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(24);
        t.setPadding(0, 0, 0, dp(12));
        root.addView(t);
    }

    private void note(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(15);
        t.setPadding(0, dp(4), 0, dp(12));
        root.addView(t);
    }

    private EditText pinField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        root.addView(e);
        return e;
    }

    private void button(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setOnClickListener(l);
        root.addView(b);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
