package com.kumud.ledgerapp;

import android.telephony.SmsManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SmsUtils {

    private static final String SIGNATURE = "\n"+"- मुकुट लाल अरविन्द कुमार किराना कोठी, खुर्जा";
    private static final SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public static void sendEntryUpdateSms(String phone, double amount, String type, double updatedBalance, long entryDateMillis, String note) {
        String dateStr = dateFmt.format(new Date(entryDateMillis));
        String action = type.equals("CREDIT") ? "received" : "paid";

        String msg = "You " + action + " " + CurrencyUtils.format(amount) + " on " + dateStr + ".\n";

        msg += balanceLine(updatedBalance) + "\n";
        if (note != null && !note.trim().isEmpty()) {
            msg += "\n"+"Note: " + note.trim() + "\n";
        }
        msg += SIGNATURE;
        send(phone, msg);
    }

    public static void sendDeletionSms(String phone, double amount, double updatedBalance, long entryDateMillis) {
        String dateStr = dateFmt.format(new Date(entryDateMillis));

        String msg = "Entry of amount " + CurrencyUtils.format(amount) + " dated " + dateStr + " deleted.\n"
                + balanceLine(updatedBalance) + "\n"
                + SIGNATURE;

        send(phone, msg);
    }

    public static void sendReminderSms(String phone, double dueAmount) {
        String dateStr = dateFmt.format(new Date(System.currentTimeMillis()));

        String msg = "Payment reminder !\n"
                + "Rs. " + CurrencyUtils.formatPlain(Math.round(dueAmount)) + " due as on " + dateStr + "\n"
                + SIGNATURE;

        send(phone, msg);
    }

    private static String balanceLine(double updatedBalance) {
        if (updatedBalance > 0.009) {
            return "Net Balance Due: " + CurrencyUtils.format(updatedBalance);
        } else if (updatedBalance < -0.009) {
            return "Net Balance Advance: " + CurrencyUtils.format(Math.abs(updatedBalance));
        } else {
            return "Net Balance: ₹0";
        }
    }

    private static void send(String phone, String msg) {
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(msg);
        smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
    }
}