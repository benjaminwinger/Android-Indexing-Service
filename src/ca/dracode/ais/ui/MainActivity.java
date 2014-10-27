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

package ca.dracode.ais.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import ca.dracode.ais.R;
import ca.dracode.ais.alarm.Alarm;
import ca.dracode.ais.service.FileListener;
import ca.dracode.ais.service.IndexService;

public class MainActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        TextView t = (TextView) findViewById(R.id.textView1);

        // TODO Remove testing code
        Intent serviceIntent = new Intent(this, IndexService.class);
        serviceIntent.putExtra("crawl", true);
        this.startService(serviceIntent);
        Intent serviceIntent2 = new Intent(this, FileListener.class);
        this.startService(serviceIntent2);
        Alarm.SetAlarm(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_settings) {
            Intent i = new Intent(this, AISPreferences.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
