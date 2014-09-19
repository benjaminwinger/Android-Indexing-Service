/*******************************************************************************
 * Copyright 2014 Benjamin Winger.
 *
 * This file is part of Android Indexing Service.
 *
 * Android Indexing Service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Android Indexing Service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Android Indexing Service.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package ca.dracode.ais.indexinfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import android.os.RemoteException;

import ca.dracode.ais.alarm.Alarm;
import ca.dracode.ais.service.FileListener;

public class IndexInfo {
    private static final String TAG = "ca.dracode.ais.indexinfo";
    private boolean enabled = true;
	/*
		Get Current State of the Indexer
		@return true if the indexer is running, false otherwise
	 */
	public boolean isIndexerRunning() {
		return false;
	}

	/*
		Sets the file change listener
		@param a listener that will be notified whenever the indexer starts indexing a
			new file
	 */
	public void setFileChangeListener(FileChangeListener listener) {

	}

	/*
		Gets the number of documents in the index
		@return the number of documents in the index
	 */
	public int getNumDocumentsInIndex() {
		return 0;
	}

	/*
		Manually stops the indexer
		@precondition the indexer is running
		@postcondition the indexer will no longer be running
	 */
	public void stopIndexer(Context context) {
        Alarm.CancelAlarm(context);
        try {
            mService.stopIndexer();
        } catch(RemoteException e){
            Log.e(TAG, "Error", e);
        }
	}

	/*
		Manually starts the indexer
		@precondition the indexer is not running
		@postcondition the indexer will be running
	 */
	public void startIndexer(Context context) {
        Alarm.SetAlarm(context);
        Intent serviceIntent = new Intent(context, FileListener.class);
        context.startService(serviceIntent);
	}

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
}
