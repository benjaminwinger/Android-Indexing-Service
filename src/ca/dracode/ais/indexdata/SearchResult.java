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

package ca.dracode.ais.indexdata;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/*
 * SearchResult.java
 * 
 * Data structure that is passed by the search service to the client library that 
 * stores what results from a search were found on a single page
 * It will be encapsulated in an ArrayList 
 */

public class SearchResult implements Parcelable {
    public static final Parcelable.Creator<SearchResult> CREATOR = new Parcelable
            .Creator<SearchResult>() {
        public SearchResult createFromParcel(Parcel in) {
            return new SearchResult(in);
        }

        public SearchResult[] newArray(int size) {
            return new SearchResult[size];
        }
    };
    private static final long serialVersionUID = -2204743540787327738L;
    LinkedHashMap<String, LinkedHashMap<Integer, List<String>>> results;

    public SearchResult() {
        this.results = new LinkedHashMap<String, LinkedHashMap<Integer, List<String>>>();
    }

    public SearchResult(Parcel in) {
        this();
        this.results = (LinkedHashMap<String, LinkedHashMap<Integer,
                List<String>>>)in.readSerializable();
    }

    /**
     * Creates a SearchResult by directly encapsulating a given LinkedHashMap
     * @param results
     */
    public SearchResult(LinkedHashMap<String, LinkedHashMap<Integer, List<String>>> results) {
        this.results = results;
    }

    /**
     * Returns the first document in the result
     * @return A LinkedHashMap<Integer,String> which maps each result in the document to its
     * respective page number
     */
    public LinkedHashMap<Integer, List<String>> getFirstResult(){
        return this.getResultAtIndex(0);
    }

    /**
     * Returns the document at the requested index
     * @param index the index of the document to return
     * @return A LinkedHashMap<Integer,String> which maps each result in the document to its
     * respective page number
     */
    public LinkedHashMap<Integer, List<String>> getResultAtIndex(int index){
        if(this.results.size() > 0)
            return (LinkedHashMap<Integer, List<String>>)this.results.entrySet().toArray()[index];
        else return null;
    }

    /**
     * Returns the names of each file in the SearchResult
     * @return A Set<String> containing each name
     */
    public Set<String> getFileNames(){
        return this.results.keySet();
    }

    public int size(){
        int size = 0;
        for(LinkedHashMap<Integer, List<String>> s : results.values()){
            size += s.size();
        }
        return size;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int arg1) {
        out.writeSerializable(results);
    }

}
