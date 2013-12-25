package com.bmw.android.indexdata;

import java.io.Serializable;

public class Result implements Serializable{
	private String text;
	private int page;
	public int getPage() {
		return page;
	}
	public void setPage(int page) {
		this.page = page;
	}
	public Result() {
		// TODO Auto-generated constructor stub
	}
	
	public Result(int page, String text) {
		this.text = text;
		this.page = page;
	}

}
