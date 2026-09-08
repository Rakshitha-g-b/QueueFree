package com.example.queuefree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * CrowdStatusActivity - Real-time crowd monitoring dashboard
 * Shows live occupancy and crowd levels for all service centers
 */
public class CrowdStatusActivity extends AppCompatActivity {

    private Spinner serviceCenterSpinner;
    private TextView crowdCountTextView, crowdLevelTextView, statusMessageTextView;
    private CardView crowdStatusCard;
    private LinearLayout userListLayout;
    private Button startDemoButton, stopDemoButton;

    private DatabaseReference databaseReference;
    private CrowdTracker crowdTracker;
    private DemoDataGenerator demoDataGenerator;

    private String selectedServiceCenter;
    private ValueEventListener crowdListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crowd_status);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        crowdTracker = new CrowdTracker();
        demoDataGenerator = new DemoDataGenerator();

        // Initialize views
        serviceCenterSpinner = findViewById(R.id.serviceCenterSpinner);
        crowdCountTextView = findViewById(R.id.crowdCountTextView);
        crowdLevelTextView = findViewById(R.id.crowdLevelTextView);
        statusMessageTextView = findViewById(R.id.statusMessageTextView);
        crowdStatusCard = findViewById(R.id.crowdStatusCard);
        userListLayout = findViewById(R.id.userListLayout);
        startDemoButton = findViewById(R.id.startDemoButton);
        stopDemoButton = findViewById(R.id.stopDemoButton);

        // Setup service center spinner
        setupServiceCenterSpinner();

        // Setup demo buttons
        setupDemoButtons();

        // Initialize demo data on first launch
        demoDataGenerator.setupDemoData();
    }

    /**
     * Setup service center selection spinner
     */
    private void setupServiceCenterSpinner() {
        List<String> serviceCenters = new ArrayList<>();
        serviceCenters.add("Select Service Center");
        serviceCenters.add("Ration Shop");
        serviceCenters.add("Government Hospital");
        serviceCenters.add("Government Office");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, serviceCenters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serviceCenterSpinner.setAdapter(adapter);

        serviceCenterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedServiceCenter = serviceCenters.get(position);
                    loadCrowdData();
                } else {
                    // Show default empty state
                    updateUI(0, "Unknown");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    /**
     * Setup demo control buttons
     */
    private void setupDemoButtons() {
        startDemoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                demoDataGenerator.startCrowdSimulation();
                demoDataGenerator.startQueueSimulation();
                Toast.makeText(CrowdStatusActivity.this,
                        "🔴 Live Demo Started!", Toast.LENGTH_SHORT).show();
                startDemoButton.setEnabled(false);
                stopDemoButton.setEnabled(true);
            }
        });

        stopDemoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                demoDataGenerator.stopSimulation();
                Toast.makeText(CrowdStatusActivity.this,
                        "Demo Stopped", Toast.LENGTH_SHORT).show();
                startDemoButton.setEnabled(true);
                stopDemoButton.setEnabled(false);
            }
        });
    }

    /**
     * Load real-time crowd data for selected service center
     */
    private void loadCrowdData() {
        if (selectedServiceCenter == null) return;

        String serviceCenterKey = selectedServiceCenter.replace(" ", "_");

        // Remove previous listener if exists
        if (crowdListener != null) {
            databaseReference.child("presence").child(serviceCenterKey)
                    .removeEventListener(crowdListener);
        }

        // Create new listener for real-time updates
        crowdListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                String crowdLevel = getCrowdLevel((int) count);
                updateUI((int) count, crowdLevel);
                loadUserList(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CrowdStatusActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        // Attach listener
        databaseReference.child("presence").child(serviceCenterKey)
                .addValueEventListener(crowdListener);
    }

    /**
     * Get crowd level based on count
     */
    private String getCrowdLevel(int count) {
        if (count == 0) return "Empty";
        if (count <= 5) return "Low";
        if (count <= 15) return "Medium";
        if (count <= 30) return "High";
        return "Very High";
    }

    /**
     * Update UI with crowd information
     */
    private void updateUI(int count, String crowdLevel) {
        crowdCountTextView.setText(String.valueOf(count));
        crowdLevelTextView.setText("Crowd Level: " + crowdLevel);

        // Set status message and colors based on crowd level
        int backgroundColor;
        int textColor = Color.WHITE;
        String message;

        switch (crowdLevel) {
            case "Empty":
                backgroundColor = Color.parseColor("#9E9E9E"); // Gray
                message = "🏪 No crowd - Perfect time to visit!";
                break;
            case "Low":
                backgroundColor = Color.parseColor("#4CAF50"); // Green
                message = "✅ Low crowd - Good time to visit";
                break;
            case "Medium":
                backgroundColor = Color.parseColor("#FFC107"); // Yellow/Orange
                message = "⚠️ Medium crowd - Expect some waiting";
                textColor = Color.BLACK;
                break;
            case "High":
                backgroundColor = Color.parseColor("#FF9800"); // Orange
                message = "⚠️ High crowd - Long wait expected";
                break;
            case "Very High":
                backgroundColor = Color.parseColor("#F44336"); // Red
                message = "🚫 Very crowded - Consider coming later";
                break;
            default:
                backgroundColor = Color.GRAY;
                message = "Select a service center to view crowd status";
        }

        crowdStatusCard.setCardBackgroundColor(backgroundColor);
        statusMessageTextView.setText(message);
        statusMessageTextView.setTextColor(textColor);
        crowdCountTextView.setTextColor(textColor);
        crowdLevelTextView.setTextColor(textColor);
    }

    /**
     * Load list of users currently present from snapshot
     */
    private void loadUserList(DataSnapshot snapshot) {
        userListLayout.removeAllViews();

        if (snapshot.getChildrenCount() == 0) {
            TextView emptyView = new TextView(CrowdStatusActivity.this);
            emptyView.setText("No users currently present");
            emptyView.setTextColor(Color.GRAY);
            emptyView.setTextSize(16);
            emptyView.setPadding(16, 16, 16, 16);
            userListLayout.addView(emptyView);
            return;
        }

        int userCount = 0;
        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
            userCount++;
            String email = userSnapshot.child("email").getValue(String.class);

            TextView userView = new TextView(CrowdStatusActivity.this);
            userView.setText(userCount + ". 👤 " + (email != null ? email : "Anonymous User"));
            userView.setTextSize(16);
            userView.setTextColor(Color.parseColor("#212121"));
            userView.setPadding(20, 16, 20, 16);
            userView.setBackgroundColor(Color.parseColor("#F5F5F5"));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 8);
            userView.setLayoutParams(params);

            userListLayout.addView(userView);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop demo when activity is destroyed
        if (demoDataGenerator != null) {
            demoDataGenerator.stopSimulation();
        }

        // Remove listeners
        if (crowdListener != null && selectedServiceCenter != null) {
            String serviceCenterKey = selectedServiceCenter.replace(" ", "_");
            databaseReference.child("presence").child(serviceCenterKey)
                    .removeEventListener(crowdListener);
        }
    }
}
