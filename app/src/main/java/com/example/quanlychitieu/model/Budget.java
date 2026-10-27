package com.example.quanlychitieu.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class Budget {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String month; // Định dạng "yyyy-MM", ví dụ "2026-10"
    private int categoryId; // 0 nếu là ngân sách tổng của tháng
    private String categoryName; // "Ngân sách tổng" hoặc tên danh mục
    private double amount;

    public Budget(String month, int categoryId, String categoryName, double amount) {
        this.month = month;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
