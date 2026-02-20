package controllers;

import models.CartItem;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

public class CartDialogController {
    
    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> nameColumn;
    @FXML private TableColumn<CartItem, Double> priceColumn;
    @FXML private TableColumn<CartItem, Integer> quantityColumn;
    @FXML private TableColumn<CartItem, Double> subtotalColumn;
    @FXML private Label totalLabel;
    
    private ObservableList<CartItem> cartItems;
    private int currentCustomerId;
    private String currentCustomerName;
    
    @FXML
    public void initialize() {
        // Initialize table columns for CartItem
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("finalPrice"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        
        // Custom cell factory for subtotal
        subtotalColumn.setCellValueFactory(cellData -> {
            CartItem item = cellData.getValue();
            double subtotal = item.getSubtotal();
            return new javafx.beans.property.SimpleDoubleProperty(subtotal).asObject();
        });
        
        // Format price column
        priceColumn.setCellFactory(column -> new TableCell<CartItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", price));
                }
            }
        });
        
        // Format quantity column
        quantityColumn.setCellFactory(column -> new TableCell<CartItem, Integer>() {
            @Override
            protected void updateItem(Integer quantity, boolean empty) {
                super.updateItem(quantity, empty);
                if (empty || quantity == null) {
                    setText(null);
                } else {
                    setText(quantity.toString());
                }
            }
        });
        
        // Format subtotal column
        subtotalColumn.setCellFactory(column -> new TableCell<CartItem, Double>() {
            @Override
            protected void updateItem(Double subtotal, boolean empty) {
                super.updateItem(subtotal, empty);
                if (empty || subtotal == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", subtotal));
                }
            }
        });
    }
    
    // Update this method to accept List<CartItem>
    public void setCartItems(List<CartItem> items) {
        this.cartItems = FXCollections.observableArrayList(items);
        cartTable.setItems(cartItems);
        updateTotal();
    }
    
    public void setCurrentCustomerId(int customerId) {
        this.currentCustomerId = customerId;
    }
    
    public void setCurrentCustomerName(String customerName) {
        this.currentCustomerName = customerName;
    }
    
    private void updateTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getSubtotal();
        }
        totalLabel.setText(String.format("Total: $%.2f", total));
    }
    
    @FXML
    private void handleRemoveItem() {
        CartItem selectedItem = cartTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            cartItems.remove(selectedItem);
            updateTotal();
        }
    }
    
    @FXML
    private void handleCheckout() {
        // This will be handled by the main controller
        System.out.println("Checkout initiated for customer: " + currentCustomerName);
    }
}