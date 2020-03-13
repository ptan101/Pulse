package tan.philip.nrf_ble.ScanListScreen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import tan.philip.nrf_ble.R;

public class BluetoothItemAdapter extends RecyclerView.Adapter<BluetoothItemAdapter.BluetoothItemViewHolder> {

    private ArrayList<BluetoothItem> mBluetoothItemList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public static class BluetoothItemViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageView;
        public TextView mTextView1;

        public BluetoothItemViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.imageView);
            mTextView1 = itemView.findViewById(R.id.textView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(listener != null) {
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    public BluetoothItemAdapter(ArrayList<BluetoothItem> bluetoothItemList)  {
        mBluetoothItemList = bluetoothItemList;
    }

    @NonNull
    @Override
    public BluetoothItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetoothitem, parent, false);
        BluetoothItemViewHolder bivh = new BluetoothItemViewHolder(v, mListener);
        return bivh;
    }

    @Override
    public void onBindViewHolder(@NonNull BluetoothItemViewHolder holder, int position) {
        BluetoothItem currentItem = mBluetoothItemList.get(position);

        holder.mImageView.setImageResource(currentItem.getImageResource());
        holder.mTextView1.setText(currentItem.getText1());
    }

    @Override
    public int getItemCount() {
        return mBluetoothItemList.size();
    }
}