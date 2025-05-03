package com.example.coursesharingapp.ui.course;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.ItemCourseBinding;
import com.example.coursesharingapp.model.Course;
import com.google.android.material.chip.Chip;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private Context context;
    private List<Course> courses;
    private OnCourseClickListener clickListener;
    private OnCourseDeleteListener deleteListener;
    private OnCourseEditListener editListener;
    private boolean showDeleteButton;
    private boolean showEditButton;

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