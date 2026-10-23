package com.kumud.ledgerapp;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtils {
    public static String format(double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return "₹" + nf.format(Math.round(amount));
    }

    public static String formatPlain(long amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return nf.format(amount);
    }
}