package com.example.adrianrodriguezweighttrackingapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull; // Added for clarity with method parameters
import androidx.appcompat.app.AppCompatActivity;

// MPAndroidChart imports
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;

// Firebase imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch; // Required for batch writes (e.g., test data generation)

// Java Date and Time API imports
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random; // Required for generating random test data
import java.util.stream.Collectors;

/**
 * WeightChartActivity is responsible for visualizing a user's weight data over time using
 * line charts. It allows users to filter the data by week, month, or year,
 * navigate forward and backwards, and generate simulated test data for demonstration purposes.
 * Data is fetched from Firebase Firestore and displayed using the MPAndroidChart library.
 */
public class WeightChartActivity extends AppCompatActivity {

    // TAG for logging messages, useful for debugging.
    private static final String TAG = "WeightChartActivity";

    // UI Elements
    private MaterialButtonToggleGroup filterToggleGroup; // Button group for selecting WEEK, MONTH, YEAR filter.
    private ImageButton btnPrevious; // Button to navigate to the previous period.
    private ImageButton btnNext;     // Button to navigate to the next period.
    private TextView tvCurrentPeriod; // Displays the currently selected time period (e.g., "Jan 2024").
    private LineChart weightLineChart; // The chart view where weight trends are displayed.
    private Button btnGenerateTestData; // Button to trigger test data generation and upload.

    // Firebase instances
    private FirebaseFirestore db;             // Firestore database instance.
    private FirebaseAuth mAuth;               // Firebase Authentication instance.
    private FirebaseUser currentUser;         // The currently authenticated user.
    private CollectionReference weightEntriesRef; // Reference to the current user's weight entries collection in Firestore.

    // Chart State Variables
    // Enum to define the different filtering modes for the chart.
    private enum FilterType { WEEK, MONTH, YEAR }
    private FilterType currentFilter = FilterType.WEEK; // Default filter type upon activity launch.
    private LocalDate currentDate; // An anchor date representing the period being displayed (e.g., a day in the current week/month/year).

    // Date Formatters for consistent date string parsing and display.
    // Used for database storage and retrieval (yyyy-MM-dd).
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

     // Called when the activity is first created. This method initializes UI components,
     // Firebase instances, sets up chart configuration, and attaches all event listeners.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_chart); // Set the layout for this activity.

        // 1. Initialize UI Elements by finding them in the layout.
        filterToggleGroup = findViewById(R.id.filterToggleGroup);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        tvCurrentPeriod = findViewById(R.id.tvCurrentPeriod);
        weightLineChart = findViewById(R.id.weightLineChart);
        btnGenerateTestData = findViewById(R.id.btnGenerateTestData); // Initialize the test data button.

