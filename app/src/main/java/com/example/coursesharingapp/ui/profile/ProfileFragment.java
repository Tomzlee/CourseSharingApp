package com.example.coursesharingapp.ui.profile;

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
import com.example.coursesharingapp.databinding.FragmentProfileBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.model.User;
import com.example.coursesharingapp.repository.AuthRepository;
import com.example.coursesharingapp.repository.CourseRepository;
import com.example.coursesharingapp.repository.UserRepository;
import com.example.coursesharingapp.ui.course.CourseAdapter;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private FragmentProfileBinding binding;
    private AuthRepository authRepository;
    private UserRepository userRepository;
    private CourseRepository courseRepository;
    private CourseAdapter courseAdapter;
    private List<Course> userCoursesList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepository = new AuthRepository();
        userRepository = new UserRepository();
        courseRepository = new CourseRepository();
        userCoursesList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
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

        // Set up sign out button
        binding.signOutButton.setOnClickListener(v -> {
            authRepository.signOut();
            Navigation.findNavController(requireView()).navigate(R.id.loginFragment);
        });

        // Setup RecyclerView
        setupRecyclerView();

        // Load user profile
        loadUserProfile(currentUser.getUid());

        // Load user's uploaded courses
        loadUserCourses(currentUser.getUid());
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter(requireContext(), userCoursesList, this);
        binding.userCoursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.userCoursesRecyclerView.setAdapter(courseAdapter);
    }

    private void loadUserProfile(String userId) {
        binding.profileProgressBar.setVisibility(View.VISIBLE);

        userRepository.getUserById(userId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                binding.profileProgressBar.setVisibility(View.GONE);

                // Display user info
                binding.usernameTv.setText(user.getUsername());
                binding.emailTv.setText(user.getEmail());
            }

            @Override
            public void onError(String errorMessage) {
                binding.profileProgressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserCourses(String userId) {
        binding.coursesProgressBar.setVisibility(View.VISIBLE);

        courseRepository.getAllCourses(new CourseRepository.CoursesCallback() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                binding.coursesProgressBar.setVisibility(View.GONE);

                // Filter courses uploaded by this user
                List<Course> filteredCourses = new ArrayList<>();
                for (Course course : courses) {
                    if (course.getUploaderUid().equals(userId)) {
                        filteredCourses.add(course);
                    }
                }

                if (filteredCourses.isEmpty()) {
                    binding.noUserCoursesTv.setVisibility(View.VISIBLE);
                } else {
                    binding.noUserCoursesTv.setVisibility(View.GONE);
                    userCoursesList.clear();
                    userCoursesList.addAll(filteredCourses);
                    courseAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.coursesProgressBar.setVisibility(View.GONE);
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