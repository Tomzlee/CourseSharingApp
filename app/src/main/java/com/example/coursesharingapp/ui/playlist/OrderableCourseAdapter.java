package com.example.coursesharingapp.ui.playlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.ItemOrderableCourseBinding;
import com.example.coursesharingapp.model.Course;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for courses that can be both selected and reordered via drag & drop
 */
public class OrderableCourseAdapter extends RecyclerView.Adapter<OrderableCourseAdapter.CourseViewHolder> {

    private Context context;
    private List<Course> courses;
    private List<Course> selectedCourses; // Maintains order of selected courses
    private Map<String, Boolean> selectedMap; // Maps course ID to selection state
    private ItemTouchHelper touchHelper;

    public OrderableCourseAdapter(Context context, List<Course> courses) {
        this.context = context;
        this.courses = courses;
        this.selectedCourses = new ArrayList<>();
        this.selectedMap = new HashMap<>();

        // Initialize all courses as unselected
        for (Course course : courses) {
            selectedMap.put(course.getId(), false);
        }
    }

    public void setTouchHelper(ItemTouchHelper touchHelper) {
        this.touchHelper = touchHelper;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderableCourseBinding binding = ItemOrderableCourseBinding.inflate(
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

    /**
     * Sets the initial selected state for courses
     * @param courseIds List of course IDs that should be selected
     */
    public void setSelectedCourseIds(List<String> courseIds) {
        // Clear current selection
        selectedMap.clear();
        selectedCourses.clear();

        // Initialize all as unselected first
        for (Course course : courses) {
            selectedMap.put(course.getId(), false);
        }

        // Mark selected courses
        if (courseIds != null) {
            // Maintain order by following the courseIds list order
            for (String courseId : courseIds) {
                selectedMap.put(courseId, true);

                // Find and add the corresponding Course object to selectedCourses
                for (Course course : courses) {
                    if (course.getId().equals(courseId)) {
                        selectedCourses.add(course);
                        break;
                    }
                }
            }
        }

        notifyDataSetChanged();
    }


    //Get list of selected course IDs in the current order
    public List<String> getSelectedCourseIds() {
        List<String> selectedIds = new ArrayList<>();
        for (Course course : selectedCourses) {
            selectedIds.add(course.getId());
        }
        return selectedIds;
    }


    //Move a course in the selected list
    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(selectedCourses, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(selectedCourses, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    public class CourseViewHolder extends RecyclerView.ViewHolder {
        private ItemOrderableCourseBinding binding;

        public CourseViewHolder(@NonNull ItemOrderableCourseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Course course, int position) {
            // Set course title
            binding.courseTitleTv.setText(course.getTitle());

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

            // Set checkbox state from our selection map
            Boolean isSelected = selectedMap.get(course.getId());
            binding.courseCheckbox.setChecked(isSelected != null && isSelected);

            // Show/hide drag handle based on selection status
            if (isSelected != null && isSelected) {
                binding.dragHandle.setVisibility(View.VISIBLE);
            } else {
                binding.dragHandle.setVisibility(View.GONE);
            }

            // Set up checkbox click listener
            binding.courseCheckbox.setOnClickListener(v -> {
                boolean newState = binding.courseCheckbox.isChecked();
                selectedMap.put(course.getId(), newState);

                if (newState) {
                    // Add to selected courses if not already there
                    if (!selectedCourses.contains(course)) {
                        selectedCourses.add(course);
                    }
                    binding.dragHandle.setVisibility(View.VISIBLE);
                } else {
                    // Remove from selected courses
                    selectedCourses.remove(course);
                    binding.dragHandle.setVisibility(View.GONE);
                }
            });

            // Set up item click to toggle checkbox
            itemView.setOnClickListener(v -> {
                boolean newState = !binding.courseCheckbox.isChecked();
                binding.courseCheckbox.setChecked(newState);
                selectedMap.put(course.getId(), newState);

                if (newState) {
                    // Add to selected courses if not already there
                    if (!selectedCourses.contains(course)) {
                        selectedCourses.add(course);
                    }
                    binding.dragHandle.setVisibility(View.VISIBLE);
                } else {
                    // Remove from selected courses
                    selectedCourses.remove(course);
                    binding.dragHandle.setVisibility(View.GONE);
                }
            });

            // Setup drag handle touch listener
            binding.dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (touchHelper != null) {
                        touchHelper.startDrag(this);
                    }
                }
                return false;
            });
        }
    }
}