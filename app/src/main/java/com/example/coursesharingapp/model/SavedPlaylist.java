package com.example.coursesharingapp.model;

import com.google.firebase.Timestamp;

import java.util.Date;

public class SavedPlaylist {
    private String id;
    private String userId;
    private String playlistId;
    private Object savedAt; // Can be either Timestamp or Long

    public SavedPlaylist() {
        // Required empty constructor for Firestore
    }

    public SavedPlaylist(String userId, String playlistId) {
        this.userId = userId;
        this.playlistId = playlistId;
        this.savedAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }

    // Handle both Timestamp and Long types for savedAt
    public Object getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(Object savedAt) {
        this.savedAt = savedAt;
    }

    // Helper method to get savedAt as long
    public long getSavedAtMillis() {
        if (savedAt instanceof Timestamp) {
            return ((Timestamp) savedAt).toDate().getTime();
        } else if (savedAt instanceof Long) {
            return (Long) savedAt;
        }
        return 0;
    }

    // Helper method to get savedAt as Date
    public Date getSavedAtDate() {
        if (savedAt instanceof Timestamp) {
            return ((Timestamp) savedAt).toDate();
        } else if (savedAt instanceof Long) {
            return new Date((Long) savedAt);
        }
        return new Date();
    }
}