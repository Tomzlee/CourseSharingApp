package com.example.coursesharingapp.repository;

import androidx.annotation.NonNull;

import com.example.coursesharingapp.model.Course;
import com.example.coursesharingapp.model.Playlist;
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

    // Get all playlists
    public void getAllPlaylists(PlaylistsCallback callback) {
        firestore.collection("playlists")
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

    // Get playlists by creator
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

    // Search all playlists by title, description, or uploader username
    public void searchAllPlaylists(String query, PlaylistsCallback callback) {
        // Get all playlists and filter client-side since Firestore doesn't support 'contains' queries
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

    // Search user's playlists by title or description
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
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}