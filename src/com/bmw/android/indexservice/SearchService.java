package com.bmw.android.indexservice;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.bmw.android.androidindexer.FileIndexer;
import com.bmw.android.androidindexer.FileSearcher;
import com.bmw.android.indexdata.Result;

public class SearchService extends Service {
	private static final String TAG = "com.bmw.android.indexservice.SearchService";
	private SearchManager sm;
	private FileSearcher searcher;
	private String path;
	private int pages;
	private ArrayList<String> text = new ArrayList<String>();

	// private final IBinder mBinder = new LocalBinder();

	private final BSearchService.Stub mBinder = new BSearchService.Stub() {
		public boolean[] find(String text) {
			return SearchService.this.sm.quickFind(text);
		}

		public void buildIndex(String filePath, List<String> text, int page,
				int maxPage) {
			SearchService.this.buildIndex(filePath, text, page, maxPage);
		}

		public boolean load(final String filePath) {
			return SearchService.this.load(filePath);
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.sm = new SearchManager();
		this.searcher = new FileSearcher();
	}

	public void buildIndex(String filePath, List<String> text, int page,
			int maxPage) {
		FileIndexer indexer = new FileIndexer();
		if (page == 0) {
			this.text.clear();
		}
		if (page + text.size() != maxPage) {
			this.text.addAll(text);
		} else {
			this.text.addAll(text);
			try {
				indexer.buildIndex(this.text, filePath);
			} catch (Exception ex) {
				Log.v("PDFIndex", "" + ex.getMessage());
				ex.printStackTrace();
			} finally {
				Log.i(TAG, "Built Index");
			}
		}
	}

	public boolean load(final String filePath) {
		this.path = filePath;
		Document tmp;
		if (SearchService.this.searcher == null) {
			Log.e(TAG, "Searcher is null");
			return false;
		}
		if ((tmp = this.searcher.getMetaFile(filePath)) != null) {
			try {
				IndexableField f = tmp.getField("pages");
				if(f == null) Log.e(TAG, "Cannot find pages in metafile: " + tmp.toString());
				this.pages = f.numericValue().intValue();
			} catch (Exception e) {
				Log.e(TAG, "Error", e);
				return false;
			}
			return true;
		} else {
			return false;
		}

	}

	private class SearchManager {
		boolean found;
		boolean finishedLoading = false;

		public SearchManager() {

		}

		private List<Result> find(String text, int variance) {
			return new ArrayList<Result>();
		}

		private boolean[] quickFind(String text) {
			Document[] docs = SearchService.this.searcher.find(FileSearcher.QUERY_BOOLEAN, "text", text,
					pages, "path", path);
			boolean[] results = new boolean[pages];
			for (int i = 0; i < docs.length; i++) {
				results[docs[i].getField("page").numericValue().intValue()] = true;
			}
			return results;
		}
	}

}
