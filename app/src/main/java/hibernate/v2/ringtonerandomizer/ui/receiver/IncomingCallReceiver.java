package hibernate.v2.ringtonerandomizer.ui.receiver;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.telephony.TelephonyManager;

import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.R;
import hibernate.v2.ringtonerandomizer.helper.DBHelper;

public class IncomingCallReceiver extends BroadcastReceiver {

	private Context mContext;
	private DBHelper dbhelper;
	private SQLiteDatabase db;

	@Override
	public void onReceive(Context mContext, Intent intent) {
		C.debug("IncomingCallReceiver onReceive");
		this.mContext = mContext;
		SharedPreferences setting = PreferenceManager.getDefaultSharedPreferences(mContext);
		if (setting.getBoolean("pref_enable", true)) {
			String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
			if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
				dbhelper = new DBHelper(mContext);

				String message;
				int result = dbhelper.changeRingtone(mContext, null);
				switch (result) {
					case DBHelper.CHANGE_RINGTONE_RESULT_SUCCESS:
						message = mContext.getString(R.string.changed_ringtone);
						break;
					case DBHelper.CHANGE_RINGTONE_RESULT_COUNT_ZERO:
						message = mContext.getString(R.string.notyet);
						break;
					case DBHelper.CHANGE_RINGTONE_RESULT_COUNT_ONE:
						message = mContext.getString(R.string.changed_ringtone_one);
						break;
					case DBHelper.CHANGE_RINGTONE_RESULT_PERMISSION:
					default:
						message = mContext.getString(R.string.change_ringtone_result_permission);
						break;
				}

				if (setting.getBoolean("pref_changed_notification", true) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					showNotification(message);
				}
				dbhelper.close();
			}
		}
	}

	private void showNotification(String message) {
		Notification notification = new NotificationCompat.Builder(mContext, "Changed Notification")
				.setSmallIcon(R.drawable.shuffle)
				.setContentTitle(mContext.getString(R.string.pref_title_changed_notification))
				.setContentText(message)
				.setPriority(NotificationCompat.PRIORITY_LOW).build();

		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
		notificationManager.notify(3, notification);
	}
}