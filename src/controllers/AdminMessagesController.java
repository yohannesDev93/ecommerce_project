package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import models.Message;
import models.User;
import services.DatabaseService;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AdminMessagesController implements Initializable {
    
    @FXML private VBox messagesContainer;
    @FXML private Label unreadCountLabel;
    @FXML private Label totalCountLabel;
    @FXML private Label summaryLabel;
    
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField searchField;
    
    @FXML private Button sendMessageBtn;
    @FXML private Button refreshBtn;
    @FXML private Button closeBtn;
    
    private DatabaseService databaseService;
    private List<Message> allMessages = new ArrayList<>();
    private List<User> allCustomers = new ArrayList<>();
    
  
    
// In AdminMessagesController.java
// Remove or comment out the styleButtons() method call since buttons are already styled in FXML
//@Override
//public void initialize(URL location, ResourceBundle resources) {
//    System.out.println("=== AdminMessagesController INITIALIZED ===");
//    
//    try {
//        databaseService = new DatabaseService();
//        
//        // Debug: Check messages table
//        databaseService.debugMessagesTable();
//        
//        // Populate status filter
//        ObservableList<String> statusOptions = FXCollections.observableArrayList(
//            "ALL", "UNREAD", "READ", "REPLIED"
//        );
//        statusFilterCombo.setItems(statusOptions);
//        statusFilterCombo.getSelectionModel().selectFirst();
//        
//        // Load all customers for sending messages
////        loadAllCustomers();
//        
//        // Load messages
//        loadAllMessages();
//        
//        // Setup filter listeners
//        setupFilters();
//        
//        // REMOVE THIS LINE - buttons are already styled in FXML
//        // styleButtons();
//        
//    } catch (Exception e) {
//        System.err.println("Failed to initialize AdminMessagesController: " + e.getMessage());
//        e.printStackTrace();
//        showError("Initialization Error", "Could not initialize messages system");
//    }
//}
//
//// You can remove the styleButtons() method completely or keep it empty
//
//    
//  
//    
//    private void setupFilters() {
//        statusFilterCombo.getSelectionModel().selectedItemProperty().addListener(
//            (obs, oldVal, newVal) -> applyFilters()
//        );
//        
//        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
//            if (newVal.isEmpty() || newVal.length() > 2) {
//                applyFilters();
//            }
//        });
//    }
    
    
 // In AdminMessagesController.java - Fix initialization
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== AdminMessagesController INITIALIZED ===");
        
        try {
            databaseService = new DatabaseService();
            
            // Debug: Check messages table
            databaseService.debugMessagesTable();
            
            // Initialize ComboBox if not null (it will be null if FXML doesn't have it)
            if (statusFilterCombo != null) {
                ObservableList<String> statusOptions = FXCollections.observableArrayList(
                    "ALL", "UNREAD", "READ", "REPLIED"
                );
                statusFilterCombo.setItems(statusOptions);
                statusFilterCombo.getSelectionModel().selectFirst();
            } else {
                System.out.println("WARNING: statusFilterCombo is null - check FXML");
            }
            
            // Check for sendMessageBtn
            if (sendMessageBtn == null) {
                System.out.println("WARNING: sendMessageBtn is null - check FXML");
            }
            
            // Load all customers for sending messages
            loadAllCustomers();
            
            // Load messages
            loadAllMessages();
            
            // Setup filter listeners
            setupFilters();
            
        } catch (Exception e) {
            System.err.println("Failed to initialize AdminMessagesController: " + e.getMessage());
            e.printStackTrace();
            showError("Initialization Error", "Could not initialize messages system");
        }
    }

    // FIXED: Add missing handleSendMessage method
