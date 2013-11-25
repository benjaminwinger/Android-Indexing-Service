package com.bmw.android.indexservice;

interface BSearchService {

	boolean[] find(String text);
	boolean buildIndex(String filePath, in List<String> text);
	boolean load(String filePath);
	
}
