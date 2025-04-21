package com.example.coursesharingapp.ui.course;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.coursesharingapp.databinding.FragmentCourseDetailBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.repository.CourseRepository;

public class CourseDetailFragment extends Fragment {

    private FragmentCourseDetailBinding binding;
    private CourseRepository courseRepository;
    private String courseId;
    private ExoPlayer player;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        courseRepository = new CourseRepository();

        // Get courseId from arguments
        if (getArguments() != null) {
            courseId = getArguments().getString("courseId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCourseDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (courseId == null) {
            Toast.makeText(requireContext(), "Course ID is missing", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return;
        }

        // Initialize player
        initializePlayer();

        // Load course details
        loadCourseDetails();
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(requireContext()).build();
        binding.videoView.setPlayer(player);
    }

    private void loadCourseDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);

        courseRepository.getCourseById(courseId, new CourseRepository.SingleCourseCallback() {
            @Override
            public void onCourseLoaded(Course course) {
                binding.progressBar.setVisibility(View.GONE);
                displayCourseDetails(course);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayCourseDetails(Course course) {
        // Set course details
        binding.courseTitleTv.setText(course.getTitle());
        binding.courseUploaderTv.setText("By: " + course.getUploaderUsername());
        binding.courseDescriptionTv.setText(course.getLongDescription());

        // Load video
        if (course.getVideoUrl() != null && !course.getVideoUrl().isEmpty()) {
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(course.getVideoUrl()));
            player.setMediaItem(mediaItem);
            player.prepare();
            // Don't auto-play, wait for user to press play
        } else {
            binding.videoNotAvailableTv.setVisibility(View.VISIBLE);
            binding.videoView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}