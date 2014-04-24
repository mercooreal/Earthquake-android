package com.android.earthquake;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.location.Location;

public class Quake {
	
	public Quake(Date _d, String _title, Location _loc, String _place, double _mag, int _depth, String _link) {
		date = _d;
		title = _title;
		location = _loc;
		place = _place;
		magnitude = _mag;
		depth = _depth;
		link = _link;
	}
	
	@Override
	public String toString() {
		SimpleDateFormat sdf = new SimpleDateFormat("HH.mm");
		String dateString = sdf.format(date);
		return dateString + ": " + magnitude + " " + depth + " " + title;
	}
	
	public Date getDate() { return date; }
	public String getTitle() { return title; }
	public Location getLocation() { return location; }
	public String getPlace() { return place; }
	public double getMagnitude() { return magnitude; }
	public int getDepth() { return depth; }
	public String getLink() { return link; }
	
	private Date date;
	private String title;
	private Location location;
	private String place;
	private double magnitude;
	private int depth;
	private String link;
}
