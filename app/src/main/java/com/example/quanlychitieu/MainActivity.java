package com.example.quanlychitieu;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.quanlychitieu.ui.BudgetFragment;
import com.example.quanlychitieu.ui.OverviewFragment;
import com.example.quanlychitieu.ui.SavingsFragment;
import com.example.quanlychitieu.ui.StatisticsFragment;
import com.example.quanlychitieu.ui.TransactionsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private final Fragment overviewFragment = new OverviewFragment();
    private final Fragment transactionsFragment = new TransactionsFragment();
    private final Fragment statisticsFragment = new StatisticsFragment();
    private final Fragment budgetFragment = new BudgetFragment();
    private final Fragment savingsFragment = new SavingsFragment();
    private Fragment activeFragment = overviewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.quanlychitieu.utils.ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .add(R.id.fragment_container, savingsFragment, "SAVINGS").hide(savingsFragment)
                .add(R.id.fragment_container, budgetFragment, "BUDGET").hide(budgetFragment)
                .add(R.id.fragment_container, statisticsFragment, "STATISTICS").hide(statisticsFragment)
                .add(R.id.fragment_container, transactionsFragment, "TRANSACTIONS").hide(transactionsFragment)
                .add(R.id.fragment_container, overviewFragment, "OVERVIEW")
                .commit();

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_overview) {
                switchFragment(overviewFragment);
                return true;
            } else if (itemId == R.id.nav_transactions) {
                switchFragment(transactionsFragment);
                return true;
            } else if (itemId == R.id.nav_statistics) {
                switchFragment(statisticsFragment);
                return true;
            } else if (itemId == R.id.nav_budget) {
                switchFragment(budgetFragment);
                return true;
            } else if (itemId == R.id.nav_savings) {
                switchFragment(savingsFragment);
                return true;
            }
            return false;
        });

        // Yêu cầu quyền gửi thông báo trên Android 13+ nếu cần
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            if (intent.getBooleanExtra("OPEN_TRANSACTIONS", false)) {
                navigateToTransactions();
            } else if (intent.getBooleanExtra("OPEN_PENDING_APPROVAL", false)) {
                int pendingId = intent.getIntExtra("PENDING_ID", -1);
                showPendingDialog(pendingId);
            }
        }
    }

    public void showPendingDialog(int pendingId) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            com.example.quanlychitieu.database.AppDatabase db = com.example.quanlychitieu.database.AppDatabase.getInstance(this);
            com.example.quanlychitieu.model.PendingTransaction pt = null;
            if (pendingId > 0) {
                pt = db.pendingTransactionDao().getById(pendingId);
            }
            if (pt == null) {
                java.util.List<com.example.quanlychitieu.model.PendingTransaction> list = db.pendingTransactionDao().getPendingTransactionsSync();
                if (list != null && !list.isEmpty()) {
                    pt = list.get(0);
                }
            }
            if (pt != null) {
                final com.example.quanlychitieu.model.PendingTransaction targetPt = pt;
                runOnUiThread(() -> {
                    com.example.quanlychitieu.ui.dialog.PendingTransactionDialogHelper.showApproveDialog(
                            MainActivity.this, targetPt, null
                    );
                });
            }
        });
    }

    private void switchFragment(Fragment target) {
        if (target != activeFragment) {
            getSupportFragmentManager().beginTransaction()
                    .hide(activeFragment)
                    .show(target)
                    .commit();
            activeFragment = target;
        }
    }

    public void navigateToTransactions() {
        bottomNav.setSelectedItemId(R.id.nav_transactions);
    }
}
