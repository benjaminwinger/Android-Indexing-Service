/*
 * Copyright 2014 Dracode Software.
 *
 * This file is part of AIS.
 *
 * AIS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AIS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AIS.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.dracode.ais.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import ca.dracode.ais.indexdata.SearchResult;
import ca.dracode.ais.indexer.FileSearcher;

/**
 * 	SearchService.java
 *
 * 	Service accessed by the client library with functions to search through the index
 */

// TODO - Make the indexer search from disk for multi-file searches and search from memory
// for single file searches (create new index in RAMDirectory and add appropriate documents in
// the load function).

public class SearchService extends Service implements IndexService.IndexCallback {
    private static final String TAG = "ca.dracode.ais.service.SearchService";
    int currentId = 0;
    HashMap<File, Integer> builtIndexes;
    private final BSearchService1_0.Stub mBinder = new BSearchService1_0.Stub() {
        public SearchResult find(int id, String doc, int type, String text, int numHits, int set,
                                 int page) {
            return sm.find(id, text, doc, numHits, type, set, page);
        }

        public SearchResult findIn(int id, List<String> docs, int type, String text, int numHits,
                                   int set) {
            return sm.findIn(id, text, docs, numHits, set, type);
        }

        public List<String> findName(int id, List<String> docs, int type, String text, int numHits,
                                     int set) {
            return sm.findName(id, text, docs, numHits, set, type);
        }

        public int buildIndex(int id, String filePath) {
            return SearchService.this.buildIndex(id, filePath);
        }

        public int load(String filePath) {
            return SearchService.this.load(filePath);
        }

        public boolean unload(String filePath) {
            return SearchService.this.unload(filePath);
        }

        public boolean interrupt(int id) {
            return sm.interrupt(id);
        }

        public int getId(){
            return ++currentId;
        }
    };
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((IndexService.LocalBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
        }
    };
    private SearchManager sm;
    private IndexService mBoundService;
    private boolean mIsBound = false;

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
        this.builtIndexes = new HashMap<File, Integer>();
    }

    private void doBindService() {
        bindService(new Intent(SearchService.this,
                IndexService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void doUnbindService() {
        if(mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    /**
     * Tells the indexer to try to build the given file
     * @param filePath - the location of the file to be built; used by the indexer to identify the file
     * @return 0 if index was built successfully;
     * 			1 if the file lock was in place due to another build operation being in progress;
     *			2 if the Service is still waiting for the rest of the pages
     *			-1 on error
     */
    public int buildIndex(int id, String filePath) {
        if(!mIsBound){
            doBindService();
        }
        while(!mIsBound || mBoundService == null){

        }
        try {
            mBoundService.createIndex(new File(filePath), this);
            mBoundService.stopWhenReady();
            doUnbindService();
            return waitForIndexer(new File(filePath));
        } catch(Exception e){
            Log.e(TAG, "Error: ", e);
        }
        return -1;
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
    public int load(final String filePath) {
        if(this.data.containsKey(filePath)) {
            return 1;
        }
        SearchData tmpData = new SearchData();
        Document tmp;
        if(SearchService.this.sm.searcher == null) {
            Log.e(TAG, "Searcher is null");
            return -1;
        }
        Log.i(TAG, "Loading: " + filePath + " " + new File(filePath).getAbsolutePath());
        if((tmp = this.sm.searcher.getMetaFile(new File(filePath).getAbsolutePath())) != null) {
            try {
                IndexableField f = tmp.getField("pages");
                if(f == null) {
                    Log.e(TAG,
                            "Cannot find pages in metafile: " + tmp.toString());
                    return -1;
                } else {
                    tmpData.pages = f.numericValue().intValue();
                }
            } catch(Exception e) {
                Log.e(TAG, "Error", e);
                return -1;
            }
            this.data.put(filePath, tmpData);
            return 0;
        } else {
            return 2;
        }

    }

    /**
     * Tells the indexer to unload a file's metadata from memory as it will not be used in future searches.
     * @param path - the location of the file; used to identify which file should be unloaded
     * @return true if the file exists in the index; false otherwise
     */
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

        /**
         *  Tells the search service to cancel any searches that are currently running
         */
        private boolean interrupt(int id) {
            return searcher.interrupt(id);
        }

        /**
         *  Used to search for file names
         * @param   directory - A list containing directories to search
         * @param   type - allows the client to specify how to filter the files
         * @param   term - the search term
         * @param   numHits - the maximum number of results to return
         */
        private List<String> findName(int id, String term, List<String> directory, int numHits,
                                      int set,
                                      int type) {
            return this.searcher.findName(id, term, "path", directory, "path", numHits, set, type);
        }

        /**
         * Used to search the contents of multiple files
         * @param    documents - A list containing the names of the documents that should be
         * searched. This allows metadata
         * for multiple files to be in the search service's memory at once. An empty list will
         * cause the search service to search all files on the device
         * Directories can also be included for search of their contents
         * @param type - allows the client to specify what type of results it wants to receive
         * @param term - the search term
         * @param numHits - the maximum number of results to return per file,
         * a value of -1 means no limit
         * @return a list containing the terms found that matched the query and what page of the
         * document they appear on.
         */
        private SearchResult findIn(int id, String term, List<String> documents, int numHits,
                                    int set, int type) {
            return this.searcher.findInFiles(id, term, "text", documents, "path", numHits, set,
                    type);
        }

        private SearchResult find(int id, String term, String constrainValue, int maxResults,
                                  int type, int set, int page) {
            /**
             * TODO - Preload information about the index in the load function for use here
             * **/
            Log.i(TAG, "Received request to search for: " + term);
            return this.searcher.findInFile(id, term, "text",
                    constrainValue, "path", maxResults, set, type, page);
        }
    }

    public int waitForIndexer(File content){
        while(!this.builtIndexes.containsKey(content)){
            try{
                Thread.sleep(5);
            } catch(InterruptedException e){
                Log.e(TAG, "Error", e);
            }
        }
        return this.builtIndexes.get(content);
    }

    public void indexCreated(File content, int retval){
        this.builtIndexes.put(content, retval);
    }
}
