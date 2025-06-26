package com.example.adrianrodriguezweighttrackingapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddWeightActivity extends AppCompatActivity {

    private EditText editTextDate;
    private EditText editTextWeight;
    private EditText editTextNotes;
    private Button buttonSave;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_weight_activity);

        // Initialize the views
        editTextDate = findViewById(R.id.editTextDate);
        editTextWeight = findViewById(R.id.editTextWeight);
        editTextNotes = findViewById(R.id.editTextNotes);
        buttonSave = findViewById(R.id.buttonSave);

        // Initialize the DatabaseHelper
        databaseHelper = new DatabaseHelper(this);

        // Set OnClickListener for the Save button
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveWeightData();
            }
        });
    }

    private void saveWeightData() {
        // Retrieve user inputs
        String date = editTextDate.getText().toString().trim();
        String weight = editTextWeight.getText().toString().trim();
        String notes = editTextNotes.getText().toString().trim();

        // Validate input
        if (date.isEmpty() || weight.isEmpty()) {
            Toast.makeText(this, "Please enter both date and weight.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert weight to float
        float weightValue;
        try {
            weightValue = Float.parseFloat(weight);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid weight value.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save data to database
        WeightData newWeightData = new WeightData(0, date, weightValue, notes);
        boolean isInserted = databaseHelper.addWeightData(date, weightValue, notes);


        if (isInserted) {
            Toast.makeText(this, "Weight data added successfully.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity and return to the previous screen
        } else {
            Toast.makeText(this, "Failed to add weight data.", Toast.LENGTH_SHORT).show();
        }
    }
}

