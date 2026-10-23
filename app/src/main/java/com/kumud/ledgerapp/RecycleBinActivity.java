package com.kumud.ledgerapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RecycleBinActivity extends AppCompatActivity {

    DBHelper db;
    RecyclerView rvRecycleBin;
    TextView tvEmptyMessage;
    Button btnTabContacts, btnTabEntries;
    boolean showingContacts = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle_bin);

        db = new DBHelper(this);
        rvRecycleBin = findViewById(R.id.rvRecycleBin);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        btnTabContacts = findViewById(R.id.btnTabContacts);
        btnTabEntries = findViewById(R.id.btnTabEntries);
        rvRecycleBin.setLayoutManager(new LinearLayoutManager(this));

        btnTabContacts.setOnClickListener(v -> {
            showingContacts = true;
            loadData();
        });
        btnTabEntries.setOnClickListener(v -> {
            showingContacts = false;
            loadData();
        });

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    void loadData() {
        if (showingContacts) {
            btnTabContacts.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6A4C93));
            btnTabEntries.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFBBBBBB));

            java.util.List<ContactBalance> deletedContacts = db.getDeletedContacts();
            rvRecycleBin.setAdapter(new RecycleBinContactAdapter(deletedContacts, db, this::loadData));
            tvEmptyMessage.setVisibility(deletedContacts.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            btnTabEntries.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6A4C93));
            btnTabContacts.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFBBBBBB));

            java.util.List<DeletedTransactionInfo> deletedEntries = db.getDeletedTransactions();
            rvRecycleBin.setAdapter(new RecycleBinTransactionAdapter(deletedEntries, db, this::loadData));
            tvEmptyMessage.setVisibility(deletedEntries.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}