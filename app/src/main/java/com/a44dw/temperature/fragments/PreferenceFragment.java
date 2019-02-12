package com.a44dw.temperature.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;

import com.a44dw.temperature.activities.Preferences;
import com.a44dw.temperature.R;

public class PreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    Preferences activity;

    public PreferenceFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (Preferences) context;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.setDefaultValues(getContext(), R.xml.preferences,false);
        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(Preferences.NUM_LINES)) {
            activity.setNumLines(Integer.valueOf(sharedPreferences.getString(key, "15")));
        }
    }
}
