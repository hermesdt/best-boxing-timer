package com.hermescavern.bestboxingtimer;

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * Created by alvaro on 19/12/13.
 */
public class MyPreferencesActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        ListPreference roundsList = (ListPreference) findPreference(getString(R.string.rounds_pref_key));
        if(roundsList.getValue() == null)
            roundsList.setValueIndex(0);

        roundsList.setSummary(roundsList.getValue().toString());
        roundsList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue.toString());
                return true;
            }
        });

        final ListPreference roundTimesList = (ListPreference) findPreference(getString(R.string.round_time_pref_key));
        if(roundTimesList.getValue() == null)
            roundTimesList.setValueIndex(3);

        roundTimesList.setSummary(roundTimesList.getEntry().toString());
        roundTimesList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(roundTimesList.getEntries()[roundTimesList.findIndexOfValue(newValue.toString())]);
                Intent intent = new Intent(BroadcastMessages.ROUND_TIME_UPDATED);
                sendBroadcast(intent);
                return true;
            }
        });

        final ListPreference restTimesList = (ListPreference) findPreference(getString(R.string.rest_time_pref_key));
        if(restTimesList.getValue() == null)
            restTimesList.setValueIndex(4);

        restTimesList.setSummary(restTimesList.getEntry().toString());
        restTimesList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(restTimesList.getEntries()[restTimesList.findIndexOfValue(newValue.toString())]);
                return true;
            }
        });

        final ListPreference warningTimesList = (ListPreference) findPreference(getString(R.string.warning_time_pref_key));
        if(warningTimesList.getValue() == null)
            warningTimesList.setValueIndex(0);

        warningTimesList.setSummary(warningTimesList.getEntry().toString());
        warningTimesList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(warningTimesList.getEntries()[warningTimesList.findIndexOfValue(newValue.toString())]);
                return true;
            }
        });
    }
}
