package com.example.coursesharingapp.ui.course;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.ItemCourseBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.repository.AuthRepository;
import com.example.coursesharingapp.repository.CourseRepository;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private Context context;
    private List<Course> courses;
    private OnCourseClickListener clickListener;
    private OnCourseDeleteListener deleteListener;
    private OnCourseEditListener editListener;
    private boolean showDeleteButton;
    private boolean showEditButton;
    private FirebaseUser currentUser;
    private CourseRepository courseRepository;
    private Set<String> savedCourseIds = new HashSet<>();
    private boolean isMyCoursesView; // New flag to track if this is "My Courses" view

    // Constructor with delete and edit functionality
    public CourseAdapter(Context context, List<Course> courses, OnCourseClickListener clickListener,
                         OnCourseDeleteListener deleteListener, OnCourseEditListener editListener,
                         boolean showDeleteButton, boolean showEditButton) {
        this.context = context;
        this.courses = courses;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
        this.showDeleteButton = showDeleteButton;
        this.showEditButton = showEditButton;
        this.isMyCoursesView = showDeleteButton || showEditButton; // If showing edit/delete, it's My Courses

        // Initialize Firebase-related objects
        AuthRepository authRepository = new AuthRepository();
        this.currentUser = authRepository.getCurrentUser();
        this.courseRepository = new CourseRepository();

        // Load saved state for all courses if the user is logged in
        if (currentUser != null) {
            loadSavedCourses();
        }
    }

    // Constructor with delete button parameter
    public CourseAdapter(Context context, List<Course> courses, OnCourseClickListener clickListener,
                         OnCourseDeleteListener deleteListener, boolean showDeleteButton) {
        this(context, courses, clickListener, deleteListener, null, showDeleteButton, false);
    }

    // Original constructor for backward compatibility
    public CourseAdapter(Context context, List<Course> courses, OnCourseClickListener clickListener) {
        this(context, courses, clickListener, null, null, false, false);
    }

    private void loadSavedCourses() {
        if (currentUser == null) return;

        courseRepository.getSavedCourses(currentUser.getUid(), new CourseRepository.CoursesCallback() {
            @Override
            public void onCoursesLoaded(List<Course> savedCourses) {
                savedCourseIds.clear();
                for (Course course : savedCourses) {
                    savedCourseIds.add(course.getId());
                }
                notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                // Just log error, no need to show message for this silent feature
                android.util.Log.e("CourseAdapter", "Error loading saved courses: " + errorMessage);
            }
        });
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCourseBinding binding = ItemCourseBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new CourseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.bind(course, position);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    public class CourseViewHolder extends RecyclerView.ViewHolder {
        private ItemCourseBinding binding;

        public CourseViewHolder(@NonNull ItemCourseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Course course, int position) {
            // Set course data
            binding.courseTitleTv.setText(course.getTitle());
            binding.courseDescriptionTv.setText(course.getShortDescription());
            binding.courseUploaderTv.setText("By: " + course.getUploaderUsername());

            // Show saved indicator if the course is saved
            if (currentUser != null && savedCourseIds.contains(course.getId())) {
                binding.savedIndicatorIv.setVisibility(View.VISIBLE);
            } else {
                binding.savedIndicatorIv.setVisibility(View.GONE);
            }

            // Handle access code display for private courses in My Courses view
            if (isMyCoursesView && course.isPrivate() && course.getAccessCode() != null &&
                    currentUser != null && currentUser.getUid().equals(course.getUploaderUid())) {

                // Show access code section
                binding.accessCodeLayout.setVisibility(View.VISIBLE);
                binding.accessCodeTv.setText(course.getAccessCode());

                // Set up copy functionality for the entire access code layout
                binding.accessCodeLayout.setOnClickListener(v -> copyAccessCodeToClipboard(course.getAccessCode()));

            } else {
                // Hide access code section for public courses or when not in My Courses
                binding.accessCodeLayout.setVisibility(View.GONE);
            }

            // Set category chip
            if (course.getCategory() != null && !course.getCategory().isEmpty()) {
                binding.courseCategoryChip.setText(course.getCategory());
                binding.courseCategoryChip.setVisibility(View.VISIBLE);

                // Set chip color based on category
                int chipColorResId = getCategoryColor(course.getCategory());
                binding.courseCategoryChip.setChipBackgroundColorResource(chipColorResId);
            } else {
                binding.courseCategoryChip.setVisibility(View.GONE);
            }

            // Load thumbnail
            if (course.getThumbnailUrl() != null && !course.getThumbnailUrl().isEmpty()) {
                Glide.with(context)
                        .load(course.getThumbnailUrl())
                        .placeholder(R.drawable.ic_placeholder_thumbnail)
                        .error(R.drawable.ic_error_thumbnail)
                        .centerCrop()
                        .into(binding.courseThumbnailIv);
            } else {
                binding.courseThumbnailIv.setImageResource(R.drawable.ic_placeholder_thumbnail);
            }

            // Set click listener
            binding.getRoot().setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onCourseClick(course, position);
                }
            });

            // Show/hide delete button based on flag
            if (showDeleteButton && deleteListener != null) {
                binding.deleteCourseButton.setVisibility(View.VISIBLE);
                binding.deleteCourseButton.setOnClickListener(v -> deleteListener.onCourseDelete(course, position));
            } else {
                binding.deleteCourseButton.setVisibility(View.GONE);
            }

            // Show/hide edit button based on flag
            if (showEditButton && editListener != null) {
                binding.editCourseButton.setVisibility(View.VISIBLE);
                binding.editCourseButton.setOnClickListener(v -> editListener.onCourseEdit(course, position));
            } else {
                binding.editCourseButton.setVisibility(View.GONE);
            }
        }

        private void copyAccessCodeToClipboard(String accessCode) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Course Access Code", accessCode);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(context, "Access code '" + accessCode + "' copied to clipboard!", Toast.LENGTH_SHORT).show();
        }

        private int getCategoryColor(String category) {
            switch (category) {
                case Course.CATEGORY_ART:
                    return android.R.color.holo_purple;
                case Course.CATEGORY_TECH:
                    return android.R.color.holo_blue_light;
                case Course.CATEGORY_BUSINESS:
                    return android.R.color.holo_orange_light;
                case Course.CATEGORY_LIFE:
                    return android.R.color.holo_green_light;
                case Course.CATEGORY_OTHER:
                default:
                    return android.R.color.darker_gray;
            }
        }
    }

    public interface OnCourseClickListener {
        void onCourseClick(Course course, int position);
    }

    public interface OnCourseDeleteListener {
        void onCourseDelete(Course course, int position);
    }

    public interface OnCourseEditListener {
        void onCourseEdit(Course course, int position);
    }
}