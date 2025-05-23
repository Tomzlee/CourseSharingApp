package com.example.coursesharingapp.ui.upload;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
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
import com.example.coursesharingapp.databinding.FragmentUploadCourseBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.repository.AuthRepository;
import com.example.coursesharingapp.repository.CourseRepository;
import com.example.coursesharingapp.repository.UserRepository;
import com.example.coursesharingapp.util.FileSizeNotificationManager;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

public class UploadCourseFragment extends Fragment {

    private static final String TAG = "UploadCourseFragment";
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
    private String selectedCategory;
    private boolean isPrivate = false;
    private String accessCode;

    // Upload progress tracking
    private int thumbnailProgress = 0;
    private int videoProgress = 0;
    private boolean thumbnailUploadComplete = false;
    private boolean videoUploadComplete = false;

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

        setupCategorySpinner();

        setupPrivacyOptions();

        binding.selectThumbnailButton.setOnClickListener(v -> selectThumbnail());
        binding.selectVideoButton.setOnClickListener(v -> selectVideo());
        binding.uploadCourseButton.setOnClickListener(v -> validateAndUploadCourse());
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                Course.CATEGORY_OPTIONS
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(categoryAdapter);

        // Default category selection
        selectedCategory = Course.CATEGORY_OPTIONS[0];
        binding.categorySpinner.setSelection(0);