//    @FXML
//    private void handleSendMessage() {
//        System.out.println("Send Message button clicked");
//        showNewMessageDialog();
//    }

    // FIXED: Update setupFilters to handle null ComboBox
    private void setupFilters() {
        if (statusFilterCombo != null) {
            statusFilterCombo.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> applyFilters()
            );
        }
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty() || newVal.length() > 2) {
                applyFilters();
            }
        });
    }
    private void loadAllCustomers() {
        new Thread(() -> {
            try {
                allCustomers = databaseService.getAllCustomers();
                System.out.println("DEBUG: Loaded " + allCustomers.size() + " customers");
            } catch (Exception e) {
                System.err.println("ERROR loading customers: " + e.getMessage());
            }
        }).start();
    }
    
    private void loadAllMessages() {
        System.out.println("Loading all messages from database...");
        
        // Show loading state
        showLoadingState();
        
        new Thread(() -> {
            try {
                allMessages = databaseService.getAllMessages();
                System.out.println("DEBUG: Retrieved " + allMessages.size() + " messages from database");
                
                if (!allMessages.isEmpty()) {
                    System.out.println("DEBUG: First message details:");
                    Message firstMsg = allMessages.get(0);
                    System.out.println("  ID: " + firstMsg.getId());
                    System.out.println("  Customer: " + firstMsg.getCustomerName());
                    System.out.println("  Subject: " + firstMsg.getSubject());
                    System.out.println("  Status: " + firstMsg.getStatus());
                }
                
                Platform.runLater(() -> {
                    applyFilters();
                    updateStatistics();
                });
                
            } catch (Exception e) {
                System.err.println("ERROR loading messages: " + e.getMessage());
                e.printStackTrace();
                
                Platform.runLater(() -> {
                    showErrorInUI("Database Error", 
                        "Failed to load messages:\n" + e.getClass().getSimpleName() + "\n" + 
                        "Message: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void applyFilters() {
        if (allMessages.isEmpty()) {
            System.out.println("DEBUG: No messages to filter");
            return;
        }
        
        List<Message> filteredMessages = new ArrayList<>(allMessages);
        
        // Apply status filter
        String selectedStatus = statusFilterCombo.getSelectionModel().getSelectedItem();
        if (selectedStatus != null && !selectedStatus.equals("ALL")) {
            filteredMessages = filteredMessages.stream()
                .filter(msg -> msg.getStatus() != null && 
                        msg.getStatus().equalsIgnoreCase(selectedStatus))
                .collect(Collectors.toList());
        }
        
        // Apply search filter
        String searchTerm = searchField.getText().trim().toLowerCase();
        if (!searchTerm.isEmpty()) {
            filteredMessages = filteredMessages.stream()
                .filter(msg -> 
                    (msg.getCustomerName() != null && msg.getCustomerName().toLowerCase().contains(searchTerm)) ||
                    (msg.getCustomerEmail() != null && msg.getCustomerEmail().toLowerCase().contains(searchTerm)) ||
                    (msg.getSubject() != null && msg.getSubject().toLowerCase().contains(searchTerm)) ||
                    (msg.getMessage() != null && msg.getMessage().toLowerCase().contains(searchTerm))
                )
                .collect(Collectors.toList());
        }
        
        displayMessages(filteredMessages);
    }
    
    private void displayMessages(List<Message> messages) {
        messagesContainer.getChildren().clear();
        
        if (messages.isEmpty()) {
            VBox noMessagesBox = new VBox(20);
            noMessagesBox.setAlignment(Pos.CENTER);
            noMessagesBox.setPadding(new Insets(40));
            
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size: 48px;");
            
            Label message = new Label("No messages found");
            message.setStyle("-fx-font-size: 18px; -fx-text-fill: #7f8c8d;");
            
            Label hint = new Label("Try changing filters or check database");
            hint.setStyle("-fx-text-fill: #999; -fx-font-size: 12px;");
            
            noMessagesBox.getChildren().addAll(icon, message, hint);
            messagesContainer.getChildren().add(noMessagesBox);
            
            summaryLabel.setText("Showing 0 messages");
            return;
        }
        
        System.out.println("DEBUG: Displaying " + messages.size() + " messages");
        
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        for (Message msg : messages) {
            try {
                VBox messageCard = createMessageCard(msg, dateFormat);
                messagesContainer.getChildren().add(messageCard);
            } catch (Exception e) {
                System.err.println("Error displaying message " + msg.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        summaryLabel.setText("Showing " + messages.size() + " of " + allMessages.size() + " messages");
    }
    
    private VBox createMessageCard(Message msg, DateTimeFormatter dateFormat) {
        VBox card = new VBox(10);
        card.setStyle("-fx-padding: 15; -fx-background-color: white; " +
                     "-fx-border-color: #e0e0e0; -fx-border-width: 1; " +
                     "-fx-border-radius: 8; -fx-background-radius: 8;");
        
        // Header with status indicator
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        // Status icon and color
        String statusIcon = "";
        String statusColor = "";
        
        switch(msg.getStatus().toUpperCase()) {
            case "UNREAD":
                statusIcon = "🔵";
                statusColor = "#2196F3";
                card.setStyle(card.getStyle() + "-fx-background-color: #E3F2FD;");
                break;
            case "READ":
                statusIcon = "⚫";
                statusColor = "#757575";
                break;
            case "REPLIED":
                statusIcon = "✅";
                statusColor = "#4CAF50";
                card.setStyle(card.getStyle() + "-fx-background-color: #E8F5E8;");
                break;
        }
        
        Label statusLabel = new Label(statusIcon + " " + msg.getStatus());
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + statusColor + ";");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Date
        String dateStr = "N/A";
        if (msg.getCreatedAt() != null) {
            dateStr = msg.getCreatedAt().format(dateFormat);
        }
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        
        header.getChildren().addAll(statusLabel, spacer, dateLabel);
        
        // Customer info
        HBox customerRow = new HBox(10);
        customerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label customerIcon = new Label("👤");
        Label customerName = new Label(msg.getCustomerName());
        customerName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label emailLabel = new Label("(" + msg.getCustomerEmail() + ")");
        emailLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        
        customerRow.getChildren().addAll(customerIcon, customerName, emailLabel);
        
        // Subject
        Label subjectLabel = new Label("📌 " + msg.getSubject());
        subjectLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        // Message preview
        HBox messageBubble = new HBox();
        messageBubble.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 15; " +
                              "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 15; " +
                              "-fx-padding: 12 15;");
        
        String messagePreview = msg.getMessage();
        if (messagePreview.length() > 150) {
            messagePreview = messagePreview.substring(0, 150) + "...";
        }
        Label messageContent = new Label(messagePreview);
        messageContent.setWrapText(true);
        messageContent.setStyle("-fx-font-size: 13px; -fx-text-fill: #212121;");
        messageBubble.getChildren().add(messageContent);
        
        // Actions
        HBox actionsBox = new HBox(10);
        
        Button viewBtn = new Button("👁️ View");
        viewBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8 15;");
        viewBtn.setOnAction(e -> showMessageDetails(msg));
        
        Button replyBtn = new Button("💬 Reply");
        replyBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8 15;");
        replyBtn.setOnAction(e -> showReplyDialog(msg));
        
        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8 15;");
        deleteBtn.setOnAction(e -> deleteMessage(msg));
        
        actionsBox.getChildren().addAll(viewBtn, replyBtn, deleteBtn);
        
        card.getChildren().addAll(header, customerRow, subjectLabel, messageBubble, actionsBox);
        
        // Add hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);", "")));
        
        return card;
    }
    
    private void showMessageDetails(Message msg) {
        // Mark as read first if unread
        if (msg.getStatus().equals("UNREAD")) {
            markMessageAsRead(msg);
        }
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chat with Customer");
        dialog.setHeaderText(null);
        
        // Set dialog size
        dialog.getDialogPane().setPrefSize(600, 700);
        
        // Create main container
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white;");
        
        // =========== TOP SECTION: Header ===========
        VBox topSection = new VBox(5);
        topSection.setStyle("-fx-padding: 15; -fx-background-color: #0084ff;");
        
        Label headerLabel = new Label("💬 Chat with " + msg.getCustomerName());
        headerLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label customerInfo = new Label("Email: " + msg.getCustomerEmail() + 
                                       " | Subject: " + msg.getSubject());
        customerInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #e3f2fd;");
        
        topSection.getChildren().addAll(headerLabel, customerInfo);
        
        // =========== CENTER SECTION: Chat History ===========
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 0;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        VBox chatHistory = new VBox(15);
        chatHistory.setStyle("-fx-padding: 15;");
        
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
        
        // Add customer message (LEFT aligned)
        addCustomerMessageToChat(msg, chatHistory, dateFormat);
        
        // Add admin reply if it exists (RIGHT aligned)
        if (msg.hasReply()) {
            addAdminReplyToChat(msg.getAdminReply(), chatHistory, dateFormat, msg.getRepliedAt());
        }
        
        scrollPane.setContent(chatHistory);
        
        // =========== BOTTOM SECTION: Reply Input ===========
        VBox bottomSection = new VBox(10);
        bottomSection.setStyle("-fx-padding: 15; -fx-background-color: white;");
        
        // Only show reply input if no reply exists yet
        if (!msg.hasReply()) {
            HBox inputContainer = new HBox(10);
            inputContainer.setAlignment(Pos.CENTER_LEFT);
            
            // Reply text area
            TextArea replyInput = new TextArea();
            replyInput.setPromptText("Type your reply to " + msg.getCustomerName() + "...");
            replyInput.setWrapText(true);
            replyInput.setPrefRowCount(3);
            replyInput.setPrefWidth(450);
            replyInput.setStyle("-fx-font-size: 14px; -fx-control-inner-background: #f8f9fa; " +
                               "-fx-border-color: #ddd; -fx-border-radius: 5;");
            
            // Send button
            Button sendButton = new Button("Send Reply");
            sendButton.setStyle("-fx-background-color: #0084ff; -fx-text-fill: white; " +
                               "-fx-font-weight: bold; -fx-padding: 12 25;");
            sendButton.setOnAction(e -> {
                String reply = replyInput.getText().trim();
                if (!reply.isEmpty()) {
                    // Send reply to database
                    sendReplyToMessage(msg, reply);
                    
                    // Add the new reply to chat history immediately
                    addAdminReplyToChat(reply, chatHistory, dateFormat, LocalDateTime.now());
                    
                    // Clear input and hide reply section
                    replyInput.clear();
                    bottomSection.getChildren().clear();
                    
                    // Add "already replied" info
                    addAlreadyRepliedInfo(bottomSection, msg, dateFormat, dialog);
                    
                    // Scroll to bottom
                    scrollPane.setVvalue(1.0);
                    
                    // Show success message
                    showNotification("✅ Reply sent to " + msg.getCustomerName());
                    
                    // Refresh messages table
                    loadAllMessages();
                }
            });
            
            inputContainer.getChildren().addAll(replyInput, sendButton);
            
            // Status label
            Label statusLabel = new Label("Press Enter to send (Shift+Enter for new line)");
            statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
            
            // Enable sending with Enter key
            replyInput.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                    event.consume(); // Prevent new line
                    sendButton.fire();
                }
            });
            
            bottomSection.getChildren().addAll(inputContainer, statusLabel);
        } else {
            // If already replied, show view-only info
            addAlreadyRepliedInfo(bottomSection, msg, dateFormat, dialog);
        }
        
        // =========== ASSEMBLE LAYOUT ===========
        root.setTop(topSection);
        root.setCenter(scrollPane);
        root.setBottom(bottomSection);
        
        dialog.getDialogPane().setContent(root);
        
        // Add close button
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Auto-scroll to bottom when dialog opens
        dialog.setOnShown(e -> {
            Platform.runLater(() -> {
                scrollPane.setVvalue(1.0);
            });
        });
        
        dialog.showAndWait();
    }
    
    private void addCustomerMessageToChat(Message msg, VBox chatHistory, DateTimeFormatter dateFormat) {
        HBox messageContainer = new HBox();
        messageContainer.setAlignment(Pos.CENTER_LEFT);
        messageContainer.setPadding(new Insets(5, 0, 5, 0));
        
        VBox messageBubble = new VBox(5);
        messageBubble.setAlignment(Pos.TOP_LEFT);
        messageBubble.setStyle("-fx-padding: 10; -fx-background-color: #e5f6fd; " +
                              "-fx-background-radius: 15 15 15 0; -fx-max-width: 400;");
        
        // Header with customer info
        HBox header = new HBox(5);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label customerIcon = new Label("👤");
        customerIcon.setStyle("-fx-font-size: 14px;");
        
        Label customerName = new Label(msg.getCustomerName());
        customerName.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #333;");
        
        Label timeLabel = new Label(msg.getCreatedAt().format(dateFormat));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        header.getChildren().addAll(customerIcon, customerName, timeLabel);
        
        // Message text
        Text messageText = new Text(msg.getMessage());
        messageText.setWrappingWidth(380);
        messageText.setStyle("-fx-font-size: 13px; -fx-fill: #333;");
        
        messageBubble.getChildren().addAll(header, messageText);
        messageContainer.getChildren().add(messageBubble);
        chatHistory.getChildren().add(messageContainer);
    }
    
    private void addAdminReplyToChat(String replyText, VBox chatHistory, DateTimeFormatter dateFormat, LocalDateTime replyTime) {
        HBox messageContainer = new HBox();
        messageContainer.setAlignment(Pos.CENTER_RIGHT);
        messageContainer.setPadding(new Insets(5, 0, 5, 0));
        
        VBox messageBubble = new VBox(5);
        messageBubble.setAlignment(Pos.TOP_RIGHT);
        messageBubble.setStyle("-fx-padding: 10; -fx-background-color: #dcf8c6; " +
                              "-fx-background-radius: 15 15 0 15; -fx-max-width: 400;");
        
        // Header with admin info
        HBox header = new HBox(5);
        header.setAlignment(Pos.CENTER_RIGHT);
        
        Label timeLabel = new Label(replyTime != null ? replyTime.format(dateFormat) : "Now");
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        Label adminName = new Label("You (Admin)");
        adminName.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #333;");
        
        Label adminIcon = new Label("👨‍💼");
        adminIcon.setStyle("-fx-font-size: 14px;");
        
        header.getChildren().addAll(timeLabel, adminName, adminIcon);
        
        // Message text
        Text messageText = new Text(replyText);
        messageText.setWrappingWidth(380);
        messageText.setStyle("-fx-font-size: 13px; -fx-fill: #333;");
        
        messageBubble.getChildren().addAll(header, messageText);
        messageContainer.getChildren().add(messageBubble);
        chatHistory.getChildren().add(messageContainer);
    }
    
    private void addAlreadyRepliedInfo(VBox bottomSection, Message msg, DateTimeFormatter dateFormat, Dialog<?> dialog) {
        VBox repliedInfo = new VBox(10);
        repliedInfo.setStyle("-fx-padding: 15; -fx-background-color: #f0f9ff; -fx-background-radius: 8;");
        
        Label repliedLabel = new Label("✅ Message Already Replied");
        repliedLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4CAF50; -fx-font-size: 14px;");
        
        Label dateLabel = new Label("Replied on: " + 
            (msg.getRepliedAt() != null ? msg.getRepliedAt().format(dateFormat) : "N/A"));
        dateLabel.setStyle("-fx-text-fill: #666;");
        
        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #666; -fx-text-fill: white; -fx-padding: 8 20;");
        closeButton.setOnAction(e -> dialog.close());
        
        repliedInfo.getChildren().addAll(repliedLabel, dateLabel, closeButton);
        bottomSection.getChildren().add(repliedInfo);
    }
    
    @FXML
    private void handleSendMessage() {
        showNewMessageDialog();
    }
    
    private void showNewMessageDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Send New Message");
        dialog.setHeaderText("Send a message to a customer");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #fafafa;");
        
        // Customer selection
        VBox customerBox = new VBox(5);
        Label customerLabel = new Label("👤 Select Customer");
        customerLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        
        ComboBox<String> customerCombo = new ComboBox<>();
        ObservableList<String> customerNames = FXCollections.observableArrayList();
        allCustomers.forEach(customer -> 
            customerNames.add(customer.getUsername() + " (" + customer.getEmail() + ")")
        );
        customerCombo.setItems(customerNames);
        customerCombo.setPromptText("Select a customer...");
        
        customerBox.getChildren().addAll(customerLabel, customerCombo);
        
        // Subject
        VBox subjectBox = new VBox(5);
        Label subjectLabel = new Label("📌 Subject");
        subjectLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        
        TextField subjectField = new TextField();
        subjectField.setPromptText("Enter message subject...");
        
        subjectBox.getChildren().addAll(subjectLabel, subjectField);
        
        // Message
        VBox messageBox = new VBox(5);
        Label messageLabel = new Label("💬 Message");
        messageLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        
        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Type your message here...");
        messageArea.setWrapText(true);
        messageArea.setPrefRowCount(6);
        
        messageBox.getChildren().addAll(messageLabel, messageArea);
        
        content.getChildren().addAll(customerBox, subjectBox, messageBox);
        
        dialog.getDialogPane().setContent(content);
        
        // Add buttons
        ButtonType sendButtonType = new ButtonType("Send Message", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);
        
        // Enable/disable send button
        dialog.getDialogPane().lookupButton(sendButtonType).setDisable(true);
        
        Runnable updateSendButton = () -> {
            boolean enabled = !customerCombo.getSelectionModel().isEmpty() &&
                            !subjectField.getText().trim().isEmpty() &&
                            !messageArea.getText().trim().isEmpty();
            dialog.getDialogPane().lookupButton(sendButtonType).setDisable(!enabled);
        };
        
        customerCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateSendButton.run());
        subjectField.textProperty().addListener((obs, oldVal, newVal) -> updateSendButton.run());
        messageArea.textProperty().addListener((obs, oldVal, newVal) -> updateSendButton.run());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == sendButtonType) {
                String selectedCustomer = customerCombo.getValue();
                String subject = subjectField.getText().trim();
                String message = messageArea.getText().trim();
                
                // Extract customer ID from selection
                String username = selectedCustomer.split(" \\(")[0];
                User selectedUser = allCustomers.stream()
                    .filter(c -> c.getUsername().equals(username))
                    .findFirst()
                    .orElse(null);
                
                if (selectedUser != null) {
                    sendNewMessageToCustomer(selectedUser.getId(), subject, message);
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    private void sendNewMessageToCustomer(int customerId, String subject, String message) {
        new Thread(() -> {
            try {
                boolean success = databaseService.sendMessageFromAdmin(customerId, subject, message);
                
                if (success) {
                    Platform.runLater(() -> {
                        showNotification("✅ Message sent to customer");
                        loadAllMessages(); // Refresh to show the sent message
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Error", "Failed to send message: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void showReplyDialog(Message msg) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Reply to Message");
        dialog.setHeaderText("Replying to: " + msg.getCustomerName());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Original message preview
        VBox originalBox = new VBox(10);
        originalBox.setStyle("-fx-padding: 15; -fx-background-color: #e8f5e8; -fx-background-radius: 10;");
        
        Label originalHeader = new Label("📨 Original Message");
        originalHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #388E3C;");
        
        Label originalSender = new Label("From: " + msg.getCustomerName());
        originalSender.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        
        TextArea originalArea = new TextArea(msg.getMessage());
        originalArea.setEditable(false);
        originalArea.setWrapText(true);
        originalArea.setPrefRowCount(4);
        originalArea.setStyle("-fx-font-size: 13px; -fx-background-color: transparent; -fx-border-width: 0;");
        
        originalBox.getChildren().addAll(originalHeader, originalSender, originalArea);
        
        // Reply area
        VBox replyBox = new VBox(10);
        
        Label replyLabel = new Label("💬 Your Reply");
        replyLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #FF9800;");
        
        TextArea replyArea = new TextArea();
        replyArea.setPromptText("Type your reply here...");
        replyArea.setWrapText(true);
        replyArea.setPrefRowCount(8);
        replyArea.setStyle("-fx-font-size: 14px; -fx-background-color: #fff3e0; " +
                         "-fx-border-color: #ffcc80; -fx-border-radius: 10; -fx-background-radius: 10;");
        
        replyBox.getChildren().addAll(replyLabel, replyArea);
        
        content.getChildren().addAll(originalBox, replyBox);
        
        dialog.getDialogPane().setContent(content);
        
        // Add buttons
        ButtonType replyButtonType = new ButtonType("Send Reply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(replyButtonType, ButtonType.CANCEL);
        
        // Enable/disable reply button
        dialog.getDialogPane().lookupButton(replyButtonType).setDisable(true);
        replyArea.textProperty().addListener((obs, oldVal, newVal) -> {
            dialog.getDialogPane().lookupButton(replyButtonType).setDisable(
                newVal.trim().isEmpty()
            );
        });
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == replyButtonType) {
                return replyArea.getText().trim();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(reply -> {
            sendReplyToMessage(msg, reply);
        });
    }
    
    private void sendReplyToMessage(Message msg, String reply) {
        new Thread(() -> {
            try {
                boolean success = databaseService.adminReplyToMessage(msg.getId(), reply);
                
                if (success) {
                    Platform.runLater(() -> {
                        showNotification("✅ Reply sent to " + msg.getCustomerName());
                        loadAllMessages();
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Error", "Failed to send reply: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void markMessageAsRead(Message msg) {
        new Thread(() -> {
            try {
                boolean success = databaseService.markMessageAsRead(msg.getId());
                
                if (success) {
                    msg.setStatus("READ");
                    Platform.runLater(() -> {
                        loadAllMessages();
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void deleteMessage(Message msg) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Message");
        confirmation.setHeaderText("Delete this message?");
        confirmation.setContentText("Are you sure you want to delete this message from " + msg.getCustomerName() + "?\nThis action cannot be undone.");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        boolean success = databaseService.deleteMessage(msg.getId());
                        
                        if (success) {
                            Platform.runLater(() -> {
                                showNotification("🗑️ Message deleted");
                                loadAllMessages();
                            });
                        }
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            showError("Error", "Failed to delete message: " + e.getMessage());
                        });
                    }
                }).start();
            }
        });
    }
    
    private void updateStatistics() {
        if (allMessages.isEmpty()) {
            unreadCountLabel.setText("0");
            totalCountLabel.setText("0");
            return;
        }
        
        int totalMessages = allMessages.size();
        long unreadCount = allMessages.stream()
            .filter(msg -> msg.getStatus() != null && msg.getStatus().equals("UNREAD"))
            .count();
        
        Platform.runLater(() -> {
            unreadCountLabel.setText(String.valueOf(unreadCount));
            totalCountLabel.setText(String.valueOf(totalMessages));
            
            System.out.println("DEBUG: Statistics updated - Total: " + totalMessages + ", Unread: " + unreadCount);
        });
    }
    
    private void showLoadingState() {
        Platform.runLater(() -> {
            messagesContainer.getChildren().clear();
            
            VBox loadingBox = new VBox(20);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.setPadding(new Insets(40));
            
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(50, 50);
            
            Label loadingLabel = new Label("Loading messages...");
            loadingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
            
            loadingBox.getChildren().addAll(spinner, loadingLabel);
            messagesContainer.getChildren().add(loadingBox);
        });
    }
    
    private void showErrorInUI(String title, String message) {
        Platform.runLater(() -> {
            messagesContainer.getChildren().clear();
            
            VBox errorBox = new VBox(20);
            errorBox.setAlignment(Pos.CENTER);
            errorBox.setPadding(new Insets(40));
            
            Label errorIcon = new Label("⚠️");
            errorIcon.setStyle("-fx-font-size: 48px;");
            
            Label errorTitle = new Label(title);
            errorTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            
            Label errorMessage = new Label(message);
            errorMessage.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
            errorMessage.setWrapText(true);
            errorMessage.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            
            Button retryButton = new Button("Retry");
            retryButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 8 20;");
            retryButton.setOnAction(e -> loadAllMessages());
            
            errorBox.getChildren().addAll(errorIcon, errorTitle, errorMessage, retryButton);
            messagesContainer.getChildren().add(errorBox);
        });
    }
    
    @FXML
    private void handleFilter() {
        applyFilters();
    }
    
    @FXML
    private void handleRefresh() {
        System.out.println("DEBUG: Refresh button clicked");
        loadAllMessages();
        loadAllCustomers();
    }
    
    @FXML
    private void handleClose() {
        if (messagesContainer.getScene() != null && messagesContainer.getScene().getWindow() != null) {
            messagesContainer.getScene().getWindow().hide();
        }
    }
    
    private void showNotification(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    // You'll need to add these methods to your DatabaseService class:
    // 1. getAllCustomers() - returns List<User> of customers
    // 2. sendMessageFromAdmin(customerId, subject, message) - sends message from admin to customer
    // 3. deleteMessage(messageId) - deletes a message
}