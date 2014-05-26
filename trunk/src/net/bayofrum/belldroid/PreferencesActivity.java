package net.bayofrum.belldroid;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
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
	    
	    // Dead code below; objects meaning number of bells,
	    // was supposed to make dependency easier.  Probably pointless.
/*	    lprefs.add(new LPref("number_of_bells", "6"));
	    lprefs.add(new LPref("my_bell", "None"));
	    lprefs.add(new LPref("selected_method", ""));
*/	    
		prefchangelistener = new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences s,
					String key) {
				if (key.equals("number_of_bells")) {
					String numBells = spref.getString("number_of_bells", "6");
					set_method_classes(numBells);
					set_method_prefs(numBells,
							spref.getString("method_class", "Plain"));
					set_my_bell(numBells);
				} else if (key.equals("method_class")) {
					set_method_prefs(spref.getString("number_of_bells", "6"),
							spref.getString("method_class", "none"));
				}
			}
		};
		
		methods = new Method(getBaseContext());
		
	    register_changelistener();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		register_changelistener();
	}
	
	private void register_changelistener() {
		spref.registerOnSharedPreferenceChangeListener(prefchangelistener);
		String numBells = spref.getString("number_of_bells", "6");
		set_method_classes(numBells);
		set_my_bell(numBells);
		set_method_prefs(numBells, spref.getString("method_class", "none"));
	}
	
	private void set_method_classes(int numBells) {
		@SuppressWarnings("deprecation")
		ListPreference classpref = 
				(ListPreference)findPreference("method_class");

		ArrayList<CharSequence> names = new ArrayList<CharSequence>();
		ArrayList<CharSequence> values = new ArrayList<CharSequence>();
		
		for (CharSequence s[] : methods.returnClasses(numBells)) {
			names.add(s[0]);
			values.add(s[1]);
		}
		
		classpref.setEntries(names.toArray(new CharSequence[0]));
		classpref.setEntryValues(values.toArray(new CharSequence[0]));
		//classpref.setValue(values.get(0).toString());
	}
	
	private void set_method_classes(String numBells) {
		set_method_classes(Integer.parseInt(numBells));
	}
	
	private void set_method_prefs(int numBells, String methodClass) {

		@SuppressWarnings("deprecation")
		ListPreference methodpref = 
				(ListPreference)findPreference("selected_method");
	    
		ArrayList<CharSequence> names = new ArrayList<CharSequence>();
		ArrayList<CharSequence> notation = new ArrayList<CharSequence>();
	    
		for (CharSequence s[] : methods.returnAll(numBells, methodClass)) {
			names.add(s[0]);
			notation.add(s[1]);
		}
	
	    methodpref.setEntries(names.toArray(new CharSequence[0]));
	    methodpref.setEntryValues(notation.toArray(new CharSequence[0]));
	}

	private void set_method_prefs(String s, String c) {
		set_method_prefs(Integer.parseInt(s), c);
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
	
	// The LPref class below is probably useless
	
	@SuppressWarnings("unused")
	private class LPref {
		
		private final ListPreference me;
		private final String name, def;
		private ArrayList<ListPreference> dependencies = 
				new ArrayList<ListPreference>();
		
		/** Make a preference with no dependencies */
		@SuppressWarnings("deprecation")
		public LPref(String name, String def) {
			this.name = name;
			this.def = def;
			me = (ListPreference) findPreference(this.name);
		}
		
		public String getName() {
			return name;
		}
		
		public String getValue() {
			return spref.getString(this.name, this.def);
		}
		
		public int getInt() {
			return Integer.parseInt(getValue());
		}
		
		public void addDependency(ListPreference dependency) {
			dependencies.add(dependency);
		}
		
		public void setEntries(ArrayList<CharSequence> entries,
				ArrayList<CharSequence> entryValues) {
			this.setEntries(entries.toArray(new CharSequence[0]),
				entryValues.toArray(new CharSequence[0]));
		}
		
		public void setEntries(CharSequence[] entries,
				CharSequence[] entryValues) {
			me.setEntries(entries);
			me.setEntryValues(entryValues);
		}
	}
}
