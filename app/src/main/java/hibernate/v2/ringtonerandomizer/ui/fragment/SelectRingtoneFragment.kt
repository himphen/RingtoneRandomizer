package hibernate.v2.ringtonerandomizer.ui.fragment

import android.Manifest
import android.media.MediaPlayer
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import hibernate.v2.ringtonerandomizer.R
import hibernate.v2.ringtonerandomizer.helper.DBHelper
import hibernate.v2.ringtonerandomizer.helper.UtilHelper
import hibernate.v2.ringtonerandomizer.model.Ringtone
import hibernate.v2.ringtonerandomizer.model.Ringtone.Companion.PATH_INTERNAL_STORAGE
import hibernate.v2.ringtonerandomizer.ui.adapter.RingtoneSelectAdapter
import kotlinx.android.synthetic.main.fragment_select_ringtone.*
import java.lang.ref.WeakReference
import java.util.ArrayList

class SelectRingtoneFragment : BaseFragment() {

    private var allRingtoneList = ArrayList<Ringtone>()
    private var shownRingtoneList = ArrayList<Ringtone>()
    private val alFilter = ArrayList<String>()
    private var chosenPath = ""
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var ringtoneSelectAdapter: RingtoneSelectAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_select_ringtone, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ringtoneSelectAdapter = RingtoneSelectAdapter(
                shownRingtoneList,
                object : RingtoneSelectAdapter.ItemClickListener {
                    override fun onItemDetailClick(ringtone: Ringtone) {
                        openDialogPlayingPreview(ringtone)
                    }
                })
        rvList.layoutManager = LinearLayoutManager(context)
        rvList.adapter = ringtoneSelectAdapter

        saveBtn.setOnClickListener { onClickSaveList() }
        filterBtn.setOnClickListener { onClickFilterDialog() }

        if (!isPermissionsGranted(PERMISSION_NAME)) {
            requestPermissions(PERMISSION_NAME, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPermissionsGranted(PERMISSION_NAME)) {
            GetDeviceRingtoneTask(this).execute()
        }
    }

    override fun onPause() {
        stopPlaying()
        super.onPause()
    }

    private class SaveTask(fragment: SelectRingtoneFragment) : AsyncTask<Void?, Void?, Void?>() {
        private var dialog: MaterialDialog? = null
        private val fragmentWeakReference: WeakReference<SelectRingtoneFragment> = WeakReference(fragment)
        private var dbHelper = DBHelper(fragment.context)

        private val fragment: SelectRingtoneFragment?
            get() = fragmentWeakReference.get()

        public override fun onPreExecute() {
            super.onPreExecute()
            fragment?.activity?.let { activity ->
                dialog = MaterialDialog(activity)
                        .message(R.string.wait)
                        .cancelable(false)
                dialog?.show()
            }
        }

        override fun doInBackground(vararg param: Void?): Void? {
            fragment?.ringtoneSelectAdapter?.checkedRingtoneList?.let { checkedRingtoneList ->
                for (ringtone in checkedRingtoneList) {
                    dbHelper.insertDBRingtone(ringtone)
                }
            }

            return null
        }

        public override fun onPostExecute(un: Void?) {
            dbHelper.close()
            Toast.makeText(fragment?.activity, R.string.done, Toast.LENGTH_SHORT).show()
            fragment?.activity?.let { activity ->
                if (activity.isDestroyed) return
                dialog?.dismiss()
                activity.finish()
            }
        }
    }

    private class GetDeviceRingtoneTask(fragment: SelectRingtoneFragment) : AsyncTask<Void?, Void?, Void?>() {
        private var dialog: MaterialDialog? = null
        private val fragmentWeakReference: WeakReference<SelectRingtoneFragment> = WeakReference(fragment)

        private val fragment: SelectRingtoneFragment?
            get() = fragmentWeakReference.get()

        public override fun onPreExecute() {
            super.onPreExecute()
            fragment?.allRingtoneList?.clear()
            fragment?.shownRingtoneList?.clear()
            fragment?.activity?.let { activity ->
                dialog = MaterialDialog(activity)
                        .message(R.string.wait)
                        .cancelable(false)
                dialog?.show()
            }
        }

