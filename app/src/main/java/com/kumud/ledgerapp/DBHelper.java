package com.kumud.ledgerapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "ledger.db";
    private static final int DB_VERSION = 5;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE contacts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "phone TEXT, " +
                "deleted INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE contact_phones (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "contact_id INTEGER, " +
                "phone TEXT UNIQUE, " +
                "is_active INTEGER DEFAULT 0, " +
                "FOREIGN KEY(contact_id) REFERENCES contacts(id))");

        db.execSQL("CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "contact_id INTEGER, " +
                "amount REAL, " +
                "type TEXT, " +
                "note TEXT, " +
                "timestamp INTEGER, " +
                "entry_date INTEGER, " +
                "deleted INTEGER DEFAULT 0, " +
                "closed INTEGER DEFAULT 0, " +
                "FOREIGN KEY(contact_id) REFERENCES contacts(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN entry_date INTEGER");
            db.execSQL("UPDATE transactions SET entry_date = timestamp WHERE entry_date IS NULL");
        }
        if (oldV < 3) {
            db.execSQL("ALTER TABLE contacts ADD COLUMN deleted INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE transactions ADD COLUMN deleted INTEGER DEFAULT 0");
        }
        if (oldV < 4) {
            db.execSQL("CREATE TABLE contact_phones (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "contact_id INTEGER, " +
                    "phone TEXT UNIQUE, " +
                    "is_active INTEGER DEFAULT 0, " +
                    "FOREIGN KEY(contact_id) REFERENCES contacts(id))");

            Cursor cursor = db.rawQuery("SELECT id, phone FROM contacts", null);
            while (cursor.moveToNext()) {
                long contactId = cursor.getLong(0);
                String phone = cursor.getString(1);
                if (phone != null && !phone.isEmpty()) {
                    ContentValues values = new ContentValues();
                    values.put("contact_id", contactId);
                    values.put("phone", phone);
                    values.put("is_active", 1);
                    db.insert("contact_phones", null, values);
                }
            }
            cursor.close();
        }
        if (oldV < 5) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN closed INTEGER DEFAULT 0");

            Cursor cCursor = db.rawQuery("SELECT id FROM contacts", null);
            List<Long> contactIds = new ArrayList<>();
            while (cCursor.moveToNext()) contactIds.add(cCursor.getLong(0));
            cCursor.close();

            for (long contactId : contactIds) {
                recomputeClosedFlagsInternal(db, contactId);
            }
        }
    }

    // ---- Contact + phone management ----

    public long addOrGetContact(String name, String phone) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT contact_id FROM contact_phones WHERE phone = ?", new String[]{phone});
        if (cursor.moveToFirst()) {
            long contactId = cursor.getLong(0);
            cursor.close();
            ContentValues restore = new ContentValues();
            restore.put("deleted", 0);
            db.update("contacts", restore, "id = ?", new String[]{String.valueOf(contactId)});
            return contactId;
        }
        cursor.close();

        ContentValues contactValues = new ContentValues();
        contactValues.put("name", name);
        contactValues.put("phone", phone);
        contactValues.put("deleted", 0);
        long contactId = db.insert("contacts", null, contactValues);

        ContentValues phoneValues = new ContentValues();
        phoneValues.put("contact_id", contactId);
        phoneValues.put("phone", phone);
        phoneValues.put("is_active", 1);
        db.insert("contact_phones", null, phoneValues);

        return contactId;
    }

    public String getActivePhone(long contactId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT phone FROM contact_phones WHERE contact_id = ? AND is_active = 1 LIMIT 1",
                new String[]{String.valueOf(contactId)});
        String phone = null;
        if (cursor.moveToFirst()) phone = cursor.getString(0);
        cursor.close();
        return phone;
    }

    public List<String> getPhonesForContact(long contactId) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT phone FROM contact_phones WHERE contact_id = ? ORDER BY is_active DESC, id ASC",
                new String[]{String.valueOf(contactId)});
        while (cursor.moveToNext()) list.add(cursor.getString(0));
        cursor.close();
        return list;
    }

    public void addPhoneToContact(long contactId, String phone, boolean makeActive) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor existing = db.rawQuery("SELECT id FROM contact_phones WHERE phone = ?", new String[]{phone});
        if (existing.moveToFirst()) {
            existing.close();
            if (makeActive) setActivePhone(contactId, phone);
            return;
        }
        existing.close();

        ContentValues values = new ContentValues();
        values.put("contact_id", contactId);
        values.put("phone", phone);
        values.put("is_active", makeActive ? 1 : 0);
        db.insert("contact_phones", null, values);

        if (makeActive) setActivePhone(contactId, phone);
    }

    public void removePhoneFromContact(long contactId, String phone) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("contact_phones", "contact_id = ? AND phone = ?",
                new String[]{String.valueOf(contactId), phone});
    }

    public void setActivePhone(long contactId, String phone) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues clearActive = new ContentValues();
        clearActive.put("is_active", 0);
        db.update("contact_phones", clearActive, "contact_id = ?", new String[]{String.valueOf(contactId)});

        ContentValues setActive = new ContentValues();
        setActive.put("is_active", 1);
        db.update("contact_phones", setActive, "contact_id = ? AND phone = ?",
                new String[]{String.valueOf(contactId), phone});

        ContentValues updateCached = new ContentValues();
        updateCached.put("phone", phone);
        db.update("contacts", updateCached, "id = ?", new String[]{String.valueOf(contactId)});
    }

    // ---- Transactions ----

    public void addTransaction(long contactId, double amount, String type, String note, long entryDate) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("contact_id", contactId);
        values.put("amount", amount);
        values.put("type", type);
        values.put("note", note);
        values.put("timestamp", System.currentTimeMillis());
        values.put("entry_date", entryDate);
        values.put("deleted", 0);
        values.put("closed", 0);
        db.insert("transactions", null, values);

        recomputeClosedFlags(contactId);
    }

    public double getBalance(long contactId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(CASE WHEN type='CREDIT' THEN amount ELSE -amount END) " +
                        "FROM transactions WHERE contact_id = ? AND deleted = 0 AND closed = 0",
                new String[]{String.valueOf(contactId)});
        double balance = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            balance = cursor.getDouble(0);
        }
        cursor.close();
        return balance;
    }

    public double getNetBalance() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(CASE WHEN t.type='CREDIT' THEN t.amount ELSE -t.amount END) " +
                        "FROM transactions t JOIN contacts c ON t.contact_id = c.id " +
                        "WHERE t.deleted = 0 AND t.closed = 0 AND c.deleted = 0", null);
        double net = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            net = cursor.getDouble(0);
        }
        cursor.close();
        return net;
    }

    public long getLastEntryDate(long contactId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT MAX(entry_date) FROM transactions WHERE contact_id = ? AND deleted = 0",
                new String[]{String.valueOf(contactId)});
        long last = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) last = cursor.getLong(0);
        cursor.close();
        return last;
    }

    public List<ContactBalance> getAllContactsWithBalance() {
        List<ContactBalance> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, phone FROM contacts WHERE deleted = 0", null);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            String phone = cursor.getString(2);
            double balance = getBalance(id);
            long lastTime = getLastEntryDate(id);
            list.add(new ContactBalance(id, name, phone, balance, lastTime));
        }
        cursor.close();
        list.sort((a, b) -> Long.compare(b.lastTimestamp, a.lastTimestamp));
        return list;
    }

    public List<TransactionEntry> getTransactionsForContact(long contactId) {
        List<TransactionEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, amount, type, note, timestamp, entry_date, closed FROM transactions " +
                        "WHERE contact_id = ? AND deleted = 0 ORDER BY timestamp ASC, id ASC",
                new String[]{String.valueOf(contactId)});
        while (cursor.moveToNext()) {
            list.add(new TransactionEntry(
                    cursor.getLong(0), cursor.getDouble(1), cursor.getString(2),
                    cursor.getString(3), cursor.getLong(4), cursor.getLong(5),
                    cursor.getInt(6) == 1));
        }
        cursor.close();
        return list;
    }

    // ---- Closed-segment tracking ----

    public void recomputeClosedFlags(long contactId) {
        SQLiteDatabase db = getWritableDatabase();
        recomputeClosedFlagsInternal(db, contactId);
    }

    private void recomputeClosedFlagsInternal(SQLiteDatabase db, long contactId) {
        Cursor cursor = db.rawQuery(
                "SELECT id, amount, type FROM transactions WHERE contact_id = ? AND deleted = 0 ORDER BY timestamp ASC, id ASC",
                new String[]{String.valueOf(contactId)});

        List<Long> ids = new ArrayList<>();
        List<Double> signed = new ArrayList<>();
        while (cursor.moveToNext()) {
            ids.add(cursor.getLong(0));
            double amt = cursor.getDouble(1);
            String type = cursor.getString(2);
            signed.add(type.equals("CREDIT") ? amt : -amt);
        }
        cursor.close();

        boolean[] closed = new boolean[ids.size()];
        double running = 0;
        int segmentStart = 0;
        for (int i = 0; i < ids.size(); i++) {
            running += signed.get(i);
            if (Math.abs(running) < 0.009) {
                for (int j = segmentStart; j <= i; j++) closed[j] = true;
                segmentStart = i + 1;
            }
        }

        for (int i = 0; i < ids.size(); i++) {
            ContentValues values = new ContentValues();
            values.put("closed", closed[i] ? 1 : 0);
            db.update("transactions", values, "id = ?", new String[]{String.valueOf(ids.get(i))});
        }
    }

    public void softDeleteTransaction(long id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("deleted", 1);
        db.update("transactions", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void restoreTransaction(long id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("deleted", 0);
        db.update("transactions", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void permanentlyDeleteTransaction(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("transactions", "id = ?", new String[]{String.valueOf(id)});
    }

    public void softDeleteContact(long contactId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("deleted", 1);
        db.update("contacts", values, "id = ?", new String[]{String.valueOf(contactId)});
    }

    public void restoreContact(long contactId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("deleted", 0);
        db.update("contacts", values, "id = ?", new String[]{String.valueOf(contactId)});
    }

    public void permanentlyDeleteContact(long contactId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("transactions", "contact_id = ?", new String[]{String.valueOf(contactId)});
        db.delete("contact_phones", "contact_id = ?", new String[]{String.valueOf(contactId)});
        db.delete("contacts", "id = ?", new String[]{String.valueOf(contactId)});
    }

    public List<ContactBalance> getDeletedContacts() {
        List<ContactBalance> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, phone FROM contacts WHERE deleted = 1", null);
        while (cursor.moveToNext()) {
            list.add(new ContactBalance(cursor.getLong(0), cursor.getString(1), cursor.getString(2), 0, 0));
        }
        cursor.close();
        return list;
    }

    public List<DeletedTransactionInfo> getDeletedTransactions() {
        List<DeletedTransactionInfo> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT t.id, t.contact_id, c.name, c.phone, t.amount, t.type, t.entry_date, t.closed " +
                        "FROM transactions t JOIN contacts c ON t.contact_id = c.id " +
                        "WHERE t.deleted = 1 ORDER BY t.id DESC", null);
        while (cursor.moveToNext()) {
            list.add(new DeletedTransactionInfo(
                    cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getString(3),
                    cursor.getDouble(4), cursor.getString(5), cursor.getLong(6),
                    cursor.getInt(7) == 1));
        }
        cursor.close();
        return list;
    }

    public void updateContactName(long contactId, String newName) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newName);
        db.update("contacts", values, "id = ?", new String[]{String.valueOf(contactId)});
    }

    public List<ContactBalance> getAllContactsRaw() {
        List<ContactBalance> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, phone FROM contacts WHERE deleted = 0", null);
        while (cursor.moveToNext()) {
            list.add(new ContactBalance(cursor.getLong(0), cursor.getString(1), cursor.getString(2), 0, 0));
        }
        cursor.close();
        return list;
    }
}