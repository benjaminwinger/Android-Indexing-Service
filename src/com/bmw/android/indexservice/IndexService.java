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
import com.bmw.android.androidindexer.FileIndex;
import com.bmw.android.androidindexer.FileIndexer;
import com.bmw.android.indexclient.MClientService;

public class IndexService extends Service {
	private static String TAG = "com.bmw.android.indexservice.IndexService";
	private ArrayList<FileIndex> indexes;
	private ServerSocket serverSocket;
	private Thread serverThread = null;
	private static final int SERVER_PORT = 6002;
	private NotificationManager nm;
	protected long startWait;
	protected boolean interrupt;
	protected long indexTime;
	private int mIsBound;
	private ArrayList<ParserService> services;

	public IndexService() {
		this.indexes = new ArrayList<FileIndex>();
		this.services = new ArrayList<ParserService>();
		// new Thread(new ServerThread()).start();
	}

	/*
	 * class ServerThread implements Runnable { public void run() { Socket
	 * socket = null; try { serverSocket = new ServerSocket(SERVER_PORT, 50,
	 * InetAddress.getLocalHost()); Log.i(TAG, "Listening at address: " +
	 * serverSocket.getInetAddress().getHostAddress() +
	 * serverSocket.getLocalPort()); } catch (Exception e) {
	 * e.printStackTrace(); } while (!Thread.currentThread().isInterrupted()) {
	 * try { //Log.i(TAG, "Waiting for Socket"); socket = serverSocket.accept();
	 * Log.i(TAG, "Client connected from address: " +
	 * socket.getInetAddress().getCanonicalHostName()); CommunicationThread
	 * commThread = new CommunicationThread( socket); new
	 * Thread(commThread).start(); } catch (Exception e) { e.printStackTrace();
	 * } } } }
	 * 
	 * class CommunicationThread implements Runnable { private Socket
	 * clientSocket; private BufferedReader input; private BufferedWriter
	 * output;
	 * 
	 * public CommunicationThread(Socket clientSocket) { this.clientSocket =
	 * clientSocket; try { this.input = new BufferedReader(new
	 * InputStreamReader( this.clientSocket.getInputStream())); } catch
	 * (IOException e) { // TODO Auto-generated catch block e.printStackTrace();
	 * } }
	 * 
	 * @SuppressWarnings("unchecked") public void run() { try { while
	 * (!Thread.currentThread().isInterrupted() &&
	 * this.clientSocket.isConnected() && !this.clientSocket.isInputShutdown()
	 * && input.ready()) { try { String read = input.readLine(); if
	 * (read.startsWith("load")) { String[] args = read.split(" "); boolean
	 * loaded = IndexService.this.load(args[1].replace("\\_", " ")); this.output
	 * = new BufferedWriter(new OutputStreamWriter(
	 * this.clientSocket.getOutputStream())); if (loaded) {
	 * output.write("load true"); } else { output.write("load false"); }
	 * output.flush(); output.close(); } else if (read.startsWith("search")) {
	 * String[] args = read.split(" "); List<Result> results =
	 * IndexService.this.sm.find(args[1], args[2].replace("\\_", " "),
	 * Integer.parseInt(args[3])); OutputStream os =
	 * clientSocket.getOutputStream(); ObjectOutputStream oos = new
	 * ObjectOutputStream(os); oos.writeObject(results); oos.flush();
	 * oos.close(); } else if (read.startsWith("qsearch")) { String[] args =
	 * read.split(" "); boolean[] results =
	 * IndexService.this.sm.quickFind(args[1], args[2].replace("\\_", " "));
	 * OutputStream os = clientSocket.getOutputStream(); ObjectOutputStream oos
	 * = new ObjectOutputStream(os); oos.writeObject(results); oos.flush();
	 * oos.close(); } else if (read.startsWith("build")){ String[] args =
	 * read.split(" "); ObjectInputStream in = new
	 * ObjectInputStream(clientSocket.getInputStream()); try {
	 * buildIndex(args[1].replace("\\_", " "),
	 * (ArrayList<String>)in.readObject()); } catch (ClassNotFoundException e) {
	 * // TODO Auto-generated catch block e.printStackTrace(); } in.close(); }
	 * else if (read.startsWith("unload")){ String[] args = read.split(" ");
	 * for(FileIndex index : IndexService.this.indexes){ if(new
	 * File(index.getFilename()).getAbsolutePath().equals(new
	 * File(args[1].replace("\\_", " ")).getAbsolutePath())){
	 * indexes.remove(index); } } }
	 * 
	 * } catch (IOException e) { e.printStackTrace(); } } } catch
	 * (NumberFormatException e1) { // TODO Auto-generated catch block
	 * e1.printStackTrace(); } catch (IOException e1) { // TODO Auto-generated
	 * catch block e1.printStackTrace(); } try { this.input.close();
	 * this.clientSocket.close(); } catch (IOException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } } }
	 */

