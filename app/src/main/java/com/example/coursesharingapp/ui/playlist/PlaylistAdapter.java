package com.example.coursesharingapp.ui.playlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coursesharingapp.databinding.ItemPlaylistBinding;
import com.example.coursesharingapp.model.Playlist;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private Context context;
    private List<Playlist> playlists;
    private OnPlaylistClickListener listener;
    private OnPlaylistDeleteListener deleteListener;
    private OnPlaylistEditListener editListener;
    private boolean showDeleteButton;
    private boolean showEditButton;
    private String currentUserUid;

    // Updated constructor with edit functionality
    public PlaylistAdapter(Context context, List<Playlist> playlists,
                           OnPlaylistClickListener listener, OnPlaylistDeleteListener deleteListener,
                           OnPlaylistEditListener editListener, boolean showDeleteButton,
                           boolean showEditButton, String currentUserUid) {
        this.context = context;
        this.playlists = playlists;
        this.listener = listener;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
        this.showDeleteButton = showDeleteButton;
        this.showEditButton = showEditButton;
        this.currentUserUid = currentUserUid;
    }

    // Original constructor for backward compatibility
    public PlaylistAdapter(Context context, List<Playlist> playlists, OnPlaylistClickListener listener) {
        this(context, playlists, listener, null, null, false, false, null);
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlaylistBinding binding = ItemPlaylistBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new PlaylistViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.bind(playlist, position);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private ItemPlaylistBinding binding;

        public PlaylistViewHolder(@NonNull ItemPlaylistBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Playlist playlist, int position) {
            // Set playlist data
            binding.playlistTitleTv.setText(playlist.getTitle());
            binding.playlistCreatorTv.setText("By: " + playlist.getCreatorUsername());

            // Set course count
            int courseCount = playlist.getCoursesCount();
            binding.coursesCountTv.setText(courseCount + (courseCount == 1 ? " course" : " courses"));

            // Set description if available
            if (playlist.getDescription() != null && !playlist.getDescription().isEmpty()) {
                binding.playlistDescriptionTv.setText(playlist.getDescription());
            } else {
                binding.playlistDescriptionTv.setText("No description available");
            }

            // Show/hide edit and delete buttons based on ownership
            boolean isOwner = currentUserUid != null && currentUserUid.equals(playlist.getCreatorUid());

            if (showDeleteButton && deleteListener != null && isOwner) {
                binding.deletePlaylistButton.setVisibility(View.VISIBLE);
                binding.deletePlaylistButton.setOnClickListener(v ->
                        deleteListener.onPlaylistDelete(playlist, position));
            } else {
                binding.deletePlaylistButton.setVisibility(View.GONE);
            }

            if (showEditButton && editListener != null && isOwner) {
                binding.editPlaylistButton.setVisibility(View.VISIBLE);
                binding.editPlaylistButton.setOnClickListener(v ->
                        editListener.onPlaylistEdit(playlist, position));
            } else {
                binding.editPlaylistButton.setVisibility(View.GONE);
            }

            // Set click listener
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist, position);
                }
            });
        }
    }

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist, int position);
    }

    public interface OnPlaylistDeleteListener {
        void onPlaylistDelete(Playlist playlist, int position);
    }

    public interface OnPlaylistEditListener {
        void onPlaylistEdit(Playlist playlist, int position);
    }
}