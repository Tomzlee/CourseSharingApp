package com.example.coursesharingapp.model;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Playlist {
    private String id;
    private String title;
    private String description;
    private String creatorUid;
    private String creatorUsername;
    private List<String> courseIds; // List of course IDs in the playlist
    private Object createdAt; // Can be either Timestamp or Long

    public Playlist() {
        // Required empty constructor for Firestore
        courseIds = new ArrayList<>();
    }

    public Playlist(String title, String description, String creatorUid, String creatorUsername) {
        this.title = title;
        this.description = description;
        this.creatorUid = creatorUid;
        this.creatorUsername = creatorUsername;
        this.courseIds = new ArrayList<>();
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatorUid() {
        return creatorUid;
    }

    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public List<String> getCourseIds() {
        return courseIds;
    }

    public void setCourseIds(List<String> courseIds) {
        this.courseIds = courseIds;
    }

    public void addCourseId(String courseId) {
        if (courseIds == null) {
            courseIds = new ArrayList<>();
        }
        if (!courseIds.contains(courseId)) {
            courseIds.add(courseId);
        }
    }

    public void removeCourseId(String courseId) {
        if (courseIds != null) {
            courseIds.remove(courseId);
        }
    }

    public int getCoursesCount() {
        return courseIds != null ? courseIds.size() : 0;
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