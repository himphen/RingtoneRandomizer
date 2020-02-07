package hibernate.v2.ringtonerandomizer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hibernate.v2.ringtonerandomizer.R
import hibernate.v2.ringtonerandomizer.model.Ringtone
import java.util.ArrayList

/**
 * Created by himphen on 25/5/16.
 */
class RingtoneSelectAdapter(
        private val dataList: MutableList<Ringtone>,
        private val clickListener: ItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface ItemClickListener {
        fun onItemDetailClick(ringtone: Ringtone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_select, parent, false))
    }

    override fun onBindViewHolder(rawHolder: RecyclerView.ViewHolder, position: Int) {
        val ringtone = dataList[position]
        val holder = rawHolder as ItemViewHolder
        holder.filenameTv.text = ringtone.name
        if (ringtone.path == null) {
            holder.filepathTv.text = Ringtone.PATH_INTERNAL_STORAGE
        } else {
            holder.filepathTv.text = ringtone.path
        }
        holder.rootView.tag = ringtone
        holder.rootView.setOnClickListener { v -> clickListener.onItemDetailClick(v.tag as Ringtone) }
        holder.checkbox.isChecked = ringtone.checked
        holder.checkbox.tag = ringtone
        holder.checkbox.setOnClickListener { view ->
            val ringtone1 = view.tag as Ringtone
            val b = !ringtone1.checked
            ringtone1.checked = b
            (view as CheckBox).isChecked = b
        }
    }

    override fun getItemCount(): Int = dataList.size

    fun refreshData(mDataList: List<Ringtone>) {
        this.dataList.clear()
        this.dataList.addAll(mDataList)
        notifyDataSetChanged()
    }

    val checkedRingtoneList: ArrayList<Ringtone>
        get() {
            val checkedRingtoneList = ArrayList<Ringtone>()
            for (ringtone in dataList) {
                if (ringtone.checked) {
                    checkedRingtoneList.add(ringtone)
                }
            }
            return checkedRingtoneList
        }

    internal class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var rootView: LinearLayout = view.findViewById(R.id.root_view)
        var filenameTv: TextView = view.findViewById(R.id.filenameTv)
        var filepathTv: TextView = view.findViewById(R.id.filepathTv)
        var checkbox: CheckBox = view.findViewById(R.id.checkbox)
    }

}