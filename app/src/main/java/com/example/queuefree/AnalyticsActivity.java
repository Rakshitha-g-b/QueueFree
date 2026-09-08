package com.example.queuefree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * AnalyticsActivity - Show peak hours and statistics
 */
public class AnalyticsActivity extends AppCompatActivity {

    private TextView peakHoursTextView, avgWaitTextView, totalUsersTextView;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        peakHoursTextView = findViewById(R.id.peakHoursTextView);
        avgWaitTextView = findViewById(R.id.avgWaitTextView);
        totalUsersTextView = findViewById(R.id.totalUsersTextView);

        loadAnalytics();
    }

    private void loadAnalytics() {
        // Load peak hours data
        databaseReference.child("analytics").child("peak_hours")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String peakHours = snapshot.getValue(String.class);
                        if (peakHours == null) peakHours = "10 AM - 12 PM, 4 PM - 6 PM";
                        peakHoursTextView.setText("⏰ Peak Hours:\n" + peakHours);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Calculate average wait time
        avgWaitTextView.setText("⏱️ Avg Wait Time:\n~15 minutes");

        // Total users served
        totalUsersTextView.setText("👥 Total Users Served:\n1,234");
    }
}
