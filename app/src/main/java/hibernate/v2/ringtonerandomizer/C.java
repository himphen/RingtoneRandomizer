package hibernate.v2.ringtonerandomizer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import hibernate.v2.ringtonerandomizer.helper.UtilHelper;
import hibernate.v2.ringtonerandomizer.model.Ringtone;

public class C extends UtilHelper {

	public static void openErrorPermissionDialog(Context mContext) {
		MaterialDialog.Builder dialog = new MaterialDialog.Builder(mContext)
				.title(R.string.ui_caution)
				.customView(R.layout.dialog_permission, true)
				.cancelable(false)
				.negativeText(R.string.ui_cancel)
				.positiveText(R.string.dialog_permission_denied_posbtn)
				.onNegative(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						Activity activity = scanForActivity(dialog.getContext());
						if (activity != null) {
							activity.finish();
						}
					}
				})
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						Activity activity = scanForActivity(dialog.getContext());
						if (activity != null) {
							try {
								Intent intent = new Intent();
								intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
								intent.addCategory(Intent.CATEGORY_DEFAULT);
								intent.setData(Uri.parse("package:" + activity.getPackageName()));
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
								intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
								activity.startActivity(intent);
								activity.finish();
							} catch (Exception e) {
								Intent intent = new Intent();
								intent.setAction(Settings.ACTION_APPLICATION_SETTINGS);
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
								intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
								activity.startActivity(intent);
								activity.finish();
							}
						}
					}
				});
		dialog.show();
	}

	public static ArrayList<Ringtone> getDeviceRingtoneList(Context context) {
		ArrayList<Ringtone> allSongList = new ArrayList<>();
		try {
			ContentResolver contentResolver = context.getContentResolver();
			String selection1 = MediaStore.Audio.Media.IS_RINGTONE + " != 0";
			String selection2 = MediaStore.Audio.Media.IS_MUSIC + " != 0";
			String selection3 = MediaStore.Audio.Media.IS_NOTIFICATION + " != 0";
			String selection4 = MediaStore.Audio.Media.IS_ALARM + " != 0";

			String selection = selection1 + " OR " + selection2 + " OR " + selection3 + " OR " + selection4;

			String[] projection = {MediaStore.Audio.Media.TITLE,
					MediaStore.Audio.Media.DATA, MediaStore.Audio.Media._ID};

			Cursor cursor = contentResolver.query(
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
					selection, null, null);

			if (cursor != null) {
				while (cursor.moveToNext()) {
					Ringtone ringtone = new Ringtone();
					ringtone.setName(cursor.getString(0));
					ringtone.setPath(cursor.getString(1));
					ringtone.setMusicId(cursor.getString(2));
					allSongList.add(ringtone);

					C.debug("Add ringtone: " + ringtone);
				}
				cursor.close();
			}

			Collections.sort(allSongList, new Comparator<Ringtone>() {
				@Override
				public int compare(Ringtone o1, Ringtone o2) {
					return o1.getPath().compareTo(o2.getPath());
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
		return allSongList;
	}

	public static String getCurrentRingtoneName(Context context) {
		try {
			return RingtoneManager.getRingtone(context,
					Settings.System.DEFAULT_RINGTONE_URI).getTitle(context);
		} catch (Exception e) {
			return "DRM Error";
		}
	}

	public static Snackbar initSnackbar(Snackbar snackbar) {
		View sbView = snackbar.getView();
		sbView.setBackgroundResource(R.color.primary_dark);
		((TextView) sbView.findViewById(android.support.design.R.id.snackbar_text)).setTextColor(Color.WHITE);
		((TextView) sbView.findViewById(android.support.design.R.id.snackbar_action)).setTextColor(Color.YELLOW);
		return snackbar;
	}

	public static void notAppFound(Activity mContext) {
		Toast.makeText(mContext, R.string.app_not_found, Toast.LENGTH_LONG).show();
		mContext.finish();
	}
}
