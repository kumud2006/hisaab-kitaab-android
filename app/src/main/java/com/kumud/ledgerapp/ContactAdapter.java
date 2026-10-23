package com.kumud.ledgerapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    public interface OnContactDeletedListener {
        void onContactDeleted();
    }

    private List<ContactBalance> contacts;
    private List<ContactBalance> fullList;
    private final DBHelper db;
    private final OnContactDeletedListener deleteListener;
    private final int[] palette = {0xFF4CAF50, 0xFF2196F3, 0xFFFF9800, 0xFF9C27B0, 0xFFE91E63, 0xFF009688};

    public ContactAdapter(List<ContactBalance> contacts, DBHelper db, OnContactDeletedListener deleteListener) {
        this.contacts = contacts;
        this.fullList = new ArrayList<>(contacts);
        this.db = db;
        this.deleteListener = deleteListener;
    }

    public void updateData(List<ContactBalance> newList) {
        this.fullList = new ArrayList<>(newList);
        this.contacts = newList;
        notifyDataSetChanged();
    }

    public void filter(String query) {
        String q = query.toLowerCase().trim();
        if (q.isEmpty()) {
            contacts = new ArrayList<>(fullList);
        } else {
            List<ContactBalance> filtered = new ArrayList<>();
            for (ContactBalance c : fullList) {
                if (c.name.toLowerCase().contains(q)) filtered.add(c);
            }
            contacts = filtered;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactBalance c = contacts.get(position);
        holder.tvName.setText(c.name);

        String initial = c.name.isEmpty() ? "?" : c.name.substring(0, 1).toUpperCase();
        holder.tvAvatar.setText(initial);
        int color = palette[Math.abs(c.name.hashCode()) % palette.length];
        holder.tvAvatar.getBackground().mutate().setTint(color);

        if (c.balance > 0.009) {
            holder.tvBalance.setText(CurrencyUtils.format(c.balance));
            holder.tvBalance.setTextColor(0xFFC62828);
        } else if (c.balance < -0.009) {
            holder.tvBalance.setText(CurrencyUtils.format(Math.abs(c.balance)));
            holder.tvBalance.setTextColor(0xFF2E7D32);
        } else {
            holder.tvBalance.setText("₹0");
            holder.tvBalance.setTextColor(0xFF888888);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ContactDetailActivity.class);
            intent.putExtra("contact_id", c.id);
            intent.putExtra("contact_name", c.name);
            intent.putExtra("contact_phone", c.phone);
            v.getContext().startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Customer")
                    .setMessage("Permanently delete " + c.name + " and all their entries? This cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        db.permanentlyDeleteContact(c.id);
                        if (deleteListener != null) deleteListener.onContactDeleted();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvBalance;
        ViewHolder(View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvBalance = itemView.findViewById(R.id.tvBalance);
        }
    }
}