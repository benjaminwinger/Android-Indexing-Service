package com.bmw.android.androidindexer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class FileIndexer {
	private static String TAG = "com.bmw.android.androidindexer.PDFIndexer";
	private IndexingListener listener;
	private Context context;
	private IndexWriter writer;
	private FileSearcher searcher;

	public FileIndexer() {
		super();
		this.searcher = new FileSearcher();
		Directory dir;
		try {
			dir = FSDirectory.open(new File(FileIndexer.getRootStorageDir()));
			Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_46);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_46,
					analyzer);
			iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			this.writer = new IndexWriter(dir, iwc);
			this.writer.commit();
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public FileIndexer(String filename, Context c) {
		this.context = c;
	}

	public FileIndexer(String filename, IndexingListener i, Context c) {
		this(filename, c);
		this.listener = i;
	}

	public boolean checkForIndex(String field, String value) throws Exception {
		return this.searcher.checkForIndex(field, value);
	}

	public static void Build(IndexWriter writer, File file, int page,
			String contents) {
		if (file.canRead()) {
			try {
				Log.i(TAG, "Started Indexing file: " + file.getName() + " "
						+ page);
				Document doc = new Document();
				doc.add(new StringField("path", file.getPath(), Field.Store.YES));
				doc.add(new StringField("id", file.getPath() + ":" + page,
						Field.Store.YES));
				doc.add(new LongField("modified", file.lastModified(),
						Field.Store.NO));
				// for(int i = 0; i < contents.size(); i++){
				doc.add(new TextField("text", contents, Field.Store.YES));
				doc.add(new IntField("page", page, Field.Store.YES));
				// }
				if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
					writer.addDocument(doc);
					writer.commit();
					// writer.forceMerge(1);
				} else {
					// TODO - Use updateDocument to delete the page that exists.
					// must create a field that combines path and page number as
					// if path is used, it will delete all pages of the document
					// in the index
					writer.updateDocument(new Term("id", file.getPath() + ":"
							+ page), doc);
					// writer.addDocument(doc);
					writer.commit();
					// writer.forceMerge(1);
				}
				Log.i(TAG, "Done Indexing file: " + file.getName() + " " + page);
			} catch (Exception e) {
				Log.e(TAG, "Error ", e);
			}
		}
	}

	// TODO - make the indexer restart indexing a file if it fails. When
	// buildIndex is called from SearchService android.os.DeadObjectException is
	// called on the SearchService from building larger indexes

	public void buildIndex(List<String> contents, String filename) {
		try {
			for (int i = 0; i < contents.size(); i++) {
				if (!this.searcher.checkForIndex("id", filename + ":" + i)) {
					boolean create = true;
					File indexDirFile = new File(
							FileIndexer.getRootStorageDir());

					Directory dir = FSDirectory.open(indexDirFile);
					Analyzer analyzer = new WhitespaceAnalyzer(
							Version.LUCENE_46);
					IndexWriterConfig iwc = new IndexWriterConfig(
							Version.LUCENE_46, analyzer);
					iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
					writer = new IndexWriter(dir, iwc);
					FileIndexer.Build(writer, new File(filename), i, contents
							.get(i).toLowerCase());
					writer.close();
				} else {
					Log.i(TAG, "Skipping " + filename + ":" + i
							+ " Already in index");
				}
			}
			File indexDirFile = new File(FileIndexer.getRootStorageDir());

			Directory dir;

			dir = FSDirectory.open(indexDirFile);

			Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_46);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_46,
					analyzer);
			iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			writer = new IndexWriter(dir, iwc);
			Log.i(TAG, "Writing Metadata");
			Document doc = new Document();
			File file = new File(filename);
			doc.add(new StringField("path", file.getPath(), Field.Store.YES));
			doc.add(new StringField("id", file.getPath() + ":meta",
					Field.Store.YES));
			doc.add(new IntField("pages", contents.size(), Field.Store.YES));
			doc.add(new LongField("modified", file.lastModified(),
					Field.Store.NO));
			if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
				writer.addDocument(doc);
				writer.commit();
				// writer.forceMerge(1);
			} else {
				// Todo - Use updateDocument to delete
				// the page that exists.
				// must create a field that combines
				// path and page number as if path is
				// used, it will delete all pages of the
				// document in the index
				writer.updateDocument(new Term("id", file.getPath() + ":meta"),
						doc);
				// writer.addDocument(doc);
				writer.commit();
				// writer.forceMerge(1);
			}
			Log.i(TAG, "Done creating metadata");
			writer.forceMerge(1);
			writer.commit();
			writer.close();
		} catch (Exception e) {
			Log.e(TAG, "Error", e);
		}
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
