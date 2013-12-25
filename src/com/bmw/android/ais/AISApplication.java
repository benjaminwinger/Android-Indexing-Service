package com.bmw.android.ais;

import org.apache.lucene.search.IndexSearcher;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.bmw.android.indexservice.IndexService;


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
