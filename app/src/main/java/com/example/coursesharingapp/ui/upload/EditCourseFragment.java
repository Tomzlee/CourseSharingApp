package com.example.coursesharingapp.ui.upload;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
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

import java.io.File;

public class EditCourseFragment extends Fragment {

    private static final int REQUEST_THUMBNAIL = 101;
    private static final int REQUEST_VIDEO = 102;

    // Define the maximum file size: 5GB in bytes
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024; // 5GB

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

        setupCategorySpinner();

        binding.selectThumbnailButton.setOnClickListener(v -> selectThumbnail());
        binding.selectVideoButton.setOnClickListener(v -> selectVideo());
        binding.updateCourseButton.setOnClickListener(v -> validateAndUpdateCourse());

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
                Uri uri = data.getData();
                // Check file size before assigning
                try {
                    if (isFileSizeValid(uri)) {
                        thumbnailUri = uri;
                        binding.thumbnailSelectedTv.setVisibility(View.VISIBLE);

                        // Show file size info
                        long fileSizeInMb = getFileSize(uri) / (1024 * 1024);
                        binding.thumbnailSelectedTv.setText("New thumbnail selected (" + fileSizeInMb + " MB)");
                    } else {
                        Toast.makeText(requireContext(), "Thumbnail file size exceeds the 5GB limit", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error checking file size: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_VIDEO) {
                Uri uri = data.getData();
                // Check file size before assigning
                try {
                    if (isFileSizeValid(uri)) {
                        videoUri = uri;
                        binding.videoSelectedTv.setVisibility(View.VISIBLE);

                        // Show file size info
                        long fileSizeInMb = getFileSize(uri) / (1024 * 1024);
                        binding.videoSelectedTv.setText("New video selected (" + fileSizeInMb + " MB)");
                    } else {
                        Toast.makeText(requireContext(), "Video file size exceeds the 5GB limit", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error checking file size: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Check if file size is within the 5GB limit
     * @param uri File URI to check
     * @return true if file size is valid, false otherwise
     */
    private boolean isFileSizeValid(Uri uri) throws Exception {
        long fileSize = getFileSize(uri);
        return fileSize <= MAX_FILE_SIZE;
    }

    /**
     * Get the file size from a URI
     * @param uri The URI to check
     * @return file size in bytes
     */
    private long getFileSize(Uri uri) throws Exception {
        ContentResolver contentResolver = requireContext().getContentResolver();

        // First try using OpenableColumns
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            // If size column exists and is not null
            if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                long size = cursor.getLong(sizeIndex);
                cursor.close();
                return size;
            }
            cursor.close();
        }

        // If we couldn't get size from cursor, try with ParcelFileDescriptor
        try {
            android.os.ParcelFileDescriptor parcelFileDescriptor =
                    contentResolver.openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                long size = parcelFileDescriptor.getStatSize();
                parcelFileDescriptor.close();
                return size;
            }
        } catch (Exception e) {
            // Log the error but continue trying other methods
            e.printStackTrace();
        }

        // As a last resort, try to get file path and check size
        String path = getPathFromUri(uri);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                return file.length();
            }
        }

        // If all methods fail, throw an exception
        throw new Exception("Could not determine file size");
    }

    /**
     * Get the actual file path from a URI
     * @param uri The URI to convert
     * @return file path or null if not found
     */
    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Video.Media.DATA};
        Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        return null;
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

        // Check thumbnailUri file size if provided
        if (thumbnailUri != null) {
            try {
                if (!isFileSizeValid(thumbnailUri)) {
                    Toast.makeText(requireContext(), "Thumbnail file exceeds the 5GB size limit", Toast.LENGTH_SHORT).show();
                    isValid = false;
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error checking thumbnail file size", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
        }

        // Check videoUri file size if provided
        if (videoUri != null) {
            try {
                if (!isFileSizeValid(videoUri)) {
                    Toast.makeText(requireContext(), "Video file exceeds the 5GB size limit", Toast.LENGTH_SHORT).show();
                    isValid = false;
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error checking video file size", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
        }

        return isValid;
    }

    private void validateAndUpdateCourse() {
        if (!validateInput()) {
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.editProgressTv.setVisibility(View.VISIBLE);
        binding.editProgressTv.setText("Starting update...");
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

        // Display file sizes in progress message if new files are selected
        StringBuilder uploadMessage = new StringBuilder("Updating course");
        try {
            if (thumbnailUri != null) {
                long thumbnailSizeMb = getFileSize(thumbnailUri) / (1024 * 1024);
                uploadMessage.append(", Thumbnail: ").append(thumbnailSizeMb).append(" MB");
            }
            if (videoUri != null) {
                long videoSizeMb = getFileSize(videoUri) / (1024 * 1024);
                uploadMessage.append(", Video: ").append(videoSizeMb).append(" MB");
            }
            if (thumbnailUri != null || videoUri != null) {
                Toast.makeText(requireContext(), uploadMessage.toString(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            // Continue with upload even if we can't display sizes
        }

        // Use new method with progress tracking if files are being updated
        if (thumbnailUri != null || videoUri != null) {
            courseRepository.editCourseWithProgress(updatedCourse, thumbnailUri, videoUri,
                    new CourseRepository.EditProgressCallback() {
                        @Override
                        public void onThumbnailProgress(int progress) {
                            requireActivity().runOnUiThread(() -> {
                                binding.editProgressTv.setText(String.format("Updating thumbnail: %d%%", progress));
                            });
                        }

                        @Override
                        public void onVideoProgress(int progress) {
                            requireActivity().runOnUiThread(() -> {
                                binding.editProgressTv.setText(String.format("Updating video: %d%%", progress));
                            });
                        }

                        @Override
                        public void onThumbnailComplete() {
                            requireActivity().runOnUiThread(() -> {
                                binding.editProgressTv.setText("Thumbnail update complete...");
                            });
                        }

                        @Override
                        public void onVideoComplete() {
                            requireActivity().runOnUiThread(() -> {
                                binding.editProgressTv.setText("Video update complete...");
                            });
                        }

                        @Override
                        public void onSuccess() {
                            requireActivity().runOnUiThread(() -> {
                                binding.progressBar.setVisibility(View.GONE);
                                binding.editProgressTv.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Course updated successfully", Toast.LENGTH_SHORT).show();
                                requireActivity().onBackPressed();
                            });
                        }

                        @Override
                        public void onError(String errorMessage) {
                            requireActivity().runOnUiThread(() -> {
                                binding.progressBar.setVisibility(View.GONE);
                                binding.editProgressTv.setVisibility(View.GONE);
                                binding.updateCourseButton.setEnabled(true);

                                // Check if error is related to file size
                                if (errorMessage.contains("file is too large") || errorMessage.contains("size exceeds")) {
                                    Toast.makeText(requireContext(), "Update failed: File size exceeds the 5GB limit",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
        } else {
            // No files to update, just update course info
            binding.editProgressTv.setText("Updating course information...");
            courseRepository.editCourse(updatedCourse, null, null, new CourseRepository.CourseCallback() {
                @Override
                public void onSuccess() {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.editProgressTv.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Course updated successfully", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                }

                @Override
                public void onError(String errorMessage) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.editProgressTv.setVisibility(View.GONE);
                    binding.updateCourseButton.setEnabled(true);
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}