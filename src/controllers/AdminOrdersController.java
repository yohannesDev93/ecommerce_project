package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import models.Order;
import models.OrderItem;
import services.DatabaseService;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AdminOrdersController implements Initializable {
    
    @FXML private VBox ordersContainer;
    @FXML private Label totalOrdersLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label processingLabel;
    @FXML private Label pendingLabel;
    @FXML private Label summaryLabel;
    
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TextField searchField;
    
    private DatabaseService databaseService;
    private List<Order> allOrders = new ArrayList<>();
    
    // Status options for dropdown
    private final ObservableList<String> statusOptions = FXCollections.observableArrayList(
        "PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"
    );
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== AdminOrdersController INITIALIZED ===");
        
        try {
            databaseService = new DatabaseService();
            
            // Populate combo boxes
            statusFilterCombo.getItems().addAll("ALL", "PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");
            sortCombo.getItems().addAll("Newest First", "Oldest First", "Total (High-Low)", "Total (Low-High)");
            
            // Set default selections
            statusFilterCombo.getSelectionModel().selectFirst();
            sortCombo.getSelectionModel().selectFirst();
            
            loadAllOrders();
            setupFilters();
        } catch (Exception e) {
            System.err.println("Failed to initialize: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Initialization Error", "Could not initialize order management system");
        }
    }
    
    private void setupFilters() {
        // Add listeners for real-time filtering
        statusFilterCombo.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> applyFilters()
        );
        
        sortCombo.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> applyFilters()
        );
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty() || newVal.length() > 2) {
                applyFilters();
            }
        });
    }
    
    private void loadAllOrders() {
        System.out.println("Loading all orders...");
        
        // Show loading
        showLoadingState();
        
        // Load in background thread
        new Thread(() -> {
            try {
                allOrders = databaseService.getAllOrders();
                System.out.println("Loaded " + allOrders.size() + " orders from database");
                
                Platform.runLater(() -> {
                    applyFilters();
                    updateStatistics();
                });
                
            } catch (Exception e) {
                System.err.println("Error loading orders: " + e.getMessage());
                e.printStackTrace();
                
                Platform.runLater(() -> {
                    showErrorInUI("Database Error", "Failed to load orders: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void applyFilters() {
        if (allOrders.isEmpty()) return;
        
        List<Order> filteredOrders = new ArrayList<>(allOrders);
        
        // Apply status filter
        String selectedStatus = statusFilterCombo.getSelectionModel().getSelectedItem();
        if (selectedStatus != null && !selectedStatus.equals("ALL")) {
            filteredOrders = filteredOrders.stream()
                .filter(order -> order.getStatus() != null && 
                        order.getStatus().equalsIgnoreCase(selectedStatus))
                .collect(Collectors.toList());
        }
        
        // Apply search filter
        String searchTerm = searchField.getText().trim().toLowerCase();
        if (!searchTerm.isEmpty()) {
            filteredOrders = filteredOrders.stream()
                .filter(order -> 
                    (order.getOrderId() != null && order.getOrderId().toLowerCase().contains(searchTerm)) ||
                    (order.getCustomerName() != null && order.getCustomerName().toLowerCase().contains(searchTerm)) ||
                    (order.getCustomerEmail() != null && order.getCustomerEmail().toLowerCase().contains(searchTerm))
                )
                .collect(Collectors.toList());
        }
        
        // Apply sorting
        String sortOption = sortCombo.getSelectionModel().getSelectedItem();
        if (sortOption != null) {
            switch (sortOption) {
                case "Newest First":
                    filteredOrders.sort((o1, o2) -> {
                        Object d1 = o1.getOrderDate();
                        Object d2 = o2.getOrderDate();
                        if (d1 instanceof Timestamp && d2 instanceof Timestamp) {
                            return ((Timestamp) d2).compareTo((Timestamp) d1);
                        }
                        return 0;
                    });
                    break;
                case "Oldest First":
                    filteredOrders.sort((o1, o2) -> {
                        Object d1 = o1.getOrderDate();
                        Object d2 = o2.getOrderDate();
                        if (d1 instanceof Timestamp && d2 instanceof Timestamp) {
                            return ((Timestamp) d1).compareTo((Timestamp) d2);
                        }
                        return 0;
                    });
                    break;
                case "Total (High-Low)":
                    filteredOrders.sort((o1, o2) -> 
                        Double.compare(o2.getTotalAmount(), o1.getTotalAmount()));
                    break;
                case "Total (Low-High)":
                    filteredOrders.sort((o1, o2) -> 
                        Double.compare(o1.getTotalAmount(), o2.getTotalAmount()));
                    break;
            }
        }
        
        displayOrders(filteredOrders);
    }
    
    private void displayOrders(List<Order> orders) {
        ordersContainer.getChildren().clear();
        
        if (orders.isEmpty()) {
            VBox noOrdersBox = new VBox(20);
            noOrdersBox.setAlignment(Pos.CENTER);
            noOrdersBox.setPadding(new Insets(40));
            
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size: 48px;");
            
            Label message = new Label("No orders match your filters");
            message.setStyle("-fx-font-size: 18px; -fx-text-fill: #7f8c8d;");
            
            Button clearFilters = new Button("Clear Filters");
            clearFilters.setOnAction(e -> {
                statusFilterCombo.getSelectionModel().selectFirst();
                searchField.clear();
                applyFilters();
            });
            
            noOrdersBox.getChildren().addAll(icon, message, clearFilters);
            ordersContainer.getChildren().add(noOrdersBox);
            
            summaryLabel.setText("Showing 0 orders");
            return;
        }
        
        // Add each order
        for (Order order : orders) {
            try {
                VBox orderCard = createOrderCard(order);
                ordersContainer.getChildren().add(orderCard);
            } catch (Exception e) {
                System.err.println("Error displaying order " + order.getOrderId() + ": " + e.getMessage());
            }
        }
        
        summaryLabel.setText("Showing " + orders.size() + " of " + allOrders.size() + " orders");
    }
    
    private VBox createOrderCard(Order order) {
        VBox card = new VBox(10);
        card.setStyle("-fx-padding: 15; -fx-background-color: white; " +
                     "-fx-border-color: #e0e0e0; -fx-border-width: 1; " +
                     "-fx-border-radius: 8; -fx-background-radius: 8; " +
                     "-fx-margin: 0 0 10 0;");
        
        // Top row - Order ID, Date, and Status
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        Label orderIdLabel = new Label("Order #" + order.getOrderId());
        orderIdLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3c72;");
        
        // Spacer
        javafx.scene.layout.Region spacer1 = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer1, javafx.scene.layout.Priority.ALWAYS);
        
        // Format date
        String dateStr = "Date: ";
        if (order.getOrderDate() != null) {
            try {
                if (order.getOrderDate() instanceof Timestamp) {
                    dateStr += ((Timestamp) order.getOrderDate()).toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
                } else {
                    dateStr += order.getOrderDate().toString();
                }
            } catch (Exception e) {
                dateStr += "N/A";
            }
        }
        
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        
        // Spacer
        javafx.scene.layout.Region spacer2 = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer2, javafx.scene.layout.Priority.ALWAYS);
        
        // Status
        String status = order.getStatus() != null ? order.getStatus() : "UNKNOWN";
        Label statusLabel = new Label(status);
        String statusColor = getStatusColor(status);
        statusLabel.setStyle(String.format("-fx-font-weight: bold; -fx-text-fill: %s; -fx-padding: 3 10; " +
                                         "-fx-background-color: %s20; -fx-background-radius: 10;", 
                                         statusColor, statusColor));
        
        topRow.getChildren().addAll(orderIdLabel, spacer1, dateLabel, spacer2, statusLabel);
        
        // Customer Info
        HBox customerRow = new HBox(10);
        customerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label customerLabel = new Label("Customer: ");
        customerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        
        Label customerName = new Label(order.getCustomerName() != null ? order.getCustomerName() : "N/A");
        customerName.setStyle("-fx-font-size: 13px;");
        
        Label emailLabel = new Label("(" + (order.getCustomerEmail() != null ? order.getCustomerEmail() : "No email") + ")");
        emailLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        
        customerRow.getChildren().addAll(customerLabel, customerName, emailLabel);
        
        // Items Section
        VBox itemsSection = new VBox(5);
        itemsSection.setPadding(new Insets(5, 0, 0, 10));
        
        Label itemsTitle = new Label("Items:");
        itemsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        itemsSection.getChildren().add(itemsTitle);
        
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                HBox itemRow = new HBox(10);
                itemRow.setAlignment(Pos.CENTER_LEFT);
                
                Label quantity = new Label(item.getQuantity() + "x");
                quantity.setStyle("-fx-font-weight: bold; -fx-min-width: 30;");
                
                Label productName = new Label(item.getProductName());
                productName.setStyle("-fx-font-size: 13px;");
                
                javafx.scene.layout.Region itemSpacer = new javafx.scene.layout.Region();
                HBox.setHgrow(itemSpacer, javafx.scene.layout.Priority.ALWAYS);
                
                Label price = new Label(String.format("USD %.2f each", item.getUnitPrice()));
                price.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
                
                Label total = new Label(String.format("= USD %.2f", item.getUnitPrice() * item.getQuantity()));
                total.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
                
                itemRow.getChildren().addAll(quantity, productName, itemSpacer, price, total);
                itemsSection.getChildren().add(itemRow);
            }
        } else {
            Label noItems = new Label("No items found");
            noItems.setStyle("-fx-text-fill: #999; -fx-font-size: 12px; -fx-font-style: italic;");
            itemsSection.getChildren().add(noItems);
        }
        
        // Bottom row - Total, Payment, Actions
        HBox bottomRow = new HBox(20);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        
        // Total
        VBox totalBox = new VBox(2);
        Label totalText = new Label("Order Total:");
        totalText.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        Label totalValue = new Label(String.format("USD %.2f", order.getTotalAmount()));
        totalValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2a5298;");
        totalBox.getChildren().addAll(totalText, totalValue);
        
        // Payment
        VBox paymentBox = new VBox(2);
        Label paymentText = new Label("Payment Method:");
        paymentText.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        Label paymentValue = new Label(order.getPaymentMethod() != null ? order.getPaymentMethod() : "N/A");
        paymentValue.setStyle("-fx-font-weight: bold;");
        paymentBox.getChildren().addAll(paymentText, paymentValue);
        
        // Actions
        HBox actionBox = new HBox(10);
        
        // View Details Button
        Button viewBtn = new Button("View Details");
        viewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10;");
        viewBtn.setOnAction(e -> showOrderDetails(order));
        
        // Update Status Button
        Button updateBtn = new Button("Update Status");
        updateBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10;");
        updateBtn.setOnAction(e -> showUpdateStatusDialog(order));
        
        actionBox.getChildren().addAll(viewBtn, updateBtn);
        
        bottomRow.getChildren().addAll(totalBox, paymentBox, actionBox);
        
        // Add all sections to card
        card.getChildren().addAll(topRow, customerRow, itemsSection, bottomRow);
        
        return card;
    }
    
    private String getStatusColor(String status) {
        if (status == null) return "#95a5a6";
        
        switch (status.toUpperCase()) {
            case "PENDING": return "#f39c12";    // Orange
            case "PROCESSING": return "#3498db"; // Blue
            case "SHIPPED": return "#9b59b6";    // Purple
            case "DELIVERED": return "#2ecc71";  // Green
            case "CANCELLED": return "#e74c3c";  // Red
            default: return "#95a5a6";           // Gray
        }
    }
    
    private void showOrderDetails(Order order) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Order Details - " + order.getOrderId());
        dialog.setHeaderText("Complete Order Information");
        
        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dateStr = "N/A";
        if (order.getOrderDate() != null && order.getOrderDate() instanceof Timestamp) {
            dateStr = ((Timestamp) order.getOrderDate()).toLocalDateTime().format(dateTimeFormat);
        }
        
        // Order Information
        addDetailRow(grid, 0, "Order ID:", order.getOrderId());
        addDetailRow(grid, 1, "Order Date:", dateStr);
        addDetailRow(grid, 2, "Status:", order.getStatus());
        addDetailRow(grid, 3, "Total Amount:", String.format("USD %.2f", order.getTotalAmount()));
        addDetailRow(grid, 4, "Payment Method:", order.getPaymentMethod());
        
        // Customer Information
        addDetailRow(grid, 5, "Customer Name:", order.getCustomerName());
        addDetailRow(grid, 6, "Customer Email:", order.getCustomerEmail());
        addDetailRow(grid, 7, "Customer Phone:", order.getCustomerPhone() != null ? order.getCustomerPhone() : "N/A");
        addDetailRow(grid, 8, "Shipping Address:", order.getShippingAddress() != null ? order.getShippingAddress() : "N/A");
        
        // Order Items
        Label itemsHeader = new Label("Order Items:");
        itemsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 0 5 0;");
        grid.add(itemsHeader, 0, 9, 2, 1);
        
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            int rowIndex = 10;
            for (OrderItem item : order.getItems()) {
                String itemText = String.format("%d x %s (USD %.2f each) = USD %.2f",
                    item.getQuantity(), item.getProductName(), 
                    item.getUnitPrice(), item.getSubtotal());
                addDetailRow(grid, rowIndex++, "", itemText);
            }
        } else {
            addDetailRow(grid, 10, "", "No items found");
        }
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setMinWidth(500);
        
        dialog.showAndWait();
    }
    
    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label keyLabel = new Label(label);
        keyLabel.setStyle("-fx-font-weight: bold;");
        grid.add(keyLabel, 0, row);
        
        Label valueLabel = new Label(value);
        valueLabel.setWrapText(true);
        grid.add(valueLabel, 1, row);
    }
    
    private void showUpdateStatusDialog(Order order) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Update Order Status");
        dialog.setHeaderText("Update status for Order: " + order.getOrderId());
        
        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label currentStatus = new Label("Current Status: " + order.getStatus());
        currentStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: " + getStatusColor(order.getStatus()) + ";");
        
        ComboBox<String> statusCombo = new ComboBox<>(statusOptions);
        statusCombo.setPromptText("Select new status");
        statusCombo.setPrefWidth(200);
        
        // Select current status
        String current = order.getStatus();
        if (current != null) {
            statusCombo.getSelectionModel().select(current);
        } else {
            statusCombo.getSelectionModel().selectFirst();
        }
        
        TextArea notesField = new TextArea();
        notesField.setPromptText("Optional: Add notes about this status change...");
        notesField.setPrefRowCount(3);
        
        content.getChildren().addAll(currentStatus, statusCombo, new Label("Notes:"), notesField);
        
        dialog.getDialogPane().setContent(content);
        
        // Add buttons
        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);
        
        // Enable/disable update button based on selection
        dialog.getDialogPane().lookupButton(updateButtonType).setDisable(true);
        statusCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            dialog.getDialogPane().lookupButton(updateButtonType).setDisable(
                newVal == null || newVal.equals(order.getStatus())
            );
        });
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                return statusCombo.getValue() + "|" + notesField.getText().trim();
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newStatusData -> {
            String[] parts = newStatusData.split("\\|", 2);
            String newStatus = parts[0];
            String notes = parts.length > 1 ? parts[1] : "";
            
            updateOrderStatus(order, newStatus, notes);
        });
    }
    
    private void updateOrderStatus(Order order, String newStatus, String notes) {
        System.out.println("Updating order " + order.getOrderId() + " to status: " + newStatus);
        
        new Thread(() -> {
            try {
                // Use DatabaseService connection
                String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
                try (PreparedStatement pstmt = databaseService.getConnection().prepareStatement(sql)) {
                    pstmt.setString(1, newStatus);
                    pstmt.setString(2, order.getOrderId());
                    int rowsUpdated = pstmt.executeUpdate();
                    
                    if (rowsUpdated > 0) {
                        // Update local order object
                        order.setStatus(newStatus);
                        
                        Platform.runLater(() -> {
                            // Show success message
                            Alert success = new Alert(Alert.AlertType.INFORMATION);
                            success.setTitle("Success");
                            success.setHeaderText("Status Updated");
                            success.setContentText("Order " + order.getOrderId() + " status changed to: " + newStatus);
                            success.show();
                            
                            // Refresh the display
                            applyFilters();
                            updateStatistics();
                        });
                    } else {
                        Platform.runLater(() -> {
                            Alert error = new Alert(Alert.AlertType.ERROR);
                            error.setTitle("Update Failed");
                            error.setHeaderText("Could not update order status");
                            error.setContentText("No rows were affected. Order might not exist.");
                            error.show();
                        });
                    }
                }
                
            } catch (SQLException e) {
                System.err.println("Error updating status: " + e.getMessage());
                e.printStackTrace();
                
                Platform.runLater(() -> {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Update Failed");
                    error.setHeaderText("Could not update order status");
                    error.setContentText("Database error: " + e.getMessage());
                    error.show();
                });
            }
        }).start();
    }
    
    private void updateStatistics() {
        if (allOrders.isEmpty()) {
            totalOrdersLabel.setText("0");
            totalRevenueLabel.setText("USD 0.00");
            processingLabel.setText("0");
            pendingLabel.setText("0");
            return;
        }
        
        int totalOrders = allOrders.size();
        double totalRevenue = allOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        
        long processingCount = allOrders.stream()
            .filter(o -> o.getStatus() != null && "PROCESSING".equalsIgnoreCase(o.getStatus()))
            .count();
        
        long pendingCount = allOrders.stream()
            .filter(o -> o.getStatus() != null && "PENDING".equalsIgnoreCase(o.getStatus()))
            .count();
        
        Platform.runLater(() -> {
            totalOrdersLabel.setText(String.valueOf(totalOrders));
            totalRevenueLabel.setText(String.format("USD %.2f", totalRevenue));
            processingLabel.setText(String.valueOf(processingCount));
            pendingLabel.setText(String.valueOf(pendingCount));
        });
    }
    
    private void showLoadingState() {
        Platform.runLater(() -> {
            ordersContainer.getChildren().clear();
            
            VBox loadingBox = new VBox(20);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.setPadding(new Insets(40));
            
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(50, 50);
            
            Label loadingLabel = new Label("Loading orders from database...");
            loadingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
            
            loadingBox.getChildren().addAll(spinner, loadingLabel);
            ordersContainer.getChildren().add(loadingBox);
        });
    }
    
    private void showErrorInUI(String title, String message) {
        Platform.runLater(() -> {
            ordersContainer.getChildren().clear();
            
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
            retryButton.setOnAction(e -> loadAllOrders());
            
            errorBox.getChildren().addAll(errorIcon, errorTitle, errorMessage, retryButton);
            ordersContainer.getChildren().add(errorBox);
        });
    }
    
    @FXML
    private void handleApplyFilters() {
        applyFilters();
    }
    
    @FXML
    private void handleRefresh() {
        loadAllOrders();
        
        Platform.runLater(() -> {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Refreshed");
            info.setHeaderText(null);
            info.setContentText("Orders list has been refreshed");
            info.show();
        });
    }
    
    @FXML
    private void handleClose() {
        // Close the window
        if (ordersContainer.getScene() != null && ordersContainer.getScene().getWindow() != null) {
            ordersContainer.getScene().getWindow().hide();
        }
    }
    
    private void showErrorDialog(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    public void shutdown() {
        if (databaseService != null) {
            databaseService.close();
        }
    }
}