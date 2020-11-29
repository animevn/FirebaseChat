package com.haanhgs.app.firebasechat;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class Adapter extends FirebaseRecyclerAdapter<Message, Adapter.MessageHolder> {

    public Adapter(@NonNull FirebaseRecyclerOptions<Message> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull final MessageHolder viewHolder, int i,
                                    @NonNull Message message) {
        final Context context = viewHolder.itemView.getContext();
        if (message.getText() != null) {
            viewHolder.messageTextView.setText(message.getText());
            viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
            viewHolder.messageImageView.setVisibility(ImageView.GONE);
        } else if (message.getImageUrl() != null) {
            String imageUrl = message.getImageUrl();
            if (imageUrl.startsWith("gs://")) {
                StorageReference storageReference = FirebaseStorage.getInstance()
                        .getReferenceFromUrl(imageUrl);
                storageReference.getDownloadUrl().addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                String downloadUrl = task.getResult().toString();
                                Glide.with(context).load(downloadUrl)
                                        .into(viewHolder.messageImageView);
                            } else {
                                Log.w("Main", "Get download url failed", task.getException());
                            }
                        });
            } else {
                Glide.with(context).load(message.getImageUrl()).into(viewHolder.messageImageView);
            }
            viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
            viewHolder.messageTextView.setVisibility(TextView.GONE);
        }

        viewHolder.messengerTextView.setText(message.getName());
        if (message.getPhotoUrl() == null) {

            viewHolder.messengerImageView.setImageDrawable(
                    context.getDrawable(R.drawable.ic_account_circle_black_36dp));
        } else {
            Glide.with(viewHolder.itemView.getContext())
                    .load(message.getPhotoUrl())
                    .into(viewHolder.messengerImageView);
        }
    }

    @NonNull
    @Override
    public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new MessageHolder(inflater.inflate(R.layout.item_message, parent, false));
    }

    public class MessageHolder extends RecyclerView.ViewHolder {
        final TextView messageTextView;
        final ImageView messageImageView;
        final TextView messengerTextView;
        final CircleImageView messengerImageView;

        public MessageHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.textview_message);
            messageImageView = itemView.findViewById(R.id.imageview_message);
            messengerTextView = itemView.findViewById(R.id.textview_messenger);
            messengerImageView = itemView.findViewById(R.id.imageview_messenger);
        }
    }
}
