package hibernate.v2.ringtonerandomizer.ui.activity

import androidx.fragment.app.Fragment
import hibernate.v2.ringtonerandomizer.R
import hibernate.v2.ringtonerandomizer.ui.fragment.SelectRingtoneFragment

class SelectRingtoneActivity : BaseFragmentActivity() {
    override var fragment: Fragment? = SelectRingtoneFragment()
    override var titleId: Int? = R.string.title_activity_settings
}