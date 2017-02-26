package hibernate.v2.ringtonerandomizer.ui.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.R;
import hibernate.v2.ringtonerandomizer.model.Ringtone;
import hibernate.v2.ringtonerandomizer.ui.activity.MainActivity;
import hibernate.v2.ringtonerandomizer.ui.activity.SelectRingtoneActivity;
import hibernate.v2.ringtonerandomizer.ui.adapter.RingtoneSelectedAdapter;
import hibernate.v2.ringtonerandomizer.ui.custom.TelephonyInfo;
import hibernate.v2.ringtonerandomizer.utils.DBHelper;

public class MainFragment extends BaseFragment {

	@BindView(R.id.rvlist)
	RecyclerView recyclerView;
	@BindView(R.id.currentText)
	TextView currentText;

	private ArrayList<Ringtone> ringtoneList = new ArrayList<>();
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		ButterKnife.bind(this, rootView);
		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		recyclerView.setLayoutManager(new LinearLayoutManager(mContext,
				LinearLayoutManager.VERTICAL, false)
		);
		openDatabase();
		getCurrent();

		RingtoneSelectedAdapter.ItemClickListener mClickListener = new RingtoneSelectedAdapter.ItemClickListener() {
			@Override
			public void onItemDetailClick(Ringtone ringtone) {
				openDialogItem(ringtone.getPath());
			}
		};

		ringtoneSelectedAdapter = new RingtoneSelectedAdapter(ringtoneList, mClickListener);
		recyclerView.setAdapter(ringtoneSelectedAdapter);

		Bundle bundle = getArguments();
		if (bundle != null) {
			if (bundle.getBoolean("shortcut_action", false)) {
				onClickRandom();
			}
		}

		SharedPreferences setting = PreferenceManager.getDefaultSharedPreferences(mContext);
		TelephonyInfo telephonyInfo = TelephonyInfo.getInstance(mContext);
		if (telephonyInfo.isDualSIM() && telephonyInfo.isSIM2Ready() && setting.getBoolean("pref_dual_warning", true)) {
			Snackbar snackbar = Snackbar.make(currentText, R.string.dual_sim_warning, Snackbar.LENGTH_LONG);
			snackbar.setAction(R.string.ui_more, new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					((MainActivity) getActivity()).openDialogTutor();
				}
			});
			C.initSnackbar(snackbar).show();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		new GetAllSavedSongTask().execute();
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

	@OnClick(R.id.random_img)
	public void onClickRandom() {
		Toast.makeText(mContext,
				DBHelper.changeRingtone(db, mContext, null),
				Toast.LENGTH_SHORT).show();
		getCurrent();
	}

	@OnClick(R.id.add_img)
	public void onClickAddRingtone() {
		startActivity(new Intent().setClass(mContext, SelectRingtoneActivity.class));
	}

	private void getCurrent() {
		currentText.setText(C.getCurrentRingtoneName(mContext));
	}

	private class GetAllSavedSongTask extends AsyncTask<Void, Void, Void> {
		private MaterialDialog dialog;

		public void onPreExecute() {
			super.onPreExecute();
			dialog = new MaterialDialog.Builder(mContext)
					.content(R.string.wait)
					.progress(true, 0)
					.cancelable(false)
					.show();
			ringtoneList.clear();
		}

		@Override
		public Void doInBackground(Void... arg0) {
			ringtoneList.addAll(DBHelper.getDBSongList(db, mContext));
			return null;
		}

		public void onPostExecute(Void un) {
			ringtoneSelectedAdapter.notifyDataSetChanged();
			dialog.dismiss();
		}
	}

	@OnClick(R.id.clear_img)
	public void openDialogClearData() {
		MaterialDialog.Builder dialog = new MaterialDialog.Builder(mContext)
				.title(R.string.clear_title)
				.content(R.string.clear_message)
				.positiveText(R.string.clear_posbtn)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						DBHelper.clearDBSongList(db);
						new GetAllSavedSongTask().execute();
					}
				})
				.negativeText(R.string.clear_navbtn);
		dialog.show();
	}

	public void openDialogItem(final String path) {
		Ringtone bean = DBHelper.getDBSong(db, path);
		if (bean != null) {
			MaterialDialog.Builder dialog = new MaterialDialog.Builder(mContext)
					.title(R.string.item_title)
					.content(getString(R.string.item_message) + bean.getName()
							+ "\n\n" + getString(R.string.item_message2) + bean.getPath())
					.positiveText(R.string.item_posbtn)
					.onPositive(new MaterialDialog.SingleButtonCallback() {
						@Override
						public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
							DBHelper.changeRingtone(db, mContext, path);
							getCurrent();
						}
					})
					.neutralText(R.string.item_netbtn)
					.onNeutral(new MaterialDialog.SingleButtonCallback() {
						@Override
						public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
							DBHelper.deleteDBSong(db, path);
							new GetAllSavedSongTask().execute();
						}
					})
					.negativeText(R.string.item_navbtn);
			dialog.show();
		}
	}
}
