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
import com.example.coursesharingapp.databinding.FragmentLoginBinding;
import com.example.coursesharingapp.repository.AuthRepository;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthRepository authRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepository = new AuthRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Navigate to Register
        binding.registerTextView.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.action_to_register));

        // Login Button Click
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();

            if (validateInput(email, password)) {
                loginUser(email, password);
            }
        });
    }

    private boolean validateInput(String email, String password) {
        boolean isValid = true;

        if (email.isEmpty()) {
            binding.emailEditText.setError("Email cannot be empty");
            isValid = false;
        }

        if (password.isEmpty()) {
            binding.passwordEditText.setError("Password cannot be empty");
            isValid = false;
        }

        return isValid;
    }

    private void loginUser(String email, String password) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.loginButton.setEnabled(false);

        authRepository.loginUser(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                binding.progressBar.setVisibility(View.GONE);
                Navigation.findNavController(requireView()).navigate(R.id.action_to_home);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                binding.loginButton.setEnabled(true);
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