/*******************************************************************************
 * Copyright 2014 Benjamin Winger.
 *
 * This file is part of Android Indexing Service.
 *
 * Android Indexing Service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Android Indexing Service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Android Indexing Service.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package ca.dracode.ais.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import ca.dracode.ais.indexdata.PageResult;
import ca.dracode.ais.indexer.FileIndexer;
import ca.dracode.ais.indexer.FileSearcher;

/*
 * 	SearchService.java
 * 
 * 	Service accessed by the client library with functions to search through the index
 *
 * 	Make the indexer search from disk for multi-file searches and search from memory
 * 	for single file searches (load file into memory in load function).
 */

public class SearchService extends Service {
    private static final String TAG = "ca.dracode.ais.service.SearchService";
    private final BSearchService1_0.Stub mBinder = new BSearchService1_0.Stub() {
	    public PageResult[] find(String doc, int type, String text, int numHits, int set, int page){
		    return sm.find(text, doc, numHits, type, set, page);
	    }

	    /**
	     * Used to search the contents of multiple files
	     * @param 	docs - A list containing the names of the documents that should be searched. This allows metadata
	     *				for multiple files to be in the search service's memory at once. An empty list will
	     *              cause the search service to search all files on the device
	     *              Directories can also be included for search of their contents
	     * 	        type - allows the client to specify what type of results it wants to receive
	     *      	text - the search term
	     *      	numHits - the maximum number of results to return per file, a value of -1 means no limit
	     * @return a list containing the terms found that matched the query and what page of the document they appear on.
	     */
	    public PageResult[] findIn(List<String> docs, int type, String text, int numHits, int set){
		    return sm.findIn(text, docs, numHits, set, type);
	    }

	    /**
	     *  Used to search for file names
	     * @param   docs - the root directory for the search.
	     *          type - allows the client to specify how to filter the files
	     *          text - the search term
	     *          numHits - the maximum number of results to return
	     */
	    public List<String> findName(List<String> docs, int type, String text, int numHits,
                                     int set){
			return sm.findName(text, docs, numHits, set, type);
	    }

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
	    public int buildIndex(String filePath, List<String> text, double page, int maxPage){
		    return SearchService.this.buildIndex(filePath, text, page, maxPage);
	    }

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
	    public int load(String filePath){
		    return SearchService.this.load(filePath);
	    }

	    /**
	     * Tells the indexer to unload a file's metadata from memory as it will not be used in future searches.
	     * @param filePath - the location of the file; used to identify which file should be unloaded
	     * @return true if the file exists in the index; false otherwise
	     */
	    public boolean unload(String filePath){
		    return SearchService.this.unload(filePath);
	    }

	    /**
	     *  Tells the search service to cancel any searches that are currently running
	     */
	    public boolean interrupt(){
		    return sm.interrupt();
	    }
    };
    private SearchManager sm;

    // private final IBinder mBinder = new LocalBinder();
    private HashMap<String, SearchData> data;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.sm = new SearchManager();
        this.data = new HashMap<String, SearchData>();
    }

    public int buildIndex(String filePath, List<String> text, double page,
                          int maxPage) {
        File indexDirFile = new File(FileIndexer.getRootStorageDir());
        File[] dirContents = indexDirFile.listFiles();
        if (dirContents != null) {
            for (File dirContent : dirContents) {
                if (dirContent.getName().equals("write.lock")) {
                    return 1;
                }
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
                indexer.buildIndex(tmpData.text, new File(filePath));
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
        if (this.data.containsKey(filePath)) {
            return 1;
        }
        SearchData tmpData = new SearchData();
        Document tmp;
        if (SearchService.this.sm.searcher == null) {
            Log.e(TAG, "Searcher is null");
            return -1;
        }
        if ((tmp = this.sm.searcher.getMetaFile(filePath)) != null) {
            try {
                IndexableField f = tmp.getField("pages");
                if (f == null) {
                    Log.e(TAG,
                            "Cannot find pages in metafile: " + tmp.toString());
                    return -1;
                } else {
                    tmpData.pages = f.numericValue().intValue();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error", e);
                return -1;
            }
            this.data.put(filePath, tmpData);
            return 0;
        } else {
            return 2;
        }

    }

    public boolean unload(String path) {
        return this.data.remove(path) != null;
    }

    private class SearchManager {
        boolean found;
        boolean finishedLoading = false;
	    private FileSearcher searcher;

        public SearchManager() {
			this.searcher = new FileSearcher();
        }

	    private boolean interrupt(){
		    return searcher.interrupt();
	    }

	    private List<String> findName(String term, List<String> directory, int numHits, int set,
                                      int type){
		    return this.searcher.findName(term, "path", directory, "path", numHits, set, type);
	    }

	    private PageResult[] findIn(String term, List<String> documents, int numHits, int set,
                                    int type){
		    return this.searcher.findIn(term, "text", documents, "path", numHits, set, type);
	    }

        private PageResult[] find(String term, String constrainValue, int maxResults, int set,
                                  int type, int page) {
            /**
             * TODO - Preload information about the index in the load function for use here
             * **/
			Log.i(TAG, "Received request to search for: " + term);
            return this.searcher.find(term, "text",
                    constrainValue, "path", maxResults, set, type, page);
        }
    }

}
