package com.bmw.android.indexservice;

import java.io.Serializable;
import java.util.List;

public class ParserService implements Serializable{
	private String name;
	private List<String> extensions;
	
	public ParserService() {
		
	}
	
	public ParserService(String name, List<String> extensions){
		this();
		this.name = name;
		this.extensions = extensions;
	}

	public String getName() {
		return name;
	}

	public boolean checkExtension(String ext) {
		return this.extensions.contains(ext);
	}

}
