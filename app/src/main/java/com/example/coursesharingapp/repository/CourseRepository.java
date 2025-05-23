package com.example.coursesharingapp.repository;

import android.net.Uri;
import android.util.Log;

import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.model.SavedCourse;
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

    public interface IsCourseBookmarkedCallback {
        void onResult(boolean isBookmarked);
        void onError(String errorMessage);
    }

    public interface AccessCodeValidationCallback {
        void onValidationResult(boolean isValid, Course course);
        void onError(String errorMessage);
    }

    // New interfaces for progress tracking
    public interface UploadProgressCallback {
        void onThumbnailProgress(int progress);
        void onVideoProgress(int progress);
        void onThumbnailComplete();
        void onVideoComplete();
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface EditProgressCallback {
        void onThumbnailProgress(int progress);
        void onVideoProgress(int progress);
        void onThumbnailComplete();
        void onVideoComplete();
        void onSuccess();
        void onError(String errorMessage);
    }

    // Get all PUBLIC courses only (no filter)
    public void getAllCourses(CoursesCallback callback) {
        firestore.collection("courses")
                .whereEqualTo("private", false) // Only get public courses
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

    // Get PUBLIC courses by category
    public void getCoursesByCategory(String category, CoursesCallback callback) {
        firestore.collection("courses")
                .whereEqualTo("category", category)
                .whereEqualTo("private", false) // Only get public courses
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

    // Get courses by uploader (my courses) - includes both public and private
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

    // Search PUBLIC courses only by title, description, or uploader username
    public void searchCourses(String query, CoursesCallback callback) {
        // Get all public courses and filter client-side since Firestore doesn't support 'contains' queries
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

    // Validate access code for private course
    public void validateCourseAccessCode(String accessCode, AccessCodeValidationCallback callback) {
        firestore.collection("courses")
                .whereEqualTo("accessCode", accessCode)
                .whereEqualTo("isPrivate", true)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            Course course = document.toObject(Course.class);
                            course.setId(document.getId());
                            callback.onValidationResult(true, course);
                        } else {
                            callback.onValidationResult(false, null);
                        }
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

    // Check if a course is bookmarked by a user
    public void isCourseSaved(String userId, String courseId, IsCourseBookmarkedCallback callback) {
        firestore.collection("savedCourses")
                .whereEqualTo("userId", userId)
                .whereEqualTo("courseId", courseId)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isBookmarked = !task.getResult().isEmpty();
                        callback.onResult(isBookmarked);
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    // Save a course for a user
    public void saveCourse(String userId, String courseId, CourseCallback callback) {
        // First check if it's already saved
        isCourseSaved(userId, courseId, new IsCourseBookmarkedCallback() {
            @Override
            public void onResult(boolean isBookmarked) {
                if (isBookmarked) {
                    // Already saved, just return success
                    callback.onSuccess();
                    return;
                }

                // Create a new SavedCourse object
                SavedCourse savedCourse = new SavedCourse(userId, courseId);

                // Add to Firestore
                firestore.collection("savedCourses")
                        .add(savedCourse)
                        .addOnSuccessListener(documentReference -> {
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            callback.onError(e.getMessage());
                        });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    // Unsave a course for a user
    public void unsaveCourse(String userId, String courseId, CourseCallback callback) {
        firestore.collection("savedCourses")
                .whereEqualTo("userId", userId)
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            // Not saved, just return success
                            callback.onSuccess();
                            return;
                        }

                        // Get the SavedCourse document ID
                        String savedCourseId = task.getResult().getDocuments().get(0).getId();

                        // Delete from Firestore
                        firestore.collection("savedCourses")
                                .document(savedCourseId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    callback.onError(e.getMessage());
                                });
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    // Get saved courses for a user (includes both public and private courses they've saved)
    public void getSavedCourses(String userId, CoursesCallback callback) {
        // First get all saved course IDs for the user
        firestore.collection("savedCourses")
                .whereEqualTo("userId", userId)
                .orderBy("savedAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            // No saved courses
                            callback.onCoursesLoaded(new ArrayList<>());
                            return;
                        }

                        List<String> courseIds = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            SavedCourse savedCourse = document.toObject(SavedCourse.class);
                            if (savedCourse != null) {
                                courseIds.add(savedCourse.getCourseId());
                            }
                        }

                        // Now fetch all these courses
                        List<Course> savedCourses = new ArrayList<>();
                        final int[] pendingRequests = {courseIds.size()};

                        for (String courseId : courseIds) {
                            getCourseById(courseId, new SingleCourseCallback() {
                                @Override
                                public void onCourseLoaded(Course course) {
                                    savedCourses.add(course);
                                    pendingRequests[0]--;
                                    checkComplete();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    // Just log error and continue
                                    Log.e(TAG, "Error loading course: " + errorMessage);
                                    pendingRequests[0]--;
                                    checkComplete();
                                }

                                private void checkComplete() {
                                    if (pendingRequests[0] == 0) {
                                        callback.onCoursesLoaded(savedCourses);
                                    }
                                }
                            });
                        }
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    // Original createCourse method - kept for backward compatibility
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

    // New method for course creation with progress tracking
    public void createCourseWithProgress(Course course, Uri thumbnailUri, Uri videoUri, UploadProgressCallback callback) {
        // First, add the course to Firestore
        DocumentReference courseRef = firestore.collection("courses").document();
        course.setId(courseRef.getId());

        // Upload thumbnail and video with progress tracking
        uploadThumbnailWithProgress(course.getId(), thumbnailUri, new UploadCallback() {
            @Override
            public void onProgress(int progress) {
                callback.onThumbnailProgress(progress);
            }

            @Override
            public void onSuccess(String thumbnailUrl) {
                callback.onThumbnailComplete();
                course.setThumbnailUrl(thumbnailUrl);

                uploadVideoWithProgress(course.getId(), videoUri, new UploadCallback() {
                    @Override
                    public void onProgress(int progress) {
                        callback.onVideoProgress(progress);
                    }

                    @Override
                    public void onSuccess(String videoUrl) {
                        callback.onVideoComplete();
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

    // Original editCourse method - kept for backward compatibility
    public void editCourse(Course course, Uri thumbnailUri, Uri videoUri, CourseCallback callback) {
        if (course.getId() == null || course.getId().isEmpty()) {
            callback.onError("Course ID is required for editing");
            return;
        }

        // Check if there are new files to upload
        if (thumbnailUri != null && videoUri != null) {
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

    // New method for course editing with progress tracking
    public void editCourseWithProgress(Course course, Uri thumbnailUri, Uri videoUri, EditProgressCallback callback) {
        if (course.getId() == null || course.getId().isEmpty()) {
            callback.onError("Course ID is required for editing");
            return;
        }

        // Check if there are new files to upload
        if (thumbnailUri != null && videoUri != null) {
            uploadThumbnailWithProgress(course.getId(), thumbnailUri, new UploadCallback() {
                @Override
                public void onProgress(int progress) {
                    callback.onThumbnailProgress(progress);
                }

                @Override
                public void onSuccess(String thumbnailUrl) {
                    callback.onThumbnailComplete();
                    course.setThumbnailUrl(thumbnailUrl);

                    uploadVideoWithProgress(course.getId(), videoUri, new UploadCallback() {
                        @Override
                        public void onProgress(int progress) {
                            callback.onVideoProgress(progress);
                        }

                        @Override
                        public void onSuccess(String videoUrl) {
                            callback.onVideoComplete();
                            course.setVideoUrl(videoUrl);
                            updateCourseInFirestore(course, new CourseCallback() {
                                @Override
                                public void onSuccess() {
                                    callback.onSuccess();
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

                @Override
                public void onError(String errorMessage) {
                    callback.onError(errorMessage);
                }
            });
        } else if (thumbnailUri != null) {
            // Upload only new thumbnail
            uploadThumbnailWithProgress(course.getId(), thumbnailUri, new UploadCallback() {
                @Override
                public void onProgress(int progress) {
                    callback.onThumbnailProgress(progress);
                }

                @Override
                public void onSuccess(String thumbnailUrl) {
                    callback.onThumbnailComplete();
                    course.setThumbnailUrl(thumbnailUrl);
                    updateCourseInFirestore(course, new CourseCallback() {
                        @Override
                        public void onSuccess() {
                            callback.onSuccess();
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
        } else if (videoUri != null) {
            // Upload only new video
            uploadVideoWithProgress(course.getId(), videoUri, new UploadCallback() {
                @Override
                public void onProgress(int progress) {
                    callback.onVideoProgress(progress);
                }

                @Override
                public void onSuccess(String videoUrl) {
                    callback.onVideoComplete();
                    course.setVideoUrl(videoUrl);
                    updateCourseInFirestore(course, new CourseCallback() {
                        @Override
                        public void onSuccess() {
                            callback.onSuccess();
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
        } else {
            // No new files, just update the course data
            updateCourseInFirestore(course, new CourseCallback() {
                @Override
                public void onSuccess() {
                    callback.onSuccess();
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onError(errorMessage);
                }
            });
        }
    }

    private void updateCourseInFirestore(Course course, CourseCallback callback) {
        firestore.collection("courses")
                .document(course.getId())
                .set(course)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Original upload methods without progress tracking
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

    // New upload methods with progress tracking
    private void uploadThumbnailWithProgress(String courseId, Uri thumbnailUri, UploadCallback callback) {
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

    private void uploadVideoWithProgress(String courseId, Uri videoUri, UploadCallback callback) {
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

    // Delete course method
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
                                        .addOnSuccessListener(aVoid -> {
                                            // Delete all saved references to this course
                                            deleteSavedCourseReferences(courseId, callback);
                                        })
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e(TAG, "Failed to delete video file: " + errorMessage);
                                // Continue with deleting the document even if file deletion fails
                                firestore.collection("courses")
                                        .document(courseId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            // Delete all saved references to this course
                                            deleteSavedCourseReferences(courseId, callback);
                                        })
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
                                        .addOnSuccessListener(aVoid -> {
                                            // Delete all saved references to this course
                                            deleteSavedCourseReferences(courseId, callback);
                                        })
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            }

                            @Override
                            public void onError(String videoError) {
                                Log.e(TAG, "Failed to delete video file: " + videoError);
                                // Continue with deleting the document even if both file deletions fail
                                firestore.collection("courses")
                                        .document(courseId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            // Delete all saved references to this course
                                            deleteSavedCourseReferences(courseId, callback);
                                        })
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
    // Delete all saved references to a course when the course is deleted
    private void deleteSavedCourseReferences(String courseId, CourseCallback callback) {
        firestore.collection("savedCourses")
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            callback.onSuccess();
                            return;
                        }

                        // Create a batch operation to delete all saved references
                        com.google.firebase.firestore.WriteBatch batch = firestore.batch();

                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            batch.delete(document.getReference());
                        }

                        batch.commit()
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    } else {
                        callback.onError(task.getException().getMessage());
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
}