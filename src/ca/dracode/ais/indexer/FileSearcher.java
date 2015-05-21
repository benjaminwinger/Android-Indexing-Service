/*
 * Copyright 2014 Dracode Software.
 *
 * This file is part of AIS.
 *
 * AIS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

package ca.dracode.ais.indexer;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import ca.dracode.ais.alarm.AutoStart;
import ca.dracode.ais.indexdata.SearchResult;

/**
 * FileSearcher.java
 *
 * Contains functions for searching the lucene index
 * @author Benjamin Winger
 */
public class FileSearcher {

    // NOTE - Standard searches take significantly longer than boolean ones due to their use of the highlighter
    //          which can make them take a long time to finish for common terms. They will be faster when the
    //          number of results is actually restricted but boolean searches should be recommended where speed is
    //          important
    /**
     * Searches for results purely based on whether or not the term occurs in the result.
     * Individual pages will be ordered by page number. If multiple files are searched at once,
     * the files will be ordered alphabetically.
     */
    public static final int QUERY_BOOLEAN = 0;
    /**
     * Searches for results based on their relevance to the search term.
     * All results will be ordered by relevance.
     */
    public static final int QUERY_STANDARD = 1;


    private final String TAG = "ca.dracode.ais.androidindexer.FileSearcher";
    private IndexSearcher indexSearcher;
    private int interrupt = -1;

    public FileSearcher(Context c) {
        IndexReader indexReader;
        IndexSearcher indexSearcher = null;
        try {
            File indexDirFile = new File(FileIndexer.getRootStorageDir());
            Directory tmpDir = FSDirectory.open(indexDirFile);
            indexReader = DirectoryReader.open(tmpDir);
            indexSearcher = new IndexSearcher(indexReader);
        } catch(IOException ioe) {
            Log.e(TAG, "Error", ioe);
        }

        this.indexSearcher = indexSearcher;
    }

    /**
     * Returns the first document that matches the given field/value combination
     * @param field The field to check
     * @param value The value to check with
     * @return the Document found by the IndexSearcher
     * @throws IOException
     */
    public Document getDocument(String field, String value) throws IOException {
        //Log.i(TAG, "Checking for existance of " + value);
        BooleanQuery qry = new BooleanQuery();
        qry.add(new TermQuery(new Term(field, value)), BooleanClause.Occur.MUST);
        if(this.indexSearcher != null) {
            ScoreDoc[] hits;
            hits = indexSearcher.search(qry, 1).scoreDocs;
            if(hits != null && hits.length > 0) {
                return indexSearcher.doc(hits[0].doc);
            }
        }
        return null;
    }

    /**
     * Searches the index for a metadata Document with "id" matching the passed id
     * @param id The "id" of the metadata Document
     * @return The metadata Document matching id or null if it does not exist
     */
    public Document getMetaFile(String id) {
        BooleanQuery qry = new BooleanQuery();
        qry.add(new TermQuery(new Term("id", id + ":meta")),
                BooleanClause.Occur.MUST);
        ScoreDoc[] hits = null;
        try {
            hits = indexSearcher.search(qry, 1).scoreDocs;
        } catch(IOException e) {
            Log.e(TAG, "Error ", e);
        }
        if(hits == null || hits.length == 0) {
            return null;
        }
        Document doc = null;
        try {
            doc = indexSearcher.doc(hits[0].doc);
        } catch(IOException e) {
            Log.e(TAG, "Error ", e);
        }
        return doc;
    }

