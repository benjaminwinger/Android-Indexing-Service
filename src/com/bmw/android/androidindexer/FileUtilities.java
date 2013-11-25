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
