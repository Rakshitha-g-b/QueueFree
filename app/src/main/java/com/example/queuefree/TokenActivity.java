package com.example.queuefree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * TokenActivity - Token Booking and Queue Display Screen
 * Users can book tokens and view their position in queue
 * Real-time queue updates using Firebase Realtime Database
 */
public class TokenActivity extends AppCompatActivity {

    // UI Components
    private TextView serviceCenterTextView, currentQueueTextView, yourPositionTextView,
            tokenNumberTextView, statusTextView, etaTextView;
    private Button bookTokenButton, refreshButton;

    // Firebase instances
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    // Service center details
    private String serviceCenter;
    private String userTokenId;

    // Notification channel ID
    private static final String CHANNEL_ID = "QueueFree_Notifications";

    // Geofencing
    private GeofencingClient geofencingClient;
    private static final float GEOFENCE_RADIUS = 200; // 200 meters

    // ETA calculation
    private static final int AVG_SERVICE_TIME_MINUTES = 5; // Average time per token

    // Language Helper
    private LanguageHelper languageHelper;

    // Crowd Tracker
    private CrowdTracker crowdTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize helpers
        languageHelper = new LanguageHelper(this);
        crowdTracker = new CrowdTracker();

        // Initialize UI components
        serviceCenterTextView = findViewById(R.id.serviceCenterTextView);
        currentQueueTextView = findViewById(R.id.currentQueueTextView);
        yourPositionTextView = findViewById(R.id.yourPositionTextView);
        tokenNumberTextView = findViewById(R.id.tokenNumberTextView);
        statusTextView = findViewById(R.id.statusTextView);
        etaTextView = findViewById(R.id.etaTextView);
        bookTokenButton = findViewById(R.id.bookTokenButton);
        refreshButton = findViewById(R.id.refreshButton);

        // Get service center from intent
        serviceCenter = getIntent().getStringExtra("SERVICE_CENTER");
        serviceCenterTextView.setText(serviceCenter);

        // Initialize Geofencing Client
        geofencingClient = LocationServices.getGeofencingClient(this);

        // Get center coordinates from intent for geofencing
        double centerLat = getIntent().getDoubleExtra("CENTER_LAT", 0);
        double centerLng = getIntent().getDoubleExtra("CENTER_LNG", 0);

        if (centerLat != 0 && centerLng != 0) {
            setupGeofence(centerLat, centerLng);
        }

        // Create notification channel
        createNotificationChannel();

        // Check if user already has a token
        checkExistingToken();

