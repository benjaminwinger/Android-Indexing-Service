package com.bmw.android.indexdata;

import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class PageResult implements Parcelable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2204743540787327738L;
	public List<String> text;
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
	
	public PageResult(List<String> text) {
		this.text = text;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int arg1) {
		out.writeList(text);
	}

}
