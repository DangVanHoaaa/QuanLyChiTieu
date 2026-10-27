package com.example.quanlychitieu.utils;

import android.content.Context;

import com.example.quanlychitieu.R;

public class CategoryUtils {

    public static int getCategoryIconRes(Context context, String iconName) {
        if (iconName == null) return R.drawable.ic_cat_expense_other;
        switch (iconName) {
            case "ic_cat_salary":
                return R.drawable.ic_cat_salary;
            case "ic_cat_bonus":
                return R.drawable.ic_cat_bonus;
            case "ic_cat_gift":
                return R.drawable.ic_cat_gift;
            case "ic_cat_income_other":
                return R.drawable.ic_cat_income_other;
            case "ic_cat_food":
                return R.drawable.ic_cat_food;
            case "ic_cat_transport":
                return R.drawable.ic_cat_transport;
            case "ic_cat_education":
                return R.drawable.ic_cat_education;
            case "ic_cat_shopping":
                return R.drawable.ic_cat_shopping;
            case "ic_cat_bill":
                return R.drawable.ic_cat_bill;
            case "ic_cat_entertainment":
                return R.drawable.ic_cat_entertainment;
            case "ic_cat_health":
                return R.drawable.ic_cat_health;
            case "ic_cat_home":
                return R.drawable.ic_cat_home;
            default:
                return R.drawable.ic_cat_expense_other;
        }
    }
}
