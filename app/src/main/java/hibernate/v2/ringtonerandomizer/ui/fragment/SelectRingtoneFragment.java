package hibernate.v2.ringtonerandomizer.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.R;
import hibernate.v2.ringtonerandomizer.helper.DBHelper;
import hibernate.v2.ringtonerandomizer.model.Ringtone;
import hibernate.v2.ringtonerandomizer.ui.adapter.RingtoneSelectAdapter;

public class SelectRingtoneFragment extends BaseFragment {

	protected final String[] PERMISSION_NAME = {
			Manifest.permission.READ_EXTERNAL_STORAGE
	};

	@BindView(R.id.rvlist)
	RecyclerView recyclerView;

	private ArrayList<Ringtone> allRingtoneList = new ArrayList<>();
	private ArrayList<Ringtone> shownRingtoneList = new ArrayList<>();
	private HashSet<String> hsFilter = new HashSet<>();
	private ArrayList<String> alFilter = new ArrayList<>();

	private String chosenPath = "";
	private MediaPlayer mediaPlayer;
	private boolean isPlaying = false;

	private DBHelper dbhelper;

	private SQLiteDatabase db;
	private RingtoneSelectAdapter ringtoneSelectAdapter;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_select_ringtone, container, false);
		ButterKnife.bind(this, rootView);
		return rootView;
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		openDatabase();

		recyclerView.setLayoutManager(new LinearLayoutManager(
				mContext, LinearLayoutManager.VERTICAL, false));

		ringtoneSelectAdapter = new RingtoneSelectAdapter(
				shownRingtoneList,
				this::openDialogPlayingPreview
		);
		recyclerView.setAdapter(ringtoneSelectAdapter);

		if (!isPermissionsGranted(PERMISSION_NAME)) {
			requestPermissions(PERMISSION_NAME, PERMISSION_REQUEST_CODE);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if (isPermissionsGranted(PERMISSION_NAME)) {
			new LoadTask(this).execute();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		closeDatabase();
	}

	@Override
	public void onPause() {
		stopPlaying();
		super.onPause();
	}

	private void openDatabase() {
		dbhelper = new DBHelper(mContext);
		db = dbhelper.getWritableDatabase();
	}

	private void closeDatabase() {
		db.close();
		dbhelper.close();
	}

	private static class SaveTask extends AsyncTask<Void, Void, Void> {
		private MaterialDialog dialog;
		private WeakReference<Fragment> fragmentWeakReference;

		private SaveTask(Fragment fragment) {
			fragmentWeakReference = new WeakReference<>(fragment);
		}

		public void onPreExecute() {
			super.onPreExecute();

			SelectRingtoneFragment fragment = (SelectRingtoneFragment) fragmentWeakReference.get();
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
		}

		@Override
		public Void doInBackground(Void... arg0) {
			SelectRingtoneFragment fragment = (SelectRingtoneFragment) fragmentWeakReference.get();
			if (fragment == null) {
				return null;
			}

			for (Ringtone ringtone : fragment.ringtoneSelectAdapter.getCheckedRingtoneList()) {
				fragment.dbhelper.insertDBRingtone(ringtone);
			}
			return null;
		}

		public void onPostExecute(Void un) {
			SelectRingtoneFragment fragment = (SelectRingtoneFragment) fragmentWeakReference.get();
			if (fragment == null) {
				return;
			}

			Activity activity = fragment.getActivity();
			assert activity != null;

			dialog.dismiss();
			Toast.makeText(activity, R.string.done, Toast.LENGTH_SHORT).show();
			activity.finish();
		}
	}

	private static class LoadTask extends AsyncTask<Void, Void, Void> {
		private MaterialDialog dialog;
		private WeakReference<Fragment> fragmentWeakReference;

		private LoadTask(Fragment fragment) {
			fragmentWeakReference = new WeakReference<>(fragment);
		}

		public void onPreExecute() {
			super.onPreExecute();

			SelectRingtoneFragment fragment = (SelectRingtoneFragment) fragmentWeakReference.get();
			if (fragment == null) {
				return;
			}

			Activity activity = fragment.getActivity();
			assert activity != null;

			fragment.allRingtoneList.clear();
			fragment.shownRingtoneList.clear();

			dialog = new MaterialDialog.Builder(activity)
					.content(R.string.wait)
					.progress(true, 0)
					.cancelable(false)
					.show();
		}

		@Override
		public Void doInBackground(Void... arg0) {
			SelectRingtoneFragment fragment = (SelectRingtoneFragment) fragmentWeakReference.get();
			if (fragment == null) {
				return null;
			}

			Activity activity = fragment.getActivity();
			assert activity != null;

			ArrayList<Ringtone> ringtones = new ArrayList<>(C.getDeviceRingtoneList(activity));
			fragment.allRingtoneList = new ArrayList<>(ringtones);
			fragment.shownRingtoneList = new ArrayList<>(ringtones);
			return null;
		}

		public void onPostExecute(Void un) {
			SelectRingtoneFragment fragment = (SelectRingtoneFragment) fragmentWeakReference.get();
			if (fragment == null) {
				return;
			}

			fragment.ringtoneSelectAdapter.refreshData(fragment.shownRingtoneList);
			dialog.dismiss();
		}
	}

	private void filterList() {
		alFilter.clear();
		hsFilter.clear();
		hsFilter.add("Internal Storage");
		for (Ringtone ringtone : allRingtoneList) {
			int posStart = 1;
			int posEnd;
			while (true) {
				if (ringtone.getPath() == null) {
					break;
				}
				posEnd = ringtone.getPath().indexOf("/", posStart);
				if (posStart == posEnd || posEnd == -1)
					break;
				posStart = posEnd + 1;
				String stringSub = ringtone.getPath().substring(0, posEnd + 1);
				hsFilter.add(stringSub);
			}
		}
		alFilter.addAll(hsFilter);
		Collections.sort(alFilter);

		ArrayList<String> alFilterRingtone = new ArrayList<>(alFilter);
		ArrayList<String> alFilterTemp = new ArrayList<>(alFilter);
		for (String element : alFilter) {
			if (!element.toLowerCase().contains("ringtone")) {
				alFilterRingtone.remove(element);
			} else {
				alFilterTemp.remove(element);
			}
		}
		alFilter.clear();
		alFilter.addAll(alFilterRingtone);
		alFilter.addAll(alFilterTemp);
	}

	@OnClick(R.id.saveBtn)
	public void onClickSaveList() {
		new SaveTask(this).execute();
	}

	@OnClick(R.id.filter_button)
	public void onClickFilterDialog() {
		filterList();
		MaterialDialog.Builder dialog = new MaterialDialog.Builder(mContext)
				.title(R.string.filter_title)
				.items(alFilter)
				.itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
					@Override
					public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
						chosenPath = text.toString();
						shownRingtoneList.clear();
						for (Ringtone ringtone : allRingtoneList) {
							if (ringtone.getPath() == null) {
								if (chosenPath.equals("Internal")) {
									shownRingtoneList.add(ringtone);
								}
							} else {
								if (ringtone.getPath().contains(chosenPath)) {
									shownRingtoneList.add(ringtone);
								}
							}
						}
						ringtoneSelectAdapter.refreshData(shownRingtoneList);
						return true;
					}
				})
				.negativeText(R.string.item_navbtn);
		dialog.show();
	}

	private void openDialogPlayingPreview(Ringtone ringtone) {
		try {
			mediaPlayer = MediaPlayer.create(mContext, Uri.parse(ringtone.getUriId()));
			mediaPlayer.setLooping(true);
			mediaPlayer.start();

			String message;
			if (ringtone.getPath() != null) {
				message = getString(R.string.item_message) + ringtone.getName()
						+ "\n\n" + getString(R.string.item_message2) + ringtone.getPath();
			} else {
				message = getString(R.string.item_message) + ringtone.getName()
						+ "\n\n" + getString(R.string.item_message2) + "Internal Storage";
			}

			MaterialDialog.Builder dialog = new MaterialDialog.Builder(mContext)
					.title(R.string.playing_sound)
					.content(message)
					.dismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							stopPlaying();
						}
					})
					.cancelable(false)
					.negativeText(R.string.item_navbtn);
			dialog.show();
		} catch (Exception e) {
			Toast.makeText(mContext, R.string.ui_error, Toast.LENGTH_SHORT).show();
		}
	}

	private void stopPlaying() {
		if (mediaPlayer != null) {
			if (isPlaying) {
				mediaPlayer.stop();
			}
			mediaPlayer.release();
			mediaPlayer = null;
			isPlaying = false;
		}
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
