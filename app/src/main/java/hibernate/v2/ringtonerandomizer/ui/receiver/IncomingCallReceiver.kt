package hibernate.v2.ringtonerandomizer.ui.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import hibernate.v2.ringtonerandomizer.R
import hibernate.v2.ringtonerandomizer.helper.DBHelper
import hibernate.v2.ringtonerandomizer.helper.UtilHelper

class IncomingCallReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        UtilHelper.debug("IncomingCallReceiver onReceive")
        val setting = PreferenceManager.getDefaultSharedPreferences(context)
        if (setting.getBoolean("pref_enable", true)) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (TelephonyManager.EXTRA_STATE_RINGING == state) {
                val dbHelper = DBHelper(context)
                val message: String
                val result = dbHelper.changeRingtone(null)
                message = when (result) {
                    DBHelper.CHANGE_RINGTONE_RESULT_SUCCESS -> context.getString(R.string.changed_ringtone)
                    DBHelper.CHANGE_RINGTONE_RESULT_COUNT_ZERO -> context.getString(R.string.notyet)
                    DBHelper.CHANGE_RINGTONE_RESULT_COUNT_ONE -> context.getString(R.string.changed_ringtone_one)
                    DBHelper.CHANGE_RINGTONE_RESULT_PERMISSION -> context.getString(R.string.change_ringtone_result_permission)
                    else -> context.getString(R.string.change_ringtone_result_permission)
                }
                if (setting.getBoolean("pref_changed_notification", true) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    showNotification(context, message)
                }
                dbHelper.close()
            }
        }
    }

    private fun showNotification(context: Context, message: String) {
        val notification = NotificationCompat.Builder(context, "Changed Notification")
                .setSmallIcon(R.drawable.shuffle)
                .setContentTitle(context.getString(R.string.pref_title_changed_notification))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW).build()
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(3, notification)
    }
}