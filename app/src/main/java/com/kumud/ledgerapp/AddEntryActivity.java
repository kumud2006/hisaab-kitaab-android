package com.kumud.ledgerapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEntryActivity extends AppCompatActivity {

    private static final int REQ_PICK_CONTACT = 1;
    private static final int REQ_READ_CONTACTS = 2;
    private static final int REQ_SEND_SMS = 3;

    long selectedContactId = -1;
    String selectedName = "";
    String selectedPhone = "";
    String selectedType = null;
    long selectedDateMillis;

    DBHelper db;
    TextView tvSelectedContact, tvPresetType;
    EditText etAmount, etNote;
    CheckBox cbNotify;
    Button btnGiven, btnReceived, btnPickDate;
    View rowTypeTabs;

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_entry);
        db = new DBHelper(this);

        Button btnPickContact = findViewById(R.id.btnPickContact);
        Button btnSave = findViewById(R.id.btnSave);
        tvSelectedContact = findViewById(R.id.tvSelectedContact);
        tvPresetType = findViewById(R.id.tvPresetType);
        etAmount = findViewById(R.id.etAmount);
        etAmount.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;

                String digitsOnly = s.toString().replaceAll("[^\\d]", "");
                if (digitsOnly.isEmpty()) {
                    isEditing = false;
                    return;
                }

                String formatted = CurrencyUtils.formatPlain(Long.parseLong(digitsOnly));

                etAmount.removeTextChangedListener(this);
                etAmount.setText(formatted);
                etAmount.setSelection(formatted.length());
                etAmount.addTextChangedListener(this);

                isEditing = false;
            }
        });
        etNote = findViewById(R.id.etNote);
        cbNotify = findViewById(R.id.cbNotify);
        btnGiven = findViewById(R.id.btnGiven);
        btnReceived = findViewById(R.id.btnReceived);
        btnPickDate = findViewById(R.id.btnPickDate);
        rowTypeTabs = findViewById(R.id.rowTypeTabs);

        selectedDateMillis = normalizeToMidnight(System.currentTimeMillis());
        btnPickDate.setText("Date: " + dateFmt.format(new Date(selectedDateMillis)));

        long preContactId = getIntent().getLongExtra("contact_id", -1);
        String preName = getIntent().getStringExtra("contact_name");
        String prePhone = getIntent().getStringExtra("contact_phone");
        if (preContactId != -1 && preName != null && prePhone != null) {
            selectedContactId = preContactId;
            selectedName = preName;
            selectedPhone = prePhone;
            btnPickContact.setVisibility(View.GONE);
            updateSelectedContactDisplay();
        }

        String presetType = getIntent().getStringExtra("preset_type");
        if (presetType != null) {
            selectedType = presetType;
            rowTypeTabs.setVisibility(View.GONE);
            tvPresetType.setVisibility(View.VISIBLE);
            if (presetType.equals("CREDIT")) {
                tvPresetType.setText("Adding: Given");
                tvPresetType.setTextColor(0xFFC62828);
                etAmount.setTextColor(0xFFC62828);
            } else {
                tvPresetType.setText("Adding: Received");
                tvPresetType.setTextColor(0xFF2E7D32);
                etAmount.setTextColor(0xFF2E7D32);
            }
        }

        btnPickContact.setOnClickListener(v -> checkContactsPermissionAndPick());
        btnGiven.setOnClickListener(v -> selectGiven());
        btnReceived.setOnClickListener(v -> selectReceived());
        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> saveEntry());
    }

    long normalizeToMidnight(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    void showDatePicker() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_calendar, null);
        CalendarView calendarView = dialogView.findViewById(R.id.calendarView);
        calendarView.setDate(selectedDateMillis);
        calendarView.setMaxDate(System.currentTimeMillis());

        final long[] tempDate = {selectedDateMillis};
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            tempDate[0] = cal.getTimeInMillis();
        });

        new AlertDialog.Builder(this)
                .setTitle("Select Date")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    selectedDateMillis = tempDate[0];
                    btnPickDate.setText("Date: " + dateFmt.format(new Date(selectedDateMillis)));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void selectGiven() {
        selectedType = "CREDIT";
        btnGiven.setBackgroundTintList(ColorStateList.valueOf(0xFFC62828));
        btnGiven.setTextColor(Color.WHITE);
        btnReceived.setBackgroundTintList(ColorStateList.valueOf(0xFFE0E0E0));
        btnReceived.setTextColor(Color.BLACK);
        etAmount.setTextColor(0xFFC62828);
    }

    void selectReceived() {
        selectedType = "DEBIT";
        btnReceived.setBackgroundTintList(ColorStateList.valueOf(0xFF2E7D32));
        btnReceived.setTextColor(Color.WHITE);
        btnGiven.setBackgroundTintList(ColorStateList.valueOf(0xFFE0E0E0));
        btnGiven.setTextColor(Color.BLACK);
        etAmount.setTextColor(0xFF2E7D32);
    }

    void checkContactsPermissionAndPick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, REQ_READ_CONTACTS);
        } else {
            pickContact();
        }
    }

    void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, REQ_PICK_CONTACT);
    }

    void updateSelectedContactDisplay() {
        long contactId = (selectedContactId != -1) ? selectedContactId : db.addOrGetContact(selectedName, selectedPhone);
        double balance = db.getBalance(contactId);

        String balanceText;
        int color;
        if (balance > 0.009) {
            balanceText = CurrencyUtils.format(balance);
            color = 0xFFC62828; // red
        } else if (balance < -0.009) {
            balanceText = CurrencyUtils.format(Math.abs(balance));
            color = 0xFF2E7D32; // green
        } else {
            balanceText = "₹0";
            color = 0xFF888888; // grey
        }

        String display = selectedName + " (" + balanceText + ")";
        android.text.SpannableString spannable = new android.text.SpannableString(display);
        int start = selectedName.length() + 2; // after "Name ("
        int end = display.length() - 1;        // before ")"
        spannable.setSpan(new android.text.style.ForegroundColorSpan(color), start, end, 0);
        tvSelectedContact.setText(spannable);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_CONTACT && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            String[] projection = {
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };
            Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                selectedName = cursor.getString(0);
                selectedPhone = cursor.getString(1).replaceAll("\\s+", "");
                cursor.close();
                updateSelectedContactDisplay();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ_CONTACTS
                && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickContact();
        } else if (requestCode == REQ_SEND_SMS
                && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            doSaveAndMaybeSms(true);
        }
    }

    void saveEntry() {
        if (selectedPhone.isEmpty()) {
            Toast.makeText(this, "Please pick a contact first", Toast.LENGTH_SHORT).show();
            return;
        }
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedType == null) {
            Toast.makeText(this, "Select Given or Received", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cbNotify.isChecked() &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, REQ_SEND_SMS);
            return;
        }

        doSaveAndMaybeSms(cbNotify.isChecked());
    }

    void doSaveAndMaybeSms(boolean shouldSendSms) {
        double amount = Double.parseDouble(etAmount.getText().toString().replaceAll(",", "").trim());
        String note = etNote.getText().toString().trim();

        long contactId = (selectedContactId != -1) ? selectedContactId : db.addOrGetContact(selectedName, selectedPhone);
        db.addTransaction(contactId, amount, selectedType, note, selectedDateMillis);

        if (shouldSendSms) {
            double updatedBalance = db.getBalance(contactId);
            try {
                SmsUtils.sendEntryUpdateSms(selectedPhone, amount, selectedType, updatedBalance, selectedDateMillis, note);
            } catch (Exception e) {
                Toast.makeText(this, "SMS failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}