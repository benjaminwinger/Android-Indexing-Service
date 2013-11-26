package com.bmw.android.androidindexer;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import android.util.Log;

public class FileIndex implements Serializable{
	private static String TAG = "com.bmw.android.androidindexer.PDFIndex";
	private String filename;
	private HashMap<String, Word> words;
	private int pages;

	public FileIndex() {
		this.words = new HashMap<String, Word>();
	}

	public FileIndex(String filename) {
		this();
		this.filename = filename;
	}

	public FileIndex(HashMap<String, Word> words, String filename, int pages) {
		this(filename);
		this.words = words;
	}

	public Word getWord(String text/* , int page */) {
		return this.words.get(text);
	}

	public String getPhrase(int page, int start, int stop) {
		String str = "";
		if (start < 0) {
			start = 0;
		}
		Entry<String, Word> iter = null;
		Set<Entry<String, Word>> c = this.words.entrySet();
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
				key = value.next.get(i).toString();
				value = this.words.get(value.next.get(i));
				if (key == null || value == null) {
					return str;
				}
				str = str.concat(" " + key);
			}
			return str;
		}
		return "";
	}

	public HashMap<String, Word> getWords() {
		return words;
	}

	public void setWords(HashMap<String, Word> words, int pages) {
		this.words = words;
		this.pages = pages;
	}
	
	public void addWord(Word w, String s){
		this.words.put(s, w);
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}


	public int getPageCount() {
		return this.pages;
	}

	public long getSize() {
		long size = 0;
		Set<String> c = this.words.keySet();
		for (String s : c) {
			byte[] bytes = s.getBytes();
			size += bytes.length;
		}
		Collection<Word> p = this.words.values();
		for (Word w : p) {
			size += w.getSize();
		}

		return size;
	}

}
