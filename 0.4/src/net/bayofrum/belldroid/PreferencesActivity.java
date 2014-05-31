/*-
 * Copyright (c) 2014, Chris Rees
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *    
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
	/**
	 * Totally unbelievable.  You have to read this!
	 * http://stackoverflow.com/questions/2542938/sharedpreferences-onsharedpreferencechangelistener-not-being-called-consistently
	 */
	private OnSharedPreferenceChangeListener prefchangelistener;
	
	private Method methods;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    /* XXX Might crash without next line ?? */
	    // PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	    addPreferencesFromResource(R.xml.preferences);
	    spref = PreferenceManager.getDefaultSharedPreferences(this);
	    
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
	    
	    entries.add("None-- call changes");
	    
	    myBell.setEntries(entries.toArray(new CharSequence[0]));
	    myBell.setEntryValues(entries.toArray(new CharSequence[0]));
	    myBell.setDefaultValue("None-- call changes");
	}
	
	private void set_my_bell(String s) {
		set_my_bell(Integer.parseInt(s));
	}
}