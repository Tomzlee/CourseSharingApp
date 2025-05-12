package com.example.coursesharingapp.ui.playlist;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
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
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistsFragment extends Fragment implements PlaylistAdapter.OnPlaylistClickListener,
        PlaylistAdapter.OnPlaylistDeleteListener, PlaylistAdapter.OnPlaylistEditListener {
    private static final String TAG = "PlaylistDebug";

    private FragmentPlaylistsBinding binding;
    private PlaylistRepository playlistRepository;
    private UserRepository userRepository;
    private CourseRepository courseRepository;
    private AuthRepository authRepository;
    private PlaylistAdapter playlistAdapter;
    private List<Playlist> playlistsList;
    private FirebaseUser currentUser;

    // Search state variables
    private String currentSearchQuery = "";
    private boolean isSearchActive = false;

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

        // Setup search functionality
        setupSearchBar();

        // Setup FAB for creating playlists
        binding.createPlaylistFab.setOnClickListener(v -> {
            if (currentUser != null) {
                showCreatePlaylistDialog();
            } else {
                Toast.makeText(requireContext(), getString(R.string.please_log_in_to_create_playlists), Toast.LENGTH_SHORT).show();
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

                // Reset search when changing tabs
                resetSearch();

                if (position == TAB_ALL_PLAYLISTS) {
                    loadAllPlaylists();
                } else if (position == TAB_MY_PLAYLISTS) {
                    if (currentUser != null) {
                        loadMyPlaylists();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.please_log_in_to_view_your_playlists), Toast.LENGTH_SHORT).show();
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

    private void setupSearchBar() {
        // Set up search functionality using the end icon (search icon)
        binding.searchLayout.setEndIconOnClickListener(v -> {
            String query = binding.searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            } else {
                // If search bar is empty, reset search and show all playlists
                resetSearch();

                // Refresh the current tab view
                int currentTab = binding.tabs.getSelectedTabPosition();
                if (currentTab == TAB_ALL_PLAYLISTS) {
                    loadAllPlaylists();
                } else if (currentTab == TAB_MY_PLAYLISTS && currentUser != null) {
                    loadMyPlaylists();
                }
            }
        });

        // Handle search on keyboard action
        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = binding.searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                } else {
                    // If search bar is empty, reset search and show all playlists
                    resetSearch();

                    // Refresh the current tab view
                    int currentTab = binding.tabs.getSelectedTabPosition();
                    if (currentTab == TAB_ALL_PLAYLISTS) {
                        loadAllPlaylists();
                    } else if (currentTab == TAB_MY_PLAYLISTS && currentUser != null) {
                        loadMyPlaylists();
                    }
                }
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        Log.d(TAG, "Performing search with query: " + query);
        currentSearchQuery = query;
        isSearchActive = true;
        binding.progressBar.setVisibility(View.VISIBLE);

        int currentTab = binding.tabs.getSelectedTabPosition();
        if (currentTab == TAB_ALL_PLAYLISTS) {
            // Search across all playlists
            playlistRepository.searchAllPlaylists(query, new PlaylistRepository.PlaylistsCallback() {
                @Override
                public void onPlaylistsLoaded(List<Playlist> playlists) {
                    binding.progressBar.setVisibility(View.GONE);
                    updatePlaylistsList(playlists, false, false);
                    binding.noPlaylistsTv.setText(playlists.isEmpty() ?
                            "No playlists matching: " + query :
                            getString(R.string.no_playlists_available));
                }

                @Override
                public void onError(String errorMessage) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else if (currentTab == TAB_MY_PLAYLISTS && currentUser != null) {
            // Search only user's playlists
            playlistRepository.searchUserPlaylists(currentUser.getUid(), query, new PlaylistRepository.PlaylistsCallback() {
                @Override
                public void onPlaylistsLoaded(List<Playlist> playlists) {
                    binding.progressBar.setVisibility(View.GONE);
                    updatePlaylistsList(playlists, true, true);
                    binding.noPlaylistsTv.setText(playlists.isEmpty() ?
                            "No playlists matching: " + query :
                            getString(R.string.no_playlists_available));
                }

                @Override
                public void onError(String errorMessage) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void resetSearch() {
        // Clear search state
        isSearchActive = false;
        currentSearchQuery = "";
        binding.searchEditText.setText("");
        binding.noPlaylistsTv.setText(R.string.no_playlists_available);
    }

    private void loadAllPlaylists() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // Skip if search is active - let the search handle it
        if (isSearchActive) {
            performSearch(currentSearchQuery);
            return;
        }

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

        // Skip if search is active - let the search handle it
        if (isSearchActive) {
            performSearch(currentSearchQuery);
            return;
        }

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

                // If search is active, refresh the search
                if (isSearchActive) {
                    performSearch(currentSearchQuery);
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Validate input for playlist creation/editing
    private boolean validateInput(String title, List<String> selectedCourseIds) {
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a playlist title", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (selectedCourseIds.isEmpty() || selectedCourseIds.size() < 2) {
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

                        // Reload the current tab
                        if (binding.tabs.getSelectedTabPosition() == TAB_ALL_PLAYLISTS) {
                            loadAllPlaylists();
                        } else {
                            loadMyPlaylists();
                        }
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

    private void showManageCoursesDialog(Playlist playlist, int position, Dialog parentDialog,
                                         List<Course> existingCourses,
                                         OrderableCourseDisplayAdapter displayAdapter) {
        Log.d(TAG, "showManageCoursesDialog started");

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        DialogManagePlaylistCoursesBinding dialogBinding =
                DialogManagePlaylistCoursesBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());
        dialog.setCancelable(true);

        // Set up RecyclerView
        RecyclerView coursesRecyclerView = dialogBinding.coursesRecyclerView;
        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Show progress
        dialogBinding.progressBar.setVisibility(View.VISIBLE);
        dialogBinding.instructionsTv.setText("Loading your courses...");

        // Show dialog immediately
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Load current user's courses
        courseRepository.getCoursesByUploader(currentUser.getUid(), new CourseRepository.CoursesCallback() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                Log.d(TAG, "User courses loaded: " + courses.size());

                requireActivity().runOnUiThread(() -> {
                    dialogBinding.progressBar.setVisibility(View.GONE);
                    dialogBinding.instructionsTv.setText(
                            "Check/uncheck courses to add or remove them from the playlist.");

                    if (courses.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "You haven't uploaded any courses", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        return;
                    }

                    // Create adapter with all courses
                    OrderableCourseAdapter adapter = new OrderableCourseAdapter(requireContext(), courses);

                    // Setup drag-and-drop
                    ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                            ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

                        @Override
                        public boolean onMove(@NonNull RecyclerView recyclerView,
                                              @NonNull RecyclerView.ViewHolder viewHolder,
                                              @NonNull RecyclerView.ViewHolder target) {
                            int fromPosition = viewHolder.getAdapterPosition();
                            int toPosition = target.getAdapterPosition();
                            adapter.moveItem(fromPosition, toPosition);
                            return true;
                        }

                        @Override
                        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                            // Not using swipe
                        }

                        @Override
                        public boolean isLongPressDragEnabled() {
                            return false;
                        }

                        @Override
                        public boolean isItemViewSwipeEnabled() {
                            return false;
                        }
                    };

                    ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
                    touchHelper.attachToRecyclerView(coursesRecyclerView);
                    adapter.setTouchHelper(touchHelper);

                    // Pre-select courses already in playlist
                    adapter.setSelectedCourseIds(playlist.getCourseIds());

                    // Set adapter
                    coursesRecyclerView.setAdapter(adapter);

                    // Set up save button
                    dialogBinding.saveButton.setOnClickListener(v -> {
                        Log.d(TAG, "Save button clicked in manage courses dialog");
                        List<String> selectedCourseIds = adapter.getSelectedCourseIds();
                        Log.d(TAG, "Selected course IDs: " + selectedCourseIds.size());

                        // Validate selection
                        if (selectedCourseIds.isEmpty() || selectedCourseIds.size() < 2) {
                            Toast.makeText(requireContext(),
                                    "Please select at least 2 courses", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Create ordered list of Course objects
                        Map<String, Course> courseMap = new HashMap<>();
                        for (Course course : courses) {
                            courseMap.put(course.getId(), course);
                        }

                        List<Course> selectedCourses = new ArrayList<>();
                        for (String id : selectedCourseIds) {
                            Course course = courseMap.get(id);
                            if (course != null) {
                                selectedCourses.add(course);
                            }
                        }

                        Log.d(TAG, "Created selected courses list with " + selectedCourses.size() + " items");

                        // Update existing courses list
                        existingCourses.clear();
                        existingCourses.addAll(selectedCourses);

                        // Force adapter update
                        displayAdapter.notifyDataSetChanged();

                        // Update playlist course IDs (not saved to database yet)
                        playlist.setCourseIds(selectedCourseIds);

                        Log.d(TAG, "Closing manage courses dialog");
                        dialog.dismiss();
                    });
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading user courses: " + errorMessage);
                requireActivity().runOnUiThread(() -> {
                    dialogBinding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }
        });

        // Set up cancel button
        dialogBinding.cancelButton.setOnClickListener(v -> {
            Log.d(TAG, "Cancel button clicked in manage courses dialog");
            dialog.dismiss();
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
                    OrderableCourseAdapter adapter = new OrderableCourseAdapter(requireContext(), courses);

                    // Setup ItemTouchHelper for drag & drop functionality
                    ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                            ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

                        @Override
                        public boolean onMove(@NonNull RecyclerView recyclerView,
                                              @NonNull RecyclerView.ViewHolder viewHolder,
                                              @NonNull RecyclerView.ViewHolder target) {
                            int fromPosition = viewHolder.getAdapterPosition();
                            int toPosition = target.getAdapterPosition();
                            adapter.moveItem(fromPosition, toPosition);
                            return true;
                        }

                        @Override
                        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                            // Not using swipe functionality
                        }

                        @Override
                        public boolean isLongPressDragEnabled() {
                            // Disable long press drag (we'll use the drag handle icon instead)
                            return false;
                        }

                        @Override
                        public boolean isItemViewSwipeEnabled() {
                            // Disable swipe
                            return false;
                        }
                    };

                    ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
                    touchHelper.attachToRecyclerView(selectCoursesRecyclerView);
                    adapter.setTouchHelper(touchHelper);

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

    private void showEditPlaylistDialog(Playlist playlist, int position) {
        // Log the start of the method
        Log.d(TAG, "showEditPlaylistDialog started for playlist: " + playlist.getTitle());
        Log.d(TAG, "Course IDs in playlist: " + playlist.getCourseIds().size());

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        DialogEditPlaylistBinding dialogBinding = DialogEditPlaylistBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());
        dialog.setCancelable(true);

        // Pre-fill current values
        dialogBinding.playlistTitleEt.setText(playlist.getTitle());
        dialogBinding.playlistDescriptionEt.setText(playlist.getDescription());

        // Display something in the title to indicate loading
        dialogBinding.currentCoursesTv.setText("Current Course Order (Loading...)");

        // Set up the RecyclerView before loading data
        RecyclerView selectedCoursesRecyclerView = dialogBinding.selectedCoursesRecyclerView;
        selectedCoursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Create a mutable list that will hold our course data
        List<Course> coursesList = new ArrayList<>();

        // Create the adapter with the empty list
        OrderableCourseDisplayAdapter orderedAdapter = new OrderableCourseDisplayAdapter(
                requireContext(), coursesList);

        // Set adapter to RecyclerView immediately
        selectedCoursesRecyclerView.setAdapter(orderedAdapter);

        // Setup touch helper
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                orderedAdapter.moveItem(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Not using swipe
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }
        };

        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(selectedCoursesRecyclerView);
        orderedAdapter.setTouchHelper(touchHelper);

        // Show dialog right away so user doesn't wait
        dialog.show();

        // Manually set dialog width
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Check if we need to load any courses
        if (playlist.getCourseIds() == null || playlist.getCourseIds().isEmpty()) {
            // No courses to load
            dialogBinding.currentCoursesTv.setText("Current Course Order (No courses yet)");
            Log.d(TAG, "No courses in playlist");
        } else {
            // Show loading indicator
            dialogBinding.progressBar.setVisibility(View.VISIBLE);

            // Count how many courses we need to load
            final int[] coursesToLoad = {playlist.getCourseIds().size()};
            Log.d(TAG, "Starting to load " + coursesToLoad[0] + " courses");

            // Keep track of loaded courses by ID
            final Map<String, Course> coursesById = new HashMap<>();

            // For each course ID in the playlist
            for (String courseId : playlist.getCourseIds()) {
                Log.d(TAG, "Loading course ID: " + courseId);

                // Load the course
                courseRepository.getCourseById(courseId, new CourseRepository.SingleCourseCallback() {
                    @Override
                    public void onCourseLoaded(Course course) {
                        Log.d(TAG, "Course loaded: " + course.getTitle());

                        // Store by ID for proper ordering
                        coursesById.put(course.getId(), course);

                        // Decrement counter
                        coursesToLoad[0]--;

                        // Check if all courses are loaded
                        if (coursesToLoad[0] <= 0) {
                            Log.d(TAG, "All courses loaded, updating UI");

                            // All courses loaded, update UI on main thread
                            requireActivity().runOnUiThread(() -> {
                                try {
                                    // Create ordered list based on playlist course IDs
                                    List<Course> orderedCourses = new ArrayList<>();
                                    for (String id : playlist.getCourseIds()) {
                                        Course c = coursesById.get(id);
                                        if (c != null) {
                                            orderedCourses.add(c);
                                        }
                                    }

                                    Log.d(TAG, "Ordered courses list created with " +
                                            orderedCourses.size() + " items");

                                    // Update adapter with ordered courses
                                    coursesList.clear();
                                    coursesList.addAll(orderedCourses);

                                    // Explicitly notify adapter of changes
                                    orderedAdapter.notifyDataSetChanged();

                                    // Update title
                                    dialogBinding.currentCoursesTv.setText(
                                            "Current Course Order (" + coursesList.size() + " courses)");

                                    // Hide progress bar
                                    dialogBinding.progressBar.setVisibility(View.GONE);

                                    // Force layout
                                    selectedCoursesRecyclerView.post(() -> {
                                        selectedCoursesRecyclerView.invalidate();
                                    });

                                    Log.d(TAG, "UI updated with course list");

                                } catch (Exception e) {
                                    Log.e(TAG, "Error updating UI: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Error loading course: " + errorMessage);

                        // Decrement counter even on error
                        coursesToLoad[0]--;

                        // Check if all attempts are done
                        if (coursesToLoad[0] <= 0) {
                            requireActivity().runOnUiThread(() -> {
                                // Hide progress
                                dialogBinding.progressBar.setVisibility(View.GONE);

                                // Update with whatever courses we have
                                if (coursesById.isEmpty()) {
                                    dialogBinding.currentCoursesTv.setText(
                                            "Current Course Order (Error loading courses)");
                                } else {
                                    // Use what we have
                                    List<Course> orderedCourses = new ArrayList<>();
                                    for (String id : playlist.getCourseIds()) {
                                        Course c = coursesById.get(id);
                                        if (c != null) {
                                            orderedCourses.add(c);
                                        }
                                    }

                                    coursesList.clear();
                                    coursesList.addAll(orderedCourses);
                                    orderedAdapter.notifyDataSetChanged();

                                    dialogBinding.currentCoursesTv.setText(
                                            "Current Course Order (" + coursesList.size() + " courses)");
                                }
                            });
                        }
                    }
                });
            }
        }

        // Set up the manage courses button
        dialogBinding.manageCoursesButton.setOnClickListener(v -> {
            Log.d(TAG, "Manage courses button clicked");
            showManageCoursesDialog(playlist, position, dialog, coursesList, orderedAdapter);
        });

        // Set up the update button
        dialogBinding.updatePlaylistButton.setOnClickListener(v -> {
            Log.d(TAG, "Update playlist button clicked");

            String title = dialogBinding.playlistTitleEt.getText().toString().trim();
            String description = dialogBinding.playlistDescriptionEt.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a playlist title", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check the actual adapter for current order
            List<String> orderedCourseIds = orderedAdapter.getCourseIds();
            Log.d(TAG, "Ordered course IDs count: " + orderedCourseIds.size());

            // Need at least 2 courses
            if (orderedCourseIds.size() < 2) {
                Toast.makeText(requireContext(),
                        "Please add at least 2 courses to your playlist",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Update the playlist
            playlist.setTitle(title);
            playlist.setDescription(description);
            playlist.setCourseIds(orderedCourseIds);

            // Show progress
            dialogBinding.progressBar.setVisibility(View.VISIBLE);

            // Save to database
            playlistRepository.updatePlaylist(playlist, new PlaylistRepository.PlaylistCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Playlist updated successfully");
                    requireActivity().runOnUiThread(() -> {
                        dialogBinding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                "Playlist updated successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                        // Update UI list
                        playlistsList.set(position, playlist);
                        playlistAdapter.notifyItemChanged(position);

                        // Refresh results if in search mode
                        if (isSearchActive) {
                            performSearch(currentSearchQuery);
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error updating playlist: " + errorMessage);
                    requireActivity().runOnUiThread(() -> {
                        dialogBinding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        // Set up cancel button
        dialogBinding.cancelButton.setOnClickListener(v -> {
            Log.d(TAG, "Cancel button clicked");
            dialog.dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}