package com.bmw.android.androidindexer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class KryoWrapper {
	private Kryo kryo = new Kryo(); // version 2.x

	public void WriteBuffered(ArrayList<HashMap<String, Word>> test, FileOutputStream os)
			throws IOException {
		Output output = null;
		try {
			output = new Output(os);
			kryo.writeObject(output, test);
		} finally {
			if (output != null) {
				output.close();
			}
		}
	}
	
	public void WriteBuffered(ArrayList<HashMap<String, Word>> test, String fileName)
			throws IOException {
		Output output = null;
		try {
			RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
			output = new Output(new FileOutputStream(raf.getFD()), 4*1024*1024);
			kryo.register(Word.class);
			kryo.writeObject(output, test);
		} finally {
			if (output != null) {
				output.close();
			}
		}
	}

	public ArrayList<HashMap<String, Word>> ReadBuffered(String fileName)
			throws IOException {
		ArrayList<HashMap<String, Word>> index = null;
		Input input = null;
		try {
			RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
			input = new Input(new FileInputStream(raf.getFD()), 8*1024*1024);
			kryo.register(Word.class);
			index = kryo.readObject(input, ArrayList.class);
		} catch(Exception e){
			e.printStackTrace();
		}
		finally {
			if (input != null) {
				input.close();
			}
		}
		return index;
	}
}
