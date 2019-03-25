package hibernate.v2.ringtonerandomizer.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import hibernate.v2.ringtonerandomizer.R;
import hibernate.v2.ringtonerandomizer.model.Ringtone;

/**
 * Created by himphen on 25/5/16.
 */
public class RingtoneSelectedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private List<Ringtone> mDataList;
	private ItemClickListener mClickListener;

	public interface ItemClickListener {
		void onItemDetailClick(Ringtone ringtone);
	}

	public RingtoneSelectedAdapter(List<Ringtone> mDataList,
	                               ItemClickListener mClickListener) {
		this.mDataList = mDataList;
		this.mClickListener = mClickListener;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context context = parent.getContext();

		View itemView = LayoutInflater.from(context).inflate(R.layout.list_item_selected, parent, false);
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
	}

	@Override
	public int getItemCount() {
		return mDataList.size();
	}

	public static class ItemViewHolder extends RecyclerView.ViewHolder {

		@BindView(R.id.root_view)
		LinearLayout rootView;
		@BindView(R.id.filenameTv)
		TextView filenameTv;
		@BindView(R.id.filepathTv)
		TextView filepathTv;

		ItemViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}
	}
}