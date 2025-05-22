package com.example.coursesharingapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.FragmentHomeBinding;
import com.example.coursesharingapp.repository.AuthRepository;
import com.example.coursesharingapp.repository.CourseRepository;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseUser;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private AuthRepository authRepository;
    private CourseRepository courseRepository;
    private HomeViewPagerAdapter viewPagerAdapter;
    private FirebaseUser currentUser;

    // Tab positions
    private static final int TAB_ALL_COURSES = 0;
    private static final int TAB_MY_COURSES = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepository = new AuthRepository();
        courseRepository = new CourseRepository();

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

        // Setup ViewPager and TabLayout
        setupViewPager();

        // Set up floating action button for adding new courses
        binding.addCourseFab.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.action_to_uploadCourse));

        binding.privateCourseAccessButton.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.action_to_privateCourseAccess));
    }

    private void setupViewPager() {
        viewPagerAdapter = new HomeViewPagerAdapter(this, currentUser);
        binding.viewPager.setAdapter(viewPagerAdapter);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    if (position == TAB_ALL_COURSES) {
                        tab.setText(R.string.all_courses);
                    } else if (position == TAB_MY_COURSES) {
                        tab.setText(R.string.my_courses);
                    }
                }).attach();

        // Handle tab changes
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });

        // Handle ViewPager page changes
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}