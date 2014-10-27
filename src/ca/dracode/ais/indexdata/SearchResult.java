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

package ca.dracode.ais.indexdata;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedHashMap;
import java.util.List;

/*
 * PageResult.java
 * 
 * Data structure that is passed by the search service to the client library that 
 * stores what results from a search were found on a single page
 * It will be encapsulated in an ArrayList 
 */

public class SearchResult implements Parcelable {
    public static final Parcelable.Creator<javax.naming.directory.SearchResult> CREATOR = new Parcelable
            .Creator<javax.naming.directory.SearchResult>() {
        public javax.naming.directory.SearchResult createFromParcel(Parcel in) {
            return new javax.naming.directory.SearchResult(in);
        }

        public javax.naming.directory.SearchResult[] newArray(int size) {
            return new javax.naming.directory.SearchResult[size];
        }
    };
    private static final long serialVersionUID = -2204743540787327738L;
    LinkedHashMap<String, LinkedHashMap<Integer, List<String>>> results;

    public SearchResult() {
        this.results = new LinkedHashMap<String, LinkedHashMap<Integer, List<String>>>();
    }

    private SearchResult(Parcel in) {
        this();
        this.results = (LinkedHashMap<String, LinkedHashMap<Integer,
                List<String>>>)in.readSerializable();
    }

    public SearchResult(LinkedHashMap<String, LinkedHashMap<Integer, List<String>>> results) {
        this.results = results;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int arg1) {
        out.writeSerializable(results);
    }

}
