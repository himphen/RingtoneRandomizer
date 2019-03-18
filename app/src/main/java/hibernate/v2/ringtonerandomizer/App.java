package hibernate.v2.ringtonerandomizer;

import android.app.Application;
import android.content.Context;

import com.blankj.utilcode.util.Utils;

/**
 * Created by himphen on 24/5/16.
 */

public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		Utils.init(this);
	}

}