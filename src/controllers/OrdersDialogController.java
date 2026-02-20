package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import models.Order;
import models.OrderItem;
import models.UserSession;
import services.DatabaseService;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class OrdersDialogController implements Initializable {
    
    @FXML private VBox ordersContainer;
    @FXML private Label totalOrdersLabel;
    @FXML private Label totalSpentLabel;
    @FXML private Label processingLabel;
    @FXML private Label deliveredLabel;
    
    private DatabaseService databaseService;
    private int currentUserId ;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== OrdersDialogController INITIALIZED ===");
        
        try {
        	currentUserId=UserSession.getUserId();
            databaseService = new DatabaseService();
            System.out.println("✓ DatabaseService initialized successfully");
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize DatabaseService: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void setCurrentUserId(int userId) {
        System.out.println("=== setCurrentUserId ===");
        System.out.println("Received User ID: " + userId);
        
        this.currentUserId = userId;
        
        if (databaseService != null) {
            loadOrders();
        } else {
            System.err.println("DatabaseService is null!");
            showErrorInUI("Database Error", "Database service not initialized");
        }
    }
    
    private void loadOrders() {
        System.out.println("=== loadOrders ===");
        System.out.println("Loading orders for user ID: " + currentUserId);
        
        if (currentUserId <= 0) {
            System.err.println("Invalid user ID!");
            showErrorInUI("Invalid User", "Please login to view orders");
            return;
        }
        
        // Show loading state
        Platform.runLater(() -> {
            ordersContainer.getChildren().clear();
            
            VBox loadingBox = new VBox(10);
            loadingBox.setAlignment(javafx.geometry.Pos.CENTER);
            loadingBox.setPadding(new Insets(40));
            
            ProgressIndicator spinner = new ProgressIndicator();
            Label loadingLabel = new Label("Loading your orders...");
            loadingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
            
            loadingBox.getChildren().addAll(spinner, loadingLabel);
            ordersContainer.getChildren().add(loadingBox);
            
            updateSummaryLabels(0, 0.0, 0, 0);
        });
        
        // Load orders in background thread
        new Thread(() -> {
            try {
                System.out.println("Querying database for orders...");
                List<Order> orders = databaseService.getCustomerOrders(currentUserId);
                
                Platform.runLater(() -> {
                    if (orders == null) {
                    	showErrorInUI("Database Error", "Failed to retrieve orders (null returned)");
                    } else {
                        System.out.println("Retrieved " + orders.size() + " orders from database");
                        displayOrders(orders);
                    }
                });
                
            } catch (Exception e) {
                System.err.println("✗ ERROR loading orders:");
                e.printStackTrace();
                
                Platform.runLater(() -> {
                    showErrorInUI("Database Error", 
                        "Failed to load orders:\n" + 
                        "Error: " + e.getClass().getSimpleName() + "\n" +
                        "Message: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void displayOrders(List<Order> orders) {
        System.out.println("=== displayOrders ===");
        System.out.println("Number of orders to display: " + orders.size());
        
        ordersContainer.getChildren().clear();
        
        if (orders.isEmpty()) {
            System.out.println("No orders found in database for user " + currentUserId);
            
            VBox noOrdersBox = new VBox(20);
            noOrdersBox.setAlignment(javafx.geometry.Pos.CENTER);
            noOrdersBox.setPadding(new Insets(60));
            
            Label icon = new Label("📦");
            icon.setStyle("-fx-font-size: 48px;");
            
            Label title = new Label("No Orders Yet");
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            
            Label message = new Label("You haven't placed any orders yet.\nStart shopping to build your order history!");
            message.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
            message.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            
            noOrdersBox.getChildren().addAll(icon, title, message);
            ordersContainer.getChildren().add(noOrdersBox);
            
            updateSummaryLabels(0, 0.0, 0, 0);
            return;
        }
        
        // Statistics
        int totalOrders = orders.size();
        double totalSpent = 0;
        int processingCount = 0;
        int deliveredCount = 0;
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        // Display each order
        for (Order order : orders) {
            try {
                System.out.println("Processing Order: " + order.getOrderId() + 
                                 " | Total: " + order.getTotalAmount() + 
                                 " | Status: " + order.getStatus());
                
                VBox orderCard = createOrderCard(order, dateFormatter);
                ordersContainer.getChildren().add(orderCard);
                
                // Update statistics
                totalSpent += order.getTotalAmount();
                
                String status = order.getStatus();
                if (status != null) {
                    switch (status.toUpperCase()) {
                        case "PROCESSING":
                            processingCount++;
                            break;
                        case "DELIVERED":
                            deliveredCount++;
                            break;
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Error creating card for order " + order.getOrderId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        updateSummaryLabels(totalOrders, totalSpent, processingCount, deliveredCount);
        System.out.println("✓ Successfully displayed " + orders.size() + " orders");
    }
    
    private VBox createOrderCard(Order order, DateTimeFormatter dateFormatter) {
        VBox card = new VBox(12);
        card.setStyle("-fx-padding: 15; -fx-background-color: white; " +
                     "-fx-border-color: #e0e0e0; -fx-border-width: 1; " +
                     "-fx-border-radius: 8; -fx-background-radius: 8;");
        card.setPadding(new Insets(15));
        
        // Header - Order ID and Date
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label orderIdLabel = new Label("Order #" + order.getOrderId());
        orderIdLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3c72;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        // Format the date
        String dateStr = "N/A";
        Object orderDate = order.getOrderDate();
        if (orderDate != null) {
            try {
                if (orderDate instanceof Timestamp) {
                    dateStr = ((Timestamp) orderDate).toLocalDateTime().format(dateFormatter);
                } else if (orderDate instanceof LocalDateTime) {
                    dateStr = ((LocalDateTime) orderDate).format(dateFormatter);
                } else if (orderDate instanceof java.util.Date) {
                    dateStr = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm")
                        .format((java.util.Date) orderDate);
                } else {
                    dateStr = orderDate.toString();
                }
            } catch (Exception e) {
                dateStr = "Date error";
            }
        }
        
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        
        header.getChildren().addAll(orderIdLabel, spacer, dateLabel);
        
        // Items Section
        VBox itemsSection = new VBox(5);
        
        Label itemsTitle = new Label("Items:");
        itemsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        itemsSection.getChildren().add(itemsTitle);
        
        List<OrderItem> items = order.getItems();
        if (items != null && !items.isEmpty()) {
            for (OrderItem item : items) {
                HBox itemRow = new HBox(8);
                itemRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                
                Label quantity = new Label(item.getQuantity() + "x");
                quantity.setStyle("-fx-font-weight: bold; -fx-min-width: 30;");
                
                Label productName = new Label(item.getProductName());
                productName.setStyle("-fx-font-size: 13px;");
                
                javafx.scene.layout.Region itemSpacer = new javafx.scene.layout.Region();
                HBox.setHgrow(itemSpacer, javafx.scene.layout.Priority.ALWAYS);
                
                Label price = new Label(String.format("USD %.2f each", item.getUnitPrice()));
                price.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
                
                itemRow.getChildren().addAll(quantity, productName, itemSpacer, price);
                itemsSection.getChildren().add(itemRow);
            }
        } else {
            Label noItems = new Label("No items found");
            noItems.setStyle("-fx-text-fill: #999; -fx-font-size: 12px; -fx-font-style: italic;");
            itemsSection.getChildren().add(noItems);
        }
        
        // Footer - Total, Payment, Status
        HBox footer = new HBox(20);
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Total
        VBox totalBox = new VBox(2);
        Label totalLabel = new Label("Total:");
        totalLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        Label totalValue = new Label(String.format("USD %.2f", order.getTotalAmount()));
        totalValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2a5298;");
        totalBox.getChildren().addAll(totalLabel, totalValue);
        
        // Payment Method
        VBox paymentBox = new VBox(2);
        Label paymentLabel = new Label("Payment:");
        paymentLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        Label paymentValue = new Label(order.getPaymentMethod() != null ? order.getPaymentMethod() : "N/A");
        paymentValue.setStyle("-fx-font-weight: bold;");
        paymentBox.getChildren().addAll(paymentLabel, paymentValue);
        
        // Status
        VBox statusBox = new VBox(2);
        Label statusLabel = new Label("Status:");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        String status = order.getStatus() != null ? order.getStatus() : "UNKNOWN";
        Label statusValue = new Label(status);
        
        // Color code status
        switch (status.toUpperCase()) {
            case "PROCESSING":
                statusValue.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                break;
            case "DELIVERED":
                statusValue.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                break;
            case "SHIPPED":
                statusValue.setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
                break;
            case "CANCELLED":
                statusValue.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                break;
            default:
                statusValue.setStyle("-fx-font-weight: bold;");
        }
        
        statusBox.getChildren().addAll(statusLabel, statusValue);
        
        footer.getChildren().addAll(totalBox, paymentBox, statusBox);
        
        // Add all sections to card
        card.getChildren().addAll(header, itemsSection, footer);
        
        return card;
    }
    
    private void updateSummaryLabels(int totalOrders, double totalSpent, 
                                     int processingCount, int deliveredCount) {
        Platform.runLater(() -> {
            totalOrdersLabel.setText(String.valueOf(totalOrders));
            totalSpentLabel.setText(String.format("USD %.2f", totalSpent));
            processingLabel.setText(String.valueOf(processingCount));
            deliveredLabel.setText(String.valueOf(deliveredCount));
            
            System.out.println("Summary updated:");
            System.out.println("  Total Orders: " + totalOrders);
            System.out.println("  Total Spent: USD " + totalSpent);
            System.out.println("  Processing: " + processingCount);
            System.out.println("  Delivered: " + deliveredCount);
        });
    }
    
    @FXML
    private void handleRefresh() {
        System.out.println("=== Refresh button clicked ===");
        
        if (currentUserId > 0) {
            loadOrders();
            
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Refresh");
                alert.setHeaderText(null);
                alert.setContentText("Orders list refreshed!");
                alert.show();
            });
        } else {
            showErrorInUI("Error", "Cannot refresh: User not set");
        }
    }
    
    private void showErrorInUI(String title, String message) {
        Platform.runLater(() -> {
            ordersContainer.getChildren().clear();
            
            VBox errorBox = new VBox(15);
            errorBox.setAlignment(javafx.geometry.Pos.CENTER);
            errorBox.setPadding(new Insets(40));
            
            Label errorIcon = new Label("⚠️");
            errorIcon.setStyle("-fx-font-size: 48px;");
            
            Label errorTitle = new Label(title);
            errorTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #d32f2f;");
            
            Label errorMessage = new Label(message);
            errorMessage.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
            errorMessage.setWrapText(true);
            errorMessage.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            
            Button retryButton = new Button("Try Again");
            retryButton.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 8 16; -fx-border-color: #ccc;");
            retryButton.setOnAction(e -> {
                if (currentUserId > 0) {
                    loadOrders();
                }
            });
            
            errorBox.getChildren().addAll(errorIcon, errorTitle, errorMessage, retryButton);
            ordersContainer.getChildren().add(errorBox);
        });
    }
}