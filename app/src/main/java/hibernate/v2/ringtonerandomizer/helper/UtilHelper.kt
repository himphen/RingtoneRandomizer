package hibernate.v2.ringtonerandomizer.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewConfiguration
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.SizeUtils
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.orhanobut.logger.Logger
import hibernate.v2.ringtonerandomizer.BuildConfig
import hibernate.v2.ringtonerandomizer.R
import hibernate.v2.ringtonerandomizer.model.Ringtone
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.ArrayList
import java.util.Locale

/**
 * UtilHelper Class
 * Created by Himphen on 10/1/2016.
 */
object UtilHelper {
    const val TAG = "tag"
    const val DEBUG_TAG = "debug_tag"
    const val PREF_IAP = "iap"
    const val PREF_LANGUAGE = "PREF_LANGUAGE"
    const val PREF_LANGUAGE_COUNTRY = "PREF_LANGUAGE_COUNTRY"
    const val IAP_PID = "donation"

    const val DELAY_AD_LAYOUT = 100L

    fun initAdView(
        context: Context?,
        adLayout: RelativeLayout,
        isPreserveSpace: Boolean = false
    ): AdView? {
        if (context == null) return null

        if (isPreserveSpace) {
            adLayout.layoutParams.height = SizeUtils.dp2px(50f)
        }
        val defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        try {
            if (!defaultPreferences.getBoolean(PREF_IAP, false)) {
                val adView = AdView(context)
                adView.adUnitId = BuildConfig.ADMOB_BANNER_ID
                adView.adSize = AdSize.BANNER
                adLayout.addView(adView)
                adView.loadAd(AdRequest.Builder().build())
                return adView
            }
        } catch (e: Exception) {
            logException(e)
        }
        return null
    }

    fun round(value: Double, places: Int): Double {
        require(places >= 0)
        var bd = BigDecimal(value)
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toDouble()
    }

    fun formatSignificant(value: Double, significant: Int): String {
        val mathContext = MathContext(significant, RoundingMode.DOWN)
        val bigDecimal = BigDecimal(value, mathContext)
        return bigDecimal.toPlainString()
    }

