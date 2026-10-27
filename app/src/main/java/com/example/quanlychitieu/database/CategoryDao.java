package com.example.quanlychitieu.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.quanlychitieu.model.Category;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    long insert(Category category);

    @Insert
    void insertAll(List<Category> categories);

    @Query("SELECT * FROM categories ORDER BY id ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM categories ORDER BY id ASC")
    List<Category> getAllCategoriesSync();

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY id ASC")
    LiveData<List<Category>> getCategoriesByType(String type);

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY id ASC")
    List<Category> getCategoriesByTypeSync(String type);

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    Category getCategoryById(int id);

    @Query("SELECT COUNT(*) FROM categories")
    int getCategoryCount();
}
