package com.example.coursesharingapp.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
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

public class ProfileFragment extends Fragment implements
        CourseAdapter.OnCourseClickListener,
        CourseAdapter.OnCourseDeleteListener,
        CourseAdapter.OnCourseEditListener {

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
            navigateToLogin();
            return;
        }

        // Set up sign out button
        binding.signOutButton.setOnClickListener(v -> {
            authRepository.signOut();
            navigateToLogin();
        });

        // Setup RecyclerView
        setupRecyclerView();

        // Load user profile
        loadUserProfile(currentUser.getUid());

        // Load user's uploaded courses
        loadUserCourses(currentUser.getUid());
    }

    private void navigateToLogin() {
        NavController navController = Navigation.findNavController(requireView());
        // Create NavOptions to clear the back stack
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.homeFragment, true)  // Pop up to home (exclusive)
                .build();
        navController.navigate(R.id.loginFragment, null, navOptions);
    }

    private void setupRecyclerView() {
        // Use constructor with showAccessCode=true
        courseAdapter = new CourseAdapter(requireContext(), userCoursesList,
                this, this, this, true, true, true);
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

        courseRepository.getCoursesByUploader(userId, new CourseRepository.CoursesCallback() {
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
                    binding.userCoursesRecyclerView.setVisibility(View.GONE);
                } else {
                    binding.noUserCoursesTv.setVisibility(View.GONE);
                    binding.userCoursesRecyclerView.setVisibility(View.VISIBLE);
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
    public void onCourseDelete(Course course, int position) {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete this course?")
                .setPositiveButton("Delete", (dialog, which) -> deleteCourse(course, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onCourseEdit(Course course, int position) {
        // Navigate to edit course fragment
        Bundle args = new Bundle();
        args.putString("courseId", course.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_to_editCourse, args);
    }

    private void deleteCourse(Course course, int position) {
        binding.coursesProgressBar.setVisibility(View.VISIBLE);

        // Delete course using CourseRepository
        courseRepository.deleteCourse(course.getId(), new CourseRepository.CourseCallback() {
            @Override
            public void onSuccess() {
                binding.coursesProgressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Course deleted successfully", Toast.LENGTH_SHORT).show();

                // Remove the course from the list
                userCoursesList.remove(position);
                courseAdapter.notifyItemRemoved(position);

                // Check if list is now empty
                if (userCoursesList.isEmpty()) {
                    binding.noUserCoursesTv.setVisibility(View.VISIBLE);
                    binding.userCoursesRecyclerView.setVisibility(View.GONE);
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}