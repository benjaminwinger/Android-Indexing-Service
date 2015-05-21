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

package ca.dracode.ais.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import ca.dracode.ais.service.FileListener;
import ca.dracode.ais.service.IndexService;

public class AutoStart extends BroadcastReceiver {

    /**
     * Checks if the received action is BOOT_COMPLETED and if so, sets the alarm for the Indexer
     * @param context
     * @param intent
     */
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED") && prefs.getBoolean("enabled", true)) {
            Alarm.SetAlarm(context);
            Intent serviceIntent = new Intent(context, FileListener.class);
            context.startService(serviceIntent);
            serviceIntent = new Intent(context, IndexService.class);
            serviceIntent.putExtra("crawl", true);
            context.startService(serviceIntent);
        }
    }
}