        // 2. Initialize Firebase instances.
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Check if a user is logged in. If not, display a toast and close the activity.
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in. Please log in to view charts.", Toast.LENGTH_LONG).show();
            finish(); // Close this activity as it requires a logged-in user.
            return;
        }

        // Initialize Firestore collection reference for the current user's weight entries.
        weightEntriesRef = db.collection("users")
                .document(currentUser.getUid())
                .collection("weightEntries");

        // 3. Initialize Chart State.
        currentDate = LocalDate.now(); // Set the anchor date to today's date initially.

        // 4. Set up UI Listeners for interactions.
        setupFilterListeners();      // For filter buttons (Week, Month, Year).
        setupNavigationListeners();  // For previous/next period buttons.
        setupTestDataButtonListener(); // For the test data generation button.

        // 5. Initialize chart configuration and load data for the default filter (WEEK).
        setupChart();
        loadWeightData(); // This will fetch data based on `currentFilter` and `currentDate`.
    }

    // Sets up listeners for the filter toggle group (Week, Month, Year buttons).
    private void setupFilterListeners() {
        filterToggleGroup.addOnButtonCheckedListener((toggleGroup, checkedId, isChecked) -> {
            if (isChecked) { // Only act when a button is selected (checked).
                if (checkedId == R.id.btnFilterWeek) {
                    currentFilter = FilterType.WEEK;
                } else if (checkedId == R.id.btnFilterMonth) {
                    currentFilter = FilterType.MONTH;
                } else if (checkedId == R.id.btnFilterYear) {
                    currentFilter = FilterType.YEAR;
                }
                currentDate = LocalDate.now(); // Reset the anchor date to the current date for the new filter.
                loadWeightData(); // Reload data for the newly selected period.
            }
        });
    }

     // Sets up listeners for the previous and next period navigation buttons.

    private void setupNavigationListeners() {
        btnPrevious.setOnClickListener(v -> {
            navigatePeriod(-1); // Navigate to the previous period.
        });

        btnNext.setOnClickListener(v -> {
            navigatePeriod(1); // Navigate to the next period.
        });
    }

    // Sets up the listener for the button that will generate our test data
    private void setupTestDataButtonListener() {
        btnGenerateTestData.setOnClickListener(v -> generateAndUploadTestData());
    }


     // Navigates the current displayed time period (week, month, or year) backward or forward.
     // The `currentDate` is adjusted based on the currentFilter and direction
    private void navigatePeriod(int direction) {
        switch (currentFilter) {
            case WEEK:
                currentDate = currentDate.plusWeeks(direction); // Move by weeks.
                break;
            case MONTH:
                currentDate = currentDate.plusMonths(direction); // Move by months.
                break;
            case YEAR:
                currentDate = currentDate.plusYears(direction); // Move by years.
                break;
        }
        loadWeightData(); // Reload the chart data for the new period.
    }

    /**
     * Configures the basic appearance and interactivity settings for the LineChart.
     * This includes description text, touch gestures, grid lines, and axis styling.
     * Note: Y-axis min/max are set dynamically based on data in `processAndDisplayChart`.
     */
    private void setupChart() {
        Description description = new Description();
        description.setText("Weight Tracker"); // Set chart description.
        weightLineChart.setDescription(description);
        weightLineChart.setNoDataText("No weight data available for this period."); // Text displayed when no data.
        weightLineChart.setTouchEnabled(true);  // Enable touch interactions.
        weightLineChart.setDragEnabled(true);   // Enable dragging (panning).
        weightLineChart.setScaleEnabled(true);  // Enable scaling (zooming).
        weightLineChart.setPinchZoom(true);     // Enable pinch-to-zoom.
        weightLineChart.setExtraOffsets(5f, 10f, 5f, 10f); // Add extra space around the chart.

        // Configure X-Axis
        XAxis xAxis = weightLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // Place X-axis labels at the bottom.
        xAxis.setDrawGridLines(false); // Do not draw vertical grid lines.
        xAxis.setGranularity(1f); // Minimum interval between values on the axis.
        xAxis.setLabelRotationAngle(-45); // Rotate labels for better readability if they overlap.
        xAxis.setTextColor(getResources().getColor(R.color.light_grey, null)); // Set X-axis label color.
        xAxis.setTextSize(12f); // Set X-axis label text size.

        // Configure Y-Axis (left axis, typically for weight values).
        weightLineChart.getAxisLeft().setDrawGridLines(true); // Draw horizontal grid lines.
        weightLineChart.getAxisRight().setEnabled(false); // Disable the right Y-axis.
        weightLineChart.getLegend().setEnabled(true); // Enable the legend (e.g., "Weight").
        weightLineChart.getAxisLeft().setTextColor(getResources().getColor(R.color.light_grey, null)); // Set Left Y-axis label color.
        weightLineChart.getAxisLeft().setTextSize(12f); // Set Left Y-axis label text size.
        weightLineChart.getAxisLeft().setGridColor(getResources().getColor(R.color.grey_700, null)); // Set grid line color for contrast.

        // Style the chart legend text.
        weightLineChart.getLegend().setTextColor(getResources().getColor(R.color.light_grey, null));
    }

    /**
     * Determines the start and end dates for the currently selected filter period (WEEK, MONTH, YEAR).
     * It then fetches the corresponding weight data from Firebase Firestore and initiates chart display.
     */
    private void loadWeightData() {
        LocalDate startDate;
        LocalDate endDate;
        String periodText; // Text to display in tvCurrentPeriod (e.g., "Jan 01 - Jan 07, 2024").

        // Calculate the start and end dates based on the current filter type.
        switch (currentFilter) {
            case WEEK:
                // Start of the week (Monday) and end of the week (Sunday).
                startDate = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                endDate = startDate.plusDays(6);
                periodText = startDate.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " +
                        endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")); // e.g., "Jan 01 - Jan 07, 2024"
                break;
            case MONTH:
                // First day of the month and last day of the month.
                startDate = currentDate.with(TemporalAdjusters.firstDayOfMonth());
                endDate = currentDate.with(TemporalAdjusters.lastDayOfMonth());
                periodText = currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")); // e.g., "January 2024"
                break;
            case YEAR:
                // First day of the year and last day of the year.
                startDate = currentDate.with(TemporalAdjusters.firstDayOfYear());
                endDate = currentDate.with(TemporalAdjusters.lastDayOfYear());
                periodText = currentDate.format(DateTimeFormatter.ofPattern("yyyy")); // e.g., "2024"
                break;
            default:
                // Fallback case (should not be reached if filter types are handled).
                startDate = LocalDate.now();
                endDate = LocalDate.now();
                periodText = "Unknown Period";
                break;
        }

        tvCurrentPeriod.setText(periodText); // Update the TextView with the current period string.

        // Format dates to strings for Firestore queries (which are string-based on "date" field).
        String startDateString = startDate.format(dateFormatter);
        String endDateString = endDate.format(dateFormatter);

        Log.d(TAG, "Fetching data for " + currentFilter.name() + ": " + startDateString + " to " + endDateString);

        // Fetch data from Firestore.
        // Query for documents where the 'date' field is within the calculated period.
        weightEntriesRef.whereGreaterThanOrEqualTo("date", startDateString)
                .whereLessThanOrEqualTo("date", endDateString)
                .orderBy("date", Query.Direction.ASCENDING) // Order results by date for chronological display.
                .get() // Perform a one-time fetch (not a real-time listener like DataDisplayActivity).
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<WeightData> rawWeightData = new ArrayList<>();
                        // Convert each fetched Firestore document to a WeightData object.
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            WeightData weightData = document.toObject(WeightData.class);
                            rawWeightData.add(weightData);
                        }
                        Log.d(TAG, "Fetched " + rawWeightData.size() + " documents.");
                        // Process the raw data and display it on the chart.
                        processAndDisplayChart(rawWeightData, startDate, endDate);
                    } else {
                        // Handle errors during data fetching.
                        Log.w(TAG, "Error getting documents: ", task.getException());
                        Toast.makeText(WeightChartActivity.this, "Error loading data for chart.", Toast.LENGTH_SHORT).show();
                        // Clear and invalidate the chart to show no data and an error message.
                        weightLineChart.clear();
                        weightLineChart.invalidate();
                        weightLineChart.setNoDataText("Failed to load data.");
                    }
                });
    }

    /**
     * Processes the raw weight data based on the current filter type, aggregates it if necessary,
     * populates the chart entries and X-axis labels, and dynamically adjusts the Y-axis range.
     *
     * @param rawWeightData The list of WeightData objects fetched from Firestore for the period.
     * @param periodStartDate The actual start date of the current display period (used for context).
     * @param periodEndDate The actual end date of the current display period (used for context).
     */
    private void processAndDisplayChart(List<WeightData> rawWeightData, @NonNull LocalDate periodStartDate, @NonNull LocalDate periodEndDate) {
        ArrayList<Entry> entries = new ArrayList<>(); // Chart entries (X, Y values).
        ArrayList<String> xAxisLabels = new ArrayList<>(); // Labels for the X-axis.

        // Sort the raw data by date to ensure chronological processing, essential for charting.
        rawWeightData.sort(Comparator.comparing(WeightData::getDate));

        // If no data is available for the period, clear the chart and display "No data" message.
        if (rawWeightData.isEmpty()) {
            weightLineChart.clear();
            weightLineChart.invalidate();
            weightLineChart.setNoDataText("No weight data available for this period.");
            // Reset Y-axis limits to their defaults when no data is present.
            weightLineChart.getAxisLeft().resetAxisMinimum();
            weightLineChart.getAxisLeft().resetAxisMaximum();
            return;
        }

        // Initialize min/max weight for dynamic Y-axis scaling.
        double minWeight = Double.MAX_VALUE;
        double maxWeight = Double.MIN_VALUE;

        // Process data based on the current filter type (WEEK, MONTH, YEAR).
        switch (currentFilter) {
            case WEEK:
                // For 'WEEK' view, we want to show each day in the week, even if no data is present for it.
                LocalDate currentDay = periodStartDate;
                int xIndex = 0; // X-axis index for chart entries.

                while (!currentDay.isAfter(periodEndDate)) {
                    final LocalDate finalCurrentDay = currentDay; // Need final variable for lambda.
                    // Find if there's a weight entry for the current day.
                    WeightData dailyData = rawWeightData.stream()
                            .filter(data -> {
                                try {
                                    return finalCurrentDay.isEqual(LocalDate.parse(data.getDate(), dateFormatter));
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing date for WEEK filter: " + data.getDate(), e);
                                    return false;
                                }
                            })
                            .findFirst() // Get the first matching entry (assuming one entry per day).
                            .orElse(null);

                    if (dailyData != null) {
                        float weight = (float) dailyData.getWeight();
                        entries.add(new Entry(xIndex, weight)); // Add entry to chart.
                        minWeight = Math.min(minWeight, weight); // Update min/max for Y-axis scaling.
                        maxWeight = Math.max(maxWeight, weight);
                    }
                    // Add X-axis label for each day of the week (e.g., "Mon\nJan 01").
                    xAxisLabels.add(currentDay.format(DateTimeFormatter.ofPattern("EEE\nMMM dd")));
                    currentDay = currentDay.plusDays(1); // Move to the next day.
                    xIndex++;
                }
                break;

            case MONTH:
                // For 'MONTH' view, aggregate data by week (within the displayed month) and show average weight.
                Map<Integer, List<WeightData>> dataByWeek = new HashMap<>();
                for (WeightData data : rawWeightData) {
                    try {
                        LocalDate date = LocalDate.parse(data.getDate(), dateFormatter);
                        // Calculate week number relative to the first day of the current month.
                        // This approximates "Week X" within the month.
                        int weekOfMonth = (int) (date.getDayOfYear() - periodStartDate.getDayOfYear()) / 7;
                        dataByWeek.computeIfAbsent(weekOfMonth, k -> new ArrayList<>()).add(data);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing date in MONTH filter: " + data.getDate(), e);
                    }
                }

                // Get sorted list of unique week indices that have data.
                List<Integer> sortedWeekIndices = dataByWeek.keySet().stream().sorted().collect(Collectors.toList());

                xIndex = 0; // Reset X-axis index for sequential numbering of weeks.
                for (Integer weekIdx : sortedWeekIndices) {
                    List<WeightData> weekData = dataByWeek.get(weekIdx);
                    if (!weekData.isEmpty()) {
                        // Calculate the average weight for the week.
                        double averageWeight = weekData.stream()
                                .mapToDouble(WeightData::getWeight)
                                .average()
                                .orElse(0.0); // Default to 0.0 if no weight data for the week (shouldn't happen here).
                        float avgWeightFloat = (float) averageWeight;
                        entries.add(new Entry(xIndex, avgWeightFloat));
                        minWeight = Math.min(minWeight, avgWeightFloat);
                        maxWeight = Math.max(maxWeight, avgWeightFloat);
                        xAxisLabels.add("Week " + (xIndex + 1)); // Label as "Week 1", "Week 2", etc.
                    }
                    xIndex++; // Increment for the next available week.
                }

                // If no entries are created after processing, clear the chart.
                if (entries.isEmpty()) {
                    weightLineChart.clear();
                    weightLineChart.invalidate();
                    weightLineChart.setNoDataText("No weight data available for this month.");
                    weightLineChart.getAxisLeft().resetAxisMinimum();
                    weightLineChart.getAxisLeft().resetAxisMaximum();
                    return;
                }
                break;

            case YEAR:
                // For 'YEAR' view, aggregate data by month and show average weight.
                Map<Integer, List<WeightData>> dataByMonth = new HashMap<>();
                for (WeightData data : rawWeightData) {
                    try {
                        LocalDate date = LocalDate.parse(data.getDate(), dateFormatter);
                        int month = date.getMonthValue(); // Get month as an integer (1-12).
                        dataByMonth.computeIfAbsent(month, k -> new ArrayList<>()).add(data);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing date in YEAR filter: " + data.getDate(), e);
                    }
                }

                // Iterate through all 12 months (from 1 to 12) to ensure all months are considered for labels.
                for (int i = 1; i <= 12; i++) {
                    List<WeightData> monthData = dataByMonth.getOrDefault(i, new ArrayList<>());
                    if (!monthData.isEmpty()) {
                        // Calculate average weight for the month.
                        double averageWeight = monthData.stream()
                                .mapToDouble(WeightData::getWeight)
                                .average()
                                .orElse(0.0);
                        float avgWeightFloat = (float) averageWeight;
                        // For year view, X-index corresponds to month index (0 for Jan, 11 for Dec).
                        entries.add(new Entry(i - 1, avgWeightFloat));
                        minWeight = Math.min(minWeight, avgWeightFloat);
                        maxWeight = Math.max(maxWeight, avgWeightFloat);
                        // Add month abbreviation as label (e.g., "Jan", "Feb").
                        xAxisLabels.add(LocalDate.of(periodStartDate.getYear(), i, 1).format(DateTimeFormatter.ofPattern("MMM")));
                    } else {
                        // If no data for a month, still add its label to maintain consistent X-axis spacing.
                        xAxisLabels.add(LocalDate.of(periodStartDate.getYear(), i, 1).format(DateTimeFormatter.ofPattern("MMM")));
                    }
                }

                // If no entries are created after processing, clear the chart.
                if (entries.isEmpty()) {
                    weightLineChart.clear();
                    weightLineChart.invalidate();
                    weightLineChart.setNoDataText("No weight data available for this year.");
                    weightLineChart.getAxisLeft().resetAxisMinimum();
                    weightLineChart.getAxisLeft().resetAxisMaximum();
                    return;
                }
                break;
        }

        // --- Dynamic Y-Axis Scaling ---
        // Adjust the Y-axis (left axis) limits to fit the data range with some padding.
        float yAxisPadding = 5f; // Padding above max and below min weight values.

        // Handle case where minWeight or maxWeight might not have been updated (e.g., if only one data point).
        if (minWeight == Double.MAX_VALUE || maxWeight == Double.MIN_VALUE) {
            weightLineChart.getAxisLeft().resetAxisMinimum(); // Reset to auto-scaling
            weightLineChart.getAxisLeft().resetAxisMaximum(); // Reset to auto-scaling
        } else {
            float lowestY = (float) minWeight - yAxisPadding;
            float highestY = (float) maxWeight + yAxisPadding;

            // Ensure the minimum Y-axis value does not go below zero, as weight cannot be negative.
            if (lowestY < 0) lowestY = 0f;

            // If the calculated range is very small
            if (highestY - lowestY < 10f) {
                float mid = (lowestY + highestY) / 2;
                lowestY = Math.max(0f, mid - 5f); // Ensure it doesn't go negative.
                highestY = mid + 5f;
            }

            // Apply the calculated min/max values to the Y-axis.
            weightLineChart.getAxisLeft().setAxisMinimum(lowestY);
            weightLineChart.getAxisLeft().setAxisMaximum(highestY);
            // Suggest a preferred number of labels, allowing the chart library to adjust.
            weightLineChart.getAxisLeft().setLabelCount(5, true);
        }

        // --- Chart Data Set and Display Logic ---
        // Create a LineDataSet with the prepared entries.
        LineDataSet dataSet = new LineDataSet(entries, "Weight"); // Label for the legend.
        dataSet.setColor(getResources().getColor(R.color.chartLineColor, null)); // Set line color from resources.
        dataSet.setValueTextColor(getResources().getColor(android.R.color.white, null)); // Set value text color.
        dataSet.setCircleColor(getResources().getColor(R.color.chartCircleColor, null)); // Set circle color for data points.
        dataSet.setCircleRadius(4f); // Set radius of circles at data points.
        dataSet.setValueTextSize(10f); // Set text size of values on data points.
        dataSet.setMode(LineDataSet.Mode.LINEAR); // Draw lines as linear segments.
        dataSet.setLineWidth(2f); // Set line thickness.
        dataSet.setDrawValues(true); // Draw numerical values on the chart points.

        // Create a LineData object from the data set.
        LineData lineData = new LineData(dataSet);
        weightLineChart.setData(lineData); // Set the data to the chart.

        // Set the custom X-axis labels using IndexAxisValueFormatter.
        weightLineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
        // Set label count for X-axis to match the number of labels, ensuring all labels are displayed.
        weightLineChart.getXAxis().setLabelCount(xAxisLabels.size(), true);

        // Animate the chart to make the loading visually appealing.
        weightLineChart.animateX(500); // Animate X-axis values over 500 milliseconds.

        // Notify the chart that its data has changed and needs to be redrawn.
        weightLineChart.notifyDataSetChanged();
        weightLineChart.invalidate();
    }


    /**
     * Initiates the process of generating and uploading simulated weight and health data
     * to the current user's Firestore collection.
     * This method is typically used for populating an empty database for demonstration.
     */
    private void generateAndUploadTestData() {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in. Cannot generate test data.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the specific Firestore collection reference for the current user.
        CollectionReference userWeightEntriesRef = db.collection("users")
                .document(currentUser.getUid())
                .collection("weightEntries");

        // Call the helper method to add the generated data using a batch write.
        addGeneratedData(userWeightEntriesRef);
    }

    // This helper function makes up a year's worth of test data and saves it to Firestore in big batches.
    private void addGeneratedData(@NonNull CollectionReference collectionRef) {
        // We want data for THIS year, starting from January 1st.
        LocalDate startDate = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        // Let's get the exact number of days in this year (it handles leap years automatically!).
        int numDays = startDate.lengthOfYear();

        double initialWeightLbs = 180.0; // Starting weight for our fake data.
        double weightFluctuationLbs = 2.0; // How much weight can randomly go up or down each day.
        double trendChangeLbsPerMonth = -0.5; // We'll pretend the person loses a little weight each month.

        String[] moods = {"Happy", "Neutral", "Motivated", "Tired", "Relaxed"}; // Some mood options.
        Random random = new Random(); // A tool to make random numbers.
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // How we want dates to look (e.g., 2025-01-01).

        WriteBatch batch = db.batch(); // This helps us send a lot of data to Firestore at once, super fast!

        double currentWeight = initialWeightLbs; // This will change as we go through the year.

        for (int i = 0; i < numDays; i++) {
            LocalDate currentDate = startDate.plusDays(i); // Get the date for today in our loop.

            // Adjust the weight a tiny bit each day based on our monthly trend.
            // We divide by how many days are in the *current month* for a better trend.
            currentWeight += (trendChangeLbsPerMonth / currentDate.lengthOfMonth());

            // Add some random up-and-down to the weight to make it more realistic.
            double fluctuation = random.nextDouble() * (2 * weightFluctuationLbs) - weightFluctuationLbs;
            double dailyWeight = currentWeight + fluctuation;
            dailyWeight = Math.max(100.0, dailyWeight); // Don't let the weight go super low!

            String dateString = currentDate.format(dateFormatter); // Turn the date into a text string.

            // Generate some other health info. About 10% of the time, we'll leave them blank (null).
            Double hoursOfSleep = (random.nextDouble() > 0.1) ? (double) Math.round((random.nextDouble() * 4.0 + 5.0) * 10) / 10 : null; // Between 5.0 and 9.0 hours
            Integer dailySteps = (random.nextDouble() > 0.1) ? random.nextInt(12001) + 3000 : null; // Between 3000 and 15000 steps
            String mood = (random.nextDouble() > 0.1) ? moods[random.nextInt(moods.length)] : null; // Pick a random mood
            Integer calorieIntake = (random.nextDouble() > 0.1) ? random.nextInt(1001) + 1500 : null; // Between 1500 and 2500 calories

            // Put all this daily info into a WeightData object.
            WeightData weightData = new WeightData(
                    dateString,
                    dailyWeight,
                    "Generated entry: " + dateString, // Just a simple note.
                    hoursOfSleep,
                    dailySteps,
                    mood,
                    calorieIntake
            );

            // Add this day's data to our batch. Firestore will give it a unique ID.
            batch.set(collectionRef.document(), weightData);
        }

        // Now, send all the data to Firestore!
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // If it worked!
                    Log.d(TAG, "Yay! Test data made and uploaded for " + numDays + " days.");
                    Toast.makeText(this, "Test data generated and loaded!", Toast.LENGTH_SHORT).show();
                    loadWeightData(); // Show the new data on the chart.
                })
                .addOnFailureListener(e -> {
                    // If something went wrong...
                    Log.e(TAG, "Uh oh! Error uploading test data", e);
                    Toast.makeText(this, "Error making test data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}