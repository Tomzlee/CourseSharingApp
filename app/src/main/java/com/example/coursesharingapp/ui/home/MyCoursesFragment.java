package com.example.coursesharingapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.FragmentMyCoursesBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.repository.AuthRepository;
import com.example.coursesharingapp.repository.CourseRepository;
import com.example.coursesharingapp.ui.course.CourseAdapter;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MyCoursesFragment extends Fragment implements
        CourseAdapter.OnCourseClickListener,
        CourseAdapter.OnCourseDeleteListener,
        CourseAdapter.OnCourseEditListener {

    private FragmentMyCoursesBinding binding;
    private CourseRepository courseRepository;
    private AuthRepository authRepository;
    private CourseAdapter courseAdapter;
    private List<Course> courseList;
    private FirebaseUser currentUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        courseRepository = new CourseRepository();
        authRepository = new AuthRepository();
        courseList = new ArrayList<>();
        currentUser = authRepository.getCurrentUser();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyCoursesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // If somehow we got here without a user, redirect to login
        if (currentUser == null) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.loginFragment);
            return;
        }

        setupRecyclerView();

        loadMyCourses();
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter(requireContext(), courseList,
                this, this, this, true, true);
        binding.coursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.coursesRecyclerView.setAdapter(courseAdapter);
    }

    private void loadMyCourses() {
        binding.progressBar.setVisibility(View.VISIBLE);

        courseRepository.getCoursesByUploader(currentUser.getUid(), new CourseRepository.CoursesCallback() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                binding.progressBar.setVisibility(View.GONE);

                if (courses.isEmpty()) {
                    binding.noCoursesTv.setVisibility(View.VISIBLE);
                    binding.coursesRecyclerView.setVisibility(View.GONE);
                } else {
                    binding.noCoursesTv.setVisibility(View.GONE);
                    binding.coursesRecyclerView.setVisibility(View.VISIBLE);
                    courseList.clear();
                    courseList.addAll(courses);
                    courseAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCourseClick(Course course, int position) {
        // Navigate to course detail using the root NavController
        Bundle args = new Bundle();
        args.putString("courseId", course.getId());
        // Get the NavController from the activity's NavHostFragment
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.courseDetailFragment, args);
    }

    @Override
    public void onCourseDelete(Course course, int position) {
        // Show confirmation dialog
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_course)
                .setMessage(R.string.confirm_delete_course)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteCourse(course, position))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onCourseEdit(Course course, int position) {
        // Navigate to edit course screen using the root NavController
        Bundle args = new Bundle();
        args.putString("courseId", course.getId());
        // Get the NavController from the activity's NavHostFragment
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.editCourseFragment, args);
    }

    private void deleteCourse(Course course, int position) {
        binding.progressBar.setVisibility(View.VISIBLE);

        courseRepository.deleteCourse(course.getId(), new CourseRepository.CourseCallback() {
            @Override
            public void onSuccess() {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), R.string.course_deleted_successfully, Toast.LENGTH_SHORT).show();

                // Remove the course from the list
                courseList.remove(position);
                courseAdapter.notifyItemRemoved(position);

                // Check if list is now empty
                if (courseList.isEmpty()) {
                    binding.noCoursesTv.setVisibility(View.VISIBLE);
                    binding.coursesRecyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
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