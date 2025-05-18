package com.example.coursesharingapp.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.FragmentProfileBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.model.Playlist;
import com.example.coursesharingapp.model.User;
import com.example.coursesharingapp.repository.AuthRepository;
import com.example.coursesharingapp.repository.CourseRepository;
import com.example.coursesharingapp.repository.PlaylistRepository;
import com.example.coursesharingapp.repository.UserRepository;
import com.example.coursesharingapp.ui.course.CourseAdapter;
import com.example.coursesharingapp.ui.playlist.PlaylistAdapter;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment implements
        CourseAdapter.OnCourseClickListener,
        CourseAdapter.OnCourseDeleteListener,
        PlaylistAdapter.OnPlaylistClickListener,
        PlaylistAdapter.OnPlaylistDeleteListener {

    private FragmentProfileBinding binding;
    private AuthRepository authRepository;
    private UserRepository userRepository;
    private CourseRepository courseRepository;
    private PlaylistRepository playlistRepository;
    private CourseAdapter courseAdapter;
    private PlaylistAdapter playlistAdapter;
    private List<Course> savedCoursesList;
    private List<Playlist> savedPlaylistsList;
    private FirebaseUser currentUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepository = new AuthRepository();
        userRepository = new UserRepository();
        courseRepository = new CourseRepository();
        playlistRepository = new PlaylistRepository();
        savedCoursesList = new ArrayList<>();
        savedPlaylistsList = new ArrayList<>();
        currentUser = authRepository.getCurrentUser();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if user is authenticated
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        // Set up sign out button
        binding.signOutButton.setOnClickListener(v -> {
            authRepository.signOut();
            navigateToLogin();
        });

        // Setup RecyclerView for saved courses
        setupCourseRecyclerView();

        // Setup RecyclerView for saved playlists
        setupPlaylistRecyclerView();

        // Load user profile
        loadUserProfile(currentUser.getUid());

        // Load user's saved courses
        loadSavedCourses(currentUser.getUid());

        // Load user's saved playlists
        loadSavedPlaylists(currentUser.getUid());
    }

    private void navigateToLogin() {
        NavController navController = Navigation.findNavController(requireView());
        // Create NavOptions to clear the back stack
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.homeFragment, true)  // Pop up to home (exclusive)
                .build();
        navController.navigate(R.id.loginFragment, null, navOptions);
    }

    private void setupCourseRecyclerView() {
        // We're using the delete listener to "unsave" courses, not truly delete them
        courseAdapter = new CourseAdapter(requireContext(), savedCoursesList, this, this, false);
        binding.userCoursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.userCoursesRecyclerView.setAdapter(courseAdapter);
    }

    private void setupPlaylistRecyclerView() {
        // We're using the delete listener to "unsave" playlists, not truly delete them
        playlistAdapter = new PlaylistAdapter(requireContext(), savedPlaylistsList, this, this, null, false, false, currentUser.getUid());
        binding.savedPlaylistsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.savedPlaylistsRecyclerView.setAdapter(playlistAdapter);
    }

    private void loadUserProfile(String userId) {
        binding.profileProgressBar.setVisibility(View.VISIBLE);

        userRepository.getUserById(userId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                binding.profileProgressBar.setVisibility(View.GONE);

                // Display user info
                binding.usernameTv.setText(user.getUsername());
                binding.emailTv.setText(user.getEmail());
            }

            @Override
            public void onError(String errorMessage) {
                binding.profileProgressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSavedCourses(String userId) {
        binding.coursesProgressBar.setVisibility(View.VISIBLE);

        courseRepository.getSavedCourses(userId, new CourseRepository.CoursesCallback() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                binding.coursesProgressBar.setVisibility(View.GONE);

                if (courses.isEmpty()) {
                    binding.noUserCoursesTv.setVisibility(View.VISIBLE);
                    binding.userCoursesRecyclerView.setVisibility(View.GONE);
                } else {
                    binding.noUserCoursesTv.setVisibility(View.GONE);
                    binding.userCoursesRecyclerView.setVisibility(View.VISIBLE);
                    savedCoursesList.clear();
                    savedCoursesList.addAll(courses);
                    courseAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.coursesProgressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSavedPlaylists(String userId) {
        binding.playlistsProgressBar.setVisibility(View.VISIBLE);

        playlistRepository.getSavedPlaylists(userId, new PlaylistRepository.PlaylistsCallback() {
            @Override
            public void onPlaylistsLoaded(List<Playlist> playlists) {
                binding.playlistsProgressBar.setVisibility(View.GONE);

                if (playlists.isEmpty()) {
                    binding.noPlaylistsTv.setVisibility(View.VISIBLE);
                    binding.savedPlaylistsRecyclerView.setVisibility(View.GONE);
                } else {
                    binding.noPlaylistsTv.setVisibility(View.GONE);
                    binding.savedPlaylistsRecyclerView.setVisibility(View.VISIBLE);
                    savedPlaylistsList.clear();
                    savedPlaylistsList.addAll(playlists);
                    playlistAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.playlistsProgressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCourseClick(Course course, int position) {
        // Navigate to course detail
        Bundle args = new Bundle();
        args.putString("courseId", course.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_to_courseDetail, args);
    }

    @Override
    public void onPlaylistClick(Playlist playlist, int position) {
        // Navigate to playlist detail
        Bundle args = new Bundle();
        args.putString("playlistId", playlist.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_to_playlistDetail, args);
    }

    @Override
    public void onCourseDelete(Course course, int position) {
        // In this context, "delete" means "unsave"
        if (currentUser == null) return;

        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.unsave_course)
                .setMessage(R.string.confirm_unsave_course)
                .setPositiveButton(R.string.unsave, (dialog, which) -> unsaveCourse(currentUser.getUid(), course, position))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onPlaylistDelete(Playlist playlist, int position) {
        // In this context, "delete" means "unsave"
        if (currentUser == null) return;

        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.unsave_playlist)
                .setMessage(R.string.confirm_unsave_playlist)
                .setPositiveButton(R.string.unsave, (dialog, which) -> unsavePlaylist(currentUser.getUid(), playlist, position))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void unsaveCourse(String userId, Course course, int position) {
        binding.coursesProgressBar.setVisibility(View.VISIBLE);

        courseRepository.unsaveCourse(userId, course.getId(), new CourseRepository.CourseCallback() {
            @Override
            public void onSuccess() {
                binding.coursesProgressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), R.string.course_unsaved, Toast.LENGTH_SHORT).show();

                // Remove the course from the list
                savedCoursesList.remove(position);
                courseAdapter.notifyItemRemoved(position);

                // Check if list is empty
                if (savedCoursesList.isEmpty()) {
                    binding.noUserCoursesTv.setVisibility(View.VISIBLE);
                    binding.userCoursesRecyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.coursesProgressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unsavePlaylist(String userId, Playlist playlist, int position) {
        binding.playlistsProgressBar.setVisibility(View.VISIBLE);

        playlistRepository.unsavePlaylist(userId, playlist.getId(), new PlaylistRepository.PlaylistCallback() {
            @Override
            public void onSuccess() {
                binding.playlistsProgressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), R.string.playlist_unsaved, Toast.LENGTH_SHORT).show();

                // Remove the playlist from the list
                savedPlaylistsList.remove(position);
                playlistAdapter.notifyItemRemoved(position);

                // Check if list is empty
                if (savedPlaylistsList.isEmpty()) {
                    binding.noPlaylistsTv.setVisibility(View.VISIBLE);
                    binding.savedPlaylistsRecyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.playlistsProgressBar.setVisibility(View.GONE);
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