package hibernate.v2.ringtonerandomizer.ui.activity

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.view.menu.ActionMenuItem
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.BillingProcessor.IBillingHandler
import com.anjlab.android.iab.v3.TransactionDetails
import hibernate.v2.ringtonerandomizer.BuildConfig
import hibernate.v2.ringtonerandomizer.R
import hibernate.v2.ringtonerandomizer.helper.UtilHelper
import hibernate.v2.ringtonerandomizer.ui.fragment.MainFragment

class MainActivity : BaseFragmentActivity() {
    override var titleId: Int? = R.string.app_name

    private lateinit var billingProcessor: BillingProcessor
    private lateinit var sharedPreferences: SharedPreferences

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val isShortcutUpdate = intent.getBooleanExtra("shortcut_action_change", false)
        fragment = MainFragment.getInstance(isShortcutUpdate)

        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        billingProcessor = BillingProcessor(this, BuildConfig.GOOGLE_IAP_KEY,
                object : IBillingHandler {
                    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
                        if (productId == UtilHelper.IAP_PID) {
                            sharedPreferences.edit().putBoolean(UtilHelper.PREF_IAP, true).apply()
                            MaterialDialog(this@MainActivity)
                                    .title(R.string.iab_complete_title)
                                    .customView(R.layout.dialog_donate)
                                    .positiveButton(R.string.ui_okay).show()
                        }
                    }

                    override fun onPurchaseHistoryRestored() {
                        if (billingProcessor.isPurchased(UtilHelper.IAP_PID)) {
                            sharedPreferences.edit().putBoolean(UtilHelper.PREF_IAP, true).apply()
                        }
                    }

                    override fun onBillingError(errorCode: Int, error: Throwable?) {}
                    override fun onBillingInitialized() {}
                })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(UtilHelper.PREF_IAP, false)) {
            val adsItem = menu.add(0, 1334, 0, R.string.action_iap)
            adsItem.setShowAsAction(ActionMenuItem.SHOW_AS_ACTION_ALWAYS)
            adsItem.setIcon(R.drawable.ic_local_play_white_24dp)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1334 -> checkPayment()
            R.id.action_settings -> {
                val intent = Intent().setClass(this,
                        SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.action_qanda -> openDialogTutor()
            R.id.action_share -> share()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun share() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message))
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_button)))
    }

    private fun openDialogTutor() {
        MaterialDialog(this)
                .title(R.string.qanda)
                .message(R.string.qanda_message)
                .negativeButton(R.string.update_navbtn)
                .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        billingProcessor.release()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!billingProcessor.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkPayment() {
        val isAvailable = BillingProcessor.isIabServiceAvailable(this)
        if (isAvailable) {
            billingProcessor.purchase(this, UtilHelper.IAP_PID)
        } else {
            Toast.makeText(this, R.string.ui_error, Toast.LENGTH_LONG).show()
        }
    }
}