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

        public String getCurrentIndexPath(){ return "/sdcard";}
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private FileListener mListenerService;
    private boolean mIsListenerBound = false;

    private ServiceConnection mListenerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mListenerService = ((FileListener.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mListenerService = null;
        }
    };

    void doBindService() {
        bindService(new Intent(InfoProxy.this,
                FileListener.class), mListenerConnection, Context.BIND_AUTO_CREATE);
        mIsListenerBound = true;
    }

    void doUnbindService() {
        if (mIsListenerBound) {
            unbindService(mListenerConnection);
            mIsListenerBound = false;
        }
    }
}
