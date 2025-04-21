package com.example.coursesharingapp.repository;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;

import com.example.coursesharingapp.model.Course;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CourseRepository {
    private static final String TAG = "CourseRepository";
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    public CourseRepository() {
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public interface CourseCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface CoursesCallback {
        void onCoursesLoaded(List<Course> courses);
        void onError(String errorMessage);
    }

    public interface SingleCourseCallback {
        void onCourseLoaded(Course course);
        void onError(String errorMessage);
    }

    public interface UploadCallback {
        void onProgress(int progress);
        void onSuccess(String downloadUrl);
        void onError(String errorMessage);
    }

    public void getAllCourses(CoursesCallback callback) {
        firestore.collection("courses")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Course> courses = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Course course = document.toObject(Course.class);
                            course.setId(document.getId());
                            courses.add(course);
                        }
                        callback.onCoursesLoaded(courses);
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }


    public void getCourseById(String courseId, SingleCourseCallback callback) {
        firestore.collection("courses")
                .document(courseId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Course course = document.toObject(Course.class);
                            course.setId(document.getId());
                            callback.onCourseLoaded(course);
                        } else {
                            callback.onError("Course not found");
                        }
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    public void createCourse(Course course, Uri thumbnailUri, Uri videoUri, CourseCallback callback) {
        // First, add the course to Firestore
        DocumentReference courseRef = firestore.collection("courses").document();
        course.setId(courseRef.getId());

        // Upload thumbnail and video
        uploadThumbnail(course.getId(), thumbnailUri, new UploadCallback() {
            @Override
            public void onProgress(int progress) {
                // Handle thumbnail upload progress
            }

            @Override
            public void onSuccess(String thumbnailUrl) {
                course.setThumbnailUrl(thumbnailUrl);

                uploadVideo(course.getId(), videoUri, new UploadCallback() {
                    @Override
                    public void onProgress(int progress) {
                        // Handle video upload progress
                    }

                    @Override
                    public void onSuccess(String videoUrl) {
                        course.setVideoUrl(videoUrl);

                        // Save course with URLs to Firestore
                        courseRef.set(course)
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    }

                    @Override
                    public void onError(String errorMessage) {
                        callback.onError(errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    private void uploadThumbnail(String courseId, Uri thumbnailUri, UploadCallback callback) {
        String fileName = "thumbnails/" + courseId + "_" + UUID.randomUUID().toString();
        StorageReference storageRef = storage.getReference().child(fileName);

        UploadTask uploadTask = storageRef.putFile(thumbnailUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress((int) progress);
        }).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                callback.onSuccess(downloadUri.toString());
            } else {
                callback.onError(task.getException().getMessage());
            }
        });
    }

    private void uploadVideo(String courseId, Uri videoUri, UploadCallback callback) {
        String fileName = "videos/" + courseId + "_" + UUID.randomUUID().toString();
        StorageReference storageRef = storage.getReference().child(fileName);

        UploadTask uploadTask = storageRef.putFile(videoUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress((int) progress);
        }).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                callback.onSuccess(downloadUri.toString());
            } else {
                callback.onError(task.getException().getMessage());
            }
        });
    }
}