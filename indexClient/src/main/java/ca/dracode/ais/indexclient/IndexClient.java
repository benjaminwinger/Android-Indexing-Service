/*******************************************************************************
 * Copyright 2014 Benjamin Winger.
 *
 * This file is part of android-indexing-service-client-library.
 *
 * android-indexing-service-client-library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * android-indexing-service-client-library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with android-indexing-service-client-library.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/


package ca.dracode.ais.indexclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ca.dracode.ais.indexdata.PageResult;
import ca.dracode.ais.service.BSearchService1_0;

public class IndexClient {
	public static final int QUERY_BOOLEAN = 0;
	public static final int QUERY_STANDARD = 1;
	private static String TAG = "ca.dracode.ais.indexclient.IndexClient";
	private BSearchService1_0 mService = null;
	private boolean mIsBound;
	private String filePath;
	private Thread t;

	private ServiceConnection mConnection = new ServiceConnection() {
		// Called when the connection with the service is established
		public void onServiceConnected(ComponentName className, IBinder service) {
			// Following the example above for an AIDL interface,
			// this gets an instance of the IRemoteInterface, which we can use
			// to call on the service
			mService = BSearchService1_0.Stub.asInterface(service);
			Log.i(TAG, "Service: " + mService);
			loadIndex(filePath);
		}

		// Called when the connection with the service disconnects unexpectedly
		public void onServiceDisconnected(ComponentName className) {
			Log.e(TAG, "Service has unexpectedly disconnected");
			mService = null;
		}
	};
	private IndexListener listener;

	public IndexClient(IndexListener listener, final Context c, String filePath) {
		this.listener = listener;
		this.filePath = filePath;
		doBindService(c);
	}

    public boolean isServiceConnected(){
        return mIsBound;
    }

	public static void createServiceFile(String dir, String name,
	                                     List<String> extensions) {
		Log.i(TAG, "Creating folder: " + dir + new File(dir).mkdirs());
		if (!new File(dir + "/Service.is").exists()) {
			BufferedWriter bw = null;
			try {

				bw = new BufferedWriter(new FileWriter(dir + "/Service.is"));
			} catch (IOException e) {
				Log.e(TAG, "Error while creating writer: ", e);
				e.printStackTrace();
			}
			if (bw != null) {
				try {
					bw.write(name);
					bw.newLine();
					for (int i = 0; i < extensions.size(); i++) {
						bw.write(extensions.get(i));
						bw.newLine();
					}
					bw.close();
				} catch (IOException e) {
					Log.e(TAG, "Error while writing: ", e);
					e.printStackTrace();
				}
			}
		}
	}

	void doBindService(Context c) {
		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		Log.i(TAG, "Binding to service...");
		mIsBound = c.bindService(new Intent(
						"ca.dracode.ais.service.IndexService.SEARCH"), mConnection,
				Context.BIND_AUTO_CREATE
		);
		Log.i(TAG, "Service is bound = " + mIsBound);
	}

	public void close(Context c, String filePath){
		this.unloadIndex(filePath);
		this.doUnbindService(c);
	}

	private void doUnbindService(Context c) {
		if (mIsBound) {
			// Detach our existing connection.
			c.unbindService(mConnection);
			mIsBound = false;
		}
	}

	public void buildIndex(final String filePath,
	                       final ArrayList<String> contents) {
		/** TODO - Tell client if the index is unbuildable due to the lock being in place.
		 * 		Will not be able to send Strings in contents that are larger than 256KB 
		 * 		In the event of this, it should either try to send them as-is and hope it 
		 * 		does not crash (theoretically it should be able to send up to 1MB, but I found
		 * 		that sending that much would cause a crash), or split up the page into multiple parts and send them separately
		 * **/

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					int length = contents.size();
					for (int i = 0; i < length; i++) {
						int size = 0;
						ArrayList<String> tmp = new ArrayList<String>();
						int init = i;
						while (i < length && size + contents.get(i).getBytes().length < 256 * 1024) {
							tmp.add(contents.get(i));
							size += contents.get(i).getBytes().length;
							i++;
						}
						Log.e(TAG, "Size: " + size + " iterator: " + i);
						mService.buildIndex(filePath, tmp, init, length);
					}
					if (listener != null) {
						listener.indexCreated(filePath);
					}
				} catch (RemoteException e) {
					Log.e(TAG, "Error while closing buffered reader", e);
				}
			}
		}).start();
	}

	public void loadIndex(final String filePath) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Log.i(TAG, "Listener = " + listener + " Bound: " + mIsBound
						+ " Service " + mService);
				try {
					if (listener != null) {
						Log.e(TAG, "Trying to load from service " + mService);
						listener.indexLoaded(filePath, mService.load(filePath));
					} else {
						mService.load(filePath);
					}
				} catch (RemoteException e) {
					Log.e(TAG, "Error while loading remote service", e);
				}
			}
		}).start();
	}

	public void unloadIndex(final String filePath) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					listener.indexUnloaded(filePath, mService.unload(filePath));
				} catch (RemoteException e) {
					Log.e(TAG, "Error while communicating with remote service", e);
				}
			}
		}).start();
	}

	public void search(final String text, final String filePath, boolean kill) {
		this.search(text, IndexClient.QUERY_STANDARD, filePath, 0, 10, 0, kill);
	}

	public void search(final String text, final String filePath, int hits, boolean kill) {
		this.search(text, IndexClient.QUERY_STANDARD, filePath, 0, hits, 0, kill);
	}

	public void cancelSearch(){
		try{
			mService.interrupt();
		} catch (RemoteException e){
			Log.e(TAG, "Error while canceling search", e);
		}
	}

	public void search(final String text, final int type, final String filePath, final int page,
                       final int hits, final int set, boolean kill) {
		if(kill && t != null){
			cancelSearch();
			try {
				while (t != null) Thread.sleep(1);
			} catch (InterruptedException e){
				Log.e(TAG, "", e);
			}
		}
		t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Log.i(TAG, "Searching for " + text);
					PageResult[] results = mService.find(filePath, type, text, hits, page, set);
					if(results != null)
					listener.searchCompleted(text, results);
					Log.i(TAG, "Done Searching for " + text);
				} catch (RemoteException e) {
					Log.e(TAG, "Error while communicating with remote service", e);
					listener.errorWhileSearching(text, filePath);
				}
				t = null;
			}
		});
		t.start();
	}

	public void searchIn(final String text, final int type, final List<String> filePath,
                         final int hits, final int set, boolean kill){
		if(kill && t != null){
			cancelSearch();
			try {
				while (t != null) Thread.sleep(1);
			} catch (InterruptedException e){
				Log.e(TAG, "", e);
			}
		}
		t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Log.i(TAG, "Searching for " + text);
					PageResult[] results = mService.findIn(filePath, type, text, hits, set);
					if(results != null) {
						listener.searchCompleted(text, results);
					}
					Log.i(TAG, "Done Searching for " + text);
				} catch (RemoteException e) {
					Log.e(TAG, "Error while communicating with remote service", e);
					listener.errorWhileSearching(text, filePath.toString());
				}
				t = null;
			}
		});
		t.start();
	}

	public void searchInPath(final String text, final int type, final List<String> filePath,
                             final int hits, final int set, boolean kill){
		if(kill && t != null){
			cancelSearch();
			try {
				while (t != null) Thread.sleep(1);
			} catch (InterruptedException e){
				Log.e(TAG, "", e);
			}
		}
		t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Log.i(TAG, "Searching for " + text);
					List<String> list = mService.findName(filePath, type, text, hits, set);
					if(list != null)
					listener.searchCompleted(text, list);
					Log.i(TAG, "Done Searching for " + text);
				} catch (RemoteException e) {
					Log.e(TAG, "Error while communicating with remote service", e);
					listener.errorWhileSearching(text, filePath.toString());
				}
				t = null;
			}
		});
		t.start();
	}
}
