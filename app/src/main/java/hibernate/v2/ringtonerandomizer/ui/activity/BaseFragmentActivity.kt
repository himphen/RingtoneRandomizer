package hibernate.v2.ringtonerandomizer.ui.activity

import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import hibernate.v2.ringtonerandomizer.helper.UtilHelper.detectLanguage
import kotlinx.android.synthetic.main.toolbar.*

/**
 * Created by himphen on 21/5/16.
 */
abstract class BaseFragmentActivity : BaseActivity() {
    open var fragment: Fragment? = null
    open var titleId: Int? = null
    open var titleString: String? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        detectLanguage(this)
        initActionBar(toolbar, titleString = titleString, titleId = titleId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragment?.let { fragment ->
            initFragment(fragment, titleString = titleString, titleId = titleId)
        }
    }
}