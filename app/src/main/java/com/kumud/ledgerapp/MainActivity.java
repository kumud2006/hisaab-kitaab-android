package com.kumud.ledgerapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.net.Uri;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements ContactAdapter.OnContactDeletedListener {

    DBHelper db;
    RecyclerView rvContacts;
    ContactAdapter adapter;
    TextView tvNetBalanceTotal;
    private static final int REQ_READ_CONTACTS_SYNC = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBHelper(this);
        rvContacts = findViewById(R.id.rvContacts);
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        tvNetBalanceTotal = findViewById(R.id.tvNetBalanceTotal);
        SwipeRefreshLayout swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(() -> {
            checkPermissionAndSyncNames();
            swipeRefresh.setRefreshing(false);
        });
        List<ContactBalance> contacts = db.getAllContactsWithBalance();
        adapter = new ContactAdapter(contacts, db, this);
        rvContacts.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.etSearch);
        TextView btnClearSearch = findViewById(R.id.btnClearSearch);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                btnClearSearch.setVisibility(s.length() > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            btnClearSearch.setVisibility(android.view.View.GONE);
        });

        Button btnAddEntry = findViewById(R.id.btnAddEntry);
        btnAddEntry.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddEntryActivity.class)));
        updateNetBalance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.updateData(db.getAllContactsWithBalance());
        updateNetBalance();
    }

    void checkPermissionAndSyncNames() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, REQ_READ_CONTACTS_SYNC);
        } else {
            syncNamesFromContacts();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ_CONTACTS_SYNC
                && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            syncNamesFromContacts();
        }
    }

    void syncNamesFromContacts() {
        List<ContactBalance> savedContacts = db.getAllContactsRaw();
        int nameUpdates = 0;
        int addedNumbers = 0;
        int removedNumbers = 0;

        for (ContactBalance saved : savedContacts) {
            String currentDeviceName = lookupNameByPhone(saved.phone);
            if (currentDeviceName != null && !currentDeviceName.equals(saved.name)) {
                db.updateContactName(saved.id, currentDeviceName);
                nameUpdates++;
            }

            List<String> deviceNumbers = lookupAllNumbersByPhone(saved.phone);
            List<String> savedNumbers = db.getPhonesForContact(saved.id);

            // device has this contact at all? if lookup returned empty, skip removal logic entirely
            // (avoids wiping numbers just because the active number search itself failed to match)
            if (deviceNumbers.isEmpty()) continue;

            // add anything new
            for (String num : deviceNumbers) {
                if (!savedNumbers.contains(num)) {
                    db.addPhoneToContact(saved.id, num, false);
                    addedNumbers++;
                }
            }

            // remove anything saved that's no longer on the device,
            // but never remove the currently active number automatically
            String activePhone = db.getActivePhone(saved.id);
            for (String num : savedNumbers) {
                if (!deviceNumbers.contains(num) && !num.equals(activePhone)) {
                    db.removePhoneFromContact(saved.id, num);
                    removedNumbers++;
                }
            }
        }

        Toast.makeText(this, nameUpdates + " name(s), +" + addedNumbers + "/-" + removedNumbers + " number(s) synced", Toast.LENGTH_SHORT).show();
        adapter.updateData(db.getAllContactsWithBalance());
    }

    List<String> lookupAllNumbersByPhone(String knownPhone) {
        List<String> numbers = new java.util.ArrayList<>();

        // find the device contact id that owns this known number
        String[] idProjection = {ContactsContract.CommonDataKinds.Phone.CONTACT_ID};
        Cursor idCursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                idProjection,
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?",
                new String[]{knownPhone}, null);

        if (idCursor == null || !idCursor.moveToFirst()) {
            if (idCursor != null) idCursor.close();
            return numbers;
        }
        String deviceContactId = idCursor.getString(0);
        idCursor.close();

        // get every number belonging to that same device contact
        String[] numProjection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor numCursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                numProjection,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{deviceContactId}, null);

        if (numCursor != null) {
            while (numCursor.moveToNext()) {
                String num = numCursor.getString(0).replaceAll("\\s+", "");
                numbers.add(num);
            }
            numCursor.close();
        }
        return numbers;
    }
    String lookupNameByPhone(String phone) {
        String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
        String selection = ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?";
        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, new String[]{phone}, null);
        String name = null;
        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(0);
            cursor.close();
        }
        return name;
    }
    @Override
    public void onContactDeleted() {
        Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show();
        adapter.updateData(db.getAllContactsWithBalance());
        updateNetBalance();
    }

    void updateNetBalance() {
        double net = db.getNetBalance();
        if (net > 0.009) {
            tvNetBalanceTotal.setText("Net: " + CurrencyUtils.format(net));
            tvNetBalanceTotal.setTextColor(0xFFC62828);
        } else if (net < -0.009) {
            tvNetBalanceTotal.setText("Net: " + CurrencyUtils.format(Math.abs(net)));
            tvNetBalanceTotal.setTextColor(0xFF2E7D32);
        } else {
            tvNetBalanceTotal.setText("Net: ₹0");
            tvNetBalanceTotal.setTextColor(0xFF888888);
        }
    }
}