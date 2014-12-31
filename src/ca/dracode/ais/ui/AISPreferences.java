/*
 * Copyright 2014 Dracode Software.
 *
 * This file is part of AIS.
 *
 * AIS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * AIS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AIS.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.dracode.ais.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.util.Log;

import ca.dracode.ais.R;
import ca.dracode.ais.indexinfo.IndexInfo;

public class AISPreferences extends PreferenceActivity {
    private static final String TAG = "ca.dracode.ais.ui.AISPreferences";
    private IndexInfo indexInfo = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        indexInfo = new IndexInfo(this);
        SwitchPreference mEnableWifi = (SwitchPreference) findPreference("enabled");
        mEnableWifi.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if(!((Boolean) newValue)) {
                        Log.i(TAG, "Stopping indexer...");
                        indexInfo.stopIndexer(getApplicationContext());
                    } else {
                        indexInfo.startIndexer(getApplicationContext());
                    }
                    return true;
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(indexInfo != null)
        indexInfo.close(this);
    }
}
