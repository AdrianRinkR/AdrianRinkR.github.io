package com.example.adrianrodriguezweighttrackingapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // For printing messages to Logcat, helpful for debugging
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull; // Helps show that a parameter shouldn't be null
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

// This screen shows all the weight entries for a user.
// It uses Firebase Firestore to get and update the data in real-time.
public class DataDisplayActivity extends AppCompatActivity implements WeightDataAdapter.OnItemDeleteListener {

    // A tag for logging messages, makes it easier to find our messages in Logcat
    private static final String TAG = "DataDisplayActivity";

    // UI parts of our screen
    private RecyclerView recyclerViewWeightData; // Shows a scrollable list of weight entries
    private WeightDataAdapter weightDataAdapter; // Helps put our weight data into the RecyclerView
    private TextView textViewNoDataMessage; // Shows a message if there's no data
    private Button buttonViewCharts; // Button to go to the charts screen

    // Firebase connections
    private FirebaseAuth mAuth; // For checking who's logged in
    private FirebaseFirestore db; // Our database connection
    private CollectionReference weightEntriesRef; // Points to where our weight data is stored for the current user
    private ListenerRegistration weightDataListener; // Keeps track of our real-time database listener

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_display); // Link this activity to its layout file

        // Get our Firebase stuff ready
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if a user is logged in. If not, send them back to the login screen.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to log in first!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(DataDisplayActivity.this, MainActivity.class));
            finish(); // Close this screen so they can't come back without logging in
            return;
        }

        // Set up where we'll store weight entries in Firestore for THIS user
        weightEntriesRef = db.collection("users")
                .document(currentUser.getUid())
                .collection("weightEntries");

        // Find our UI elements from the layout
        recyclerViewWeightData = findViewById(R.id.recyclerViewWeightData);
        textViewNoDataMessage = findViewById(R.id.textViewNoDataMessage);

        // Tell the RecyclerView how to arrange its items (like a list, top to bottom)
        recyclerViewWeightData.setLayoutManager(new LinearLayoutManager(this));
        // Make an adapter to handle our list data and tell it to use "this" activity for deletions
        weightDataAdapter = new WeightDataAdapter(new ArrayList<>(), this);
        recyclerViewWeightData.setAdapter(weightDataAdapter);

        // --- Set up what happens when buttons are clicked ---

        // Button to add new data
        Button buttonAddData = findViewById(R.id.buttonAddData);
        buttonAddData.setOnClickListener(v -> {
            Intent intent = new Intent(DataDisplayActivity.this, AddWeightActivity.class);
            startActivity(intent); // Go to the add weight screen
        });

        // Button to see charts
        buttonViewCharts = findViewById(R.id.buttonViewCharts);
        buttonViewCharts.setOnClickListener(v -> {
            Intent intent = new Intent(DataDisplayActivity.this, WeightChartActivity.class);
            startActivity(intent); // Go to the charts screen
        });

        // Button to log out
        Button buttonLogout = findViewById(R.id.buttonLogout);
        if (buttonLogout != null) { // Just a quick check to make sure the button exists
            buttonLogout.setOnClickListener(v -> {
                mAuth.signOut(); // Log the user out of Firebase
                Toast.makeText(DataDisplayActivity.this, "You've been logged out.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(DataDisplayActivity.this, MainActivity.class)); // Go back to login
                finish(); // Close this activity
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Double-check if the user is still logged in when this screen shows up again
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Looks like you were logged out, please sign in.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(DataDisplayActivity.this, MainActivity.class));
            finish();
            return;
        }

        // Make sure our database reference is pointing to the right user's data
        if (weightEntriesRef == null || !weightEntriesRef.getPath().contains(currentUser.getUid())) {
            weightEntriesRef = db.collection("users").document(currentUser.getUid()).collection("weightEntries");
        }

        // Start listening for weight data updates
        loadWeightData();
    }


    @Override
    protected void onStop() {
        super.onStop();
        // When this screen is not visible, stop listening for database changes.
        // This saves battery and prevents errors.
        if (weightDataListener != null) {
            weightDataListener.remove(); // Stop the listener
            Log.d(TAG, "Stopped listening for data.");
        }
    }

    // Sets up a real-time listener for the user's weight entries.
    // This function runs whenever the data changes in the database.
    private void loadWeightData() {
        weightDataListener = weightEntriesRef.orderBy("date", Query.Direction.DESCENDING) // Sort by date, newest first
                .addSnapshotListener((snapshots, e) -> {
                    // If there's an error getting data
                    if (e != null) {
                        Log.e(TAG, "Problem getting weight data:", e);
                        Toast.makeText(DataDisplayActivity.this, "Error loading data.", Toast.LENGTH_LONG).show();
                        textViewNoDataMessage.setText("Couldn't load data. Try again later!");
                        textViewNoDataMessage.setVisibility(View.VISIBLE);
                        recyclerViewWeightData.setVisibility(View.GONE);
                        return;
                    }

                    // If we got some data back
                    if (snapshots != null && !snapshots.isEmpty()) {
                        Log.d(TAG, "Got " + snapshots.size() + " weight entries.");
                        List<WeightData> updatedList = new ArrayList<>();
                        // Go through each weight entry we got
                        for (QueryDocumentSnapshot doc : snapshots) {
                            // Turn the database entry into our WeightData object
                            WeightData data = doc.toObject(WeightData.class);
                            data.setDocumentId(doc.getId()); // Save the document ID so we can delete it later
                            updatedList.add(data);
                        }
                        weightDataAdapter.updateData(updatedList); // Update the list shown on screen

                        // Show the list, hide the "no data" message
                        textViewNoDataMessage.setVisibility(View.GONE);
                        recyclerViewWeightData.setVisibility(View.VISIBLE);
                    } else {
                        // If there's no data
                        Log.d(TAG, "No weight data for this user yet.");
                        weightDataAdapter.updateData(new ArrayList<>()); // Clear the list on screen
                        // Show the "no data" message, hide the list
                        textViewNoDataMessage.setText("No weight data recorded yet. Tap '+' to add your first entry!");
                        textViewNoDataMessage.setVisibility(View.VISIBLE);
                        recyclerViewWeightData.setVisibility(View.GONE);
                    }
                });
    }

    // This gets called by the adapter when the delete button next to a weight entry is clicked
    @Override
    public void onDeleteClick(@NonNull String documentId) {
        Log.d(TAG, "Delete button clicked for ID: " + documentId);

        // Make sure the ID isn't empty
        if (documentId.isEmpty()) {
            Toast.makeText(DataDisplayActivity.this, "Can't delete: missing ID.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Tried to delete with an empty document ID.");
            return;
        }

        // Go ahead and try to delete it from Firebase
        deleteWeightData(documentId);
    }

    // Deletes a weight entry from Firebase
    private void deleteWeightData(@NonNull String documentId) {
        // Just make sure someone is still logged in before trying to delete
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(DataDisplayActivity.this, "Error: Not logged in. Cannot delete.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Tried to delete but no user logged in.");
            return;
        }

        // Get the right place in the database to delete from
        CollectionReference userWeightEntriesRef = db.collection("users")
                .document(currentUser.getUid())
                .collection("weightEntries");

        Log.d(TAG, "Trying to delete document ID: " + documentId);

        // Tell Firebase to delete the document
        userWeightEntriesRef.document(documentId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Success! The screen will update by itself because of the real-time listener.
                    Toast.makeText(DataDisplayActivity.this, "Entry deleted!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Document deleted successfully: " + documentId);
                })
                .addOnFailureListener(e -> {
                    // Uh oh, something went wrong
                    Toast.makeText(DataDisplayActivity.this, "Error deleting entry: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to delete document " + documentId + ": " + e.getMessage(), e);
                });
    }
}
