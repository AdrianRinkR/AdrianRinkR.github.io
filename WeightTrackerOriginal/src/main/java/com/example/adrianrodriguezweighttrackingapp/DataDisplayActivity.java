package com.example.adrianrodriguezweighttrackingapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import android.content.Intent;
import com.example.adrianrodriguezweighttrackingapp.WeightData;

public class DataDisplayActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private GridLayout gridLayoutData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_display);

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(this);
        gridLayoutData = findViewById(R.id.gridLayoutData);

        // Load data from database
        loadWeightData();

        // Initialize the Add Entry button
        Button buttonAddData = findViewById(R.id.buttonAddData);

        // Set an OnClickListener for the Add Entry button
        buttonAddData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle the button click here (e.g., open a new activity or show a dialog)
                Intent intent = new Intent(DataDisplayActivity.this, AddWeightActivity.class);
                startActivity(intent);
            }
        });
    }


    private void loadWeightData() {
        List<WeightData> weightDataList = databaseHelper.getAllWeightData();

        for (WeightData data : weightDataList) {
            // Create TextViews for each entry and add to GridLayout
            TextView dateView = new TextView(this);
            dateView.setText(data.getDate());
            gridLayoutData.addView(dateView);

            TextView weightView = new TextView(this);
            weightView.setText(String.valueOf(data.getWeight()) + " lbs");
            gridLayoutData.addView(weightView);

            TextView notesView = new TextView(this);
            notesView.setText(data.getNotes());
            gridLayoutData.addView(notesView);

            Button deleteButton = new Button(this);
            deleteButton.setText("Delete");
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Delete the weight entry from the database
                    boolean isDeleted = databaseHelper.deleteWeightData(data.getId());

                    if (isDeleted) {
                        // Refresh the grid to reflect the changes
                        gridLayoutData.removeAllViews();
                        loadWeightData();
                    }
                }
            });
            gridLayoutData.addView(deleteButton);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear the existing data to prevent duplicates
        gridLayoutData.removeAllViews();
        // Reload data from the database
        loadWeightData();
    }

}