        // Book token button click listener
        bookTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookToken();
            }
        });
        RewardsSystem rewards = new RewardsSystem();
        rewards.awardPoints(10, "Token booked");

        Toast.makeText(TokenActivity.this,
                "Token booked! +10 points earned",
                Toast.LENGTH_SHORT).show();
        // Refresh button click listener
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateQueueStatus();
            }
        });

        // Start listening to queue updates
        listenToQueueUpdates();
    }

    /**
     * Check if user already has an active token for this service center
     */
    private void checkExistingToken() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        String serviceCenterKey = serviceCenter.replace(" ", "_");

        databaseReference.child("tokens").child(serviceCenterKey)
                .orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // User has existing token
                            for (DataSnapshot tokenSnapshot : snapshot.getChildren()) {
                                String status = tokenSnapshot.child("status").getValue(String.class);

                                if ("waiting".equals(status)) {
                                    userTokenId = tokenSnapshot.getKey();
                                    Long tokenNumber = tokenSnapshot.child("tokenNumber")
                                            .getValue(Long.class);

                                    tokenNumberTextView.setText("Token #" + tokenNumber);
                                    bookTokenButton.setEnabled(false);
                                    bookTokenButton.setText("Token Already Booked");

                                    updateQueueStatus();
                                    break;
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(TokenActivity.this,
                                "Error checking token: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Book a new token for the selected service center
     */
    private void bookToken() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String serviceCenterKey = serviceCenter.replace(" ", "_");

        // Get current queue count to generate token number
        databaseReference.child("queues").child(serviceCenterKey).child("totalTokens")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long totalTokens = snapshot.getValue(Long.class);
                        if (totalTokens == null) {
                            totalTokens = 0L;
                        }

                        long newTokenNumber = totalTokens + 1;

                        // Create token data
                        HashMap<String, Object> tokenData = new HashMap<>();
                        tokenData.put("userId", userId);
                        tokenData.put("email", currentUser.getEmail());
                        tokenData.put("serviceCenter", serviceCenter);
                        tokenData.put("tokenNumber", newTokenNumber);
                        tokenData.put("status", "waiting");
                        tokenData.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()).format(new Date()));

                        // Generate unique token ID
                        String tokenId = databaseReference.child("tokens")
                                .child(serviceCenterKey).push().getKey();

                        if (tokenId != null) {
                            userTokenId = tokenId;

                            // Save token to database
                            databaseReference.child("tokens").child(serviceCenterKey)
                                    .child(tokenId).setValue(tokenData)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                // Update queue statistics
                                                databaseReference.child("queues")
                                                        .child(serviceCenterKey)
                                                        .child("totalTokens")
                                                        .setValue(newTokenNumber);

                                                // Update UI
                                                tokenNumberTextView.setText("Token #" + newTokenNumber);
                                                bookTokenButton.setEnabled(false);
                                                bookTokenButton.setText("Token Booked");

                                                Toast.makeText(TokenActivity.this,
                                                        "Token booked successfully!",
                                                        Toast.LENGTH_SHORT).show();

                                                // Speak token notification
                                                String notification = languageHelper.getTokenNotification((int) newTokenNumber);
                                                languageHelper.speak(notification);

                                                // Mark user as present at service center
                                                crowdTracker.markUserPresent(serviceCenter);

                                                updateQueueStatus();
                                            } else {
                                                Toast.makeText(TokenActivity.this,
                                                        "Failed to book token",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(TokenActivity.this,
                                "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Update and display current queue status
     */
    private void updateQueueStatus() {
        String serviceCenterKey = serviceCenter.replace(" ", "_");

        // Get current serving token number
        databaseReference.child("queues").child(serviceCenterKey).child("currentServing")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long currentServing = snapshot.getValue(Long.class);
                        if (currentServing == null) {
                            currentServing = 0L;
                        }

                        currentQueueTextView.setText("Current Queue: #" + currentServing);

                        // Calculate user's position if they have a token
                        if (userTokenId != null) {
                            calculateUserPosition(serviceCenterKey, currentServing);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(TokenActivity.this,
                                "Error updating queue", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Calculate user's position in the queue with ETA
     */
    private void calculateUserPosition(String serviceCenterKey, Long currentServing) {
        databaseReference.child("tokens").child(serviceCenterKey).child(userTokenId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Long userTokenNumber = snapshot.child("tokenNumber")
                                    .getValue(Long.class);
                            String status = snapshot.child("status").getValue(String.class);

                            if (userTokenNumber != null && "waiting".equals(status)) {
                                long position = userTokenNumber - currentServing;

                                // Calculate and display ETA
                                calculateETA(currentServing, userTokenNumber);

                                if (position <= 0) {
                                    yourPositionTextView.setText("Your turn is NOW!");
                                    statusTextView.setText("Status: Please proceed to counter");
                                    statusTextView.setTextColor(getResources()
                                            .getColor(android.R.color.holo_green_dark));

                                    // Send notification with voice
                                    String turnNotification = languageHelper.getTurnNotification();
                                    sendNotification("Your Turn!", turnNotification);
                                    languageHelper.speak(turnNotification);
                                } else if (position <= 3) {
                                    yourPositionTextView.setText("Your Position: " + position);
                                    statusTextView.setText("Status: Your turn is coming soon!");
                                    statusTextView.setTextColor(getResources()
                                            .getColor(android.R.color.holo_orange_dark));

                                    // Send notification for near turn
                                    sendNotification("Almost Your Turn!",
                                            "You are " + position + " positions away");
                                } else {
                                    yourPositionTextView.setText("Your Position: " + position);
                                    statusTextView.setText("Status: Waiting in queue");
                                    statusTextView.setTextColor(getResources()
                                            .getColor(android.R.color.darker_gray));
                                }
                            } else if ("served".equals(status)) {
                                yourPositionTextView.setText("Token Completed");
                                statusTextView.setText("Status: Thank you for visiting");
                                statusTextView.setTextColor(getResources()
                                        .getColor(android.R.color.holo_blue_dark));
                                bookTokenButton.setEnabled(true);
                                bookTokenButton.setText("Book New Token");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(TokenActivity.this,
                                "Error calculating position", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Calculate and display ETA (Estimated Time of Arrival)
     */
    private void calculateETA(long currentServing, long userTokenNumber) {
        long tokensAhead = userTokenNumber - currentServing;

        if (tokensAhead <= 0) {
            etaTextView.setText("⏰ Your turn is NOW!");
            etaTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            int estimatedMinutes = (int) (tokensAhead * AVG_SERVICE_TIME_MINUTES);

            String etaText;
            if (estimatedMinutes < 60) {
                etaText = "⏰ ETA: ~" + estimatedMinutes + " minutes";
            } else {
                int hours = estimatedMinutes / 60;
                int mins = estimatedMinutes % 60;
                etaText = "⏰ ETA: ~" + hours + " hr " + mins + " min";
            }

            etaTextView.setText(etaText);
            etaTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));

            // Voice notification when within 10 minutes
            if (estimatedMinutes <= 10 && estimatedMinutes > 0) {
                languageHelper.speak("Your token " + userTokenNumber +
                        " will be served in approximately " + estimatedMinutes + " minutes");
            }
        }
    }

    /**
     * Listen to real-time queue updates
     */
    private void listenToQueueUpdates() {
        String serviceCenterKey = serviceCenter.replace(" ", "_");

        // Listen for changes in current serving number
        databaseReference.child("queues").child(serviceCenterKey).child("currentServing")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        updateQueueStatus();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    /**
     * Setup geofence around service center
     */
    private void setupGeofence(double latitude, double longitude) {
        Geofence geofence = new Geofence.Builder()
                .setRequestId(serviceCenter)
                .setCircularRegion(latitude, longitude, GEOFENCE_RADIUS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        PendingIntent geofencePendingIntent = PendingIntent.getBroadcast(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(TokenActivity.this,
                                    "Location tracking enabled", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Geofence", "Failed to add geofence", e);
                        }
                    });
        }
    }

    /**
     * Create notification channel for Android O and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "QueueFree Notifications";
            String description = "Notifications for queue updates";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Send notification to user
     */
    private void sendNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Check for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                return;
            }
        }

        notificationManager.notify(1, builder.build());
    }

    /**
     * Cleanup when activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Shutdown language helper
        if (languageHelper != null) {
            languageHelper.shutdown();
        }

        // Mark user as left from service center
        if (crowdTracker != null) {
            crowdTracker.markUserLeft();
        }
    }
}
