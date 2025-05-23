package com.example.coursesharingapp.ui.playlist;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coursesharingapp.databinding.ItemPlaylistBinding;
import com.example.coursesharingapp.model.Playlist;
import com.example.coursesharingapp.repository.AuthRepository;
import com.example.coursesharingapp.repository.PlaylistRepository;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private Context context;
    private List<Playlist> playlists;
    private OnPlaylistClickListener listener;
    private OnPlaylistDeleteListener deleteListener;
    private OnPlaylistEditListener editListener;
    private boolean showDeleteButton;
    private boolean showEditButton;
    private String currentUserUid;
    private FirebaseUser currentUser;
    private PlaylistRepository playlistRepository;
    private Set<String> savedPlaylistIds = new HashSet<>();
    private boolean isMyPlaylistsView; // New flag to track if this is "My Playlists" view

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
        this.isMyPlaylistsView = showDeleteButton || showEditButton; // If showing edit/delete, it's My Playlists

        // Initialize Firebase-related objects
        AuthRepository authRepository = new AuthRepository();
        this.currentUser = authRepository.getCurrentUser();
        this.playlistRepository = new PlaylistRepository();

        // Load saved state for all playlists if the user is logged in
        if (currentUser != null) {
            loadSavedPlaylists();
        }
    }

    // Original constructor for backward compatibility
    public PlaylistAdapter(Context context, List<Playlist> playlists, OnPlaylistClickListener listener) {
        this(context, playlists, listener, null, null, false, false, null);
    }

    private void loadSavedPlaylists() {
        if (currentUser == null) return;

        playlistRepository.getSavedPlaylists(currentUser.getUid(), new PlaylistRepository.PlaylistsCallback() {
            @Override
            public void onPlaylistsLoaded(List<Playlist> savedPlaylists) {
                savedPlaylistIds.clear();
                for (Playlist playlist : savedPlaylists) {
                    savedPlaylistIds.add(playlist.getId());
                }
                notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                // Just log error, no need to show message for this silent feature
                android.util.Log.e("PlaylistAdapter", "Error loading saved playlists: " + errorMessage);
            }
        });
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

            // Show saved indicator if the playlist is saved
            if (currentUser != null && savedPlaylistIds.contains(playlist.getId())) {
                binding.savedIndicatorIv.setVisibility(View.VISIBLE);
            } else {
                binding.savedIndicatorIv.setVisibility(View.GONE);
            }

            // Set course count
            int courseCount = playlist.getCoursesCount();
            binding.coursesCountTv.setText(courseCount + (courseCount == 1 ? " course" : " courses"));

            // Handle access code display for private playlists in My Playlists view
            if (isMyPlaylistsView && playlist.isPrivate() && playlist.getAccessCode() != null &&
                    currentUser != null && currentUser.getUid().equals(playlist.getCreatorUid())) {

                // Show access code section
                binding.accessCodeLayout.setVisibility(View.VISIBLE);
                binding.accessCodeTv.setText(playlist.getAccessCode());

                // Set up copy functionality for the entire access code layout
                binding.accessCodeLayout.setOnClickListener(v -> copyAccessCodeToClipboard(playlist.getAccessCode()));

            } else {
                // Hide access code section for public playlists or when not in My Playlists
                binding.accessCodeLayout.setVisibility(View.GONE);
            }

            // Set description if available - constrain it properly when access code is shown
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

        private void copyAccessCodeToClipboard(String accessCode) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Playlist Access Code", accessCode);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(context, "Access code '" + accessCode + "' copied to clipboard!", Toast.LENGTH_SHORT).show();
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