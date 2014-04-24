package com.android.earthquake;

import java.util.ArrayList;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class EarthquakeMap extends FragmentActivity {

	private GoogleMap map;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.earthquake_map);
		
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
		
		if (status != ConnectionResult.SUCCESS) {
			int requestCode = 10;
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
			dialog.show();
		} else {
			MapFragment sf = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
			
			map = sf.getMap();
			
			ArrayList<Quake> quakes = EarthquakeList.getEarthquakes();
			ArrayList<Marker> markers = new ArrayList<Marker>();
			
			for (Quake q : quakes) {
				Double lat = q.getLocation().getLatitude();
				Double lng = q.getLocation().getLongitude();
				LatLng ll = new LatLng(lat, lng);
				Marker m = map.addMarker(new MarkerOptions()
				.position(ll)
				.title(q.getTitle())
				.snippet("Magnitude: " + q.getMagnitude() + "\n" + "Depth: " + q.getDepth()));
				markers.add(m);
			}
			
			Intent i = getIntent();
			
			if (i.hasExtra("SELECTED_QUAKE_INDEX")) {
				Marker currentMarker = markers.get(i.getIntExtra("SELECTED_QUAKE_INDEX", 0));
				map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentMarker.getPosition(), 5.0f));
				currentMarker.showInfoWindow();
			}
			
		}
	}

}
