package hibernate.v2.ringtonerandomizer.ui.fragment;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.preference.Preference;
import android.view.View;

import com.blankj.utilcode.util.AppUtils;

import hibernate.v2.ringtonerandomizer.BuildConfig;
import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.R;

public class SettingsFragment extends BasePreferenceFragment {

	private Preference prefChangedNotificationAndroidO;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_general);
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Preference prefReport = findPreference("pref_report");
		Preference prefMoreApp = findPreference("pref_more_app");
		Preference prefVersion = findPreference("pref_version");
		Preference prefChangedNotification = findPreference("pref_changed_notification");
		prefChangedNotificationAndroidO = findPreference("pref_changed_notification_android_o");

		prefVersion.setSummary(AppUtils.getAppVersionName());

		prefReport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				openDialogReport();
				return false;
			}
		});
		prefMoreApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				openDialogMoreApp();
				return false;
			}
		});

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			prefChangedNotification.setVisible(false);
			prefChangedNotificationAndroidO.setVisible(true);
			prefChangedNotificationAndroidO.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent();
					intent.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
					intent.putExtra(Settings.EXTRA_APP_PACKAGE, AppUtils.getAppPackageName());
					intent.putExtra(Settings.EXTRA_CHANNEL_ID, "Changed Notification");
					startActivity(intent);
					return false;
				}
			});
		} else {
			prefChangedNotification.setVisible(true);
			prefChangedNotificationAndroidO.setVisible(false);
		}
	}

	public void onResume() {
		super.onResume();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);
			assert notificationManager != null;
			NotificationChannel notificationChannel = notificationManager.getNotificationChannel("Changed Notification");

			if (notificationChannel != null) {
				if (notificationChannel.getImportance() != NotificationManager.IMPORTANCE_NONE) {
					prefChangedNotificationAndroidO.setSummary(getString(R.string.pref_des_changed_notificationOn));
				} else {
					prefChangedNotificationAndroidO.setSummary(getString(R.string.pref_des_changed_notificationOff));
				}
			}
		}
	}

	private void openDialogMoreApp() {
		try {
			Uri uri = Uri.parse("market://search?q=pub:\"Hibernate\"");
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			C.notAppFound(mContext);
		}
	}

	private void openDialogReport() {
		Intent intent = new Intent(Intent.ACTION_SEND);

		String text = "Android Version: " + Build.VERSION.RELEASE + "\n";
		text += "SDK Level: " + String.valueOf(Build.VERSION.SDK_INT) + "\n";
		text += "Version: " + AppUtils.getAppVersionName() + "\n";
		text += "Brand: " + Build.BRAND + "\n";
		text += "Model: " + Build.MODEL + "\n\n\n";

		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_EMAIL, BuildConfig.CONTACT_EMAIL);
		intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_title));
		intent.putExtra(Intent.EXTRA_TEXT, text);

		startActivity(Intent.createChooser(intent, getString(R.string.report)));
	}
}
