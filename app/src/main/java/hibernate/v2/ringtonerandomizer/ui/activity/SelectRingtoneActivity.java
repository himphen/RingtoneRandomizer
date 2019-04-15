package hibernate.v2.ringtonerandomizer.ui.activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.widget.Toolbar;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdView;

import butterknife.BindView;
import butterknife.ButterKnife;
import hibernate.v2.ringtonerandomizer.C;
import hibernate.v2.ringtonerandomizer.R;
import hibernate.v2.ringtonerandomizer.ui.fragment.SelectRingtoneFragment;

public class SelectRingtoneActivity extends BaseActivity {

	@BindView(R.id.toolbar)
	Toolbar toolbar;

	private AdView adView;

	@BindView(R.id.adLayout)
	RelativeLayout adLayout;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		C.detectLanguage(mContext);
		initActionBar(getSupportActionBar(), R.string.title_activity_choose);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_container_adview);
		ButterKnife.bind(this);
		setSupportActionBar(toolbar);

		initActionBar(getSupportActionBar(), R.string.title_activity_choose);

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				adView = C.initAdView(mContext, adLayout);
			}
		}, DELAY_AD_LAYOUT);

		Fragment fragment = new SelectRingtoneFragment();
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction()
				.replace(R.id.container, fragment)
				.commit();

	}

	@Override
	public void onDestroy() {
		if (adView != null) {
			adView.removeAllViews();
			adView.destroy();
		}
		super.onDestroy();
	}
}