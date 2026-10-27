package com.example.quanlychitieu.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.quanlychitieu.model.Transaction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportReportUtils {

    public static void showExportDialog(Context context, List<Transaction> transactions, String periodLabel) {
        if (transactions == null || transactions.isEmpty()) {
            Toast.makeText(context, "Không có dữ liệu giao dịch trong khoảng thời gian này để xuất báo cáo!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Xuất file Excel (.csv)", "Xuất tài liệu PDF (.pdf)"};
        new AlertDialog.Builder(context)
                .setTitle("Chọn định dạng xuất báo cáo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        exportToExcel(context, transactions, periodLabel);
                    } else {
                        exportToPdf(context, transactions, periodLabel);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    public static void exportToExcel(Context context, List<Transaction> transactions, String periodLabel) {
        try {
            File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "BaoCaoChiTieu_" + timeStamp + ".csv";
            File file = new File(dir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);

            // Ghi BOM UTF-8 để Excel tiếng Việt không bị lỗi font
            writer.write('\uFEFF');

            // Header báo cáo
            writer.write("BÁO CÁO THU CHI - VÍ THÔNG MINH\n");
            writer.write("Khoảng thời gian:," + periodLabel + "\n");
            writer.write("Ngày xuất file:," + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()) + "\n\n");

            // Cột dữ liệu
            writer.write("Mã GD,Ngày,Loại giao dịch,Danh mục,Số tiền (VNĐ),Ghi chú\n");

            double totalIncome = 0;
            double totalExpense = 0;

            for (Transaction t : transactions) {
                boolean isIncome = "INCOME".equalsIgnoreCase(t.getType());
                if (isIncome) {
                    totalIncome += t.getAmount();
                } else {
                    totalExpense += t.getAmount();
                }

                String typeStr = isIncome ? "Thu nhập" : "Chi tiêu";
                String note = t.getNote() != null ? t.getNote().replace(",", " ") : "";
                String line = String.format(Locale.getDefault(), "%d,%s,%s,%s,%.0f,%s\n",
                        t.getId(),
                        DateUtils.formatDisplayDate(t.getDate()),
                        typeStr,
                        t.getCategoryName(),
                        t.getAmount(),
                        note);
                writer.write(line);
            }

            // Tổng kết
            writer.write("\nTỔNG KẾT\n");
            writer.write(String.format(Locale.getDefault(), "Tổng thu nhập:,%.0f VNĐ\n", totalIncome));
            writer.write(String.format(Locale.getDefault(), "Tổng chi tiêu:,%.0f VNĐ\n", totalExpense));
            writer.write(String.format(Locale.getDefault(), "Số dư kỳ này:,%.0f VNĐ\n", (totalIncome - totalExpense)));

            writer.flush();
            writer.close();
            fos.close();

            Toast.makeText(context, "Đã xuất file Excel: " + fileName, Toast.LENGTH_SHORT).show();
            openOrShareFile(context, file, "text/csv");

        } catch (Exception e) {
            Toast.makeText(context, "Lỗi khi xuất file Excel: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void exportToPdf(Context context, List<Transaction> transactions, String periodLabel) {
        try {
            File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "BaoCaoChiTieu_" + timeStamp + ".pdf";
            File file = new File(dir, fileName);

            PdfDocument document = new PdfDocument();
            // Khổ A4 tiêu chuẩn 595 x 842 points
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#16A34A"));
            paint.setTextSize(18f);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            // Tiêu đề
            canvas.drawText("BÁO CÁO THU CHI - VÍ THÔNG MINH", 40, 50, paint);

            paint.setTextSize(12f);
            paint.setColor(Color.parseColor("#64748B"));
            paint.setTypeface(Typeface.DEFAULT);
            canvas.drawText("Khoảng thời gian: " + periodLabel, 40, 75, paint);
            canvas.drawText("Ngày lập: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()), 40, 95, paint);

            // Đường kẻ ngang
            paint.setColor(Color.parseColor("#E2E8F0"));
            paint.setStrokeWidth(1.5f);
            canvas.drawLine(40, 110, 555, 110, paint);

            // Tổng kết số liệu
            double totalIncome = 0;
            double totalExpense = 0;
            for (Transaction t : transactions) {
                if ("INCOME".equalsIgnoreCase(t.getType())) totalIncome += t.getAmount();
                else totalExpense += t.getAmount();
            }

            paint.setTextSize(12f);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setColor(Color.parseColor("#16A34A"));
            canvas.drawText("Thu nhập: +" + CurrencyUtils.formatVnd(totalIncome), 40, 135, paint);

            paint.setColor(Color.parseColor("#DC2626"));
            canvas.drawText("Chi tiêu: -" + CurrencyUtils.formatVnd(totalExpense), 220, 135, paint);

            paint.setColor(Color.parseColor("#0F172A"));
            canvas.drawText("Số dư: " + CurrencyUtils.formatVnd(totalIncome - totalExpense), 400, 135, paint);

            canvas.drawLine(40, 150, 555, 150, paint);

            // Header bảng
            paint.setColor(Color.parseColor("#0F172A"));
            paint.setTextSize(11f);
            canvas.drawText("Ngày", 40, 175, paint);
            canvas.drawText("Loại", 110, 175, paint);
            canvas.drawText("Danh mục", 175, 175, paint);
            canvas.drawText("Số tiền", 300, 175, paint);
            canvas.drawText("Ghi chú", 410, 175, paint);

            canvas.drawLine(40, 185, 555, 185, paint);

            // Dòng dữ liệu
            int y = 205;
            paint.setTypeface(Typeface.DEFAULT);

            int maxItems = Math.min(transactions.size(), 26);
            for (int i = 0; i < maxItems; i++) {
                Transaction t = transactions.get(i);
                boolean isIncome = "INCOME".equalsIgnoreCase(t.getType());

                paint.setColor(Color.parseColor("#475569"));
                canvas.drawText(DateUtils.formatDisplayDate(t.getDate()), 40, y, paint);

                if (isIncome) {
                    paint.setColor(Color.parseColor("#16A34A"));
                    canvas.drawText("Thu", 110, y, paint);
                } else {
                    paint.setColor(Color.parseColor("#DC2626"));
                    canvas.drawText("Chi", 110, y, paint);
                }

                paint.setColor(Color.parseColor("#0F172A"));
                canvas.drawText(t.getCategoryName(), 175, y, paint);

                if (isIncome) paint.setColor(Color.parseColor("#16A34A"));
                else paint.setColor(Color.parseColor("#DC2626"));
                canvas.drawText(CurrencyUtils.formatVnd(t.getAmount()), 300, y, paint);

                paint.setColor(Color.parseColor("#64748B"));
                String note = t.getNote() != null ? t.getNote() : "";
                if (note.length() > 22) note = note.substring(0, 20) + "..";
                canvas.drawText(note, 410, y, paint);

                y += 22;
            }

            if (transactions.size() > maxItems) {
                paint.setColor(Color.parseColor("#94A3B8"));
                canvas.drawText("... và " + (transactions.size() - maxItems) + " giao dịch khác.", 40, y + 10, paint);
            }

            document.finishPage(page);

            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            Toast.makeText(context, "Đã xuất file PDF: " + fileName, Toast.LENGTH_SHORT).show();
            openOrShareFile(context, file, "application/pdf");

        } catch (Exception e) {
            Toast.makeText(context, "Lỗi khi xuất file PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void openOrShareFile(Context context, File file, String mimeType) {
        try {
            Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Mở hoặc chia sẻ báo cáo"));
        } catch (Exception e) {
            Toast.makeText(context, "File đã được lưu tại: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }
}
