//package com.kumud.ledgerapp;
//
//import android.Manifest;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import android.view.View;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import java.util.List;
//
//public class ContactDetailActivity extends AppCompatActivity implements TransactionAdapter.OnTransactionDeletedListener {
//
//    private static final int REQ_SEND_SMS_DELETE = 10;
//    private static final int REQ_SEND_SMS_REMINDER = 11;
//    Button btnSendReminder;
//
//    DBHelper db;
//    long contactId;
//    String contactName;
//    String contactPhone;
//
//    TextView tvNetBalance;
//    RecyclerView rvTransactions;
//    TransactionAdapter adapter;
//
//    double pendingAmount;
//    long pendingEntryDate;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_contact_detail);
//
//        db = new DBHelper(this);
//
//        contactId = getIntent().getLongExtra("contact_id", -1);
//        contactName = getIntent().getStringExtra("contact_name");
//        contactPhone = getIntent().getStringExtra("contact_phone");
//
//        TextView tvContactName = findViewById(R.id.tvContactName);
//        tvNetBalance = findViewById(R.id.tvNetBalance);
//        rvTransactions = findViewById(R.id.rvTransactions);
//        Button btnGivenTab = findViewById(R.id.btnGivenTab);
//        Button btnReceivedTab = findViewById(R.id.btnReceivedTab);
//        btnSendReminder = findViewById(R.id.btnSendReminder);
//        btnSendReminder.setOnClickListener(v -> checkPermissionAndSendReminder());
//
//        tvContactName.setText(contactName);
//        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
//
//        btnGivenTab.setOnClickListener(v -> openAddEntry("CREDIT"));
//        btnReceivedTab.setOnClickListener(v -> openAddEntry("DEBIT"));
//
//        loadData();
//    }
//
//    void openAddEntry(String presetType) {
//        Intent intent = new Intent(ContactDetailActivity.this, AddEntryActivity.class);
//        intent.putExtra("contact_name", contactName);
//        intent.putExtra("contact_phone", contactPhone);
//        intent.putExtra("preset_type", presetType);
//        startActivity(intent);
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        loadData();
//    }
//
//    void loadData() {
//        double balance = db.getBalance(contactId);
//        if (balance > 0.009) {
//            tvNetBalance.setText("Net Balance Due: " + CurrencyUtils.format(balance));
//            tvNetBalance.setTextColor(0xFFC62828);
//        } else if (balance < -0.009) {
//            tvNetBalance.setText("Net Balance Advance  " + CurrencyUtils.format(Math.abs(balance)));
//            tvNetBalance.setTextColor(0xFF2E7D32);
//        } else {
//            tvNetBalance.setText("Net Balance: ₹0");
//            tvNetBalance.setTextColor(0xFF888888);
//        }
//        btnSendReminder.setVisibility(balance > 0.009 ? View.VISIBLE : View.GONE);
//
//        List<TransactionEntry> transactions = db.getTransactionsForContact(contactId);
//        adapter = new TransactionAdapter(transactions, db, this);
//        rvTransactions.setAdapter(adapter);
//        if (adapter.getItemCount() > 0) {
//            rvTransactions.scrollToPosition(adapter.getItemCount() - 1);
//        }
//    }
//
//    void checkPermissionAndSendReminder() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.SEND_SMS}, REQ_SEND_SMS_REMINDER);
//        } else {
//            sendReminderNow();
//        }
//    }
//
//    void sendReminderNow() {
//        double balance = db.getBalance(contactId);
//        try {
//            SmsUtils.sendReminderSms(contactPhone, balance);
//            Toast.makeText(this, "Reminder sent", Toast.LENGTH_SHORT).show();
//        } catch (Exception e) {
//            Toast.makeText(this, "SMS failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            if (requestCode == REQ_SEND_SMS_DELETE) {
//                sendDeletionSmsNow();
//            } else if (requestCode == REQ_SEND_SMS_REMINDER) {
//                sendReminderNow();
//            }
//        }
//    }
//    @Override
//    public void onTransactionDeleted(TransactionEntry entry) {
//        loadData();
//        Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
//
//        pendingAmount = entry.amount;
//        pendingEntryDate = entry.entryDate;
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.SEND_SMS}, REQ_SEND_SMS_DELETE);
//            return;
//        }
//
//        sendDeletionSmsNow();
//    }
//
//
//
//    void sendDeletionSmsNow() {
//        double updatedBalance = db.getBalance(contactId);
//        try {
//            SmsUtils.sendDeletionSms(contactPhone, pendingAmount, updatedBalance, pendingEntryDate);
//        } catch (Exception e) {
//            Toast.makeText(this, "SMS failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
//        }
//    }
//
//}
//

