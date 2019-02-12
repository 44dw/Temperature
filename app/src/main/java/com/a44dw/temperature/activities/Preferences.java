package com.a44dw.temperature.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.a44dw.temperature.R;
import com.a44dw.temperature.fragments.PreferenceFragment;

public class Preferences extends AppCompatActivity {

    public static final String NUM_LINES = "prefs_key_lines";
    public static final String CHANGE_NAME = "prefs_key_open";
    private int numLines = 0;
    public static final String EXTRA_LINES = "lines";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        getSupportFragmentManager().beginTransaction().add(R.id.prefs_layout, new PreferenceFragment()).commit();
    }

    public void setNumLines(int val) {
        numLines = val;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_LINES, numLines));
        super.onBackPressed();
    }
}
