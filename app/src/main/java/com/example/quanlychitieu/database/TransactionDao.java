package com.example.quanlychitieu.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.quanlychitieu.model.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    long insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("DELETE FROM transactions WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    List<Transaction> getAllTransactionsSync();

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC LIMIT :limit")
    LiveData<List<Transaction>> getRecentTransactions(int limit);

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC, id DESC")
    LiveData<List<Transaction>> getTransactionsByType(String type);

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    LiveData<List<Transaction>> getTransactionsBetweenDates(String startDate, String endDate);

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    List<Transaction> getTransactionsBetweenDatesSync(String startDate, String endDate);

    @Query("SELECT * FROM transactions WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC, id DESC")
    LiveData<List<Transaction>> getTransactionsByMonth(String monthPrefix);

    @Query("SELECT * FROM transactions WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC, id DESC")
    List<Transaction> getTransactionsByMonthSync(String monthPrefix);

    @Query("SELECT * FROM transactions WHERE note LIKE '%' || :keyword || '%' ORDER BY date DESC, id DESC")
    LiveData<List<Transaction>> searchByNote(String keyword);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME'")
    LiveData<Double> getTotalIncome();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE'")
    LiveData<Double> getTotalExpense();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME'")
    Double getTotalIncomeSync();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE'")
    Double getTotalExpenseSync();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND date LIKE :monthPrefix || '%'")
    LiveData<Double> getMonthlyIncome(String monthPrefix);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND date LIKE :monthPrefix || '%'")
    LiveData<Double> getMonthlyExpense(String monthPrefix);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND date LIKE :monthPrefix || '%'")
    Double getMonthlyIncomeSync(String monthPrefix);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND date LIKE :monthPrefix || '%'")
    Double getMonthlyExpenseSync(String monthPrefix);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND categoryId = :categoryId AND date LIKE :monthPrefix || '%'")
    Double getMonthlyExpenseByCategorySync(String monthPrefix, int categoryId);
}
