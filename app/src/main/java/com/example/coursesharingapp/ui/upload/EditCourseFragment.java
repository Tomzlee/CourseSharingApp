package com.example.coursesharingapp.ui.upload;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.FragmentEditCourseBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.repository.CourseRepository;

public class EditCourseFragment extends Fragment {

    private static final int REQUEST_THUMBNAIL = 101;
    private static final int REQUEST_VIDEO = 102;

    private FragmentEditCourseBinding binding;
    private CourseRepository courseRepository;

    private Uri thumbnailUri;
    private Uri videoUri;
    private Course courseToEdit;
    private String courseId;
    private String selectedCategory;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        courseRepository = new CourseRepository();

        // Get course ID from arguments
        if (getArguments() != null) {
            courseId = getArguments().getString("courseId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditCourseBinding.inflate(inflater, container, false);
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

        // Setup category spinner
        setupCategorySpinner();

        // Setup click listeners
        binding.selectThumbnailButton.setOnClickListener(v -> selectThumbnail());
        binding.selectVideoButton.setOnClickListener(v -> selectVideo());
        binding.updateCourseButton.setOnClickListener(v -> validateAndUpdateCourse());

        // Load course details
        loadCourseDetails();
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                Course.CATEGORY_OPTIONS
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(categoryAdapter);

        binding.categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = Course.CATEGORY_OPTIONS[position];
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Keep existing selection
            }
        });
    }

    private void loadCourseDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);

        courseRepository.getCourseById(courseId, new CourseRepository.SingleCourseCallback() {
            @Override
            public void onCourseLoaded(Course course) {
                binding.progressBar.setVisibility(View.GONE);
                courseToEdit = course;
                populateFields(course);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
            }
        });
    }

    private void populateFields(Course course) {
        binding.courseTitleEt.setText(course.getTitle());
        binding.shortDescriptionEt.setText(course.getShortDescription());
        binding.longDescriptionEt.setText(course.getLongDescription());

        // Set category selection
        if (course.getCategory() != null) {
            String[] categories = Course.CATEGORY_OPTIONS;
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(course.getCategory())) {
                    binding.categorySpinner.setSelection(i);
                    selectedCategory = course.getCategory();
                    break;
                }
            }
        }
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
                binding.thumbnailSelectedTv.setText("New thumbnail selected");
                binding.thumbnailSelectedTv.setVisibility(View.VISIBLE);
            } else if (requestCode == REQUEST_VIDEO) {
                videoUri = data.getData();
                binding.videoSelectedTv.setText("New video selected");
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

        return isValid;
    }

    private void validateAndUpdateCourse() {
        if (!validateInput()) {
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.updateCourseButton.setEnabled(false);

        String title = binding.courseTitleEt.getText().toString().trim();
        String shortDescription = binding.shortDescriptionEt.getText().toString().trim();
        String longDescription = binding.longDescriptionEt.getText().toString().trim();

        // Create updated course object
        Course updatedCourse = new Course(title, shortDescription, longDescription,
                courseToEdit.getUploaderUid(), courseToEdit.getUploaderUsername(), selectedCategory);
        updatedCourse.setId(courseId);

        // Keep existing URLs if not changing files
        if (thumbnailUri == null) {
            updatedCourse.setThumbnailUrl(courseToEdit.getThumbnailUrl());
        }
        if (videoUri == null) {
            updatedCourse.setVideoUrl(courseToEdit.getVideoUrl());
        }

        courseRepository.editCourse(updatedCourse, thumbnailUri, videoUri, new CourseRepository.CourseCallback() {
            @Override
            public void onSuccess() {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Course updated successfully", Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                binding.updateCourseButton.setEnabled(true);
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