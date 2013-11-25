package com.bmw.android.androidindexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class FileIndexer {
	private static String TAG = "com.bmw.android.androidindexer.PDFIndexer";
	private IndexingListener listener;
	private FileIndex index;
	private Context context;

	public FileIndexer() {
		super();
	}

	public FileIndexer(String filename, Context c) {
		this.index = new FileIndex(filename);
		this.context = c;
	}

	public FileIndexer(String filename, IndexingListener i, Context c) {
		this(filename, c);
		this.listener = i;
	}

	public FileIndex loadIndex() {
		try {
			if (new File(index.getFilename()).exists()) {
				this.index.setWords(new KryoWrapper().ReadBuffered(FileIndexer
						.getFileDir(index.getFilename())));
				this.index.setIndexed(this.index.getWords().size());
			}
			if (this.listener != null) {
				this.listener.indexLoaded();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this.index;
	}

	public FileIndex buildIndex(List<String> contents, String filename) {
		if (!new File(FileIndexer.getStorageDir()).exists()) {
			if (new File(FileIndexer.getStorageDir()).mkdirs()) {
				Log.i(TAG, "Created Folders at " + FileIndexer.getStorageDir());
			} else {
				Log.i(TAG,
						"Failed to create folder at "
								+ FileIndexer.getStorageDir());
			}
		}
		String text = "";
		for (int i = 0; i < contents.size(); i++) {
			text = contents.get(i);
			Log.i("PDF", "indexing: Page " + i);
			StringTokenizer tokens = new StringTokenizer(text);
			ArrayList<String> words = new ArrayList<String>();
			HashMap<String, Word> indexed = new HashMap<String, Word>();
			while (tokens.hasMoreTokens()) {
				words.add(tokens.nextToken());
			}
			for (int j = words.size() - 1; j >= 0; j--) {
				boolean found = false;
				Word temp = null;
				if ((temp = indexed.get(words.get(j))) != null) {
					found = true;
					if (j < words.size() - 1) {
						String next = words.get(j + 1);
						temp.addNext(next, j);
					}
				}
				if (!found) {
					temp = new Word();
					if (j < words.size() - 2) {
						String next = words.get(j + 1);
						temp.addNext(next, j);
					}
					indexed.put(words.get(j), temp);
				}
			}
			this.index.setWordsForPage(indexed, i);
			this.index.setIndexed(i + 1);
		}
		if (this.listener != null) {
			this.listener.indexingCompleted();
		}
		return this.index;
	}
	
	public void writeIndexz(){
		try {
			new KryoWrapper().WriteBuffered(index.getWords(),
					FileIndexer.getFileDir(index.getFilename()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// new KryoWrapper().WriteBuffered(this.index.getWords(),
		// this.context.openFileOutput(new
		// File(this.index.getFilename()).getName(),
		// Context.MODE_PRIVATE));
	}

	public static String getStorageDir() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}

		if (mExternalStorageAvailable && mExternalStorageWriteable) {
			return Environment.getExternalStorageDirectory()
					+ "/Android/data/com.bmw.android.ais/indexes";
		} else {
			return null;
		}
	}

	public static String getRootStorageDir() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}

		if (mExternalStorageAvailable && mExternalStorageWriteable) {
			return Environment.getExternalStorageDirectory()
					+ "/Android/data/com.bmw.android.ais";
		} else {
			return null;
		}
	}

	public static String getFileDir(String filepath) {
		String tmp;
		if (filepath == null) {
			return null;
		}
		if ((tmp = FileIndexer.getStorageDir()) != null) {
			String file;
			if (filepath.lastIndexOf("/") != -1) {
				file = filepath.substring(filepath.lastIndexOf("/"),
						filepath.lastIndexOf("."));
			} else {
				file = filepath;
			}

			return tmp + file + ".index";
		} else {
			return null;
		}
	}

	public static boolean indexExists(String filepath) {
		boolean mExternalStorageAvailable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
		}
		int index1 = filepath.lastIndexOf("/");
		int index2 = filepath.lastIndexOf(".");
		if (index1 == -1 || index2 == -1) {
			return false;
		}
		String file = filepath.substring(index1, index2);

		String path = Environment.getExternalStorageDirectory()
				+ "/Android/data/com.bmw.android.ais/indexes/" + file;
		if (mExternalStorageAvailable) {
			return (new File(path + ".index")).exists();
		} else {
			return false;
		}

	}
}
