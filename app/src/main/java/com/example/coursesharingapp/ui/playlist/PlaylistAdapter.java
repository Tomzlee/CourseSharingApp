package com.example.coursesharingapp.ui.playlist;

import android.content.Context;
import android.view.LayoutInflater;
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

    public PlaylistAdapter(Context context, List<Playlist> playlists, OnPlaylistClickListener listener) {
        this.context = context;
        this.playlists = playlists;
        this.listener = listener;
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
}