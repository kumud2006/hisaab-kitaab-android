**Hisaab Kitaab**

An offline-first Android ledger app that replaces a local business's manual credit/debit bookkeeping. Transactions are tracked per contact in a WhatsApp-style chat interface, so non-technical users can record and read their accounts without any training.
Built end to end, from database schema design through deployment to the client.

**-------------------------------------------**

**Features**

**Fully offline:** works without internet; all data is stored locally on the device

**Chat-style transaction view:** credit and debit entries appear as message bubbles grouped under date headers, which feels familiar to non-technical users

**Real-time balance calculation:** each contact's running balance updates instantly across unlimited transaction history

**Soft delete and recovery:** deleted entries are flagged rather than removed, so they can be restored

**Contacts integration:** pull people from the phone's contacts through the Android Contacts API instead of typing them in

**Automated SMS notifications:** send transaction messages to contacts using SmsManager

**-------------------------------------------**

**Tech Stack**

**Platform**	Android

**Language**	Java

**UI**	XML layouts

**Database**	SQLite (3-table relational schema)

**Android APIs**	Contacts API, SmsManager

**Build**	Gradle (Kotlin DSL)
-------------------------------------------

**Project Structure**

app/src/main/

├── java/com/kumud/ledgerapp/

│   ├── AddEntryActivity.java

│   ├── ContactAdapter.java

│   ├── ContactBalance.java

│   ├── ContactDetailActivity.java

│   └── ...

├── res/

│   ├── layout/

│   ├── drawable/

│   └── values/

└── AndroidManifest.xml

**-------------------------------------------**

**Design Highlights**

**Relational schema:** a 3-table SQLite design that supports soft-delete recovery and computes balances from the full transaction history rather than storing a value that can drift out of sync

**Chat UI:** a RecyclerView-based adapter renders entries as credit/debit bubbles with date headers

**Built for a real user:** designed around how the shop owner already kept accounts, so the app matches an existing workflow
