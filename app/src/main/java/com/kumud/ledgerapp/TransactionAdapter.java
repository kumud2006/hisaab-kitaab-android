package com.kumud.ledgerapp;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnTransactionDeletedListener {
        void onTransactionDeleted(TransactionEntry entry);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CREDIT = 1;
    private static final int TYPE_DEBIT = 2;
    private static final int TYPE_DIVIDER = 3;

    private static class DividerMarker {}

    private final List<Object> items = new ArrayList<>();
    private final List<Double> runningBalances = new ArrayList<>(); // aligned with items, null-ignored for non-bubbles
    private final DBHelper db;
    private final OnTransactionDeletedListener listener;
    private final SimpleDateFormat entryDateFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat headerFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public TransactionAdapter(List<TransactionEntry> transactions, DBHelper db, OnTransactionDeletedListener listener) {
        this.db = db;
        this.listener = listener;

        double runningOpen = 0;
        long lastHeaderDay = -1;

        for (int i = 0; i < transactions.size(); i++) {
            TransactionEntry t = transactions.get(i);
            long day = startOfDay(t.timestamp);
            if (day != lastHeaderDay) {
                items.add(headerText(t.timestamp));
                runningBalances.add(null);
                lastHeaderDay = day;
            }

            if (!t.closed) {
                runningOpen += t.type.equals("CREDIT") ? t.amount : -t.amount;
                items.add(t);
                runningBalances.add(runningOpen);
            } else {
                items.add(t);
                runningBalances.add(0.0); // closed entries always net to zero
            }

            boolean nextIsOpen = (i + 1 < transactions.size()) && !transactions.get(i + 1).closed;
            if (t.closed && nextIsOpen) {
                items.add(new DividerMarker());
                runningBalances.add(null);
                lastHeaderDay = -1;
            }
        }
    }

    private long startOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private String headerText(long millis) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        if (isSameDay(target, today)) return "Today";
        if (isSameDay(target, yesterday)) return "Yesterday";
        return headerFmt.format(new Date(millis));
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof String) return TYPE_HEADER;
        if (item instanceof DividerMarker) return TYPE_DIVIDER;
        return ((TransactionEntry) item).type.equals("CREDIT") ? TYPE_CREDIT : TYPE_DEBIT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_date_header, parent, false));
        } else if (viewType == TYPE_DIVIDER) {
            return new DividerViewHolder(inflater.inflate(R.layout.item_settled_divider, parent, false));
        } else {
            int layoutRes = (viewType == TYPE_CREDIT) ? R.layout.item_bubble_credit : R.layout.item_bubble_debit;
            return new BubbleViewHolder(inflater.inflate(layoutRes, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvHeaderDate.setText((String) item);
        } else if (holder instanceof BubbleViewHolder) {
            TransactionEntry t = (TransactionEntry) item;
            BubbleViewHolder bh = (BubbleViewHolder) holder;
            bh.tvAmount.setText(CurrencyUtils.format(t.amount));
            bh.tvDate.setText(entryDateFmt.format(new Date(t.entryDate)));
            bh.itemView.setAlpha(t.closed ? 0.35f : 1f);
            if (t.note != null && !t.note.trim().isEmpty()) {
                bh.tvNote.setText(t.note.trim());
                bh.tvNote.setVisibility(View.VISIBLE);
            } else {
                bh.tvNote.setVisibility(View.GONE);
            }

            Double bal = runningBalances.get(position);
            if (bal != null) {
                if (bal > 0.009) {
                    bh.tvRunningBalance.setText("DUE " + CurrencyUtils.format(bal));
                    bh.tvRunningBalance.setTextColor(0xFFC62828);
                } else if (bal < -0.009) {
                    bh.tvRunningBalance.setText("ADV " + CurrencyUtils.format(Math.abs(bal)));
                    bh.tvRunningBalance.setTextColor(0xFF2E7D32);
                } else {
                    bh.tvRunningBalance.setText("Settled");
                    bh.tvRunningBalance.setTextColor(0xFF999999);
                }
            }

            bh.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Delete Entry")
                        .setMessage("Delete this entry of " + CurrencyUtils.format(t.amount) + " dated "
                                + entryDateFmt.format(new Date(t.entryDate)) + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            db.permanentlyDeleteTransaction(t.id);
                            if (listener != null) listener.onTransactionDeleted(t);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderDate;
        HeaderViewHolder(View itemView) {
            super(itemView);
            tvHeaderDate = itemView.findViewById(R.id.tvHeaderDate);
        }
    }

    static class DividerViewHolder extends RecyclerView.ViewHolder {
        DividerViewHolder(View itemView) {
            super(itemView);
        }
    }

    static class BubbleViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount, tvDate, tvRunningBalance, tvNote;
        BubbleViewHolder(View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvRunningBalance = itemView.findViewById(R.id.tvRunningBalance);
            tvNote = itemView.findViewById(R.id.tvNote);
        }
    }
}