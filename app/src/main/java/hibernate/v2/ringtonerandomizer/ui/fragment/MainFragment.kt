package hibernate.v2.ringtonerandomizer.ui.fragment

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import hibernate.v2.ringtonerandomizer.R
import hibernate.v2.ringtonerandomizer.helper.DBHelper
import hibernate.v2.ringtonerandomizer.helper.UtilHelper
import hibernate.v2.ringtonerandomizer.model.Ringtone
import hibernate.v2.ringtonerandomizer.ui.activity.SelectRingtoneActivity
import hibernate.v2.ringtonerandomizer.ui.adapter.RingtoneSelectedAdapter
import kotlinx.android.synthetic.main.fragment_main.*
import java.lang.ref.WeakReference
import java.util.ArrayList

class MainFragment : BaseFragment() {
    private val currentRingtoneList = ArrayList<Ringtone>()
    private lateinit var dbHelper: DBHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DBHelper(context)
        rvList.layoutManager = LinearLayoutManager(context)
        rvList.adapter = RingtoneSelectedAdapter(
                currentRingtoneList,
                object : RingtoneSelectedAdapter.ItemClickListener {
                    override fun onItemDetailClick(ringtone: Ringtone) {
                        openDialogItem(ringtone)
                    }
                })

        randomIv.setOnClickListener { onClickRandom() }
        addIv.setOnClickListener { onClickAddRingtone() }
        clearIv.setOnClickListener { openDialogClearData() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context?.getSystemService(NotificationManager::class.java)
            if (notificationManager?.getNotificationChannel("Changed Notification") == null) {
                val channel = NotificationChannel("Changed Notification",
                        context?.getString(R.string.pref_title_changed_notification),
                        NotificationManager.IMPORTANCE_LOW)
                channel.description = context?.getString(R.string.pref_des_changed_notificationOn)
                notificationManager?.createNotificationChannel(channel)
            }
        }
        if (!isPermissionsGranted(PERMISSION_NAME)) {
            requestPermissions(PERMISSION_NAME, PERMISSION_REQUEST_CODE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                UtilHelper.openErrorSystemPermissionDialog(context)
            }
        } else {
            AppUpdater(context)
                    .showEvery(4)
                    .setDisplay(Display.NOTIFICATION)
                    .start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPermissionsGranted(PERMISSION_NAME)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(context)) {
                    UtilHelper.openErrorSystemPermissionDialog(context)
                    return
                }
            }
            updateCurrentRingtoneText()
            val bundle = arguments
            if (bundle != null) {
                if (bundle.getBoolean("shortcut_action", false)) {
                    onClickRandom()
                }
            }
            GetSavedRingtoneTask(this).execute()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }

    private fun onClickRandom() {
        val message: String
        val result = dbHelper.changeRingtone(null)
        message = when (result) {
            DBHelper.CHANGE_RINGTONE_RESULT_SUCCESS -> getString(R.string.changed_ringtone)
            DBHelper.CHANGE_RINGTONE_RESULT_COUNT_ZERO -> getString(R.string.notyet)
            DBHelper.CHANGE_RINGTONE_RESULT_COUNT_ONE -> getString(R.string.changed_ringtone_one)
            DBHelper.CHANGE_RINGTONE_RESULT_PERMISSION -> getString(R.string.change_ringtone_result_permission)
            else -> getString(R.string.change_ringtone_result_permission)
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        updateCurrentRingtoneText()
    }

    private fun onClickAddRingtone() {
        activity?.let { activity ->
            startActivity(Intent().setClass(activity, SelectRingtoneActivity::class.java))
        }
    }

    private fun updateCurrentRingtoneText() {
        currentText.text = UtilHelper.getCurrentRingtoneName(context)
    }

    private class GetSavedRingtoneTask(fragment: Fragment) : AsyncTask<Void?, Void?, Void?>() {
        private var dialog: MaterialDialog? = null
        private val fragmentWeakReference: WeakReference<Fragment> = WeakReference(fragment)
        private var dbHelper = DBHelper(fragment.context)

        private val fragment: MainFragment?
            get() = fragmentWeakReference.get() as MainFragment?

        public override fun onPreExecute() {
            super.onPreExecute()
            fragment?.activity?.let { activity ->
                dialog = MaterialDialog(activity)
                        .message(R.string.wait)
                        .cancelable(false)
                dialog?.show()
            }
            fragment?.currentRingtoneList?.clear()
        }

        override fun doInBackground(vararg arg0: Void?): Void? {
            fragment?.currentRingtoneList?.addAll(dbHelper.getDBRingtoneList())
            return null
        }

        public override fun onPostExecute(un: Void?) {
            dbHelper.close()
            fragment?.activity?.let { activity ->
                if (activity.isDestroyed) return
                fragment?.rvList?.adapter?.notifyDataSetChanged()
                dialog?.dismiss()
            }
        }
    }

    private fun openDialogClearData() {
        context?.let { context ->
            val dialog = MaterialDialog(context)
                    .title(R.string.clear_title)
                    .message(R.string.clear_message)
                    .positiveButton(R.string.clear_posbtn) {
                        dbHelper.clearDBRingtoneList()
                        GetSavedRingtoneTask(this@MainFragment).execute()
                    }
                    .negativeButton(R.string.clear_navbtn)
            dialog.show()
        }
    }

    fun openDialogItem(ringtone: Ringtone) {
        val message: String = if (ringtone.path != null) {
            (getString(R.string.item_message) + ringtone.name
                    + "\n\n" + getString(R.string.item_message2) + ringtone.path)
        } else {
            (getString(R.string.item_message) + ringtone.name
                    + "\n\n" + getString(R.string.item_message2) + Ringtone.PATH_INTERNAL_STORAGE)
        }
        context?.let { context ->
            @Suppress("DEPRECATION")
            MaterialDialog(context)
                    .title(R.string.item_title)
                    .message(text = message)
                    .positiveButton(R.string.item_posbtn) {
                        dbHelper.changeRingtone(ringtone)
                        updateCurrentRingtoneText()
                    }
                    .neutralButton(R.string.item_netbtn) {
                        dbHelper.deleteDBRingtone(ringtone.uriId)
                        GetSavedRingtoneTask(this@MainFragment).execute()
                    }
                    .negativeButton(R.string.item_navbtn)
                    .show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!hasAllPermissionsGranted(grantResults)) {
                UtilHelper.openErrorPermissionDialog(context)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requestCode == 500 && !Settings.System.canWrite(context)) {
                activity?.finish()
            }
        }
    }

    companion object {
        fun getInstance(isShortcutUpdate: Boolean): MainFragment {
            val fragment = MainFragment()
            val args = Bundle()
            args.putBoolean("shortcut_action", isShortcutUpdate)
            fragment.arguments = args
            return fragment
        }

        private val PERMISSION_NAME = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE
        )
    }
}