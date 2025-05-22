package com.example.coursesharingapp.ui.playlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.FragmentPlaylistDetailBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.model.Playlist;
import com.example.coursesharingapp.repository.AuthRepository;
import com.example.coursesharingapp.repository.PlaylistRepository;
import com.example.coursesharingapp.ui.course.CourseAdapter;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistDetailFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private FragmentPlaylistDetailBinding binding;
    private PlaylistRepository playlistRepository;
    private AuthRepository authRepository;
    private CourseAdapter courseAdapter;
    private List<Course> coursesList;
    private String playlistId;
    private FirebaseUser currentUser;
    private boolean isSaved = false;
    private Playlist currentPlaylist;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlistRepository = new PlaylistRepository();
        authRepository = new AuthRepository();
        coursesList = new ArrayList<>();
        currentUser = authRepository.getCurrentUser();

        // Get playlistId from arguments
        if (getArguments() != null) {
            playlistId = getArguments().getString("playlistId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (playlistId == null) {
            Toast.makeText(requireContext(), "Playlist ID is missing", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return;
        }

        binding.savePlaylistButton.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(requireContext(), R.string.please_login_to_save, Toast.LENGTH_SHORT).show();
                return;
            }
            toggleSavePlaylist();
        });

        setupRecyclerView();

        checkIfPlaylistSaved();

        loadPlaylistWithCourses();
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter(requireContext(), coursesList, this);
        binding.playlistCoursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.playlistCoursesRecyclerView.setAdapter(courseAdapter);
    }

    private void checkIfPlaylistSaved() {
        if (currentUser == null || playlistId == null) return;

        playlistRepository.isPlaylistSaved(currentUser.getUid(), playlistId, new PlaylistRepository.IsPlaylistSavedCallback() {
            @Override
            public void onResult(boolean isSaved) {
                PlaylistDetailFragment.this.isSaved = isSaved;
                updateSaveButton();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(requireContext(), "Error checking save status: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSaveButton() {
        if (isSaved) {
            binding.savePlaylistButton.setText(R.string.unsave_playlist);
            binding.savePlaylistButton.setIcon(ContextCompat.getDrawable(requireContext(), android.R.drawable.btn_star_big_on));
        } else {
            binding.savePlaylistButton.setText(R.string.save_playlist);
            binding.savePlaylistButton.setIcon(ContextCompat.getDrawable(requireContext(), android.R.drawable.btn_star_big_off));
        }
    }

    private void toggleSavePlaylist() {
        if (currentUser == null || playlistId == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);

        if (isSaved) {
            // Unsave playlist
            playlistRepository.unsavePlaylist(currentUser.getUid(), playlistId, new PlaylistRepository.PlaylistCallback() {
                @Override
                public void onSuccess() {
                    binding.progressBar.setVisibility(View.GONE);
                    isSaved = false;
                    updateSaveButton();
                    Toast.makeText(requireContext(), R.string.playlist_unsaved, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Save playlist
            playlistRepository.savePlaylist(currentUser.getUid(), playlistId, new PlaylistRepository.PlaylistCallback() {
                @Override
                public void onSuccess() {
                    binding.progressBar.setVisibility(View.GONE);
                    isSaved = true;
                    updateSaveButton();
                    Toast.makeText(requireContext(), R.string.playlist_saved, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadPlaylistWithCourses() {
        binding.progressBar.setVisibility(View.VISIBLE);

        playlistRepository.getPlaylistWithCourses(playlistId, new PlaylistRepository.PlaylistWithCoursesCallback() {
            @Override
            public void onPlaylistWithCoursesLoaded(Playlist playlist, List<Course> courses) {
                binding.progressBar.setVisibility(View.GONE);
                currentPlaylist = playlist;
                displayPlaylistDetails(playlist);
                displayCourses(courses, playlist);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayPlaylistDetails(Playlist playlist) {
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
    }

    private void displayCourses(List<Course> courses, Playlist playlist) {
        if (courses.isEmpty()) {
            binding.noCoursesTv.setVisibility(View.VISIBLE);
            binding.playlistCoursesRecyclerView.setVisibility(View.GONE);
        } else {
            binding.noCoursesTv.setVisibility(View.GONE);
            binding.playlistCoursesRecyclerView.setVisibility(View.VISIBLE);

            // Create a map for quick lookups of courses by id
            Map<String, Course> courseMap = new HashMap<>();
            for (Course course : courses) {
                courseMap.put(course.getId(), course);
            }

            // Create an ordered list based on the playlist's courseIds order
            List<Course> orderedCourses = new ArrayList<>();
            for (String courseId : playlist.getCourseIds()) {
                Course course = courseMap.get(courseId);
                if (course != null) {
                    orderedCourses.add(course);
                }
            }

            // Update the adapter with ordered courses
            coursesList.clear();
            coursesList.addAll(orderedCourses);
            courseAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCourseClick(Course course, int position) {
        // Navigate to course detail
        Bundle args = new Bundle();
        args.putString("courseId", course.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_to_courseDetail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}