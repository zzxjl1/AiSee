package com.idealbroker.aisee;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            findPreference("pref_app_version").setSummary(BuildConfig.VERSION_NAME);
            findPreference("pref_soc_temp").setSummary(ToolUtils.getTemperature()+"℃");
            findPreference("pref_system_release").setSummary(android.os.Build.VERSION.RELEASE);
            findPreference("pref_device_model").setSummary(android.os.Build.MODEL);
            findPreference("pref_device_brand").setSummary(android.os.Build.BRAND);

        }
    }
}