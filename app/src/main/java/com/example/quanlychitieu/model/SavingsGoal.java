package com.example.quanlychitieu.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "savings_goals")
public class SavingsGoal {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private double targetAmount;
    private double currentAmount;
    private String targetDate; // "yyyy-MM-dd"
    private boolean isCompleted;

    public SavingsGoal(String name, double targetAmount, double currentAmount, String targetDate, boolean isCompleted) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.targetDate = targetDate;
        this.isCompleted = isCompleted;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public String getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(String targetDate) {
        this.targetDate = targetDate;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}
