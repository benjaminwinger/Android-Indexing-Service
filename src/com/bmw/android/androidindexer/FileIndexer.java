/*******************************************************************************
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
 * along with The Android Indexing Service.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmw.android.androidindexer;

/*
 * FileIndexer.java
 * 
 * Contains functions for building the lucene index.
 *  
 */

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

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
	
	public boolean checkForIndex(String field, String value) throws Exception {
		return this.searcher.checkForIndex(field, value);
	}


	// TODO - make the indexer restart indexing a file if it fails. When
	// buildIndex is called from SearchService android.os.DeadObjectException is
	// called on the SearchService from building larger indexes

	public int buildIndex(List<String> contents, String filename) {
		File indexDirFile = new File(FileIndexer.getRootStorageDir());
		try {
			for (int i = 0; i < contents.size(); i++) {
				if (!this.searcher.checkForIndex("id", filename + ":" + i)) {
					Directory dir = FSDirectory.open(indexDirFile);
					Analyzer analyzer = new WhitespaceAnalyzer(
							Version.LUCENE_46);
					IndexWriterConfig iwc = new IndexWriterConfig(
							Version.LUCENE_46, analyzer);
					iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
					writer = new IndexWriter(dir, iwc);
					FileIndexer.Build(writer, new File(filename), i, contents
							.get(i).toLowerCase(Locale.US));
					writer.close();
				} else {
					Log.i(TAG, "Skipping " + filename + ":" + i
							+ " Already in index");
				}
			}

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
			return -1;
		}
		return 0;
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

}
