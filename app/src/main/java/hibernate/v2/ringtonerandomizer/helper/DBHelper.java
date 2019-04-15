package hibernate.v2.ringtonerandomizer.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.model.Ringtone;

import static android.provider.BaseColumns._ID;

public class DBHelper extends SQLiteOpenHelper {

	public final static int CHANGE_RINGTONE_RESULT_PERMISSION = 1;
	public final static int CHANGE_RINGTONE_RESULT_COUNT_ZERO = 2;
	public final static int CHANGE_RINGTONE_RESULT_COUNT_ONE = 3;
	public final static int CHANGE_RINGTONE_RESULT_SUCCESS = 4;

	private final static String DATABASE_NAME = "ringtone.database";
	private static final String DB_TABLE_RINGTONE = "ringtone_table";

	private static final String DB_COL_NAME = "name";
	private static final String DB_COL_PATH = "path";
	private static final String DB_COL_URI_ID = "uri_id";

	private final static int DATABASE_VERSION = 4;
	private SQLiteDatabase database;

	public void setDatabase() {
		database = getWritableDatabase();
	}

	public SQLiteDatabase getDatabase() {
		return database;
	}

	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		setDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		final String INIT_TABLE = "CREATE TABLE " + DB_TABLE_RINGTONE + " ("
				+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DB_COL_NAME
				+ " text, " + DB_COL_PATH + " text, " + DB_COL_URI_ID + " text);";
		db.execSQL(INIT_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		final String DROP_TABLE = "DROP TABLE IF EXISTS " + DB_TABLE_RINGTONE;
		db.execSQL(DROP_TABLE);
		onCreate(db);
	}

