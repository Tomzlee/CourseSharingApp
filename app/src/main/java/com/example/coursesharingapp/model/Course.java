package com.example.coursesharingapp.model;

import com.google.firebase.Timestamp;
import java.util.Date;

public class Course {
    private String id;
    private String title;
    private String shortDescription;
    private String longDescription;
    private String uploaderUid;
    private String uploaderUsername;
    private String thumbnailUrl;
    private String videoUrl;
    private Object createdAt; // Can be either Timestamp or Long

    public Course() {
        // Required empty constructor for Firestore
    }

    public Course(String title, String shortDescription, String longDescription,
                  String uploaderUid, String uploaderUsername) {
        this.title = title;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.uploaderUid = uploaderUid;
        this.uploaderUsername = uploaderUsername;
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

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    public String getUploaderUid() {
        return uploaderUid;
    }

    public void setUploaderUid(String uploaderUid) {
        this.uploaderUid = uploaderUid;
    }

    public String getUploaderUsername() {
        return uploaderUsername;
    }

    public void setUploaderUsername(String uploaderUsername) {
        this.uploaderUsername = uploaderUsername;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
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