package com.example.adrianrodriguezweighttrackingapp;

import com.google.firebase.firestore.Exclude; // Import for Firestore exclusion annotation

/**
 * WeightData is a Plain Old Java Object (POJO) that represents a single
 * weight entry along with associated health metrics. This class is designed
 * to be directly mapped to and from Firebase Firestore documents.
 *
 * It includes fields for date, weight, notes, and optional additional metrics
 * like hours of sleep, daily steps, mood, and calorie intake. The `documentId`
 * field is used internally by the application to reference the corresponding
 * Firestore document for operations like updates or deletions, and is excluded
 * from direct serialization to Firestore.
 */
public class WeightData {

    // Original weight tracking fields
    private String date;        // The date of the weight entry
    private double weight;      // The recorded weight.
    private String notes;       // Optional notes for the entry.

    // Additional health metrics
    private Double hoursOfSleep;    // Hours of sleep for the day.
    private Integer dailySteps;     // Number of steps taken for the day.
    private String mood;            // The user's mood for the day.
    private Integer calorieIntake;  // Estimated calorie intake for the day.

    // Internal field to store the Firestore document ID.
    private String documentId;

    /**
     * Required public no-argument constructor for Firebase Firestore.
     * Firestore needs this constructor to automatically convert a document
     * snapshot into a WeightData object using DataSnapshot.getValue(WeightData.class).
     */
    public WeightData() {
        // Default constructor is intentionally empty.
    }

    /**
     * Full constructor for creating new WeightData objects with all fields.
     *
     * @param date The date of the weight entry in "yyyy-MM-dd" format.
     * @param weight The recorded weight as a double.
     * @param notes Optional string for any additional notes.
     * @param hoursOfSleep Optional: Hours of sleep as a Double (can be null).
     * @param dailySteps Optional: Number of daily steps as an Integer (can be null).
     * @param mood Optional: User's mood as a String (can be null).
     * @param calorieIntake Optional: Calorie intake as an Integer (can be null).
     */
    public WeightData(String date, double weight, String notes,
                      Double hoursOfSleep, Integer dailySteps, String mood, Integer calorieIntake) {
        this.date = date;
        this.weight = weight;
        this.notes = notes;
        this.hoursOfSleep = hoursOfSleep;
        this.dailySteps = dailySteps;
        this.mood = mood;
        this.calorieIntake = calorieIntake;
        // documentId is intentionally not set here
        // documentId is assigned after retrieval from Firestore.
    }

    // --- Getters for all fields ---
    // Firestore uses these getters to retrieve data when converting this object to a document.

    public String getDate() {
        return date;
    }

    public double getWeight() {
        return weight;
    }

    public String getNotes() {
        return notes;
    }

    public Double getHoursOfSleep() {
        return hoursOfSleep;
    }

    public Integer getDailySteps() {
        return dailySteps;
    }

    public String getMood() {
        return mood;
    }

    public Integer getCalorieIntake() {
        return calorieIntake;
    }

    /**
     * Retrieves the Firestore document ID associated with this object.
     *
     * `@Exclude` annotation tells Firestore to ignore this method
     * when saving or loading data. The `documentId` is managed by the application
     * after data retrieval, not as part of the document's direct data.
     */
    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    // Setters for all fields

    public void setDate(String date) {
        this.date = date;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setHoursOfSleep(Double hoursOfSleep) {
        this.hoursOfSleep = hoursOfSleep;
    }

    public void setDailySteps(Integer dailySteps) {
        this.dailySteps = dailySteps;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public void setCalorieIntake(Integer calorieIntake) {
        this.calorieIntake = calorieIntake;
    }

    /**
     * Sets the Firestore document ID for this object. This setter is called to link the POJO to its
     * database record for operations
     *
     * @param documentId The unique ID of the Firestore document.
     */
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}