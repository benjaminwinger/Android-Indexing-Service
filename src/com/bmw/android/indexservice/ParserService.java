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
package com.bmw.android.indexservice;

import java.io.Serializable;
import java.util.List;

/*
 * ParserService.java
 * Info object created for use in the IndexService for accessing client services
 */

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
