package com.android.earthquake;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.location.Location;
import android.net.ParseException;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class EarthquakeService extends Service implements OnSharedPreferenceChangeListener {
	private Timer timer;
	private SharedPreferences prefs;
	private boolean autoUpdate;
	private int minimumMagnitude;
	private int updateFreq;
	private boolean danger = false;
	private int dangerCount = 0;
	private int dangerIndex = 0;
	
	private RequestReceiver receiver;
	
	public static final String EARTHQUAKES_UPDATED = "Earthquakes_Updated";
	
	private ArrayList<Quake> earthquakes;
	private ArrayList<Quake> tempEarthquakes;
	
	@Override
	public void onCreate() {
		Log.v("SERVICE","CREATED");
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		
		IntentFilter filter;
	    filter = new IntentFilter(EarthquakeList.REFRESH_EARTHQUAKES);
	    receiver = new RequestReceiver();
	    registerReceiver(receiver, filter);
		
		updateFromPreferences();
		tempEarthquakes = new ArrayList<Quake>();
		earthquakes = EarthquakeList.getEarthquakes();
		
		new DownloadTask().execute();
		
		if (autoUpdate)
			initTimer();
	}
	
	private TimerTask doRefresh;
	
	private void initTimer() {
		Log.v("SERVICE","INITIATING TIMER");
		
		doRefresh = new TimerTask() {
			public void run() {
			      Log.v("SERVICE","DOING TASK");
			      refreshEarthquakes();
			}
		};
			
		timer = new Timer("earthquakeUpdates");
		
		Log.v("SERVICE","UPDATE FREQ" + updateFreq);
		timer.scheduleAtFixedRate(doRefresh, 0, updateFreq*60*1000);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		Log.v("SERVICE","CHANGE " + key);
		Log.v("SERVICE","UPDATING FROM PREFERENCES");
		updateFromPreferences();
		if (key.equals(Preferences.PREF_AUTO_UPDATE)) {
			if (autoUpdate) {
				Log.v("SERVICE","AUTO UPDATE ON");
				initTimer();
			} else {
				try {
					timer.cancel();
					Log.v("SERVICE","AUTO UPDATE OFF");
				} catch (NullPointerException e) {
					e.printStackTrace();
					timer = null;
				}	
			}
		}
		if (key.equals(Preferences.PREF_UPDATE_FREQ) &&
				autoUpdate) {
			Log.v("SERVICE","RESTARTING TIMER");
			try {
				timer.cancel();
			} catch (NullPointerException e) {
				e.printStackTrace();
				timer = null;
			}
			initTimer();
		}
		if (key.equals(Preferences.PREF_MIN_MAG)) {
			Log.v("SERVICE","CHANGED MAGNITUDE");
			new DownloadTask().execute();
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		Log.v("SERVICE","BINDED");
		return null;
	}
	
	private void updateFromPreferences() {
		minimumMagnitude = Integer.parseInt(prefs.getString(Preferences.PREF_MIN_MAG, "2"));
	    updateFreq = Integer.parseInt(prefs.getString(Preferences.PREF_UPDATE_FREQ, "60"));
	    autoUpdate = prefs.getBoolean(Preferences.PREF_AUTO_UPDATE, false);
	}
	
	@SuppressLint("NewApi")
	private void refreshEarthquakes() {
		URL url;
		
		try {
			DefaultHttpClient   httpclient = new DefaultHttpClient(new BasicHttpParams());
			String quakeFeed = getString(R.string.quake_feed);
			HttpGet httppost = new HttpGet(quakeFeed);
				// Depends on your web service
				httppost.setHeader("Content-type", "application/json");

				InputStream inputStream = null;
				String result = null;
				try {
				    HttpResponse response = httpclient.execute(httppost);           
				    HttpEntity entity = response.getEntity();

				    inputStream = entity.getContent();
				    // json is UTF-8 by default
				    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
				    StringBuilder sb = new StringBuilder();

				    String line = null;
				    while ((line = reader.readLine()) != null)
				    {
				        sb.append(line + "\n");
				    }
				    result = sb.toString();
				} catch (Exception e) { 
				    // Oops
				}
				finally {
				    try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
				}
				JSONArray jArray = new JSONArray(result);
				for (int i = 0; i < jArray.length(); i++)
				{
				    try {
				        JSONObject oneObject = jArray.getJSONObject(i);
				        String dt = oneObject.getString("date_time");
				        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				        Date qdate = new GregorianCalendar(0,0,0).getTime();
				        try {
				            qdate = sdf.parse(dt);
				        } catch (ParseException e) {
				            e.printStackTrace();
				        }
				        String title = oneObject.getString("title");
				        Location l = new Location("dummyGPS");
				        l.setLatitude(oneObject.getDouble("latitude"));
				        l.setLongitude(oneObject.getDouble("longitude"));
				        String place = oneObject.getString("location");
				        Double magnitude = oneObject.getDouble("magnitude");
				        Integer depth = oneObject.getInt("depth");
				        String linkString = oneObject.getString("link");
				        Quake quake = new Quake(qdate, title, l, place, magnitude, depth, linkString);
			            addNewQuake(quake, oneObject.getString("danger"), i);
				    } catch (JSONException e) {
				        // Oops
				    }
				}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			loadQuakesFromProvider();
			earthquakes.clear();
			for (Quake q : tempEarthquakes) {
				earthquakes.add(q);
			}
			sendBroadcast(new Intent(EARTHQUAKES_UPDATED));
			if (danger) {
				Log.v("DANGER",""+ dangerCount);
				Intent intent = new Intent(this, EarthquakeMap.class);
				intent.putExtra("SELECTED_QUAKE_INDEX", dangerIndex);
				PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
				
				Notification n  = new Notification.Builder(this)
				        .setContentTitle(dangerCount + " dangerous earthquakes near you!")
				        .setContentText("Tap for detailed view...")
				        .setSmallIcon(R.drawable.ic_launcher)
				        .setContentIntent(pIntent)
				        .setAutoCancel(true).build();
				  
				NotificationManager notificationManager = 
				  (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

				notificationManager.notify(0, n); 
				dangerCount = 0;
			}
			Log.v("SERVICE","SENT");
		}
	}
		
		public void addNewQuake(Quake _quake, String _danger, int i) {
			 ContentResolver cr = getContentResolver();
			 
			 String w = EarthquakeProvider.KEY_DATE + " = " + _quake.getDate().getTime();
			 
			 Cursor c = cr.query(EarthquakeProvider.CONTENT_URI, null, w, null, null);
			 
			 if (c.getCount() == 0) {
		        if (_danger.equals("none")) {
		        	Log.v("DANGER",_danger);
		        	danger = true;
		        	dangerCount++;
		        	if (dangerIndex == 0)
		        		dangerIndex = i;
		        }
				 ContentValues values = new ContentValues();
				 
				 values.put(EarthquakeProvider.KEY_DATE, _quake.getDate().getTime());
				 values.put(EarthquakeProvider.KEY_TITLE, _quake.getTitle());
				 
				 double lat = _quake.getLocation().getLatitude();
				 double lng = _quake.getLocation().getLongitude();
				 
				 values.put(EarthquakeProvider.KEY_LOCATION_LAT, lat);
				 values.put(EarthquakeProvider.KEY_LOCATION_LNG, lng);
				 values.put(EarthquakeProvider.KEY_PLACE, _quake.getPlace());
				 values.put(EarthquakeProvider.KEY_LINK, _quake.getLink());
				 values.put(EarthquakeProvider.KEY_MAGNITUDE, _quake.getMagnitude());
				 values.put(EarthquakeProvider.KEY_DEPTH, _quake.getDepth());
				 
				 cr.insert(EarthquakeProvider.CONTENT_URI, values);			 
			 }
			 
			 c.close();
		}	
		
		public void addQuakeToArray(Quake _quake) {
			 if (_quake.getMagnitude() >= minimumMagnitude) {
				 tempEarthquakes.add(_quake);
			 } 
		}
		
		public void loadQuakesFromProvider() {
			tempEarthquakes.clear();
			 
			 ContentResolver cr = getContentResolver();
			 
			 Cursor c = cr.query(EarthquakeProvider.CONTENT_URI, null, null, null, null);
			 
			 if (c.moveToFirst()) {
				 do {
					 Long datems = c.getLong(EarthquakeProvider.DATE_COLUMN);
					 String title = c.getString(EarthquakeProvider.TITLE_COLUMN);
					 Float lat = c.getFloat(EarthquakeProvider.LATITUDE_COLUMN);
					 Float lng = c.getFloat(EarthquakeProvider.LONGTITUDE_COLUMN);
					 String place = c.getString(EarthquakeProvider.PLACE_COLUMN);
					 Double mag = c.getDouble(EarthquakeProvider.MAGNITUDE_COLUMN);
					 Integer depth = c.getInt(EarthquakeProvider.DEPTH_COLUMN);
					 String link = c.getString(EarthquakeProvider.LINK_COLUMN);
					 
					 Location location = new Location("dummy");
					 location.setLongitude(lng);
					 location.setLatitude(lat);
					 
					 Date date = new Date(datems);
					 
					 Quake q = new Quake(date, title, location, place, mag, depth, link);
					 addQuakeToArray(q);
				 } while (c.moveToNext());
			 }
			 c.close();
		}
		
		private class RequestReceiver extends BroadcastReceiver {
			@Override
			public void onReceive(Context context, Intent intent) {
				new DownloadTask().execute();
			}
		}
		
		private class DownloadTask extends AsyncTask<Void, Void, Void> {
			@Override
			protected Void doInBackground(Void... params) {
				refreshEarthquakes();
				return null;
			}
		}
}