    /**
     * Searches for matches in a file's path and name
     * <p>
     *     For example, a search with the term "Foo" and the constrainValues {"/Bar", "/Stool"}
     *     will return files with a path related to "Foo" only from the directories or
     *     subdirectories or "/Bar" and "/Stool"
     * </p>
     * @param id Identifier for the instance of ClientService that spawned the search
     * @param term The search term for choosing and ranking results
     * @param field The field to search, i.e. "path"
     * @param constrainValues The paths to which to constrain the search
     * @param constrainField The field used to constrain searches
     * @param maxResults The maximum number of results that will be returned
     * @param set The set number, e.g., searching set 0 returns the first n results,
     *            searching set 1 returns the 2nd n results. Set must be positive.
     * @param type The type of the search, one of QUERY_BOOLEAN or QUERY_STANDARD
     * @return A list containing all of the paths the searcher found, sorted by relevance
     */
    public ArrayList<String> findName(int id, String term, String field,
                                      List<String> constrainValues,
                                      String constrainField, int maxResults, int set, int type) {
        if(this.interrupt == id) {
            this.interrupt = -1;
            return null;
        }
        Query qry = this.getQuery(term, field, type);
        if(qry != null){
            Filter filter = this.getFilter(constrainField, constrainValues, type, -1, -1);
            ScoreDoc[] hits = null;
            try {
                hits = indexSearcher.search(qry, filter, maxResults + maxResults * set).scoreDocs;
            } catch(IOException e) {
                Log.e(TAG, "Error ", e);
            }
            if(this.interrupt == id) {
                this.interrupt = -1;
                return null;
            }
            ArrayList<String> docs = this.getDocPaths(maxResults, set, hits);
            Log.i(TAG, "Found instance of term in " + docs.size() + " documents");
            return docs;
        } else {
            Log.e(TAG, "Query Type: " + type + " not recognised");
            return new ArrayList<String>();
        }
    }

    /**
     * Searches for matches in the contents of multiple files
     * <p>
     *     For example, a search with the term "Foo" and the constrainValues {"/Bar", "/Stool"}
     *     will return pages with contents related to "Foo" only from the directories or
     *     subdirectories or "/Bar" and "/Stool"
     * </p>
     * @param id Identifier for the instance of ClientService that spawned the search
     * @param term The search term for choosing and ranking results
     * @param field The field to search, i.e. "contents"
     * @param constrainValues The paths to which to constrain the search
     * @param constrainField The field used to constrain searches
     * @param maxResults The maximum number of results that will be returned
     * @param set The set number, e.g., searching set 0 returns the first n results,
     *            searching set 1 returns the 2nd n results. Set must be positive
     * @param type The type of the search, one of QUERY_BOOLEAN or QUERY_STANDARD
     * @return A SearchResult containing the results sorted by relevance and page
     */
    public SearchResult findInFiles(int id, String term, String field,
                                     List<String> constrainValues,
                                                                                   String constrainField, int maxResults, int set, int type) {
        if(this.interrupt == id) {
            this.interrupt = -1;
            return null;
        }
        Query qry = this.getQuery(term, field, type);
        if(qry != null){
            //Filter filter = this.getFilter(constrainField, constrainValues, type, -1, -1);
            ScoreDoc[] hits = null;
            try {
                if(type == QUERY_BOOLEAN){
                    Sort sort = new Sort(new SortField("path", SortField.Type.STRING),
                            new SortField("page", SortField.Type.INT));
                    hits = indexSearcher.search(qry, null, maxResults *
                            set + maxResults, sort).scoreDocs;
                } else {
                    hits = indexSearcher.search(qry, null, maxResults *
                            set + maxResults).scoreDocs;
                }

            } catch(IOException e) {
                Log.e(TAG, "Error ", e);
            }
            if(this.interrupt == id) {
                this.interrupt = -1;
                return null;
            }
            List<Document> docs = this.getDocs(maxResults, set, hits);
            Log.i(TAG, "Found instance of term in " + docs.size() + " documents");
            return this.getHighlightedResults(docs, qry, type, term, maxResults);
        } else {
            Log.e(TAG, "Query Type: " + type + " not recognised");
            return null;
        }
    }

