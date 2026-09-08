package com.example.queuefree;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * GeofenceBroadcastReceiver - Handles geofence transition events
 * Triggers when user enters/exits service center vicinity (200m radius)
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceReceiver";
    private static final String CHANNEL_ID = "QueueFree_Geofence";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error");
            return;
        }

        // Get the transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // User entered the geofence (within 200m of center)
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            if (triggeringGeofences != null && !triggeringGeofences.isEmpty()) {
                String locationName = triggeringGeofences.get(0).getRequestId();

                // Send notification
                sendGeofenceNotification(context,
                        "You're Near!",
                        "You've arrived near " + locationName + ". Check your queue status!");

                // Mark user as "Arrived" in Firebase
                markUserAsArrived(context, locationName);
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // User exited the geofence
            Log.d(TAG, "User left the area");
        }
    }

    /**
     * Send geofence notification with permission check
     */
    private void sendGeofenceNotification(Context context, String title, String message) {
        // Check for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted");
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        notificationManager.notify(2, builder.build());
    }

    /**
     * Mark user as arrived in Firebase
     */
    private void markUserAsArrived(Context context, String locationName) {
        // Update Firebase with arrival status
        // Implementation depends on your data structure
        Log.d(TAG, "User marked as arrived at: " + locationName);
    }
}
