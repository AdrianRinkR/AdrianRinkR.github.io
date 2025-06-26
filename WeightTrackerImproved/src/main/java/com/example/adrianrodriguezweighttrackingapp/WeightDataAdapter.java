package com.example.adrianrodriguezweighttrackingapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull; // Annotation for indicating non-null parameters/returns
import androidx.recyclerview.widget.RecyclerView; // Core RecyclerView components

import java.util.List;
import java.util.Locale; // Used for consistent number formatting

/**
 * WeightDataAdapter is a custom RecyclerView.Adapter that displays a list of
 * WeightData objects. Each item in the list represents a single
 * entry and its associated user input data. It provides functionality to update
 * the displayed data and handle delete actions for individual items.
 */
public class WeightDataAdapter extends RecyclerView.Adapter<WeightDataAdapter.WeightViewHolder> {

    // The list of WeightData objects to be displayed in the RecyclerView.
    private List<WeightData> weightList;
    // Listener for delete button clicks on individual items.
    private OnItemDeleteListener deleteListener;

    /**
     * Interface definition for a callback when an item's delete button is clicked.
     */
    public interface OnItemDeleteListener {
        void onDeleteClick(String documentId);
    }

    // Constructor for the WeightDataAdapter.
    public WeightDataAdapter(List<WeightData> weightList, OnItemDeleteListener deleteListener) {
        this.weightList = weightList;
        this.deleteListener = deleteListener; // Assign the provided listener to the private field.
    }

    /**
     * Updates the data set of the adapter and notifies the RecyclerView to refresh its views
     *
     * @param newData The new list of WeightData objects to display.
     */
    public void updateData(List<WeightData> newData) {
        this.weightList.clear();     // Clear existing data.
        this.weightList.addAll(newData); // Add all new data.
        notifyDataSetChanged();      // Notify the RecyclerView that the data set has changed.
    }

    /**
     * Called when RecyclerView needs a new WeightViewHolder of the given type to represent
     * an item. This method inflates the layout for a single item.
     *
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new WeightViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public WeightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout (item_weight_data.xml) to create a new View.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weight_data, parent, false);
        return new WeightViewHolder(view); // Return a new ViewHolder instance.
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the WeightViewHolder#itemView to reflect
     * the item at the given position in the weightList
     */
    @Override
    public void onBindViewHolder(@NonNull WeightViewHolder holder, int position) {
        // Get the WeightData object for the current position.
        WeightData currentWeight = weightList.get(position);

        // Bind core weight data to TextViews.
        holder.textViewDate.setText("Date: " + currentWeight.getDate());
        // Format weight to one decimal place and append " lbs".
        holder.textViewWeight.setText("Weight: " + String.format(Locale.US, "%.1f", currentWeight.getWeight()) + " lbs");

        // Handle optional Notes field: show if present, hide otherwise.
        if (currentWeight.getNotes() != null && !currentWeight.getNotes().isEmpty()) {
            holder.textViewNotes.setText("Notes: " + currentWeight.getNotes());
            holder.textViewNotes.setVisibility(View.VISIBLE);
        } else {
            holder.textViewNotes.setVisibility(View.GONE); // Hide the TextView if no notes.
        }

        // Handle new optional data points (Sleep, Steps, Calories, Mood).
        // Each is checked for nullability and visibility is adjusted accordingly.

        // Hours of Sleep
        if (currentWeight.getHoursOfSleep() != null) {
            holder.textViewSleep.setText("Sleep: " + String.format(Locale.US, "%.1f", currentWeight.getHoursOfSleep()) + " hrs");
            holder.textViewSleep.setVisibility(View.VISIBLE);
        } else {
            holder.textViewSleep.setVisibility(View.GONE);
        }

        // Daily Steps
        if (currentWeight.getDailySteps() != null) {
            holder.textViewSteps.setText("Steps: " + currentWeight.getDailySteps());
            holder.textViewSteps.setVisibility(View.VISIBLE);
        } else {
            holder.textViewSteps.setVisibility(View.GONE);
        }

        // Calorie Intake
        if (currentWeight.getCalorieIntake() != null) {
            holder.textViewCalories.setText("Calories: " + currentWeight.getCalorieIntake() + " kcal");
            holder.textViewCalories.setVisibility(View.VISIBLE);
        } else {
            holder.textViewCalories.setVisibility(View.GONE);
        }

        // Mood (also check for "Select Mood" if that's a default spinner hint you want to hide)
        if (currentWeight.getMood() != null && !currentWeight.getMood().isEmpty() && !currentWeight.getMood().equals("Select Mood")) {
            holder.textViewMood.setText("Mood: " + currentWeight.getMood());
            holder.textViewMood.setVisibility(View.VISIBLE);
        } else {
            holder.textViewMood.setVisibility(View.GONE);
        }

        // Set up the click listener for the delete button.
        // When clicked, it calls the onDeleteClick method of the assigned deleteListener.
        holder.buttonDelete.setOnClickListener(v -> {
            // Ensure the listener is not null before invoking the callback.
            if (deleteListener != null) {
                // Pass the Firestore document ID associated with this item for deletion.
                deleteListener.onDeleteClick(currentWeight.getDocumentId());
            }
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items currently in the `weightList`.
     */
    @Override
    public int getItemCount() {
        return weightList.size();
    }

    /**
     * WeightViewHolder is an inner static class that holds references to all the UI
     * components (TextViews, Button) within a single item view (`item_weight_data.xml`).
     * This pattern improves performance by preventing repeated findViewById calls.
     */
    public static class WeightViewHolder extends RecyclerView.ViewHolder {
        // UI components for displaying weight entry data.
        public TextView textViewDate;
        public TextView textViewWeight;
        public TextView textViewNotes;
        public Button buttonDelete; // The button to initiate deletion of this item.

        // TextViews for the new additional health data points.
        public TextView textViewSleep;
        public TextView textViewSteps;
        public TextView textViewCalories;
        public TextView textViewMood;

        /**
         * Constructor for the ViewHolder. The root View of a single list item
         */
        public WeightViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize TextViews and Button by finding them within the itemView.
            textViewDate = itemView.findViewById(R.id.textViewItemDate);
            textViewWeight = itemView.findViewById(R.id.textViewItemWeight);
            textViewNotes = itemView.findViewById(R.id.textViewItemNotes);
            buttonDelete = itemView.findViewById(R.id.buttonItemDelete);

            // Initialize new TextViews for additional data points.
            textViewSleep = itemView.findViewById(R.id.textViewItemSleep);
            textViewSteps = itemView.findViewById(R.id.textViewItemSteps);
            textViewCalories = itemView.findViewById(R.id.textViewItemCalories);
            textViewMood = itemView.findViewById(R.id.textViewItemMood);
        }
    }
}