    /**
     * Searches for matches in the contents of a single file
     * <p>
     *     For example, a search with the term "Foo" and the constrainValue "/Bar.txt"
     *     will return pages with contents related to "Foo" only from inside the file "/Bar.txt"
     * </p>
     * @param id Identifier for the instance of ClientService that spawned the search
     * @param term The search term for choosing and ranking results
     * @param field The field to search, i.e. "contents"
     * @param constrainValue The path to which to constrain the search
     * @param constrainField The field used to constrain searches
     * @param maxResults The maximum number of results that will be returned
     * @param set The set number, e.g., searching set 0 returns the first n results,
     *            searching set 1 returns the 2nd n results. A negative set can be used to search
     *            backwards from a page.
     * @param type The type of the search, one of QUERY_BOOLEAN or QUERY_STANDARD
     * @return A SearchResult containing the results sorted by relevance and page
     */
    public SearchResult findInFile(int id, String term, String field, String constrainValue,
                                                                                  String constrainField, int maxResults, int set, int type,
                                                                                  final int page) {

        Query qry = this.getQuery(term, field, type);
        Log.i(TAG, "Query: " + term + " " + field + " " + type + " " + constrainValue);
        if(this.interrupt == id) {
            this.interrupt = -1;
            return null;
        }
        if(qry != null){
            String[] values = {constrainValue};
            Filter filter;
            ScoreDoc[] hits = null;
            try {
                Log.i(TAG, "Searching...");
                Sort sort;
                if(type == QUERY_STANDARD){
                    sort = new Sort();
                    filter = this.getFilter(constrainField, Arrays.asList(values), type, page,
                            Integer.MAX_VALUE);
                    hits = indexSearcher.search(qry, filter, maxResults * set + maxResults,
                            sort).scoreDocs;
                } else {
                    if(set >= 0) {
                        sort = new Sort(new SortField("page", SortField.Type.INT));
                        filter = this.getFilter(constrainField, Arrays.asList(values), type, page,
                                Integer.MAX_VALUE);
                        hits = indexSearcher.search(qry, filter, maxResults * set + maxResults,
                                sort).scoreDocs;
                        if(hits.length < maxResults) {
                            filter = this.getFilter(constrainField, Arrays.asList(values), type, 0,
                                    page - 1);
                            hits = concat(hits, indexSearcher.search(qry, filter,
                                    maxResults,
                                    sort).scoreDocs);
                        }
                    } else {
                        sort = new Sort(new SortField("page", SortField.Type.INT, true));
                        filter = this.getFilter(constrainField, Arrays.asList(values), type, 0,
                                page - 1);
                        hits = indexSearcher.search(qry, filter, Integer.MAX_VALUE,
                                sort).scoreDocs;
                        if(hits.length < maxResults){
                            filter = this.getFilter(constrainField, Arrays.asList(values), type, page,
                                    Integer.MAX_VALUE);
                            hits = concat(hits, indexSearcher.search(qry, filter,
                                    maxResults - hits.length,
                                    sort).scoreDocs);
                        } else {
                            ScoreDoc[] tmp = hits;
                            hits = new ScoreDoc[maxResults * -(set + 1) + maxResults];
                            System.arraycopy(tmp, 0, hits, 0, maxResults * -(set + 1) + maxResults);
                        }
                    }
                }
            } catch(IOException e) {
                Log.e(TAG, "Error ", e);
            }
            if(this.interrupt == id) {
                this.interrupt = -1;
                return null;
            }
            if(hits != null) {
                Log.i(TAG, "Found instance of term in " + hits.length + " documents");
                return this.getHighlightedResults(this.getDocs(maxResults, set, hits), qry,
                        type,
                        term, maxResults);
            }
        } else {
            Log.e(TAG, "Query Type: " + type + " not recognised");
            return null;
        }
        return null;
    }

    ScoreDoc[] concat(ScoreDoc[] A, ScoreDoc[] B) {
        int aLen = A.length;
        int bLen = B.length;
        ScoreDoc[] C= new ScoreDoc[aLen+bLen];
        System.arraycopy(A, 0, C, 0, aLen);
        System.arraycopy(B, 0, C, aLen, bLen);
        return C;
    }

