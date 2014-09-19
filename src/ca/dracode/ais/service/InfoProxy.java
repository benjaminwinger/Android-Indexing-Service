/*
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
 * along with AIS.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.dracode.ais.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import ca.dracode.ais.indexinfo.FileChangeListener;
import ca.dracode.ais.indexinfo.IndexComm;

public class InfoProxy extends Service {
    private final IndexComm.Stub mBinder = new IndexComm.Stub() {
        public void stopIndexer(){
            mListenerService.stopListener();
        }

        public int getNumDocumentsInIndex(){
            return 0;
        }

        public boolean isIndexerRunning(){
            return false;
        }

        public void setFileChangeListener(FileChangeListener listener){

        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private FileListener mListenerService;
    private boolean mIsListenerBound = false;

    private ServiceConnection mListenerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mListenerService = ((FileListener.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mListenerService = null;
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(InfoProxy.this,
                FileListener.class), mListenerConnection, Context.BIND_AUTO_CREATE);
        mIsListenerBound = true;
    }

    void doUnbindService() {
        if (mIsListenerBound) {
            // Detach our existing connection.
            unbindService(mListenerConnection);
            mIsListenerBound = false;
        }
    }
}
