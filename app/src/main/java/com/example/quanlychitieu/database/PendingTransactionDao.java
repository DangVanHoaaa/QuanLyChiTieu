package com.example.quanlychitieu.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.quanlychitieu.model.PendingTransaction;

import java.util.List;

@Dao
public interface PendingTransactionDao {

    @Insert
    long insert(PendingTransaction transaction);

    @Update
    void update(PendingTransaction transaction);

    @Delete
    void delete(PendingTransaction transaction);

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM pending_transactions WHERE status = 'PENDING' ORDER BY timestamp DESC")
    LiveData<List<PendingTransaction>> getPendingTransactions();

    @Query("SELECT * FROM pending_transactions WHERE status = 'PENDING' ORDER BY timestamp DESC")
    List<PendingTransaction> getPendingTransactionsSync();

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = 'PENDING'")
    LiveData<Integer> getPendingCount();

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = 'PENDING'")
    int getPendingCountSync();

    @Query("SELECT * FROM pending_transactions WHERE id = :id LIMIT 1")
    PendingTransaction getById(int id);

    @Query("DELETE FROM pending_transactions WHERE status = 'PENDING'")
    void deleteAllPending();
}
