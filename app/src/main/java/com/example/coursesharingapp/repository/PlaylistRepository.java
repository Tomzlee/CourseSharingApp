package com.example.coursesharingapp.repository;

import androidx.annotation.NonNull;

import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.model.Playlist;
import com.example.coursesharingapp.model.SavedPlaylist;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PlaylistRepository {
    private static final String TAG = "PlaylistRepository";
    private final FirebaseFirestore firestore;
    private final CourseRepository courseRepository;

    public PlaylistRepository() {
        firestore = FirebaseFirestore.getInstance();
        courseRepository = new CourseRepository();
    }

    public interface PlaylistCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface PlaylistsCallback {
        void onPlaylistsLoaded(List<Playlist> playlists);
        void onError(String errorMessage);
    }

    public interface SinglePlaylistCallback {
        void onPlaylistLoaded(Playlist playlist);
        void onError(String errorMessage);
    }

    public interface PlaylistWithCoursesCallback {
        void onPlaylistWithCoursesLoaded(Playlist playlist, List<Course> courses);
        void onError(String errorMessage);
    }

    public interface IsPlaylistSavedCallback {
        void onResult(boolean isSaved);
        void onError(String errorMessage);
    }

    public interface AccessCodeValidationCallback {
        void onValidationResult(boolean isValid, Playlist playlist);
        void onError(String errorMessage);
    }

    // Get all PUBLIC playlists only
    public void getAllPlaylists(PlaylistsCallback callback) {
        firestore.collection("playlists")
                .whereEqualTo("private", false) // Only get public playlists
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Playlist> playlists = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Playlist playlist = document.toObject(Playlist.class);
                            playlist.setId(document.getId());
                            playlists.add(playlist);
                        }
                        callback.onPlaylistsLoaded(playlists);
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    // Get playlists by creator - includes both public and private
    public void getPlaylistsByCreator(String creatorUid, PlaylistsCallback callback) {
        firestore.collection("playlists")
                .whereEqualTo("creatorUid", creatorUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Playlist> playlists = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Playlist playlist = document.toObject(Playlist.class);
                            playlist.setId(document.getId());
                            playlists.add(playlist);
                        }
                        callback.onPlaylistsLoaded(playlists);
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    // Search all PUBLIC playlists by title, description, or uploader username
    public void searchAllPlaylists(String query, PlaylistsCallback callback) {
        // Get all public playlists and filter client-side since Firestore doesn't support 'contains' queries
        getAllPlaylists(new PlaylistsCallback() {
            @Override
            public void onPlaylistsLoaded(List<Playlist> allPlaylists) {
                List<Playlist> filteredPlaylists = new ArrayList<>();
                String lowercaseQuery = query.toLowerCase();

                for (Playlist playlist : allPlaylists) {
                    // Check title
                    boolean matchesTitle = playlist.getTitle() != null &&
                            playlist.getTitle().toLowerCase().contains(lowercaseQuery);

                    // Check description
                    boolean matchesDescription = playlist.getDescription() != null &&
                            playlist.getDescription().toLowerCase().contains(lowercaseQuery);

                    // Check uploader username
                    boolean matchesUsername = playlist.getCreatorUsername() != null &&
                            playlist.getCreatorUsername().toLowerCase().contains(lowercaseQuery);

                    if (matchesTitle || matchesDescription || matchesUsername) {
                        filteredPlaylists.add(playlist);
                    }
                }

                callback.onPlaylistsLoaded(filteredPlaylists);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    // Search user's playlists by title or description - includes both public and private
    public void searchUserPlaylists(String creatorUid, String query, PlaylistsCallback callback) {
        // Get user's playlists and filter client-side
        getPlaylistsByCreator(creatorUid, new PlaylistsCallback() {
            @Override
            public void onPlaylistsLoaded(List<Playlist> userPlaylists) {
                List<Playlist> filteredPlaylists = new ArrayList<>();
                String lowercaseQuery = query.toLowerCase();

                for (Playlist playlist : userPlaylists) {
                    // Check title
                    boolean matchesTitle = playlist.getTitle() != null &&
                            playlist.getTitle().toLowerCase().contains(lowercaseQuery);

                    // Check description
                    boolean matchesDescription = playlist.getDescription() != null &&
                            playlist.getDescription().toLowerCase().contains(lowercaseQuery);

                    if (matchesTitle || matchesDescription) {
                        filteredPlaylists.add(playlist);
                    }
                }

                callback.onPlaylistsLoaded(filteredPlaylists);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    // Validate access code for private playlist
    public void validatePlaylistAccessCode(String accessCode, AccessCodeValidationCallback callback) {
        firestore.collection("playlists")
                .whereEqualTo("accessCode", accessCode)
                .whereEqualTo("private", true)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            Playlist playlist = document.toObject(Playlist.class);
                            playlist.setId(document.getId());
                            callback.onValidationResult(true, playlist);
                        } else {
                            callback.onValidationResult(false, null);
                        }
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    // Get a single playlist by ID
    public void getPlaylistById(String playlistId, SinglePlaylistCallback callback) {
        firestore.collection("playlists")
                .document(playlistId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Playlist playlist = document.toObject(Playlist.class);
                            playlist.setId(document.getId());
                            callback.onPlaylistLoaded(playlist);
                        } else {
                            callback.onError("Playlist not found");
                        }
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    // Get a playlist and all its courses
    public void getPlaylistWithCourses(String playlistId, PlaylistWithCoursesCallback callback) {
        getPlaylistById(playlistId, new SinglePlaylistCallback() {
            @Override
            public void onPlaylistLoaded(Playlist playlist) {
                List<String> courseIds = playlist.getCourseIds();
                if (courseIds == null || courseIds.isEmpty()) {
                    // If no courses in the playlist, return an empty list
                    callback.onPlaylistWithCoursesLoaded(playlist, new ArrayList<>());
                    return;
                }

                final List<Course> courses = new ArrayList<>();
                final int[] remainingCourses = {courseIds.size()};

                for (String courseId : courseIds) {
                    courseRepository.getCourseById(courseId, new CourseRepository.SingleCourseCallback() {
                        @Override
                        public void onCourseLoaded(Course course) {
                            courses.add(course);
                            remainingCourses[0]--;
                            checkComplete();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            remainingCourses[0]--;
                            checkComplete();
                        }

                        private void checkComplete() {
                            if (remainingCourses[0] == 0) {
                                callback.onPlaylistWithCoursesLoaded(playlist, courses);
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    // Create a new playlist
    public void createPlaylist(Playlist playlist, PlaylistCallback callback) {
        DocumentReference playlistRef = firestore.collection("playlists").document();
        playlist.setId(playlistRef.getId());

        playlistRef.set(playlist)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Update an existing playlist
    public void updatePlaylist(Playlist playlist, PlaylistCallback callback) {
        firestore.collection("playlists")
                .document(playlist.getId())
                .set(playlist)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Add a course to a playlist
    public void addCourseToPlaylist(String playlistId, String courseId, PlaylistCallback callback) {
        getPlaylistById(playlistId, new SinglePlaylistCallback() {
            @Override
            public void onPlaylistLoaded(Playlist playlist) {
                playlist.addCourseId(courseId);
                updatePlaylist(playlist, callback);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    // Remove a course from a playlist
    public void removeCourseFromPlaylist(String playlistId, String courseId, PlaylistCallback callback) {
        getPlaylistById(playlistId, new SinglePlaylistCallback() {
            @Override
            public void onPlaylistLoaded(Playlist playlist) {
                playlist.removeCourseId(courseId);
                updatePlaylist(playlist, callback);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    // Delete a playlist
    public void deletePlaylist(String playlistId, PlaylistCallback callback) {
        firestore.collection("playlists")
                .document(playlistId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // After deleting the playlist, also delete all saved references
                    deleteSavedPlaylistReferences(playlistId, callback);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Delete all saved references to a playlist when the playlist is deleted
    private void deleteSavedPlaylistReferences(String playlistId, PlaylistCallback callback) {
        firestore.collection("savedPlaylists")
                .whereEqualTo("playlistId", playlistId)
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

    // Check if a playlist is saved by a user
    public void isPlaylistSaved(String userId, String playlistId, IsPlaylistSavedCallback callback) {
        firestore.collection("savedPlaylists")
                .whereEqualTo("userId", userId)
                .whereEqualTo("playlistId", playlistId)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isSaved = !task.getResult().isEmpty();
                        callback.onResult(isSaved);
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    // Save a playlist for a user
    public void savePlaylist(String userId, String playlistId, PlaylistCallback callback) {
        // First check if it's already saved
        isPlaylistSaved(userId, playlistId, new IsPlaylistSavedCallback() {
            @Override
            public void onResult(boolean isSaved) {
                if (isSaved) {
                    // Already saved, just return success
                    callback.onSuccess();
                    return;
                }

                // Create a new SavedPlaylist object
                SavedPlaylist savedPlaylist = new SavedPlaylist(userId, playlistId);

                // Add to Firestore
                firestore.collection("savedPlaylists")
                        .add(savedPlaylist)
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

    // Unsave a playlist for a user
    public void unsavePlaylist(String userId, String playlistId, PlaylistCallback callback) {
        firestore.collection("savedPlaylists")
                .whereEqualTo("userId", userId)
                .whereEqualTo("playlistId", playlistId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            // Not saved, just return success
                            callback.onSuccess();
                            return;
                        }

                        // Get the SavedPlaylist document ID
                        String savedPlaylistId = task.getResult().getDocuments().get(0).getId();

                        // Delete from Firestore
                        firestore.collection("savedPlaylists")
                                .document(savedPlaylistId)
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

    // Get saved playlists for a user (includes both public and private playlists they've saved)
    public void getSavedPlaylists(String userId, PlaylistsCallback callback) {
        // First get all saved playlist IDs for the user
        firestore.collection("savedPlaylists")
                .whereEqualTo("userId", userId)
                .orderBy("savedAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            // No saved playlists
                            callback.onPlaylistsLoaded(new ArrayList<>());
                            return;
                        }

                        List<String> playlistIds = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            SavedPlaylist savedPlaylist = document.toObject(SavedPlaylist.class);
                            if (savedPlaylist != null) {
                                playlistIds.add(savedPlaylist.getPlaylistId());
                            }
                        }

                        // Now fetch all these playlists
                        List<Playlist> savedPlaylists = new ArrayList<>();
                        final int[] pendingRequests = {playlistIds.size()};

                        for (String playlistId : playlistIds) {
                            getPlaylistById(playlistId, new SinglePlaylistCallback() {
                                @Override
                                public void onPlaylistLoaded(Playlist playlist) {
                                    savedPlaylists.add(playlist);
                                    pendingRequests[0]--;
                                    checkComplete();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    // Just log error and continue
                                    android.util.Log.e(TAG, "Error loading playlist: " + errorMessage);
                                    pendingRequests[0]--;
                                    checkComplete();
                                }

                                private void checkComplete() {
                                    if (pendingRequests[0] == 0) {
                                        callback.onPlaylistsLoaded(savedPlaylists);
                                    }
                                }
                            });
                        }
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }
}