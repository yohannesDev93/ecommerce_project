package controllers;

import models.Product;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ProductCardController {
    
    @FXML private ImageView productImage;
    @FXML private Label productName;
    @FXML private Label productCategory;
    @FXML private Label originalPrice;
    @FXML private Label finalPrice;
    @FXML private Label discountLabel;
    @FXML private Label stockStatus;
    @FXML private Button detailsButton;
    @FXML private Button cartButton;
    @FXML private Button buyButton;
    
    private Product product;
    private CustomerStoreController mainController;
    
    public void setData(Product product, CustomerStoreController mainController) {
        this.product = product;
        this.mainController = mainController;
        
        // Set product data
        productName.setText(product.getName());
        productCategory.setText(product.getCategoryName());
        
        // Set prices
        double price = product.getPrice();
        double finalPriceValue = product.getFinalPrice();
        
        finalPrice.setText(String.format("$%.2f", finalPriceValue));
        
        // Show original price and discount if there's a discount
        if (product.getDiscountPercentage() > 0) {
            originalPrice.setText(String.format("$%.2f", price));
            originalPrice.setVisible(true);
            discountLabel.setText(String.format("-%.0f%%", product.getDiscountPercentage()));
            discountLabel.setVisible(true);
        } else {
            originalPrice.setVisible(false);
            discountLabel.setVisible(false);
        }
        
        // Set stock status with better styling
        int stock = product.getStockQuantity();
        if (stock > 10) {
            stockStatus.setText("✅ In Stock");
            stockStatus.setStyle("-fx-text-fill: #27ae60;");
        } else if (stock > 0) {
            stockStatus.setText("🔥 Only " + stock + " left!");
            stockStatus.setStyle("-fx-text-fill: #f39c12;");
        } else {
            stockStatus.setText("⛔ Out of Stock");
            stockStatus.setStyle("-fx-text-fill: #e74c3c;");
            cartButton.setDisable(true);
            cartButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 6;");
            buyButton.setDisable(true);
            buyButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8;");
        }
        
        // Set image (will be set by main controller)
    }
    
    public void setProductImage(Image image) {
        productImage.setImage(image);
    }
    
    @FXML
    private void handleViewDetails(ActionEvent event) {
        if (mainController != null && product != null) {
            mainController.showProductDetails(product,event);
        }
    }
    
    @FXML
    private void handleAddToCart(ActionEvent event) {
        if (mainController != null && product != null) {
            mainController.addToCart(product,event);
        }
    }
    
    @FXML
    private void handleBuyNow(ActionEvent event) {
        if (mainController != null && product != null) {
            mainController.buyNow(product,event);
        }
    }
}