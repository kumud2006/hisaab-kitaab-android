package com.kumud.ledgerapp;

public class TransactionEntry {
    public long id;
    public double amount;
    public String type;
    public String note;
    public long timestamp;
    public long entryDate;
    public boolean closed;

    public TransactionEntry(long id, double amount, String type, String note, long timestamp, long entryDate, boolean closed) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.note = note;
        this.timestamp = timestamp;
        this.entryDate = entryDate;
        this.closed = closed;
    }
}