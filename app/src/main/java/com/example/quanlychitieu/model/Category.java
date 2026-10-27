package com.example.quanlychitieu.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String type; // "INCOME" hoặc "EXPENSE"
    private String iconName;
    private String colorHex;

    public Category(String name, String type, String iconName, String colorHex) {
        this.name = name;
        this.type = type;
        this.iconName = iconName;
        this.colorHex = colorHex;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }
}
