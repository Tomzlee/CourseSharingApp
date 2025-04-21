package com.example.coursesharingapp.ui.auth;

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
import com.example.coursesharingapp.databinding.FragmentRegisterBinding;
import com.example.coursesharingapp.repository.AuthRepository;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthRepository authRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepository = new AuthRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Navigate back to Login
        binding.loginTextView.setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        // Register Button Click
        binding.registerButton.setOnClickListener(v -> {
            String username = binding.usernameEditText.getText().toString().trim();
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();

            if (validateInput(username, email, password, confirmPassword)) {
                registerUser(username, email, password);
            }
        });
    }

    private boolean validateInput(String username, String email, String password, String confirmPassword) {
        boolean isValid = true;

        if (username.isEmpty()) {
            binding.usernameEditText.setError("Username cannot be empty");
            isValid = false;
        }

        if (email.isEmpty()) {
            binding.emailEditText.setError("Email cannot be empty");
            isValid = false;
        }

        if (password.isEmpty()) {
            binding.passwordEditText.setError("Password cannot be empty");
            isValid = false;
        } else if (password.length() < 6) {
            binding.passwordEditText.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordEditText.setError("Passwords do not match");
            isValid = false;
        }

        return isValid;
    }

    private void registerUser(String username, String email, String password) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.registerButton.setEnabled(false);

        // First check if username is available
        authRepository.checkUsernameAvailability(username, new AuthRepository.UsernameCheckCallback() {
            @Override
            public void onUsernameAvailable(boolean isAvailable) {
                if (isAvailable) {
                    // Username is available, proceed with registration
                    authRepository.registerUser(email, password, username, new AuthRepository.AuthCallback() {
                        @Override
                        public void onSuccess() {
                            binding.progressBar.setVisibility(View.GONE);
                            Navigation.findNavController(requireView()).navigate(R.id.action_to_home);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.registerButton.setEnabled(true);
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.registerButton.setEnabled(true);
                    binding.usernameEditText.setError("Username already exists");
                    Toast.makeText(requireContext(), "Username already exists. Please choose another username.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                binding.registerButton.setEnabled(true);
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