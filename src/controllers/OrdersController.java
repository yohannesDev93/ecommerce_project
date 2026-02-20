package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.Order;
import models.OrderItem;
import services.DatabaseService;

import java.sql.SQLException;
import java.util.List;

public class OrdersController {
    
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String> orderIdColumn;
    @FXML private TableColumn<Order, String> customerColumn;
    @FXML private TableColumn<Order, String> dateColumn;
    @FXML private TableColumn<Order, Double> amountColumn;
    @FXML private TableColumn<Order, String> statusColumn;
    @FXML private TableColumn<Order, String> paymentColumn;
    
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private TextField searchField;
    
    @FXML private TextArea orderDetailsArea;
    @FXML private ComboBox<String> updateStatusCombo;
    @FXML private TextArea statusNotesArea;
    
    private DatabaseService dbService;
    private ObservableList<Order> ordersList;
    
    public void initialize() {
        dbService = new DatabaseService();
        ordersList = FXCollections.observableArrayList();
        
        // Initialize table columns
        orderIdColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getOrderId()));
        
        customerColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCustomerName()));
        
        dateColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getOrderDate().toString()));
        
        amountColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleDoubleProperty(
                cellData.getValue().getTotalAmount()).asObject());
        
        statusColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));
        
        paymentColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPaymentMethod()));
        
        // Style status column
        statusColumn.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status.toLowerCase()) {
                        case "processing":
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                            break;
                        case "shipped":
                            setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                            break;
                        case "delivered":
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "cancelled":
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        // Initialize filter combos
        statusFilterCombo.getItems().addAll("All", "Processing", "Shipped", "Delivered", "Cancelled");
        statusFilterCombo.setValue("All");
        
        updateStatusCombo.getItems().addAll("Processing", "Shipped", "Delivered", "Cancelled");
        
        // Load orders
        loadOrders();
        
        // Add selection listener
        ordersTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> showOrderDetails(newValue));
        
        // Add double-click listener for quick status update
        ordersTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && ordersTable.getSelectionModel().getSelectedItem() != null) {
                Order selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
                showQuickStatusUpdateDialog(selectedOrder);
            }
        });
    }
    
    private void loadOrders() {
        try {
            List<Order> orders = dbService.getAllOrders();
            ordersList.setAll(orders);
            ordersTable.setItems(ordersList);
        } catch (Exception e) {
            showAlert("Error", "Failed to load orders: " + e.getMessage(), 
                      Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleRefresh() {
        loadOrders();
        orderDetailsArea.clear();
        updateStatusCombo.setValue(null);
        statusNotesArea.clear();
    }
    
    @FXML
    private void handleFilter() {
        String statusFilter = statusFilterCombo.getValue();
        
        if (statusFilter.equals("All")) {
            ordersTable.setItems(ordersList);
        } else {
            ObservableList<Order> filteredList = FXCollections.observableArrayList();
            for (Order order : ordersList) {
                if (order.getStatus().equalsIgnoreCase(statusFilter)) {
                    filteredList.add(order);
                }
            }
            ordersTable.setItems(filteredList);
        }
    }
    
    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase();
        
        if (searchText.isEmpty()) {
            ordersTable.setItems(ordersList);
        } else {
            ObservableList<Order> filteredList = FXCollections.observableArrayList();
            for (Order order : ordersList) {
                if (order.getOrderId().toLowerCase().contains(searchText) ||
                    order.getCustomerName().toLowerCase().contains(searchText) ||
                    order.getCustomerEmail().toLowerCase().contains(searchText)) {
                    filteredList.add(order);
                }
            }
            ordersTable.setItems(filteredList);
        }
    }
    
    private void showOrderDetails(Order order) {
        if (order == null) return;
        
        StringBuilder details = new StringBuilder();
        details.append("Order ID: ").append(order.getOrderId()).append("\n");
        details.append("Customer: ").append(order.getCustomerName()).append("\n");
        details.append("Email: ").append(order.getCustomerEmail()).append("\n");
        details.append("Phone: ").append(order.getCustomerPhone()).append("\n");
        details.append("Address: ").append(order.getShippingAddress()).append("\n\n");
        details.append("Order Date: ").append(order.getOrderDate()).append("\n");
        details.append("Payment Method: ").append(order.getPaymentMethod()).append("\n");
        details.append("Status: ").append(order.getStatus()).append("\n\n");
        details.append("ITEMS:\n");
        details.append("═══════════════════════════════════════\n");
        
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                details.append(String.format("• %s\n", item.getProductName()));
                details.append(String.format("  Quantity: %d\n", item.getQuantity()));
                details.append(String.format("  Unit Price: $%.2f\n", item.getUnitPrice()));
                details.append(String.format("  Subtotal: $%.2f\n\n", item.getSubtotal()));
            }
        }
        
        details.append("═══════════════════════════════════════\n");
        details.append(String.format("TOTAL AMOUNT: $%.2f\n", order.getTotalAmount()));
        
        orderDetailsArea.setText(details.toString());
    }
    
    @FXML
    private void handleUpdateStatus() {
        Order selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
        String newStatus = updateStatusCombo.getValue();
        String notes = statusNotesArea.getText().trim();
        
        if (selectedOrder == null) {
            showAlert("Warning", "Please select an order to update", 
                      Alert.AlertType.WARNING);
            return;
        }
        
        if (newStatus == null || newStatus.isEmpty()) {
            showAlert("Warning", "Please select a new status", 
                      Alert.AlertType.WARNING);
            return;
        }
        
        try {
            boolean success = dbService.updateOrderStatus(
                selectedOrder.getId(), newStatus, notes);
            
            if (success) {
                showAlert("Success", "Order status updated successfully!", 
                          Alert.AlertType.INFORMATION);
                selectedOrder.setStatus(newStatus);
                ordersTable.refresh();
                showOrderDetails(selectedOrder);
                statusNotesArea.clear();
            } else {
                showAlert("Error", "Failed to update order status", 
                          Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            showAlert("Error", "Database error: " + e.getMessage(), 
                      Alert.AlertType.ERROR);
        }
    }
    
    private void showQuickStatusUpdateDialog(Order order) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Quick Status Update");
        dialog.setHeaderText("Update status for Order: " + order.getOrderId());
        
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Processing", "Shipped", "Delivered", "Cancelled");
        statusCombo.setValue(order.getStatus());
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("New Status:"),
            statusCombo
        );
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return statusCombo.getValue();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(newStatus -> {
            try {
                dbService.updateOrderStatus(order.getId(), newStatus, 
                    "Status updated via quick update");
                order.setStatus(newStatus);
                ordersTable.refresh();
                showOrderDetails(order);
            } catch (Exception e) {
                showAlert("Error", "Failed to update status: " + e.getMessage(), 
                          Alert.AlertType.ERROR);
            }
        });
    }
    
    @FXML
    private void handleExportOrders() {
        // Implement export to CSV/Excel functionality
        showAlert("Info", "Export feature coming soon!", Alert.AlertType.INFORMATION);
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}