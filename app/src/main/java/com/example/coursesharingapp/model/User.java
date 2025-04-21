package com.example.coursesharingapp.model;

import com.google.firebase.Timestamp;

import java.util.Date;

public class User {
    private String id;
    private String username;
    private String email;
    private Object createdAt; // Can be either Timestamp or Long

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Handle both Timestamp and Long types for createdAt
    public Object getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }

    // Helper method to get createdAt as long
    public long getCreatedAtMillis() {
        if (createdAt instanceof Timestamp) {
            return ((Timestamp) createdAt).toDate().getTime();
        } else if (createdAt instanceof Long) {
            return (Long) createdAt;
        }
        return 0;
    }

    // Helper method to get createdAt as Date
    public Date getCreatedAtDate() {
        if (createdAt instanceof Timestamp) {
            return ((Timestamp) createdAt).toDate();
        } else if (createdAt instanceof Long) {
            return new Date((Long) createdAt);
        }
        return new Date();
    }
}
