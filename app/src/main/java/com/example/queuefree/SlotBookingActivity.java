package com.example.queuefree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * SlotBookingActivity - Time slot based token booking
 * Users can book specific 15-minute time slots
 * Reduces crowding and improves queue management
 */
public class SlotBookingActivity extends AppCompatActivity {

    private TextView serviceCenterTextView, selectedSlotTextView;
    private ListView slotsListView;
    private Button bookSlotButton;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    private String serviceCenter;
    private List<TimeSlot> slotsList;
    private List<String> displaySlots;
    private SlotAdapter adapter;
    private TimeSlot selectedSlot;

    private static final int MAX_SLOTS_PER_TIME = 5; // Max people per slot

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slot_booking);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Get service center from intent
        serviceCenter = getIntent().getStringExtra("SERVICE_CENTER");

        // Initialize views
        serviceCenterTextView = findViewById(R.id.serviceCenterTextView);
        selectedSlotTextView = findViewById(R.id.selectedSlotTextView);
        slotsListView = findViewById(R.id.slotsListView);
        bookSlotButton = findViewById(R.id.bookSlotButton);

        serviceCenterTextView.setText(serviceCenter);

        slotsList = new ArrayList<>();
        displaySlots = new ArrayList<>();

        // Use custom adapter
        adapter = new SlotAdapter(this, displaySlots, slotsList);
        slotsListView.setAdapter(adapter);

        // Generate time slots
        generateTimeSlots();

        // Load slot availability
        loadSlotAvailability();

        // List item click
        slotsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < slotsList.size()) {
                    selectedSlot = slotsList.get(position);
                    selectedSlotTextView.setText("Selected: " + selectedSlot.getTimeRange());
                    bookSlotButton.setEnabled(selectedSlot.getAvailableSpots() > 0);
                }
            }
        });

        // Book slot button
        bookSlotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookSelectedSlot();
            }
        });
    }

    /**
     * Generate 15-minute time slots for the day
     */
    private void generateTimeSlots() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 9); // Start at 9 AM
        calendar.set(Calendar.MINUTE, 0);

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        for (int i = 0; i < 32; i++) { // 8 hours * 4 slots = 32 slots
            String startTime = timeFormat.format(calendar.getTime());

            calendar.add(Calendar.MINUTE, 15);
            String endTime = timeFormat.format(calendar.getTime());

            String slotId = startTime.replace(" ", "").replace(":", "");
            TimeSlot slot = new TimeSlot(slotId, startTime + " - " + endTime,
                    MAX_SLOTS_PER_TIME, MAX_SLOTS_PER_TIME);
            slotsList.add(slot);
        }
    }

    /**
     * Load slot availability from Firebase
     */
    private void loadSlotAvailability() {
        String serviceCenterKey = serviceCenter.replace(" ", "_");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        databaseReference.child("slots").child(serviceCenterKey).child(today)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        displaySlots.clear();

                        for (TimeSlot slot : slotsList) {
                            if (snapshot.hasChild(slot.getSlotId())) {
                                Long bookedCount = snapshot.child(slot.getSlotId())
                                        .child("booked").getValue(Long.class);
                                if (bookedCount != null) {
                                    slot.setAvailableSpots(MAX_SLOTS_PER_TIME - bookedCount.intValue());
                                }
                            }

                            String availability = slot.getAvailableSpots() > 0 ?
                                    "✅ Available (" + slot.getAvailableSpots() + " spots)" :
                                    "❌ Full";

                            String crowdLevel = getCrowdLevel(slot.getAvailableSpots());

                            displaySlots.add(slot.getTimeRange() + "\n" +
                                    availability + " " + crowdLevel);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SlotBookingActivity.this,
                                "Error loading slots", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Get crowd level indicator
     */
    private String getCrowdLevel(int available) {
        if (available >= 4) return "🟢 Low crowd";
        if (available >= 2) return "🟡 Medium crowd";
        if (available >= 1) return "🟠 High crowd";
        return "🔴 Full";
    }

    /**
     * Book selected time slot
     */
    private void bookSelectedSlot() {
        if (selectedSlot == null) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String serviceCenterKey = serviceCenter.replace(" ", "_");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        String userId = mAuth.getCurrentUser().getUid();

        // Create booking data
        HashMap<String, Object> bookingData = new HashMap<>();
        bookingData.put("userId", userId);
        bookingData.put("email", mAuth.getCurrentUser().getEmail());
        bookingData.put("timeSlot", selectedSlot.getTimeRange());
        bookingData.put("status", "booked");

        // Save booking
        String bookingId = databaseReference.child("slots").child(serviceCenterKey)
                .child(today).child(selectedSlot.getSlotId())
                .child("bookings").push().getKey();

        if (bookingId != null) {
            databaseReference.child("slots").child(serviceCenterKey)
                    .child(today).child(selectedSlot.getSlotId())
                    .child("bookings").child(bookingId)
                    .setValue(bookingData)
                    .addOnSuccessListener(aVoid -> {
                        // Increment booked count
                        databaseReference.child("slots").child(serviceCenterKey)
                                .child(today).child(selectedSlot.getSlotId())
                                .child("booked")
                                .setValue(MAX_SLOTS_PER_TIME - selectedSlot.getAvailableSpots() + 1);

                        Toast.makeText(SlotBookingActivity.this,
                                "Slot booked successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SlotBookingActivity.this,
                                "Failed to book slot", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Custom adapter for time slots with colored indicators
     */
    private class SlotAdapter extends ArrayAdapter<String> {
        private Context context;
        private List<String> slots;
        private List<TimeSlot> slotData;

        public SlotAdapter(Context context, List<String> slots, List<TimeSlot> slotData) {
            super(context, android.R.layout.simple_list_item_1, slots);
            this.context = context;
            this.slots = slots;
            this.slotData = slotData;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            TextView textView = (TextView) super.getView(position, convertView, parent);

            // Set text properties for better visibility
            textView.setTextSize(16);
            textView.setPadding(20, 20, 20, 20);
            textView.setTextColor(getResources().getColor(android.R.color.black));

            // Set background color based on availability
            if (position < slotData.size()) {
                TimeSlot slot = slotData.get(position);
                int available = slot.getAvailableSpots();

                if (available >= 4) {
                    textView.setBackgroundColor(0xFFE8F5E9); // Light green
                } else if (available >= 2) {
                    textView.setBackgroundColor(0xFFFFF9C4); // Light yellow
                } else if (available >= 1) {
                    textView.setBackgroundColor(0xFFFFE0B2); // Light orange
                } else {
                    textView.setBackgroundColor(0xFFFFCDD2); // Light red
                }
            }

            return textView;
        }
    }

    /**
     * TimeSlot model class
     */
    private static class TimeSlot {
        private String slotId;
        private String timeRange;
        private int totalSpots;
        private int availableSpots;

        public TimeSlot(String slotId, String timeRange, int totalSpots, int availableSpots) {
            this.slotId = slotId;
            this.timeRange = timeRange;
            this.totalSpots = totalSpots;
            this.availableSpots = availableSpots;
        }

        public String getSlotId() { return slotId; }
        public String getTimeRange() { return timeRange; }
        public int getTotalSpots() { return totalSpots; }
        public int getAvailableSpots() { return availableSpots; }
        public void setAvailableSpots(int spots) { this.availableSpots = spots; }
    }
}
