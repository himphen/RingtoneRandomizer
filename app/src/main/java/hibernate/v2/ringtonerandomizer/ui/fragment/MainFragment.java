package hibernate.v2.ringtonerandomizer.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

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

	private SQLiteDatabase db;
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
		openDatabase();

		recyclerView.setLayoutManager(new LinearLayoutManager(
				mContext, LinearLayoutManager.VERTICAL, false));

		ringtoneSelectedAdapter = new RingtoneSelectedAdapter(
				currentRingtoneList,
				this::openDialogItem
		);
		recyclerView.setAdapter(ringtoneSelectedAdapter);

		if (!isPermissionsGranted(PERMISSION_NAME)) {
			requestPermissions(PERMISSION_NAME, PERMISSION_REQUEST_CODE);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if (isPermissionsGranted(PERMISSION_NAME)) {
			getCurrent();

			Bundle bundle = getArguments();
			if (bundle != null) {
				if (bundle.getBoolean("shortcut_action", false)) {
					onClickRandom();
				}
			}

			new GetAllSavedSongTask(this).execute();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		closeDatabase();
	}

	private void openDatabase() {
		dbhelper = new DBHelper(mContext);
		db = dbhelper.getWritableDatabase();
	}

	private void closeDatabase() {
		db.close();
		dbhelper.close();
	}

	@OnClick(R.id.randomIv)
	public void onClickRandom() {
		Toast.makeText(mContext,
				DBHelper.changeRingtone(db, mContext, null),
				Toast.LENGTH_SHORT).show();
		getCurrent();
	}

	@OnClick(R.id.addIv)
	public void onClickAddRingtone() {
		startActivity(new Intent().setClass(mContext, SelectRingtoneActivity.class));
	}

	private void getCurrent() {
		currentText.setText(C.getCurrentRingtoneName(mContext));
	}

	private static class GetAllSavedSongTask extends AsyncTask<Void, Void, Void> {
		private MaterialDialog dialog;
		private WeakReference<Fragment> fragmentWeakReference;

		private GetAllSavedSongTask(Fragment fragment) {
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

			fragment.currentRingtoneList.addAll(DBHelper.getDBRingtoneList(fragment.db, activity));
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
						DBHelper.clearDBRingtoneList(db);
						new GetAllSavedSongTask(MainFragment.this).execute();
					}
				})
				.negativeText(R.string.clear_navbtn);
		dialog.show();
	}

	public void openDialogItem(Ringtone ringtone) {
		MaterialDialog.Builder dialog = new MaterialDialog.Builder(mContext)
				.title(R.string.item_title)
				.content(getString(R.string.item_message) + ringtone.getName()
						+ "\n\n" + getString(R.string.item_message2) + ringtone.getPath())
				.positiveText(R.string.item_posbtn)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						DBHelper.changeRingtone(db, mContext, ringtone.getPath());
						getCurrent();
					}
				})
				.neutralText(R.string.item_netbtn)
				.onNeutral(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						DBHelper.deleteDBRingtone(db, ringtone.getPath());
						new GetAllSavedSongTask(MainFragment.this).execute();
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
}
