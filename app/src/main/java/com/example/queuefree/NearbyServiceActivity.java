package com.example.queuefree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * NearbyServiceActivity - Shows nearest service centers
 * Uses GPS to find and display centers sorted by distance
 */
public class NearbyServiceActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 100;

    private ListView nearbyCentersListView;
    private TextView locationStatusTextView;
    private ProgressBar progressBar;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseReference;

    private List<ServiceCenter> serviceCentersList;
    private ServiceCenterAdapter adapter;

    private Location userLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_service);

        // Initialize views
        nearbyCentersListView = findViewById(R.id.nearbyCentersListView);
        locationStatusTextView = findViewById(R.id.locationStatusTextView);
        progressBar = findViewById(R.id.progressBar);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        databaseReference = FirebaseDatabase.getInstance().getReference();

        serviceCentersList = new ArrayList<>();

        // Use custom adapter for better visibility
        adapter = new ServiceCenterAdapter(this, serviceCentersList);
        nearbyCentersListView.setAdapter(adapter);

        // Check and request location permission
        checkLocationPermission();

        // List item click listener
        nearbyCentersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < serviceCentersList.size()) {
                    ServiceCenter center = serviceCentersList.get(position);
                    navigateToToken(center);
                }
            }
        });
    }

    /**
     * Check and request location permission
     */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                locationStatusTextView.setText("Location permission denied");
                locationStatusTextView.setTextColor(Color.RED);
                Toast.makeText(this, "Location permission is required to find nearby centers",
                        Toast.LENGTH_LONG).show();

                // Load centers without distance
                loadServiceCentersWithoutLocation();
            }
        }
    }

    /**
     * Get user's current location
     */
    private void getUserLocation() {
        progressBar.setVisibility(View.VISIBLE);
        locationStatusTextView.setText("Getting your location...");
        locationStatusTextView.setTextColor(Color.parseColor("#2196F3"));

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            userLocation = location;
                            locationStatusTextView.setText("✅ Location found! Showing nearby centers");
                            locationStatusTextView.setTextColor(Color.parseColor("#4CAF50"));
                            loadServiceCenters();
                        } else {
                            locationStatusTextView.setText("⚠️ Unable to get location. Showing all centers");
                            locationStatusTextView.setTextColor(Color.parseColor("#FF9800"));
                            progressBar.setVisibility(View.GONE);

                            // Load centers without distance calculation
                            loadServiceCentersWithoutLocation();
                        }
                    }
                });
    }

    /**
     * Load service centers from Firebase and calculate distance
     */
    private void loadServiceCenters() {
        databaseReference.child("service_centers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        serviceCentersList.clear();

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            // No data - add sample centers
                            createSampleCenters();
                            locationStatusTextView.setText("📍 Showing sample centers");
                            locationStatusTextView.setTextColor(Color.parseColor("#9C27B0"));
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        for (DataSnapshot centerSnapshot : snapshot.getChildren()) {
                            ServiceCenter center = centerSnapshot.getValue(ServiceCenter.class);

                            if (center != null && userLocation != null) {
                                // Calculate distance
                                float[] results = new float[1];
                                Location.distanceBetween(
                                        userLocation.getLatitude(),
                                        userLocation.getLongitude(),
                                        center.getLatitude(),
                                        center.getLongitude(),
                                        results
                                );

                                center.setDistance(results[0]);
                                serviceCentersList.add(center);
                            }
                        }

                        // Sort by distance
                        Collections.sort(serviceCentersList, new Comparator<ServiceCenter>() {
                            @Override
                            public int compare(ServiceCenter c1, ServiceCenter c2) {
                                return Float.compare(c1.getDistance(), c2.getDistance());
                            }
                        });

                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);

                        if (serviceCentersList.isEmpty()) {
                            locationStatusTextView.setText("❌ No service centers found nearby");
                            locationStatusTextView.setTextColor(Color.RED);
                        } else {
                            locationStatusTextView.setText("✅ Found " + serviceCentersList.size() + " centers nearby");
                            locationStatusTextView.setTextColor(Color.parseColor("#4CAF50"));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(NearbyServiceActivity.this,
                                "Error loading centers: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        locationStatusTextView.setText("❌ Error loading data");
                        locationStatusTextView.setTextColor(Color.RED);
                    }
                });
    }

    /**
     * Load centers without location (fallback)
     */
    private void loadServiceCentersWithoutLocation() {
        databaseReference.child("service_centers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        serviceCentersList.clear();

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            createSampleCenters();
                            return;
                        }

                        for (DataSnapshot centerSnapshot : snapshot.getChildren()) {
                            ServiceCenter center = centerSnapshot.getValue(ServiceCenter.class);
                            if (center != null) {
                                serviceCentersList.add(center);
                            }
                        }

                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);

                        locationStatusTextView.setText("📍 Showing all service centers");
                        locationStatusTextView.setTextColor(Color.parseColor("#2196F3"));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        createSampleCenters();
                    }
                });
    }

    /**
     * Create sample centers for testing
     */
    private void createSampleCenters() {
        ServiceCenter center1 = new ServiceCenter(
                "Ration Shop - Jayanagar",
                "Ration Shop",
                12.9352,
                77.6245,
                "4th Block, Jayanagar, Bangalore"
        );
        center1.setDistance(1200); // 1.2 km

        ServiceCenter center2 = new ServiceCenter(
                "Government Hospital - Koramangala",
                "Government Hospital",
                12.9279,
                77.6271,
                "Koramangala, Bangalore"
        );
        center2.setDistance(2500); // 2.5 km

        ServiceCenter center3 = new ServiceCenter(
                "Government Office - Indiranagar",
                "Government Office",
                12.9716,
                77.6412,
                "Indiranagar, Bangalore"
        );
        center3.setDistance(3800); // 3.8 km

        serviceCentersList.add(center1);
        serviceCentersList.add(center2);
        serviceCentersList.add(center3);

        adapter.notifyDataSetChanged();

        locationStatusTextView.setText("📍 Showing sample centers for demo");
        locationStatusTextView.setTextColor(Color.parseColor("#9C27B0"));
    }

    /**
     * Navigate to token activity
     */
    private void navigateToToken(ServiceCenter center) {
        Intent intent = new Intent(NearbyServiceActivity.this, TokenActivity.class);
        intent.putExtra("SERVICE_CENTER", center.getName());
        intent.putExtra("CENTER_LAT", center.getLatitude());
        intent.putExtra("CENTER_LNG", center.getLongitude());
        startActivity(intent);
    }

    /**
     * Custom adapter for service centers with enhanced visibility
     */
    private class ServiceCenterAdapter extends ArrayAdapter<ServiceCenter> {

        public ServiceCenterAdapter(Context context, List<ServiceCenter> centers) {
            super(context, 0, centers);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ServiceCenter center = getItem(position);

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(
                        android.R.layout.simple_list_item_2, parent, false);
            }

            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            if (center != null) {
                // Main title
                String title = "🏢 " + center.getName();
                text1.setText(title);
                text1.setTextSize(18);
                text1.setTextColor(Color.parseColor("#212121"));
                // ✅ CORRECT
                text1.setTypeface(null, android.graphics.Typeface.BOLD);

                // Subtitle with distance and address
                String distance = center.getDistance() > 0 ?
                        String.format("%.2f km away", center.getDistance() / 1000) :
                        "Distance unavailable";

                String subtitle = "📍 " + distance + "\n" + center.getAddress();
                text2.setText(subtitle);
                text2.setTextSize(14);
                text2.setTextColor(Color.parseColor("#757575"));
            }

            // Set padding for better visibility
            convertView.setPadding(20, 20, 20, 20);
            convertView.setBackgroundColor(Color.WHITE);

            return convertView;
        }
    }
}
