package hibernate.v2.ringtonerandomizer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hibernate.v2.ringtonerandomizer.R
import hibernate.v2.ringtonerandomizer.model.Ringtone

/**
 * Created by himphen on 25/5/16.
 */
class RingtoneSelectedAdapter(
        private val dataList: List<Ringtone>,
        private val clickListener: ItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface ItemClickListener {
        fun onItemDetailClick(ringtone: Ringtone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        val itemView = LayoutInflater.from(context).inflate(R.layout.list_item_selected, parent, false)
        return ItemViewHolder(itemView)
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
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    internal class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var rootView: LinearLayout = view.findViewById(R.id.root_view)
        var filenameTv: TextView = view.findViewById(R.id.filenameTv)
        var filepathTv: TextView = view.findViewById(R.id.filepathTv)
    }

}