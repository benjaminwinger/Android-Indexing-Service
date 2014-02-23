

package com.bmw.android.indexservice;

import com.bmw.android.indexdata.PageResult;

interface BSearchService1_0 {
	// VERSION 1.0
	/**
	 * Used to search file contents
	 * @param 	doc - the name of the document that should be searched. This allows metadata 
	 *				for multiple files to be in the search service's memory at once.
	 * 			type - allows the client to specify what type of results it wants to receive
	 * 			text - the search query
	 * 			numHits - the number of results the client wants to receive
	 * 			page - the starting page for results (if results end up on a page
	 *				 before this page they are pushed to the end of the returned list)
	 * @return a list containing the terms found that matched the query and what page of the document they appear on.
	 */
	PageResult[] find(String doc, int type, String text, int numHits, int page);
	
	/**
	 * used to send file contents to the indexing service. Because of the limitations of 
	 * the service communicsation system the information may have to be sent in chunks as
	 * there can only be a maximum of about 1MB in the buffer at a time (which is shared 
	 * among all applications). The client class sends data in chunks that do not exceed 256KB,
	 * @param 	filePath - the location of the file to be built; used by the indexer to identify the file
	 *			text - the text to be added to the index
	 *			page - the page upon which the chunk of the file that is being transferred starts. 
	 *					It is a double to allow the transfer of parts of a single page if the page is too large
	 *			maxPage - the total number of pages in the entire file
	 * @return 	0 if index was built successfully; 
	 * 			1 if the file lock was in place due to another build operation being in progress;
	 *			2 if the Service is still waiting for the rest of the pages
	 *			-1 on error
	 */
	int buildIndex(String filePath, in List<String> text, double page, int maxPage);
	
	/**
	 * Tells the indexer to load a file's metadata into memory for use in searches.
	 * @param filePath - the location of the file to prepare; is also the identifier for the file's data in the index
	 * @return 0 if the file exists in the index and was not already loaded; 
	 *	 			1 if the file was already loaded; 
	 *			2 if the file was not loaded and does not exist in the index; 
	 *			-1 if there was an error
	 */
	int load(String filePath);
	
	/**
	 * Tells the indexer to unload a file's metadata from memory as it will not be used in future searches.
	 * @param filePath - the location of the file; used to identify which file should be unloaded
	 * @return true if the file exists in the index; false otherwise
	 */
	boolean unload(String filePath);	
}
