package com.bmw.android.indexservice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.lucene.search.IndexSearcher;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.bmw.android.ais.R;
import com.bmw.android.androidindexer.FileIndexer;
import com.bmw.android.indexclient.MClientService;

public class IndexService extends Service {
	private static String TAG = "com.bmw.android.indexservice.IndexService";
	private ServerSocket serverSocket;
	private Thread serverThread = null;
	private static final int SERVER_PORT = 6002;
	private NotificationManager nm;
	protected long startWait;
	protected boolean interrupt;
	protected long indexTime;
	private int mIsBound;
	private boolean doneCrawling;
	private ArrayList<ParserService> services;
	private Queue<Indexable> pIndexes;
	private IndexSearcher indexSearcher;
	private FileIndexer indexer;

	public IndexService() {
		this.services = new ArrayList<ParserService>();
		// new Thread(new ServerThread()).start();
	}

	@Override
	public void onCreate() {
		nm = (NotificationManager) this
				.getSystemService(this.NOTIFICATION_SERVICE);
		IndexService.this.notifyPersistent(
				getText(R.string.notification_indexer_started), 1);
		try {
			boolean mExternalStorageAvailable = false;
			boolean mExternalStorageWriteable = false;
			String state = Environment.getExternalStorageState();

			if (Environment.MEDIA_MOUNTED.equals(state)) {
				// We can read and write the media
				mExternalStorageAvailable = mExternalStorageWriteable = true;
			} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
				// We can only read the media
				mExternalStorageAvailable = true;
				mExternalStorageWriteable = false;
			} else {
				// Something else is wrong. It may be one of many other states,
				// but
				// all we need
				// to know is we can neither read nor write
				mExternalStorageAvailable = mExternalStorageWriteable = false;
			}

			if (mExternalStorageAvailable && mExternalStorageWriteable) {
				this.loadServices(new File(Environment
						.getExternalStorageDirectory() + "/Android/data"));
			} else {
				notify("Error: External Storage not mounted", 2);
				return;
			}
			this.loadServices(Environment.getExternalStorageDirectory());
			this.indexer = new FileIndexer();
			this.pIndexes = new LinkedList<Indexable>();
			this.doneCrawling = false;
			new Thread(new Runnable() {
				public void run() {
					while (true) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						Indexable tmp = pIndexes.poll();
						if (tmp != null) {
							if (tmp.tmpData == null || tmp.tmpData.size() == 0) {
								Log.v(TAG, "Invalid File");
								IndexService.this.notify(
										"Problem indexing file "
												+ new File(tmp.currentPath)
														.getName()
												+ ": Invalid file",
										R.string.notification_indexer_error);

							} else {
								Log.i(TAG, "Testing Lucene on index: "
										+ tmp.currentPath);
								try {
									indexer.buildIndex(tmp.tmpData,
											tmp.currentPath);
								} catch (Exception e) {
									Log.e(TAG, "Error ", e);
								}
							}
						}
						if (doneCrawling && mIsBound == 0
								&& pIndexes.size() == 0) {

							IndexService.this.stopSelf();
						}
					}
				}
			}).start();
			crawl(new File("/"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void loadServices(File directory) {
		File[] contents = directory.listFiles();
		for (int i = 0; i < contents.length; i++) {
			if (contents[i].canRead()) {
				if (contents[i].isFile()) {
					if (contents[i].getName().toLowerCase().endsWith(".is")) {
						BufferedReader br = null;
						try {
							br = new BufferedReader(new FileReader(
									contents[i].getAbsolutePath()));
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (br != null) {
							try {
								String name = br.readLine();
								Log.i(TAG, "Found service of name: " + name);
								ArrayList<String> tmpExt = new ArrayList<String>();
								if (name != null) {
									String tmp;
									while ((tmp = br.readLine()) != null) {
										tmpExt.add(tmp);
									}
								}
								br.close();
								this.services.add(new ParserService(name,
										tmpExt));
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				} else {
					this.loadServices(contents[i]);
				}
			}
		}
	}

	public void onDestroy() {
		nm.cancel(1);
	}

	private class Indexable {
		private ArrayList<String> tmpData;
		private String currentPath;
		private IBinder mService;
		private String serviceName = null;
		private RemoteBuilder builder;

		public Indexable(ArrayList<String> tmpData, String currentPath,
				IBinder mService, String serviceName, RemoteBuilder builder) {
			super();
			this.tmpData = tmpData;
			this.currentPath = currentPath;
			this.mService = mService;
			this.serviceName = serviceName;
			this.builder = builder;
		}

	}

	private class RemoteBuilder {
		private ArrayList<String> tmpData;
		private String currentPath;
		private IBinder mService;
		private String serviceName = null;

		public RemoteBuilder(String path, String serviceName) {
			this.currentPath = path;
			this.serviceName = serviceName;
			this.doBindService(getApplicationContext());
		}

		void doBindService(Context c) {
			// Establish a connection with the service. We use an explicit
			// class name because we want a specific service implementation that
			// we know will be running in our own process (and thus won't be
			// supporting component replacement by other applications).
			Log.i(TAG, "Binding to service...");

			if (serviceName == null) {
				return;
			}
			/*
			 * while(mIsBound > 0){ try { Thread.sleep(1); } catch
			 * (InterruptedException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); } }
			 */
			if (c.bindService(new Intent(serviceName), mConnection,
					Context.BIND_AUTO_CREATE)) {
				mIsBound++;
			}
			Log.i(TAG, "Service is bound = " + mIsBound);
		}

		public void doUnbindService(Context c) {
			if (mIsBound > 0) {
				// Detach our existing connection.
				c.unbindService(mConnection);
				mIsBound--;
				/*
				 * if (mIsBound == 0) { nm.cancel(1); stopSelf(); }
				 */
			}
		}

		private ServiceConnection mConnection = new ServiceConnection() {
			// Called when the connection with the service is established
			public void onServiceConnected(ComponentName className,
					IBinder service) {
				// Following the example above for an AIDL interface,
				// this gets an instance of the IRemoteInterface, which we can
				// use
				// to call on the service
				mService = service;
				Log.i(TAG, "Service: " + mService);
				try {
					if (serviceName.equals("com.bmw.android.quickpdf.CONNECT")) {
						MClientService tmp = MClientService.Stub
								.asInterface(mService);
						tmp.loadFile(currentPath);
						tmpData = new ArrayList<String>();
						int pages = tmp.getPageCount();
						for (int i = 0; i < pages; i++) {
							tmpData.add(tmp.getWordsForPage(i));
						}
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// build();
				pIndexes.add(new Indexable(tmpData, currentPath, mService,
						serviceName, RemoteBuilder.this));
				doUnbindService(getApplicationContext());
			}

			// Called when the connection with the service disconnects
			// unexpectedly
			public void onServiceDisconnected(ComponentName className) {
				Log.e(TAG, "Service has unexpectedly disconnected");
				mService = null;
				mIsBound--;
			}
		};
	}

	public void crawl(File directory) throws IOException {
		// Log.i(TAG, "Indexing directory " + directory.getAbsolutePath());
		File[] contents = directory.listFiles();
		for (int i = 0; i < contents.length; i++) {
			if (contents[i].canRead()) {
				if (contents[i].isFile()) {
					String serviceName = null;
					for (int j = 0; j < services.size(); j++) {
						int mLoc = contents[i].getName().lastIndexOf(".") + 1;
						if (mLoc != -1) {
							boolean found = services.get(j).checkExtension(
									contents[i].getName().substring(mLoc)
											.toLowerCase());
							if (found) {
								serviceName = services.get(j).getName();
							}
						}
					}
					if (serviceName != null) {
						String files = "";
						try {
							BufferedReader br = new BufferedReader(
									new FileReader(
											FileIndexer.getRootStorageDir()
													+ "/FileLocations.txt"));
							String tmp;
							while ((tmp = br.readLine()) != null) {
								files = files.concat(tmp);
								files = files.concat("\n");
							}
							br.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						if (!files.contains(contents[i].getAbsolutePath())) {
							try {
								BufferedWriter bw = new BufferedWriter(
										new FileWriter(
												FileIndexer.getRootStorageDir()
														+ "/FileLocations.txt"));
								bw.append(files + contents[i].getAbsolutePath()
										+ "\n");
								bw.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						try {
							if (indexer.checkForIndex("id",
									contents[i].getAbsolutePath() + ":meta")) {
								Log.i(TAG, "Found index; skipping.");
							} else {
								Log.i(TAG, "Index not found, building index");
								try {
									new RemoteBuilder(
											contents[i].getAbsolutePath(),
											serviceName);
								} catch (Exception e) {
									Log.e(TAG, "" + e.getMessage());
								}
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		for (int i = 0; i < contents.length; i++) {
			if (contents[i].canRead()) {
				if (contents[i].isDirectory()
						&& contents[i].getAbsolutePath().equals(
								contents[i].getCanonicalPath())) {
					this.crawl(contents[i]);
				}
			}
		}
		this.doneCrawling = true;
	}

	public void notify(CharSequence c, int id) {
		Notification notification = new Notification(R.drawable.file_icon, c,
				System.currentTimeMillis());
		Intent intent = new Intent(getApplicationContext(), IndexService.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(
				getApplicationContext(), 0, intent, 0);
		notification.setLatestEventInfo(this, c, c, pendingIntent);
		nm.notify(id, notification);
	}

	public void notifyPersistent(CharSequence c, int id) {
		Notification not = new Notification(R.drawable.file_icon, c,
				System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, IndexService.class),
				Notification.FLAG_ONGOING_EVENT);
		not.flags = Notification.FLAG_ONGOING_EVENT;
		not.setLatestEventInfo(this, "AIS", "Indexing...", contentIntent);
		nm.notify(1, not);
	}

	public static String getIconLocation(String filename) {
		return FileIndexer.getRootStorageDir() + "/icons/" + filename + ".png";
	}

	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		IndexService getService() {
			return IndexService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

}
