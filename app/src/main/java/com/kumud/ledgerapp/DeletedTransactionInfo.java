package com.kumud.ledgerapp;

public class DeletedTransactionInfo {
    public long id;
    public long contactId;
    public String contactName;
    public String contactPhone;
    public double amount;
    public String type;
    public long entryDate;
    public boolean closed;

    public DeletedTransactionInfo(long id, long contactId, String contactName, String contactPhone,
                                  double amount, String type, long entryDate, boolean closed) {
        this.id = id;
        this.contactId = contactId;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.amount = amount;
        this.type = type;
        this.entryDate = entryDate;
        this.closed = closed;
    }
}