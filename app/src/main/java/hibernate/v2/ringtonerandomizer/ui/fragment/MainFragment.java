package hibernate.v2.ringtonerandomizer.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.R;
import hibernate.v2.ringtonerandomizer.helper.DBHelper;
import hibernate.v2.ringtonerandomizer.model.Ringtone;
import hibernate.v2.ringtonerandomizer.ui.activity.SelectRingtoneActivity;
import hibernate.v2.ringtonerandomizer.ui.adapter.RingtoneSelectedAdapter;

public class MainFragment extends BaseFragment {

	protected final String[] PERMISSION_NAME = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.READ_PHONE_STATE
	};

	@BindView(R.id.rvlist)
	RecyclerView recyclerView;
	@BindView(R.id.currentText)
	TextView currentText;

	private ArrayList<Ringtone> currentRingtoneList = new ArrayList<>();

	private DBHelper dbhelper;
	private RingtoneSelectedAdapter ringtoneSelectedAdapter;

	public static MainFragment getInstance(boolean isShortcutUpdate) {
		MainFragment fragment = new MainFragment();

		Bundle args = new Bundle();
		args.putBoolean("shortcut_action", isShortcutUpdate);
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		ButterKnife.bind(this, rootView);
		return rootView;
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		dbhelper = new DBHelper(mContext);

		recyclerView.setLayoutManager(new LinearLayoutManager(
				mContext, LinearLayoutManager.VERTICAL, false));

		ringtoneSelectedAdapter = new RingtoneSelectedAdapter(
				currentRingtoneList,
				this::openDialogItem
		);
		recyclerView.setAdapter(ringtoneSelectedAdapter);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);
			assert notificationManager != null;

			if (notificationManager.getNotificationChannel("Changed Notification") == null) {
				NotificationChannel channel = new NotificationChannel("Changed Notification",
						mContext.getString(R.string.pref_title_changed_notification),
						NotificationManager.IMPORTANCE_LOW);
				channel.setDescription(mContext.getString(R.string.pref_des_changed_notificationOn));
				notificationManager.createNotificationChannel(channel);
			}
		}

		if (!isPermissionsGranted(PERMISSION_NAME)) {
			requestPermissions(PERMISSION_NAME, PERMISSION_REQUEST_CODE);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!Settings.System.canWrite(mContext)) {
				C.openErrorSystemPermissionDialog(mContext);
			}
		} else {
			AppUpdater appUpdater = new AppUpdater(mContext)
					.showEvery(4)
					.setDisplay(Display.NOTIFICATION);
			appUpdater.start();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if (isPermissionsGranted(PERMISSION_NAME)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (!Settings.System.canWrite(mContext)) {
					C.openErrorSystemPermissionDialog(mContext);
					return;
				}
			}
			getCurrent();

			Bundle bundle = getArguments();
			if (bundle != null) {
				if (bundle.getBoolean("shortcut_action", false)) {
					onClickRandom();
				}
			}

			new GetSavedRingtoneTask(this).execute();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		dbhelper.close();
	}

	@OnClick(R.id.randomIv)
	public void onClickRandom() {
		String message;
		int result = dbhelper.changeRingtone(mContext, null);
		switch (result) {
			case DBHelper.CHANGE_RINGTONE_RESULT_SUCCESS:
				message = getString(R.string.changed_ringtone);
				break;
			case DBHelper.CHANGE_RINGTONE_RESULT_COUNT_ZERO:
				message = getString(R.string.notyet);
				break;
			case DBHelper.CHANGE_RINGTONE_RESULT_COUNT_ONE:
				message = getString(R.string.changed_ringtone_one);
				break;
			case DBHelper.CHANGE_RINGTONE_RESULT_PERMISSION:
			default:
				message = getString(R.string.change_ringtone_result_permission);
				break;
		}

		Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
		getCurrent();
	}

	@OnClick(R.id.addIv)
	public void onClickAddRingtone() {
		startActivity(new Intent().setClass(mContext, SelectRingtoneActivity.class));
	}

	private void getCurrent() {
		currentText.setText(C.getCurrentRingtoneName(mContext));
	}

	private static class GetSavedRingtoneTask extends AsyncTask<Void, Void, Void> {
		private MaterialDialog dialog;
		private WeakReference<Fragment> fragmentWeakReference;

		private GetSavedRingtoneTask(Fragment fragment) {
			fragmentWeakReference = new WeakReference<>(fragment);
		}

		public void onPreExecute() {
			super.onPreExecute();
			MainFragment fragment = (MainFragment) fragmentWeakReference.get();
			if (fragment == null) {
				return;
			}

			Activity activity = fragment.getActivity();
			assert activity != null;

			dialog = new MaterialDialog.Builder(activity)
					.content(R.string.wait)
					.progress(true, 0)
					.cancelable(false)
					.show();
			fragment.currentRingtoneList.clear();
		}

		@Override
		public Void doInBackground(Void... arg0) {
			MainFragment fragment = (MainFragment) fragmentWeakReference.get();
			if (fragment == null) {
				return null;
			}

			Activity activity = fragment.getActivity();
			assert activity != null;

			fragment.currentRingtoneList.addAll(fragment.dbhelper.getDBRingtoneList(activity));
			return null;
		}

		public void onPostExecute(Void un) {
			MainFragment fragment = (MainFragment) fragmentWeakReference.get();
			if (fragment == null) {
				return;
			}

			Activity activity = fragment.getActivity();
			assert activity != null;

			fragment.ringtoneSelectedAdapter.notifyDataSetChanged();
			dialog.dismiss();
		}
	}

	@OnClick(R.id.clearIv)
	public void openDialogClearData() {
		MaterialDialog.Builder dialog = new MaterialDialog.Builder(mContext)
				.title(R.string.clear_title)
				.content(R.string.clear_message)
				.positiveText(R.string.clear_posbtn)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						dbhelper.clearDBRingtoneList();
						new GetSavedRingtoneTask(MainFragment.this).execute();
					}
				})
				.negativeText(R.string.clear_navbtn);
		dialog.show();
	}

	public void openDialogItem(Ringtone ringtone) {
		String message;
		if (ringtone.getPath() != null) {
			message = getString(R.string.item_message) + ringtone.getName()
					+ "\n\n" + getString(R.string.item_message2) + ringtone.getPath();
		} else {
			message = getString(R.string.item_message) + ringtone.getName()
					+ "\n\n" + getString(R.string.item_message2) + "Internal Storage";
		}

		MaterialDialog.Builder dialog = new MaterialDialog.Builder(mContext)
				.title(R.string.item_title)
				.content(message)
				.positiveText(R.string.item_posbtn)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						dbhelper.changeRingtone(mContext, ringtone);
						getCurrent();
					}
				})
				.neutralText(R.string.item_netbtn)
				.onNeutral(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						dbhelper.deleteDBRingtone(ringtone.getUriId());
						new GetSavedRingtoneTask(MainFragment.this).execute();
					}
				})
				.negativeText(R.string.item_navbtn);
		dialog.show();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == PERMISSION_REQUEST_CODE) {
			if (!hasAllPermissionsGranted(grantResults)) {
				C.openErrorPermissionDialog(mContext);
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (requestCode == 500 && !Settings.System.canWrite(mContext)) {
				mContext.finish();
			}
		}
	}
}