    /**
     * Creates a query based on the given term field and type
     * @param term Search Term for the query
     * @param field Document Field for the Query which the term is matched against
     * @param type The type of query to be created, either QUERY_BOOLEAN, or QUERY_STANDARD,
     * @return a query for the given field and term using either a BooleanQuery with a
     * WildcardQuery for the term or a Query built from a QueryParser and SimpleAnalyzer
     */
    private Query getQuery(String term, String field, int type) {
        Query qry = null;
        if(type == FileSearcher.QUERY_BOOLEAN) {
            qry = new BooleanQuery();
            String[] words = term.split(" ");
            ((BooleanQuery) qry).add(new WildcardQuery(new Term(field, "*" + words[0])),
                    BooleanClause.Occur.MUST);
            if(words.length > 1) {
                for(int i = 1; i < words.length - 1; i++) {
                    ((BooleanQuery) qry).add(new WildcardQuery(new Term(field, words[i])),
                            BooleanClause.Occur.MUST);
                }
                ((BooleanQuery) qry).add(new WildcardQuery(new Term(field,
                                words[words.length - 1] + "*")),
                        BooleanClause.Occur.MUST);
            }
        } else if(type == FileSearcher.QUERY_STANDARD) {
            try {
                qry = new QueryParser(Version.LUCENE_47, field,
                        new SimpleAnalyzer(Version.LUCENE_47)).parse(term);
            } catch(ParseException e) {
                e.printStackTrace();
            }
        }
        return qry;
    }

    /**
     * Creates a directory filter; also filters a range of pages
     * @param constrainField The field that contains the directory info
     * @param constrainValues The directories to which the filters shold limit
     * @return The created filter
     */
    private Filter getFilter(String constrainField, List<String> constrainValues,
                             int type, int startPage,
                             int endPage){
        BooleanQuery cqry = new BooleanQuery();
        if(constrainValues.size() == 1){
            cqry.add(new TermQuery(new Term(constrainField, constrainValues.get(0))),
                    BooleanClause.Occur.MUST);
        } else {
            for(String s : constrainValues) {
                cqry.add(new TermQuery(new Term(constrainField, s)),
                        BooleanClause.Occur.SHOULD);
            }
        }
        if(type == FileSearcher.QUERY_BOOLEAN && startPage != -1 && endPage != -1) {
            cqry.add(NumericRangeQuery.newIntRange("page", startPage, endPage, true, true),
                    BooleanClause.Occur.MUST);
        }
        return new QueryWrapperFilter(cqry);
    }

