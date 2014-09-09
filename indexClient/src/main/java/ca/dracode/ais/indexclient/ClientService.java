/*******************************************************************************
 * Copyright 2014 Benjamin Winger.
 *
 * This file is part of android-indexing-service-client-library.
 *
 * android-indexing-service-client-library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * android-indexing-service-client-library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with android-indexing-service-client-library.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/


package ca.dracode.ais.indexclient;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * @author benjamin
 */
public abstract class ClientService extends Service {

	/**
	 * Template
	 */
	/*private final MClientService.Stub mBinder = new MClientService.Stub() {
		public List<String> getWords(String path){
			return this.getWords(path);
		}
	};*/

	/**
	 * Load libraries to access the file here so that it only has to be done once.
	 * Only load information specific to each file. Generic loading should be done in
	 * the onCreate function
	 * This will always be the first function called
	 *
	 * @param path - the path of the file to be loaded
	 */
	public abstract void loadFile(String path);

	/**
	 * The indexer will query the contents of each page one at a time.
	 * Sending all of the information at once is too large to transfer in some files.
	 *
	 * @param page - the page of the file to be returned
	 * @return - A string containing all of the words on the page
	 */
	public abstract String getWordsForPage(int page);

	/**
	 * @return - the number of pages in the file specified at loadFile(String path)
	 */
	public abstract int getPageCount();


	/*
		Load as much as possible in the constructor as the loadFile function may
		be called multiple times.
	 */
	@Override
	public void onCreate() {

	}

	@Override
	public IBinder onBind(Intent i) {
		return null;
	}

}
