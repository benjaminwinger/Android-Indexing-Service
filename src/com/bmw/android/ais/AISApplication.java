/*******************************************************************************
 * Copyright 2014 Benjamin Winger.
 * 
 * This file is part of AIS.
 * 
 * AIS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * AIS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with The Android Indexing Service.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmw.android.ais;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.bmw.android.indexservice.IndexService;

import org.apache.lucene.search.IndexSearcher;

/*
 * AISApplication.java
 * 
 * This file currently starts the IndexService upon application launch if it is not already started.
 * 
 * v0.3
 * Implement alarmManager to start the service at user-defined intervals
 */

public class AISApplication extends Application {
    
    private final static String TAG = "com.bmw.android.ais";
	private IndexSearcher indexSearcher;
    
    public void onCreate() {
        super.onCreate();        
    }
    
    /**
     * Called by system when low on memory.
     * Currently only logs.
     */
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "onLowMemory"); // TODO: free some memory (caches) in native code
    }
    
    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (IndexService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
}
