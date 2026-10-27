package com.example.quanlychitieu.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.quanlychitieu.model.Budget;

import java.util.List;

@Dao
public interface BudgetDao {
    @Insert
    long insert(Budget budget);

    @Update
    void update(Budget budget);

    @Delete
    void delete(Budget budget);

    @Query("SELECT * FROM budgets WHERE month = :month ORDER BY categoryId ASC")
    LiveData<List<Budget>> getBudgetsByMonth(String month);

    @Query("SELECT * FROM budgets WHERE month = :month ORDER BY categoryId ASC")
    List<Budget> getBudgetsByMonthSync(String month);

    @Query("SELECT * FROM budgets WHERE month = :month AND categoryId = 0 LIMIT 1")
    LiveData<Budget> getTotalBudgetByMonth(String month);

    @Query("SELECT * FROM budgets WHERE month = :month AND categoryId = 0 LIMIT 1")
    Budget getTotalBudgetByMonthSync(String month);

    @Query("SELECT * FROM budgets WHERE month = :month AND categoryId = :categoryId LIMIT 1")
    Budget getCategoryBudgetSync(String month, int categoryId);
}
