/**
 *  Copyright 2014 Benjamin Winger
 *  
 *  This file is part of The Android Indexing Service.
 *
 *   The Android Indexing Service is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   The Android Indexing Service is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with The Android Indexing Service.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bmw.android.androidindexer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

class FileUtilities {
	private String absolutePath;
	private final Context context;

	public FileUtilities(Context context) {
		super();
		this.context = context;
	}

	public void write(String fileName, FileIndexer data) {
		/*
		 * File root = Environment.getExternalStorageDirectory(); File outDir =
		 * new File(root.getAbsolutePath() + File.separator +
		 * "EZ_time_tracker"); if (!outDir.isDirectory()) { outDir.mkdir(); }
		 */
		Log.i("FileUtilities", "Saving...");
		try
        {
        	ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(fileName))); //Select where you wish to save the file...
        	oos.writeObject(data); // write the class as an 'object'
        	oos.flush(); // flush the stream to insure all of the information was written to 'save.bin'
        	oos.close();// close the stream
        }
        catch(Exception ex)
        {
		Log.v("PDFIndex",ex.getMessage());
        	ex.printStackTrace();
        }
			Toast.makeText(
					context.getApplicationContext(),
					"Report successfully saved to: "
							+ fileName, Toast.LENGTH_LONG)
					.show();

	}

	public String getAbsolutePath() {
		return absolutePath;
	}

}
