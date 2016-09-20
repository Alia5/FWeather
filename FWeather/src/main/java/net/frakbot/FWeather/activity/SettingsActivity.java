/*
 * Copyright 2014 Sebastiano Poggi and Francesco Pontillo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.frakbot.FWeather.activity;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

import net.frakbot.FWeather.R;
import net.frakbot.FWeather.fragments.AdvancedPreferencesFragment;
import net.frakbot.FWeather.fragments.BackupPreferenceFragment;
import net.frakbot.FWeather.fragments.CustomizationPreferencesFragment;
import net.frakbot.FWeather.fragments.DataSyncPreferencesFragment;
import net.frakbot.FWeather.fragments.InformationPreferencesFragment;
import net.frakbot.FWeather.util.WeatherLocationPreference;
import net.frakbot.FWeather.util.WidgetHelper;
import net.frakbot.global.Const;
import net.frakbot.util.feedback.FeedbackHelper;
import net.frakbot.util.log.FLog;

/**
 * A {@link android.preference.PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;
    private static final String TAG = SettingsActivity.class.getSimpleName();

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = null;
    private int mNewWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (intent != null) {
            mNewWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

            if (AppWidgetManager.ACTION_APPWIDGET_CONFIGURE.equals(intent.getAction())) {
                // See http://code.google.com/p/android/issues/detail?id=2539
                setResult(RESULT_CANCELED, new Intent()
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mNewWidgetId));
            } else if (Intent.ACTION_CREATE_SHORTCUT.equals(intent.getAction())) {
                // The following code has been taken from Roman Nurik's DashClock widget
                // (see https://code.google.com/p/dashclock/source/browse/main/src/main/java/com/google/android/apps/dashclock/configuration/ConfigurationActivity.java)
                Intent.ShortcutIconResource icon = new Intent.ShortcutIconResource();
                icon.packageName = getPackageName();
                icon.resourceName = getResources().getResourceName(R.drawable.ic_launcher);
                setResult(RESULT_OK, new Intent()
                        .putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.activity_settings))
                        .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon)
                        .putExtra(Intent.EXTRA_SHORTCUT_INTENT,
                                Intent.makeMainActivity(
                                        new ComponentName(this, SettingsActivity.class))));
                finish();
            }
        }

        mHandler = new Handler();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Sets and shows the title in the ActionBar
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            setupActionBar(actionBar);
        }

        setupSimplePreferencesScreen();
    }

    private void setupActionBar(ActionBar actionBar) {
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);

        Button btnDone = (Button) getLayoutInflater().inflate(R.layout.include_ab_done, null);

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mNewWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mNewWidgetId));
                }

                finish();
            }
        });

        actionBar.setCustomView(btnDone);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the SharedPreferences listener (me, duh)
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Update the widgets after the configuration is closed
        FLog.d(this, "SettingsActivity", "Closing the settings Activity; updating widgets");
        requestWidgetsUpdate(true, true);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();

        // Register a SharedPreferences listener (me, duh)
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected boolean isValidFragment(String fragmentName) {
        // We only accept our own fragments to use, or those provided by the super
        return AdvancedPreferencesFragment.class.getName().equals(fragmentName) ||
                BackupPreferenceFragment.class.getName().equals(fragmentName) ||
                CustomizationPreferencesFragment.class.getName().equals(fragmentName) ||
                DataSyncPreferencesFragment.class.getName().equals(fragmentName) ||
                InformationPreferencesFragment.class.getName().equals(fragmentName) ||
                super.isValidFragment(fragmentName);
    }

    /**
     * This gets called whenever a SharedPreference changes;
     * then, it notifies the BackupManager that something has changed.
     * {@inheritDoc}
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        new BackupManager(this).dataChanged();
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    @SuppressWarnings("deprecation")
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
        setPreferenceScreen(screen);

        // Add 'customization' preferences, and a corresponding header.
        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_customization);
        screen.addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_customization);

        // Add 'data and sync' preferences, and a corresponding header.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_data_sync);
        screen.addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_data_sync);
        setupRefreshNowOnClickListener(findPreference(getString(R.string.pref_key_sync_force)));

        // Add 'advanced' preferences, and a corresponding header.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_advanced);
        screen.addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_advanced);
        setupAnalyticsOnChangeListener((SwitchPreference) findPreference(getString(R.string.pref_key_analytics)));

        // Add 'info' preferences, and a corresponding header.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_info);
        screen.addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_info);
        setupFeedbackOnClickListener(findPreference(getString(R.string.pref_key_feedback)));
        setupChangelogOnClickListener(findPreference(getString(R.string.pref_key_changelog)));

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_sync_frequency)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_ui_override_language)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_ui_bgopacity)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_weather_location)));
    }

    /**
     * Sets up the OnClick listener for the refresh now preference.
     *
     * @param preference The refresh now preference
     */
    private void setupRefreshNowOnClickListener(Preference preference) {
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FLog.i(SettingsActivity.this, TAG, "Forcing weather update (user request)");
                requestWidgetsUpdate(true);

                return true;
            }
        });
    }

    /**
     * Requests an update of all the widgets we currently have.
     *
     * @param forced True if this is a forced update request, false otherwise
     */
    private void requestWidgetsUpdate(boolean forced) {
        requestWidgetsUpdate(forced, false);
    }

    /**
     * Requests an update of all the widgets we currently have. It can optionally
     * also be silent (no UI).
     *
     * @param forced True if this is a forced update request, false otherwise
     * @param silent True if this is a silent forced update request, false otherwise
     */
    private void requestWidgetsUpdate(boolean forced, boolean silent) {
        Intent i = WidgetHelper.getUpdaterIntent(this, forced, silent);

        startService(i);
    }

    /**
     * Sets up the OnClick listener for the feedback preference.
     * Inspired by http://blog.tomtasche.at/2012/10/use-built-in-feedback-mechanism-on.html
     *
     * @param preference The feedback preference
     */
    public void setupFeedbackOnClickListener(Preference preference) {
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                FLog.i(SettingsActivity.this, TAG, "Sending feedback");

                FeedbackHelper.sendFeedback(SettingsActivity.this);
                preference.setEnabled(false);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        preference.setEnabled(true);
                    }
                }, 5000);
                return true;
            }
        });
    }

    /**
     * Handles the preference change by requesting the TrackerHelper to send an event.
     *
     * @param preference The changed preference
     * @param newValue   The new value
     */
    private void handlePreferenceChange(Preference preference, Object newValue) {
        Long value = (long) 0;
        if (preference.getKey().equals(Const.Preferences.ANALYTICS)) {
            if (newValue == Boolean.FALSE) {
                value = (long) 0;
            } else {
                value = (long) 1;
            }
        } else if (preference.getKey().equals(Const.Preferences.SYNC_FREQUENCY)) {
            value = Long.valueOf((String) newValue);
        }
    }

    /**
     * Builds the listener for the preference changes.
     */
    private void buildListener() {
        if (sBindPreferenceSummaryToValueListener != null) {
            FLog.v(TAG, "buildListener() won't do anything because the listener already exists");
            return;
        }

        sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();

                if (preference.getKey().equals(Const.Preferences.SYNC_FREQUENCY)) {
                    SharedPreferences prefs = preference.getSharedPreferences();
                    // If the old value differs from the new value
                    if (!prefs.getString(Const.Preferences.SYNC_FREQUENCY, "-1").equals(stringValue)) {
                        sendSyncPreferenceChangedBroadcast();
                        // Handle the generic preference change
                        handlePreferenceChange(preference, value);
                    }
                }

                if (preference instanceof ListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);

                    // Set the summary to reflect the new value.
                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                } else if (preference instanceof WeatherLocationPreference) {
                    preference.setSummary(
                            WeatherLocationPreference.getDisplayValue(preference.getContext(), stringValue));
                } else {
                    // For all other preferences, set the summary to the value's
                    // simple string representation.
                    preference.setSummary(stringValue);
                }

                return true;
            }
        };
    }

    /**
     * Sets up the Changelog preference click listener.
     *
     * @param preference The Changelog preference.
     */
    public void setupChangelogOnClickListener(Preference preference) {
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder b = new AlertDialog.Builder(SettingsActivity.this/*,
                                                                R.style.Theme_FWeather_Settings_Dialog*/);
                b.setTitle(R.string.pref_title_changelog)
                        .setView(getLayoutInflater().inflate(R.layout.dialog_changelog, null))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                b.show();

                return true;
            }
        });
    }

    /**
     * Sets up the Analytics preference listener.
     *
     * @param preference The Analytics preference.
     */
    public void setupAnalyticsOnChangeListener(SwitchPreference preference) {
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                // Handle the generic preference change
                handlePreferenceChange(preference, newValue);

                if (newValue == Boolean.FALSE) {

                    AlertDialog.Builder b = new AlertDialog.Builder(SettingsActivity.this);
                    b.setMessage(R.string.analytics_disable_warning)
                            .setPositiveButton(android.R.string.ok, null);

                    AlertDialog dialog = b.create();
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            Toast.makeText(SettingsActivity.this, R.string.analytics_disabled,
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                    dialog.show();

                    FLog.i(SettingsActivity.this, TAG, "Disabled Google Analytics");

                    return true;
                } else {
                    Toast.makeText(SettingsActivity.this, R.string.analytics_enabled_thanks, Toast.LENGTH_SHORT)
                            .show();

                    FLog.i(SettingsActivity.this, TAG, "Enabled Google Analytics");
                    return true;
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link android.preference.PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    @SuppressWarnings({"ConstantConditions", "PointlessBooleanExpression"})
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<PreferenceActivity.Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    public void bindPreferenceSummaryToValue(Preference preference) {
        if (sBindPreferenceSummaryToValueListener == null) {
            FLog.i(TAG, "Preference summary listener isn't initialized, creating it now");
            buildListener();
        }

        // Set the listener to watch for value changes.
        preference
                .setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(
                preference,
                PreferenceManager.getDefaultSharedPreferences(
                        preference.getContext()).getString(preference.getKey(), ""));
    }

    /**
     * Broadcasts an action that causes the update of the updater alarm.
     * If the preference changes, the update rate of the application data
     * will change too, and a new update will be requested.
     */
    private void sendSyncPreferenceChangedBroadcast() {
        Intent i = new Intent(Const.Intents.SYNC_RATE_PREFERENCE_CHANGED_ACTION);
        sendBroadcast(i);
    }

}
