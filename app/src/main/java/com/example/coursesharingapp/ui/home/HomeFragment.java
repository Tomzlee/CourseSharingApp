package com.example.coursesharingapp.ui.home;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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
    private FirebaseUser currentUser;

    // Filter states
    private static final int FILTER_ALL = 0;
    private static final int FILTER_BY_CATEGORY = 1;
    private static final int FILTER_BY_AUTHOR = 2;
    private static final int FILTER_BY_SEARCH = 3;

    private int currentFilter = FILTER_ALL;
    private String currentCategory = "";
    private String currentSearchQuery = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepository = new AuthRepository();
        courseRepository = new CourseRepository();
        courseList = new ArrayList<>();

        // Handle back button press
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // If user is on the home screen and presses back, exit the app instead of navigating back to login
                requireActivity().finish();
            }
        });
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
        currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            Navigation.findNavController(requireView()).navigate(R.id.loginFragment);
            return;
        }

        // Setup RecyclerView
        setupRecyclerView();

        // Setup filter toolbar
        setupFilterToolbar();

        // Setup category chip group
        setupCategoryChips();

        // Setup search
        setupSearchBar();

        // Set up floating action button for adding new courses
        binding.addCourseFab.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.action_to_uploadCourse));

        // Load all courses initially
        loadCourses();
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter(requireContext(), courseList, this);
        binding.coursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.coursesRecyclerView.setAdapter(courseAdapter);
    }

    private void setupFilterToolbar() {
        binding.filterToolbar.setTitle("All Courses");
    }

    private void setupCategoryChips() {
        // Set up chip click listeners
        binding.chipAll.setOnClickListener(v -> {
            currentFilter = FILTER_ALL;
            currentCategory = "";
            binding.filterToolbar.setTitle("All Courses");
            loadCourses();
        });

        binding.chipArt.setOnClickListener(v -> {
            filterByCategory(Course.CATEGORY_ART);
        });

        binding.chipTech.setOnClickListener(v -> {
            filterByCategory(Course.CATEGORY_TECH);
        });

        binding.chipBusiness.setOnClickListener(v -> {
            filterByCategory(Course.CATEGORY_BUSINESS);
        });

        binding.chipLife.setOnClickListener(v -> {
            filterByCategory(Course.CATEGORY_LIFE);
        });

        binding.chipOther.setOnClickListener(v -> {
            filterByCategory(Course.CATEGORY_OTHER);
        });

        binding.chipMyCourses.setOnClickListener(v -> {
            currentFilter = FILTER_BY_AUTHOR;
            binding.filterToolbar.setTitle("My Courses");
            loadMyCourses();
        });
    }

    private void setupSearchBar() {
        // Set up search functionality
        binding.searchLayout.setEndIconOnClickListener(v -> {
            String query = binding.searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            }
        });

        // Also allow search on keyboard action
        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = binding.searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                }
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        currentFilter = FILTER_BY_SEARCH;
        currentSearchQuery = query;
        binding.filterToolbar.setTitle("Searching: " + query);
        // Uncheck all category chips when searching
        binding.categoryChipGroup.clearCheck();
        searchCourses(query);
    }

    private void filterByCategory(String category) {
        currentFilter = FILTER_BY_CATEGORY;
        currentCategory = category;
        binding.filterToolbar.setTitle("Category: " + category);
        loadCoursesByCategory(category);
    }

    private void loadCourses() {
        binding.progressBar.setVisibility(View.VISIBLE);

        courseRepository.getAllCourses(new CourseRepository.CoursesCallback() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                binding.progressBar.setVisibility(View.GONE);
                updateCoursesList(courses);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCoursesByCategory(String category) {
        binding.progressBar.setVisibility(View.VISIBLE);

        courseRepository.getCoursesByCategory(category, new CourseRepository.CoursesCallback() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                binding.progressBar.setVisibility(View.GONE);
                updateCoursesList(courses);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMyCourses() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String uid = currentUser.getUid();

        courseRepository.getCoursesByUploader(uid, new CourseRepository.CoursesCallback() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                binding.progressBar.setVisibility(View.GONE);
                updateCoursesList(courses);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchCourses(String query) {
        binding.progressBar.setVisibility(View.VISIBLE);

        courseRepository.searchCourses(query, new CourseRepository.CoursesCallback() {
            @Override
            public void onCoursesLoaded(List<Course> courses) {
                binding.progressBar.setVisibility(View.GONE);
                updateCoursesList(courses);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCoursesList(List<Course> courses) {
        if (courses.isEmpty()) {
            binding.noCoursesTv.setVisibility(View.VISIBLE);

            // Set appropriate "no courses" message based on current filter
            if (currentFilter == FILTER_ALL) {
                binding.noCoursesTv.setText("No courses available");
            } else if (currentFilter == FILTER_BY_CATEGORY) {
                binding.noCoursesTv.setText("No courses in category: " + currentCategory);
            } else if (currentFilter == FILTER_BY_AUTHOR) {
                binding.noCoursesTv.setText("You haven't uploaded any courses yet");
            } else if (currentFilter == FILTER_BY_SEARCH) {
            binding.noCoursesTv.setText("No courses or instructors matching: " + currentSearchQuery);
        }
            binding.coursesRecyclerView.setVisibility(View.GONE);
        } else {
            binding.noCoursesTv.setVisibility(View.GONE);
            binding.coursesRecyclerView.setVisibility(View.VISIBLE);
        }

        courseList.clear();
        courseList.addAll(courses);
        courseAdapter.notifyDataSetChanged();
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