        override fun doInBackground(vararg param: Void?): Void? {
            val ringtones = UtilHelper.getDeviceRingtoneList(fragment?.activity)
            // Clone the ArrayList
            fragment?.allRingtoneList = ArrayList(ringtones)
            fragment?.shownRingtoneList = ArrayList(ringtones)
            return null
        }

        public override fun onPostExecute(un: Void?) {
            fragment?.activity?.let { activity ->
                fragment?.shownRingtoneList?.let { shownRingtoneList ->
                    fragment?.ringtoneSelectAdapter?.refreshData(shownRingtoneList)
                }
                if (activity.isDestroyed) return
                dialog?.dismiss()
            }
        }
    }

    private fun filterList() {
        alFilter.clear()
        val hsFilter = hashSetOf(PATH_INTERNAL_STORAGE)
        for (ringtone in allRingtoneList) {
            var posStart = 1
            var posEnd: Int
            ringtone.path?.let {
                while (true) {
                    posEnd = it.indexOf("/", posStart)
                    if (posStart == posEnd || posEnd == -1) break
                    posStart = posEnd + 1
                    val stringSub = it.substring(0, posEnd + 1)
                    hsFilter.add(stringSub)
                }
            }
        }
        alFilter.addAll(hsFilter)
        alFilter.sort()
        val alFilterRingtone = ArrayList(alFilter)
        val alFilterTemp = ArrayList(alFilter)
        for (element in alFilter) {
            if (!element.toLowerCase().contains("ringtone")) {
                alFilterRingtone.remove(element)
            } else {
                alFilterTemp.remove(element)
            }
        }
        alFilter.clear()
        alFilter.addAll(alFilterRingtone)
        alFilter.addAll(alFilterTemp)
    }

    private fun onClickSaveList() {
        SaveTask(this).execute()
    }

    private fun onClickFilterDialog() {
        filterList()
        context?.let { context ->
            MaterialDialog(context)
                    .title(R.string.filter_title)
                    .listItemsSingleChoice(items = alFilter, waitForPositiveButton = false) { dialog, _, text ->
                        chosenPath = text.toString()
                        shownRingtoneList.clear()
                        for (ringtone in allRingtoneList) {
                            ringtone.path?.let { path ->
                                if (path.contains(chosenPath)) {
                                    shownRingtoneList.add(ringtone)
                                }
                            } ?: run {
                                if (chosenPath == PATH_INTERNAL_STORAGE) {
                                    shownRingtoneList.add(ringtone)
                                }
                            }
                        }
                        ringtoneSelectAdapter.refreshData(shownRingtoneList)
                        currentPathTv.text = chosenPath
                        dialog.dismiss()
                    }
                    .neutralButton(R.string.ui_reset) { dialog ->
                        ringtoneSelectAdapter.refreshData(allRingtoneList)
                        currentPathTv.text = "N/A"
                        dialog.dismiss()
                    }
                    .negativeButton(R.string.item_navbtn)
                    .show()
        }
    }

    private fun openDialogPlayingPreview(ringtone: Ringtone) {
        try {
            mediaPlayer = MediaPlayer.create(context, Uri.parse(ringtone.uriId))
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
            val message: String = if (ringtone.path != null) {
                (getString(R.string.item_message) + ringtone.name
                        + "\n\n" + getString(R.string.item_message2) + ringtone.path)
            } else {
                (getString(R.string.item_message) + ringtone.name
                        + "\n\n" + getString(R.string.item_message2) + PATH_INTERNAL_STORAGE)
            }
            context?.let { context ->
                MaterialDialog(context)
                        .title(R.string.playing_sound)
                        .message(text = message)
                        .onDismiss { stopPlaying() }
                        .negativeButton(R.string.item_navbtn)
                        .show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, R.string.ui_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPlaying() {
        if (isPlaying) {
            mediaPlayer?.stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!hasAllPermissionsGranted(grantResults)) {
                UtilHelper.openErrorPermissionDialog(context)
            }
        }
    }

    companion object {
        private val PERMISSION_NAME = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}