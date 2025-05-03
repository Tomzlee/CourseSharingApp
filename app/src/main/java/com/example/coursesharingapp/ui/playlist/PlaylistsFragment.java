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
import com.example.coursesharingapp.databinding.FragmentPlaylistsBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.model.Playlist;
import com.example.coursesharingapp.model.User;
import com.example.coursesharingapp.repository.AuthRepository;
import com.example.coursesharingapp.repository.CourseRepository;
import com.example.coursesharingapp.repository.PlaylistRepository;
import com.example.coursesharingapp.repository.UserRepository;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class PlaylistsFragment extends Fragment implements PlaylistAdapter.OnPlaylistClickListener {

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
        playlistAdapter = new PlaylistAdapter(requireContext(), playlistsList, this);
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
                updatePlaylistsList(playlists);
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
                updatePlaylistsList(playlists);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePlaylistsList(List<Playlist> playlists) {
        if (playlists.isEmpty()) {
            binding.noPlaylistsTv.setVisibility(View.VISIBLE);
            binding.playlistsRecyclerView.setVisibility(View.GONE);
        } else {
            binding.noPlaylistsTv.setVisibility(View.GONE);
            binding.playlistsRecyclerView.setVisibility(View.VISIBLE);
        }

        playlistsList.clear();
        playlistsList.addAll(playlists);
        playlistAdapter.notifyDataSetChanged();
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
    public void onPlaylistClick(Playlist playlist, int position) {
        // Navigate to playlist detail
        Bundle args = new Bundle();
        args.putString("playlistId", playlist.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_to_playlistDetail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}