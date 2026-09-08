package com.example.queuefree;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * DashboardActivity - Service Center Selection Screen
 * Displays available service centers for token booking
 * Users can select different government services
 */
public class DashboardActivity extends AppCompatActivity {

    // UI Components - Declaration only (no initialization)
    private TextView welcomeTextView;
    private Button rationShopButton, hospitalButton, govtOfficeButton, logoutButton;
    private Button nearbyServicesButton, slotBookingButton;

    // Firebase Authentication instance
    private FirebaseAuth mAuth;
    private Button crowdStatusButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        crowdStatusButton = findViewById(R.id.crowdStatusButton);
        crowdStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, CrowdStatusActivity.class));
            }
        });
        // ✅ CORRECT - Initialize UI components INSIDE onCreate()
        welcomeTextView = findViewById(R.id.welcomeTextView);
        rationShopButton = findViewById(R.id.rationShopButton);
        hospitalButton = findViewById(R.id.hospitalButton);
        govtOfficeButton = findViewById(R.id.govtOfficeButton);
        logoutButton = findViewById(R.id.logoutButton);
        nearbyServicesButton = findViewById(R.id.nearbyServicesButton);
        slotBookingButton = findViewById(R.id.slotBookingButton);

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            welcomeTextView.setText("Welcome, " + currentUser.getEmail());
        }

        // Ration Shop button click listener
        rationShopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToTokenActivity("Ration Shop");
            }
        });

        // Hospital button click listener
        hospitalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToTokenActivity("Government Hospital");
            }
        });

        // Government Office button click listener
        govtOfficeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToTokenActivity("Government Office");
            }
        });

        // Nearby Services button click listener
        nearbyServicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, NearbyServiceActivity.class);
                startActivity(intent);
            }
        });

        // Slot Booking button click listener
        slotBookingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, SlotBookingActivity.class);
                intent.putExtra("SERVICE_CENTER", "Ration Shop"); // Or let user choose
                startActivity(intent);
            }
        });

        // Logout button click listener
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Sign out from Firebase
                mAuth.signOut();

                // Navigate back to MainActivity
                Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    /**
     * Navigate to Token Activity with selected service center
     * @param serviceCenter The name of the selected service center
     */
    private void navigateToTokenActivity(String serviceCenter) {
        Intent intent = new Intent(DashboardActivity.this, TokenActivity.class);
        intent.putExtra("SERVICE_CENTER", serviceCenter);
        startActivity(intent);
    }
}