    /**
     * Takes a list of Documents and highlights information relevant to a given Query
     * @param docs The documents to highlight
     * @param qry The query used to highlight the documents
     * @param type The type of the search, one of QUERY_BOOLEAN,
     *             which just notes the page on which the term exists or QUERY_STANDARD,
     *             which gives highlighted fragments and the page on which they exist.
     * @param term The term that created the query
     * @param maxResults The maximum number of results that will be returned
     * @return A SearchResult containing the results sorted by relevance and page
     */
    private SearchResult getHighlightedResults(List<Document> docs, Query qry, int type,
                                               String term, int maxResults){
        try {
            int numResults = 0;
            LinkedHashMap<String, LinkedHashMap<Integer, List<String>>> results = new LinkedHashMap<String, LinkedHashMap<Integer, List<String>>>();
            for(int i = 0; i < docs.size() && numResults < maxResults; i++) {
                Document d = docs.get(i);
                int docPage = Integer.parseInt(d.get("page"));
                String name = d.get("path");
                LinkedHashMap<Integer, List<String>> docResult = results.get(name);
                if(docResult == null){
                    docResult = new LinkedHashMap<Integer,
                            List<String>>();
                    results.put(name, docResult);
                }
                if(type != FileSearcher.QUERY_BOOLEAN) {
                    String contents = d.get("text");
                    Highlighter highlighter = new Highlighter(new QueryScorer(qry));

                    String[] frag = null;
                    try {
                        frag = highlighter.getBestFragments(new SimpleAnalyzer(Version.LUCENE_47), "text", contents, maxResults - numResults);
                        numResults += frag.length;
                    } catch(IOException e) {
                        Log.e(TAG, "Error while reading index", e);
                    } catch(InvalidTokenOffsetsException e) {
                        Log.e(TAG, "Error while highlighting", e);
                    }
                    if(frag != null) {
                        Log.i(TAG, "Frags: " + frag.length + " " + frag + " " + frag[0]);
                    }
                    ArrayList<String> tmpList = new ArrayList<String>(Arrays
                            .asList(frag != null ? frag : new String[0]));
                    Log.i(TAG, "list " + tmpList.getClass().getName());
                    docResult.put(docPage, tmpList);
                } else {
                    ArrayList<String> tmp = new ArrayList<String>();
                    tmp.add(term);
                    docResult.put(docPage, tmp);
                }

            }
            Log.i(TAG, "" + results.size());
            return new SearchResult(results);
        } catch(Exception e) {
            Log.e("TAG", "Error while Highlighting", e);
            return null;
        }
    }

    /**
     * Takes an array of ScoreDoc and turns it into the relevant number of Documents
     * @param maxResults The maximum number of documents that will be parsed
     * @param set The set number, e.g., searching set 0 returns the first n results,
     *            searching set 1 returns the 2nd n results
     * @param hits The ScoreDoc to be parsed
     * @return The list of documents that is maxResults long and skips set*maxResults results
     */
    private ArrayList<Document> getDocs(int maxResults, int set, ScoreDoc[] hits){
        ArrayList<Document> docs = new ArrayList<Document>();
        int max = maxResults;
        if(max > hits.length)max = hits.length;
        Log.i(TAG, "Max: " + maxResults + " Set: " + set);
        if(set >= 0) {
            for(int i = 0; i < hits.length;
                i++) {
                try {
                    Document tmp = indexSearcher.doc(hits[i].doc);
                    docs.add(tmp);
                } catch(IOException e) {
                    Log.e(TAG, "Error ", e);
                }
            }
        } else {
            for(int i = 0; i < hits.length && i < (max * -(set + 1) +
                    max);
                i++) {
                try {
                    Document tmp = indexSearcher.doc(hits[i].doc);
                    docs.add(tmp);
                } catch(IOException e) {
                    Log.e(TAG, "Error ", e);
                }
            }
        }
        Log.i(TAG, "doc size" + docs.size());
        return docs;
    }

    /**
     * Takes an array of ScoreDoc and turns it into the relevant number of Document paths
     * @param maxResults The maximum number of paths that will be parsed
     * @param set The set number, e.g., searching set 0 returns the first n results,
     *            searching set 1 returns the 2nd n results
     * @param hits The ScoreDoc to be parsed
     * @return The list of paths that is maxResults long and skips set*maxResults results
     */
    private ArrayList<String> getDocPaths(int maxResults, int set, ScoreDoc[] hits){
        ArrayList<String> docs = new ArrayList<String>();
        for(int i = maxResults * set; i < hits.length && i < maxResults * set + maxResults; i++) {
            try{
                Document tmp = indexSearcher.doc(hits[i].doc);
                docs.add(tmp.get("path"));
            } catch(IOException e) {
                Log.e(TAG, "Error ", e);
            }
        }
        return docs;
    }

    /**
     * Calls for the any current searches to be inturrupted
     * @return false if the interrupt flag was already set; otherwise true
     */
    public boolean interrupt(int id) {
        // TODO - Must become path dependent so that interrupt calls do not interrupt searches
        // from other applications
        int tmp = this.interrupt;
        this.interrupt = id;
        return !(tmp == id);
    }
}
