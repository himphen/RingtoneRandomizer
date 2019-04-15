package hibernate.v2.ringtonerandomizer.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
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

	public interface ItemClickListener {
		void onItemDetailClick(Ringtone ringtone);
	}

	public RingtoneSelectAdapter(@NonNull List<Ringtone> mDataList,
	                             ItemClickListener mClickListener) {
		this.mDataList = mDataList;
		this.mClickListener = mClickListener;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context context = parent.getContext();

		View itemView = LayoutInflater.from(context).inflate(R.layout.list_item_select, parent, false);
		return new ItemViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder rawHolder, int position) {
		Ringtone ringtone = mDataList.get(position);
		ItemViewHolder holder = (ItemViewHolder) rawHolder;

		holder.filenameTv.setText(ringtone.getName());
		if (ringtone.getPath() == null) {
			holder.filepathTv.setText("Internal Storage");
		} else {
			holder.filepathTv.setText(ringtone.getPath());
		}

		holder.rootView.setTag(ringtone);
		holder.rootView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mClickListener.onItemDetailClick((Ringtone) v.getTag());
			}
		});

		holder.checkbox.setChecked(ringtone.getChecked());
		holder.checkbox.setTag(ringtone);
		holder.checkbox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Ringtone ringtone = (Ringtone) v.getTag();
				boolean b = !ringtone.getChecked();
				ringtone.setChecked(b);
				((CheckBox) v).setChecked(b);
			}
		});
	}

	@Override
	public int getItemCount() {
		return mDataList.size();
	}

	public void refreshData(@NonNull List<Ringtone> mDataList) {
		this.mDataList.clear();
		this.mDataList.addAll(mDataList);
		this.notifyDataSetChanged();
	}

	public ArrayList<Ringtone> getCheckedRingtoneList() {
		ArrayList<Ringtone> checkedRingtoneList = new ArrayList<>();
		for (Ringtone ringtone : mDataList) {
			if (ringtone.getChecked()) {
				checkedRingtoneList.add(ringtone);
			}
		}

		return checkedRingtoneList;
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