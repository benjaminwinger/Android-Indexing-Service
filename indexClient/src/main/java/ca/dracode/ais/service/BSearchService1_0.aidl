

package ca.dracode.ais.service;

import ca.dracode.ais.indexdata.PageResult;

/*
 * 	BSearchService1_0.aidl
 * 
 * 	Interface definition file for the SearchService
 * 	The client library uses this to interface with the search service
 * 	This file should ideally be modified as little as possible so that there are
 * 		not incompatibility problems with applications using older versions of the service
 *	When it is upgraded, the older version must continue to be supported
 *
 *
 */

interface BSearchService1_0 {
	// VERSION 1.0
	/**
	 * Used to search file contents
	 * @param 	doc - the name of the document that should be searched. This allows metadata
	 *				for multiple files to be in the search service's memory at once.
	 * 			type - allows the client to specify what type of results it wants to receive
	 * 			text - the search term
	 * 			numHits - the maximum number of results to return, a value of -1 means no limit
	 *          resultSet - specifies the result set number. Set 0 returns results numbered 0 ..
	 *              (numHits - 1). If this number is larger than 0 and the available number of  hits
	 *              is greater than numHits, the function will return results numbered from
	 *              (numHits * resultSet) .. (lesser of (available hits) and (numHits * resultSet +
	 *               numHits - 1)). Ignored if numHits is -1
	 * 			page - the starting page for results (if results end up on a page
	 *				 before this page they are pushed to the end of the returned list)
	 * @return a list containing the terms found that matched the query and what page of the document they appear on.
	 */
	PageResult[] find(String doc, int type, String text, int numHits, int resultSet, int page);

    /**
     * Used to search the contents of multiple files
     * @param 	docs - A list containing the names of the documents that should be searched. This allows metadata
     *				for multiple files to be in the search service's memory at once. An empty list will
     *              cause the search service to search all files on the device
     *              Directories can also be included for search of their contents
     * 			type - allows the client to specify what type of results it wants to receive
     * 			text - the search term
     * 			numHits - the maximum number of results to return per file, a value of -1 means no limit
     *          resultSet - specifies the result set number. Set 0 returns results numbered 0 ..
     *              (numHits - 1). If this number is larger than 0 and the available number of  hits
     *              is greater than numHits, the function will return results numbered from
     *              (numHits * resultSet) .. (lesser of (available hits) and (numHits * resultSet +
     *               numHits - 1)). Ignored if numHits is -1
     * @return a list containing the terms found that matched the query and what page of the document they appear on.
     */
    PageResult[] findIn(inout List<String> docs, int type, String text, int numHits, int resultSet);

    /**
     *  Used to search for file names
     * @param   dir - the root directory for the search.
     *          type - allows the client to specify how to filter the files
     *          text - the search term
     *          numHits - the maximum number of results to return, a value of -1 means no limit
     *          resultSet - specifies the result set number. Set 0 returns results numbered 0 ..
     *              (numHits - 1). If this number is larger than 0 and the available number of  hits
     *              is greater than numHits, the function will return results numbered from
     *              (numHits * resultSet) .. (lesser of (available hits) and (numHits * resultSet +
     *               numHits - 1)). Ignored if numHits is -1
    */
	List<String> findName(inout List<String> docs, int type, String text, int numHits,
	int resultSet);
	
	/**
	 * used to send file contents to the indexing service. Because of the limitations of 
	 * the service communicsation system the information may have to be sent in chunks as
	 * there can only be a maximum of about 1MB in the buffer at a time (which is shared 
	 * among all applications). The client class sends data in chunks that do not exceed 256KB,
	 * currently pages cannot exceed 256KB as the data transfer will fail
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
	 * The function can be called multiple times to load several files. Files remain loaded until the unload
	 * function is called. Please make sure to call unload when you are finished with the document.
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

	/**
	 *  Tells the search service to cancel any searches that are currently running
	*/
	boolean interrupt();
}
