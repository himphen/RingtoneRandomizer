package hibernate.v2.ringtonerandomizer.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.view.menu.ActionMenuItem;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.google.android.gms.ads.AdView;

import butterknife.BindView;
import butterknife.ButterKnife;
import hibernate.v2.ringtonerandomizer.BuildConfig;
import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.R;
import hibernate.v2.ringtonerandomizer.ui.fragment.MainFragment;

public class MainActivity extends BaseActivity {

	@BindView(R.id.toolbar)
	Toolbar toolbar;

	private AdView adView;

	@BindView(R.id.adLayout)
	RelativeLayout adLayout;
	private BillingProcessor billingProcessor;
	private SharedPreferences settingDefault;


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		C.detectLanguage(mContext);

		ActionBar ab = initActionBar(getSupportActionBar(), R.string.app_name);
		ab.setDisplayHomeAsUpEnabled(false);
		ab.setHomeButtonEnabled(false);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_container_adview);
		settingDefault = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		ButterKnife.bind(this);
		setSupportActionBar(toolbar);

		ActionBar ab = initActionBar(getSupportActionBar(), R.string.app_name);
		ab.setDisplayHomeAsUpEnabled(false);
		ab.setHomeButtonEnabled(false);

		C.forceShowMenu(mContext);

		billingProcessor = new BillingProcessor(mContext, BuildConfig.GOOGLE_IAP_KEY,
				new BillingProcessor.IBillingHandler() {
					@Override
					public void onProductPurchased(String productId, TransactionDetails details) {
						if (productId.equals(C.IAP_PID)) {
							settingDefault.edit().putBoolean(C.PREF_IAP, true).apply();
							MaterialDialog.Builder dialog = new MaterialDialog.Builder(mContext)
									.title(R.string.iab_complete_title)
									.customView(R.layout.dialog_donate, true)
									.positiveText(R.string.ui_okay);
							dialog.show();
						}
					}

					@Override
					public void onPurchaseHistoryRestored() {
						if (billingProcessor.isPurchased(C.IAP_PID)) {
							settingDefault.edit().putBoolean(C.PREF_IAP, true).apply();
						}
					}

					@Override
					public void onBillingError(int errorCode, Throwable error) {
					}

					@Override
					public void onBillingInitialized() {
					}
				});

		adView = C.initAdView(mContext, adLayout);

		Fragment fragment = new MainFragment();
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction()
				.replace(R.id.container, fragment)
				.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);

		if (!PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(C.PREF_IAP, false)) {
			MenuItem adsItem = menu.add(0, 1334, 0, R.string.action_iap);
			adsItem.setShowAsAction(ActionMenuItem.SHOW_AS_ACTION_ALWAYS);
			adsItem.setIcon(R.drawable.ic_local_play_white_24dp);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case 1334:
				checkPayment();
				break;
			case R.id.action_settings:
				Intent intent = new Intent().setClass(mContext,
						SettingsActivity.class);
				startActivity(intent);
				break;
			case R.id.action_qanda:
				openDialogTutor();
				break;
			case R.id.action_share:
				share();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void share() {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_TEXT,
				getResources().getString(R.string.share_message));
		intent.setType("text/plain");
		startActivity(Intent.createChooser(intent,
				getResources().getString(R.string.share_button)));
	}

	private void openDialogTutor() {
		MaterialDialog.Builder dialog = new MaterialDialog.Builder(mContext)
				.title(R.string.qanda)
				.content(R.string.qanda_message)
				.negativeText(R.string.update_navbtn);
		dialog.show();
	}

	@Override
	public void onDestroy() {
		if (adView != null) {
			adView.removeAllViews();
			adView.destroy();
		}

		if (billingProcessor != null)
			billingProcessor.release();

		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (!billingProcessor.handleActivityResult(requestCode, resultCode, data)) {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void checkPayment() {
		boolean isAvailable = BillingProcessor.isIabServiceAvailable(mContext);
		if (isAvailable) {
			billingProcessor.purchase(mContext, C.IAP_PID);
		} else {
			Toast.makeText(mContext, R.string.ui_error, Toast.LENGTH_LONG).show();
		}
	}

}
