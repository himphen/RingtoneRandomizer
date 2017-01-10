package hibernate.v2.ringtonerandomizer.ui.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.R;
import hibernate.v2.ringtonerandomizer.model.Ringtone;
import hibernate.v2.ringtonerandomizer.ui.activity.SelectRingtoneActivity;
import hibernate.v2.ringtonerandomizer.ui.adapter.RingtoneSelectedAdapter;
import hibernate.v2.ringtonerandomizer.utils.DBHelper;

public class MainFragment extends BaseFragment {

	@BindView(R.id.rvlist)
	RecyclerView recyclerView;
	@BindView(R.id.currentText)
	TextView currentText;
	@BindView(R.id.random_img)
	ImageView randomImg;
	@BindView(R.id.add_img)
	ImageView addImg;
	@BindView(R.id.clear_img)
	ImageView clearImg;

	private ArrayList<Ringtone> ringtoneList = new ArrayList<>();
	private DBHelper dbhelper;
	private SQLiteDatabase db;
	private RingtoneSelectedAdapter ringtoneSelectedAdapter;

	public MainFragment() {
		// Required empty public constructor
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
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		recyclerView.setLayoutManager(
				new LinearLayoutManager(mContext,
						LinearLayoutManager.VERTICAL, false)
		);
		randomImg.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				random();
			}
		});
		addImg.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				startActivity(new Intent().setClass(mContext,
						SelectRingtoneActivity.class));
			}
		});
		clearImg.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				openDialogClearData();
			}
		});
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


	private void random() {
		Toast.makeText(mContext,
				DBHelper.changeRingtone(db, mContext, null),
				Toast.LENGTH_LONG).show();
		getCurrent();
	}

	private void getCurrent() {
		currentText.setText(C.getCurrentRingtoneName(mContext));
	}

	private class GetAllSavedSongTask extends AsyncTask<Void, Void, Void> {
		private ProgressDialog dialog = new ProgressDialog(mContext);

		public void onPreExecute() {
			super.onPreExecute();
			dialog.setCancelable(false);
			dialog.setMessage(getString(R.string.wait));
			dialog.show();
		}

		@Override
		public Void doInBackground(Void... arg0) {
			ringtoneList.clear();
			ringtoneList.addAll(DBHelper.getDBSongList(db, mContext));
			return null;
		}

		public void onPostExecute(Void un) {
			ringtoneSelectedAdapter.notifyDataSetChanged();
			dialog.dismiss();
		}
	}

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
						Toast.makeText(mContext,
								R.string.clear_done, Toast.LENGTH_SHORT).show();
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
							Toast.makeText(mContext,
									R.string.item_done, Toast.LENGTH_SHORT).show();
							ringtoneSelectedAdapter.notifyDataSetChanged();
						}
					})
					.neutralText(R.string.item_netbtn)
					.onNeutral(new MaterialDialog.SingleButtonCallback() {
						@Override
						public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
							DBHelper.deleteDBSong(db, path);
							new GetAllSavedSongTask().execute();
							Toast.makeText(mContext,
									R.string.clear_done, Toast.LENGTH_SHORT).show();
							ringtoneSelectedAdapter.notifyDataSetChanged();
						}
					})
					.negativeText(R.string.item_navbtn);
			dialog.show();
		}
	}
}
