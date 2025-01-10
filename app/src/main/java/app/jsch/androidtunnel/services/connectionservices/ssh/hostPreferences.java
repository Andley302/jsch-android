package app.jsch.androidtunnel.connectionservices.ssh;

/* Copyright 2011 Shai Ayal
 * 
 * This file is part of SPT.
 *
 * SPT is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPT is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SPT.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import app.jsch.androidtunnel.R;
import app.jsch.androidtunnel.services.connectionservices.ssh.util.FileList;
import app.jsch.androidtunnel.services.connectionservices.ssh.util.PrefsFunc;


public class hostPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		populatePKList ();
		
		class PrefsSummary extends PrefsFunc {
			@Override
			public void go(Preference pref) {
				updatePrefSummary(pref);
			}
		}
		iteratePrefs(new PrefsSummary());
	}
	
	
	private void populatePKList () {
		final FileList fl = new FileList();
		ListPreference lp = (ListPreference) findPreference("privatekeyfile");
        if (lp != null) {
            lp.setEntries(fl.getNamesList());
            lp.setEntryValues(fl.getFnamesList());
        }
	}

	@Override 
	protected void onResume(){
		super.onResume();
		// Set up a listener whenever a key changes             
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override 
	protected void onPause() { 
		super.onPause();
		// Unregister the listener whenever a key changes             
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);     
	} 

	private void iteratePrefs(PrefsFunc pf) {
        for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
            initIterator(getPreferenceScreen().getPreference(i), pf);
        }
	}

	private void initIterator(Preference p, PrefsFunc pf){
        if (p instanceof PreferenceCategory){
             PreferenceCategory pCat = (PreferenceCategory)p;
             for(int i=0;i<pCat.getPreferenceCount();i++){
                 initIterator(pCat.getPreference(i), pf);
             }
         }else{
             pf.go(p);
         }
     }

	private void updatePrefSummary (Preference pref) {
		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());
		}	
		else if (pref instanceof EditTextPreference) {
			EditTextPreference textPref = (EditTextPreference) pref;
			pref.setSummary(textPref.getText());
		}
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedprefs, String key) {
		updatePrefSummary(findPreference(key));
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.prefs, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		/*switch (item.getItemId()) {
		case R.id.prefexport:
			class PrefsExport extends PrefsFunc {
				private Properties props = null;
				public PrefsExport() {
					props = new Properties();
				}
				@Override
				public void go(Preference pref) {
					try {
						if (pref instanceof ListPreference) {
							ListPreference listPref = (ListPreference) pref;
							props.setProperty(listPref.getKey(), listPref.getValue());
						}	
						else if (pref instanceof EditTextPreference) {
							EditTextPreference textPref = (EditTextPreference) pref;
							props.setProperty(textPref.getKey(), textPref.getText());
						}
						else if (pref instanceof CheckBoxPreference) {
							CheckBoxPreference checkPref = (CheckBoxPreference) pref;
							props.setProperty(checkPref.getKey(), checkPref.isChecked() ? "yes" : "no");
						}
					} catch (NullPointerException e) {
						// We assume this means the value is null which we will convert to an empty string
						props.setProperty(pref.getKey(), "");
					}
				}
				public Properties getProps() {
					return props;
				}
			}
			PrefsExport pe = new PrefsExport();
			iteratePrefs(pe);

			final File sdcard = Environment.getExternalStorageDirectory();

			// Don't show a dialog if the SD card is completely absent.
			final String state = Environment.getExternalStorageState();
			if (!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)
					&& !Environment.MEDIA_MOUNTED.equals(state)) {
				Toast.makeText(getApplicationContext(), R.string.ro_filesystem, Toast.LENGTH_SHORT).show();
				return true;
			}

			try {
				pe.getProps().save(new FileOutputStream(sdcard.getAbsolutePath() + "/spt_prefs.txt"),"");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
            return true;
		case R.id.prefimport:
			class PrefsImport extends PrefsFunc {
				private Properties props = null;
				public PrefsImport(String fname){
					props = new Properties();
					try {
						props.load(new FileInputStream(fname));
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				@Override
				public void go(Preference pref) {
					if (pref instanceof ListPreference) {
						ListPreference listPref = (ListPreference) pref;
						listPref.setValue(props.getProperty(listPref.getKey(),""));
					}	
					else if (pref instanceof EditTextPreference) {
						EditTextPreference textPref = (EditTextPreference) pref;
						textPref.setText(props.getProperty(textPref.getKey(), ""));
					}
					else if (pref instanceof CheckBoxPreference) {
						CheckBoxPreference checkPref = (CheckBoxPreference) pref;
						checkPref.setChecked(props.getProperty(checkPref.getKey(), "no").equals("yes"));
					}
				}
			}
			final FileList fl = new FileList();
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(R.string.choose_prefs_file);
			alert.setItems(fl.getNamesList(),new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					PrefsImport pi = new PrefsImport(fl.getFnamesList()[item]);
					iteratePrefs(pi);
				}
			});
			alert.show();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
	}*/

		return true;

	}
}