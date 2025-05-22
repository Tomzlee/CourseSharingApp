package com.example.coursesharingapp.ui.course;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.coursesharingapp.R;
import com.example.coursesharingapp.databinding.FragmentPrivateCourseAccessBinding;
import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.repository.CourseRepository;

public class PrivateCourseAccessFragment extends Fragment {

    private FragmentPrivateCourseAccessBinding binding;
    private CourseRepository courseRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        courseRepository = new CourseRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPrivateCourseAccessBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up access button click listener
        binding.accessCourseButton.setOnClickListener(v -> {
            String accessCode = binding.accessCodeEditText.getText().toString().trim();
            if (validateInput(accessCode)) {
                validateAccessCode(accessCode);
            }
        });
    }

    private boolean validateInput(String accessCode) {
        if (accessCode.isEmpty()) {
            binding.accessCodeEditText.setError("Access code cannot be empty");
            return false;
        }

        if (accessCode.length() != 6) {
            binding.accessCodeEditText.setError("Access code must be 6 digits");
            return false;
        }

        if (!accessCode.matches("\\d{6}")) {
            binding.accessCodeEditText.setError("Access code must contain only numbers");
            return false;
        }

        return true;
    }

    private void validateAccessCode(String accessCode) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.accessCourseButton.setEnabled(false);

        courseRepository.validateCourseAccessCode(accessCode, new CourseRepository.AccessCodeValidationCallback() {
            @Override
            public void onValidationResult(boolean isValid, Course course) {
                binding.progressBar.setVisibility(View.GONE);
                binding.accessCourseButton.setEnabled(true);

                if (isValid && course != null) {
                    // Access code is valid, navigate to course detail
                    Bundle args = new Bundle();
                    args.putString("courseId", course.getId());
                    Navigation.findNavController(requireView()).navigate(R.id.action_to_courseDetail, args);
                } else {
                    // Invalid access code
                    binding.accessCodeEditText.setError("Invalid access code");
                    Toast.makeText(requireContext(), "Invalid access code. Please check and try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                binding.accessCourseButton.setEnabled(true);
                Toast.makeText(requireContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}