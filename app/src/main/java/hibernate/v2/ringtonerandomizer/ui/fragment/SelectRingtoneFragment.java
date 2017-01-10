package hibernate.v2.ringtonerandomizer.ui.fragment;

import android.app.ProgressDialog;
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
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.R;
import hibernate.v2.ringtonerandomizer.model.Ringtone;
import hibernate.v2.ringtonerandomizer.ui.adapter.RingtoneChooseAdapter;
import hibernate.v2.ringtonerandomizer.utils.DBHelper;

public class SelectRingtoneFragment extends BaseFragment {

	@BindView(R.id.get_value)
	Button getValue;
	@BindView(R.id.filter_button)
	Button filterButton;
	@BindView(R.id.rvlist)
	RecyclerView recyclerView;

	private ArrayList<Ringtone> chosenSongList = new ArrayList<>();
	private ArrayList<Ringtone> allSongList, showList;
	private HashSet<String> hsFilter = new HashSet<>();
	private ArrayList<String> alFilter = new ArrayList<>();

	private String chosenPath = "";
	private MediaPlayer mediaPlayer;
	private boolean isPlaying = false;

	private DBHelper dbhelper;

	private SQLiteDatabase db;
	private RingtoneChooseAdapter ringtoneChooseAdapter;

	public SelectRingtoneFragment() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for mContext fragment
		View rootView = inflater.inflate(R.layout.fragment_choose, container, false);
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
		openDatabase();

		recyclerView.setLayoutManager(
				new LinearLayoutManager(mContext,
						LinearLayoutManager.VERTICAL, false)
		);

		filterButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openDialogFilter();
			}
		});

		getValue.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				HashMap<String, Ringtone> ringtoneHashMap = ringtoneChooseAdapter.getSelectedRingtoneMap();
				for (Map.Entry<String, Ringtone> ringtone : ringtoneHashMap.entrySet()) {
					chosenSongList.add(ringtone.getValue());
				}
				new SaveTask().execute();
			}
		});

		allSongList = new ArrayList<>(C.getDeviceSongList(mContext));
		showList = new ArrayList<>(allSongList);

		RingtoneChooseAdapter.ItemClickListener mClickListener = new RingtoneChooseAdapter.ItemClickListener() {
			@Override
			public void onItemDetailClick(Ringtone ringtone) {
				openDialogPlayingPreview(ringtone.getPath());
			}
		};

		RingtoneChooseAdapter.ItemCheckListener mCheckListener = new RingtoneChooseAdapter.ItemCheckListener() {
			@Override
			public void onItemDetailCheck(Ringtone ringtone, boolean isChecked) {
				if (isChecked) {
					ringtoneChooseAdapter.addRingtone(ringtone);
				} else {
					ringtoneChooseAdapter.removeRingtone(ringtone);
				}
			}
		};

		ringtoneChooseAdapter = new RingtoneChooseAdapter(showList, mClickListener, mCheckListener);
		recyclerView.setAdapter(ringtoneChooseAdapter);
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
		private ProgressDialog dialog = new ProgressDialog(mContext);

		public void onPreExecute() {
			super.onPreExecute();
			dialog.setCancelable(false);
			dialog.setMessage(getString(R.string.wait));
			dialog.show();
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

		for (String element : alFilter) {
			Log.i("alFilter", element);
		}
	}

	private void openDialogFilter() {
//		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//		builder.setTitle(R.string.filter_title);
//		filterList();
//		ListView modeList = new ListView(mContext);
//		final ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(mContext,
//				android.R.layout.simple_list_item_1, android.R.id.text1,
//				alFilter);
//		modeList.setAdapter(modeAdapter);
//		modeList.setTextFilterEnabled(true);
//		modeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> parent, View v,
//			                        int position, long id) {
//				chosenPath = parent.getItemAtPosition(position).toString();
//				Log.i("CM", chosenPath);
//				showList.clear();
//				for (Ringtone ringtone : allSongList) {
//					if (ringtone.getPath().contains(chosenPath)) {
//						showList.add(ringtone);
//					}
//				}
//				ringtoneChooseAdapter.notifyDataSetChanged();
//				dialog.dismiss();
//			}
//		});
//		builder.setView(modeList);
//		dialog.show();

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
						ringtoneChooseAdapter.notifyDataSetChanged();
						return true;
					}
				})
				.negativeText(R.string.item_navbtn);
		dialog.show();
	}

	private void openDialogPlayingPreview(String map) {
		try {
			mediaPlayer = MediaPlayer.create(
					mContext,
					Uri.parse("content://media/external/audio/media/"
							+ DBHelper.getIDByPath(mContext, map)));
			mediaPlayer
					.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
						@Override
						public void onCompletion(MediaPlayer arg0) {
							stopPlaying();
						}
					});
			mediaPlayer.start();
			MaterialDialog.Builder dialog = new MaterialDialog.Builder(mContext)
					.content(R.string.playing_sound)
					.cancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							stopPlaying();
						}
					})
					.dismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							stopPlaying();
						}
					})
					.negativeText(R.string.item_navbtn);
			dialog.show();
		} catch (Exception e) {
			MaterialDialog.Builder dialog = new MaterialDialog.Builder(mContext)
					.content(R.string.playing_sound)
					.negativeText(R.string.item_navbtn);
			dialog.show();
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
