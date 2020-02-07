package hibernate.v2.ringtonerandomizer.ui.fragment

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.blankj.utilcode.util.AppUtils
import hibernate.v2.ringtonerandomizer.BuildConfig
import hibernate.v2.ringtonerandomizer.R
import hibernate.v2.ringtonerandomizer.helper.UtilHelper

class SettingsFragment : PreferenceFragmentCompat() {
    private var prefChangedNotificationAndroidO: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_general)

        val prefReport = findPreference<Preference>("pref_report")
        val prefMoreApp = findPreference<Preference>("pref_more_app")
        val prefVersion = findPreference<Preference>("pref_version")
        val prefChangedNotification = findPreference<Preference>("pref_changed_notification")
        prefChangedNotificationAndroidO = findPreference("pref_changed_notification_android_o")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            prefChangedNotification?.isVisible = false
            prefChangedNotificationAndroidO?.isVisible = true
            prefChangedNotificationAndroidO?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, AppUtils.getAppPackageName())
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, "Changed Notification")
                startActivity(intent)
                false
            }
        } else {
            prefChangedNotification?.isVisible = true
            prefChangedNotificationAndroidO?.isVisible = false
        }
        prefVersion?.summary = AppUtils.getAppVersionName()
        prefReport?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            openDialogReport()
            false
        }
        prefMoreApp?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            openDialogMoreApp()
            false
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.let { context ->
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager?.let {
                    val notificationChannel = notificationManager.getNotificationChannel("Changed Notification")
                    if (notificationChannel != null) {
                        if (notificationChannel.importance != NotificationManager.IMPORTANCE_NONE) {
                            prefChangedNotificationAndroidO?.summary = getString(R.string.pref_des_changed_notificationOn)
                        } else {
                            prefChangedNotificationAndroidO?.summary = getString(R.string.pref_des_changed_notificationOff)
                        }
                    }
                }
            }
        }
    }

    private fun openDialogMoreApp() {
        try {
            val uri = Uri.parse("market://search?q=pub:\"Hibernate\"")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            UtilHelper.notAppFound(activity)
        }
    }

    private fun openDialogReport() {
        val intent = Intent(Intent.ACTION_SEND)
        var text = "Android Version: " + Build.VERSION.RELEASE + "\n"
        text += "SDK Level: " + Build.VERSION.SDK_INT.toString() + "\n"
        text += "Version: " + AppUtils.getAppVersionName() + "\n"
        text += "Brand: " + Build.BRAND + "\n"
        text += "Model: " + Build.MODEL + "\n\n\n"
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_EMAIL, BuildConfig.CONTACT_EMAIL)
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_title))
        intent.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(intent, getString(R.string.report)))
    }
}