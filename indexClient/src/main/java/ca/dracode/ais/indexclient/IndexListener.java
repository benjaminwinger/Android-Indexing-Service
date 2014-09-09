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

import java.util.List;
import ca.dracode.ais.indexdata.PageResult;

public interface IndexListener {
	public void indexCreated(String path);

	public void indexLoaded(String path, int loaded);

	public void indexUnloaded(String path, boolean unloaded);

	public void searchCompleted(String text, PageResult[] pageResults);

	public void searchCompleted(String text, List<String> results);

	public void errorWhileSearching(String text, String index);
}
