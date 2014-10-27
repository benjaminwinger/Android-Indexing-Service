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

package ca.dracode.ais.indexer;

/*
 * FileIndexer.java
 * 
 * Contains functions for building the lucene index.
 *  TODO - Evaluate the usefulness of ForceMerging as it increases total indexing time by about 17%
 */

import android.os.Environment;
import android.util.Log;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileIndexer {
    private static final String TAG = "ca.dracode.ais.androidindexer.FileIndexer";
    private IndexWriter writer;
    private FileSearcher searcher;

    public FileIndexer() {
        super();
        this.searcher = new FileSearcher();
        Directory dir;
        try {
            dir = FSDirectory.open(new File(FileIndexer.getRootStorageDir()));
            Analyzer analyzer = new SimpleAnalyzer(Version.LUCENE_47);
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_47,
                    analyzer);
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            this.writer = new IndexWriter(dir, iwc);
            this.writer.commit();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a Document containing contents and metadata for a specific page of a file
     * @param writer The writer used to save the metadata
     * @param file The file that the page belongs to
     * @param page The index of the page in the file
     * @param contents The string contents of the file
     */
    public static void Build(IndexWriter writer, File file, int page,
                             String contents) {
        if(file.canRead()) {
            try {
                //Log.i(TAG, "Started Indexing file: " + file.getName() + " "
                //		+ page);
                Document doc = new Document();
                doc.add(new StringField("id", file.getPath() + ":" + page,
                        Field.Store.NO));
                doc.add(new StringField("path", file.getPath(),
                        Field.Store.YES));
                doc.add(new LongField("modified", file.lastModified(),
                        Field.Store.YES));
                // for(int i = 0; i < contents.size(); i++){
                doc.add(new TextField("text", "" + contents, Field.Store.YES));
                doc.add(new IntField("page", page, Field.Store.YES));
                // }
                // TODO - Check what OpenMode.CREATE_OR_APPEND does; I think updateDocument should
                // always be used with CREATE_OR_APPEND, the if part may need to be removed
                if(writer.getConfig().getOpenMode() == OpenMode.CREATE) {
                    writer.addDocument(doc);
                } else {
                    // TODO - Test UpdateDocument
                    writer.updateDocument(new Term("id", file.getPath() + ":"
                            + page), doc);
                }
                //Log.i(TAG, "Done Indexing file: " + file.getName() + " " + page);
            } catch(Exception e) {
                Log.e(TAG, "Error ", e);
            }
        }
    }

    /**
     * Gets the directory that stores the Index
     * @return the path of the directory that stores the Index; null if the directory is not
     * writeable or is not available
     */
    public static String getRootStorageDir() {
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();

        if(Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but
            // all we need
            // to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if(mExternalStorageAvailable && mExternalStorageWriteable) {
            return Environment.getExternalStorageDirectory()
                    + "/Android/data/ca.dracode.ais";
        } else {
            return null;
        }
    }


    // TODO - make the indexer restart indexing a file if it fails. When
    // buildIndex is called from SearchService android.os.DeadObjectException is
    // called on the SearchService from building larger indexes


    /**
     * Checks to see if a file with a matching Document field and value exists in the index
     * @param field The field to use to check for the file's existence
     * @param value The identifier for the file; type depends of the given field
     * @param modified The time that the file in the filesystem was last modified; used to
     *                 determine if the file has been modified since it was indexed
     * @return -1 if the index does not exist; 1 if the index exists but is out of date; 0 if the
     * index exists and is up to date
     * @throws IOException
     */
    private int checkForIndex(String field, String value, long modified) throws IOException {
        Document doc = this.searcher.getDocument(field, value);
        if(doc != null) {
            if(Long.parseLong(doc.get("modified")) < modified) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return -1;
        }
    }

    /**
     * Checks to see if a file with a matching id exists in the index
     * @param id The field to use to check for the file's existence
     * @param modified The time that the file in the filesystem was last modified; used to
     *                 determine if the file has been modified since it was indexed
     * @return -1 if the index does not exist; 1 if the index exists but is out of date; 0 if the
     * index exists and is up to date
     * @throws IOException
     */
    public int checkForIndex(String id, long modified) throws IOException {
        return this.checkForIndex("id", id, modified);
    }

    /**
     * Removes all files matching the given path
     * <p>
     *     Removes the metadata file and page files for the given file path
     * </p>
     * @param path Path of the file(s) to be removed
     * @return true if one or more files were removed; false otherwise
     */
    public boolean removeIndex(String path) {
        try {
            if(this.checkForIndex("path", path, 0) != -1) {
                this.writer.deleteDocuments(new Term("path", path));
                return true;
            }
        } catch(IOException e) {
            Log.e(TAG, "Error ", e);
        }
        return false;
    }

    /**
     * Creates the metadata Document for a given file
     * @param filename The path of the file that the metadata will describe
     * @param pages The number of pages in the file; -1 if it's contents are not indexed
     * @return 0 upon successful index creation; -1 on error
     */
    public int buildIndex(String filename, int pages) {
        try {
            //Log.i(TAG, "Writing Metadata");
            Document doc = new Document();
            File file = new File(filename);
            doc.add(new StringField("id", file.getPath() + ":meta", Field.Store.NO));
            doc.add(new LongField("modified", file.lastModified(), Field.Store.YES));
            doc.add(new StringField("path", file.getAbsolutePath(), Field.Store.YES));
            if(pages != -1) {
                doc.add(new IntField("pages", pages, Field.Store.YES));
            }
            if(writer.getConfig().getOpenMode() == OpenMode.CREATE) {
                writer.addDocument(doc);
            } else {
                writer.updateDocument(new Term("id", file.getPath() + ":meta"),
                        doc);
            }
            Log.i(TAG, "Done creating metadata for file " + filename);
            // Must only call ForceMerge and Commit once per document as they are very resource heavy operations
            writer.commit();
        } catch(Exception e) {
            Log.e(TAG, "Error", e);
            return -1;
        }
        return 0;
    }

    /**
     * Creates a metadata Document and page Documents for a given file
     * @param contents The text contents of the file; separated into pages
     * @param file The file that will be added to the index
     * @return 0 upon successful index creation; -1 otherwise
     */
    public int buildIndex(List<String> contents, File file) {
        try {
            for(int i = 0; i < contents.size(); i++) {
                if(this.checkForIndex("id", file.getAbsolutePath() + ":" + i, file.lastModified()) != 0) {
                    FileIndexer.Build(writer, file, i, contents
                            .get(i));
                } else {
                    //Log.i(TAG, "Skipping " + file.getAbsolutePath() + ":" + i
                    //		+ " Already in index");
                }
            }
            buildIndex(file.getPath(), contents.size());
        } catch(Exception e) {
            Log.e(TAG, "Error", e);
            return -1;
        }
        return 0;
    }

    /**
     * Closes the index
     * <p>
     *     Must be called before the indexer is disposed of
     * </p>
     */
    public void close() {
        try {
            writer.commit();
            // TODO - Determine how much of a speed increase is gained while searching after ForceMerge
            writer.forceMerge(1);
            writer.close();
        } catch(IOException e) {
            Log.e(TAG, "Error while closing indexwriter", e);
        }
    }
}
