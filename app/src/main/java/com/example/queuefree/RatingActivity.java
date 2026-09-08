package com.example.queuefree;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * RatingActivity - Rate service quality after token completion
 */
public class RatingActivity extends AppCompatActivity {

    private TextView serviceCenterTextView;
    private RatingBar serviceRatingBar, staffRatingBar, waitTimeRatingBar;
    private EditText feedbackEditText;
    private Button submitButton;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String serviceCenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        databaseReference = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        serviceCenter = getIntent().getStringExtra("SERVICE_CENTER");

        serviceCenterTextView = findViewById(R.id.serviceCenterTextView);
        serviceRatingBar = findViewById(R.id.serviceRatingBar);
        staffRatingBar = findViewById(R.id.staffRatingBar);
        waitTimeRatingBar = findViewById(R.id.waitTimeRatingBar);
        feedbackEditText = findViewById(R.id.feedbackEditText);
        submitButton = findViewById(R.id.submitButton);

        serviceCenterTextView.setText("Rate: " + serviceCenter);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitRating();
            }
        });
    }

    private void submitRating() {
        if (mAuth.getCurrentUser() == null) return;

        float serviceRating = serviceRatingBar.getRating();
        float staffRating = staffRatingBar.getRating();
        float waitTimeRating = waitTimeRatingBar.getRating();
        String feedback = feedbackEditText.getText().toString().trim();

        if (serviceRating == 0 || staffRating == 0 || waitTimeRating == 0) {
            Toast.makeText(this, "Please rate all categories", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        String serviceCenterKey = serviceCenter.replace(" ", "_");

        HashMap<String, Object> ratingData = new HashMap<>();
        ratingData.put("userId", userId);
        ratingData.put("email", mAuth.getCurrentUser().getEmail());
        ratingData.put("serviceRating", serviceRating);
        ratingData.put("staffRating", staffRating);
        ratingData.put("waitTimeRating", waitTimeRating);
        ratingData.put("averageRating", (serviceRating + staffRating + waitTimeRating) / 3);
        ratingData.put("feedback", feedback);
        ratingData.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()).format(new Date()));

        String ratingId = databaseReference.child("ratings").child(serviceCenterKey).push().getKey();

        if (ratingId != null) {
            databaseReference.child("ratings").child(serviceCenterKey).child(ratingId)
                    .setValue(ratingData)
                    .addOnSuccessListener(aVoid -> {
                        // Award points for feedback
                        RewardsSystem rewards = new RewardsSystem();
                        rewards.awardPoints(20, "Feedback submitted");

                        Toast.makeText(RatingActivity.this,
                                "✅ Thank you for your feedback! +20 points",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });
        }
    }
}
