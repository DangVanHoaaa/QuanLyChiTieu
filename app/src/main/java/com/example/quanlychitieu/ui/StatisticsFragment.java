package com.example.quanlychitieu.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.CategoryStatAdapter;
import com.example.quanlychitieu.model.Transaction;
import com.example.quanlychitieu.repository.ExpenseRepository;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.chip.Chip;
import com.example.quanlychitieu.utils.ExportReportUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsFragment extends Fragment {

    private ExpenseRepository repository;
    private CategoryStatAdapter statAdapter;

    private Chip chipWeek, chipMonth, chipYear;
    private TextView tvTimeRangeLabel;
    private ImageButton btnPrevStatTime, btnNextStatTime;
    private View layoutSelectStatTime;
    private LinearLayout layoutStatComparison;
    private TextView tvStatComparisonText;
    private TextView tvTotalIncome, tvTotalExpense;
    private LinearLayout layoutNoData, layoutCharts;

    private BarChart barChart;
    private PieChart pieChart;
    private LineChart lineChart;
    private RecyclerView rvCategoryBreakdown;
    private View btnExportReport;

    private List<Transaction> currentTransactions = new ArrayList<>();
    private int selectedTimeMode = 1; // 0: Week, 1: Month, 2: Year
    private Calendar selectedCalendar = Calendar.getInstance();
    private LiveData<List<Transaction>> currentStatLiveData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        repository = new ExpenseRepository(requireActivity().getApplication());

        chipWeek = view.findViewById(R.id.chip_week);
        chipMonth = view.findViewById(R.id.chip_month);
        chipYear = view.findViewById(R.id.chip_year);
        tvTimeRangeLabel = view.findViewById(R.id.tv_time_range_label);
        btnPrevStatTime = view.findViewById(R.id.btn_prev_stat_time);
        btnNextStatTime = view.findViewById(R.id.btn_next_stat_time);
        layoutSelectStatTime = view.findViewById(R.id.layout_select_stat_time);
        layoutStatComparison = view.findViewById(R.id.layout_stat_comparison);
        tvStatComparisonText = view.findViewById(R.id.tv_stat_comparison_text);

        tvTotalIncome = view.findViewById(R.id.tv_stat_total_income);
        tvTotalExpense = view.findViewById(R.id.tv_stat_total_expense);
        layoutNoData = view.findViewById(R.id.layout_no_stat_data);
        layoutCharts = view.findViewById(R.id.layout_stat_charts);

        barChart = view.findViewById(R.id.chart_bar_income_expense);
        pieChart = view.findViewById(R.id.chart_pie_category);
        lineChart = view.findViewById(R.id.chart_line_trend);
        rvCategoryBreakdown = view.findViewById(R.id.rv_category_breakdown);
        btnExportReport = view.findViewById(R.id.btn_export_report);

        if (btnExportReport != null) {
            btnExportReport.setOnClickListener(v -> {
                ExportReportUtils.showExportDialog(requireContext(), currentTransactions, tvTimeRangeLabel.getText().toString());
            });
        }

        setupRecyclerView();
        setupChartsStyle();
        setupTimeFilter();
        loadStatisticsData();

        return view;
    }

    private void setupRecyclerView() {
        statAdapter = new CategoryStatAdapter(requireContext());
        rvCategoryBreakdown.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategoryBreakdown.setAdapter(statAdapter);
    }

    private void setupChartsStyle() {
        // Cấu hình Biểu đồ đường LineChart
        if (lineChart != null) {
            lineChart.getDescription().setEnabled(false);
            lineChart.setDrawGridBackground(false);
            lineChart.animateX(700);
            XAxis lineX = lineChart.getXAxis();
            lineX.setPosition(XAxis.XAxisPosition.BOTTOM);
            lineX.setDrawGridLines(false);
            lineX.setTextColor(Color.parseColor("#475569"));
            lineX.setTextSize(10f);

            lineChart.getAxisRight().setEnabled(false);
            lineChart.getAxisLeft().setDrawGridLines(true);
            lineChart.getAxisLeft().setGridColor(Color.parseColor("#E2E8F0"));
            lineChart.getAxisLeft().setAxisMinimum(0f);

            Legend lineLegend = lineChart.getLegend();
            lineLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            lineLegend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            lineLegend.setTextSize(11f);
        }

        // Cấu hình Biểu đồ cột
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setFitBars(true);
        barChart.animateY(800);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Thu nhập", "Chi tiêu"}));
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.parseColor("#475569"));

        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisLeft().setGridColor(Color.parseColor("#E2E8F0"));
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getLegend().setEnabled(false);

        // Cấu hình Biểu đồ tròn
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setHoleRadius(50f);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(11f);
        pieChart.setCenterText("Chi tiêu");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.parseColor("#0F172A"));
        pieChart.animateY(800);

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);
        legend.setTextSize(11f);
    }

    private void setupTimeFilter() {
        chipWeek.setOnClickListener(v -> {
            selectedTimeMode = 0;
            loadStatisticsData();
        });

        chipMonth.setOnClickListener(v -> {
            selectedTimeMode = 1;
            loadStatisticsData();
        });

        chipYear.setOnClickListener(v -> {
            selectedTimeMode = 2;
            loadStatisticsData();
        });

        if (btnPrevStatTime != null) {
            btnPrevStatTime.setOnClickListener(v -> {
                if (selectedTimeMode == 0) {
                    selectedCalendar.add(Calendar.WEEK_OF_YEAR, -1);
                } else if (selectedTimeMode == 1) {
                    selectedCalendar.add(Calendar.MONTH, -1);
                } else {
                    selectedCalendar.add(Calendar.YEAR, -1);
                }
                loadStatisticsData();
            });
        }

        if (btnNextStatTime != null) {
            btnNextStatTime.setOnClickListener(v -> {
                if (selectedTimeMode == 0) {
                    selectedCalendar.add(Calendar.WEEK_OF_YEAR, 1);
                } else if (selectedTimeMode == 1) {
                    selectedCalendar.add(Calendar.MONTH, 1);
                } else {
                    selectedCalendar.add(Calendar.YEAR, 1);
                }
                loadStatisticsData();
            });
        }

        if (layoutSelectStatTime != null) {
            layoutSelectStatTime.setOnClickListener(v -> {
                if (selectedTimeMode == 1) {
                    showSelectMonthDialog();
                } else if (selectedTimeMode == 2) {
                    showSelectYearDialog();
                } else {
                    showSelectWeekDialog();
                }
            });
        }
    }

    private void showSelectMonthDialog() {
        if (!isAdded() || getContext() == null) return;

        Calendar tempCal = (Calendar) selectedCalendar.clone();
        int currentYear = tempCal.get(Calendar.YEAR);

        String[] monthNames = new String[]{
                "Thg 1", "Thg 2", "Thg 3", "Thg 4",
                "Thg 5", "Thg 6", "Thg 7", "Thg 8",
                "Thg 9", "Thg 10", "Thg 11", "Thg 12"
        };

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvYear = dialogView.findViewById(R.id.tv_picker_year);
        View btnPrevYear = dialogView.findViewById(R.id.btn_picker_prev_year);
        View btnNextYear = dialogView.findViewById(R.id.btn_picker_next_year);
        GridLayout gridMonths = dialogView.findViewById(R.id.grid_months);
        View btnCurrentMonth = dialogView.findViewById(R.id.btn_quick_current_month);
        View btnPrevMonth = dialogView.findViewById(R.id.btn_quick_prev_month);

        int[] selectedYear = {currentYear};
        tvYear.setText(String.valueOf(selectedYear[0]));

        btnPrevYear.setOnClickListener(v -> {
            selectedYear[0]--;
            tvYear.setText(String.valueOf(selectedYear[0]));
        });

        btnNextYear.setOnClickListener(v -> {
            selectedYear[0]++;
            tvYear.setText(String.valueOf(selectedYear[0]));
        });

        if (gridMonths != null) {
            for (int i = 0; i < gridMonths.getChildCount(); i++) {
                final int monthIndex = i;
                View child = gridMonths.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setText(monthNames[i]);
                    child.setOnClickListener(v -> {
                        selectedCalendar.set(Calendar.YEAR, selectedYear[0]);
                        selectedCalendar.set(Calendar.MONTH, monthIndex);
                        selectedCalendar.set(Calendar.DAY_OF_MONTH, 1);
                        dialog.dismiss();
                        loadStatisticsData();
                    });
                }
            }
        }

        btnCurrentMonth.setOnClickListener(v -> {
            selectedCalendar = Calendar.getInstance();
            dialog.dismiss();
            loadStatisticsData();
        });

        btnPrevMonth.setOnClickListener(v -> {
            selectedCalendar = Calendar.getInstance();
            selectedCalendar.add(Calendar.MONTH, -1);
            dialog.dismiss();
            loadStatisticsData();
        });

        dialog.show();
    }

    private void showSelectYearDialog() {
        if (!isAdded() || getContext() == null) return;
        int curYear = selectedCalendar.get(Calendar.YEAR);
        String[] years = new String[]{
                String.valueOf(curYear - 2),
                String.valueOf(curYear - 1),
                String.valueOf(curYear),
                String.valueOf(curYear + 1)
        };
        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn năm xem thống kê")
                .setItems(years, (dialog, which) -> {
                    selectedCalendar.set(Calendar.YEAR, Integer.parseInt(years[which]));
                    loadStatisticsData();
                })
                .show();
    }

    private void showSelectWeekDialog() {
        if (!isAdded() || getContext() == null) return;
        String[] options = new String[]{"Tuần này", "Tuần trước", "2 tuần trước", "3 tuần trước"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn tuần xem thống kê")
                .setItems(options, (dialog, which) -> {
                    selectedCalendar = Calendar.getInstance();
                    selectedCalendar.add(Calendar.WEEK_OF_YEAR, -which);
                    loadStatisticsData();
                })
                .show();
    }

    private void loadMonthComparison() {
        if (layoutStatComparison == null || tvStatComparisonText == null) return;

        Calendar prevCal = (Calendar) selectedCalendar.clone();
        prevCal.add(Calendar.MONTH, -1);
        String prevMonthIso = DateUtils.formatMonthIso(prevCal);
        String currentMonthIso = DateUtils.formatMonthIso(selectedCalendar);

        repository.getMonthlyExpense(currentMonthIso).observe(getViewLifecycleOwner(), currentSpent -> {
            double cur = currentSpent != null ? currentSpent : 0.0;
            repository.getMonthlyExpense(prevMonthIso).observe(getViewLifecycleOwner(), prevSpent -> {
                if (!isAdded() || layoutStatComparison == null) return;
                double prev = prevSpent != null ? prevSpent : 0.0;

                layoutStatComparison.setVisibility(View.VISIBLE);
                String prevMonthDisplay = DateUtils.formatDisplayMonth(prevCal);

                if (prev == 0 && cur == 0) {
                    tvStatComparisonText.setText("💡 " + DateUtils.formatDisplayMonth(selectedCalendar) + " và " + prevMonthDisplay + " chưa có chi tiêu");
                } else if (prev == 0) {
                    tvStatComparisonText.setText("📈 Chi tiêu: " + CurrencyUtils.formatVnd(cur) + " (" + prevMonthDisplay + " không có dữ liệu chi tiêu)");
                } else {
                    double diff = cur - prev;
                    int percent = (int) Math.round((Math.abs(diff) / prev) * 100);
                    if (diff < 0) {
                        tvStatComparisonText.setText("📉 Chi tiêu giảm " + CurrencyUtils.formatVnd(Math.abs(diff)) + " (-" + percent + "%) so với " + prevMonthDisplay);
                    } else if (diff > 0) {
                        tvStatComparisonText.setText("📈 Chi tiêu tăng " + CurrencyUtils.formatVnd(diff) + " (+" + percent + "%) so với " + prevMonthDisplay);
                    } else {
                        tvStatComparisonText.setText("📊 Chi tiêu bằng " + prevMonthDisplay + " (" + CurrencyUtils.formatVnd(cur) + ")");
                    }
                }
            });
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadStatisticsData();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            loadStatisticsData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currentStatLiveData != null) {
            try {
                currentStatLiveData.removeObservers(getViewLifecycleOwner());
            } catch (Exception ignored) {}
            currentStatLiveData = null;
        }
    }

    private void loadStatisticsData() {
        if (!isAdded()) return;

        String startDate, endDate;
        if (selectedTimeMode == 0) { // Tuần
            String[] weekRange = DateUtils.getStartAndEndOfWeek(selectedCalendar);
            startDate = weekRange[0];
            endDate = weekRange[1];
            tvTimeRangeLabel.setText("Tuần (" + DateUtils.formatDisplayDate(startDate) + " - " + DateUtils.formatDisplayDate(endDate) + ")");
            if (layoutStatComparison != null) layoutStatComparison.setVisibility(View.GONE);
        } else if (selectedTimeMode == 1) { // Tháng
            String[] monthRange = DateUtils.getStartAndEndOfMonth(selectedCalendar);
            startDate = monthRange[0];
            endDate = monthRange[1];
            tvTimeRangeLabel.setText(DateUtils.formatDisplayMonth(selectedCalendar));
            loadMonthComparison();
        } else { // Năm
            String[] yearRange = DateUtils.getStartAndEndOfYear(selectedCalendar);
            startDate = yearRange[0];
            endDate = yearRange[1];
            tvTimeRangeLabel.setText(DateUtils.formatDisplayYear(selectedCalendar));
            if (layoutStatComparison != null) layoutStatComparison.setVisibility(View.GONE);
        }

        // Hủy observer cũ của khoảng thời gian trước đó
        if (currentStatLiveData != null) {
            try {
                currentStatLiveData.removeObservers(getViewLifecycleOwner());
            } catch (Exception ignored) {}
        }

        // Đăng ký LiveData theo thời gian thực: Tự động cập nhật ngay khi thêm / xóa / sửa bất kỳ giao dịch nào
        try {
            currentStatLiveData = repository.getTransactionsBetweenDatesLiveData(startDate, endDate);
            currentStatLiveData.observe(getViewLifecycleOwner(), list -> {
                if (isAdded() && getContext() != null) {
                    updateUiWithData(list);
                }
            });
        } catch (Exception e) {
            repository.getTransactionsBetweenDates(startDate, endDate, list -> {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (isAdded() && getContext() != null) {
                        updateUiWithData(list);
                    }
                });
            });
        }
    }

    private void updateUiWithData(List<Transaction> transactions) {
        this.currentTransactions = transactions != null ? transactions : new ArrayList<>();

        if (transactions == null || transactions.isEmpty()) {
            layoutNoData.setVisibility(View.VISIBLE);
            layoutCharts.setVisibility(View.GONE);
            tvTotalIncome.setText("0 VNĐ");
            tvTotalExpense.setText("0 VNĐ");
            return;
        }

        layoutNoData.setVisibility(View.GONE);
        layoutCharts.setVisibility(View.VISIBLE);

        double totalIncome = 0;
        double totalExpense = 0;
        Map<String, Double> expenseByCategory = new HashMap<>();

        for (Transaction t : transactions) {
            if ("INCOME".equalsIgnoreCase(t.getType())) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
                String cat = t.getCategoryName();
                expenseByCategory.put(cat, expenseByCategory.getOrDefault(cat, 0.0) + t.getAmount());
            }
        }

        tvTotalIncome.setText(CurrencyUtils.formatVnd(totalIncome));
        tvTotalExpense.setText(CurrencyUtils.formatVnd(totalExpense));

        // 1. Cập nhật Biểu đồ đường LineChart (Xu hướng Thu - Chi)
        updateLineChartData(transactions);

        // 1. Biểu đồ cột BarChart: So sánh Thu nhập và Chi tiêu
        List<BarEntry> barEntries = new ArrayList<>();
        barEntries.add(new BarEntry(0, (float) totalIncome));
        barEntries.add(new BarEntry(1, (float) totalExpense));

        BarDataSet barDataSet = new BarDataSet(barEntries, "Thu nhập / Chi tiêu");
        barDataSet.setColors(new int[]{
                ContextCompat.getColor(requireContext(), R.color.income_green),
                ContextCompat.getColor(requireContext(), R.color.expense_red)
        });
        barDataSet.setValueTextSize(11f);
        barDataSet.setValueTextColor(Color.parseColor("#334155"));

        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);
        barChart.invalidate();

        // 2. Biểu đồ tròn PieChart & Danh sách tỷ trọng danh mục chi tiêu
        List<CategoryStatAdapter.CategoryStatItem> statItems = new ArrayList<>();
        List<PieEntry> pieEntries = new ArrayList<>();

        if (totalExpense > 0) {
            pieChart.setVisibility(View.VISIBLE);
            for (Map.Entry<String, Double> entry : expenseByCategory.entrySet()) {
                float percent = (float) ((entry.getValue() / totalExpense) * 100);
                statItems.add(new CategoryStatAdapter.CategoryStatItem(entry.getKey(), entry.getValue(), percent));
                pieEntries.add(new PieEntry((float) entry.getValue().doubleValue(), entry.getKey()));
            }

            // Sắp xếp giảm dần theo số tiền
            Collections.sort(statItems, (o1, o2) -> Double.compare(o2.getAmount(), o1.getAmount()));
            statAdapter.setItems(statItems);

            PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
            pieDataSet.setSliceSpace(3f);
            pieDataSet.setSelectionShift(5f);

            // Bảng màu đẹp mắt
            ArrayList<Integer> colors = new ArrayList<>();
            for (int c : ColorTemplate.MATERIAL_COLORS) colors.add(c);
            for (int c : ColorTemplate.VORDIPLOM_COLORS) colors.add(c);
            for (int c : ColorTemplate.COLORFUL_COLORS) colors.add(c);
            pieDataSet.setColors(colors);

            PieData pieData = new PieData(pieDataSet);
            pieData.setValueFormatter(new PercentFormatter(pieChart));
            pieData.setValueTextSize(11f);
            pieData.setValueTextColor(Color.DKGRAY);

            pieChart.setData(pieData);
            pieChart.invalidate();
        } else {
            pieChart.setVisibility(View.GONE);
            statAdapter.setItems(new ArrayList<>());
        }
    }

    private void updateLineChartData(List<Transaction> transactions) {
        if (lineChart == null) return;

        // Gom nhóm theo ngày
        Map<String, Double> incomeMap = new HashMap<>();
        Map<String, Double> expenseMap = new HashMap<>();
        List<String> sortedDates = new ArrayList<>();

        for (Transaction t : transactions) {
            String date = t.getDate();
            if (!sortedDates.contains(date)) {
                sortedDates.add(date);
            }
            if ("INCOME".equalsIgnoreCase(t.getType())) {
                incomeMap.put(date, incomeMap.getOrDefault(date, 0.0) + t.getAmount());
            } else {
                expenseMap.put(date, expenseMap.getOrDefault(date, 0.0) + t.getAmount());
            }
        }

        Collections.sort(sortedDates);

        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        for (int i = 0; i < sortedDates.size(); i++) {
            String d = sortedDates.get(i);
            float inc = incomeMap.getOrDefault(d, 0.0).floatValue();
            float exp = expenseMap.getOrDefault(d, 0.0).floatValue();
            incomeEntries.add(new Entry(i, inc));
            expenseEntries.add(new Entry(i, exp));

            // Hiển thị ngày/tháng ngắn gọn ví dụ 26/10
            String display = d.length() >= 10 ? d.substring(8, 10) + "/" + d.substring(5, 7) : d;
            xLabels.add(display);
        }

        LineDataSet incomeSet = new LineDataSet(incomeEntries, "Thu nhập");
        incomeSet.setColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        incomeSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        incomeSet.setLineWidth(2.5f);
        incomeSet.setCircleRadius(4f);
        incomeSet.setDrawCircleHole(false);
        incomeSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        incomeSet.setDrawValues(false);

        LineDataSet expenseSet = new LineDataSet(expenseEntries, "Chi tiêu");
        expenseSet.setColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        expenseSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        expenseSet.setLineWidth(2.5f);
        expenseSet.setCircleRadius(4f);
        expenseSet.setDrawCircleHole(false);
        expenseSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        expenseSet.setDrawValues(false);

        LineData lineData = new LineData(incomeSet, expenseSet);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        lineChart.getXAxis().setGranularity(1f);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }
}
