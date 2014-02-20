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
import java.io.IOException;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import android.util.Log;

public class FileSearcher {
	private final String TAG = "com.bmw.android.androidindexer.FileSearcher";
	private IndexSearcher indexSearcher;
	public static final int QUERY_BOOLEAN = 0;
	public static final int QUERY_STANDARD = 1;

	public FileSearcher() {
		IndexReader indexReader = null;
		IndexSearcher indexSearcher = null;
		try {
			File indexDirFile = new File(FileIndexer.getRootStorageDir());
			Directory tmpDir = FSDirectory.open(indexDirFile);
			indexReader = DirectoryReader.open(tmpDir);
			indexSearcher = new IndexSearcher(indexReader);
		} catch (IOException ioe) {
			Log.e(TAG, "Error", ioe);
		}

		this.indexSearcher = indexSearcher;
	}

	public boolean checkForIndex(String field, String value) throws Exception {
		/*
		 * TODO Add capacity to check if the file needs to be updated by
		 * comparing the index metadata's modified date with the file's modified
		 * date
		 */

		Log.i(TAG, "Checking for existance of " + value);
		BooleanQuery qry = new BooleanQuery();
		qry.add(new TermQuery(new Term(field, value)), BooleanClause.Occur.MUST);
		if (this.indexSearcher != null && qry != null) {
			ScoreDoc[] hits;
			hits = indexSearcher.search(qry, 1).scoreDocs;
			if (hits.length > 0) {
				return true;
			} else {
				return false;
			}
		} else {
			Log.i(TAG, "Unable to check for index; building anyways");
			IndexReader indexReader = null;
			IndexSearcher indexSearcher = null;
			try {
				File indexDirFile = new File(FileIndexer.getRootStorageDir());
				Directory tmpDir = FSDirectory.open(indexDirFile);
				indexReader = DirectoryReader.open(tmpDir);
				indexSearcher = new IndexSearcher(indexReader);
			} catch (IOException ioe) {
				Log.e(TAG, "Error", ioe);
			}
			this.indexSearcher = indexSearcher;
			return false;
		}
	}

	public Document getMetaFile(String value) {
		BooleanQuery qry = new BooleanQuery();
		qry.add(new TermQuery(new Term("id", value + ":meta")),
				BooleanClause.Occur.MUST);
		ScoreDoc[] hits = null;
		try {
			hits = indexSearcher.search(qry, 1).scoreDocs;
		} catch (IOException e) {
			Log.e(TAG, "Error ", e);
		}
		if (hits.length == 0) {
			return null;
		}
		Document doc = null;
		try {
			doc = indexSearcher.doc(hits[0].doc);
		} catch (IOException e) {
			Log.e(TAG, "Error ", e);
		}
		return doc;
	}

	// TODO - Need to decide what types of searches to return. Currently, the
	// boolean search will return individual letters in a word and beyond that
	// only complete words. This should be changed so that it will give results
	// for the letters of the boolean search appearing in any part of a word

	public Document[] find(int type, String field, String value,
			int numResults, String constrainField, String constrainValue) {

		Query qry = null;
		if (type == FileSearcher.QUERY_BOOLEAN) {
			qry = new BooleanQuery();
			((BooleanQuery) qry).add(new TermQuery(new Term(field, value
					)), BooleanClause.Occur.MUST);
		} else if (type == FileSearcher.QUERY_STANDARD) {
			try {
				qry = new QueryParser(Version.LUCENE_46, field,
						new WhitespaceAnalyzer(Version.LUCENE_46)).parse(value);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			// qry.add(new Query(field, value));
		}
		if (qry != null) {
			BooleanQuery cqry = new BooleanQuery();
			cqry.add(new TermQuery(new Term(constrainField, constrainValue)),
					BooleanClause.Occur.MUST);
			Filter filter = new QueryWrapperFilter(cqry);
			ScoreDoc[] hits = null;
			try {
				hits = indexSearcher.search(qry, filter, numResults).scoreDocs;
			} catch (IOException e) {
				Log.e(TAG, "Error ", e);
			}
			Document[] docs = new Document[hits.length];
			for (int i = 0; i < hits.length; i++) {
				try {
					docs[i] = indexSearcher.doc(hits[i].doc);
				} catch (IOException e) {
					Log.e(TAG, "Error ", e);
				}
			}
			// TODO - Needs to return not only the document but what word/phrase
			// was found. This will need to use the Lucene Highlighter library
			return docs;
		} else {
			Log.e(TAG, "Query Type: " + type + " not recognised");
			return new Document[0];
		}
	}
}
