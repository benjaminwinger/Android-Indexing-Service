package ca.dracode.ais.indexinfo;

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
interface IndexComm {
    void stopIndexer();

    /*
        Get Current State of the Indexer
        @return true if the indexer is running, false otherwise
     */
    boolean isIndexerRunning();

    /*
        Sets the file change listener
        @param a listener that will be notified whenever the indexer starts indexing a
            new file
     */
    void setFileChangeListener(out FileChangeListener listener);

    /*
        Gets the number of documents in the index
        @return the number of documents in the index
     */
    int getNumDocumentsInIndex();
}