	@Override
	public void onCreate() {
		nm = (NotificationManager) this
				.getSystemService(this.NOTIFICATION_SERVICE);
		IndexService.this.notifyPersistent(
				getText(R.string.notification_indexer_started),
				1);
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
	
	public void onDestroy(){
		nm.cancel(1);
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
				if (mIsBound == 0) {
					nm.cancel(1);
					stopSelf();
				}
			}
		}

		protected void build() {
			if (tmpData == null || tmpData.size() == 0) {
				Log.v(TAG, "Invalid File");
				IndexService.this.notify("Problem indexing file "
						+ new File(currentPath).getName() + ": Invalid file",
						R.string.notification_indexer_error);

			} else {
				FileIndexer index = new FileIndexer(currentPath, IndexService.this.getApplicationContext());
				index.buildIndex(tmpData, currentPath);
				index.writeIndex();
				IndexService.this.notify(
						getString(R.string.notification_indexer_indexed_file)
								+ new File(currentPath).getName(),
						R.string.notification_indexer_indexed_file);
			}
			doUnbindService(getApplicationContext());
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
				build();
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
						if (!FileIndexer.indexExists(contents[i]
								.getAbsolutePath())) {
							Log.i(TAG,
									"Creating index for "
											+ contents[i].getAbsolutePath());
							try {
								new RemoteBuilder(
										contents[i].getAbsolutePath(),
										serviceName);
							} catch (Exception e) {
								Log.e(TAG, "" + e.getMessage());
							}
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

	/*
	 * public boolean buildIndex(String filePath, List<String> text){ if
	 * (!FileIndexer.indexExists(filePath)) { FileIndex tmpIndex = null; try {
	 * FileIndexer indexer = new FileIndexer(filePath); tmpIndex =
	 * indexer.buildIndex(text, filePath);
	 * IndexService.this.indexes.add(tmpIndex); } catch (Exception ex) {
	 * Log.v("PDFIndex", "" + ex.getMessage()); ex.printStackTrace(); } finally
	 * { Log.i(TAG, "Loaded Index of size: " + tmpIndex.getSize()); } return
	 * true; } else { return false; } }
	 * 
	 * public boolean load(final String filePath) { if
	 * (FileIndexer.indexExists(filePath)) { FileIndex tmpIndex = null; try {
	 * FileIndexer indexer = new FileIndexer(filePath); tmpIndex =
	 * indexer.loadIndex(); IndexService.this.indexes.add(tmpIndex); } catch
	 * (Exception ex) { Log.v("PDFIndex", "" + ex.getMessage());
	 * ex.printStackTrace(); } finally { Log.i(TAG, "Loaded Index of size: " +
	 * tmpIndex.getSize()); } return true; } else { return false; } }
	 * 
	 * private class SearchManager { boolean found; boolean finishedLoading =
	 * false;
	 * 
	 * public SearchManager() {
	 * 
	 * }
	 * 
	 * public void loadAllIndexes() { File directory = new
	 * File(FileIndexer.getStorageDir()); File[] contents =
	 * directory.listFiles(new FileFilter() {
	 * 
	 * @Override public boolean accept(File f) { if (f.isFile() &&
	 * f.getName().toLowerCase().endsWith(".index") && f.canRead()) { return
	 * true; } return false; }
	 * 
	 * });
	 * 
	 * Comparator<File> cmp = new Comparator<File>() {
	 * 
	 * @Override public int compare(File left, File right) { if (left.length() <
	 * right.length()) { return -1; } else if (left.length() == right.length())
	 * { return 0; } else { return 1; } } }; if (contents != null) {
	 * Arrays.sort(contents, cmp); }
	 * 
	 * for (int i = 0; i < contents.length; i++) { Log.i(TAG,
	 * "Loading index; size is: " + contents[i].length()); indexes.add(new
	 * FileIndexer(contents[i].getAbsolutePath()) .loadIndex()); } Log.i(TAG,
	 * "Done loading indexes"); finishedLoading = true; }
	 * 
	 * private List<Result> find(String text, String indexname, int variance) {
	 * FileIndex index = null; for (int i = 0; i < indexes.size(); i++) { if
	 * (indexes.get(i).getFilename().equals(indexname)) { index =
	 * indexes.get(i); } } if (index == null) { return new ArrayList<Result>();
	 * } if (text == null) throw new
	 * IllegalStateException("text cannot be null"); if (text.length() < 1) {
	 * return new ArrayList<Result>(); } String tmp = text; String pdfName = new
	 * File(index.getFilename()).getName(); pdfName = pdfName.replace(".index",
	 * ""); pdfName = pdfName + ".pdf"; String[] search = tmp.split(" "); for
	 * (int j = 0; j < search.length; j++) { search[j] =
	 * search[j].toLowerCase(); } ArrayList<Result> results = new
	 * ArrayList<Result>(); for (int page = 0; page < index.getPageCount();
	 * page++) { if (search.length == 1) { Set<Entry<String, Word>> words =
	 * index .getWordsForPage(page).entrySet(); for (Entry<String, Word> e :
	 * words) { if (e.getKey().contains(search[0])) { Set<Entry<Integer,
	 * String>> curr = e.getValue().next .entrySet(); for (Entry<Integer,
	 * String> entry : curr) { results.add(new Result(page, this.getResult(
	 * index, page, entry.getKey(), 1, variance))); } } } } else { Set<String>
	 * words = index.getWordsForPage(page).keySet(); int location = -1; for
	 * (String w : words) { if (w.endsWith((search[0]))) { Word currentWord =
	 * index.getWordsForPage(page).get( w); if (search.length == 2) {
	 * Set<Entry<Integer, String>> curr = currentWord.next .entrySet(); for
	 * (Entry<Integer, String> entry : curr) { if
	 * (entry.getValue().startsWith(search[1])) { location = entry.getKey();
	 * results.add(new Result( page, this.getResult(index, page, location,
	 * search.length, variance))); } } } else { Set<Entry<Integer, String>> curr
	 * = currentWord.next .entrySet(); for (Entry<Integer, String> entry : curr)
	 * { if (entry.getValue().equals(search[1])) { if (this.matchesString(index,
	 * page, search, 2, index.getWord(entry.getValue(), page), entry.getKey() +
	 * 1)) { location = entry.getKey();
	 * 
	 * results.add(new Result(page, this .getResult(index, page, location,
	 * search.length, variance)));
	 * 
	 * } } } } }
	 * 
	 * }
	 * 
	 * } }
	 * 
	 * return results; }
	 * 
	 * private boolean[] quickFind(String text, String indexname) { FileIndex
	 * index = null; for (int i = 0; i < indexes.size(); i++) { if
	 * (indexes.get(i).getFilename().equals(indexname)) { index =
	 * indexes.get(i); } } if (index == null) { return new boolean[0]; } if
	 * (text == null) throw new IllegalStateException("text cannot be null"); if
	 * (text.length() < 1) { return new boolean[0]; } String tmp = text; String
	 * pdfName = new File(index.getFilename()).getName(); pdfName =
	 * pdfName.replace(".index", ""); pdfName = pdfName + ".pdf"; String[]
	 * search = tmp.split(" "); for (int j = 0; j < search.length; j++) {
	 * search[j] = search[j].toLowerCase(); } boolean[] results = new
	 * boolean[index.getPageCount()]; for (int page = 0; page <
	 * index.getPageCount(); page++) { if (search.length == 1) {
	 * Set<Entry<String, Word>> words = index .getWordsForPage(page).entrySet();
	 * for (Entry<String, Word> e : words) { if (e.getKey().contains(search[0]))
	 * { results[page] = true; break; } } } else { Set<String> words =
	 * index.getWordsForPage(page).keySet(); int location = -1; for (String w :
	 * words) { if (w.endsWith((search[0]))) { Word currentWord =
	 * index.getWordsForPage(page).get( w); if (search.length == 2) {
	 * Set<Entry<Integer, String>> curr = currentWord.next .entrySet(); for
	 * (Entry<Integer, String> entry : curr) { if
	 * (entry.getValue().startsWith(search[1])) { results[page] = true; break; }
	 * } } else { Set<Entry<Integer, String>> curr = currentWord.next
	 * .entrySet(); for (Entry<Integer, String> entry : curr) { if
	 * (entry.getValue().equals(search[1])) { if (this.matchesString(index,
	 * page, search, 2, index.getWord(entry.getValue(), page), entry.getKey() +
	 * 1)) { results[page] = true; break; } } } } } } } }
	 * 
	 * return results; }
	 * 
	 * private boolean matchesString(FileIndex index, int page, String[] search,
	 * int searchPos, Word w, int position) { if (searchPos >= search.length) {
	 * return true; } String next = w.next.get(position); if (searchPos ==
	 * search.length - 1) { if (!next.startsWith(search[searchPos])) { return
	 * false; } } else { if (!next.equals(search[searchPos])) { return false; }
	 * } Word tmpWd = index.getWord(next, page); return
	 * this.matchesString(index, page, search, searchPos + 1, tmpWd, position +
	 * 1); }
	 * 
	 * private String getResult(FileIndex index, int page, int pos, int len, int
	 * variance) { return index.getPhrase(page, pos - variance, pos + len +
	 * variance); } }
	 */

}
