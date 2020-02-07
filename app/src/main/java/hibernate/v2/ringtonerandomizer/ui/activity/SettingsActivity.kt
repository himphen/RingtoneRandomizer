package hibernate.v2.ringtonerandomizer.ui.activity

import androidx.fragment.app.Fragment
import hibernate.v2.ringtonerandomizer.R
import hibernate.v2.ringtonerandomizer.ui.fragment.SettingsFragment

class SettingsActivity : BaseFragmentActivity() {
    override var fragment: Fragment? = SettingsFragment()
    override var titleId: Int? = R.string.title_activity_settings
}