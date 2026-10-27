package com.example.quanlychitieu;

import android.os.Bundle;

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
