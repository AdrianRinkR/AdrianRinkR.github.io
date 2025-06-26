package com.example.adrianrodriguezweighttrackingapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * AddWeightActivity is responsible for allowing users to input and save their daily
 * weight entries along with additional health metrics such as sleep, steps, calories, and mood.
 * It provides a date picker for easy date selection and handles data validation before
 * persisting the information to a user-specific collection in Firebase Firestore.
 */
public class AddWeightActivity extends AppCompatActivity {

    // UI components for core weight entry fields
    private EditText editTextDate;
    private EditText editTextWeight;
    private EditText editTextNotes;
    private Button buttonSave;
    private ProgressBar progressBar;

    // UI components for additional health data points
    private EditText editTextSleep; // For hours of sleep
    private EditText editTextSteps; // For daily steps
    private EditText editTextCalories; // For calorie intake
    private Spinner spinnerMood; // For selecting mood from predefined options

    // Firebase instances for authentication and database interaction
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Date formatter for consistent date string handling
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

    /**
     * Called when the activity is first created. This is where UI components are initialized,
     * Firebase instances are set up, and listeners are attached.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data
     * it most recently supplied in onSaveInstanceState(Bundle).
     * Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_weight_activity); // Set the layout for this activity.

        // Initialize Firebase Authentication and Firestore instances.
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize core UI components by finding them in the layout.
        editTextDate = findViewById(R.id.editTextDate);
        editTextWeight = findViewById(R.id.editTextWeight);
        editTextNotes = findViewById(R.id.editTextNotes);
        buttonSave = findViewById(R.id.buttonSave);
        progressBar = findViewById(R.id.progressBar);

        // Initialize new UI components for extended health data.
        editTextSleep = findViewById(R.id.editTextSleep);
        editTextSteps = findViewById(R.id.editTextSteps);
        editTextCalories = findViewById(R.id.editTextCalories);
        spinnerMood = findViewById(R.id.spinnerMood);

        // Set up the ArrayAdapter for the Mood Spinner.
        // It populates the spinner with options from the 'mood_options' array defined in strings.xml.
        // 'android.R.layout.simple_spinner_item' is the default layout for the spinner item itself.
        // 'android.R.layout.simple_spinner_dropdown_item' is used for the dropdown list appearance.
        ArrayAdapter<CharSequence> moodAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.mood_options, // Reference to the string array resource
                android.R.layout.simple_spinner_item
        );
        moodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMood.setAdapter(moodAdapter);


        // Set an OnClickListener for the date EditText to show the DatePickerDialog.
        editTextDate.setOnClickListener(v -> showDatePickerDialog());

        // Set an OnClickListener for the Save Button to initiate the data saving process.
        buttonSave.setOnClickListener(v -> saveWeightData());
    }

    /**
     * Displays a DatePickerDialog to allow the user to select a date.
     * The selected date is then formatted and set as the text for editTextDate.
     */
    private void showDatePickerDialog() {
        // Get the current date to set as the initial date in the picker.
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create and show the DatePickerDialog.
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                // Listener for when a date is selected.
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDay); // Set the selected date to the calendar.
                    editTextDate.setText(dateFormatter.format(calendar.getTime())); // Format and display the date.
                },
                year, month, day); // Initial year, month, and day for the picker.
        datePickerDialog.show();
    }

    /**
     * Gathers all user inputs, performs validation, and saves the data to Firebase Firestore.
     * Displays a ProgressBar during the save operation and provides user feedback via Snackbar/Toast.
     */
    private void saveWeightData() {
        // Retrieve the currently authenticated Firebase user.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // If no user is logged in, inform the user and redirect to the MainActivity (login screen).
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to save data.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(AddWeightActivity.this, MainActivity.class));
            finish(); // Finish this activity to prevent going back to it.
            return;
        }

        // Get the unique user ID to store data in a user-specific collection.
        String userId = currentUser.getUid();

        // Retrieve and trim input strings from UI fields.
        String dateString = editTextDate.getText().toString().trim();
        String weightString = editTextWeight.getText().toString().trim();
        String notes = editTextNotes.getText().toString().trim();

        // Retrieve and trim input strings for new optional data points.
        String sleepString = editTextSleep.getText().toString().trim();
        String stepsString = editTextSteps.getText().toString().trim();
        String caloriesString = editTextCalories.getText().toString().trim();
        // Get the selected item from the Mood Spinner.
        String selectedMood = spinnerMood.getSelectedItem().toString();


        // --- Validation for mandatory fields (Date and Weight) ---

        // Validate Date field.
        if (dateString.isEmpty()) {
            Toast.makeText(this, "Please select a date.", Toast.LENGTH_SHORT).show();
            return;
        }

        Date date;
        try {
            date = dateFormatter.parse(dateString); // Attempt to parse the date string.
            // A null date after parsing indicates a format issue or an unparseable string.
            if (date == null) {
                Toast.makeText(this, "Invalid date format. Please use YYYY-MM-DD.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException e) {
            // Catch parsing exceptions (e.g., malformed date string).
            Toast.makeText(this, "Invalid date format. Please use YYYY-MM-DD.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate Weight field.
        if (weightString.isEmpty()) {
            Toast.makeText(this, "Please enter your weight.", Toast.LENGTH_SHORT).show();
            return;
        }

        double weightValue;
        try {
            weightValue = Double.parseDouble(weightString); // Attempt to parse weight to a double.
            // Weight must be a positive number.
            if (weightValue <= 0) {
                Toast.makeText(this, "Weight must be a positive number.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            // Catch parsing exceptions (e.g., non-numeric input for weight).
            Toast.makeText(this, "Invalid weight value.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Validation for new optional fields ---
        // These fields are initialized to null and only parsed if input is provided.

        Double hoursOfSleep = null;
        if (!sleepString.isEmpty()) {
            try {
                hoursOfSleep = Double.parseDouble(sleepString); // Parse sleep hours to a Double.
                // Basic validation: sleep hours should be within a reasonable range (0 to 24).
                if (hoursOfSleep < 0 || hoursOfSleep > 24) {
                    Toast.makeText(this, "Hours of sleep must be between 0 and 24.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid hours of sleep value.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Integer dailySteps = null;
        if (!stepsString.isEmpty()) {
            try {
                dailySteps = Integer.parseInt(stepsString); // Parse daily steps to an Integer.
                // Validation: steps cannot be negative.
                if (dailySteps < 0) {
                    Toast.makeText(this, "Daily steps cannot be negative.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid daily steps value.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Integer calorieIntake = null;
        if (!caloriesString.isEmpty()) {
            try {
                calorieIntake = Integer.parseInt(caloriesString); // Parse calorie intake to an Integer.
                // Validation: calories cannot be negative.
                if (calorieIntake < 0) {
                    Toast.makeText(this, "Calorie intake cannot be negative.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid calorie intake value.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Mood selection validation: If the first item (typically a "Select Mood" hint) is chosen,
        // treat the mood as not selected (null). This makes mood an optional field.
        if (spinnerMood.getSelectedItemPosition() == 0) {
            selectedMood = null; // Store null if the default/hint item is selected.
        }


        // Show the ProgressBar and disable the save button to indicate a background operation.
        progressBar.setVisibility(View.VISIBLE);
        buttonSave.setEnabled(false);

        // Create a new WeightData object encapsulating all the collected and validated data.
        WeightData newWeightData = new WeightData(dateString, weightValue, notes,
                hoursOfSleep, dailySteps, selectedMood, calorieIntake);

        // Define the Firestore collection path where weight entries will be stored.
        // This creates a hierarchical structure: "users/{userId}/weightEntries".
        CollectionReference weightEntriesRef = db.collection("users").document(userId).collection("weightEntries");

        // Save the new WeightData object to Firestore.
        // .add() creates a new document with an auto-generated ID within the collection.
        weightEntriesRef.add(newWeightData)
                .addOnSuccessListener(documentReference -> {
                    // Data saved successfully.
                    // Hide ProgressBar and re-enable the save button.
                    progressBar.setVisibility(View.GONE);
                    buttonSave.setEnabled(true);

                    // Clear all input fields after successful saving, preparing for a new entry.
                    editTextDate.setText("");
                    editTextWeight.setText("");
                    editTextNotes.setText("");
                    editTextSleep.setText("");
                    editTextSteps.setText("");
                    editTextCalories.setText("");
                    spinnerMood.setSelection(0); // Reset spinner to the first item (e.g., "Select Mood").

                    // Display a success message using a Snackbar for better user experience.
                    Snackbar.make(findViewById(android.R.id.content), "Weight entry saved successfully!", Snackbar.LENGTH_LONG)
                            .show();
                })
                .addOnFailureListener(e -> {
                    // Data saving failed.
                    // Hide ProgressBar and re-enable the save button.
                    progressBar.setVisibility(View.GONE);
                    buttonSave.setEnabled(true);

                    // Display an error message to the user.
                    Toast.makeText(AddWeightActivity.this, "Error adding weight data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}