package com.example.coursesharingapp.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.coursesharingapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private EditText userNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText passwordVerifyEditText;
    private TextView registrationTextView;
    private Button registerButton;
    private Button cancelButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private NavController navController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        // Initialize UI elements
        userNameEditText = view.findViewById(R.id.userNameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        passwordVerifyEditText = view.findViewById(R.id.passwordVerifyEditText);
        registrationTextView = view.findViewById(R.id.registrationTextView);
        registerButton = view.findViewById(R.id.registerButton);
        cancelButton = view.findViewById(R.id.cancelButton);

        // Setup click listeners
        registerButton.setOnClickListener(v -> registerUser());
        cancelButton.setOnClickListener(v -> navigateToLogin());
    }

    private void registerUser() {
        String username = userNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String passwordVerify = passwordVerifyEditText.getText().toString().trim();

        // Validate inputs
        if (username.isEmpty()) {
            userNameEditText.setError("Username is required");
            userNameEditText.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password should be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        if (!password.equals(passwordVerify)) {
            registrationTextView.setText("Passwords don't match");
            passwordVerifyEditText.setError("Passwords don't match");
            passwordVerifyEditText.requestFocus();
            return;
        }

        // Show a toast to indicate registration is in progress
        Toast.makeText(requireContext(), "Registering user...", Toast.LENGTH_SHORT).show();

        // Create user with Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Registration successful
                        Toast.makeText(requireContext(), "Authentication successful!", Toast.LENGTH_SHORT).show();

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save additional user data to Firestore
                            saveUserData(user.getUid(), username, email);
                            //navigateToHome();
                        } else {
                            // This shouldn't happen, but just in case
                            Toast.makeText(requireContext(), "Registration successful but user is null", Toast.LENGTH_SHORT).show();
                            // Force navigation to login instead
                            navigateToLogin();
                        }
                    } else {
                        // Registration failed - show the specific error
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(requireContext(),
                                "Registration failed: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserData(String userId, String username, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        userData.put("createdAt", System.currentTimeMillis());

        // Show toast to indicate we're saving user data
        Toast.makeText(requireContext(), "Saving user data...", Toast.LENGTH_SHORT).show();

        mFirestore.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    // User data saved successfully
                    Context context = getContext();
                    Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show();
                    // Make sure we navigate to home screen
                    navigateToHome();
                })
                .addOnFailureListener(e -> {
                    // Failed to save user data
                    Toast.makeText(requireContext(),
                            "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    // Still navigate to home screen since authentication was successful
                    navigateToHome();
                });
    }

    private void navigateToLogin() {
        navController.navigateUp();
    }

    private void navigateToHome() {
        Toast.makeText(requireContext(), "Navigating to home screen...", Toast.LENGTH_SHORT).show();

        try {
            // Using the action for navigation
            navController.navigate(R.id.action_registerFragment_to_homeFragment);
            Toast.makeText(requireContext(), "Navigation successful", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Log the error and try an alternative navigation approach
            e.printStackTrace();
            Toast.makeText(requireContext(), "Navigation error: " + e.getMessage(), Toast.LENGTH_LONG).show();

            try {
                // Try direct navigation to destination instead of using action
                navController.navigate(R.id.homeFragment);
                Toast.makeText(requireContext(), "Direct navigation successful", Toast.LENGTH_SHORT).show();
            } catch (Exception e2) {
                e2.printStackTrace();
                Toast.makeText(requireContext(), "All navigation failed: " + e2.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}