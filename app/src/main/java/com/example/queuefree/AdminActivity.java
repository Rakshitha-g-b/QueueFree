package com.example.queuefree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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
 * AdminActivity - Admin Dashboard for Queue Management
 * Staff can view and manage tokens in the queue
 * Mark tokens as served to update queue status
 */
public class AdminActivity extends AppCompatActivity {

    // UI Components
    private Spinner serviceCenterSpinner;
    private TextView currentServingTextView, totalTokensTextView;
    private ListView tokensListView;
    private Button nextTokenButton, resetQueueButton;

    // Firebase Database reference
    private DatabaseReference databaseReference;

    // Data variables
    private String selectedServiceCenter;
    private List<TokenModel> tokensList;
    private ArrayAdapter<String> tokensAdapter;
    private List<String> tokensDisplayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize UI components
        serviceCenterSpinner = findViewById(R.id.serviceCenterSpinner);
        currentServingTextView = findViewById(R.id.currentServingTextView);
        totalTokensTextView = findViewById(R.id.totalTokensTextView);
        tokensListView = findViewById(R.id.tokensListView);
        nextTokenButton = findViewById(R.id.nextTokenButton);
        resetQueueButton = findViewById(R.id.resetQueueButton);

        // Initialize lists
        tokensList = new ArrayList<>();
        tokensDisplayList = new ArrayList<>();

        // Setup service center spinner
        setupServiceCenterSpinner();

        // Setup tokens list adapter
        tokensAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, tokensDisplayList);
        tokensListView.setAdapter(tokensAdapter);

        // Next token button click listener
        nextTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serveNextToken();
            }
        });

        // Reset queue button click listener
        resetQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetQueue();
            }
        });

        // List item click listener to mark specific token as served
        tokensListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < tokensList.size()) {
                    TokenModel token = tokensList.get(position);
                    markTokenAsServed(token);
                }
            }
        });
    }

    /**
     * Setup service center spinner with available options
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
                    loadQueueData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    /**
     * Load queue data for selected service center
     */
    private void loadQueueData() {
        if (selectedServiceCenter == null) return;

        String serviceCenterKey = selectedServiceCenter.replace(" ", "_");

        // Load queue statistics
        databaseReference.child("queues").child(serviceCenterKey)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long currentServing = snapshot.child("currentServing").getValue(Long.class);
                        Long totalTokens = snapshot.child("totalTokens").getValue(Long.class);

                        if (currentServing == null) currentServing = 0L;
                        if (totalTokens == null) totalTokens = 0L;

                        currentServingTextView.setText("Current Serving: #" + currentServing);
                        totalTokensTextView.setText("Total Tokens: " + totalTokens);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminActivity.this,
                                "Error loading queue data", Toast.LENGTH_SHORT).show();
                    }
                });

        // Load all tokens
        loadTokensList(serviceCenterKey);
    }

    /**
     * Load list of all tokens for the service center
     */
    private void loadTokensList(String serviceCenterKey) {
        databaseReference.child("tokens").child(serviceCenterKey)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        tokensList.clear();
                        tokensDisplayList.clear();

                        for (DataSnapshot tokenSnapshot : snapshot.getChildren()) {
                            String tokenId = tokenSnapshot.getKey();
                            Long tokenNumber = tokenSnapshot.child("tokenNumber")
                                    .getValue(Long.class);
                            String email = tokenSnapshot.child("email").getValue(String.class);
                            String status = tokenSnapshot.child("status").getValue(String.class);
                            String timestamp = tokenSnapshot.child("timestamp")
                                    .getValue(String.class);

                            TokenModel token = new TokenModel(tokenId, tokenNumber,
                                    email, status, timestamp);
                            tokensList.add(token);

                            // Add to display list only if waiting
                            if ("waiting".equals(status)) {
                                String displayText = "Token #" + tokenNumber +
                                        " - " + email + " (" + status + ")";
                                tokensDisplayList.add(displayText);
                            }
                        }

                        tokensAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminActivity.this,
                                "Error loading tokens", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Serve next token in queue
     */
    private void serveNextToken() {
        if (selectedServiceCenter == null) {
            Toast.makeText(this, "Please select a service center", Toast.LENGTH_SHORT).show();
            return;
        }

        String serviceCenterKey = selectedServiceCenter.replace(" ", "_");

        // Get current serving number
        databaseReference.child("queues").child(serviceCenterKey).child("currentServing")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long currentServing = snapshot.getValue(Long.class);
                        if (currentServing == null) currentServing = 0L;

                        long nextToken = currentServing + 1;

                        // Update current serving
                        databaseReference.child("queues").child(serviceCenterKey)
                                .child("currentServing").setValue(nextToken);

                        // Find and mark token as served
                        for (TokenModel token : tokensList) {
                            if (token.getTokenNumber() != null &&
                                    token.getTokenNumber() == nextToken &&
                                    "waiting".equals(token.getStatus())) {

                                databaseReference.child("tokens").child(serviceCenterKey)
                                        .child(token.getTokenId()).child("status")
                                        .setValue("served");

                                Toast.makeText(AdminActivity.this,
                                        "Token #" + nextToken + " served",
                                        Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminActivity.this,
                                "Error serving token", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Mark specific token as served
     */
    private void markTokenAsServed(TokenModel token) {
        if (selectedServiceCenter == null) return;

        String serviceCenterKey = selectedServiceCenter.replace(" ", "_");

        // Mark token as served
        databaseReference.child("tokens").child(serviceCenterKey)
                .child(token.getTokenId()).child("status").setValue("served");

        Toast.makeText(this, "Token #" + token.getTokenNumber() + " marked as served",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Reset queue - clear all tokens
     */
    private void resetQueue() {
        if (selectedServiceCenter == null) {
            Toast.makeText(this, "Please select a service center", Toast.LENGTH_SHORT).show();
            return;
        }

        String serviceCenterKey = selectedServiceCenter.replace(" ", "_");

        // Reset queue counters
        databaseReference.child("queues").child(serviceCenterKey).child("currentServing")
                .setValue(0);
        databaseReference.child("queues").child(serviceCenterKey).child("totalTokens")
                .setValue(0);

        // Delete all tokens
        databaseReference.child("tokens").child(serviceCenterKey).removeValue();

        Toast.makeText(this, "Queue reset successfully", Toast.LENGTH_SHORT).show();
    }

    /**
     * TokenModel class to hold token data
     */
    private static class TokenModel {
        private String tokenId;
        private Long tokenNumber;
        private String email;
        private String status;
        private String timestamp;

        public TokenModel(String tokenId, Long tokenNumber, String email,
                          String status, String timestamp) {
            this.tokenId = tokenId;
            this.tokenNumber = tokenNumber;
            this.email = email;
            this.status = status;
            this.timestamp = timestamp;
        }

        public String getTokenId() {
            return tokenId;
        }

        public Long getTokenNumber() {
            return tokenNumber;
        }

        public String getStatus() {
            return status;
        }
    }
}
