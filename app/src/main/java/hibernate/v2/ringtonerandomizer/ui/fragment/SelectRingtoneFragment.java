package hibernate.v2.ringtonerandomizer.ui.fragment;

import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.R;
import hibernate.v2.ringtonerandomizer.model.Ringtone;
import hibernate.v2.ringtonerandomizer.ui.adapter.RingtoneSelectAdapter;
import hibernate.v2.ringtonerandomizer.utils.DBHelper;

public class SelectRingtoneFragment extends BaseFragment {

	@BindView(R.id.rvlist)
	RecyclerView recyclerView;

	private ArrayList<Ringtone> chosenSongList = new ArrayList<>();
	private ArrayList<Ringtone> allSongList = new ArrayList<>();
	private ArrayList<Ringtone> showList = new ArrayList<>();
	private HashSet<String> hsFilter = new HashSet<>();
	private ArrayList<String> alFilter = new ArrayList<>();

	private String chosenPath = "";
	private MediaPlayer mediaPlayer;
	private boolean isPlaying = false;

	private DBHelper dbhelper;

	private SQLiteDatabase db;
	private RingtoneSelectAdapter ringtoneSelectAdapter;

	public SelectRingtoneFragment() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for mContext fragment
		View rootView = inflater.inflate(R.layout.fragment_select_ringtone, container, false);
		ButterKnife.bind(this, rootView);
		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		openDatabase();

		recyclerView.setLayoutManager(new LinearLayoutManager(
				mContext, LinearLayoutManager.VERTICAL, false));

		new LoadTask().execute();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		closeDatabase();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopPlaying();
	}

	private void openDatabase() {
		dbhelper = new DBHelper(mContext);
		db = dbhelper.getWritableDatabase();
	}

	private void closeDatabase() {
		db.close();
		dbhelper.close();
	}

	private class SaveTask extends AsyncTask<Void, Void, Void> {
		private MaterialDialog dialog;

		public void onPreExecute() {
			super.onPreExecute();
			dialog = new MaterialDialog.Builder(mContext)
					.content(R.string.wait)
					.progress(true, 0)
					.cancelable(false)
					.show();
		}

		@Override
		public Void doInBackground(Void... arg0) {
			for (Ringtone ringtone : chosenSongList) {
				DBHelper.insertDBSong(db, ringtone);
			}
			return null;
		}

		public void onPostExecute(Void un) {
			dialog.dismiss();
			Toast.makeText(mContext, R.string.done,
					Toast.LENGTH_SHORT).show();
			mContext.finish();
		}
	}

	private class LoadTask extends AsyncTask<Void, Void, Void> {
		private MaterialDialog dialog;

		public void onPreExecute() {
			super.onPreExecute();
			dialog = new MaterialDialog.Builder(mContext)
					.content(R.string.wait)
					.progress(true, 0)
					.cancelable(false)
					.show();
			allSongList.clear();
			showList.clear();
		}

		@Override
		public Void doInBackground(Void... arg0) {
			allSongList = new ArrayList<>(C.getDeviceSongList(mContext));
			return null;
		}

		public void onPostExecute(Void un) {
			showList = new ArrayList<>(allSongList);

			RingtoneSelectAdapter.ItemClickListener mClickListener = new RingtoneSelectAdapter.ItemClickListener() {
				@Override
				public void onItemDetailClick(Ringtone ringtone) {
					openDialogPlayingPreview(ringtone.getPath());
				}
			};

			RingtoneSelectAdapter.ItemCheckListener mCheckListener = new RingtoneSelectAdapter.ItemCheckListener() {
				@Override
				public void onItemDetailCheck(Ringtone ringtone, boolean isChecked) {
					if (isChecked) {
						ringtoneSelectAdapter.addRingtone(ringtone);
					} else {
						ringtoneSelectAdapter.removeRingtone(ringtone);
					}
				}
			};

			ringtoneSelectAdapter = new RingtoneSelectAdapter(showList, mClickListener, mCheckListener);
			recyclerView.setAdapter(ringtoneSelectAdapter);
			dialog.dismiss();
		}
	}

	private void filterList() {
		alFilter.clear();
		hsFilter.clear();
		for (Ringtone ringtone : allSongList) {
			int posStart = 1;
			int posEnd;
			while (true) {
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
		HashMap<String, Ringtone> ringtoneHashMap = ringtoneSelectAdapter.getSelectedRingtoneMap();
		for (Map.Entry<String, Ringtone> ringtone : ringtoneHashMap.entrySet()) {
			chosenSongList.add(ringtone.getValue());
		}
		new SaveTask().execute();
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
						Log.i("chosenPath", chosenPath);
						showList.clear();
						for (Ringtone ringtone : allSongList) {
							if (ringtone.getPath().contains(chosenPath)) {
								showList.add(ringtone);
							}
						}
						ringtoneSelectAdapter.notifyDataSetChanged();
						return true;
					}
				})
				.negativeText(R.string.item_navbtn);
		dialog.show();
	}

	private void openDialogPlayingPreview(String string) {
		try {
			mediaPlayer = MediaPlayer.create(mContext,
					Uri.parse("content://media/external/audio/media/" + DBHelper.getIDByPath(mContext, string)));
			mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer arg0) {
					stopPlaying();
				}
			});
			mediaPlayer.setLooping(true);
			mediaPlayer.start();
			MaterialDialog.Builder dialog = new MaterialDialog.Builder(mContext)
					.content(R.string.playing_sound)
					.dismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							stopPlaying();
						}
					})
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
}
