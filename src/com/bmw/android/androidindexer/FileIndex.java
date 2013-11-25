package com.bmw.android.androidindexer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import android.util.Log;

public class FileIndex {
	private static String TAG = "com.bmw.android.androidindexer.PDFIndex";
	private String filename;
	private ArrayList<HashMap<String, Word>> words;
	private int pages;
	private int indexed = 0;
	
	public FileIndex() {
		this.words = new ArrayList<HashMap<String, Word>>();
	}
	
	public FileIndex(String filename){
		this();
		this.filename = filename;
	}
	
	public FileIndex(ArrayList<HashMap<String, Word>> words, String filename){
		this(filename);
		this.words = words;
		this.pages = words.size();
	}
	
	public HashMap<String, Word> getWordsForPage(int page) {
		while (page > this.indexed) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return this.words.get(page);
	}
	
	public Word getWord(String text, int page) {
		while (page > this.indexed) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return this.words.get(page).get(text);
	}

	public String getPhrase(int page, int start, int stop) {
		String str = "";
		if(start < 0){
			start = 0;
		}
		HashMap<String, Word> tmp = this.words.get(page);
		Entry<String, Word> iter = null;
		Set<Entry<String, Word>> c = tmp.entrySet();
		for (Entry<String, Word> e : c) {
			if (e.getValue().containsPos(start)) {
				iter = e;
				break;
			}
		}
		if (iter != null) {
			Word value = iter.getValue();
			String key = iter.getKey();
			str = key;
			Log.i(TAG, key);
			for (int i = start; i < stop; i++) {
				key = value.next.get(i);
				value = tmp.get(value.next.get(i));
				if(key == null || value == null){
					return str;
				}
				str = str.concat(" " + key);
			}
			return str;
		}
		return "";
	}
	
	public void setWordsForPage(HashMap<String, Word> words, int page) {
		if(this.words.size() <= page){
			for(int i = 0; i <= page; i++){
				this.words.add(new HashMap<String, Word>());
			}
			this.pages = page + 1;
		}
		this.words.set(page, words);
	}

	public ArrayList<HashMap<String, Word>> getWords() {
		return words;
	}

	public void setWords(ArrayList<HashMap<String, Word>> words) {
		this.words = words;
		if(words != null){
			this.pages = words.size();
		}
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public int getIndexed() {
		return indexed;
	}

	public void setIndexed(int indexed) {
		this.indexed = indexed;
	}
	
	public int getPageCount(){
		return this.pages;
	}
	
	public long getSize() {
		long size = 0;
		for (HashMap<String, Word> m : this.words) {
			Set<String> c = m.keySet();
			for (String s : c) {
				byte[] bytes = s.getBytes();
				size += bytes.length;
			}
			Collection<Word> p = m.values();
			for (Word w : p) {
				size += w.getSize();
			}
		}
		return size;
	}
	
}
