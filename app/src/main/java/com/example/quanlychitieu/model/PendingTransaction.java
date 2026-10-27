package com.example.quanlychitieu.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_transactions")
public class PendingTransaction {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String bankName;
    private String type; // "INCOME" hoặc "EXPENSE"
    private double amount;
    private String suggestedCategory;
    private String rawContent;
    private String note;
    private String date; // "yyyy-MM-dd"
    private long timestamp;
    private String status; // "PENDING", "APPROVED", "DISMISSED"

    public PendingTransaction(String bankName, String type, double amount, String suggestedCategory,
                              String rawContent, String note, String date, long timestamp, String status) {
        this.bankName = bankName;
        this.type = type;
        this.amount = amount;
        this.suggestedCategory = suggestedCategory;
        this.rawContent = rawContent;
        this.note = note;
        this.date = date;
        this.timestamp = timestamp;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getSuggestedCategory() {
        return suggestedCategory;
    }

    public void setSuggestedCategory(String suggestedCategory) {
        this.suggestedCategory = suggestedCategory;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
