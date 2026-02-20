package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import models.Message;
import services.DatabaseService;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class CustomerHelpController implements Initializable {
    
    @FXML private Label customerInfoLabel;
    @FXML private Label statusLabel;
    @FXML private TextArea messageInput;
    @FXML private VBox messagesContainer;
    
    private DatabaseService databaseService;
    private int customerId;
    private String customerName;
    private String customerEmail;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("CustomerHelpController initialized");
        
        try {
            databaseService = new DatabaseService();
        } catch (Exception e) {
            System.err.println("Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void setCustomerInfo(int customerId, String customerName, String customerEmail) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        
        customerInfoLabel.setText("Customer: " + customerName);
        
        // Load all messages
        loadMessages();
    }
    
    private void loadMessages() {
        new Thread(() -> {
            try {
                List<Message> messages = databaseService.getCustomerMessages(customerId);
                
                Platform.runLater(() -> {
                    messagesContainer.getChildren().clear();
                    
                    if (messages.isEmpty()) {
                        // Show empty state
                        showEmptyState();
                        return;
                    }
                    
                    // Display all messages
                    for (Message msg : messages) {
                        // Show customer message
                        showCustomerMessage(msg);
                        
                        // Show admin reply if exists
                        if (msg.hasReply()) {
                            showAdminReply(msg);
                        }
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Error loading messages", e.getMessage());
                });
            }
        }).start();
    }
    
    private void showEmptyState() {
        VBox emptyBox = new VBox(10);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(40));
        
        Label icon = new Label("💬");
        icon.setStyle("-fx-font-size: 48px;");
        
        Label message = new Label("No messages yet");
        message.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
        
        Label hint = new Label("Send your first message below");
        hint.setStyle("-fx-text-fill: #999; -fx-font-size: 12px;");
        
        emptyBox.getChildren().addAll(icon, message, hint);
        messagesContainer.getChildren().add(emptyBox);
    }
    
    private void showCustomerMessage(Message msg) {
        HBox messageRow = new HBox();
        messageRow.setAlignment(Pos.CENTER_RIGHT);
        messageRow.setPadding(new Insets(5, 0, 5, 0));
        
        VBox messageBubble = new VBox(5);
        messageBubble.setStyle("-fx-background-color: #0084ff; -fx-background-radius: 15; " +
                             "-fx-padding: 10 15; -fx-max-width: 300;");
        
        // Message text
        Label messageText = new Label(msg.getMessage());
        messageText.setWrapText(true);
        messageText.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");
        
        // Time and status
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        
        String timeStr = msg.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11px;");
        
        // Status icon
        String statusIcon = "✓"; // Default
        if (msg.getStatus() != null) {
            if (msg.getStatus().equals("REPLIED")) {
                statusIcon = "✓✓";
            } else if (msg.getStatus().equals("READ")) {
                statusIcon = "👁️";
            }
        }
        
        Label statusLabel = new Label(statusIcon);
        statusLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11px;");
        
        footer.getChildren().addAll(timeLabel, statusLabel);
        messageBubble.getChildren().addAll(messageText, footer);
        messageRow.getChildren().add(messageBubble);
        
        messagesContainer.getChildren().add(messageRow);
    }
    
    private void showAdminReply(Message msg) {
        HBox messageRow = new HBox();
        messageRow.setAlignment(Pos.CENTER_LEFT);
        messageRow.setPadding(new Insets(5, 0, 5, 0));
        
        VBox messageBubble = new VBox(5);
        messageBubble.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 15; " +
                             "-fx-padding: 10 15; -fx-max-width: 300;");
        
        // Admin header
        HBox header = new HBox(5);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label adminIcon = new Label("👨‍💼");
        Label adminLabel = new Label("Admin");
        adminLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #555;");
        
        header.getChildren().addAll(adminIcon, adminLabel);
        
        // Reply text
        Label replyText = new Label(msg.getAdminReply());
        replyText.setWrapText(true);
        replyText.setStyle("-fx-font-size: 14px; -fx-text-fill: #000;");
        
        // Time
        String timeStr = "Now";
        if (msg.getRepliedAt() != null) {
            timeStr = msg.getRepliedAt().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        
        messageBubble.getChildren().addAll(header, replyText, timeLabel);
        messageRow.getChildren().add(messageBubble);
        
        messagesContainer.getChildren().add(messageRow);
    }
    
    @FXML
    private void handleSendMessage() {
        String message = messageInput.getText().trim();
        
        if (message.isEmpty()) {
            showStatus("Please enter a message", "orange");
            return;
        }
        
        // Clear input immediately
        messageInput.clear();
        showStatus("Sending...", "blue");
        
        // Save to database
        new Thread(() -> {
            try {
                boolean success = databaseService.saveCustomerMessage(
                    customerId, customerName, customerEmail, 
                    "Support Request", message
                );
                
                Platform.runLater(() -> {
                    if (success) {
                        showStatus("Message sent!", "green");
                        // Reload messages to show the new one
                        loadMessages();
                    } else {
                        showStatus("Failed to send", "red");
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showStatus("Error: " + e.getMessage(), "red");
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    private void showStatus(String text, String color) {
        statusLabel.setText(text);
        
        switch(color) {
            case "green":
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
                break;
            case "red":
                statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
                break;
            case "blue":
                statusLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 12px;");
                break;
            case "orange":
                statusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 12px;");
                break;
        }
        
        // Clear after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    if (statusLabel.getText().equals(text)) {
                        statusLabel.setText("");
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}