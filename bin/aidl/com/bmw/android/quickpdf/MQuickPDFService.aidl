package com.bmw.android.quickpdf;

interface MQuickPDFService {
	/**
	 * Load libraries to access the file here so that it only has to be done once.
	 * This will always be the first function called 
	 * @param path - the path of the file to be loaded
	 */
	void loadFile(String path);
	
	/**
	 * The indexer will query the contents of each page one at a time.
	 * Sending all of the information at once is too large to transfer in some files.
	 * @param page - the page of the file to be returned
	 * @return - A string containing all of the words on the page
	 */
	String getWordsForPage(int page);
	
	/**
	 * 
	 * @return - the number of pages in the file specified at loadFile(String path)
	 */
	int getPageCount();
}
