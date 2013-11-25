package com.bmw.android.androidindexer;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;

public class Word implements Serializable {
	public HashMap<Integer, Next> next;

	public Word(){
		super();
		next = new HashMap<Integer, Next>();
	}
	
	/*public Collection<String> getNext() {
		return other.;
	}*/
	
	public boolean containsPos(int pos){
		return this.next.get(pos) != null;
	}
	
	public void addNext(String text, int page, int linknum){
		this.next.put(linknum, new Next(text, page));
	}
	public int getSize(){
		int size = 0;
		Collection<Next> tmp = next.values();
		for(Next s : tmp){
			byte[] bytes = s.word.getBytes();
			size += bytes.length;
			size += Integer.SIZE/8*2;
		}
		return size;
	}
}
