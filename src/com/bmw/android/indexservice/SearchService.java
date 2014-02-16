package com.bmw.android.indexservice;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.bmw.android.androidindexer.FileIndex;
import com.bmw.android.androidindexer.FileIndexer;
import com.bmw.android.androidindexer.FileSearcher;
import com.bmw.android.androidindexer.Next;
import com.bmw.android.androidindexer.Word;
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

		/*
		 * public void loadAllIndexes() { File directory = new
		 * File(FileIndexer.getStorageDir()); File[] contents =
		 * directory.listFiles(new FileFilter() {
		 * 
		 * @Override public boolean accept(File f) { if (f.isFile() &&
		 * f.getName().toLowerCase().endsWith(".index") && f.canRead()) { return
		 * true; } return false; }
		 * 
		 * });
		 * 
		 * Comparator<File> cmp = new Comparator<File>() {
		 * 
		 * @Override public int compare(File left, File right) { if
		 * (left.length() < right.length()) { return -1; } else if
		 * (left.length() == right.length()) { return 0; } else { return 1; } }
		 * }; if (contents != null) { Arrays.sort(contents, cmp); }
		 * 
		 * for (int i = 0; i < contents.length; i++) { Log.i(TAG,
		 * "Loading index; size is: " + contents[i].length()); indexes.add(new
		 * FileIndexer(contents[i].getAbsolutePath()) .loadIndex()); }
		 * Log.i(TAG, "Done loading indexes"); finishedLoading = true; }
		 */

		private List<Result> find(String text, int variance) {
			return new ArrayList<Result>();
			/*
			 * FileIndex index = SearchService.this.index;
			 * 
			 * if (index == null) { return new ArrayList<Result>(); } if (text
			 * == null) throw new IllegalStateException("text cannot be null");
			 * if (text.length() < 1) { return new ArrayList<Result>(); } String
			 * tmp = text; String pdfName = new
			 * File(index.getFilename()).getName(); pdfName =
			 * pdfName.replace(".index", ""); pdfName = pdfName + ".pdf";
			 * String[] search = tmp.split(" "); for (int j = 0; j <
			 * search.length; j++) { search[j] = search[j].toLowerCase(); }
			 * ArrayList<Result> results = new ArrayList<Result>(); if
			 * (search.length == 1) { Set<Entry<String, Word>> words =
			 * index.getWords().entrySet(); for (Entry<String, Word> e : words)
			 * { if (e.getKey().contains(search[0])) { Set<Entry<Integer, Next>>
			 * curr = e.getValue().next
			 * 
			 * .entrySet(); for (Entry<Integer, Next> entry : curr) {
			 * results.add(new Result(entry.getValue().page, this
			 * .getResult(index, entry.getValue().page, entry.getKey(), 1,
			 * variance))); } } } } else { Set<String> words =
			 * index.getWords().keySet(); int location = -1; for (String w :
			 * words) { if (w.endsWith((search[0]))) { Word currentWord =
			 * index.getWords().get(w); if (search.length == 2) {
			 * Set<Entry<Integer, Next>> curr = currentWord.next .entrySet();
			 * for (Entry<Integer, Next> entry : curr) { if
			 * (entry.getValue().word.startsWith(search[1])) { location =
			 * entry.getKey(); results.add(new Result( entry.getValue().page,
			 * this.getResult(index, entry.getValue().page, location,
			 * search.length, variance))); } } } else { Set<Entry<Integer,
			 * Next>> curr = currentWord.next .entrySet(); for (Entry<Integer,
			 * Next> entry : curr) { if (entry.getValue().equals(search[1])) {
			 * if (this.matchesString(index, search, 2, index
			 * .getWord(entry.getValue().word), entry.getKey() + 1)) { location
			 * = entry.getKey();
			 * 
			 * results.add(new Result( entry.getValue().page,
			 * this.getResult(index, entry.getValue().page, location,
			 * search.length, variance)));
			 * 
			 * } } } } }
			 * 
			 * } }
			 * 
			 * return results;
			 */
		}

		private boolean[] quickFind(String text) {
			Document[] docs = SearchService.this.searcher.find("text", text,
					pages, "path", path);
			boolean[] results = new boolean[pages];
			for (int i = 0; i < docs.length; i++) {
				results[docs[i].getField("page").numericValue().intValue()] = true;
			}
			return results;
			/*
			 * FileIndex index = SearchService.this.index;
			 * 
			 * if (index == null) { return new boolean[0]; } if (text == null)
			 * throw new IllegalStateException("text cannot be null"); if
			 * (text.length() < 1) { return new boolean[0]; } String tmp = text;
			 * String pdfName = new File(index.getFilename()).getName(); pdfName
			 * = pdfName.replace(".index", ""); pdfName = pdfName + ".pdf";
			 * String[] search = tmp.split(" "); for (int j = 0; j <
			 * search.length; j++) { search[j] = search[j].toLowerCase(); }
			 * boolean[] results = new boolean[index.getPageCount()]; if
			 * (search.length == 1) { Set<Entry<String, Word>> words =
			 * index.getWords().entrySet(); for (Entry<String, Word> e : words)
			 * { Collection<Next> tmpCol = e.getValue().next.values(); for(Next
			 * next : tmpCol){ if(!results[next.page]){ if
			 * (e.getKey().contains(search[0])) { for (Next n :
			 * e.getValue().next.values()) { results[n.page] = true; Log.i(TAG,
			 * "Found at Page: " + n.page); } } } } } } else { Set<String> words
			 * = index.getWords().keySet(); int location = -1; for (String w :
			 * words) { if (w.endsWith((search[0]))) { Word currentWord =
			 * index.getWords().get(w); if (search.length == 2) {
			 * Set<Entry<Integer, Next>> curr = currentWord.next .entrySet();
			 * for (Entry<Integer, Next> entry : curr) { if
			 * (entry.getValue().word.startsWith(search[1])) {
			 * results[entry.getValue().page] = true; } } } else {
			 * Set<Entry<Integer, Next>> curr = currentWord.next .entrySet();
			 * for (Entry<Integer, Next> entry : curr) { if
			 * (entry.getValue().equals(search[1])) { if (this.matchesString(
			 * index, search, 2, index.getWord(entry.getValue().word),
			 * entry.getKey() + 1)) { results[entry.getValue().page] = true; } }
			 * } } } } }
			 * 
			 * return results;
			 */
		}

		private boolean matchesString(FileIndex index, String[] search,
				int searchPos, Word w, int position) {
			if (searchPos >= search.length) {
				return true;
			}
			Next next = w.next.get(position);
			if (searchPos == search.length - 1) {
				if (!next.word.startsWith(search[searchPos])) {
					return false;
				}
			} else {
				if (!next.equals(search[searchPos])) {
					return false;
				}
			}
			Word tmpWd = index.getWord(next.word);
			return this.matchesString(index, search, searchPos + 1, tmpWd,
					position + 1);
		}

		private String getResult(FileIndex index, int page, int pos, int len,
				int variance) {
			return index.getPhrase(page, pos - variance, pos + len + variance);
		}
	}

}
