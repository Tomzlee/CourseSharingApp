package com.example.coursesharingapp.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
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
    private String category; // Category field
    private Object createdAt; // Can be either Timestamp or Long

    // We need to keep the field name as isPrivate since "private" is a reserved word in Java
    // But we use @PropertyName to tell Firestore to store it as "private"
    private boolean isPrivate;

    private String accessCode; // 9-digit unique access code for private courses

    // Category constants
    public static final String CATEGORY_ART = "Art";
    public static final String CATEGORY_TECH = "Tech/Programming";
    public static final String CATEGORY_BUSINESS = "Business/Marketing";
    public static final String CATEGORY_LIFE = "Life";
    public static final String CATEGORY_OTHER = "Other";

    // Category options for spinner/dropdown
    public static final String[] CATEGORY_OPTIONS = {
            CATEGORY_ART, CATEGORY_TECH, CATEGORY_BUSINESS, CATEGORY_LIFE, CATEGORY_OTHER
    };

    public Course() {
        // Required empty constructor for Firestore
        this.isPrivate = false;
    }

    public Course(String title, String shortDescription, String longDescription,
                  String uploaderUid, String uploaderUsername, String category) {
        this.title = title;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.uploaderUid = uploaderUid;
        this.uploaderUsername = uploaderUsername;
        this.category = category;
        this.createdAt = Timestamp.now();
        this.isPrivate = false;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    // Private course getters and setters
    // Use PropertyName to map the field to "private" in Firestore
    @PropertyName("private")
    public boolean isPrivate() {
        return isPrivate;
    }

    @PropertyName("private")
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }
}