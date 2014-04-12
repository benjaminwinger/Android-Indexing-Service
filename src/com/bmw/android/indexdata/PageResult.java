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

package com.bmw.android.indexdata;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/*
 * PageResult.java
 * 
 * Data structure that is passed by the search service to the client library that 
 * stores what results from a search were found on a single page
 * It will be encapsulated in an ArrayList 
 */

public class PageResult implements Parcelable {
    public static final Parcelable.Creator<PageResult> CREATOR = new Parcelable.Creator<PageResult>() {
        public PageResult createFromParcel(Parcel in) {
            return new PageResult(in);
        }

        public PageResult[] newArray(int size) {
            return new PageResult[size];
        }
    };
    /**
     *
     */
    private static final long serialVersionUID = -2204743540787327738L;
    public List<String> text;
    public int page;
	public String document;

    public PageResult() {
		this.text = new ArrayList<String>();
    }

    private PageResult(Parcel in) {
	    this();
        in.readList(this.text, null);
	    this.page = in.readInt();
	    this.document = in.readString();
    }

    public PageResult(List<String> text, int page, String document) {
        this.text = text;
        this.page = page;
	    this.document = document;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int arg1) {
        out.writeList(text);
	    out.writeInt(this.page);
	    out.writeString(this.document);
    }

}
