package com.bmw.android.indexservice;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.bmw.android.androidindexer.FileIndexer;
import com.bmw.android.androidindexer.FileSearcher;
import com.bmw.android.indexdata.PageResult;

public class SearchService extends Service {
	private static final String TAG = "com.bmw.android.indexservice.SearchService";
	private SearchManager sm;
	private FileSearcher searcher;
	private HashMap<String, SearchData> data;

	// private final IBinder mBinder = new LocalBinder();

	private final BSearchService1_0.Stub mBinder = new BSearchService1_0.Stub() {
		public PageResult[] find(String doc, int type, String text, int numHits, int page) {
			return SearchService.this.sm.find(doc, type, text, numHits, page);
		}

		public int buildIndex(String filePath, List<String> text, double page,
				int maxPage) {
			return SearchService.this.buildIndex(filePath, text, page, maxPage);
		}

		public int load(final String filePath) {
			return SearchService.this.load(filePath);
		}
		
		public boolean unload(final String filePath){
			return SearchService.this.unload(filePath);
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
		this.data = new HashMap<String, SearchData>();
	}

	public int buildIndex(String filePath, List<String> text, double page,
			int maxPage) {
		File indexDirFile = new File(FileIndexer.getRootStorageDir());
		File[] dirContents = indexDirFile.listFiles();
		for(int i = 0; i < dirContents.length; i++){
			if(dirContents[i].getName().equals("write.lock")){
				return 1;
			}
		}
		FileIndexer indexer = new FileIndexer();
		SearchData tmpData = this.data.get(filePath);
		if (page == 0) {
			tmpData.text.clear();
		}
		if (page + text.size() != maxPage) {
			tmpData.text.addAll(text);
		} else {
			tmpData.text.addAll(text);
			try {
				indexer.buildIndex(tmpData.text, filePath);
			} catch (Exception ex) {
				Log.v("PDFIndex", "" + ex.getMessage());
				ex.printStackTrace();
			} finally {
				Log.i(TAG, "Built Index");
			}
			return 0;
		}
		return 2;
	}

	public int load(final String filePath) {
		if(this.data.containsKey(filePath)){
			return 1;
		}
		SearchData tmpData = new SearchData();
		String path = filePath;
		Document tmp;
		if (SearchService.this.searcher == null) {
			Log.e(TAG, "Searcher is null");
			return -1;
		}
		if ((tmp = this.searcher.getMetaFile(filePath)) != null) {
			try {
				IndexableField f = tmp.getField("pages");
				if(f == null) Log.e(TAG, "Cannot find pages in metafile: " + tmp.toString());
				tmpData.pages = f.numericValue().intValue();
			} catch (Exception e) {
				Log.e(TAG, "Error", e);
				return -1;
			}
			this.data.put(path, tmpData);
			return 0;
		} else {
			return 2;
		}

	}
	
	public boolean unload(String path){
		return this.data.remove(path) != null;
	}

	private class SearchManager {
		boolean found;
		boolean finishedLoading = false;

		public SearchManager() {

		}

		private PageResult[] find(String doc, int type, String text, int numHits, int page) {
			/** TODO - Utilize the remaining function arguments 
			 * 	Doc - the document to be searched; should replace local variable path if the document has already been loaded
			 * 	numHits - the maximum number of results to load into the PageResult array
			 * 	page - the page to start on if only loading a certain number of results
			 * **/
			SearchData tmpData = SearchService.this.data.get(doc);
			Document[] docs = SearchService.this.searcher.find(type, "text", text,
					tmpData.pages, "path", doc);
			PageResult[] results = new PageResult[tmpData.pages];
			for(int i = 0; i < results.length; i++){
				results[i] = new PageResult(new ArrayList());
			}
			for (int i = 0; i < docs.length; i++) {
				String result = "";
				/** TODO - Add Highlighter Code to retrieve the generated phrase here **/
				results[docs[i].getField("page").numericValue().intValue()].text.add(result);
			}
			return results;
		}
	}

}
