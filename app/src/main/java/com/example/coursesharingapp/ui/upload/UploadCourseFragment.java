package com.example.coursesharingapp.ui.upload;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.FragmentUploadCourseBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.repository.AuthRepository;
import com.example.coursesharingapp.repository.CourseRepository;
import com.example.coursesharingapp.repository.UserRepository;
import com.google.firebase.auth.FirebaseUser;

public class UploadCourseFragment extends Fragment {

    private static final int REQUEST_THUMBNAIL = 101;
    private static final int REQUEST_VIDEO = 102;

    private FragmentUploadCourseBinding binding;
    private AuthRepository authRepository;
    private UserRepository userRepository;
    private CourseRepository courseRepository;

    private Uri thumbnailUri;
    private Uri videoUri;
    private FirebaseUser currentUser;
    private String username;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepository = new AuthRepository();
        userRepository = new UserRepository();
        courseRepository = new CourseRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadCourseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if user is authenticated
        currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            Navigation.findNavController(requireView()).navigate(R.id.loginFragment);
            return;
        }

        // Get username for the course
        getUsernameFromFirestore();

        // Setup click listeners
        binding.selectThumbnailButton.setOnClickListener(v -> selectThumbnail());
        binding.selectVideoButton.setOnClickListener(v -> selectVideo());
        binding.uploadCourseButton.setOnClickListener(v -> validateAndUploadCourse());
    }

    private void getUsernameFromFirestore() {
        binding.progressBar.setVisibility(View.VISIBLE);

        userRepository.getUserById(currentUser.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(com.example.coursesharingapp.model.User user) {
                binding.progressBar.setVisibility(View.GONE);
                username = user.getUsername();
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
            }
        });
    }

    private void selectThumbnail() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_THUMBNAIL);
    }

    private void selectVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_VIDEO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_THUMBNAIL) {
                thumbnailUri = data.getData();
                binding.thumbnailSelectedTv.setText("Thumbnail selected");
                binding.thumbnailSelectedTv.setVisibility(View.VISIBLE);
            } else if (requestCode == REQUEST_VIDEO) {
                videoUri = data.getData();
                binding.videoSelectedTv.setText("Video selected");
                binding.videoSelectedTv.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean validateInput() {
        String title = binding.courseTitleEt.getText().toString().trim();
        String shortDescription = binding.shortDescriptionEt.getText().toString().trim();
        String longDescription = binding.longDescriptionEt.getText().toString().trim();

        boolean isValid = true;

        if (title.isEmpty()) {
            binding.courseTitleEt.setError("Title cannot be empty");
            isValid = false;
        }

        if (shortDescription.isEmpty()) {
            binding.shortDescriptionEt.setError("Short description cannot be empty");
            isValid = false;
        }

        if (longDescription.isEmpty()) {
            binding.longDescriptionEt.setError("Long description cannot be empty");
            isValid = false;
        }

        if (thumbnailUri == null) {
            Toast.makeText(requireContext(), "Please select a thumbnail image", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (videoUri == null) {
            Toast.makeText(requireContext(), "Please select a video", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void validateAndUploadCourse() {
        if (!validateInput()) {
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.uploadCourseButton.setEnabled(false);

        String title = binding.courseTitleEt.getText().toString().trim();
        String shortDescription = binding.shortDescriptionEt.getText().toString().trim();
        String longDescription = binding.longDescriptionEt.getText().toString().trim();

        Course course = new Course(title, shortDescription, longDescription, currentUser.getUid(), username);

        courseRepository.createCourse(course, thumbnailUri, videoUri, new CourseRepository.CourseCallback() {
            @Override
            public void onSuccess() {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Course uploaded successfully", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigate(R.id.homeFragment);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                binding.uploadCourseButton.setEnabled(true);
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