package com.kumud.ledgerapp;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecycleBinTransactionAdapter extends RecyclerView.Adapter<RecycleBinTransactionAdapter.ViewHolder> {

    public interface OnChangeListener {
        void onChanged();
    }

    private final List<DeletedTransactionInfo> transactions;
    private final DBHelper db;
    private final OnChangeListener listener;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public RecycleBinTransactionAdapter(List<DeletedTransactionInfo> transactions, DBHelper db, OnChangeListener listener) {
        this.transactions = transactions;
        this.db = db;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recycle_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeletedTransactionInfo t = transactions.get(position);
        holder.tvContactName.setText(t.contactName);
        String typeLabel = t.type.equals("CREDIT") ? "Given" : "Received";
        holder.tvDetails.setText(typeLabel + " " + CurrencyUtils.format(t.amount) + " on " + dateFmt.format(new Date(t.entryDate)));

        holder.btnRestore.setOnClickListener(v -> {
            db.restoreTransaction(t.id);
            if (!t.closed) {
                db.recomputeClosedFlags(t.contactId);
            }
            Toast.makeText(v.getContext(), "Entry restored", Toast.LENGTH_SHORT).show();
            if (listener != null) listener.onChanged();
        });

        holder.btnDeleteForever.setOnClickListener(v ->
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Delete Forever")
                        .setMessage("Permanently delete this entry? This cannot be undone.")
                        .setPositiveButton("Delete Forever", (dialog, which) -> {
                            db.permanentlyDeleteTransaction(t.id);
                            Toast.makeText(v.getContext(), "Permanently deleted", Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onChanged();
                        })
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContactName, tvDetails;
        Button btnRestore, btnDeleteForever;
        ViewHolder(View itemView) {
            super(itemView);
            tvContactName = itemView.findViewById(R.id.tvContactName);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            btnRestore = itemView.findViewById(R.id.btnRestore);
            btnDeleteForever = itemView.findViewById(R.id.btnDeleteForever);
        }
    }
}