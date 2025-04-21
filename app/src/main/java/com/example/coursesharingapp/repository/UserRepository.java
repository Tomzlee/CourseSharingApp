package com.example.coursesharingapp.repository;

import com.example.coursesharingapp.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final FirebaseFirestore firestore;

    public UserRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public interface UserCallback {
        void onUserLoaded(User user);
        void onError(String errorMessage);
    }

    public void createUser(User user, AuthRepository.AuthCallback callback) {
        firestore.collection("users")
                .document(user.getId())
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getUserById(String userId, UserCallback callback) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            User user = document.toObject(User.class);
                            callback.onUserLoaded(user);
                        } else {
                            callback.onError("User not found");
                        }
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    public void getUserByUsername(String username, UserCallback callback) {
        firestore.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        User user = task.getResult().getDocuments().get(0).toObject(User.class);
                        callback.onUserLoaded(user);
                    } else {
                        callback.onError("User not found");
                    }
                });
    }
}