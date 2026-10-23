package com.kumud.ledgerapp;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecycleBinContactAdapter extends RecyclerView.Adapter<RecycleBinContactAdapter.ViewHolder> {

    public interface OnChangeListener {
        void onChanged();
    }

    private final List<ContactBalance> contacts;
    private final DBHelper db;
    private final OnChangeListener listener;

    public RecycleBinContactAdapter(List<ContactBalance> contacts, DBHelper db, OnChangeListener listener) {
        this.contacts = contacts;
        this.db = db;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recycle_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactBalance c = contacts.get(position);
        holder.tvName.setText(c.name);
        holder.tvPhone.setText(c.phone);

        holder.btnRestore.setOnClickListener(v -> {
            db.restoreContact(c.id);
            Toast(v, "Contact restored");
            if (listener != null) listener.onChanged();
        });

        holder.btnDeleteForever.setOnClickListener(v ->
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Delete Forever")
                        .setMessage("Permanently delete " + c.name + " and all their entries? This cannot be undone.")
                        .setPositiveButton("Delete Forever", (dialog, which) -> {
                            db.permanentlyDeleteContact(c.id);
                            Toast(v, "Permanently deleted");
                            if (listener != null) listener.onChanged();
                        })
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    private void Toast(View v, String msg) {
        android.widget.Toast.makeText(v.getContext(), msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone;
        android.widget.Button btnRestore, btnDeleteForever;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            btnRestore = itemView.findViewById(R.id.btnRestore);
            btnDeleteForever = itemView.findViewById(R.id.btnDeleteForever);
        }
    }
}