package com.bmw.android.ais;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.LinearLayout;

import com.bmw.android.androidindexer.FileIndexer;
import com.bmw.android.indexservice.IndexService;

public class MainActivity extends Activity {

	private IndexSearcher indexSearcher;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*new Thread(new Runnable() {
			@Override
			public void run() {
				if (!isMyServiceRunning()) {
					Intent serviceIntent = new Intent(MainActivity.this, IndexService.class);
					startService(serviceIntent);
					Log.e("AIS", "Started Service");
				} else {
					Log.e("AIS", "Service Already Running");
				}
			}
		}).start();*/
		if(!this.isMyServiceRunning()){
        	Intent serviceIntent = new Intent(this, IndexService.class);
			this.startService(serviceIntent);
        }
		this.setContentView(new LinearLayout(this));
		IndexReader indexReader = null;
        IndexSearcher indexSearcher = null;
        try{
             File indexDirFile = new File(FileIndexer.getRootStorageDir());
             Directory dir = FSDirectory.open(indexDirFile);
             indexReader  = DirectoryReader.open(dir);
             indexSearcher = new IndexSearcher(indexReader);
        }catch(IOException ioe){
            ioe.printStackTrace();
        }

        this.indexSearcher = indexSearcher;
        
        String field = "text";
        String value = "Benjamin";
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
        QueryParser parser = new QueryParser(Version.LUCENE_46, field, analyzer);

        try {
           Log.e("Main", "Searching...");
           Query query = null;
		try {
			query = parser.parse(value);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
           ScoreDoc[] hits = indexSearcher.search(query, 10).scoreDocs;
           Log.e("Main", "Found " + hits.length + " results");
           for (int i = 0; i < hits.length; i++) {
                Document doc = indexSearcher.doc(hits[i].doc);
                Log.e("Main", "Found term at: " + doc.getField("path").stringValue() +" " + doc.getField("page").numericValue().longValue());
           }

        } catch (IOException e) {
        	Log.e("Main", "Error ", e);
            Log.e("Main", "Failed to search");
        }
	}

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (IndexService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
