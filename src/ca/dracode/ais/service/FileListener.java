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
    private AISObserver listener;
    private IBinder mBinder = new LocalBinder();
    private Timer timeSinceLastChange;
    private static final int DELAY = 2000;

    @Override
    public void onCreate(){
        this.timeSinceLastChange = new Timer();
        this.listener = new AISObserver(Environment.getExternalStorageDirectory().getPath());
        Log.i(TAG, "Starting listener on " + Environment.getExternalStorageDirectory().getPath());
        this.listener.startWatching();
    }

    Handler timerHandler = new Handler();
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

    private class AISObserver extends FileObserver{
        public AISObserver(String path){
            super(path);
        }

        public void onEvent(int event, String path){
            if(path != null) {
                if(path.contains("Android/data/ca.dracode.ais")) {
                    return;
                }
                if(event == FileObserver.MODIFY || event
                        == FileObserver.MOVED_TO || event == FileObserver.CREATE) {
                    if(mBoundService == null){
                        doBindService();
                        while(mBoundService == null){
                            try{
                                Thread.sleep(10);
                            } catch(InterruptedException e){
                                Log.e(TAG, "Error ", e);
                            }
                        }
                    }
                    // If a file was changed or created, re-index file or index the new file
                    mBoundService.createIndex(new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + path));
                    timerHandler.postDelayed(timerRunnable, DELAY);
                    Log.i(TAG, "File changed: " + path);
                } else if(event == FileObserver.DELETE || event == FileObserver.MOVED_FROM) {
                    if(mBoundService == null){
                        doBindService();
                        while(mBoundService == null){
                            try{
                                Thread.sleep(10);
                            } catch(InterruptedException e){
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
        FileListener getService() {
            return FileListener.this;
        }
    }

    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    private IndexService mBoundService;
    private boolean mIsBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((IndexService.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(FileListener.this,
                IndexService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
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
}