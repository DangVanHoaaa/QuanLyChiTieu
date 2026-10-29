package com.example.quanlychitieu.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.quanlychitieu.model.Budget;
import com.example.quanlychitieu.model.Category;
import com.example.quanlychitieu.model.PendingTransaction;
import com.example.quanlychitieu.model.SavingsGoal;
import com.example.quanlychitieu.model.Transaction;
import androidx.room.migration.Migration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

@Database(entities = {Transaction.class, Category.class, Budget.class, SavingsGoal.class, PendingTransaction.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "quan_ly_chi_tieu_db";
    private static volatile AppDatabase INSTANCE;

    public abstract TransactionDao transactionDao();
    public abstract CategoryDao categoryDao();
    public abstract BudgetDao budgetDao();
    public abstract SavingsGoalDao savingsGoalDao();
    public abstract PendingTransactionDao pendingTransactionDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `pending_transactions` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`bankName` TEXT, " +
                    "`type` TEXT, " +
                    "`amount` REAL NOT NULL, " +
                    "`suggestedCategory` TEXT, " +
                    "`rawContent` TEXT, " +
                    "`note` TEXT, " +
                    "`date` TEXT, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`status` TEXT)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            // Khởi tạo danh mục mặc định khi tạo database lần đầu
                            Executors.newSingleThreadExecutor().execute(() -> {
                                initDefaultCategories(getInstance(context));
                            });
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void initDefaultCategories(AppDatabase database) {
        CategoryDao dao = database.categoryDao();
        List<Category> list = new ArrayList<>();
        // Danh mục thu nhập mặc định
        list.add(new Category("Lương", "INCOME", "ic_cat_salary", "#16A34A"));
        list.add(new Category("Thưởng", "INCOME", "ic_cat_bonus", "#22C55E"));
        list.add(new Category("Tiền được cho", "INCOME", "ic_cat_gift", "#10B981"));
        list.add(new Category("Thu nhập khác", "INCOME", "ic_cat_income_other", "#059669"));

        // Danh mục chi tiêu mặc định
        list.add(new Category("Ăn uống", "EXPENSE", "ic_cat_food", "#EF4444"));
        list.add(new Category("Đi lại", "EXPENSE", "ic_cat_transport", "#F97316"));
        list.add(new Category("Học tập", "EXPENSE", "ic_cat_education", "#3B82F6"));
        list.add(new Category("Mua sắm", "EXPENSE", "ic_cat_shopping", "#EC4899"));
        list.add(new Category("Hóa đơn", "EXPENSE", "ic_cat_bill", "#8B5CF6"));
        list.add(new Category("Giải trí", "EXPENSE", "ic_cat_entertainment", "#06B6D4"));
        list.add(new Category("Sức khỏe", "EXPENSE", "ic_cat_health", "#14B8A6"));
        list.add(new Category("Nhà ở", "EXPENSE", "ic_cat_home", "#6366F1"));
        list.add(new Category("Chi tiêu khác", "EXPENSE", "ic_cat_expense_other", "#64748B"));

        for (Category c : list) {
            if (dao.getCategoryByNameAndTypeSync(c.getName(), c.getType()) == null) {
                dao.insert(c);
            }
        }
    }
}
