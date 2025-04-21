package com.example.coursesharingapp.repository;

import android.net.Uri;
import android.util.Log;

import com.example.coursesharingapp.model.Course;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CourseRepositoryHelper {
    private static final String TAG = "CourseRepositoryHelper";

    /**
     * When creating a new course, ensure we store the createdAt field as a Timestamp
     * This method demonstrates how to create a course with a Timestamp
     */
    public static void createCourseWithTimestamp(FirebaseFirestore firestore, Course course) {
        DocumentReference courseRef = firestore.collection("courses").document();
        course.setId(courseRef.getId());

        // Create a map with all the course data, explicitly setting createdAt as a Timestamp
        Map<String, Object> courseData = new HashMap<>();
        courseData.put("id", course.getId());
        courseData.put("title", course.getTitle());
        courseData.put("shortDescription", course.getShortDescription());
        courseData.put("longDescription", course.getLongDescription());
        courseData.put("uploaderUid", course.getUploaderUid());
        courseData.put("uploaderUsername", course.getUploaderUsername());
        courseData.put("thumbnailUrl", course.getThumbnailUrl());
        courseData.put("videoUrl", course.getVideoUrl());
        courseData.put("createdAt", Timestamp.now()); // Explicitly use Timestamp

        // Set the document with the prepared data
        courseRef.set(courseData);
    }

    /**
     * Helper method to upload a file to Firebase Storage
     */
    public static void uploadFile(FirebaseStorage storage, Uri fileUri, String folderPath,
                                  UploadCallback callback) {
        String fileName = folderPath + "/" + UUID.randomUUID().toString();
        StorageReference storageRef = storage.getReference().child(fileName);

        storageRef.putFile(fileUri)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) /
                            taskSnapshot.getTotalByteCount();
                    callback.onProgress((int) progress);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return storageRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    callback.onSuccess(downloadUri.toString());
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    public interface UploadCallback {
        void onProgress(int progress);
        void onSuccess(String downloadUrl);
        void onError(String errorMessage);
    }
}