package net.bayofrum.belldroid;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class PreferencesActivity extends PreferenceActivity {

	private SharedPreferences spref;
	/* Totally unbelieveable.  You have to read this!
	 * http://stackoverflow.com/questions/2542938/sharedpreferences-onsharedpreferencechangelistener-not-being-called-consistently
	 */
	private OnSharedPreferenceChangeListener prefchangelistener;
	
	private Method methods;
	
	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    /* Might crash without next line ?? */
//	    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	    addPreferencesFromResource(R.xml.preferences);
	    spref = PreferenceManager.getDefaultSharedPreferences(this);
		prefchangelistener = new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences s,
					String key) {
				if (key.equals("number_of_bells")) {
					String numBells = spref.getString("number_of_bells", "6");
					set_method_prefs(numBells);
					set_my_bell(numBells);
				}
			}
		};
		
		methods = new Method(getBaseContext());
		
	    register_changelistener();
	    
/*	    Preference updateDb = findPreference("update_db");
	    updateDb.setOnPreferenceClickListener(
	    		new Preference.OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				update_method_db();
				return true;
			}
		});
*/
	}
	
	@Override
	public void onResume() {
		super.onResume();
		register_changelistener();
	}
	
	private void register_changelistener() {
		spref.registerOnSharedPreferenceChangeListener(prefchangelistener);
		String numBells = spref.getString("number_of_bells", "6");
		set_method_prefs(numBells);
		set_my_bell(numBells);
	}
	
	private void set_method_prefs(int numBells) {

		@SuppressWarnings("deprecation")
		ListPreference methodpref = 
				(ListPreference)findPreference("selected_method");
	    
		ArrayList<CharSequence> names = new ArrayList<CharSequence>();
		ArrayList<CharSequence> notation = new ArrayList<CharSequence>();
	    
		for (CharSequence s[] : methods.returnAll(numBells)) {
			names.add(s[0]);
			notation.add(s[1]);
		}
	
	    methodpref.setEntries(names.toArray(new CharSequence[0]));
	    methodpref.setEntryValues(notation.toArray(new CharSequence[0]));
	}

	private void set_method_prefs(String s) {
		set_method_prefs(Integer.parseInt(s));
	}
	
	private void set_my_bell(int numBells) {

		@SuppressWarnings("deprecation")
		ListPreference myBell = 
				(ListPreference)findPreference("my_bell");
	    
		ArrayList<String> entries = new ArrayList<String>();
		
	    for (int i = 0; i < numBells; i++) {
	    	entries.add(String.valueOf(i+1));
	    }
	    
	    entries.add("None");
	    
	    myBell.setEntries(entries.toArray(new CharSequence[0]));
	    myBell.setEntryValues(entries.toArray(new CharSequence[0]));
	    myBell.setDefaultValue("None");
	}
	
	private void set_my_bell(String s) {
		set_my_bell(Integer.parseInt(s));
	}
	
/*	private void update_method_db() {
		int numBells = Integer.parseInt(
				spref.getString("number_of_bells", "6"));
		methods.updateDb(numBells);
	}
*/
}
