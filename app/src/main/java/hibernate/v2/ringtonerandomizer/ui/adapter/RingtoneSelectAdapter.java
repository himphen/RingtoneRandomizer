package hibernate.v2.ringtonerandomizer.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import hibernate.v2.ringtonerandomizer.R;
import hibernate.v2.ringtonerandomizer.model.Ringtone;

/**
 * Created by himphen on 25/5/16.
 */
public class RingtoneSelectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private List<Ringtone> mDataList;
	private ItemClickListener mClickListener;
	private ItemCheckListener mCheckListener;
	private HashMap<String, Ringtone> selectedRingtoneMap = new HashMap<>();

	public interface ItemClickListener {
		void onItemDetailClick(Ringtone ringtone);
	}

	public interface ItemCheckListener {
		void onItemDetailCheck(Ringtone ringtone, boolean isChecked);
	}

	public RingtoneSelectAdapter(List<Ringtone> mDataList,
	                             ItemClickListener mClickListener,
	                             ItemCheckListener mCheckListener) {
		this.mDataList = mDataList;
		this.mClickListener = mClickListener;
		this.mCheckListener = mCheckListener;
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		Context context = parent.getContext();

		View itemView = LayoutInflater.from(context).inflate(R.layout.list_item_select, parent, false);
		return new ItemViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder rawHolder, int position) {
		Ringtone item = mDataList.get(position);
		ItemViewHolder holder = (ItemViewHolder) rawHolder;

		holder.filenameTv.setText(item.getName());
		holder.filepathTv.setText(item.getPath());

		holder.rootView.setTag(item);
		holder.rootView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mClickListener.onItemDetailClick((Ringtone) v.getTag());
			}
		});

		holder.checkbox.setTag(item);
		holder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton v, boolean isChecked) {
				mCheckListener.onItemDetailCheck((Ringtone) v.getTag(), isChecked);
			}
		});
	}

	@Override
	public int getItemCount() {
		return mDataList == null ? 0 : mDataList.size();
	}

	public HashMap<String, Ringtone> getSelectedRingtoneMap() {
		return selectedRingtoneMap;
	}

	public void addRingtone(Ringtone ringtone) {
		selectedRingtoneMap.put(ringtone.getPath(), ringtone);
	}

	public void removeRingtone(Ringtone ringtone) {
		selectedRingtoneMap.remove(ringtone.getPath());
	}

	static class ItemViewHolder extends RecyclerView.ViewHolder {

		@BindView(R.id.root_view)
		LinearLayout rootView;
		@BindView(R.id.filenameTv)
		TextView filenameTv;
		@BindView(R.id.filepathTv)
		TextView filepathTv;
		@BindView(R.id.checkbox)
		CheckBox checkbox;

		ItemViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}
	}
}