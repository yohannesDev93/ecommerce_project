package models;

import java.time.LocalDateTime;

public class Message {
    private int id;
    private int customerId;
    private String customerName;
    private String customerEmail;
    private String subject;
    private String message;
    private String adminReply;
    private String status; // UNREAD, READ, REPLIED
    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;
    
    // Constructors
    public Message() {}
    
    public Message(int customerId, String customerName, String customerEmail, 
                   String subject, String message) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.subject = subject;
        this.message = message;
        this.status = "UNREAD";
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getRepliedAt() { return repliedAt; }
    public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }
    
    // Helper methods
    public boolean hasReply() {
        return adminReply != null && !adminReply.trim().isEmpty();
    }
    
    public String getStatusWithIcon() {
        switch(status.toUpperCase()) {
            case "UNREAD": return "📨 Unread";
            case "READ": return "👁️ Read";
            case "REPLIED": return "✅ Replied";
            default: return status;
        }
    }
    
    public String getFormattedCreatedAt() {
        if (createdAt != null) {
            return createdAt.toString();
        }
        return "";
    }
    
    public String getFormattedRepliedAt() {
        if (repliedAt != null) {
            return repliedAt.toString();
        }
        return "Not replied yet";
    }
}