package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import models.CartItem;
import services.DatabaseService;
import models.Order;
import models.OrderItem;
import java.util.List;
import java.util.ArrayList;

public class CheckoutController {
    @FXML private VBox orderSummary;
    @FXML private Label totalLabel;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressField;
    @FXML private Label errorLabel;
    @FXML private Label paymentMethodLabel;
    
    private List<CartItem> cartItems;
    private double totalAmount;
    private String paymentMethod;
    private int currentCustomerId;
    private String currentCustomerName;
    private DatabaseService databaseService;
    
    @FXML
    public void initialize() {
        databaseService = new DatabaseService();
        errorLabel.setVisible(false);
    }
    
    public void setCartItems(List<CartItem> items) {
        this.cartItems = items;
        updateOrderSummary();
    }
    
    public void setTotalAmount(double amount) {
        this.totalAmount = amount;
        totalLabel.setText(String.format("$%.2f", amount));
    }
    
    public void setPaymentMethod(String method) {
        this.paymentMethod = method;
        if (paymentMethodLabel != null) {
            paymentMethodLabel.setText("Payment Method: " + method);
        }
    }
    
    public void setCurrentCustomerId(int id) {
        this.currentCustomerId = id;
    }
    
    public void setCurrentCustomerName(String name) {
        this.currentCustomerName = name;
        if (fullNameField != null) {
            fullNameField.setText(name);
        }
    }
    
    private void updateOrderSummary() {
        orderSummary.getChildren().clear();
        
        for (CartItem item : cartItems) {
            HBox itemRow = new HBox(10);
            itemRow.setPadding(new Insets(5, 0, 5, 0));
            
            Label nameLabel = new Label(item.getProductName());
            nameLabel.setPrefWidth(200);
            
            Label qtyLabel = new Label("x" + item.getQuantity());
            qtyLabel.setPrefWidth(50);
            
            Label priceLabel = new Label(String.format("$%.2f", item.getPrice()));
            priceLabel.setPrefWidth(80);
            
            Label subtotalLabel = new Label(String.format("$%.2f", item.getPrice() * item.getQuantity()));
            subtotalLabel.setPrefWidth(80);
            subtotalLabel.setStyle("-fx-font-weight: bold;");
            
            itemRow.getChildren().addAll(nameLabel, qtyLabel, priceLabel, subtotalLabel);
            orderSummary.getChildren().add(itemRow);
        }
    }
    
    public boolean processOrder() {
        if (!validateInput()) {
            return false;
        }
        
        try {
            // Generate order ID
            String orderId = "ORD-" + System.currentTimeMillis();
            
            // Create order
            Order order = new Order();
            order.setOrderId(orderId);
            order.setCustomerId(currentCustomerId);
            order.setCustomerName(fullNameField.getText());
            order.setCustomerEmail(emailField.getText());
            order.setShippingAddress(addressField.getText());
            order.setTotalAmount(totalAmount);
            order.setCurrency("USD");
            order.setPaymentMethod(paymentMethod);
            order.setStatus("Processing");
            
            // Create order items
            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(orderId);
                orderItem.setProductId(cartItem.getProductId());
                orderItem.setProductName(cartItem.getProductName());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setUnitPrice(cartItem.getPrice());
                orderItem.setSubtotal(cartItem.getPrice() * cartItem.getQuantity());
                orderItems.add(orderItem);
            }
            
            // Save to database
            boolean success = databaseService.saveOrder(order, orderItems);
            
            if (success) {
                // Show success message
                showSuccessAlert(orderId);
                return true;
            } else {
                errorLabel.setText("Failed to save order. Please try again.");
                errorLabel.setVisible(true);
                return false;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Error: " + e.getMessage());
            errorLabel.setVisible(true);
            return false;
        }
    }
    
    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();
        
        if (fullNameField.getText().trim().isEmpty()) {
            errors.append("• Full name is required\n");
            fullNameField.setStyle("-fx-border-color: red;");
        } else {
            fullNameField.setStyle("-fx-border-color: green;");
        }
        
        if (!emailField.getText().matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            errors.append("• Valid email is required\n");
            emailField.setStyle("-fx-border-color: red;");
        } else {
            emailField.setStyle("-fx-border-color: green;");
        }
        
        if (!phoneField.getText().matches("^\\+251\\d{9}$")) {
            errors.append("• Phone must be in format +251XXXXXXXXX\n");
            phoneField.setStyle("-fx-border-color: red;");
        } else {
            phoneField.setStyle("-fx-border-color: green;");
        }
        
        if (addressField.getText().trim().isEmpty()) {
            errors.append("• Shipping address is required\n");
            addressField.setStyle("-fx-border-color: red;");
        } else {
            addressField.setStyle("-fx-border-color: green;");
        }
        
        if (errors.length() > 0) {
            errorLabel.setText(errors.toString());
            errorLabel.setVisible(true);
            return false;
        }
        
        errorLabel.setVisible(false);
        return true;
    }
    
    private void showSuccessAlert(String orderId) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Order Placed Successfully");
        alert.setHeaderText("Thank you for your order!");
        alert.setContentText("Order ID: " + orderId + 
                           "\n\nYour order has been placed successfully.\n" +
                           "You can track your order in the 'Orders' section.");
        alert.showAndWait();
    }
}