	@Nullable
	public Ringtone getDBRingtone(@Nullable String uriId) {
		C.debug("getDBRingtone");
		try {
			Cursor cursor = database.query(DB_TABLE_RINGTONE,
					new String[]{DB_COL_NAME, DB_COL_PATH, DB_COL_URI_ID},
					DB_COL_URI_ID + "=" + DatabaseUtils.sqlEscapeString(uriId),
					null, null, null, null);
			cursor.moveToFirst();
			if (cursor.getCount() == 0) {
				cursor.close();
				return null;
			} else {
				Ringtone ringtone = new Ringtone();
				ringtone.setName(cursor.getString(0));
				ringtone.setPath(cursor.getString(1));
				ringtone.setUriId(cursor.getString(2));
				cursor.close();
				return ringtone;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public ArrayList<Ringtone> getDBRingtoneList(Context context) {
		C.debug("getDBRingtoneList");
		checkExist(context);
		ArrayList<Ringtone> ringtoneList = new ArrayList<>();

		Cursor cursor = database.query(DB_TABLE_RINGTONE,
				new String[]{DB_COL_NAME, DB_COL_PATH, DB_COL_URI_ID},
				null, null, null, null, null);
		while (cursor.moveToNext()) {
			Ringtone ringtone = new Ringtone();
			ringtone.setName(cursor.getString(0));
			ringtone.setPath(cursor.getString(1));
			ringtone.setUriId(cursor.getString(2));
			ringtoneList.add(ringtone);
		}
		cursor.close();

		Collections.sort(ringtoneList, new Comparator<Ringtone>() {
			@Override
			public int compare(Ringtone o1, Ringtone o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		return ringtoneList;
	}

	public void clearDBRingtoneList() {
		C.debug("clearDBRingtoneList");
		database.delete(DB_TABLE_RINGTONE, null, null);
	}

	public void deleteDBRingtone(String uriId) {
		C.debug("deleteDBRingtone");
		database.delete(DB_TABLE_RINGTONE, DB_COL_URI_ID + "=" + DatabaseUtils.sqlEscapeString(uriId), null);
	}

	public void insertDBRingtone(Ringtone ringtone) {
		C.debug("insertDBRingtone");
		if (getDBRingtone(ringtone.getUriId()) == null) {
			ContentValues values = new ContentValues();
			values.put(DB_COL_NAME, ringtone.getName());
			values.put(DB_COL_PATH, ringtone.getPath());
			values.put(DB_COL_URI_ID, ringtone.getUriId());
			database.insert(DB_TABLE_RINGTONE, null, values);
		} else {
			C.debug("exist");
		}
	}

	private void checkExist(Context mContext) {
		C.debug("checkExist");
		ArrayList<String> deleteList = new ArrayList<>();
		ArrayList<Ringtone> allRingtoneList = new ArrayList<>(C.getDeviceRingtoneList(mContext));
		Cursor cursor = database.query(DB_TABLE_RINGTONE,
				new String[]{DB_COL_URI_ID},
				null, null, null, null, null);
		while (cursor.moveToNext()) {
			boolean exist = false;
			for (Ringtone ringtone : allRingtoneList) {
				if (cursor.getString(0).equals(ringtone.getUriId())) {
					exist = true;
					break;
				}
			}
			if (!exist) {
				deleteList.add(cursor.getString(0));
			}
		}
		cursor.close();
		for (String e : deleteList) {
			deleteDBRingtone(e);
		}
	}

	@Nullable
	private Ringtone getDBRandomRingtone(Context context) {
		C.debug("getDBRandomRingtone");
		checkExist(context);

		Uri currentUri = RingtoneManager.getActualDefaultRingtoneUri(
				context, RingtoneManager.TYPE_RINGTONE);

		String selection = null;

		if (currentUri != null) {
			selection = DB_COL_URI_ID
					+ " != "
					+ DatabaseUtils.sqlEscapeString(currentUri.toString());
		}

		C.debug("selection: " + selection);
		try {
			Cursor cursor = database.query(DB_TABLE_RINGTONE,
					new String[]{DB_COL_NAME, DB_COL_PATH, DB_COL_URI_ID},
					selection, null, null, null, "RANDOM()");

			C.debug("cursor.getCount(): " + cursor.getCount());
			cursor.moveToFirst();
			if (cursor.getCount() == 0) {
				cursor.close();
				return null;
			} else {
				Ringtone ringtone = new Ringtone();
				ringtone.setName(cursor.getString(0));
				ringtone.setPath(cursor.getString(1));
				ringtone.setUriId(cursor.getString(2));
				cursor.close();
				return ringtone;
			}
		} catch (SQLiteException e) {
			return null;
		}
	}

	public int getDBRingtoneCount(Context context) {
		C.debug("getDBRingtoneCount");
		try {
			checkExist(context);
			Cursor cursor = database.query(DB_TABLE_RINGTONE, null, null, null, null, null, null);
			int count = cursor.getCount();
			cursor.close();
			return count;
		} catch (SQLiteException e) {
			return 0;
		}
	}

	public int changeRingtone(Context mContext, Ringtone ringtone) {
		// API 23 or higher user has to authorize manually for this permission
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!Settings.System.canWrite(mContext)) {
				return CHANGE_RINGTONE_RESULT_PERMISSION;
			}
		}

		int count = getDBRingtoneCount(mContext);
		switch (count) {
			case 0:
				return CHANGE_RINGTONE_RESULT_COUNT_ZERO;
			case 1:
				return CHANGE_RINGTONE_RESULT_COUNT_ONE;
			default:
				String ringtoneID = null;
				String name = null;
				if (ringtone == null) {
					ringtone = getDBRandomRingtone(mContext);
					if (ringtone != null) {
						ringtoneID = ringtone.getUriId();
						name = ringtone.getName();
					}
				} else {
					ringtoneID = ringtone.getUriId();
					name = ringtone.getName();
				}

				if (ringtoneID != null && name != null) {
					try {
						Uri pickedUri = Uri.parse(ringtoneID);
						RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, pickedUri);
						return CHANGE_RINGTONE_RESULT_SUCCESS;
					} catch (Exception e) {
						Crashlytics.logException(e);
						return CHANGE_RINGTONE_RESULT_PERMISSION;
					}
				}
		}
		return CHANGE_RINGTONE_RESULT_COUNT_ZERO;
	}
}