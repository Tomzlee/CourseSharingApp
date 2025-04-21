package com.example.coursesharingapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.FragmentHomeBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.repository.AuthRepository;
import com.example.coursesharingapp.repository.CourseRepository;
import com.example.coursesharingapp.ui.course.CourseAdapter;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private FragmentHomeBinding binding;
    private AuthRepository authRepository;
    private CourseRepository courseRepository;
    private CourseAdapter courseAdapter;
    private List<Course> courseList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepository = new AuthRepository();
        courseRepository = new CourseRepository();
        courseList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if user is authenticated
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            Navigation.findNavController(requireView()).navigate(R.id.loginFragment);
            return;
        }

        // Setup RecyclerView
        setupRecyclerView();

        // Set up floating action button for adding new courses
        binding.addCourseFab.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.action_to_uploadCourse));

        // Load courses
        loadCourses();
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter(requireContext(), courseList, this);
        binding.coursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.coursesRecyclerView.setAdapter(courseAdapter);
    }

    private void loadCourses() {
        binding.progressBar.setVisibility(View.VISIBLE);

        courseRepository.getAllCourses(new CourseRepository.CoursesCallback() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                binding.progressBar.setVisibility(View.GONE);

                if (courses.isEmpty()) {
                    binding.noCoursesTv.setVisibility(View.VISIBLE);
                } else {
                    binding.noCoursesTv.setVisibility(View.GONE);
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
        // Navigate to course detail
        Bundle args = new Bundle();
        args.putString("courseId", course.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_to_courseDetail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}