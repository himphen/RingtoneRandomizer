package hibernate.v2.ringtonerandomizer.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.view.View;

import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.R;

public class SettingsFragment extends BasePreferenceFragment {

	public SettingsFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_general);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Preference prefReport = findPreference("pref_report");
		Preference prefMoreApp = findPreference("pref_more_app");
		Preference prefVersion = findPreference("pref_version");

		prefVersion.setSummary(C.getCurrentVersionName(mContext));

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
	}

	private void openDialogMoreApp() {
		Uri uri = Uri.parse("market://search?q=pub:\"Hibernate\"");
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}

	private void openDialogReport() {
		Intent intent = new Intent(Intent.ACTION_SEND);
		String[] tos = {"hibernatev2@gmail.com"};
		intent.putExtra(Intent.EXTRA_EMAIL, tos);
		intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_title));
		intent.putExtra(Intent.EXTRA_TEXT, Build.BRAND + " " + Build.DEVICE
				+ " " + Build.VERSION.RELEASE + " " + "\n\n"
				+ getString(R.string.report_subject) + "\n\n");
		intent.setType("message/rfc822");
		startActivity(Intent.createChooser(intent, getString(R.string.report)));
	}
}