    @Suppress("DEPRECATION")
    fun detectLanguage(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        var language = preferences.getString(PREF_LANGUAGE, "") ?: ""
        var languageCountry = preferences.getString(PREF_LANGUAGE_COUNTRY, "") ?: ""
        if (language == "") {
            val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Resources.getSystem().configuration.locales[0]
            } else {
                Resources.getSystem().configuration.locale
            }
            language = locale.language
            languageCountry = locale.country
        }
        val res = context.resources
        val conf = res.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            conf.setLocale(Locale(language, languageCountry))
        } else {
            conf.locale = Locale(language, languageCountry)
        }
        val dm = res.displayMetrics
        res.updateConfiguration(conf, dm)
    }

    fun debug(message: String) {
        if (BuildConfig.DEBUG) {
            Logger.d(message)
        }
    }

    fun logException(e: Exception) {
        if (BuildConfig.DEBUG) {
            e.printStackTrace()
        } else {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    fun isPermissionsGranted(context: Context?, permissions: Array<String>): Boolean {
        context?.let {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(it, permission) == PackageManager.PERMISSION_DENIED) {
                    return false
                }
            }
            return true
        } ?: run {
            return false
        }
    }

    fun hasAllPermissionsGranted(grantResults: IntArray): Boolean {
        for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    fun scanForActivity(context: Context?): Activity? {
        return when (context) {
            is Activity -> context
            is ContextWrapper -> scanForActivity(context.baseContext)
            else -> null
        }
    }

    fun startSettingsActivity(context: Context, action: String?) {
        try {
            context.startActivity(Intent(action))
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }


    fun openErrorPermissionDialog(context: Context?) {
        context?.let {
            MaterialDialog(it)
                    .title(R.string.ui_caution)
                    .customView(R.layout.dialog_permission)
                    .cancelable(false)
                    .positiveButton(R.string.ui_okay) { dialog ->
                        scanForActivity(dialog.context)?.let { activity ->
                            try {
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                intent.addCategory(Intent.CATEGORY_DEFAULT)
                                intent.data = Uri.parse("package:" + activity.packageName)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                activity.startActivity(intent)
                                activity.finish()
                            } catch (e: Exception) {
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_SETTINGS
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                activity.startActivity(intent)
                                activity.finish()
                            }
                        }
                    }
                    .negativeButton(R.string.ui_cancel) { dialog ->
                        scanForActivity(dialog.context)?.finish()
                    }
                    .show()
        }
    }

    fun openErrorSystemPermissionDialog(context: Context?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context?.let {
                MaterialDialog(context)
                        .title(R.string.write_settings_permission_dialog_title)
                        .customView(R.layout.dialog_system_permission)
                        .cancelable(false)
                        .negativeButton(R.string.ui_cancel) { dialog ->
                            scanForActivity(dialog.context)?.finish()
                        }
                        .positiveButton(R.string.write_settings_permission_dialog_pos_btn) { dialog ->
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + AppUtils.getAppPackageName()))
                            scanForActivity(dialog.context)?.startActivityForResult(intent, 500)
                        }
                        .show()
            }
        }
    }

    fun getDeviceRingtoneList(context: Context?): ArrayList<Ringtone> {
        val allRingtoneList = ArrayList<Ringtone>()
        context?.let {
            try {
                val contentResolver = context.contentResolver
                val selection1 = MediaStore.Audio.Media.IS_RINGTONE + " != 0"
                val selection2 = MediaStore.Audio.Media.IS_MUSIC + " != 0"
                val selection3 = MediaStore.Audio.Media.IS_NOTIFICATION + " != 0"
                val selection4 = MediaStore.Audio.Media.IS_ALARM + " != 0"
                val selection = "$selection1 OR $selection2 OR $selection3 OR $selection4"
                val projection = arrayOf(MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DATA, MediaStore.Audio.Media._ID)
                var cursor = contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
                        selection, null, null)
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val ringtone = Ringtone(
                                name = cursor.getString(0),
                                path = cursor.getString(1),
                                uriId = "content://media/external/audio/media/" + cursor.getString(2)
                        )
                        debug("ringtone 1: $ringtone")
                        allRingtoneList.add(ringtone)
                    }
                    cursor.close()
                }
                val manager = RingtoneManager(context)
                manager.setType(RingtoneManager.TYPE_RINGTONE)
                cursor = manager.cursor
                while (cursor.moveToNext()) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val id = cursor.getString(RingtoneManager.ID_COLUMN_INDEX)
                    val uri = cursor.getString(RingtoneManager.URI_COLUMN_INDEX)
                    val ringtone = Ringtone(name = title, uriId = "$uri/$id")
                    debug("ringtone 2: $ringtone")
                    allRingtoneList.add(ringtone)
                }
                allRingtoneList.sortWith(Comparator { o1, o2 -> o1.name.compareTo(o2.name) })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return allRingtoneList
    }

    fun getCurrentRingtoneName(context: Context?): String {
        return try {
            RingtoneManager.getRingtone(context,
                    Settings.System.DEFAULT_RINGTONE_URI).getTitle(context)
        } catch (e: Exception) {
            "DRM Error"
        }
    }

    @Suppress("SameParameterValue")
    fun snackbar(view: View?, string: String? = null, @StringRes stringRid: Int? = null): Snackbar? {
        val snackbar: Snackbar
        view?.let {
            snackbar = when {
                string != null -> Snackbar.make(view, string, Snackbar.LENGTH_LONG)
                stringRid != null -> Snackbar.make(view, stringRid, Snackbar.LENGTH_LONG)
                else -> return null
            }

            val sbView = snackbar.view
            sbView.setBackgroundResource(R.color.primary_dark)
            sbView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).setTextColor(Color.WHITE)
            sbView.findViewById<TextView>(com.google.android.material.R.id.snackbar_action).setTextColor(
                    ContextCompat.getColor(snackbar.context, R.color.gold))

            return snackbar
        }

        return null
    }

    fun notAppFound(context: Activity?) {
        Toast.makeText(context, R.string.app_not_found, Toast.LENGTH_LONG).show()
        context?.finish()
    }

    @SuppressLint("HardwareIds")
    fun getAdMobDeviceID(context: Context): String {
        val androidId =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return androidId.md5().toUpperCase(Locale.getDefault())
    }
}

fun String.md5(): String {
    return try {
        val md = MessageDigest.getInstance("MD5")
        BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
    } catch (e: Exception) {
        ""
    }
}