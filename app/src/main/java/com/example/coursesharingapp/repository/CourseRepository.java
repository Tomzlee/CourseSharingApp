package com.example.coursesharingapp.repository;

import android.net.Uri;
import android.util.Log;

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

    // Define the maximum file size: 5GB in bytes
    // Define the maximum file size: 5GB in bytes
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024; // 5GB

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

    // Get all courses (no filter)
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

    // Get courses by category
    public void getCoursesByCategory(String category, CoursesCallback callback) {
        firestore.collection("courses")
                .whereEqualTo("category", category)
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

    // Get courses by uploader (my courses)
    public void getCoursesByUploader(String uploaderUid, CoursesCallback callback) {
        firestore.collection("courses")
                .whereEqualTo("uploaderUid", uploaderUid)
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

    // Search courses by name (title)
    // Search courses by title, description, or uploader username
    public void searchCourses(String query, CoursesCallback callback) {
        // Get all courses and filter client-side since Firestore doesn't support 'contains' queries
        getAllCourses(new CoursesCallback() {
            @Override
            public void onCoursesLoaded(List<Course> allCourses) {
                List<Course> filteredCourses = new ArrayList<>();
                String lowercaseQuery = query.toLowerCase();

                for (Course course : allCourses) {
                    // Check if the query matches title, description, or uploader username
                    if (course.getTitle().toLowerCase().contains(lowercaseQuery) ||
                            (course.getShortDescription() != null &&
                                    course.getShortDescription().toLowerCase().contains(lowercaseQuery)) ||
                            (course.getUploaderUsername() != null &&
                                    course.getUploaderUsername().toLowerCase().contains(lowercaseQuery))) {
                        filteredCourses.add(course);
                    }
                }

                callback.onCoursesLoaded(filteredCourses);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
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

    public void editCourse(Course course, Uri thumbnailUri, Uri videoUri, CourseCallback callback) {
        // Check if course ID exists
        if (course.getId() == null || course.getId().isEmpty()) {
            callback.onError("Course ID is required for editing");
            return;
        }

        // Check if there are new files to upload
        if (thumbnailUri != null && videoUri != null) {
            // Upload both new thumbnail and video
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
                            updateCourseInFirestore(course, callback);
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
        } else if (thumbnailUri != null) {
            // Upload only new thumbnail
            uploadThumbnail(course.getId(), thumbnailUri, new UploadCallback() {
                @Override
                public void onProgress(int progress) {
                    // Handle thumbnail upload progress
                }

                @Override
                public void onSuccess(String thumbnailUrl) {
                    course.setThumbnailUrl(thumbnailUrl);
                    updateCourseInFirestore(course, callback);
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onError(errorMessage);
                }
            });
        } else if (videoUri != null) {
            // Upload only new video
            uploadVideo(course.getId(), videoUri, new UploadCallback() {
                @Override
                public void onProgress(int progress) {
                    // Handle video upload progress
                }

                @Override
                public void onSuccess(String videoUrl) {
                    course.setVideoUrl(videoUrl);
                    updateCourseInFirestore(course, callback);
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onError(errorMessage);
                }
            });
        } else {
            // No new files, just update the course data
            updateCourseInFirestore(course, callback);
        }
    }

    private void updateCourseInFirestore(Course course, CourseCallback callback) {
        firestore.collection("courses")
                .document(course.getId())
                .set(course)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void uploadThumbnail(String courseId, Uri thumbnailUri, UploadCallback callback) {
        String fileName = "thumbnails/" + courseId + "_" + UUID.randomUUID().toString();
        StorageReference storageRef = storage.getReference().child(fileName);

        // Add metadata to restrict file size
        com.google.firebase.storage.StorageMetadata metadata = new com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/*")
                .build();


        UploadTask uploadTask = storageRef.putFile(thumbnailUri, metadata);

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

    public void deleteCourse(String courseId, CourseCallback callback) {
        // First, get the course to find associated files
        getCourseById(courseId, new SingleCourseCallback() {
            @Override
            public void onCourseLoaded(Course course) {
                // Delete files from storage
                deleteFileFromUrl(course.getThumbnailUrl(), new CourseCallback() {
                    @Override
                    public void onSuccess() {
                        deleteFileFromUrl(course.getVideoUrl(), new CourseCallback() {
                            @Override
                            public void onSuccess() {
                                // Delete the course document from Firestore
                                firestore.collection("courses")
                                        .document(courseId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e(TAG, "Failed to delete video file: " + errorMessage);
                                // Continue with deleting the document even if file deletion fails
                                firestore.collection("courses")
                                        .document(courseId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Failed to delete thumbnail file: " + errorMessage);
                        // Continue with deleting the video file and document even if thumbnail deletion fails
                        deleteFileFromUrl(course.getVideoUrl(), new CourseCallback() {
                            @Override
                            public void onSuccess() {
                                firestore.collection("courses")
                                        .document(courseId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            }

                            @Override
                            public void onError(String videoError) {
                                Log.e(TAG, "Failed to delete video file: " + videoError);
                                // Continue with deleting the document even if both file deletions fail
                                firestore.collection("courses")
                                        .document(courseId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    private void deleteFileFromUrl(String fileUrl, CourseCallback callback) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            callback.onSuccess();
            return;
        }

        try {
            StorageReference fileRef = storage.getReferenceFromUrl(fileUrl);
            fileRef.delete()
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid file URL: " + fileUrl);
            callback.onError("Invalid file URL");
        }
    }

    private void uploadVideo(String courseId, Uri videoUri, UploadCallback callback) {
        String fileName = "videos/" + courseId + "_" + UUID.randomUUID().toString();
        StorageReference storageRef = storage.getReference().child(fileName);

        // Add metadata to restrict file size
        com.google.firebase.storage.StorageMetadata metadata = new com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("video/*")
                .build();

        UploadTask uploadTask = storageRef.putFile(videoUri, metadata);

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

    // Note: We removed the getFileSize method since we're now handling file size limits
    // through Firebase Storage rules and error handling in the upload task
}