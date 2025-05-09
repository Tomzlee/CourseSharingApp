package com.example.coursesharingapp.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.example.coursesharingapp.R;

/**
 * Helper class to display file size limit notifications to users
 * before they attempt to upload files.
 */
public class FileSizeNotificationManager {

    private static final String PREF_NAME = "file_size_prefs";
    private static final String PREF_KEY_DONT_SHOW_AGAIN = "dont_show_file_size_notification";

    // Maximum file size in bytes (5GB)
    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024;

    // Maximum file size in MB for display
    public static final long MAX_FILE_SIZE_MB = MAX_FILE_SIZE / (1024 * 1024);

    /**
     * Shows a notification about the file size limit before uploading
     * @param context The context
     * @param callback Callback with the result (proceed or cancel)
     */
    public static void showFileSizeLimitNotification(Context context, FileSizeNotificationCallback callback) {
        // Check if the user has chosen not to see this notification again
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean dontShowAgain = prefs.getBoolean(PREF_KEY_DONT_SHOW_AGAIN, false);

        if (dontShowAgain) {
            // Skip the notification if the user doesn't want to see it
            callback.onProceed();
            return;
        }

        // Create a custom dialog view with a checkbox
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_file_size_notice, null);
        CheckBox dontShowAgainCheckbox = dialogView.findViewById(R.id.dont_show_again_checkbox);

        // Create and show the dialog
        new AlertDialog.Builder(context)
                .setTitle("File Size Limit")
                .setView(dialogView)
                .setMessage("Please note that there is a 5GB size limit for video and image uploads. Files exceeding this limit will be rejected.")
                .setPositiveButton("I Understand", (dialog, which) -> {
                    // Save the "don't show again" preference if checked
                    if (dontShowAgainCheckbox.isChecked()) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(PREF_KEY_DONT_SHOW_AGAIN, true);
                        editor.apply();
                    }
                    callback.onProceed();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    callback.onCancel();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Format file size for display
     * @param sizeInBytes File size in bytes
     * @return Formatted size string (e.g., "4.2 MB" or "1.3 GB")
     */
    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeInBytes / 1024.0);
        } else if (sizeInBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", sizeInBytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", sizeInBytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Reset the "don't show again" preference
     * @param context The context
     */
    public static void resetDontShowAgainPreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_KEY_DONT_SHOW_AGAIN, false);
        editor.apply();
    }

    /**
     * Callback interface for the file size notification
     */
    public interface FileSizeNotificationCallback {
        void onProceed();
        void onCancel();
    }
}