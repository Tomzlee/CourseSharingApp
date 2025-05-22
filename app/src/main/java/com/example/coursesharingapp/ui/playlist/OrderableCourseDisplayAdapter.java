package com.example.coursesharingapp.ui.playlist;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.ItemOrderedCourseBinding;
import com.example.coursesharingapp.model.Course;

import java.util.Collections;
import java.util.List;

/**
 * Simplified adapter for displaying ordered courses in a playlist
 */
public class OrderableCourseDisplayAdapter extends RecyclerView.Adapter<OrderableCourseDisplayAdapter.CourseViewHolder> {

    private static final String TAG = "CourseDisplayAdapter";
    private final Context context;
    private final List<Course> courses; // Direct reference to the list passed in constructor
    private ItemTouchHelper touchHelper;

    public OrderableCourseDisplayAdapter(Context context, List<Course> courses) {
        this.context = context;
        this.courses = courses; // Store direct reference - we'll manipulate the original list
        Log.d(TAG, "Adapter created with list of size: " + this.courses.size());
    }

    public void setTouchHelper(ItemTouchHelper touchHelper) {
        this.touchHelper = touchHelper;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "Creating view holder");
        ItemOrderedCourseBinding binding = ItemOrderedCourseBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new CourseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        if (position < 0 || position >= courses.size()) {
            Log.e(TAG, "Invalid position: " + position + ", list size: " + courses.size());
            return;
        }

        Course course = courses.get(position);
        Log.d(TAG, "Binding position " + position + ", course: " + course.getTitle());
        holder.bind(course, position);
    }

    @Override
    public int getItemCount() {
        int size = courses.size();
        Log.d(TAG, "getItemCount called, returning: " + size);
        return size;
    }


    //Get course IDs in their current order

    public List<String> getCourseIds() {
        return courses.stream()
                .map(Course::getId)
                .collect(java.util.stream.Collectors.toList());
    }


    //Move a course in the list

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= courses.size() ||
                toPosition < 0 || toPosition >= courses.size()) {
            Log.e(TAG, "Invalid positions: from=" + fromPosition + ", to=" + toPosition);
            return;
        }

        Log.d(TAG, "Moving item from " + fromPosition + " to " + toPosition);

        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(courses, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(courses, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    public class CourseViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrderedCourseBinding binding;

        public CourseViewHolder(@NonNull ItemOrderedCourseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Course course, int position) {
            // Set position number
            binding.orderNumberTv.setText(String.valueOf(position + 1));

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

            // Setup drag handle
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