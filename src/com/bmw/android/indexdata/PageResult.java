/*******************************************************************************
 * Copyright 2014 Benjamin Winger.
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
 * along with The Android Indexing Service.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmw.android.indexdata;

import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * PageResult.java
 * 
 * Data structure that is passed by the search service to the client library that 
 * stores what results from a search were found on a single page
 * It will be encapsulated in an ArrayList 
 */

public class PageResult implements Parcelable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2204743540787327738L;
	public List<String> text;
	public int page;
	public PageResult() {

	}
	
	private PageResult(Parcel in){
		in.readList(this.text, null);
	}
	
	public static final Parcelable.Creator<PageResult> CREATOR = new Parcelable.Creator<PageResult>() {
		public PageResult createFromParcel(Parcel in) {
			return new PageResult(in);
		}

		public PageResult[] newArray(int size) {
			return new PageResult[size];
		}
	};
	
	public PageResult(List<String> text, int page) {
		this.text = text;
		this.page = page;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int arg1) {
		out.writeList(text);
	}

}
