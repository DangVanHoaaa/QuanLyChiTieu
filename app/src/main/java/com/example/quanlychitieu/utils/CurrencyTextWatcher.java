package com.example.quanlychitieu.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyTextWatcher implements TextWatcher {

    private final EditText editText;
    private String current = "";
    private final DecimalFormat formatter;

    public CurrencyTextWatcher(EditText editText) {
        this.editText = editText;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        this.formatter = new DecimalFormat("#,###", symbols);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        String str = s.toString();
        if (str.equals(current)) {
            return;
        }

        editText.removeTextChangedListener(this);

        try {
            // Lấy toàn bộ số
            String cleanString = str.replaceAll("[^0-9]", "");
            if (!cleanString.isEmpty()) {
                double parsed = Double.parseDouble(cleanString);
                String formatted = formatter.format(parsed);
                current = formatted;
                editText.setText(formatted);
                editText.setSelection(formatted.length());
            } else {
                current = "";
                editText.setText("");
            }
        } catch (Exception ignored) {
        }

        editText.addTextChangedListener(this);
    }
}
