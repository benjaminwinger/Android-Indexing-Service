package com.bmw.android.androidindexer;

import java.io.Serializable;

public class Next implements Serializable {
	String word;
	int page;

	public Next(){
		
	}
	
	public Next(String word, int page){
		this.word = word;
		this.page = page;
	}
	
}
