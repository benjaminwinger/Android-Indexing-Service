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

package ca.dracode.ais.indexinfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import ca.dracode.ais.alarm.Alarm;
import ca.dracode.ais.service.FileListener;

public class IndexInfo {
    private static final String TAG = "ca.dracode.ais.indexinfo";
    private boolean enabled = true;
    private boolean mIsBound = false;
    private IndexComm mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IndexComm.Stub.asInterface(service);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e(TAG, "Service has unexpectedly disconnected");
            mService = null;
        }
    };

    public IndexInfo(Context context){
        this.doBindService(context);
    }

    public void close(Context context){
        this.doUnbindService(context);
    }

    /**
     * Connects the IndexInfo to the InfoProxy which is running in the indexer process
     * @param context
     */
    private void doBindService(Context context) {
    // Establish a connection with the service.
        Log.i(TAG, "Binding to service...");
        mIsBound = context.bindService(new Intent("ca.dracode.ais.service.InfoProxy.PROXY"),
                mConnection,
                Context.BIND_AUTO_CREATE);
        Log.i(TAG, "Service is bound = " + mIsBound);
    }

    private void doUnbindService(Context context){
        if(mIsBound) {
            context.unbindService(mConnection);
        }
    }

    /**
     * Get Current State of the Indexer
     * @return true if the indexer is running, false otherwise
     */
    public boolean isIndexerRunning() {
        return false;
    }

    /**
     * Sets the file change listener
     * @return The path that the indexer is currently indexing
     */
    public String getCurrentIndexPath(){
        try {
            return mService.getCurrentIndexPath();
        } catch(RemoteException e){
            Log.e(TAG, "Error", e);
        }
        return "Error connecting to the service!";
    }

    /**
     * Gets the number of documents in the index
     * @return the number of documents in the index
     */
    public int getNumDocumentsInIndex() {
        return 0;
    }

    /**
     * Manually stops the indexer
     * @param context
     */
    public void stopIndexer(Context context) {
        Log.i(TAG, "Stopping Indexer");
        Alarm.CancelAlarm(context);
        try {
            if(mService != null) {
                Log.i(TAG, "Stopping Indexer...");
                mService.stopIndexer();
                Log.i(TAG, "Stopping Indexer...");
            }
        } catch(RemoteException e) {
            Log.e(TAG, "Error", e);
        }
    }

    /**
     * Manually starts the indexer
     * @param context
     */
    public void startIndexer(Context context) {
        Alarm.SetAlarm(context);
        Intent serviceIntent = new Intent(context, FileListener.class);
        context.startService(serviceIntent);
    }
}
