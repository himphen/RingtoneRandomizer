package hibernate.v2.ringtonerandomizer.helper

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.Settings
import hibernate.v2.ringtonerandomizer.helper.UtilHelper.logException
import hibernate.v2.ringtonerandomizer.model.Ringtone
import java.util.ArrayList

class DBHelper(val context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private var database: SQLiteDatabase = writableDatabase

    override fun onCreate(db: SQLiteDatabase) {
        val sql = ("CREATE TABLE " + DB_TABLE_RINGTONE + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DB_COL_NAME
                + " text, " + DB_COL_PATH + " text, " + DB_COL_URI_ID + " text);")
        db.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val sql = "DROP TABLE IF EXISTS $DB_TABLE_RINGTONE"
        db.execSQL(sql)
        onCreate(db)
    }

    private fun getDBRingtone(uriId: String?): Ringtone? {
        UtilHelper.debug("getDBRingtone")
        return try {
            val cursor = database.query(DB_TABLE_RINGTONE, arrayOf(DB_COL_NAME, DB_COL_PATH, DB_COL_URI_ID),
                    DB_COL_URI_ID + "=" + DatabaseUtils.sqlEscapeString(uriId),
                    null, null, null, null)
            cursor.moveToFirst()
            if (cursor.count == 0) {
                cursor.close()
                null
            } else {
                val ringtone = Ringtone(
                        name = cursor.getString(0),
                        path = cursor.getString(1),
                        uriId = cursor.getString(2)
                )
                cursor.close()
                ringtone
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getDBRingtoneList(): ArrayList<Ringtone> {
        UtilHelper.debug("getDBRingtoneList")
        checkExist()
        val ringtoneList = ArrayList<Ringtone>()
        val cursor = database.query(DB_TABLE_RINGTONE, arrayOf(DB_COL_NAME, DB_COL_PATH, DB_COL_URI_ID),
                null, null, null, null, null)
        while (cursor.moveToNext()) {
            val ringtone = Ringtone(
                    name = cursor.getString(0),
                    path = cursor.getString(1),
                    uriId = cursor.getString(2)
            )
            ringtoneList.add(ringtone)
        }
        cursor.close()
        ringtoneList.sortWith(Comparator { o1, o2 -> o1.name.compareTo(o2.name) })
        return ringtoneList
    }

    fun clearDBRingtoneList() {
        UtilHelper.debug("clearDBRingtoneList")
        database.delete(DB_TABLE_RINGTONE, null, null)
    }

    fun deleteDBRingtone(uriId: String?) {
        UtilHelper.debug("deleteDBRingtone")
        database.delete(DB_TABLE_RINGTONE, DB_COL_URI_ID + "=" + DatabaseUtils.sqlEscapeString(uriId), null)
    }

    fun insertDBRingtone(ringtone: Ringtone) {
        UtilHelper.debug("insertDBRingtone")
        if (getDBRingtone(ringtone.uriId) == null) {
            val values = ContentValues()
            values.put(DB_COL_NAME, ringtone.name)
            values.put(DB_COL_PATH, ringtone.path)
            values.put(DB_COL_URI_ID, ringtone.uriId)
            database.insert(DB_TABLE_RINGTONE, null, values)
        } else {
            UtilHelper.debug("exist")
        }
    }

    private fun checkExist() {
        UtilHelper.debug("checkExist")
        val deleteList = ArrayList<String>()
        val allRingtoneList = UtilHelper.getDeviceRingtoneList(context)
        val cursor = database.query(DB_TABLE_RINGTONE, arrayOf(DB_COL_URI_ID),
                null, null, null, null, null)
        while (cursor.moveToNext()) {
            var exist = false
            for (ringtone in allRingtoneList) {
                if (cursor.getString(0) == ringtone.uriId) {
                    exist = true
                    break
                }
            }
            if (!exist) {
                deleteList.add(cursor.getString(0))
            }
        }
        cursor.close()
        for (e in deleteList) {
            deleteDBRingtone(e)
        }
    }

    private fun getDBRandomRingtone(): Ringtone? {
        UtilHelper.debug("getDBRandomRingtone")
        checkExist()
        val currentUri = RingtoneManager.getActualDefaultRingtoneUri(
                context, RingtoneManager.TYPE_RINGTONE)
        var selection: String? = null
        if (currentUri != null) {
            selection = (DB_COL_URI_ID
                    + " != "
                    + DatabaseUtils.sqlEscapeString(currentUri.toString()))
        }
        UtilHelper.debug("selection: $selection")
        return try {
            val cursor = database.query(DB_TABLE_RINGTONE, arrayOf(DB_COL_NAME, DB_COL_PATH, DB_COL_URI_ID),
                    selection, null, null, null, "RANDOM()")
            UtilHelper.debug("cursor.getCount(): " + cursor.count)
            cursor.moveToFirst()
            if (cursor.count == 0) {
                cursor.close()
                null
            } else {
                val ringtone = Ringtone(
                        name = cursor.getString(0),
                        path = cursor.getString(1),
                        uriId = cursor.getString(2)
                )
                cursor.close()
                ringtone
            }
        } catch (e: SQLiteException) {
            null
        }
    }

    private fun getDBRingtoneCount(): Int {
        UtilHelper.debug("getDBRingtoneCount")
        return try {
            checkExist()
            val cursor = database.query(DB_TABLE_RINGTONE, null, null, null, null, null, null)
            val count = cursor.count
            cursor.close()
            count
        } catch (e: SQLiteException) {
            0
        }
    }

    fun changeRingtone(ringtone: Ringtone?): Int {
        val selectedRingtone: Ringtone?
        // API 23 or higher user has to authorize manually for this permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                return CHANGE_RINGTONE_RESULT_PERMISSION
            }
        }
        when (getDBRingtoneCount()) {
            0 -> return CHANGE_RINGTONE_RESULT_COUNT_ZERO
            1 -> return CHANGE_RINGTONE_RESULT_COUNT_ONE
            else -> {
                var ringtoneID: String? = null
                var name: String? = null

                if (ringtone == null) {
                    selectedRingtone = getDBRandomRingtone()
                    selectedRingtone?.let {
                        ringtoneID = it.uriId
                        name = it.name
                    }
                } else {
                    selectedRingtone = ringtone
                    ringtoneID = ringtone.uriId
                    name = ringtone.name
                }

                if (selectedRingtone != null && name != null) {
                    return try {
                        val pickedUri = Uri.parse(ringtoneID)
                        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, pickedUri)
                        CHANGE_RINGTONE_RESULT_SUCCESS
                    } catch (e: Exception) {
                        logException(e)
                        CHANGE_RINGTONE_RESULT_PERMISSION
                    }
                }
            }
        }
        return CHANGE_RINGTONE_RESULT_COUNT_ZERO
    }

    companion object {
        const val CHANGE_RINGTONE_RESULT_PERMISSION = 1
        const val CHANGE_RINGTONE_RESULT_COUNT_ZERO = 2
        const val CHANGE_RINGTONE_RESULT_COUNT_ONE = 3
        const val CHANGE_RINGTONE_RESULT_SUCCESS = 4
        private const val DATABASE_NAME = "ringtone.database"
        private const val DB_TABLE_RINGTONE = "ringtone_table"
        private const val DB_COL_NAME = "name"
        private const val DB_COL_PATH = "path"
        private const val DB_COL_URI_ID = "uri_id"
        private const val DATABASE_VERSION = 4
    }
}