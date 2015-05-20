/*
 * Copyright 2014 Dracode Software.
 *
 * This file is part of AIS.
 *
 * AIS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * AIS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AIS.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.dracode.ais.ui;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ca.dracode.ais.R;
import ca.dracode.ais.indexclient.IndexClient;
import ca.dracode.ais.indexclient.IndexListener;
import ca.dracode.ais.indexdata.SearchResult;

public class MainActivity extends Activity implements IndexListener {
    private static final String TAG = "ca.dracode.ais.ui.MainActivity";
    private IndexClient ic;
    private ExpandableListView resultView;
    private SearchResult result = new SearchResult();
    private boolean canSearch = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        this.ic = new IndexClient(this, this);
        this.resultView = (ExpandableListView)this.findViewById(R.id.list);
        this.result = new SearchResult();
        this.resultView.setAdapter(la);
        this.resultView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view,
                                        int groupPosition, int childPosition, long id) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                LinkedHashMap<Integer, List<String>> data = result.getResultAtIndex(groupPosition);
                int page = data.keySet().toArray(new Integer[0])[childPosition];
                String term = ((List<String>)data.values().toArray()[childPosition]).get(0);
                term = term.substring(term.indexOf("<B>"), term.lastIndexOf("</B>"));
                term = term.replace("<B>", "").replace("</B>", "");
                File f = new File(result.getFileNames().toArray(new String[0])[groupPosition]);
                if(f.getName().contains(".")) {
                    String extension = "";

                    int i = f.getName().lastIndexOf('.');
                    if (i > 0) {
                        extension = f.getName().substring(i+1);
                    }
                    String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    intent.setDataAndType(Uri.fromFile(f), mime);
                    intent.putExtra("page", page); // makes sure that the search goes forwards
                    Log.i(TAG, "Requesting launch of " + f.getName() + " at page " + page + " " +
                            "highlighting " + term);
                    intent.putExtra("search", term);
                    startActivity(intent);
                } else {
                    return false;
                }
                return true;
            }
        });
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        this.handleIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        this.result = new SearchResult();
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        this.handleIntent(intent);
        this.resultView.setAdapter(la);
    }

    private void handleIntent(Intent intent){
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.i(TAG, "Searching for: " + query);
            search(query);
        }
    }


    @Override
    public void onDestroy(){
        ic.close(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_settings) {
            Intent i = new Intent(this, AISPreferences.class);
            startActivity(i);
            return true;
        }
        if(id == R.id.menu_search){
            this.onSearchRequested();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void search(String query) {
        if(!canSearch)return;
        ArrayList<String> tmp = new ArrayList<String>();
        Log.i(TAG, "Service: " + this.ic.isServiceConnected());
        SearchResult tmpResult = this.ic.searchIn(query, IndexClient.QUERY_STANDARD, tmp, 10, 0,
                false);
        if(tmpResult != null) {
            this.showResult(tmpResult);
        }
    }

    @Override
    public void indexCreated(String s) {

    }

    @Override
    public void indexLoaded(String s, int i) {

    }

    @Override
    public void indexUnloaded(String s, boolean b) {

    }

    @Override
    public void errorWhileSearching(String s, String s2) {

    }

    @Override
    public void connectedToService() {
        canSearch = true;
        Toast.makeText(this, "Connected to Search Service", 2).show();
    }

    @Override
    public void disconnectedFromService() {

    }

    private void showResult(SearchResult result){
        Log.i(TAG, "Result: " + result);
        this.result = result;
        this.resultView.requestLayout();
    }

    ExpandableListAdapter la = new ExpandableListAdapter() {
        @Override
        public void registerDataSetObserver(DataSetObserver dataSetObserver) {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {

        }

        @Override
        public int getGroupCount() {
            return result.size();
        }

        @Override
        public int getChildrenCount(int i) {
            return result.getResultAtIndex(i).size();
        }

        @Override
        public Object getGroup(int i) {
            return result.getResultAtIndex(i);
        }

        @Override
        public Object getChild(int i, int i2) {
            LinkedHashMap<Integer, List<String>> tmp = result.getResultAtIndex(i);
            Iterator<Map.Entry<Integer, List<String>>> iter = tmp.entrySet().iterator();
            for(int j = 0; j < i2 - 1; j++){
                iter.next();
            }
            return iter.next();
        }

        @Override
        public long getGroupId(int i) {
            return 0;
        }

        @Override
        public long getChildId(int i, int i2) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int i, boolean expanded, View view, ViewGroup viewGroup) {
            LinkedHashMap<Integer, List<String>> group = (LinkedHashMap<Integer,
                    List<String>>)this.getGroup(i);
            String name = (String)result.getFileNames().toArray()[i];
            if(view == null){
                view = getLayoutInflater().inflate(R.layout.result_document, null);
            }
            TextView title = (TextView)view.findViewById(R.id.resultTitle);
            title.setText(name);
            return view;
        }

        @Override
        public View getChildView(int i, int i2, boolean b, View view, ViewGroup viewGroup) {
            Map.Entry<Integer, List<String>> child = (Map.Entry<Integer,
                    List<String>>)this.getChild(i, i2);
            if(view == null){
                view = getLayoutInflater().inflate(R.layout.result_page, null);
            }
            TextView page = (TextView)view.findViewById(R.id.pagenum);
            page.setText((child.getKey() + 1) + " ");
            TextView text = (TextView)view.findViewById(R.id.text);
            StringBuilder sb = new StringBuilder();
            for(String s : child.getValue()){
                sb.append("..." + s + "...\n");
            }
            text.setText(Html.fromHtml(sb.toString()));
            return view;
        }

        @Override
        public boolean isChildSelectable(int i, int i2) {
            return true;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEmpty() {
            return result.size() == 0;
        }

        @Override
        public void onGroupExpanded(int i) {

        }

        @Override
        public void onGroupCollapsed(int i) {

        }

        @Override
        public long getCombinedChildId(long l, long l2) {
            return 0;
        }

        @Override
        public long getCombinedGroupId(long l) {
            return 0;
        }
    };
}
