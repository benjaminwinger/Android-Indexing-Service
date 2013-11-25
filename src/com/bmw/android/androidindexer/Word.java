package com.bmw.android.androidindexer;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;

public class Word implements Serializable {
	public HashMap<Integer, String> next;

	public Word(){
		super();
		next = new HashMap<Integer, String>();
	}
	
	/*public Collection<String> getNext() {
		return other.;
	}*/
	
	public boolean containsPos(int pos){
		return this.next.get(pos) != null;
	}
	
	public void addNext(String text, int linknum){
		this.next.put(linknum, text);
	}
	public int getSize(){
		int size = 0;
		Collection<String> tmp = next.values();
		for(String s : tmp){
			byte[] bytes = s.getBytes();
			size += bytes.length;
			size += Integer.SIZE/8;
		}
		return size;
	}
}
