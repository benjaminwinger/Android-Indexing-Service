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

package ca.dracode.ais.indexer;

import android.util.Log;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.FieldCache.IntParser;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparator.NumericComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
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
import java.util.List;

import ca.dracode.ais.indexdata.PageResult;

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
    public static final int QUERY_BOOLEAN = 0;
    public static final int QUERY_STANDARD = 1;


    private final String TAG = "ca.dracode.ais.androidindexer.FileSearcher";
    private IndexSearcher indexSearcher;
    private boolean interrupt = false;

    public FileSearcher() {
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
     * @param term The search term for choosing and ranking results
     * @param field The field to search, i.e. "path"
     * @param constrainValues The paths to which to constrain the search
     * @param constrainField The field used to constrain searches
     * @param maxResults The maximum number of results that will be returned
     * @param set The set number, e.g., searching set 0 returns the first n results,
     *            searching set 1 returns the 2nd n results
     * @param type The type of the search, one of QUERY_BOOLEAN or QUERY_STANDARD
     * @return A list containing all of the paths the searcher found, sorted by relevance
     */
    public List<String> findName(String term, String field, List<String> constrainValues,
                                 String constrainField, int maxResults, int set, int type) {
        Query qry = this.getQuery(term, field, type);
        if(qry != null){
            Filter filter = this.getFilter(constrainField, constrainValues);
            ScoreDoc[] hits = null;
            try {
                hits = indexSearcher.search(qry, filter, maxResults + maxResults * set).scoreDocs;
            } catch(IOException e) {
                Log.e(TAG, "Error ", e);
            }
            if(this.interrupt) {
                this.interrupt = true;
                return null;
            }
            List<String> docs = this.getDocPaths(maxResults, set, hits);
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
     * @param term The search term for choosing and ranking results
     * @param field The field to search, i.e. "contents"
     * @param constrainValues The paths to which to constrain the search
     * @param constrainField The field used to constrain searches
     * @param maxResults The maximum number of results that will be returned
     * @param set The set number, e.g., searching set 0 returns the first n results,
     *            searching set 1 returns the 2nd n results
     * @param type The type of the search, one of QUERY_BOOLEAN or QUERY_STANDARD
     * @return A list of PageResult containing all of the results the searcher found,
     * sorted by relevance
     */
    public PageResult[] findInFiles(String term, String field, List<String> constrainValues,
                               String constrainField, int maxResults, int set, int type) {
        Query qry = this.getQuery(term, field, type);
        if(qry != null){
            Filter filter = this.getFilter(constrainField, constrainValues);
            ScoreDoc[] hits = null;
            try {
                hits = indexSearcher.search(qry, filter, maxResults * set + maxResults).scoreDocs;
            } catch(IOException e) {
                Log.e(TAG, "Error ", e);
            }
            if(this.interrupt) {
                this.interrupt = true;
                return null;
            }
            List<Document> docs = this.getDocs(maxResults, set, hits);
            Log.i(TAG, "Found instance of term in " + docs.size() + " documents");
            return this.getHighlightedResults(docs, qry, type, term, maxResults);
        } else {
            Log.e(TAG, "Query Type: " + type + " not recognised");
            return new PageResult[0];
        }
    }

    /**
     * Searches for matches in the contents of multiple files
     * <p>
     *     For example, a search with the term "Foo" and the constrainValue "/Bar.txt"
     *     will return pages with contents related to "Foo" only from inside the file "/Bar.txt"
     * </p>
     * @param term The search term for choosing and ranking results
     * @param field The field to search, i.e. "contents"
     * @param constrainValue The path to which to constrain the search
     * @param constrainField The field used to constrain searches
     * @param maxResults The maximum number of results that will be returned
     * @param set The set number, e.g., searching set 0 returns the first n results,
     *            searching set 1 returns the 2nd n results
     * @param type The type of the search, one of QUERY_BOOLEAN or QUERY_STANDARD
     * @return A list of PageResult containing all of the results the searcher found,
     * sorted by relevance
     */
    public PageResult[] findInFile(String term, String field, String constrainValue,
                             String constrainField, int maxResults, int set, int type,
                             final int page) {
        Query qry = this.getQuery(term, field, type);
        if(qry != null){
            String[] values = {constrainValue};
            Filter filter = this.getFilter(constrainField, Arrays.asList(values));
            ScoreDoc[] hits = null;
            try {
                Log.i(TAG, "Searching...");
                hits = indexSearcher.search(qry, filter, maxResults * set + maxResults,
                        this.getPagedSort(page)).scoreDocs;
            } catch(IOException e) {
                Log.e(TAG, "Error ", e);
            }
            if(this.interrupt) {
                this.interrupt = true;
                return null;
            }

            Log.i(TAG, "Found instance of term in " + hits.length + " documents");
            return this.getHighlightedResults(this.getDocs(maxResults, set, hits), qry, type,
                    term, maxResults);
        } else {
            Log.e(TAG, "Query Type: " + type + " not recognised");
            return new PageResult[0];
        }
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
        if(this.interrupt) {
            this.interrupt = false;
            return null;
        }
        Query qry = null;
        if(type == FileSearcher.QUERY_BOOLEAN) {
            String[] terms = term.split(" ");
            qry = new BooleanQuery();
            ((BooleanQuery) qry).add(new WildcardQuery(new Term(field, "*" + term + "*")),
                    BooleanClause.Occur.MUST);
        } else if(type == FileSearcher.QUERY_STANDARD) {
            try {
                qry = new QueryParser(Version.LUCENE_47, field,
                        new SimpleAnalyzer(Version.LUCENE_47)).parse(term);
            } catch(ParseException e) {
                e.printStackTrace();
            }
        }
        if(this.interrupt) {
            this.interrupt = false;
            return null;
        }
        return qry;
    }

    /**
     * Creates a directory filter
     * @param constrainField The field that contains the directory info
     * @param constrainValues The directories to which the filters shold limit
     * @return The created filter
     */
    private Filter getFilter(String constrainField, List<String> constrainValues){
        BooleanQuery cqry = new BooleanQuery();
        for(String s : constrainValues) {
            cqry.add(new TermQuery(new Term(constrainField, s)),
                    BooleanClause.Occur.SHOULD);
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
     * @return An array containing a PageResult entry for each result
     */
    private PageResult[] getHighlightedResults(List<Document> docs, Query qry, int type,
                                               String term, int maxResults){
        try {
            int numResults = 0;
            PageResult[] results = new PageResult[docs.size()];
            for(int i = 0; i < docs.size() && numResults < maxResults; i++) {
                if(this.interrupt) {
                    this.interrupt = false;
                    return null;
                }
                Document d = docs.get(i);
                int docPage = Integer.parseInt(d.get("page"));
                String name = d.get("path");
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
                    Log.i(TAG, "Frags: " + frag.length + " " + frag + " " + frag[0]);
                    ArrayList<String> tmpList = new ArrayList<String>(Arrays
                            .asList(frag != null ? frag : new String[0]));
                    Log.i(TAG, "list " + tmpList.getClass().getName());
                    results[i] = (new PageResult(tmpList, docPage, name));
                    Log.i(TAG, "" + results[i]);
                } else {
                    ArrayList<String> tmp = new ArrayList<String>();
                    tmp.add(term);
                    results[i] = (new PageResult(tmp, docPage, name));
                }

            }
            Log.i(TAG, "" + results.length);
            if(this.interrupt) {
                this.interrupt = false;
                return null;
            }
            return results;
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
    private List<Document> getDocs(int maxResults, int set, ScoreDoc[] hits){
        ArrayList<Document> docs = new ArrayList<Document>();
        for(int i = maxResults * set; i < hits.length && i < (maxResults * set + maxResults);
            i++) {
            try {
                Document tmp = indexSearcher.doc(hits[i].doc);
                docs.add(tmp);
            } catch(IOException e) {
                Log.e(TAG, "Error ", e);
            }
        }
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
    private List<String> getDocPaths(int maxResults, int set, ScoreDoc[] hits){
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
    public boolean interrupt() {
        // TODO - Must become path dependent so that interrupt calls do not interrupt searches
        // from other applications
        boolean tmp = this.interrupt;
        this.interrupt = true;
        return !tmp;
    }

    /**
     * Creates a sort that splits results around a given page
     * @param page The page that will show first in the results,
     * @return A Sort object that splits results around the given page
     */
    private Sort getPagedSort(final int page){
        return new Sort(new SortField("page", new FieldComparatorSource() {
            @Override
            public FieldComparator<?> newComparator(String fieldname, int numhits,
                                                    int sortpos,
                                                    boolean reversed) throws
                    IOException {
                return new PagedIntComparator(numhits, fieldname, null,
                        null) {
                    public int compare(int slot1, int slot2) {
                        final int v1 = this.values[slot1];
                        final int v2 = this.values[slot2];
                        if(v1 < page && v2 >= page) {
                            return 1;
                        } else if(v1 >= page && v2 < page) {
                            return -1;
                        } else {
                            if(v1 > v2) {
                                return 1;
                            } else if(v1 < v2) {
                                return -1;
                            } else {
                                return 0;
                            }
                        }
                    }

                    @Override
                    public int compareBottom(int doc) {
                        int v2 = this.currentReaderValues.get(doc);
                        if(docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
                            v2 = missingValue;
                        }
                        if(v2 < page) {
                            return -1;
                        }
                        if(this.bottom > v2) {
                            return 1;
                        } else if(this.bottom < v2) {
                            return -1;
                        } else {
                            return 0;
                        }
                    }

                    @Override
                    public int compareTop(int doc) {
                        int docValue = this.currentReaderValues.get(doc);
                        if(docsWithField != null && docValue == 0 && !docsWithField.get(doc)) {
                            docValue = missingValue;
                        }
                        if(this.topValue < page && docValue > page) {
                            return 1;
                        }
                        if(this.topValue < docValue) {
                            return -1;
                        } else if(this.topValue > docValue) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                };
            }
        }));
    }

    /**
     * Adapted from Lucene 4.7 IntComparator
     * <p>
     *      Identical but intended for sorting pages of a document starting at a certain page.
     *      Previous pages are added to the end of the list
     * </p>
     * @since v0.4
     */
    public abstract class PagedIntComparator extends NumericComparator<Integer> {
        protected final int[] values;
        private final IntParser parser;
        protected FieldCache.Ints currentReaderValues;
        protected int bottom;                           // Value of bottom of queue
        protected int topValue;

        PagedIntComparator(int numHits, String field, FieldCache.Parser parser,
                           Integer missingValue) {
            super(field, missingValue);
            values = new int[numHits];
            this.parser = (IntParser) parser;
        }

        @Override
        public void copy(int slot, int doc) {
            int v2 = currentReaderValues.get(doc);
            // Test for v2 == 0 to save Bits.get method call for
            // the common case (doc has value and value is non-zero):
            if(docsWithField != null && v2 == 0 && !docsWithField.get(doc)) {
                v2 = missingValue;
            }

            values[slot] = v2;
        }

        @Override
        public FieldComparator<Integer> setNextReader(AtomicReaderContext context) throws IOException {
            // NOTE: must do this before calling super otherwise
            // we compute the docsWithField Bits twice!
            currentReaderValues = FieldCache.DEFAULT.getInts(context.reader(), field, parser, missingValue != null);
            return super.setNextReader(context);
        }

        @Override
        public void setBottom(final int bottom) {
            this.bottom = values[bottom];
        }

        @Override
        public void setTopValue(Integer value) {
            topValue = value;
        }

        @Override
        public Integer value(int slot) {
            return Integer.valueOf(values[slot]);
        }
    }
}
