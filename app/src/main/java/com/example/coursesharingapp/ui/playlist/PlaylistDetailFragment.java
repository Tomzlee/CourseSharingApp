package com.example.coursesharingapp.ui.playlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.FragmentPlaylistDetailBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.model.Playlist;
import com.example.coursesharingapp.repository.PlaylistRepository;
import com.example.coursesharingapp.ui.course.CourseAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistDetailFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private FragmentPlaylistDetailBinding binding;
    private PlaylistRepository playlistRepository;
    private CourseAdapter courseAdapter;
    private List<Course> coursesList;
    private String playlistId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlistRepository = new PlaylistRepository();
        coursesList = new ArrayList<>();

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

        // Setup RecyclerView
        setupRecyclerView();

        // Load playlist details and courses
        loadPlaylistWithCourses();
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter(requireContext(), coursesList, this);
        binding.playlistCoursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.playlistCoursesRecyclerView.setAdapter(courseAdapter);
    }

    private void loadPlaylistWithCourses() {
        binding.progressBar.setVisibility(View.VISIBLE);

        playlistRepository.getPlaylistWithCourses(playlistId, new PlaylistRepository.PlaylistWithCoursesCallback() {
            @Override
            public void onPlaylistWithCoursesLoaded(Playlist playlist, List<Course> courses) {
                binding.progressBar.setVisibility(View.GONE);
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