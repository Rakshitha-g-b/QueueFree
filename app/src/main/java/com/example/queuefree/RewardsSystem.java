package com.example.queuefree;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

/**
 * RewardsSystem - Gamification with points and badges
 * Users earn points for using the app and arriving on time
 */
public class RewardsSystem {

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    public RewardsSystem() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Award points to user
     */
    public void awardPoints(int points, String reason) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        databaseReference.child("users").child(userId).child("points")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long currentPoints = snapshot.getValue(Long.class);
                        if (currentPoints == null) currentPoints = 0L;

                        long newPoints = currentPoints + points;
                        databaseReference.child("users").child(userId).child("points")
                                .setValue(newPoints);

                        // Log the reward
                        String rewardId = databaseReference.child("users").child(userId)
                                .child("rewards_history").push().getKey();
                        if (rewardId != null) {
                            databaseReference.child("users").child(userId)
                                    .child("rewards_history").child(rewardId)
                                    .child("points").setValue(points);
                            databaseReference.child("users").child(userId)
                                    .child("rewards_history").child(rewardId)
                                    .child("reason").setValue(reason);
                            databaseReference.child("users").child(userId)
                                    .child("rewards_history").child(rewardId)
                                    .child("timestamp").setValue(System.currentTimeMillis());
                        }

                        checkBadges(newPoints, userId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    /**
     * Check and award badges based on points
     */
    private void checkBadges(long points, String userId) {
        String badge = null;

        if (points >= 1000) {
            badge = "🏆 Diamond Member";
        } else if (points >= 500) {
            badge = "💎 Gold Member";
        } else if (points >= 200) {
            badge = "🥈 Silver Member";
        } else if (points >= 50) {
            badge = "🥉 Bronze Member";
        }

        if (badge != null) {
            databaseReference.child("users").child(userId).child("badge").setValue(badge);
        }
    }

    /**
     * Get user's current points
     */
    public interface PointsCallback {
        void onPointsReceived(long points, String badge);
    }

    public void getUserPoints(PointsCallback callback) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        databaseReference.child("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long points = snapshot.child("points").getValue(Long.class);
                        String badge = snapshot.child("badge").getValue(String.class);

                        if (points == null) points = 0L;
                        if (badge == null) badge = "🆕 Newbie";

                        callback.onPointsReceived(points, badge);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onPointsReceived(0, "🆕 Newbie");
                    }
                });
    }
}
