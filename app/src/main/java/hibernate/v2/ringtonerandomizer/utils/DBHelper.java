package hibernate.v2.ringtonerandomizer.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.R;
import hibernate.v2.ringtonerandomizer.model.Ringtone;

import static android.provider.BaseColumns._ID;

public class DBHelper extends SQLiteOpenHelper {

	private final static String DATABASE_NAME = "ringtone.db";
	private static final String DB_TABLE_RINGTONE = "ringtone_table";

	private final static int DATABASE_VERSION = 1;

	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		final String INIT_TABLE = "CREATE TABLE " + DB_TABLE_RINGTONE + " ("
				+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + C.DB_COL_NAME
				+ " text, " + C.DB_COL_PATH + " text, " + C.DB_COL_POSITION
				+ " text);";
		db.execSQL(INIT_TABLE);
		final String INIT_TABLE2 = "CREATE TABLE " + C.DB_TABLE_NOTI + " ("
				+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + C.DB_COL_NAME
				+ " text, " + C.DB_COL_PATH + " text, " + C.DB_COL_POSITION
				+ " text);";
		db.execSQL(INIT_TABLE2);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		final String DROP_TABLE = "DROP TABLE IF EXISTS " + DB_TABLE_RINGTONE;
		db.execSQL(DROP_TABLE);
		onCreate(db);
	}


	@Nullable
	public static Ringtone getDBSong(SQLiteDatabase db, String path) {
		Log.i(C.TAG, "getDBSong");
		try {
			Cursor cursor = db.query(DB_TABLE_RINGTONE, new String[]{C.DB_COL_NAME,
					C.DB_COL_PATH}, C.DB_COL_PATH + "="
					+ DatabaseUtils.sqlEscapeString(path), null, null, null, null);
			cursor.moveToFirst();
			if (cursor.getCount() == 0) {
				cursor.close();
				return null;
			} else {
				Ringtone ringtone = new Ringtone();
				ringtone.setName(cursor.getString(0));
				ringtone.setPath(cursor.getString(1));
				cursor.close();
				return ringtone;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	public static ArrayList<Ringtone> getDBSongList(SQLiteDatabase db, Context context) {
		Log.i(C.TAG, "getDBSongList");
		checkExist(db, context);
		ArrayList<Ringtone> ringtoneList = new ArrayList<>();
		Cursor cursor = db.query(DB_TABLE_RINGTONE,
				new String[]{C.DB_COL_NAME, C.DB_COL_PATH},
				null, null, null, null, null);
		while (cursor.moveToNext()) {
			Ringtone ringtone = new Ringtone();
			ringtone.setName(cursor.getString(0));
			ringtone.setPath(cursor.getString(1));
			ringtoneList.add(ringtone);
		}
		cursor.close();

		Collections.sort(ringtoneList, new Comparator<Ringtone>() {
			@Override
			public int compare(Ringtone o1, Ringtone o2) {
				return o1.getPath().compareTo(o2.getPath());
			}
		});

		return ringtoneList;
	}

	public static void clearDBSongList(SQLiteDatabase db) {
		Log.i(C.TAG, "clearDBSongList");
		db.delete(DB_TABLE_RINGTONE, null, null);
	}

	public static void deleteDBSong(SQLiteDatabase db, String path) {
		Log.i(C.TAG, "deleteDBSong");
		db.delete(DB_TABLE_RINGTONE, C.DB_COL_PATH + "=" + DatabaseUtils.sqlEscapeString(path), null);
	}

	public static void insertDBSong(SQLiteDatabase db, Ringtone ringtone) {
		Log.i(C.TAG, "insertDBSong");
		if (getDBSong(db, ringtone.getPath()) == null) {
			ContentValues values = new ContentValues();
			values.put(C.DB_COL_NAME, ringtone.getName());
			values.put(C.DB_COL_PATH, ringtone.getPath());
			db.insert(DB_TABLE_RINGTONE, null, values);
		} else
			Log.i(C.TAG, "exist");
	}

	private static void checkExist(SQLiteDatabase db, Context context) {
		Log.i(C.TAG, "checkExist");
		ArrayList<Ringtone> allSongList = new ArrayList<>();
		ArrayList<String> deleteList = new ArrayList<>();
		allSongList.addAll(C.getDeviceSongList(context));
		Cursor cursor = db.query(DB_TABLE_RINGTONE, new String[]{C.DB_COL_NAME,
						C.DB_COL_PATH}, null, null, null, null,
				null);
		while (cursor.moveToNext()) {
			boolean exist = false;
			for (Ringtone ringtone : allSongList) {
				if (cursor.getString(1).equals(ringtone.getPath())) {
					exist = true;
					break;
				}
			}
			if (!exist) {
				deleteList.add(cursor.getString(1));
			}
		}
		cursor.close();
		for (String e : deleteList) {
			deleteDBSong(db, e);
		}
	}

	@Nullable
	private static Ringtone getDBRandomSong(SQLiteDatabase db, Context context) {
		checkExist(db, context);

		Uri currentUri = RingtoneManager.getActualDefaultRingtoneUri(
				context, RingtoneManager.TYPE_RINGTONE);

		String currentRingtonePath = currentUri.toString();

		String selection = C.DB_COL_PATH
				+ " != "
				+ DatabaseUtils.sqlEscapeString(currentRingtonePath);

		Log.i(C.TAG, "selection: " + selection);
		try {
			Cursor cursor = db.query(DB_TABLE_RINGTONE, new String[]{C.DB_COL_NAME,
							C.DB_COL_PATH}, selection, null, null,
					null, "RANDOM()");

			Log.i(C.TAG, "cursor.getCount(): " + cursor.getCount());
			cursor.moveToFirst();
			if (cursor.getCount() == 0) {
				cursor.close();
				return null;
			} else {
				Ringtone bean = new Ringtone();
				bean.setName(cursor.getString(0));
				bean.setPath(cursor.getString(1));
				bean.setPosition(cursor.getString(2));
				cursor.close();
				return bean;
			}
		} catch (SQLiteException e) {
			return null;
		}
	}

	public static int getDBSongCount(SQLiteDatabase db, Context context) {
		Log.i(C.TAG, "getDBSongCount");
		try {
			checkExist(db, context);
			Cursor cursor = db.query(DB_TABLE_RINGTONE, null, null, null, null, null, null);
			int count = cursor.getCount();
			cursor.close();
			return count;
		} catch (SQLiteException e) {
			return 0;
		}
	}

	public static String changeRingtone(SQLiteDatabase db, Context context, String path) {
		int count = getDBSongCount(db, context);
		if (count == 0) {
			return context.getString(R.string.notyet);
		} else if (count == 1) {
			return context.getString(R.string.changed_ringtone_one);
		} else {
			String name = null;
			if (path == null) {
				Ringtone bean = getDBRandomSong(db, context);
				if (bean != null) {
					path = bean.getPath();
					name = bean.getName();
				}
			} else {
				Ringtone bean = getDBSong(db, path);
				if (bean != null) {
					name = bean.getName();
				}
			}

			if (path != null && name != null) {
				try {
					Uri pickedUri = C.getUriByPath(path);

					RingtoneManager.setActualDefaultRingtoneUri(context,
							RingtoneManager.TYPE_RINGTONE, pickedUri);
					return context.getString(R.string.changed_ringtone) + " - " + name;

				} catch (Exception ignored) {
				}
			}
		}
		return context.getString(R.string.notyet);
	}
}