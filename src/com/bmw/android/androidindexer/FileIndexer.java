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

package com.bmw.android.androidindexer;

/*
 * FileIndexer.java
 * 
 * Contains functions for building the lucene index.
 *  TODO - Evaluate the usefullness of ForceMerging as it increases total indexing time by about 17%
 */

import android.os.Environment;
import android.util.Log;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
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

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileIndexer {
    private static String TAG = "com.bmw.android.androidindexer.FileIndexer";
    private IndexWriter writer;
    private FileSearcher searcher;

	public FileIndexer() {
		super();
		this.searcher = new FileSearcher();
		Directory dir;
		try {
			dir = FSDirectory.open(new File(FileIndexer.getRootStorageDir()));
            Analyzer analyzer = new SimpleAnalyzer(Version.LUCENE_47);
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_47,
                    analyzer);
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            this.writer = new IndexWriter(dir, iwc);
			this.writer.commit();
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
				doc.add(new StringField("id", file.getPath() + ":" + page,
						Field.Store.YES));
				doc.add(new StringField("path", file.getPath(),
						Field.Store.YES));
				doc.add(new LongField("modified", file.lastModified(),
						Field.Store.YES));
				// for(int i = 0; i < contents.size(); i++){
				doc.add(new TextField("text", "" + contents, Field.Store.YES));
				doc.add(new IntField("page", page, Field.Store.YES));
				// }
				if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
					writer.addDocument(doc);
				} else {
					// TODO - Test UpdateDocument
					writer.updateDocument(new Term("id", file.getPath() + ":"
							+ page), doc);
				}
				Log.i(TAG, "Done Indexing file: " + file.getName() + " " + page);
			} catch (Exception e) {
				Log.e(TAG, "Error ", e);
			}
		}
	}

    public static String getRootStorageDir() {
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
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


	// TODO - make the indexer restart indexing a file if it fails. When
	// buildIndex is called from SearchService android.os.DeadObjectException is
	// called on the SearchService from building larger indexes

    public boolean checkForIndex(String field, String value) throws Exception {
        return this.searcher.checkForIndex(field, value);
    }

    public int buildIndex(List<String> contents, String filename) {
        File indexDirFile = new File(FileIndexer.getRootStorageDir());
        try {
            for (int i = 0; i < contents.size(); i++) {
                if (!this.searcher.checkForIndex("id", filename + ":" + i)) {
                    FileIndexer.Build(writer, new File(filename), i, contents
                            .get(i));

                } else {
                    Log.i(TAG, "Skipping " + filename + ":" + i
                            + " Already in index");
                }
            }
            Log.i(TAG, "Writing Metadata");
            Document doc = new Document();
            File file = new File(filename);
            doc.add(new StringField("id", file.getPath() + ":meta",
                    Field.Store.YES));
            doc.add(new IntField("pages", contents.size(), Field.Store.YES));
            doc.add(new LongField("modified", file.lastModified(),
                    Field.Store.NO));
            if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
                writer.addDocument(doc);
            } else {
                // Todo - Use updateDocument to delete
                // the page that exists.
                // must create a field that combines
                // path and page number as if path is
                // used, it will delete all pages of the
                // document in the index
                writer.updateDocument(new Term("id", file.getPath() + ":meta"),
                        doc);
                writer.commit();
            }
            Log.i(TAG, "Done creating metadata");
            // Must only call ForceMerge and Commit once per document as they are very resource heavy operations
            writer.commit();
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
            return -1;
        }
        return 0;
    }

    public void close() {
        try {
            writer.commit();
            // TODO - Determine how much of a speed increase is gained while searching after ForceMerge
            writer.forceMerge(1);
            writer.close();
        } catch (IOException e) {
	        Log.e(TAG, "Error while closing indexwriter", e);
        }
    }
}
