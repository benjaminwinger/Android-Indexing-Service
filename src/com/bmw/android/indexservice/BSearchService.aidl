package com.bmw.android.indexservice;

interface BSearchService {

	boolean[] find(String text);
	void buildIndex(String filePath, in List<String> text, int page, int maxPage);
	boolean load(String filePath);
	
}
