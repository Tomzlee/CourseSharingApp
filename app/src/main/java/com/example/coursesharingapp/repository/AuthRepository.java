package com.example.coursesharingapp.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import com.example.coursesharingapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthRepository {
    private static final String TAG = "AuthRepository";
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final UserRepository userRepository;

    public AuthRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        userRepository = new UserRepository();
    }

    public interface AuthCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface UsernameCheckCallback {
        void onUsernameAvailable(boolean isAvailable);
        void onError(String errorMessage);
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void checkUsernameAvailability(String username, UsernameCheckCallback callback) {
        firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isAvailable = task.getResult().isEmpty();
                        callback.onUsernameAvailable(isAvailable);
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    public void registerUser(String email, String password, String username, AuthCallback callback) {
        // First check if username is available
        checkUsernameAvailability(username, new UsernameCheckCallback() {
            @Override
            public void onUsernameAvailable(boolean isAvailable) {
                if (isAvailable) {
                    // Username is available, proceed with registration
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser firebaseUser = task.getResult().getUser();
                                    if (firebaseUser != null) {
                                        // Create user in Firestore
                                        User user = new User(firebaseUser.getUid(), username, email);
                                        userRepository.createUser(user, new AuthCallback() {
                                            @Override
                                            public void onSuccess() {
                                                callback.onSuccess();
                                            }

                                            @Override
                                            public void onError(String errorMessage) {
                                                // If Firestore save fails, delete the Auth user
                                                firebaseUser.delete();
                                                callback.onError(errorMessage);
                                            }
                                        });
                                    }
                                } else {
                                    callback.onError(task.getException().getMessage());
                                }
                            });
                } else {
                    callback.onError("Username already exists. Please choose another username.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    public void signOut() {
        firebaseAuth.signOut();
    }
}