package com.example.coursesharingapp.ui.playlist;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.DialogCreatePlaylistBinding;
import com.example.coursesharingapp.databinding.DialogEditPlaylistBinding;
import com.example.coursesharingapp.databinding.DialogManagePlaylistCoursesBinding;
import com.example.coursesharingapp.databinding.FragmentPlaylistsBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.model.Playlist;
import com.example.coursesharingapp.model.User;
import com.example.coursesharingapp.repository.AuthRepository;
import com.example.coursesharingapp.repository.CourseRepository;
import com.example.coursesharingapp.repository.PlaylistRepository;
import com.example.coursesharingapp.repository.UserRepository;
import com.example.coursesharingapp.ui.course.CourseAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class PlaylistsFragment extends Fragment implements PlaylistAdapter.OnPlaylistClickListener,
        PlaylistAdapter.OnPlaylistDeleteListener, PlaylistAdapter.OnPlaylistEditListener {

    private FragmentPlaylistsBinding binding;
    private PlaylistRepository playlistRepository;
    private UserRepository userRepository;
    private CourseRepository courseRepository;
    private AuthRepository authRepository;
    private PlaylistAdapter playlistAdapter;
    private List<Playlist> playlistsList;
    private FirebaseUser currentUser;

    private static final int TAB_ALL_PLAYLISTS = 0;
    private static final int TAB_MY_PLAYLISTS = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlistRepository = new PlaylistRepository();
        courseRepository = new CourseRepository();
        authRepository = new AuthRepository();
        playlistsList = new ArrayList<>();
        userRepository = new UserRepository();

        // Check if user is authenticated
        currentUser = authRepository.getCurrentUser();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPlaylistsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup RecyclerView
        setupRecyclerView();

        // Setup tabs
        setupTabs();

        // Setup FAB for creating playlists
        binding.createPlaylistFab.setOnClickListener(v -> {
            if (currentUser != null) {
                showCreatePlaylistDialog();
            } else {
                Toast.makeText(requireContext(), "Please log in to create playlists", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigate(R.id.loginFragment);
            }
        });

        // Load playlists
        loadAllPlaylists();
    }

    private void setupRecyclerView() {
        // Initialize with appropriate constructor based on current tab
        boolean showEdit = binding.tabs.getSelectedTabPosition() == TAB_MY_PLAYLISTS;
        boolean showDelete = binding.tabs.getSelectedTabPosition() == TAB_MY_PLAYLISTS;
        String uid = currentUser != null ? currentUser.getUid() : null;

        playlistAdapter = new PlaylistAdapter(requireContext(), playlistsList, this, this, this, showDelete, showEdit, uid);
        binding.playlistsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.playlistsRecyclerView.setAdapter(playlistAdapter);
    }

    private void setupTabs() {
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == TAB_ALL_PLAYLISTS) {
                    loadAllPlaylists();
                } else if (position == TAB_MY_PLAYLISTS) {
                    if (currentUser != null) {
                        loadMyPlaylists();
                    } else {
                        Toast.makeText(requireContext(), "Please log in to view your playlists", Toast.LENGTH_SHORT).show();
                        binding.tabs.selectTab(binding.tabs.getTabAt(TAB_ALL_PLAYLISTS));
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });
    }

    private void loadAllPlaylists() {
        binding.progressBar.setVisibility(View.VISIBLE);

        playlistRepository.getAllPlaylists(new PlaylistRepository.PlaylistsCallback() {
            @Override
            public void onPlaylistsLoaded(List<Playlist> playlists) {
                binding.progressBar.setVisibility(View.GONE);
                updatePlaylistsList(playlists, false, false);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMyPlaylists() {
        binding.progressBar.setVisibility(View.VISIBLE);

        playlistRepository.getPlaylistsByCreator(currentUser.getUid(), new PlaylistRepository.PlaylistsCallback() {
            @Override
            public void onPlaylistsLoaded(List<Playlist> playlists) {
                binding.progressBar.setVisibility(View.GONE);
                updatePlaylistsList(playlists, true, true);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePlaylistsList(List<Playlist> playlists, boolean showDeleteButtons, boolean showEditButtons) {
        if (playlists.isEmpty()) {
            binding.noPlaylistsTv.setVisibility(View.VISIBLE);
            binding.playlistsRecyclerView.setVisibility(View.GONE);
        } else {
            binding.noPlaylistsTv.setVisibility(View.GONE);
            binding.playlistsRecyclerView.setVisibility(View.VISIBLE);
        }

        playlistsList.clear();
        playlistsList.addAll(playlists);

        // Create new adapter with updated settings
        String uid = currentUser != null ? currentUser.getUid() : null;
        playlistAdapter = new PlaylistAdapter(requireContext(), playlistsList, this, this, this, showDeleteButtons, showEditButtons, uid);
        binding.playlistsRecyclerView.setAdapter(playlistAdapter);
    }

    @Override
    public void onPlaylistClick(Playlist playlist, int position) {
        // Navigate to playlist detail
        Bundle args = new Bundle();
        args.putString("playlistId", playlist.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_to_playlistDetail, args);
    }

    @Override
    public void onPlaylistDelete(Playlist playlist, int position) {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete this playlist?")
                .setPositiveButton("Delete", (dialog, which) -> deletePlaylist(playlist, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onPlaylistEdit(Playlist playlist, int position) {
        showEditPlaylistDialog(playlist, position);
    }

    private void deletePlaylist(Playlist playlist, int position) {
        binding.progressBar.setVisibility(View.VISIBLE);

        playlistRepository.deletePlaylist(playlist.getId(), new PlaylistRepository.PlaylistCallback() {
            @Override
            public void onSuccess() {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Playlist deleted successfully", Toast.LENGTH_SHORT).show();

                // Remove the playlist from the list and refresh the adapter
                playlistsList.remove(position);
                playlistAdapter.notifyItemRemoved(position);

                // Check if list is empty
                if (playlistsList.isEmpty()) {
                    binding.noPlaylistsTv.setVisibility(View.VISIBLE);
                    binding.playlistsRecyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditPlaylistDialog(Playlist playlist, int position) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        DialogEditPlaylistBinding dialogBinding = DialogEditPlaylistBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());
        dialog.setCancelable(true);

        // Pre-fill current values
        dialogBinding.playlistTitleEt.setText(playlist.getTitle());
        dialogBinding.playlistDescriptionEt.setText(playlist.getDescription());

        // Set up the selected courses RecyclerView
        RecyclerView selectedCoursesRecyclerView = dialogBinding.selectedCoursesRecyclerView;
        selectedCoursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Load current courses in the playlist
        List<Course> currentCourses = new ArrayList<>();
        loadPlaylistCourses(playlist.getId(), currentCourses, selectedCoursesRecyclerView);

        // Set up the manage courses button
        dialogBinding.manageCoursesButton.setOnClickListener(v -> {
            showManageCoursesDialog(playlist, position, dialog);
        });

        // Set up the update button
        dialogBinding.updatePlaylistButton.setOnClickListener(v -> {
            String title = dialogBinding.playlistTitleEt.getText().toString().trim();
            String description = dialogBinding.playlistDescriptionEt.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a playlist title", Toast.LENGTH_SHORT).show();
                return;
            }

            updatePlaylist(playlist, title, description, position, dialog, dialogBinding);
        });

        // Set up cancel button
        dialogBinding.cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Show dialog
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showManageCoursesDialog(Playlist playlist, int position, Dialog parentDialog) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        DialogManagePlaylistCoursesBinding dialogBinding = DialogManagePlaylistCoursesBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());
        dialog.setCancelable(true);

        // Set up RecyclerView
        RecyclerView coursesRecyclerView = dialogBinding.coursesRecyclerView;
        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Show progress bar
        dialogBinding.progressBar.setVisibility(View.VISIBLE);

        // Load current user's courses
        courseRepository.getCoursesByUploader(currentUser.getUid(), new CourseRepository.CoursesCallback() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                dialogBinding.progressBar.setVisibility(View.GONE);

                if (courses.isEmpty()) {
                    Toast.makeText(requireContext(), "You haven't uploaded any courses", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    return;
                }

                // Create adapter with pre-selected courses
                SelectableCourseAdapter adapter = new SelectableCourseAdapter(requireContext(), courses);

                // Pre-select courses that are already in the playlist
                for (int i = 0; i < courses.size(); i++) {
                    Course course = courses.get(i);
                    if (playlist.getCourseIds().contains(course.getId())) {
                        // This is the fix - we need to set the selected state before setting the adapter
                        adapter.getSelectedCourses().put(course.getId(), true);
                    }
                }

                // Properly set the adapter to the RecyclerView
                coursesRecyclerView.setAdapter(adapter);

                // Set up save button
                dialogBinding.saveButton.setOnClickListener(v -> {
                    List<String> selectedCourseIds = adapter.getSelectedCourseIds();

                    // Validate selection
                    if (selectedCourseIds.isEmpty() || selectedCourseIds.size() < 2) {
                        Toast.makeText(requireContext(), "Please select at least 2 courses", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Update playlist courses
                    playlist.setCourseIds(selectedCourseIds);
                    dialog.dismiss();

                    // Update the selected courses RecyclerView in the parent dialog
                    updateSelectedCoursesView(playlist.getId(), dialogBinding);
                });
            }

            @Override
            public void onError(String errorMessage) {
                dialogBinding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        // Set up cancel button
        dialogBinding.cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Show dialog
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
    private void loadPlaylistCourses(String playlistId, List<Course> coursesList, RecyclerView recyclerView) {
        playlistRepository.getPlaylistWithCourses(playlistId, new PlaylistRepository.PlaylistWithCoursesCallback() {
            @Override
            public void onPlaylistWithCoursesLoaded(Playlist playlist, List<Course> courses) {
                coursesList.clear();
                coursesList.addAll(courses);

                // Create a simple adapter to display the current courses
                CourseAdapter adapter = new CourseAdapter(requireContext(), coursesList, (course, pos) -> {
                    // Handle course click if needed
                });
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSelectedCoursesView(String playlistId, DialogManagePlaylistCoursesBinding dialogBinding) {
        List<Course> currentCourses = new ArrayList<>();
        loadPlaylistCourses(playlistId, currentCourses, dialogBinding.coursesRecyclerView);
    }

    private void updatePlaylist(
            Playlist playlist,
            String title,
            String description,
            int position,
            Dialog dialog,
            DialogEditPlaylistBinding dialogBinding // <-- passed directly
    ) {
        // Update playlist data
        playlist.setTitle(title);
        playlist.setDescription(description);

        // Show progress bar
        dialogBinding.progressBar.setVisibility(View.VISIBLE);

        // Update playlist in Firestore
        playlistRepository.updatePlaylist(playlist, new PlaylistRepository.PlaylistCallback() {
            @Override
            public void onSuccess() {
                dialogBinding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Playlist updated successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                // Update the playlist in the list and notify adapter
                playlistsList.set(position, playlist);
                playlistAdapter.notifyItemChanged(position);
            }

            @Override
            public void onError(String errorMessage) {
                dialogBinding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreatePlaylistDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        DialogCreatePlaylistBinding dialogBinding = DialogCreatePlaylistBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());
        dialog.setCancelable(true);

        // Initialize views and recycler view
        RecyclerView selectCoursesRecyclerView = dialogBinding.selectCoursesRecyclerView;
        selectCoursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Load user's courses
        dialogBinding.progressBar.setVisibility(View.VISIBLE);

        courseRepository.getCoursesByUploader(currentUser.getUid(), new CourseRepository.CoursesCallback() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                dialogBinding.progressBar.setVisibility(View.GONE);

                if (courses.isEmpty()) {
                    dialogBinding.noCoursesTv.setVisibility(View.VISIBLE);
                    dialogBinding.selectCoursesRecyclerView.setVisibility(View.GONE);
                    dialogBinding.createPlaylistButton.setEnabled(false);
                } else {
                    // Create and set adapter
                    SelectableCourseAdapter adapter = new SelectableCourseAdapter(requireContext(), courses);
                    selectCoursesRecyclerView.setAdapter(adapter);

                    // Set up create button
                    dialogBinding.createPlaylistButton.setOnClickListener(v -> {
                        String title = dialogBinding.playlistTitleEt.getText().toString().trim();
                        String description = dialogBinding.playlistDescriptionEt.getText().toString().trim();
                        List<String> selectedCourseIds = adapter.getSelectedCourseIds();

                        if (validateInput(title, selectedCourseIds)) {
                            createPlaylist(title, description, selectedCourseIds, dialog);
                        }
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                dialogBinding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        // Set up cancel button
        dialogBinding.cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Show dialog
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private boolean validateInput(String title, List<String> selectedCourseIds) {
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a playlist title", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (selectedCourseIds.isEmpty() || selectedCourseIds.size() == 1) {
            Toast.makeText(requireContext(), "Please select at least two courses", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void createPlaylist(String title, String description, List<String> courseIds, Dialog dialog) {
        binding.progressBar.setVisibility(View.VISIBLE);

        // First get the username from Firestore
        userRepository.getUserById(currentUser.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                // Now create the playlist with the username
                Playlist playlist = new Playlist(title, description, currentUser.getUid(), user.getUsername());
                playlist.setCourseIds(courseIds);

                playlistRepository.createPlaylist(playlist, new PlaylistRepository.PlaylistCallback() {
                    @Override
                    public void onSuccess() {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Playlist created successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadAllPlaylists();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}