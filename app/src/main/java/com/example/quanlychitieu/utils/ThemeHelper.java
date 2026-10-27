package com.example.quanlychitieu.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;

import com.example.quanlychitieu.R;

public class ThemeHelper {
    private static final String PREF_NAME = "app_theme_prefs";
    private static final String KEY_THEME = "current_theme_id";

    public static final int THEME_GREEN = 0;
    public static final int THEME_BLUE = 1;
    public static final int THEME_PURPLE = 2;
    public static final int THEME_ORANGE = 3;
    public static final int THEME_PINK = 4;

    public static void applyTheme(Activity activity) {
        int themeId = getSavedTheme(activity);
        switch (themeId) {
            case THEME_BLUE:
                activity.setTheme(R.style.Theme_QuanLyChiTieu_Blue);
                break;
            case THEME_PURPLE:
                activity.setTheme(R.style.Theme_QuanLyChiTieu_Purple);
                break;
            case THEME_ORANGE:
                activity.setTheme(R.style.Theme_QuanLyChiTieu_Orange);
                break;
            case THEME_PINK:
                activity.setTheme(R.style.Theme_QuanLyChiTieu_Pink);
                break;
            default:
                activity.setTheme(R.style.Theme_QuanLyChiTieu);
                break;
        }
    }

    public static int getSavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, THEME_GREEN);
    }

    public static void saveTheme(Context context, int themeId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, themeId).apply();
    }

    public static void showThemeDialog(Activity activity, Runnable onThemeChanged) {
        String[] themes = {
                "🌿 Xanh Lá Hiện Đại (Mặc định)",
                "🌊 Xanh Đại Dương (Trí tuệ)",
                "💜 Tím Quý Phái (Sang trọng)",
                "🍊 Cam Năng Động (Tươi trẻ)",
                "🌸 Hồng Ngọt Ngào (Tinh tế)"
        };

        int currentTheme = getSavedTheme(activity);

        new AlertDialog.Builder(activity)
                .setTitle("Chọn phong cách giao diện")
                .setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
                    if (which != currentTheme) {
                        saveTheme(activity, which);
                        dialog.dismiss();
                        if (onThemeChanged != null) {
                            onThemeChanged.run();
                        }
                    } else {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
