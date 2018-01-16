package musicplayer.developer.it.musify;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends AppCompatPreferenceActivity {

    public static boolean hasDataChanged = false;
    private static Context context;
    public static ArrayList<String> folderContainingMusic = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
        context = getApplicationContext();
    }

    public static class MainPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);

            //Filter SwitchPreferences
            bindPreferenceSummaryToValue(findPreference(getString(R.string.filter_by_name)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.filter_by_extension)));

            //MusicFolders MultiSelectListPreference
            Set<String> defaultValue = new HashSet<>();
            final ArrayList<String> musicFolders = Utility.getMusicFoldersList(getActivity());
            folderContainingMusic = musicFolders;
            CharSequence[] entries = musicFolders.toArray(new CharSequence[musicFolders.size()]);
            CharSequence[] entryValues = new CharSequence[entries.length];
            for(int i = 0; i < entries.length; i++){
                String s = String.valueOf(i);
                entryValues[i] = s;
                defaultValue.add(s);
                Log.i("entryValues",entryValues[i].toString());
            }

            final MultiSelectListPreference musicFoldersPreference = (MultiSelectListPreference)
                    findPreference(getString(R.string.music_folders));
            musicFoldersPreference.setEntries(entries);
            musicFoldersPreference.setEntryValues(entryValues);
            musicFoldersPreference.setValues(defaultValue);  //FOR DEFAULT VALUES
            //Need not set bindPreferenceSummaryToValue for MultiSelectListPreference as all are selected by default..
            

            //SleepTimer listPreference
            final ListPreference sleepDataPref = (ListPreference) findPreference("sleep_timer");
            String val = sleepDataPref.getValue();
            int index = getIndexForValue(val);
            sleepDataPref.setValueIndex(index);

//            if(Utility.isTimerOn)
//                bindPreferenceSummaryToValue(findPreference(getString(R.string.sleep_time_remaining)));
//            else
                bindPreferenceSummaryToValue(sleepDataPref);

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }




    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String stringValue = newValue.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
                hasDataChanged = true;

                if(listPreference.getKey().equals("sleep_timer")){
                    String value = stringValue;
                    //Toast.makeText(this,value,Toast.LENGTH_LONG).show();
                    Log.i("SleepTime :",value);
                    if(!value.equals("") && !value.equals(" ")) {
                        if (!value.equals("0") && !value.equals("00")) {
                           if(!Utility.isTimerOn)
                               setSleepTime(value);
                           else
                               Utility.sleep_timer.cancel();
                        }
                        else if(value.equals("00")){
                            Utility.isTimerOn = false;
                            if(Utility.isTimerOn)
                                Utility.sleep_timer.cancel();
                            Toast.makeText(context,"Timer off",Toast.LENGTH_SHORT).show();
                        }
                    }
                }


            } else if (preference instanceof EditTextPreference) {
                if (preference.getKey().equals("filter_by_name") ||preference.getKey().equals("filter_by_extension")) {
                    // update the changed gallery name to summary filed
                    preference.setSummary(stringValue);
                    hasDataChanged = true;
                }
            } else {
                preference.setSummary(stringValue);
                hasDataChanged = true;
            }
            return true;
        }
    };

    private static void setSleepTime(String sleepTime){

        long sleepTimeInMillis = Long.valueOf(sleepTime);
        sleepTimeInMillis *= 60000;

        if(!Utility.isTimerOn) {
            Utility.setTimer(context, sleepTimeInMillis);
            Toast.makeText(context, "Sleep timer set for " + sleepTime + " minutes", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(context, "Sleep timer running", Toast.LENGTH_SHORT).show();
        }
    }

    private static int getIndexForValue(String val) {
        int index;
        switch (val){
            case "00":
                index = 0;
                return index;
            case "2":
                index = 1;
                return index;
            case "10":
                index = 2;
                return index;
            case "20":
                index = 3;
                return index;
            case "30":
                index = 4;
                return index;
            case "60":
                index = 5;
                return index;
            case "0":
                index = 6;
                return index;
        }
        return -1;
    }
}


/*

        final ListPreference musicFoldersPref = (ListPreference) findPreference(getString(R.string.music_folders));
        musicFoldersPref.setEntries(entries);
        musicFoldersPref.setEntryValues(entryValues);
        musicFoldersPref.setDefaultValue(entryValues);

        bindPreferenceSummaryToValue(musicFoldersPref);
        musicFoldersPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {return true;
            }
        });


 */
