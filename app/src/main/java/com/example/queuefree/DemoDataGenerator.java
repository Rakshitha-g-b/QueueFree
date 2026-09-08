package com.example.queuefree;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Random;

/**
 * DemoDataGenerator - Generates realistic demo data for presentations
 * Automatically creates service centers, simulates crowd, and manages queue
 */
public class DemoDataGenerator {

    private static final String TAG = "DemoDataGenerator";
    private DatabaseReference databaseReference;
    private Handler handler;
    private Random random;

    private boolean isGenerating = false;

    public DemoDataGenerator() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        handler = new Handler(Looper.getMainLooper());
        random = new Random();
    }

    /**
     * Setup initial demo data - service centers and queues
     */
    public void setupDemoData() {
        Log.d(TAG, "Setting up demo data...");

        // Create service centers
        createServiceCenter("Ration_Shop", "Ration Shop - Jayanagar",
                "Ration Shop", 12.9352, 77.6245,
                "4th Block, Jayanagar, Bangalore");

        createServiceCenter("Government_Hospital", "Government Hospital - Koramangala",
                "Government Hospital", 12.9279, 77.6271,
                "Koramangala, Bangalore");

        createServiceCenter("Government_Office", "Government Office - Indiranagar",
                "Government Office", 12.9716, 77.6412,
                "Indiranagar, Bangalore");

        // Initialize queue counters
        initializeQueue("Ration_Shop");
        initializeQueue("Government_Hospital");
        initializeQueue("Government_Office");

        Log.d(TAG, "Demo data setup complete!");
    }

    /**
     * Create a service center in Firebase
     */
    private void createServiceCenter(String key, String name, String type,
                                     double lat, double lng, String address) {
        HashMap<String, Object> centerData = new HashMap<>();
        centerData.put("name", name);
        centerData.put("type", type);
        centerData.put("latitude", lat);
        centerData.put("longitude", lng);
        centerData.put("address", address);

        databaseReference.child("service_centers").child(key).setValue(centerData);
    }

    /**
     * Initialize queue with starting values
     */
    private void initializeQueue(String serviceCenter) {
        HashMap<String, Object> queueData = new HashMap<>();
        queueData.put("currentServing", 0);
        queueData.put("totalTokens", 0);

        databaseReference.child("queues").child(serviceCenter).setValue(queueData);
    }

    /**
     * Start simulating live crowd - people entering and leaving
     */
    public void startCrowdSimulation() {
        if (isGenerating) {
            Log.w(TAG, "Simulation already running");
            return;
        }

        isGenerating = true;
        Log.d(TAG, "Starting crowd simulation...");

        simulateCrowdForCenter("Ration_Shop", "Ration Shop");
        simulateCrowdForCenter("Government_Hospital", "Government Hospital");
        simulateCrowdForCenter("Government_Office", "Government Office");
    }

    /**
     * Simulate crowd for a specific service center
     */
    private void simulateCrowdForCenter(String centerKey, String centerName) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isGenerating) return;

                // Randomly add or remove people
                int action = random.nextInt(3); // 0=add, 1=remove, 2=no change

                if (action == 0) {
                    // Add a person
                    addSimulatedPerson(centerKey, centerName);
                } else if (action == 1) {
                    // Remove a random person
                    removeSimulatedPerson(centerKey);
                }

                // Schedule next update (every 3-8 seconds)
                int delay = 3000 + random.nextInt(5000);
                handler.postDelayed(this, delay);
            }
        }, 2000); // Start after 2 seconds
    }

    /**
     * Add a simulated person to the crowd
     */
    private void addSimulatedPerson(String centerKey, String centerName) {
        String[] demoNames = {
                "demo.user1@gmail.com",
                "demo.user2@gmail.com",
                "demo.user3@gmail.com",
                "rajesh.kumar@gmail.com",
                "priya.sharma@gmail.com",
                "amit.patel@gmail.com",
                "sneha.reddy@gmail.com",
                "vijay.singh@gmail.com",
                "lakshmi.iyer@gmail.com",
                "karthik.rao@gmail.com"
        };

        String userId = "demo_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
        String email = demoNames[random.nextInt(demoNames.length)];

        HashMap<String, Object> presenceData = new HashMap<>();
        presenceData.put("userId", userId);
        presenceData.put("email", email);
        presenceData.put("status", "present");
        presenceData.put("timestamp", ServerValue.TIMESTAMP);

        databaseReference.child("presence").child(centerKey).child(userId)
                .setValue(presenceData);

        Log.d(TAG, "Added person to " + centerName + ": " + email);
    }

    /**
     * Remove a random simulated person from the crowd
     */
    private void removeSimulatedPerson(String centerKey) {
        databaseReference.child("presence").child(centerKey)
                .limitToFirst(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                        String firstKey = snapshot.getChildren().iterator().next().getKey();
                        databaseReference.child("presence").child(centerKey)
                                .child(firstKey).removeValue();
                        Log.d(TAG, "Removed person from " + centerKey);
                    }
                });
    }

    /**
     * Simulate queue progression
     */
    public void startQueueSimulation() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isGenerating) return;

                // Randomly advance queues
                advanceQueue("Ration_Shop");
                advanceQueue("Government_Hospital");
                advanceQueue("Government_Office");

                // Schedule next update (every 10-15 seconds)
                int delay = 10000 + random.nextInt(5000);
                handler.postDelayed(this, delay);
            }
        }, 5000);
    }

    /**
     * Advance queue counter
     */
    private void advanceQueue(String centerKey) {
        databaseReference.child("queues").child(centerKey).child("currentServing")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Long current = snapshot.getValue(Long.class);
                    if (current == null) current = 0L;

                    // Advance by 1
                    databaseReference.child("queues").child(centerKey)
                            .child("currentServing").setValue(current + 1);

                    Log.d(TAG, "Advanced queue for " + centerKey + " to " + (current + 1));
                });
    }

    /**
     * Stop all simulations
     */
    public void stopSimulation() {
        isGenerating = false;
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Simulation stopped");
    }

    /**
     * Clear all demo data
     */
    public void clearDemoData() {
        databaseReference.child("presence").removeValue();
        databaseReference.child("queues").removeValue();
        databaseReference.child("tokens").removeValue();
        Log.d(TAG, "Demo data cleared");
    }
}
