package com.kumud.ledgerapp;

public class ContactBalance {
    public long id;
    public String name;
    public String phone;
    public double balance;
    public long lastTimestamp;

    public ContactBalance(long id, String name, String phone, double balance, long lastTimestamp) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.balance = balance;
        this.lastTimestamp = lastTimestamp;
    }
}