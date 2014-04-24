package com.android.earthquake;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class EarthquakeList extends Activity  {

	public static final String REFRESH_EARTHQUAKES = "Refresh_Earthquakes";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		earthquakeListView = (ListView)findViewById(R.id.earthquakeListView);
		
		earthquakeListView.setOnItemClickListener(new OnItemClickListener() {
			@SuppressWarnings("deprecation")
			@Override
			public void onItemClick(AdapterView<?> _av,View _v,int _index,long arg3) {
				selectedQuake = earthquakes.get(_index);
				selectedQuakeIndex = _index;
				showDialog(QUAKE_DIALOG);
			}
		});
		
		int layoutID = android.R.layout.simple_list_item_1;
		aa = new ArrayAdapter<Quake>(this, layoutID, earthquakes);
		earthquakeListView.setAdapter(aa);
		
		startService(new Intent(EarthquakeList.this,EarthquakeService.class));
	}
	
	@Override 
    public void onResume() {
      IntentFilter filter;
      filter = new IntentFilter(EarthquakeService.EARTHQUAKES_UPDATED);
      receiver = new EarthquakeReceiver();
      registerReceiver(receiver, filter);
      Log.v("GUI","RESUMED REGISTERED");
      
      updateEarthquakesList();
      super.onResume();
    }

    @Override
    public void onPause() {
      unregisterReceiver(receiver);
      Log.v("GUI","PAUSED UNREGISTERED");
      super.onPause();
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_UPDATE, Menu.NONE, R.string.menu_update);
		menu.add(0, MENU_PREFERENCES, Menu.NONE, R.string.menu_preferences);
		menu.add(0, MENU_MAP, Menu.NONE, R.string.menu_map);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		
		switch (item.getItemId()) {
		case MENU_UPDATE:
			Log.v("GUI","REFRESH");
			sendBroadcast(new Intent(REFRESH_EARTHQUAKES));
			return true;
		case MENU_PREFERENCES:
			Intent i = new Intent(this, Preferences.class);
			startActivity(i);
			return true;
		case MENU_MAP:
			Intent m = new Intent(this, EarthquakeMap.class);
			startActivity(m);
			return true;
		}
		return false;
	}
	
	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case QUAKE_DIALOG:
			LayoutInflater li = LayoutInflater.from(this);
			View quakeDetailsView = li.inflate(R.layout.quake_details, null);
			
			AlertDialog.Builder quakeDialog = new AlertDialog.Builder(this);
			quakeDialog.setTitle("Quake Time");
			quakeDialog.setView(quakeDetailsView);
			quakeDialog.setNeutralButton("Show on map", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent i = new Intent(EarthquakeList.this,EarthquakeMap.class);
					i.putExtra("SELECTED_QUAKE_INDEX", selectedQuakeIndex);
					startActivity(i);
				}
			});
			return quakeDialog.create();
		}
		return null;
	}
	
	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case QUAKE_DIALOG:
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			String dateString = sdf.format(selectedQuake.getDate());
			String quakeText = "Magnitude "  +  selectedQuake.getMagnitude() + "\n" + "Depth " + selectedQuake.getDepth() + "\n" + selectedQuake.getTitle() + "\n" + selectedQuake.getLink();
			
			AlertDialog quakeDialog = (AlertDialog)dialog;
			quakeDialog.setTitle(dateString);
			TextView tv = (TextView)quakeDialog.findViewById(R.id.quakeDetailsTextView);
			tv.setText(quakeText);
			
			break;
		}
	}

	
	
	public static ArrayList<Quake> getEarthquakes() {
		return earthquakes;
	}
	
	public void updateEarthquakesList() {
		aa.notifyDataSetChanged();
	}
	
	private ListView earthquakeListView;
	private ArrayAdapter<Quake> aa;
	
	public static ArrayList<Quake> earthquakes = new ArrayList<Quake>();
	private static final int MENU_UPDATE = Menu.FIRST;
	private static final int MENU_PREFERENCES = Menu.FIRST + 1;
	private static final int MENU_MAP = Menu.FIRST + 2;
	private static final int QUAKE_DIALOG = 1;
	private Quake selectedQuake;
	private int selectedQuakeIndex;
	
	private EarthquakeReceiver receiver;
	
	private class EarthquakeReceiver extends BroadcastReceiver {
	      @Override
	      public void onReceive(Context context, Intent intent) {
	    	  Log.v("GUI","RECEIVED");
	          aa.notifyDataSetChanged();
	      }
   	}
}