package com.kumud.ledgerapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContactDetailActivity extends AppCompatActivity implements TransactionAdapter.OnTransactionDeletedListener {

    private static final int REQ_SEND_SMS_DELETE = 10;
    private static final int REQ_SEND_SMS_REMINDER = 11;

    DBHelper db;
    long contactId;
    String contactName;
    String contactPhone; // currently active phone, refreshed on loadData
    TextView tvNetBalance, tvContactPhone, btnSendReminder;
    RecyclerView rvTransactions;
    TransactionAdapter adapter;

    double pendingAmount;
    long pendingEntryDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_detail);

        db = new DBHelper(this);

        contactId = getIntent().getLongExtra("contact_id", -1);
        contactName = getIntent().getStringExtra("contact_name");

        TextView tvContactName = findViewById(R.id.tvContactName);
        tvContactPhone = findViewById(R.id.tvContactPhone);
        tvNetBalance = findViewById(R.id.tvNetBalance);
        rvTransactions = findViewById(R.id.rvTransactions);
        Button btnGivenTab = findViewById(R.id.btnGivenTab);
        Button btnReceivedTab = findViewById(R.id.btnReceivedTab);
        btnSendReminder = findViewById(R.id.btnSendReminder);

        tvContactName.setText(contactName);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        tvContactPhone.setOnClickListener(v -> showNumberDialog());
        btnGivenTab.setOnClickListener(v -> openAddEntry("CREDIT"));
        btnReceivedTab.setOnClickListener(v -> openAddEntry("DEBIT"));
        btnSendReminder.setOnClickListener(v -> checkPermissionAndSendReminder());

        loadData();
    }

    void openAddEntry(String presetType) {
        Intent intent = new Intent(ContactDetailActivity.this, AddEntryActivity.class);
        intent.putExtra("contact_id", contactId);
        intent.putExtra("contact_name", contactName);
        intent.putExtra("contact_phone", contactPhone);
        intent.putExtra("preset_type", presetType);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    void loadData() {
        contactPhone = db.getActivePhone(contactId);
        tvContactPhone.setText("▾ " + (contactPhone != null ? contactPhone : "no number"));

        double balance = db.getBalance(contactId);
        if (balance > 0.009) {
            tvNetBalance.setText("Net Balance Due " + CurrencyUtils.format(balance));
            tvNetBalance.setTextColor(0xFFC62828);
        } else if (balance < -0.009) {
            tvNetBalance.setText("Net Balance Advance " + CurrencyUtils.format(Math.abs(balance)));
            tvNetBalance.setTextColor(0xFF2E7D32);
        } else {
            tvNetBalance.setText("₹0");
            tvNetBalance.setTextColor(0xFF888888);
        }
        btnSendReminder.setVisibility(balance > 0.009 ? View.VISIBLE : View.GONE);

        List<TransactionEntry> transactions = db.getTransactionsForContact(contactId);
        adapter = new TransactionAdapter(transactions, db, this);
        rvTransactions.setAdapter(adapter);
        if (adapter.getItemCount() > 0) {
            rvTransactions.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    // ---- Number management ----

    void showNumberDialog() {
        List<String> phones = db.getPhonesForContact(contactId);
        if (phones.isEmpty()) {
            Toast.makeText(this, "No numbers saved", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] options = new String[phones.size()];
        for (int i = 0; i < phones.size(); i++) {
            options[i] = phones.get(i).equals(contactPhone) ? phones.get(i) + "  (active)" : phones.get(i);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select number for " + contactName)
                .setItems(options, (dialog, which) -> {
                    String chosen = phones.get(which);
                    db.setActivePhone(contactId, chosen);
                    Toast.makeText(this, "Active number updated", Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .show();
    }

    // ---- Reminder SMS ----

    void checkPermissionAndSendReminder() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, REQ_SEND_SMS_REMINDER);
        } else {
            sendReminderNow();
        }
    }

    void sendReminderNow() {
        double balance = db.getBalance(contactId);
        try {
            SmsUtils.sendReminderSms(contactPhone, balance);
            Toast.makeText(this, "Reminder sent", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "SMS failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ---- Delete entry SMS ----

    @Override
    public void onTransactionDeleted(TransactionEntry entry) {
        // Only recompute closed-segment boundaries if the deleted entry was still "open" (live).
        // If it was already a closed/settled entry, leave the frozen balance untouched.
        if (!entry.closed) {
            db.recomputeClosedFlags(contactId);
        }

        loadData();
        Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();

        pendingAmount = entry.amount;
        pendingEntryDate = entry.entryDate;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, REQ_SEND_SMS_DELETE);
            return;
        }

        sendDeletionSmsNow();
    }

    void sendDeletionSmsNow() {
        double updatedBalance = db.getBalance(contactId);
        try {
            SmsUtils.sendDeletionSms(contactPhone, pendingAmount, updatedBalance, pendingEntryDate);
        } catch (Exception e) {
            Toast.makeText(this, "SMS failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQ_SEND_SMS_DELETE) {
                sendDeletionSmsNow();
            } else if (requestCode == REQ_SEND_SMS_REMINDER) {
                sendReminderNow();
            }
        }
    }
}