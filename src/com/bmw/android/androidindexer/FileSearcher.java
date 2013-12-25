package com.bmw.android.androidindexer;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import android.util.Log;

public class FileSearcher {
	private final String TAG = "com.bmw.android.androidindexer.FileSearcher";
	private IndexSearcher indexSearcher;

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
	
	public Document getMetaFile(String value){
		BooleanQuery qry = new BooleanQuery();
		qry.add(new TermQuery(new Term("id", value + ":meta")), BooleanClause.Occur.MUST);
        ScoreDoc[] hits = null;
		try {
			hits = indexSearcher.search(qry, 1).scoreDocs;
		} catch (IOException e) {
			Log.e(TAG, "Error ", e);
		}
		Document doc = null;
		try {
			doc = indexSearcher.doc(hits[0].doc);
		} catch (IOException e) {
			Log.e(TAG, "Error ", e);
		}
        return doc;
	}

	public Document[] find(String field, String value, int numResults, String constrainField, String constrainValue){
		BooleanQuery qry = new BooleanQuery();
		qry.add(new TermQuery(new Term(field, value)), BooleanClause.Occur.MUST);
		BooleanQuery cqry = new BooleanQuery();
		cqry.add(new TermQuery(new Term(constrainField, constrainValue)), BooleanClause.Occur.MUST);
		Filter filter = new QueryWrapperFilter(cqry);
        ScoreDoc[] hits = null;
		try {
			hits = indexSearcher.search(qry, filter, numResults).scoreDocs;
		} catch (IOException e) {
			Log.e(TAG, "Error ", e);
		}
		Document[] docs = new Document[hits.length];
		for(int i = 0; i < hits.length; i++){
			try {
				docs[i] = indexSearcher.doc(hits[i].doc);
			} catch (IOException e) {
				Log.e(TAG, "Error ", e);
			}
		}
        return docs;
	}

}
