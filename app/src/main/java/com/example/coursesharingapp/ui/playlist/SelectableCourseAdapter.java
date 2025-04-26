package com.example.coursesharingapp.ui.playlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.coursesharingapp.R;
import com.example.coursesharingapp.model.Course;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectableCourseAdapter extends RecyclerView.Adapter<SelectableCourseAdapter.CourseViewHolder> {

    private Context context;
    private List<Course> courses;
    private Map<String, Boolean> selectedCourses; // Maps course ID to selection state

    public SelectableCourseAdapter(Context context, List<Course> courses) {
        this.context = context;
        this.courses = courses;
        this.selectedCourses = new HashMap<>();

        // Initialize all courses as unselected
        for (Course course : courses) {
            selectedCourses.put(course.getId(), false);
        }
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selectable_course, parent, false);
        return new CourseViewHolder(view);
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

    // Get list of selected course IDs
    public List<String> getSelectedCourseIds() {
        List<String> selectedIds = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : selectedCourses.entrySet()) {
            if (entry.getValue()) {
                selectedIds.add(entry.getKey());
            }
        }
        return selectedIds;
    }

    public class CourseViewHolder extends RecyclerView.ViewHolder {
        private ImageView thumbnailImageView;
        private TextView titleTextView;
        private CheckBox selectCheckBox;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImageView = itemView.findViewById(R.id.course_thumbnail_iv);
            titleTextView = itemView.findViewById(R.id.course_title_tv);
            selectCheckBox = itemView.findViewById(R.id.course_checkbox);
        }

        public void bind(Course course, int position) {
            // Set course title
            titleTextView.setText(course.getTitle());

            // Load thumbnail
            if (course.getThumbnailUrl() != null && !course.getThumbnailUrl().isEmpty()) {
                Glide.with(context)
                        .load(course.getThumbnailUrl())
                        .placeholder(R.drawable.ic_placeholder_thumbnail)
                        .error(R.drawable.ic_error_thumbnail)
                        .centerCrop()
                        .into(thumbnailImageView);
            } else {
                thumbnailImageView.setImageResource(R.drawable.ic_placeholder_thumbnail);
            }

            // Set checkbox state from our selection map
            Boolean isSelected = selectedCourses.get(course.getId());
            selectCheckBox.setChecked(isSelected != null && isSelected);

            // Set click listeners
            View.OnClickListener clickListener = v -> {
                boolean newState = !selectCheckBox.isChecked();
                selectCheckBox.setChecked(newState);
                selectedCourses.put(course.getId(), newState);
            };

            // Make both checkbox and the whole item clickable
            selectCheckBox.setOnClickListener(v -> {
                selectedCourses.put(course.getId(), selectCheckBox.isChecked());
            });

            itemView.setOnClickListener(clickListener);
        }
    }
}