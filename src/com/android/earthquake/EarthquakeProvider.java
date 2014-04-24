package com.android.earthquake;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class EarthquakeProvider extends ContentProvider {

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count;
		
		switch (uriMatcher.match(uri)) {
		case QUAKES:
			count = earthquakeDB.delete(EARTHQUAKE_TABLE, selection, selectionArgs);
			break;
		case QUAKE_ID:
			String segment = uri.getPathSegments().get(1);
			count = earthquakeDB.delete(EARTHQUAKE_TABLE, KEY_ID + "=" + segment + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case QUAKES:
			return "vnd.android.cursor.dir/vnd.android.earthquake";
		case QUAKE_ID:
			return "vnd.android.cursor.item/vnd.android.earthquake";
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri _uri, ContentValues _values) {
		long rowID = earthquakeDB.insert(EARTHQUAKE_TABLE, "quake", _values);
		
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		}
		throw new SQLException("Failed to insert row into " + _uri);
	}

	@Override
	public boolean onCreate() {
		Context context = getContext();
		
		earthquakeDatabaseHelper dbHelper = new earthquakeDatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
		earthquakeDB = dbHelper.getWritableDatabase();
		return (earthquakeDB == null) ? false : true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		qb.setTables(EARTHQUAKE_TABLE);
		
		switch (uriMatcher.match(uri)) {
		case QUAKE_ID:
			qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
			break;
		default:
			break;
		}
		
		String orderBy;
		if (sortOrder == null) {
			orderBy = KEY_DATE + " DESC";
		} else {
			orderBy = sortOrder;
		}
		
		Cursor c = qb.query(earthquakeDB, projection, selection, selectionArgs, null, null, orderBy);
		
		c.setNotificationUri(getContext().getContentResolver(), uri);
		
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count;
		
		switch (uriMatcher.match(uri)) {
		case QUAKES:
			count = earthquakeDB.update(EARTHQUAKE_TABLE, values, selection, selectionArgs);
			break;
		case QUAKE_ID:
			String segment = uri.getPathSegments().get(1);
			count = earthquakeDB.update(EARTHQUAKE_TABLE, values, KEY_ID + "=" + segment + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
		default:
			throw new SQLException("Failed to insert row into " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
	
	public static final Uri CONTENT_URI = Uri.parse("content://com.android.provider.earthquake/earthquakes");
	
	private SQLiteDatabase earthquakeDB;
	
	private static final String TAG = "EarthquakeProvider";
	private static final String DATABASE_NAME = "earthquakes.db";
	private static final int DATABASE_VERSION = 1;
	private static final String EARTHQUAKE_TABLE = "earthquakes";
	
	public static final String KEY_ID = "_id";
	public static final String KEY_DATE = "date";
	public static final String KEY_TITLE = "title";
	public static final String KEY_LOCATION_LAT = "latitude";
	public static final String KEY_LOCATION_LNG = "longtitude";
	public static final String KEY_PLACE = "place";
	public static final String KEY_MAGNITUDE = "magnitude";
	public static final String KEY_DEPTH = "depth";
	public static final String KEY_LINK = "link";
	
	public static final int DATE_COLUMN = 1;
	public static final int TITLE_COLUMN = 2;
	public static final int LATITUDE_COLUMN = 3;
	public static final int LONGTITUDE_COLUMN = 4;
	public static final int PLACE_COLUMN = 5;
	public static final int MAGNITUDE_COLUMN = 6;
	public static final int DEPTH_COLUMN = 7;
	public static final int LINK_COLUMN = 8;
	
	public static class earthquakeDatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_CREATE = 
				"create table " + EARTHQUAKE_TABLE + " (" 
					      + KEY_ID + " integer primary key autoincrement, "
					      + KEY_DATE + " INTEGER, "
					      + KEY_TITLE + " TEXT, "
					      + KEY_LOCATION_LAT + " FLOAT, "
					      + KEY_LOCATION_LNG + " FLOAT, "
					      + KEY_PLACE + " TEXT, "
					      + KEY_MAGNITUDE + " FLOAT, "
					      + KEY_DEPTH + " INTEGER, "
					      + KEY_LINK + " TEXT);";

		public earthquakeDatabaseHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);			
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			dropTable(db);
		}	
		
		public void dropTable(SQLiteDatabase db) {
			db.execSQL("DROP TABLE IF EXISTS " + EARTHQUAKE_TABLE);
			onCreate(db);
		}
	}
	
	private static final int QUAKES = 1;
	private static final int QUAKE_ID = 2;
	
	private static final UriMatcher uriMatcher;
	
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI("com.android.provider.earthquake", "earthquakes", QUAKES);
		uriMatcher.addURI("com.android.provider.earthquake", "earthquakes/#", QUAKE_ID);
	}
}
