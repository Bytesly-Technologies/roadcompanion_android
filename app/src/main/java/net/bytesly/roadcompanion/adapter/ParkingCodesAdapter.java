package net.bytesly.roadcompanion.adapter;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.bytesly.roadcompanion.AppController;
import net.bytesly.roadcompanion.MainActivity;
import net.bytesly.roadcompanion.R;

import org.w3c.dom.Text;

import java.util.HashSet;
import java.util.List;

public class ParkingCodesAdapter extends RecyclerView.Adapter<ParkingCodesAdapter.ViewHolder> {

    LayoutInflater mInflater;
    List<String> codeList;
    Context mContext;

    View.OnClickListener deleteButtonListener;

    public ParkingCodesAdapter(Context context, View.OnClickListener deleteButtonListener) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        setHasStableIds(true);

        this.deleteButtonListener = deleteButtonListener;
    }

    @NonNull
    @Override
    public ParkingCodesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.parking_code_entry_row, parent, false);

        return new ViewHolder(view, deleteButtonListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ParkingCodesAdapter.ViewHolder holder, int position) {
        if(codeList != null) {
            String currItem = codeList.get(position);
            holder.textViewCode.setText(currItem);
        }
    }

    public void setCodeList(List<String> codeList) {
        this.codeList = codeList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if(codeList != null) {
            return codeList.size();
        }
        else {
            return 0;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textViewCode;
        ImageView imageViewCopyIcon;

        public ViewHolder(@NonNull View itemView, View.OnClickListener deleteButtonListener) {
            super(itemView);
            textViewCode = itemView.findViewById(R.id.parkingcode_entry_code);
            imageViewCopyIcon = itemView.findViewById(R.id.parkingcode_copy_icon);
            imageViewCopyIcon.setOnClickListener(this);
            itemView.findViewById(R.id.parkingcode_delete_icon).setOnClickListener(deleteButtonListener);
        }

        @Override
        public void onClick(View v) {
            String currentCode = codeList.get(getAdapterPosition());
            switch (v.getId()) {
                case R.id.parkingcode_copy_icon:
                    ClipboardManager clipboard = (ClipboardManager)
                            mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Parking code", currentCode);
                    clipboard.setPrimaryClip(clip);
                    imageViewCopyIcon.setImageResource(R.drawable.ic_baseline_check_circle_outline_24);
                    ImageViewCompat.setImageTintList(imageViewCopyIcon, ColorStateList.valueOf(Color.GREEN));
                    Toast.makeText(mContext, R.string.copied_toastmsg, Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            imageViewCopyIcon.setImageResource(R.drawable.ic_baseline_content_copy_24);
                            ImageViewCompat.setImageTintList(imageViewCopyIcon, ColorStateList.valueOf(mContext.getColor(R.color.white)));
                        }
                    }, 1000);
                    break;
            }
        }
    }
}
