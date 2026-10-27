package com.example.quanlychitieu.model;

public class ChatMessage {
    public static final String SENDER_USER = "USER";
    public static final String SENDER_BOT = "BOT";

    private String id;
    private String sender;
    private String content;
    private long timestamp;
    private boolean pendingConfirmation;
    private Transaction proposedTransaction;
    private boolean confirmed;
    private boolean cancelled;

    public ChatMessage(String sender, String content) {
        this.id = String.valueOf(System.currentTimeMillis() + Math.random());
        this.sender = sender;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.pendingConfirmation = false;
        this.proposedTransaction = null;
        this.confirmed = false;
        this.cancelled = false;
    }

    public ChatMessage(String sender, String content, Transaction proposedTransaction) {
        this(sender, content);
        this.pendingConfirmation = (proposedTransaction != null);
        this.proposedTransaction = proposedTransaction;
    }

    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isPendingConfirmation() {
        return pendingConfirmation;
    }

    public void setPendingConfirmation(boolean pendingConfirmation) {
        this.pendingConfirmation = pendingConfirmation;
    }

    public Transaction getProposedTransaction() {
        return proposedTransaction;
    }

    public void setProposedTransaction(Transaction proposedTransaction) {
        this.proposedTransaction = proposedTransaction;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isUser() {
        return SENDER_USER.equals(sender);
    }
}
