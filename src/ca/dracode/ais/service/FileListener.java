/*
 * Copyright 2014 Dracode Software.
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
 * along with AIS.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.dracode.ais.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.util.Timer;

public class FileListener extends Service {
    private static final String TAG = "ca.dracode.ais.service.FileListener";
    private static final int DELAY = 2000;
    Handler timerHandler = new Handler();
    private AISObserver listener;
    private IBinder mLocalBinder = new LocalBinder();
    private Timer timeSinceLastChange;
    private IndexService mBoundService;
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if(mBoundService != null) {
                Log.i(TAG, "Stopping indexer");
                mBoundService.stopWhenReady();
                doUnbindService();
                mBoundService = null;
            }
        }
    };
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((IndexService.LocalBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
        }
    };
    private boolean mIsBound = false;

    @Override
    public void onCreate() {
        this.timeSinceLastChange = new Timer();
        this.listener = new AISObserver(Environment.getExternalStorageDirectory().getPath());
        Log.i(TAG, "Starting listener on " + Environment.getExternalStorageDirectory().getPath());
        this.listener.startWatching();
    }

    /**
     * Tells the listener to stop watching the filesystem and close
     */
    public void stopListener() {
        this.listener.stopWatching();
        this.doUnbindService();
        this.stopSelf();
    }

    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    private void doBindService() {
        bindService(new Intent(FileListener.this,
                IndexService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void doUnbindService() {
        if(mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    private class AISObserver extends FileObserver {
        public AISObserver(String path) {
            super(path);
        }

        @Override
        public void onEvent(int event, String path) {
            if(path != null) {
                if(path.contains("Android/data/ca.dracode.ais")) {
                    return;
                }
                if(event == FileObserver.MODIFY || event
                        == FileObserver.MOVED_TO || event == FileObserver.CREATE) {
                    if(mBoundService == null) {
                        doBindService();
                        while(mBoundService == null) {
                            try {
                                Thread.sleep(10);
                            } catch(InterruptedException e) {
                                Log.e(TAG, "Error ", e);
                            }
                        }
                    }
                    // If a file was changed or created, re-index file or index the new file
                    mBoundService.createIndex(new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + path), null);
                    timerHandler.postDelayed(timerRunnable, DELAY);
                    Log.i(TAG, "File changed: " + path);
                } else if(event == FileObserver.DELETE || event == FileObserver.MOVED_FROM) {
                    if(mBoundService == null) {
                        doBindService();
                        while(mBoundService == null) {
                            try {
                                Thread.sleep(10);
                            } catch(InterruptedException e) {
                                Log.e(TAG, "Error ", e);
                            }
                        }
                    }
                    // If a file was deleted, remove the file from the index
                    mBoundService.removeIndex(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + path);
                    Log.i(TAG, "File Deleted");
                    timerHandler.postDelayed(timerRunnable, DELAY);
                }
            }
        }
    }

    public class LocalBinder extends Binder {
        public FileListener getService() {
            return FileListener.this;
        }
    }
}