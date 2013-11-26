package com.bmw.android.androidindexer;

import java.io.Serializable;

public class Next implements Serializable {
	public String word;
	public int page;

	public Next(){
		
	}
	
	public Next(String word, int page){
		this.word = word;
		this.page = page;
	}
	
}
