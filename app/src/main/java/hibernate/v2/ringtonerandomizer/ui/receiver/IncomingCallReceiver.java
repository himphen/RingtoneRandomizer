package hibernate.v2.ringtonerandomizer.ui.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.R;
import hibernate.v2.ringtonerandomizer.ui.activity.MainActivity;
import hibernate.v2.ringtonerandomizer.utils.DBHelper;

public class IncomingCallReceiver extends BroadcastReceiver {

	private Context mContext;
	private DBHelper dbhelper;
	private SQLiteDatabase db;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(C.TAG, "IncomingCallReceiver onReceive");
		mContext = context;
		SharedPreferences setting = PreferenceManager.getDefaultSharedPreferences(context);
		if (setting.getBoolean("pref_enable", true)) {
			openDatabase();
			String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
			if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
				String message = DBHelper.changeRingtone(db, context, null);
				if (setting.getBoolean("pref_changed_notification", false)) {
					showNotification(message);
				}
			}
			closeDatabase();
		}
	}

	private void openDatabase() {
		dbhelper = new DBHelper(mContext);
		db = dbhelper.getWritableDatabase();
	}

	private void closeDatabase() {
		db.close();
		dbhelper.close();
	}

	private void showNotification(String message) {
		NotificationManager mgrNotification = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		PendingIntent pIntent = PendingIntent.getActivity(mContext, 0,
				new Intent(mContext, MainActivity.class),
				PendingIntent.FLAG_ONE_SHOT);
		Notification.Builder builder = new Notification.Builder(mContext);

		builder.setContentTitle(mContext.getString(R.string.app_name))
				.setContentText(message)
				.setSmallIcon(R.drawable.shuffle)
				.setContentIntent(pIntent);
		Notification notification = builder.build();
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		mgrNotification.notify(1, notification);
	}
}