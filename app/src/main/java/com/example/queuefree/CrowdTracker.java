package com.example.queuefree;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

/**
 * CrowdTracker - Real-time presence tracking system
 * Tracks users currently at service centers using Firebase presence detection
 */
public class CrowdTracker {

    private static final String TAG = "CrowdTracker";

    private DatabaseReference databaseReference;
    private DatabaseReference presenceRef;
    private DatabaseReference connectedRef;
    private FirebaseAuth mAuth;

    private String currentServiceCenter;
    private String userId;

    public CrowdTracker() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Mark user as present at a service center
     */
    public void markUserPresent(String serviceCenter) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in");
            return;
        }

        userId = currentUser.getUid();
        currentServiceCenter = serviceCenter.replace(" ", "_");

        // Reference to user's presence at this service center
        presenceRef = databaseReference.child("presence")
                .child(currentServiceCenter)
                .child(userId);

        // Listen for connection state changes
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);

                if (connected != null && connected) {
                    // User is connected - mark as present
                    HashMap<String, Object> presenceData = new HashMap<>();
                    presenceData.put("userId", userId);
                    presenceData.put("email", currentUser.getEmail());
                    presenceData.put("status", "present");
                    presenceData.put("timestamp", ServerValue.TIMESTAMP);

                    // Set presence data
                    presenceRef.setValue(presenceData);

                    // Remove user when they disconnect
                    presenceRef.onDisconnect().removeValue();

                    Log.d(TAG, "User marked as present at " + serviceCenter);
                } else {
                    Log.d(TAG, "User disconnected");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Connection listener error: " + error.getMessage());
            }
        });
    }

    /**
     * Mark user as left (manually remove presence)
     */
    public void markUserLeft() {
        if (presenceRef != null) {
            presenceRef.removeValue();
            Log.d(TAG, "User marked as left");
        }
    }

    /**
     * Get real-time crowd count for a service center
     */
    public void getCrowdCount(String serviceCenter, CrowdCountListener listener) {
        String serviceCenterKey = serviceCenter.replace(" ", "_");

        databaseReference.child("presence").child(serviceCenterKey)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long count = snapshot.getChildrenCount();
                        String crowdLevel = getCrowdLevel((int) count);
                        listener.onCrowdCountUpdate((int) count, crowdLevel);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onError(error.getMessage());
                    }
                });
    }

    /**
     * Calculate crowd level based on count
     */
    private String getCrowdLevel(int count) {
        if (count == 0) return "Empty";
        if (count <= 5) return "Low";
        if (count <= 15) return "Medium";
        if (count <= 30) return "High";
        return "Very High";
    }

    /**
     * Listener interface for crowd count updates
     */
    public interface CrowdCountListener {
        void onCrowdCountUpdate(int count, String crowdLevel);
        void onError(String error);
    }
}
