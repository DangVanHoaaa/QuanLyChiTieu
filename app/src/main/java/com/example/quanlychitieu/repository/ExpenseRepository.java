package com.example.quanlychitieu.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.quanlychitieu.database.AppDatabase;
import com.example.quanlychitieu.database.BudgetDao;
import com.example.quanlychitieu.database.CategoryDao;
import com.example.quanlychitieu.database.SavingsGoalDao;
import com.example.quanlychitieu.database.TransactionDao;
import com.example.quanlychitieu.model.Budget;
import com.example.quanlychitieu.model.Category;
import com.example.quanlychitieu.model.SavingsGoal;
import com.example.quanlychitieu.model.Transaction;
import com.example.quanlychitieu.utils.DateUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseRepository {
    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;
    private final BudgetDao budgetDao;
    private final SavingsGoalDao savingsGoalDao;
    private final ExecutorService executorService;

    public interface Callback<T> {
        void onComplete(T result);
    }

    public ExpenseRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        transactionDao = db.transactionDao();
        categoryDao = db.categoryDao();
        budgetDao = db.budgetDao();
        savingsGoalDao = db.savingsGoalDao();
        executorService = Executors.newFixedThreadPool(4);

        // Đảm bảo luôn nạp danh mục mặc định vào cơ sở dữ liệu nếu chưa có
        executorService.execute(() -> {
            if (categoryDao.getCategoryCount() == 0) {
                initDefaultCategories();
            }
        });
    }

    private void initDefaultCategories() {
        java.util.List<Category> list = new java.util.ArrayList<>();
        list.add(new Category("Lương", "INCOME", "ic_cat_salary", "#16A34A"));
        list.add(new Category("Thưởng", "INCOME", "ic_cat_bonus", "#22C55E"));
        list.add(new Category("Tiền được cho", "INCOME", "ic_cat_gift", "#10B981"));
        list.add(new Category("Thu nhập khác", "INCOME", "ic_cat_income_other", "#059669"));

        list.add(new Category("Ăn uống", "EXPENSE", "ic_cat_food", "#EF4444"));
        list.add(new Category("Đi lại", "EXPENSE", "ic_cat_transport", "#F97316"));
        list.add(new Category("Học tập", "EXPENSE", "ic_cat_education", "#3B82F6"));
        list.add(new Category("Mua sắm", "EXPENSE", "ic_cat_shopping", "#EC4899"));
        list.add(new Category("Hóa đơn", "EXPENSE", "ic_cat_bill", "#8B5CF6"));
        list.add(new Category("Giải trí", "EXPENSE", "ic_cat_entertainment", "#06B6D4"));
        list.add(new Category("Sức khỏe", "EXPENSE", "ic_cat_health", "#14B8A6"));
        list.add(new Category("Nhà ở", "EXPENSE", "ic_cat_home", "#6366F1"));
        list.add(new Category("Chi tiêu khác", "EXPENSE", "ic_cat_expense_other", "#64748B"));

        categoryDao.insertAll(list);
    }

    // --- TRANSACTIONS ---
    public LiveData<List<Transaction>> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }

    public LiveData<List<Transaction>> getRecentTransactions(int limit) {
        return transactionDao.getRecentTransactions(limit);
    }

    public LiveData<List<Transaction>> getTransactionsByType(String type) {
        return transactionDao.getTransactionsByType(type);
    }

    public LiveData<List<Transaction>> getTransactionsByMonth(String monthPrefix) {
        return transactionDao.getTransactionsByMonth(monthPrefix);
    }

    public LiveData<List<Transaction>> searchByNote(String keyword) {
        return transactionDao.searchByNote(keyword);
    }

    public LiveData<Double> getTotalIncome() {
        return transactionDao.getTotalIncome();
    }

    public LiveData<Double> getTotalExpense() {
        return transactionDao.getTotalExpense();
    }

    public LiveData<Double> getMonthlyIncome(String monthPrefix) {
        return transactionDao.getMonthlyIncome(monthPrefix);
    }

    public LiveData<Double> getMonthlyExpense(String monthPrefix) {
        return transactionDao.getMonthlyExpense(monthPrefix);
    }

    public void insertTransaction(Transaction transaction, Callback<Long> callback) {
        executorService.execute(() -> {
            long id = transactionDao.insert(transaction);
            if (callback != null) {
                callback.onComplete(id);
            }
        });
    }

    public void updateTransaction(Transaction transaction, Callback<Void> callback) {
        executorService.execute(() -> {
            transactionDao.update(transaction);
            if (callback != null) {
                callback.onComplete(null);
            }
        });
    }

    public void deleteTransaction(Transaction transaction, Callback<Void> callback) {
        executorService.execute(() -> {
            transactionDao.delete(transaction);
            if (callback != null) {
                callback.onComplete(null);
            }
        });
    }

    public void getTransactionsBetweenDates(String start, String end, Callback<List<Transaction>> callback) {
        executorService.execute(() -> {
            List<Transaction> list = transactionDao.getTransactionsBetweenDatesSync(start, end);
            if (callback != null) {
                callback.onComplete(list);
            }
        });
    }

    public LiveData<List<Transaction>> getTransactionsBetweenDatesLiveData(String start, String end) {
        return transactionDao.getTransactionsBetweenDates(start, end);
    }

    public void getTransactionsByMonthSync(String monthPrefix, Callback<List<Transaction>> callback) {
        executorService.execute(() -> {
            List<Transaction> list = transactionDao.getTransactionsByMonthSync(monthPrefix);
            if (callback != null) {
                callback.onComplete(list);
            }
        });
    }

    // --- CATEGORIES ---
    public LiveData<List<Category>> getAllCategories() {
        return categoryDao.getAllCategories();
    }

    public LiveData<List<Category>> getCategoriesByType(String type) {
        return categoryDao.getCategoriesByType(type);
    }

    public void insertCategory(Category category, Callback<Long> callback) {
        executorService.execute(() -> {
            long id = categoryDao.insert(category);
            if (callback != null) {
                callback.onComplete(id);
            }
        });
    }

    public void getCategoriesByTypeSync(String type, Callback<List<Category>> callback) {
        executorService.execute(() -> {
            if (categoryDao.getCategoryCount() == 0) {
                initDefaultCategories();
            }
            List<Category> list = categoryDao.getCategoriesByTypeSync(type);
            if (list == null || list.isEmpty()) {
                initDefaultCategories();
                list = categoryDao.getCategoriesByTypeSync(type);
            }
            if (callback != null) {
                callback.onComplete(list);
            }
        });
    }

    // --- BUDGETS ---
    public LiveData<List<Budget>> getBudgetsByMonth(String month) {
        return budgetDao.getBudgetsByMonth(month);
    }

    public LiveData<Budget> getTotalBudgetByMonth(String month) {
        return budgetDao.getTotalBudgetByMonth(month);
    }

    public void saveOrUpdateBudget(Budget budget, Callback<Void> callback) {
        executorService.execute(() -> {
            Budget existing;
            if (budget.getCategoryId() == 0) {
                existing = budgetDao.getTotalBudgetByMonthSync(budget.getMonth());
            } else {
                existing = budgetDao.getCategoryBudgetSync(budget.getMonth(), budget.getCategoryId());
            }

            if (existing != null) {
                existing.setAmount(budget.getAmount());
                budgetDao.update(existing);
            } else {
                budgetDao.insert(budget);
            }

            if (callback != null) {
                callback.onComplete(null);
            }
        });
    }

    public void deleteBudget(Budget budget, Callback<Void> callback) {
        executorService.execute(() -> {
            budgetDao.delete(budget);
            if (callback != null) {
                callback.onComplete(null);
            }
        });
    }

    public void getMonthlyExpenseForCategory(String monthPrefix, int categoryId, Callback<Double> callback) {
        executorService.execute(() -> {
            Double spent = transactionDao.getMonthlyExpenseByCategorySync(monthPrefix, categoryId);
            if (callback != null) {
                callback.onComplete(spent != null ? spent : 0.0);
            }
        });
    }

    // --- SAVINGS GOALS ---
    public LiveData<List<SavingsGoal>> getAllSavingsGoals() {
        return savingsGoalDao.getAllGoals();
    }

    public void insertSavingsGoal(SavingsGoal goal, Callback<Long> callback) {
        executorService.execute(() -> {
            long id = savingsGoalDao.insert(goal);
            if (callback != null) {
                callback.onComplete(id);
            }
        });
    }

    public void updateSavingsGoal(SavingsGoal goal, Callback<Void> callback) {
        executorService.execute(() -> {
            savingsGoalDao.update(goal);
            if (callback != null) {
                callback.onComplete(null);
            }
        });
    }

    public void deleteSavingsGoal(SavingsGoal goal, Callback<Void> callback) {
        executorService.execute(() -> {
            savingsGoalDao.delete(goal);
            if (callback != null) {
                callback.onComplete(null);
            }
        });
    }

    /**
     * Nạp tiền vào mục tiêu tiết kiệm và tự động tạo giao dịch Chi tiêu để trừ vào tổng số dư hiện tại
     */
    public void depositToSavingsGoal(SavingsGoal goal, double depositAmount, Callback<Void> callback) {
        executorService.execute(() -> {
            // 1. Cập nhật số tiền hiện tại của mục tiêu tiết kiệm
            double newCurrent = goal.getCurrentAmount() + depositAmount;
            goal.setCurrentAmount(newCurrent);
            goal.setCompleted(newCurrent >= goal.getTargetAmount());
            savingsGoalDao.update(goal);

            // 2. Tìm hoặc tự động tạo danh mục "Tiết kiệm"
            int catId = -1;
            List<Category> categories = categoryDao.getCategoriesByTypeSync("EXPENSE");
            if (categories != null) {
                for (Category c : categories) {
                    if ("Tiết kiệm".equalsIgnoreCase(c.getName())) {
                        catId = c.getId();
                        break;
                    }
                }
            }
            if (catId == -1) {
                Category cat = new Category("Tiết kiệm", "EXPENSE", "ic_nav_savings", "#059669");
                catId = (int) categoryDao.insert(cat);
            }

            // 3. Tạo giao dịch chi tiêu để trừ vào tổng số dư hiện tại
            Transaction tx = new Transaction(
                    "EXPENSE",
                    depositAmount,
                    catId,
                    "Tiết kiệm",
                    DateUtils.getTodayIso(),
                    "Nạp tiền tiết kiệm: " + goal.getName(),
                    System.currentTimeMillis()
            );
            transactionDao.insert(tx);

            if (callback != null) {
                callback.onComplete(null);
            }
        });
    }
}
