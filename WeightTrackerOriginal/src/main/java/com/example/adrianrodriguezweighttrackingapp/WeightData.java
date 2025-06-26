package com.example.adrianrodriguezweighttrackingapp;

public class WeightData {
    private int id;
    private String date;
    private double weight;
    private String notes;

    public WeightData(int id, String date, double weight, String notes) {
        this.id = id;
        this.date = date;
        this.weight = weight;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public double getWeight() {
        return weight;
    }

    public String getNotes() {
        return notes;
    }
}
