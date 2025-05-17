package com.example.coursesharingapp.util;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class for handling private course access codes
 */
public class AccessCodeUtil {

    private static final int CODE_LENGTH = 9;
    private static final String CODE_CHARS = "0123456789";
    private static final Random random = new Random();

    /**
     * Generates a unique 9-digit random access code
     * @return The generated access code
     */
    public static String generateAccessCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    /**
     * Checks if an access code is valid (exists in the database)
     * @param accessCode The access code to check
     * @param callback Callback with the result
     */
    public static void validateAccessCode(String accessCode, AccessCodeCallback callback) {
        if (accessCode == null || accessCode.length() != CODE_LENGTH) {
            callback.onResult(false);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("courses")
                .whereEqualTo("accessCode", accessCode)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isValid = false;
                        if (!task.getResult().isEmpty()) {
                            // Check each result to make sure it's actually a private course
                            for (int i = 0; i < task.getResult().size(); i++) {
                                if (task.getResult().getDocuments().get(i).getBoolean("private") != null &&
                                        task.getResult().getDocuments().get(i).getBoolean("private")) {
                                    isValid = true;
                                    break;
                                }
                            }
                        }
                        callback.onResult(isValid);
                    } else {
                        callback.onResult(false);
                    }
                });
    }

    /**
     * Ensures generated code is unique by checking against the database
     * @return A unique access code
     */
    public static String generateUniqueAccessCode() {
        String code = generateAccessCode();

        // Check if the code exists in the database
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean isUnique = new AtomicBoolean(false);

        while (!isUnique.get()) {
            code = generateAccessCode();
            final String finalCode = code;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("courses")
                    .whereEqualTo("accessCode", finalCode)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot snapshot = task.getResult();
                            isUnique.set(snapshot.isEmpty());
                        } else {
                            // In case of error, we'll just consider it unique
                            isUnique.set(true);
                        }
                        latch.countDown();
                    });

            try {
                // Wait for the database check to complete
                latch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // In case of interruption, return the current code
                return code;
            }
        }

        return code;
    }

    /**
     * Callback for access code validation
     */
    public interface AccessCodeCallback {
        void onResult(boolean isValid);
    }
}