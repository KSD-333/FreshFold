package com.ankita.freshfold.ui.wallet;

public class Transaction {
    private String title;
    private String date;
    private String amount;
    private String emoji;
    private boolean isCredit;
    public long timestamp;

    public Transaction(String title, String date, String amount, String emoji, boolean isCredit) {
        this.title = title;
        this.date = date;
        this.amount = amount;
        this.emoji = emoji;
        this.isCredit = isCredit;
    }

    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getAmount() { return amount; }
    public String getEmoji() { return emoji; }
    public boolean isCredit() { return isCredit; }
}
