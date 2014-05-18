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

package ca.dracode.ais.alarm;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ca.dracode.ais.service.IndexService;

public class Alarm extends BroadcastReceiver {

	public static void SetAlarm(Context context) {
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, Alarm.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		// sets the alarm to repeat every 10 minutes
		// TODO - Make alarm time change according to a user preference
		if(am != null)
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), getMinutes(10), pi);
	}

	public static void CancelAlarm(Context context) {
		Intent i = new Intent(context, Alarm.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		AlarmManager am = (AlarmManager) context.getSystemService("Context.ALARM_SERVICE");
		am.cancel(pi);
	}

	private static int getMinutes(int minutes) {
		return minutes * 60000;
	}

	public void onReceive(Context context, Intent intent) {
		// Starts the indexService
		if (!this.isMyServiceRunning(context)) {
			Intent serviceIntent = new Intent(context, IndexService.class);
			context.startService(serviceIntent);
		}
	}

	private boolean isMyServiceRunning(Context c) {
		ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (IndexService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