        // Listen for category selection changes
        binding.categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = Course.CATEGORY_OPTIONS[position];
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Keep default selection
            }
        });
    }

    private void setupPrivacyOptions() {
        // Set up radio button listeners
        binding.privacyRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.public_radio_button) {
                isPrivate = false;
                accessCode = null;
                binding.accessCodeCard.setVisibility(View.GONE);
            } else if (checkedId == R.id.private_radio_button) {
                isPrivate = true;
                accessCode = Course.generateNewAccessCode();
                binding.accessCodeDisplayTv.setText(accessCode);
                binding.accessCodeCard.setVisibility(View.VISIBLE);
            }
        });

        // Default to public
        binding.publicRadioButton.setChecked(true);
    }

    private void getUsernameFromFirestore() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.uploadProgressTv.setText("Loading user information...");

        userRepository.getUserById(currentUser.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(com.example.coursesharingapp.model.User user) {
                binding.progressBar.setVisibility(View.GONE);
                binding.uploadProgressTv.setVisibility(View.GONE);
                username = user.getUsername();
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                binding.uploadProgressTv.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
            }
        });
    }

    private void selectThumbnail() {
        // Show file size notification before proceeding
        FileSizeNotificationManager.showFileSizeLimitNotification(requireContext(),
                new FileSizeNotificationManager.FileSizeNotificationCallback() {
                    @Override
                    public void onProceed() {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, REQUEST_THUMBNAIL);
                    }

                    @Override
                    public void onCancel() {
                        // User cancelled, do nothing
                    }
                });
    }

    private void selectVideo() {
        // Show file size notification before proceeding
        FileSizeNotificationManager.showFileSizeLimitNotification(requireContext(),
                new FileSizeNotificationManager.FileSizeNotificationCallback() {
                    @Override
                    public void onProceed() {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, REQUEST_VIDEO);
                    }

                    @Override
                    public void onCancel() {
                        // User cancelled, do nothing
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_THUMBNAIL) {
                Uri uri = data.getData();
                // Check file size before assigning
                try {
                    long fileSize = getFileSize(uri);
                    if (isFileSizeValid(uri)) {
                        thumbnailUri = uri;
                        binding.thumbnailSelectedTv.setVisibility(View.VISIBLE);

                        // Show file size info
                        String formattedSize = FileSizeNotificationManager.formatFileSize(fileSize);
                        binding.thumbnailSelectedTv.setText("Thumbnail selected (" + formattedSize + ")");
                    } else {
                        String formattedSize = FileSizeNotificationManager.formatFileSize(fileSize);
                        Toast.makeText(requireContext(),
                                "Thumbnail file size exceeds the 5GB limit (" + formattedSize + ")",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking thumbnail size: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error checking file size: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_VIDEO) {
                Uri uri = data.getData();
                // Check file size before assigning
                try {
                    long fileSize = getFileSize(uri);
                    if (isFileSizeValid(uri)) {
                        videoUri = uri;
                        binding.videoSelectedTv.setVisibility(View.VISIBLE);

                        // Show file size info
                        String formattedSize = FileSizeNotificationManager.formatFileSize(fileSize);
                        binding.videoSelectedTv.setText("Video selected (" + formattedSize + ")");
                    } else {
                        String formattedSize = FileSizeNotificationManager.formatFileSize(fileSize);
                        Toast.makeText(requireContext(),
                                "Video file size exceeds the 5GB limit (" + formattedSize + ")",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking video size: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error checking file size: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
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
            Log.e(TAG, "Error with ParcelFileDescriptor: " + e.getMessage());
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
     * Check if file size is within the 5GB limit
     * @param uri File URI to check
     * @return true if file size is valid, false otherwise
     */
    private boolean isFileSizeValid(Uri uri) throws Exception {
        long fileSize = getFileSize(uri);
        return fileSize <= FileSizeNotificationManager.MAX_FILE_SIZE;
    }

    /**
     * Get the actual file path from a URI
     * @param uri The URI to convert
     * @return file path or null if not found
     */
    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Video.Media.DATA};
        Cursor cursor = null;
        String result = null;

        try {
            cursor = requireContext().getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                // Try first column
                int column_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                if (column_index == -1) {
                    // Try second column if first doesn't exist
                    column_index = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
                }

                if (column_index != -1) {
                    result = cursor.getString(column_index);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file path: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
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
        } else {
            // Validate thumbnail file size again
            try {
                if (!isFileSizeValid(thumbnailUri)) {
                    long fileSize = getFileSize(thumbnailUri);
                    String formattedSize = FileSizeNotificationManager.formatFileSize(fileSize);
                    Toast.makeText(requireContext(),
                            "Thumbnail file exceeds the 5GB size limit (" + formattedSize + ")",
                            Toast.LENGTH_SHORT).show();
                    isValid = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error re-validating thumbnail size: " + e.getMessage());
                Toast.makeText(requireContext(), "Error checking thumbnail file size", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
        }

        if (videoUri == null) {
            Toast.makeText(requireContext(), "Please select a video", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else {
            // Validate video file size again
            try {
                if (!isFileSizeValid(videoUri)) {
                    long fileSize = getFileSize(videoUri);
                    String formattedSize = FileSizeNotificationManager.formatFileSize(fileSize);
                    Toast.makeText(requireContext(),
                            "Video file exceeds the 5GB size limit (" + formattedSize + ")",
                            Toast.LENGTH_SHORT).show();
                    isValid = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error re-validating video size: " + e.getMessage());
                Toast.makeText(requireContext(), "Error checking video file size", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
        }

        return isValid;
    }

    private void updateUploadProgress() {
        if (thumbnailUploadComplete && videoUploadComplete) {
            binding.uploadProgressTv.setText("Finalizing upload...");
            return;
        }

        // Calculate overall progress (thumbnail is typically smaller, so weight it less)
        // Assuming thumbnail is 10% of total upload, video is 90%
        int overallProgress = (int) ((thumbnailProgress * 0.1) + (videoProgress * 0.9));

        String progressText;
        if (!thumbnailUploadComplete && !videoUploadComplete) {
            progressText = String.format("Uploading thumbnail: %d%%", thumbnailProgress);
        } else if (thumbnailUploadComplete && !videoUploadComplete) {
            progressText = String.format("Uploading video: %d%%", videoProgress);
        } else {
            progressText = String.format("Upload progress: %d%%", overallProgress);
        }

        binding.uploadProgressTv.setText(progressText);
    }

    private void validateAndUploadCourse() {
        if (!validateInput()) {
            return;
        }

        // Reset progress tracking
        thumbnailProgress = 0;
        videoProgress = 0;
        thumbnailUploadComplete = false;
        videoUploadComplete = false;

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.uploadProgressTv.setVisibility(View.VISIBLE);
        binding.uploadProgressTv.setText("Starting upload...");
        binding.uploadCourseButton.setEnabled(false);

        String title = binding.courseTitleEt.getText().toString().trim();
        String shortDescription = binding.shortDescriptionEt.getText().toString().trim();
        String longDescription = binding.longDescriptionEt.getText().toString().trim();

        // Create course with privacy settings
        Course course = new Course(title, shortDescription, longDescription,
                currentUser.getUid(), username, selectedCategory, isPrivate);

        // If private course already has access code, make sure it's set
        if (isPrivate && accessCode != null) {
            course.setAccessCode(accessCode);
        }

        // Display file sizes in progress message
        try {
            StringBuilder uploadMessage = new StringBuilder("Starting upload");
            if (isPrivate) {
                uploadMessage.append(" of private course");
            } else {
                uploadMessage.append(" of public course");
            }

            long thumbnailSize = getFileSize(thumbnailUri);
            long videoSize = getFileSize(videoUri);

            uploadMessage.append(": Thumbnail (")
                    .append(FileSizeNotificationManager.formatFileSize(thumbnailSize))
                    .append("), Video (")
                    .append(FileSizeNotificationManager.formatFileSize(videoSize))
                    .append(")");

            Toast.makeText(requireContext(), uploadMessage.toString(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // Continue with upload even if we can't display sizes
            Log.e(TAG, "Error getting file sizes for upload message: " + e.getMessage());
        }

        courseRepository.createCourseWithProgress(course, thumbnailUri, videoUri,
                new CourseRepository.UploadProgressCallback() {
                    @Override
                    public void onThumbnailProgress(int progress) {
                        requireActivity().runOnUiThread(() -> {
                            thumbnailProgress = progress;
                            updateUploadProgress();
                        });
                    }

                    @Override
                    public void onVideoProgress(int progress) {
                        requireActivity().runOnUiThread(() -> {
                            videoProgress = progress;
                            updateUploadProgress();
                        });
                    }

                    @Override
                    public void onThumbnailComplete() {
                        requireActivity().runOnUiThread(() -> {
                            thumbnailUploadComplete = true;
                            updateUploadProgress();
                        });
                    }

                    @Override
                    public void onVideoComplete() {
                        requireActivity().runOnUiThread(() -> {
                            videoUploadComplete = true;
                            updateUploadProgress();
                        });
                    }

                    @Override
                    public void onSuccess() {
                        requireActivity().runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.uploadProgressTv.setVisibility(View.GONE);

                            String successMessage = "Course uploaded successfully!";
                            if (isPrivate) {
                                successMessage += "\nAccess Code: " + accessCode + "\nShare this code with people you want to give access to your course.";
                            }

                            Toast.makeText(requireContext(), successMessage, Toast.LENGTH_LONG).show();
                            Navigation.findNavController(requireView()).navigate(R.id.homeFragment);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        requireActivity().runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.uploadProgressTv.setVisibility(View.GONE);
                            binding.uploadCourseButton.setEnabled(true);

                            // Check if error is related to file size
                            if (errorMessage.contains("file is too large") || errorMessage.contains("size exceeds")) {
                                Toast.makeText(requireContext(), "Upload failed: File size exceeds the 5GB limit",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                            }

                            Log.e(TAG, "Upload error: " + errorMessage);
                        });
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}