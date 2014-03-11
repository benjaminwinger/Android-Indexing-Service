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

package com.bmw.android.indexservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.bmw.android.androidindexer.FileIndexer;
import com.bmw.android.androidindexer.FileSearcher;
import com.bmw.android.indexdata.PageResult;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/*
 * 	SearchService.java
 * 
 * 	Service accessed by the client library with functions to search through the index
 * 
 * 	v0.3
 * 	Make the indexer search from disk for multi-file searches and search from memory
 * 	for single file searches (load file into memory in load function).
 */

public class SearchService extends Service {
    private static final String TAG = "com.bmw.android.indexservice.SearchService";
    private final BSearchService1_0.Stub mBinder = new BSearchService1_0.Stub() {
        public PageResult[] find(String doc, int type, String text,
                                 int numHits, int page) {
            return SearchService.this.sm.find(doc, type, text, numHits, page);
        }

        public int buildIndex(String filePath, List<String> text, double page,
                              int maxPage) {
            return SearchService.this.buildIndex(filePath, text, page, maxPage);
        }

        public int load(final String filePath) {
            return SearchService.this.load(filePath);
        }

        public boolean unload(final String filePath) {
            return SearchService.this.unload(filePath);
        }
    };
    private SearchManager sm;
    private FileSearcher searcher;

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
        this.searcher = new FileSearcher();
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
                indexer.buildIndex(tmpData.text, filePath);
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
        if (SearchService.this.searcher == null) {
            Log.e(TAG, "Searcher is null");
            return -1;
        }
        if ((tmp = this.searcher.getMetaFile(filePath)) != null) {
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

        public SearchManager() {

        }

        private PageResult[] find(String doc, int type, String text,
                                  int numHits, int page) {
            /**
             * TODO - Utilize the remaining function arguments Doc - the
             * document to be searched; should replace local variable path if
             * the document has already been loaded numHits - the maximum number
             * of results to load into the PageResult array page - the page to
             * start on if only loading a certain number of results
             * **/
            SearchData tmpData = SearchService.this.data.get(doc);
            return SearchService.this.searcher.find(type, "text",
                    text, tmpData.pages, "path", doc, 10);
        }
    }

}
