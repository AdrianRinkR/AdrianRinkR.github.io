package com.example.adrianrodriguezweighttrackingapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;


public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "weightTracker.db";
    private static final String TABLE_USERS = "users";
    private static final String COL_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String TABLE_WEIGHT = "weight_data";
    private static final String COL_DATE = "date";
    private static final String COL_WEIGHT = "weight";
    private static final String COL_NOTES = "notes";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT, " +
                COL_PASSWORD + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_WEIGHT + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DATE + " TEXT, " +
                COL_WEIGHT + " REAL, " +
                COL_NOTES + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHT);
        onCreate(db);
    }


    // Method to add a new user
    public boolean addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public Cursor checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase(); // Get readable database
        String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + " = ? AND " + COL_PASSWORD + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{username, password});
        return cursor;
    }


    public boolean addWeightData(String date, double weight, String notes) {
        SQLiteDatabase db = this.getWritableDatabase(); // Get writable database
        ContentValues values = new ContentValues();
        values.put(COL_DATE, date);
        values.put(COL_WEIGHT, weight);
        values.put(COL_NOTES, notes);
        long result = db.insert(TABLE_WEIGHT, null, values);
        db.close(); // Close the database after operation
        return result != -1;
    }

    public List<WeightData> getAllWeightData() {
        List<WeightData> weightDataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_WEIGHT, null, null, null, null, null, COL_DATE + " DESC"); // Use TABLE_WEIGHT

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Extract data from cursor
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)); // Use COL_ID
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)); // Use COL_DATE
                double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_WEIGHT)); // Use COL_WEIGHT
                String notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES)); // Use COL_NOTES

                // Create a WeightData object and add to the list
                WeightData weightData = new WeightData(id, date, weight, notes);
                weightDataList.add(weightData);
            } while (cursor.moveToNext());

            cursor.close();
        }

        db.close();
        return weightDataList;
    }
    public boolean deleteWeightData(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_WEIGHT, COL_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return result > 0; // Return true if at least one row was deleted
    }

}