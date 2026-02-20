package controllers;

import services.DatabaseService;
import models.Product;
import models.UserSession;
import models.Category;
import models.CartItem;
import models.Order;
import models.OrderItem;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.awt.desktop.UserSessionEvent;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.util.Duration;

public class CustomerStoreController {
		private String currentCurrency = "USD";
		 private UserSession userSession;
    
	 	@FXML private TextField searchField;
	    @FXML private ComboBox<String> categoryFilter;
	    @FXML private ComboBox<String> sortComboBox;
	    @FXML private Button cartButton;
	    @FXML private Button ordersButton;
	    @FXML private Button helpButton;
	    @FXML private Button logoutButton;
	    @FXML private Label welcomeLabel;
	    @FXML private FlowPane productGrid;
	    @FXML private ComboBox<String> currencyComboBox;
	    @FXML private Label currencyInfoLabel;
	    
	    
	    private DatabaseService databaseService;
	    private ObservableList<Product> productList;
	    private List<CartItem> cartItems = new ArrayList<>(); // Changed from Product to CartItem
	    private String currentCustomerName = "Guest";
	    private int currentCustomerId;
	    private Map<String, Double> currencyRates = new HashMap<>();
    
    
    
// Add these new FXML fields at the top with others:
@FXML private VBox loadingOverlay;
@FXML private ProgressIndicator loadingSpinner;
@FXML private Label dot1;
@FXML private Label dot2;
@FXML private Label dot3;
@FXML private VBox errorOverlay;
@FXML private Label errorLabel;
@FXML private ProgressBar loadingProgress;

// Add this Timeline for dot animation
private Timeline dotAnimation;
@FXML private Label loadingLabel;

public void initialize() {
    System.out.println("CustomerStoreController initialized!");
    
    try {
        // Initialize database service
        databaseService = new DatabaseService();
        currentCustomerId = userSession.getUserId();
        // Update welcome message
        welcomeLabel.setText("Welcome to our store 🎉");
        
        // Set up currency rates
        currencyRates.put("USD", 1.0);
        currencyRates.put("ETB", 155.45);
        currencyRates.put("EUR", 0.87);
        
        // Initialize combo boxes with basic data
        if (currencyComboBox.getItems().isEmpty()) {
            currencyComboBox.getItems().addAll("USD", "ETB", "EUR");
            currencyComboBox.setValue("USD");
        }
        
        if (sortComboBox.getItems().isEmpty()) {
            sortComboBox.getItems().addAll(
                "Default", 
                "Price: Low to High", 
                "Price: High to Low", 
                "Name: A-Z", 
                "Name: Z-A"
            );
            sortComboBox.setValue("Default");
        }
        
        if (categoryFilter.getItems().isEmpty()) {
            categoryFilter.getItems().add("All");
            categoryFilter.setValue("All");
        }
        
        // Add currency listener
        currencyComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentCurrency = newVal;
                updateCurrencyInfoLabel(newVal);
                refreshProductDisplay();
            }
        });
        
        // Setup search functionality
        setupSearchFunctionality();
        
        // Update cart count
        updateCartCount();
        
        // Update currency info
        updateCurrencyInfoLabel("USD");
        
        // Setup dot animation
        setupDotAnimation();
        
        // Show loading animation
        showLoading(true);
        
        // Load data asynchronously
        loadDataAsync();
        
    } catch (Exception e) {
        System.err.println("Error in initialize: " + e.getMessage());
        e.printStackTrace();
        showAlert("Error", "Failed to initialize store: " + e.getMessage());
        showError("Failed to initialize: " + e.getMessage());
    }
}

private void setupDotAnimation() {
    dotAnimation = new Timeline(
        new KeyFrame(Duration.ZERO, e -> {
            dot1.setVisible(true);
            dot2.setVisible(false);
            dot3.setVisible(false);
        }),
        new KeyFrame(Duration.millis(300), e -> {
            dot1.setVisible(true);
            dot2.setVisible(true);
            dot3.setVisible(false);
        }),
        new KeyFrame(Duration.millis(600), e -> {
            dot1.setVisible(true);
            dot2.setVisible(true);
            dot3.setVisible(true);
        }),
        new KeyFrame(Duration.millis(900), e -> {
            dot1.setVisible(false);
            dot2.setVisible(false);
            dot3.setVisible(false);
        })
    );
    dotAnimation.setCycleCount(Timeline.INDEFINITE);
}

private void showLoading(boolean show) {
    loadingOverlay.setVisible(show);
    loadingOverlay.setManaged(show);
    
    if (show) {
        // Clear product grid
        productGrid.getChildren().clear();
        
        // Start animations
        if (dotAnimation != null) {
            dotAnimation.play();
        }
        
        // Add subtle scale animation to spinner
        ScaleTransition scale = new ScaleTransition(Duration.millis(2000), loadingSpinner);
        scale.setFromX(1.5);
        scale.setFromY(1.5);
        scale.setToX(1.6);
        scale.setToY(1.6);
        scale.setAutoReverse(true);
        scale.setCycleCount(Timeline.INDEFINITE);
        scale.play();
        
        loadingLabel.setText("Loading Products");
    } else {
        // Stop animations
        if (dotAnimation != null) {
            dotAnimation.stop();
        }
        
        // Hide error overlay if it's showing
        errorOverlay.setVisible(false);
        errorOverlay.setManaged(false);
    }
}

private void showError(String message) {
    showLoading(false);
    errorLabel.setText(message);
    errorOverlay.setVisible(true);
    errorOverlay.setManaged(true);
}

@FXML
private void retryLoading() {
    showLoading(true);
    errorOverlay.setVisible(false);
    errorOverlay.setManaged(false);
    loadDataAsync();
}

private void loadDataAsync() {
    // Run database operations in background thread
    new Thread(() -> {
        try {
            // Simulate loading delay (remove in production)
            Thread.sleep(1000);
            
            // Load products
            List<Product> products = databaseService.getAllProducts();
            productList = FXCollections.observableArrayList(products);
            
            // Load categories for filter
            List<Category> categories = databaseService.getAllCategories();
            ObservableList<String> categoryOptions = FXCollections.observableArrayList("All");
            
            for (Category category : categories) {
                categoryOptions.add(category.getName());
            }
            
            // Update UI on JavaFX Application Thread
            Platform.runLater(() -> {
                // Update category filter
                categoryFilter.setItems(categoryOptions);
                categoryFilter.setValue("All");
                
                // Hide loading overlay
                showLoading(false);
                
                // Display products in batches
                displayProductsInBatches(productList);
                
                System.out.println("Loaded " + products.size() + " products from database");
            });
            
        } catch (Exception e) {
            System.err.println("Error loading data: " + e.getMessage());
            Platform.runLater(() -> {
                showError("Failed to load products: " + e.getMessage());
            });
        }
    }).start();
}

// Update your displayProductsInBatches method to hide loading when done
private void displayProductsInBatches(List<Product> products) {
    if (products.isEmpty()) {
        Label noProductsLabel = new Label("No products available");
        noProductsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
        productGrid.getChildren().add(noProductsLabel);
        return;
    }
    
    // Show loading with different message
    Platform.runLater(() -> {
        loadingLabel.setText("Rendering Products");
    });
    
    // Use Task for better performance with large product lists
    Task<Void> displayTask = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
            int batchSize = 12; // Display 12 products per batch
            int totalProducts = products.size();
            
            for (int i = 0; i < totalProducts; i += batchSize) {
                final int start = i;
                final int end = Math.min(i + batchSize, totalProducts);
                List<Product> batch = products.subList(start, end);
                
                Platform.runLater(() -> {
                    for (Product product : batch) {
                        try {
                            VBox productCard = createProductCard(product);
                            productGrid.getChildren().add(productCard);
                        } catch (Exception e) {
                            System.err.println("Error creating product card: " + e.getMessage());
                            VBox simpleCard = createSimpleProductCard(product);
                            productGrid.getChildren().add(simpleCard);
                        }
                    }
                });
                
                // Update progress
                updateProgress(i + batchSize, totalProducts);
                
                // Small delay between batches
                Thread.sleep(30);
            }
            
            Platform.runLater(() -> {
                showLoading(false);
            });
            
            return null;
        }
    };
    
    // Bind progress bar to task progress
    loadingProgress.progressProperty().bind(displayTask.progressProperty());
    
    // Start task in new thread
    new Thread(displayTask).start();
}
    
    private void openCustomerLogin(ActionEvent event) {
        try {
        	Node source = (Node) event.getSource();
	        Stage currentStage = (Stage) source.getScene().getWindow();
	        
	        // Load role selection screen
	        Parent root = FXMLLoader.load(getClass().getResource("/fxml/CustomerLogin.fxml"));
	        Stage newStage = new Stage();
	        newStage.setScene(new Scene(root, 800, 500));
	        newStage.setTitle("Welcome Back to login page");
	        newStage.setMaximized(true);
	        // Optional: Show logout confirmation
//	        System.out.println("User '" + username + "' logged out successfully");
	        
	        // Close current and show new
	        currentStage.close();
	        newStage.show();
        } catch (Exception e) {
            showAlert("Error", "Cannot load customer login: " + e.getMessage());
        }
    }
    @FXML
    private void handleCurrencyChange() {
        String selectedCurrency = currencyComboBox.getValue();
        if (selectedCurrency != null) {
            updateCurrencyInfoLabel(selectedCurrency);
            reloadProductsWithCurrency();
        }
    }


    private void reloadProductsWithCurrency() {
        String currency = currencyComboBox.getValue();
        // Update product display with new currency
        // You'll need to update your product display logic
        // to use the convertPrice() method below
    }
    
    
    private static final Map<String, Double> CONVERSION_RATES = new HashMap<>();
    static {
        CONVERSION_RATES.put("USD", 1.0);
        CONVERSION_RATES.put("ETB", 155.45);  
        CONVERSION_RATES.put("EUR", 0.89);  
    }
    
 
    
    
	private void updateCurrencyInfoLabel(String currency) {
	    double rateUSD = CONVERSION_RATES.get("USD");
	    double rateETB = CONVERSION_RATES.get("ETB");
	    double rateEUR = CONVERSION_RATES.get("EUR");
	    
	    switch(currency) {
	        case "USD":
	            currencyInfoLabel.setText(String.format("USD (1 USD = %.1f ETB = %.2f EUR)", 
	                rateETB/rateUSD, rateEUR/rateUSD));
	            break;
	        case "ETB":
	            currencyInfoLabel.setText(String.format("ETB (1 ETB = %.4f USD = %.4f EUR)", 
	                rateUSD/rateETB, rateEUR/rateETB));
	            break;
	        case "EUR":
	            currencyInfoLabel.setText(String.format("EUR (1 EUR = %.2f USD = %.1f ETB)", 
	                rateUSD/rateEUR, rateETB/rateEUR));
	            break;
	    }
	}
	
	private double convertPrice(double priceUSD, String targetCurrency) {
	    Double rate = CONVERSION_RATES.getOrDefault(targetCurrency, 1.0);
	    return priceUSD * rate;
	}

	private String formatPrice(double price, String currency) {
	    switch(currency) {
	        case "USD":
	            return String.format("$%.2f", price);
	        case "ETB":
	            return String.format("ETB %.2f", price);
	        case "EUR":
	            return String.format("€%.2f", price);
	        default:
	            return String.format("$%.2f", price);
	    }
	}
    
    
    
    private void loadProducts() {
        try {
            List<Product> products = databaseService.getAllProducts();
            productList = FXCollections.observableArrayList(products);
            displayProducts(productList);
            System.out.println("Loaded " + products.size() + " products from database");
        } catch (Exception e) {
            System.err.println("Error loading products: " + e.getMessage());
            productList = FXCollections.observableArrayList();
            displayProducts(productList);
        }
    }
    
    private void refreshProductDisplay() {
        // Refresh all product cards with new currency
        for (Node node : productGrid.getChildren()) {
            if (node instanceof VBox) {
                updateProductCard((VBox) node);
            }
        }
    }

    private void updateProductCard(VBox card) {
        // Find price label in card (it's at index 3 - priceContainer)
        if (card.getChildren().size() > 3) {
            Node priceNode = card.getChildren().get(3);
            if (priceNode instanceof HBox) {
                HBox priceContainer = (HBox) priceNode;
                // Get the product from card's user data
                Product product = (Product) card.getUserData();
                if (product != null) {
                    updatePriceContainer(priceContainer, product);
                }
            }
        }
    }

    private void updatePriceContainer(HBox priceContainer, Product product) {
        // Convert price to current currency
        double priceInUSD = product.getFinalPrice();
        double convertedPrice = convertPrice(priceInUSD, currentCurrency);
        String formattedPrice = formatPrice(convertedPrice, currentCurrency);
        
        // Clear existing price labels
        priceContainer.getChildren().clear();
        
        if (product.getDiscountPercentage() > 0) {
            // Show original price with strikethrough
            double originalConverted = convertPrice(product.getPrice(), currentCurrency);
            String originalFormatted = formatPrice(originalConverted, currentCurrency);
            
            Label originalLabel = new Label(originalFormatted);
            originalLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #95a5a6; -fx-strikethrough: true;");
            
            Label discountedLabel = new Label(formattedPrice);
            discountedLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            
            priceContainer.getChildren().addAll(originalLabel, new Label(" → "), discountedLabel);
        } else {
            // Show regular price
            Label priceLabel = new Label(formattedPrice);
            priceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            priceContainer.getChildren().add(priceLabel);
        }
    }
    
    private void displayProducts(List<Product> products) {
        // Clear existing products
        productGrid.getChildren().clear();
        
        // Create product cards for each product
        for (Product product : products) {
            try {
                VBox productCard = createProductCard(product);
                productGrid.getChildren().add(productCard);
            } catch (Exception e) {
                System.err.println("Error creating product card for " + product.getName() + ": " + e.getMessage());
                e.printStackTrace();
                // Create a simple card as fallback
                VBox simpleCard = createSimpleProductCard(product);
                productGrid.getChildren().add(simpleCard);
            }
        }
    }
    
    private VBox createProductCard(Product product) {
    // Create product card container
    VBox card = new VBox(10);
    card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 12; " +
                 "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 2);");
    card.setPrefWidth(240);
    card.setPrefHeight(380);
    
    // Store product reference in the card (IMPORTANT FOR CURRENCY UPDATES)
    card.setUserData(product);
    
    // Product Image Container with Badge
    StackPane imageContainer = new StackPane();
    
    // Product Image
    ImageView productImage = new ImageView();
    try {
        // Try to load product image
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            File imageFile = new File(product.getImageUrl());
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                productImage.setImage(image);
            } else {
                // Try URL or use default
                try {
                    Image image = new Image(product.getImageUrl());
                    productImage.setImage(image);
                } catch (Exception e) {
                    loadDefaultImage(productImage);
                }
            }
        } else {
            loadDefaultImage(productImage);
        }
    } catch (Exception e) {
        loadDefaultImage(productImage);
    }
    productImage.setFitWidth(200);
    productImage.setFitHeight(140);
    productImage.setPreserveRatio(true);
    productImage.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
    
    // Discount Badge
    VBox badgeContainer = new VBox();
    badgeContainer.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
    Label discountLabel = new Label();
    discountLabel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; " +
                         "-fx-font-size: 12px; -fx-padding: 3 8; -fx-background-radius: 0 0 0 8;");
    
    if (product.getDiscountPercentage() > 0) {
        discountLabel.setText(String.format("-%.0f%%", product.getDiscountPercentage()));
        badgeContainer.getChildren().add(discountLabel);
    }
    
    imageContainer.getChildren().addAll(productImage, badgeContainer);
    
    // Product Name
    Label nameLabel = new Label(product.getName());
    nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
    nameLabel.setWrapText(true);
    nameLabel.setMaxWidth(200);
    nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
    
    // Product Category
    Label categoryLabel = new Label(product.getCategoryName());
    categoryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-font-style: italic;");
    
    // Price Container - UPDATED FOR CURRENCY SUPPORT
    HBox priceContainer = new HBox(8);
    priceContainer.setAlignment(javafx.geometry.Pos.CENTER);
    
    // Create price display based on current currency
    updatePriceDisplay(priceContainer, product);
    
    // Stock Status
    Label stockLabel = new Label();
    int stock = product.getStockQuantity();
    if (stock > 10) {
        stockLabel.setText("✅ In Stock");
        stockLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px; -fx-font-weight: bold;");
    } else if (stock > 0) {
        stockLabel.setText("🔥 Only " + stock + " left!");
        stockLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 12px; -fx-font-weight: bold;");
    } else {
        stockLabel.setText("⛔ Out of Stock");
        stockLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;");
    }
    
    // Action Buttons Container
    HBox buttonContainer = new HBox(10);
    buttonContainer.setAlignment(javafx.geometry.Pos.CENTER);
    
    Button viewButton = new Button("👁️ Details");
    viewButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px; " +
                       "-fx-font-weight: bold; -fx-background-radius: 6;");
    viewButton.setPrefWidth(95);
    viewButton.setPrefHeight(32);
    viewButton.setOnAction(e -> showProductDetails(product,e));
    
    Button cartBtn = new Button("🛒 Add");
    cartBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 12px; " +
                    "-fx-font-weight: bold; -fx-background-radius: 6;");
    cartBtn.setPrefWidth(95);
    cartBtn.setPrefHeight(32);
    cartBtn.setOnAction(e -> addToCart(product,e));
    
    // Disable buttons if out of stock
    if (stock <= 0) {
        cartBtn.setDisable(true);
        cartBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 12px; " +
                       "-fx-font-weight: bold; -fx-background-radius: 6;");
    }
    
    buttonContainer.getChildren().addAll(viewButton, cartBtn);
    
    // Buy Now Button
    Button buyButton = new Button("🚀 BUY NOW");
    buyButton.setStyle("-fx-background-color: linear-gradient(to right, #f39c12, #e67e22); " +
                      "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; " +
                      "-fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(243, 156, 18, 0.3), 5, 0, 0, 2);");
    buyButton.setPrefWidth(200);
    buyButton.setPrefHeight(38);
    buyButton.setOnAction(e -> buyNow(product,e));
    
    if (stock <= 0) {
        buyButton.setDisable(true);
        buyButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                         "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8;");
    }
    
    // Add all elements to card
    card.getChildren().addAll(
        imageContainer, nameLabel, categoryLabel, 
        priceContainer, stockLabel, buttonContainer, buyButton
    );
    
    // Set alignment
    card.setAlignment(javafx.geometry.Pos.TOP_CENTER);
    VBox.setMargin(imageContainer, new Insets(0, 0, 10, 0));
    
    return card;
	}
	
	// NEW METHOD: Update price display based on currency
	private void updatePriceDisplay(HBox priceContainer, Product product) {
	    // Clear existing price labels
	    priceContainer.getChildren().clear();
	    
	    // Get prices in current currency
	    double priceInUSD = product.getFinalPrice();
	    double originalPriceInUSD = product.getPrice();
	    
	    double convertedPrice = convertPrice(priceInUSD, currentCurrency);
	    double originalConvertedPrice = convertPrice(originalPriceInUSD, currentCurrency);
	    
	    String formattedPrice = formatPrice(convertedPrice, currentCurrency);
	    String originalFormattedPrice = formatPrice(originalConvertedPrice, currentCurrency);
	    
	    if (product.getDiscountPercentage() > 0) {
	        // Show original price with strikethrough and discounted price
	        Label originalLabel = new Label(originalFormattedPrice);
	        originalLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #95a5a6; -fx-strikethrough: true;");
	        
	        Label discountedLabel = new Label(formattedPrice);
	        discountedLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
	        
	        priceContainer.getChildren().addAll(originalLabel, new Label(" → "), discountedLabel);
	    } else {
	        // Show regular price
	        Label priceLabel = new Label(formattedPrice);
	        priceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
	        priceContainer.getChildren().add(priceLabel);
	    }
	}
    
    private void loadDefaultImage(ImageView imageView) {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default_product.png"));
            imageView.setImage(defaultImage);
        } catch (Exception e) {
            // Use placeholder if default image not found
            imageView.setStyle("-fx-background-color: #ecf0f1; -fx-min-width: 200; -fx-min-height: 140; " +
                             "-fx-background-radius: 8;");
        }
    }
    
    private VBox createSimpleProductCard(Product product) {
	    // Create a simple card as fallback when FXML loading fails
	    VBox card = new VBox(10);
	    card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 8; " +
	                 "-fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
	    card.setPrefWidth(220);
	    card.setPrefHeight(280);
	    
	    // Store product reference (for currency updates)
	    card.setUserData(product);
	    
	    // Product name
	    Label nameLabel = new Label(product.getName());
	    nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
	    nameLabel.setWrapText(true);
	    nameLabel.setMaxWidth(180);
	    
	    // Price label with currency support
	    HBox priceBox = new HBox(5);
	    priceBox.setAlignment(javafx.geometry.Pos.CENTER);
	    
	    if (product.getDiscountPercentage() > 0) {
	        // Show original price with strikethrough
	        double originalConverted = convertPrice(product.getPrice(), currentCurrency);
	        String originalFormatted = formatPrice(originalConverted, currentCurrency);
	        
	        Label originalLabel = new Label(originalFormatted);
	        originalLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6; -fx-strikethrough: true;");
	        
	        // Show discounted price
	        double discountedConverted = convertPrice(product.getFinalPrice(), currentCurrency);
	        String discountedFormatted = formatPrice(discountedConverted, currentCurrency);
	        
	        Label discountedLabel = new Label(discountedFormatted);
	        discountedLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
	        
	        priceBox.getChildren().addAll(originalLabel, new Label("→"), discountedLabel);
	    } else {
	        // Show regular price
	        double priceConverted = convertPrice(product.getFinalPrice(), currentCurrency);
	        String priceFormatted = formatPrice(priceConverted, currentCurrency);
	        
	        Label priceLabel = new Label(priceFormatted);
	        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
	        priceBox.getChildren().add(priceLabel);
	    }
	    
	    // Category
	    Label categoryLabel = new Label(product.getCategoryName());
	    categoryLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
	    
	    // Stock info
	    Label stockLabel = new Label("Stock: " + product.getStockQuantity());
	    stockLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #27ae60;");
	    
	    // Buttons
	    HBox buttonBox = new HBox(10);
	    buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
	    
	    Button addButton = new Button("Add to Cart");
	    addButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 12px;");
	    addButton.setOnAction(e -> addToCart(product,e));
	    
	    Button buyButton = new Button("Buy Now");
	    buyButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px;");
	    buyButton.setOnAction(e -> buyNow(product,e));
	    
	    buttonBox.getChildren().addAll(addButton, buyButton);
	    
	    card.getChildren().addAll(nameLabel, categoryLabel, priceBox, stockLabel, buttonBox);
	    card.setAlignment(javafx.geometry.Pos.CENTER);
	    
	    return card;
	}
    
    private void setupSearchFunctionality() {
        // Search field listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts();
        });
        
        // Category filter listener
        categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts();
        });
        
        // Sort listener
        sortComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts();
        });
    }
    
    private void filterProducts() {
        String searchText = searchField.getText().toLowerCase();
        String selectedCategory = categoryFilter.getValue();
        
        List<Product> filtered = new ArrayList<>();
        
        for (Product product : productList) {
            boolean matchesSearch = searchText.isEmpty() ||
                product.getName().toLowerCase().contains(searchText) ||
                (product.getDescription() != null && product.getDescription().toLowerCase().contains(searchText));
            
            boolean matchesCategory = selectedCategory.equals("All Categories") ||
                (product.getCategoryName() != null && product.getCategoryName().equals(selectedCategory));
            
            if (matchesSearch && matchesCategory) {
                filtered.add(product);
            }
        }
        
        // Apply sorting
        sortProducts(filtered);
        displayProducts(filtered);
    }
    
    private void sortProducts(List<Product> products) {
        String sortOption = sortComboBox.getValue();
        if (sortOption == null) return;
        
        switch (sortOption) {
            case "Price: Low to High":
                products.sort((p1, p2) -> Double.compare(p1.getFinalPrice(), p2.getFinalPrice()));
                break;
            case "Price: High to Low":
                products.sort((p1, p2) -> Double.compare(p2.getFinalPrice(), p1.getFinalPrice()));
                break;
            case "Name: A-Z":
                products.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
                break;
            case "Name: Z-A":
                products.sort((p1, p2) -> p2.getName().compareToIgnoreCase(p1.getName()));
                break;
            case "Discount: High to Low":
                products.sort((p1, p2) -> Double.compare(p2.getDiscountPercentage(), p1.getDiscountPercentage()));
                break;
            // Featured - default order
        }
    }
    
    @FXML
    private void handleSearch() {
        filterProducts();
    }
    
  void showProductDetails(Product product, ActionEvent event) {
    // Create a custom dialog for better styling
    Dialog<Void> dialog = new Dialog<>();
    dialog.setTitle("Product Details");
    
    dialog.setX(200);
    dialog.setY(0);
    // Set dialog owner for proper window behavior
    if (event.getSource() instanceof Node) {
        Node sourceNode = (Node) event.getSource();
        dialog.initOwner(sourceNode.getScene().getWindow());
    }
    
    // ===== CRITICAL: Set result converter for close button =====
    dialog.setResultConverter(dialogButton -> {
        // This allows the dialog to close properly
        return null;
    });
    
    // Create main container
    VBox container = new VBox(15);
    container.setPadding(new Insets(20));
    container.setStyle("-fx-background-color: #f8f9fa;");
    
    // ===== HEADER SECTION =====
    HBox headerBox = new HBox(10);
    headerBox.setAlignment(Pos.CENTER_LEFT);
    
    Label productName = new Label(product.getName());
    productName.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
    
    Label categoryLabel = new Label("Category: " + product.getCategoryName());
    categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
    
    VBox headerText = new VBox(5, productName, categoryLabel);
    headerBox.getChildren().add(headerText);
    
    // ===== PRICE SECTION =====
    VBox priceSection = createSectionBox("💰 Pricing Information");
    
    HBox originalPriceBox = createInfoRow("Original Price:", 
        String.format("$%.2f", product.getPrice()));
    
    HBox discountBox = createInfoRow("Discount:", 
        String.format("%.1f%%", product.getDiscountPercentage()));
    
    HBox finalPriceBox = createInfoRow("Final Price:", 
        String.format("$%.2f", product.getFinalPrice()));
    finalPriceBox.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");
    
    double savings = product.getPrice() - product.getFinalPrice();
    HBox savingsBox = createInfoRow("You Save:", 
        String.format("$%.2f", savings));
    savingsBox.setStyle("-fx-text-fill: #e74c3c;");
    
    priceSection.getChildren().addAll(originalPriceBox, discountBox, finalPriceBox, savingsBox);
    
    // ===== STOCK SECTION =====
    VBox stockSection = createSectionBox("📦 Stock Information");
    
    HBox quantityBox = createInfoRow("Available Units:", 
        String.valueOf(product.getStockQuantity()));
    
    String stockStatus = product.getStockQuantity() > 0 ? 
        "✅ IN STOCK" : "❌ OUT OF STOCK";
    String stockColor = product.getStockQuantity() > 0 ? "#27ae60" : "#e74c3c";
    
    HBox statusBox = createInfoRow("Status:", stockStatus);
    statusBox.setStyle("-fx-font-weight: bold; -fx-text-fill: " + stockColor + ";");
    
    stockSection.getChildren().addAll(quantityBox, statusBox);
    
    // ===== DESCRIPTION SECTION =====
    VBox descSection = createSectionBox("📝 Description");
    
    TextArea descriptionArea = new TextArea(
        product.getDescription() != null && !product.getDescription().isEmpty() ?
        product.getDescription() : "No description available for this product."
    );
    descriptionArea.setEditable(false);
    descriptionArea.setWrapText(true);
    descriptionArea.setPrefHeight(100);
    descriptionArea.setStyle("-fx-control-inner-background: #ffffff; " +
                           "-fx-border-color: #bdc3c7; -fx-border-radius: 5;");
    
    descSection.getChildren().add(descriptionArea);
    
    // ===== ACTION BUTTONS =====
    HBox buttonBox = new HBox(15);
    buttonBox.setAlignment(Pos.CENTER);
    buttonBox.setPadding(new Insets(10, 0, 0, 0));
    
    Button addToCartBtn = createStyledButton("🛒 Add to Cart", "#3498db");
    addToCartBtn.setOnAction(e -> {
        dialog.setResult(null); // Important: Set result before closing
        dialog.close();
        addToCart(product, event);
    });
    
    Button buyNowBtn = createStyledButton("🚀 Buy Now", "#2ecc71");
    buyNowBtn.setOnAction(e -> {
        dialog.setResult(null); // Important: Set result before closing
        dialog.close();
        buyNow(product, event);
    });
    
    // Create close button with proper dialog button type
    ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
    dialog.getDialogPane().getButtonTypes().add(closeButtonType);
    
    // Get the actual close button from dialog pane
    Button closeButton = (Button) dialog.getDialogPane().lookupButton(closeButtonType);
    if (closeButton != null) {
        closeButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                           "-fx-font-weight: bold; -fx-font-size: 14px; " +
                           "-fx-padding: 10 20; -fx-background-radius: 5;");
        
        // Add hover effects to dialog's close button
        closeButton.setOnMouseEntered(e -> 
            closeButton.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; " +
                               "-fx-font-weight: bold; -fx-font-size: 14px; " +
                               "-fx-padding: 10 20; -fx-background-radius: 5;"));
        closeButton.setOnMouseExited(e -> 
            closeButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                               "-fx-font-weight: bold; -fx-font-size: 14px; " +
                               "-fx-padding: 10 20; -fx-background-radius: 5;"));
    }
    
    // Disable buy now button if out of stock
    if (product.getStockQuantity() <= 0) {
        buyNowBtn.setDisable(true);
        buyNowBtn.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: #7f8c8d;");
        buyNowBtn.setTooltip(new Tooltip("Product is out of stock"));
    }
    
    buttonBox.getChildren().addAll(addToCartBtn, buyNowBtn);
    
    // ===== ASSEMBLE DIALOG =====
    container.getChildren().addAll(
        headerBox,
        new Separator(),
        priceSection,
        stockSection,
        descSection,
        new Separator(),
        buttonBox
    );
    
    dialog.getDialogPane().setContent(container);
    
    // Set dialog size
    dialog.getDialogPane().setPrefWidth(700);
    dialog.getDialogPane().setPrefHeight(300);
    
    // ===== IMPORTANT: Handle window close (X button) =====
    dialog.setOnCloseRequest(e -> {
        // Allow the dialog to close
        dialog.setResult(null);
    });
    
    // ===== ALTERNATIVE SIMPLER APPROACH =====
    // If you prefer a simpler approach, use this instead of the above:
    // dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    
    // Show dialog
    dialog.showAndWait();
}

// Helper methods remain the same...
	
	// Helper method to create section boxes
	private VBox createSectionBox(String title) {
	    VBox section = new VBox(10);
	    section.setPadding(new Insets(15));
	    section.setStyle("-fx-background-color: white; -fx-border-color: #ecf0f1; " +
	                    "-fx-border-radius: 8; -fx-background-radius: 8;");
	    
	    Label sectionTitle = new Label(title);
	    sectionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
	                         "-fx-text-fill: #2c3e50; -fx-padding: 0 0 5 0;");
	    
	    section.getChildren().add(sectionTitle);
	    return section;
	}

// Helper method to create info rows
private HBox createInfoRow(String label, String value) {
    HBox row = new HBox(10);
    row.setAlignment(Pos.CENTER_LEFT);
    
    Label keyLabel = new Label(label);
    keyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e; " +
                     "-fx-min-width: 120;");
    
    Label valueLabel = new Label(value);
    valueLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; " +
                       "-fx-text-fill: #2c3e50;");
    
    row.getChildren().addAll(keyLabel, valueLabel);
    return row;
}

// Helper method to create styled buttons
private Button createStyledButton(String text, String color) {
    Button button = new Button(text);
    button.setStyle("-fx-background-color: " + color + "; " +
                   "-fx-text-fill: white; -fx-font-weight: bold; " +
                   "-fx-font-size: 14px; -fx-padding: 10 20; " +
                   "-fx-background-radius: 5; -fx-cursor: hand;");
    
    // Hover effect
    button.setOnMouseEntered(e -> 
        button.setStyle("-fx-background-color: derive(" + color + ", -20%); " +
                       "-fx-text-fill: white; -fx-font-weight: bold; " +
                       "-fx-font-size: 14px; -fx-padding: 10 20; " +
                       "-fx-background-radius: 5; -fx-cursor: hand;"));
    button.setOnMouseExited(e -> 
        button.setStyle("-fx-background-color: " + color + "; " +
                       "-fx-text-fill: white; -fx-font-weight: bold; " +
                       "-fx-font-size: 14px; -fx-padding: 10 20; " +
                       "-fx-background-radius: 5; -fx-cursor: hand;"));
    
    return button;
}
    
    
    private boolean saveOrderToDatabase(String customerName, String email, String phone, 
            String address, String paymentMethod, double total) {
			try {
			// Generate unique order ID
			String orderId = "ORD-" + System.currentTimeMillis() + "-" + 
			 (int)(Math.random() * 1000);
			
			System.out.println("Creating order: " + orderId);
			System.out.println("Customer: " + customerName);
			System.out.println("Email: " + email);
			System.out.println("Phone: " + phone);
			System.out.println("Address: " + address);
			System.out.println("Payment: " + paymentMethod);
			System.out.println("Total: $" + total);
			System.out.println("Cart items: " + cartItems.size());
			
			// Create order object
			Order order = new Order();
			order.setOrderId(orderId);
			order.setCustomerId(currentCustomerId);
			order.setCustomerName(customerName);
			order.setCustomerEmail(email);
			order.setCustomerPhone(phone);
			order.setShippingAddress(address);
			order.setTotalAmount(total);
			order.setCurrency("USD");
			order.setPaymentMethod(paymentMethod);
			order.setStatus("Processing");
			
			// Create order items from cart items
			List<OrderItem> orderItems = new ArrayList<>();
			
			for (CartItem cartItem : cartItems) {
			OrderItem orderItem = new OrderItem();
			orderItem.setOrderId(orderId);
			orderItem.setProductId(cartItem.getProductId());
			orderItem.setProductName(cartItem.getProductName());
			orderItem.setQuantity(cartItem.getQuantity());
			orderItem.setUnitPrice(cartItem.getPrice());
			orderItem.setSubtotal(cartItem.getSubtotal());
			
			orderItems.add(orderItem);
			
			System.out.println("Order item: " + cartItem.getProductName() + 
			      " x" + cartItem.getQuantity() + 
			      " @ $" + cartItem.getPrice());
			}
			
			// Save to database
			boolean success = databaseService.saveOrder(order, orderItems);
			
			if (success) {
			System.out.println("Order saved successfully to database!");
			
			// Show success message with order details
			StringBuilder orderDetails = new StringBuilder();
			orderDetails.append("🎉 Order Placed Successfully!\n\n");
			orderDetails.append("Order ID: ").append(orderId).append("\n");
			orderDetails.append("Customer: ").append(customerName).append("\n");
			orderDetails.append("Email: ").append(email).append("\n");
			orderDetails.append("Shipping Address: ").append(address).append("\n");
			orderDetails.append("Payment Method: ").append(paymentMethod).append("\n");
			orderDetails.append("Total Amount: $").append(String.format("%.2f", total)).append("\n\n");
			orderDetails.append("Order Status: Processing\n");
			orderDetails.append("You'll receive a confirmation email shortly.\n");
			orderDetails.append("You can track your order in the 'Orders' section.");
			
			showAlert("Order Confirmation", orderDetails.toString());
			return true;
			} else {
			System.err.println("Failed to save order to database!");
			showAlert("Error", "Failed to save order. Please try again.");
			return false;
			}
			
			} catch (Exception e) {
			e.printStackTrace();
			showAlert("Error", "Failed to save order: " + e.getMessage());
			return false;
			}
}
    
    
    public void setCurrentUser(UserSession session) {
        this.userSession = session;
        
        // You can now use userSession throughout your controller
        if (userSession != null && userSession.isLoggedIn()) {
            System.out.println("Store loaded for user: " + userSession.getFullName());
            System.out.println("User ID: " + userSession.getUserId());
            
            // Update UI elements if needed
            // welcomeLabel.setText("Welcome, " + userSession.getFullName());
        }
    }
   
    @FXML
	void viewOrders(ActionEvent event) {
	    try {
	        // Get the user session
	        UserSession session = UserSession.getInstance();
	         
	        
	        // Check if user is logged in
	        if (!session.isLoggedIn()) {
	            showAlert("Not Logged In", "Please login to view your orders");
	            openCustomerLogin(event);
	            return;
	        }
	        
	        // Get user ID from session
	        int currentUserId = session.getUserId();
	        if(currentUserId<1) {
	            showAlert("Not Logged In", "Please login to view your orders");
	        	openCustomerLogin(event);
	        	return;
	        }
	        // Load the orders dialog
	        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OrdersDialog.fxml"));
	        Parent root = loader.load();
	        
	        // Get the controller
	        OrdersDialogController controller = loader.getController();
	        
	        // Pass the user ID to the controller
	        controller.setCurrentUserId(currentUserId);
	        
	        // Create a new stage for the dialog
	        Stage ordersStage = new Stage();
	        ordersStage.setTitle("My Orders - " + session.getFullName());
	        ordersStage.initModality(Modality.APPLICATION_MODAL);
	        ordersStage.initOwner(cartButton.getScene().getWindow());
	        
	        Scene scene = new Scene(root);
	        scene.getStylesheets().add(getClass().getResource("/styles/orders.css").toExternalForm());
	        ordersStage.setScene(scene);
	        
	        // Show the dialog
	        ordersStage.showAndWait();
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        // Fallback to simple alert if dialog fails
	        showSimpleOrdersAlert();
	    }
	}

	
	private void showSimpleOrdersAlert() {
        try {
            List<Order> orders = databaseService.getCustomerOrders(currentCustomerId);
            
            StringBuilder ordersText = new StringBuilder();
            ordersText.append("📦 ORDER HISTORY\n\n");
            
            if (orders.isEmpty()) {
                ordersText.append("No orders found.\nStart shopping to place your first order!");
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
                for (Order order : orders) {
                    ordersText.append(String.format("🆔 %s\n", order.getOrderId()));
                    ordersText.append(String.format("📅 %s\n", sdf.format(order.getOrderDate())));
                    ordersText.append(String.format("💰 %s %.2f\n", order.getCurrency(), order.getTotalAmount()));
                    ordersText.append(String.format("📦 Status: %s\n", order.getStatus()));
                    ordersText.append(String.format("💳 Payment: %s\n", order.getPaymentMethod()));
                    ordersText.append("──────────────\n\n");
                }
            }
            
            showAlert("My Orders", ordersText.toString());
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load orders: " + e.getMessage());
        }
    }
    
    
    
    @FXML
    private void openHelp(ActionEvent event) {
        showHelpDialog(event);
    }

    private void showHelpDialog(ActionEvent event) {
    try {
        // Get current user from UserSession
        UserSession session = UserSession.getInstance();
        int currentUserId = session.getUserId();
        String fullName = session.getFullName();
        String username = session.getUsername();
        
        if(currentUserId<1) {
            showAlert("Not Logged In", "Please login to view your orders");
        	openCustomerLogin(event);
        	return;
        }
        
        System.out.println("Opening help dialog for user: " + fullName + " (ID: " + currentUserId + ")");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerHelpDialog.fxml"));
        Parent root = loader.load();
        
        CustomerHelpController controller = loader.getController();
        controller.setCustomerInfo(currentUserId, fullName, username);
        
        Stage helpStage = new Stage();
        helpStage.setTitle("Contact Support - " + fullName);
        helpStage.initModality(Modality.APPLICATION_MODAL);
        
        // Set owner window
        if (event.getSource() instanceof Node) {
            Node source = (Node) event.getSource();
            helpStage.initOwner(source.getScene().getWindow());
        }
        
        Scene scene = new Scene(root);
        helpStage.setScene(scene);
        
        helpStage.showAndWait();
        
    } catch (Exception e) {
        e.printStackTrace();
        showAlert("Error", "Could not load help dialog: " + e.getMessage());
    }
}
    
    @FXML
	private void handleLogout(ActionEvent event) {
	    try {
	        // Get the user session
	        UserSession session = UserSession.getInstance();
	        
	        // Get user info before clearing (for logging)
	        String username = session.getUsername();
	        
	        // Clear the user session
	        session.clearSession();
	        
	        // Close database connection if needed
	        if (databaseService != null) {
	            databaseService.close();
	        }
	        
	        // Get current stage
	        Node source = (Node) event.getSource();
	        Stage currentStage = (Stage) source.getScene().getWindow();
	        
	        // Load role selection screen
	        Parent root = FXMLLoader.load(getClass().getResource("/fxml/RoleSelection.fxml"));
	        Stage newStage = new Stage();
	        newStage.setScene(new Scene(root, 800, 500));
	        newStage.setTitle("E-Commerce Store - Select Role");
	        newStage.setMaximized(true);
	        // Optional: Show logout confirmation
	        System.out.println("User '" + username + "' logged out successfully");
	        
	        // Close current and show new
	        currentStage.close();
	        newStage.show();
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        showAlert("Error", "Logout failed: " + e.getMessage());
	    }
	}

    
    
    
   private double calculateCartTotal() {
    double total = 0;
    for (CartItem item : cartItems) {
        total += item.getSubtotal();
    }
    
    if(total<500 && total!=0) {
    	total=total+10;
    }
    return total;
	}
	
	private int getTotalItemCount() {
	    int count = 0;
	    for (CartItem item : cartItems) {
	        count += item.getQuantity();
	    }
	    return count;
	}
	
	private void updateCartCount() {
	    Platform.runLater(() -> {
	        int count = getTotalItemCount();
	        cartButton.setText("🛒 Cart (" + count + ")");
	    });
	}
    
	 private void showCartDialog(ActionEvent event) {
     int currentUserId = UserSession.getUserId(); 
     if(currentUserId<1) {
         showAlert("Not Logged In", "Please login to view your orders");
         openCustomerLogin(event);
         return;
     }
    if (cartItems.isEmpty()) {
        showAlert("🛒 Shopping Cart", "Your cart is empty!\n\nStart shopping to add items to your cart.");
        return;
    }
    
    // DEBUG: Print cart items
    System.out.println("=== DEBUG: Cart Items ===");
    for (CartItem item : cartItems) {
        System.out.println("Product: " + item.getProductName());
        System.out.println("Final Price: " + item.getFinalPrice());
        System.out.println("Quantity: " + item.getQuantity());
        System.out.println("Subtotal: " + item.getSubtotal());
        System.out.println("---");
    }
    System.out.println("Total: " + calculateCartTotal());
    System.out.println("=== END DEBUG ===");
    
    // Create a custom dialog
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Shopping Cart");
    dialog.setHeaderText("Your Shopping Cart (" + getTotalItemCount() + " items)");
    dialog.initOwner(cartButton.getScene().getWindow());
//    dialog.getDialogPane().setPrefSize(800, 600); // Set dialog size
    
    // Create table
    TableView<CartItem> tableView = new TableView<>();
    tableView.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-border-radius: 10px; -fx-background-radius: 10px;");
    tableView.setPrefHeight(250); 
 // Make dialog resizable
//    dialog.setResizable(true);

    // Set min size instead of pref size
    dialog.getDialogPane().setPrefSize(800, 650);

    // Create columns
    TableColumn<CartItem, String> nameColumn = new TableColumn<>("Product");
    nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductName()));
    nameColumn.setPrefWidth(300);
    nameColumn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
    
    TableColumn<CartItem, Double> priceColumn = new TableColumn<>("Price");
    priceColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getFinalPrice()).asObject());
    priceColumn.setCellFactory(column -> new TableCell<CartItem, Double>() {
        @Override
        protected void updateItem(Double price, boolean empty) {
            super.updateItem(price, empty);
            if (empty || price == null) {
                setText(null);
                setStyle("");
            } else {
                setText(String.format("$%.2f", price));
                setStyle("-fx-font-size: 14px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
                setAlignment(Pos.CENTER_RIGHT);
            }
        }
    });
    priceColumn.setPrefWidth(120);
    
    TableColumn<CartItem, Integer> quantityColumn = new TableColumn<>("Quantity");
    quantityColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getQuantity()).asObject());
    quantityColumn.setCellFactory(column -> new TableCell<CartItem, Integer>() {
        @Override
        protected void updateItem(Integer quantity, boolean empty) {
            super.updateItem(quantity, empty);
            if (empty || quantity == null) {
                setText(null);
                setStyle("");
            } else {
                setText(String.valueOf(quantity));
                setStyle("-fx-font-size: 14px; -fx-text-fill: #3498db; -fx-font-weight: bold;");
                setAlignment(Pos.CENTER);
            }
        }
    });
    quantityColumn.setPrefWidth(100);
    
    TableColumn<CartItem, Double> subtotalColumn = new TableColumn<>("Subtotal");
    subtotalColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getSubtotal()).asObject());
    subtotalColumn.setCellFactory(column -> new TableCell<CartItem, Double>() {
        @Override
        protected void updateItem(Double subtotal, boolean empty) {
            super.updateItem(subtotal, empty);
            if (empty || subtotal == null) {
                setText(null);
                setStyle("");
            } else {
                setText(String.format("$%.2f", subtotal));
                setStyle("-fx-font-size: 14px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                setAlignment(Pos.CENTER_RIGHT);
            }
        }
    });
    subtotalColumn.setPrefWidth(150);
    
    tableView.getColumns().addAll(nameColumn, priceColumn, quantityColumn, subtotalColumn);
    
    // Create observable list for table
    ObservableList<CartItem> observableCartItems = FXCollections.observableArrayList(cartItems);
    tableView.setItems(observableCartItems);
    
    // Set row factory for alternating colors
    tableView.setRowFactory(tv -> new TableRow<CartItem>() {
        @Override
        protected void updateItem(CartItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setStyle("");
            } else {
                if (getIndex() % 2 == 0) {
                    setStyle("-fx-background-color: #ffffff; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1px 0;");
                } else {
                    setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1px 0;");
                }
                setOnMouseEntered(e -> setStyle("-fx-background-color: #e8f4fd; -fx-cursor: hand;"));
                setOnMouseExited(e -> {
                    if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: #ffffff;");
                    } else {
                        setStyle("-fx-background-color: #f8f9fa;");
                    }
                });
            }
        }
    });
    
    // Calculate and display total
    double total = calculateCartTotal();
    
    // Total and shipping labels
    Label totalLabel = new Label(String.format("Total: $%.2f", total));
    totalLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; " +
                       "-fx-padding: 15px; -fx-background-color: linear-gradient(to right, #f8f9fa, #ffffff); " +
                       "-fx-border-color: #3498db; -fx-border-width: 2px; -fx-border-radius: 10px; " +
                       "-fx-background-radius: 10px;");
    
    String shipping = (total > 500 || total == 0) ? "FREE" : "$10";
    Label shippingLabel = new Label("Shipping Fee: " + shipping);
    shippingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + 
                          ((total > 500 || total == 0) ? "#27ae60" : "#f39c12") + ";");
    
    // Create styled buttons
    String buttonBaseStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10px 20px; " +
                           "-fx-background-radius: 8px; -fx-border-radius: 8px; " +
                           "-fx-cursor: hand; -fx-min-width: 120px;";
    
    Button removeButton = new Button("🗑️ Remove Selected");
    removeButton.setStyle(buttonBaseStyle + "-fx-background-color: #e74c3c; -fx-text-fill: white;");
    
    Button clearButton = new Button("🗑️ Clear Cart");
    clearButton.setStyle(buttonBaseStyle + "-fx-background-color: #95a5a6; -fx-text-fill: white;");
    
    Button increaseButton = new Button("➕");
    increaseButton.setStyle(buttonBaseStyle + "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-min-width: 60px;");
    
    Button decreaseButton = new Button("➖");
    decreaseButton.setStyle(buttonBaseStyle + "-fx-background-color: #e67e22; -fx-text-fill: white; -fx-min-width: 60px;");
    
    // Add hover effects
    addButtonHoverEffect(removeButton, "#c0392b");
    addButtonHoverEffect(clearButton, "#7f8c8d");
    addButtonHoverEffect(increaseButton, "#229954");
    addButtonHoverEffect(decreaseButton, "#d35400");
    
    // Button actions
    removeButton.setOnAction(e -> {
        CartItem selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cartItems.remove(selected);
            observableCartItems.setAll(cartItems);
            double newTotal = calculateCartTotal();
            String newShipping = (newTotal > 500 || newTotal == 0) ? "FREE" : "$10";
            totalLabel.setText(String.format("Total: $%.2f", newTotal));
            shippingLabel.setText("Shipping Fee: " + newShipping);
            shippingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + 
                                  ((newTotal > 500 || newTotal == 0) ? "#27ae60" : "#f39c12") + ";");
            updateCartCount();
        }
    });
    
    clearButton.setOnAction(e -> {
        cartItems.clear();
        dialog.close();
        updateCartCount();
        showAlert("🗑️ Cart Cleared", "Your shopping cart has been cleared.");
    });
    
    increaseButton.setOnAction(e -> {
        CartItem selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setQuantity(selected.getQuantity() + 1);
            tableView.refresh();
            double newTotal = calculateCartTotal();
            String newShipping = (newTotal > 500 || newTotal == 0) ? "FREE" : "$10";
            totalLabel.setText(String.format("Total: $%.2f", newTotal));
            shippingLabel.setText("Shipping Fee: " + newShipping);
            shippingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + 
                                  ((newTotal > 500 || newTotal == 0) ? "#27ae60" : "#f39c12") + ";");
            updateCartCount();
        }
    });
    
    decreaseButton.setOnAction(e -> {
        CartItem selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getQuantity() > 1) {
            selected.setQuantity(selected.getQuantity() - 1);
            tableView.refresh();
            double newTotal = calculateCartTotal();
            String newShipping = (newTotal > 500 || newTotal == 0) ? "FREE" : "$10";
            totalLabel.setText(String.format("Total: $%.2f", newTotal));
            shippingLabel.setText("Shipping Fee: " + newShipping);
            shippingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + 
                                  ((newTotal > 500 || newTotal == 0) ? "#27ae60" : "#f39c12") + ";");
            updateCartCount();
        }
    });
    
    // Quantity controls
    HBox quantityControls = new HBox(15);
    quantityControls.setPadding(new Insets(15, 0, 0, 0));
    quantityControls.setAlignment(Pos.CENTER_LEFT);
    
    Label quantityLabel = new Label("Adjust Quantity:");
    quantityLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
    quantityControls.getChildren().addAll(quantityLabel, decreaseButton, increaseButton);
    
    // Cart summary box
    VBox summaryBox = new VBox(10);
    summaryBox.setPadding(new Insets(20));
    summaryBox.setStyle("-fx-background-color: linear-gradient(to bottom, #f8f9fa, #ffffff); " +
                       "-fx-border-color: #e0e0e0; -fx-border-width: 1px; -fx-border-radius: 10px; " +
                       "-fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);");
    summaryBox.getChildren().addAll(totalLabel, shippingLabel, quantityControls);
    
    // Button box
    HBox buttonBox = new HBox(15, removeButton, clearButton);
    buttonBox.setPadding(new Insets(15, 0, 0, 0));
    buttonBox.setAlignment(Pos.CENTER);
    
    // Main layout
    VBox content = new VBox(20);
    content.setPadding(new Insets(25));
    content.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f0f9ff);");
    content.getChildren().addAll(tableView, summaryBox, buttonBox);
    
    dialog.getDialogPane().setContent(content);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    
    // Style dialog pane
    dialog.getDialogPane().setStyle(
        "-fx-background-color: linear-gradient(to bottom right, #ffffff, #f0f9ff); " +
        "-fx-border-color: #e0e0e0; " +
        "-fx-border-width: 1px; " +
        "-fx-border-radius: 20px; " +
        "-fx-background-radius: 20px; " +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);"
    );
    
    // Style header
    dialog.getDialogPane().lookup(".header-panel").setStyle(
        "-fx-background-color: linear-gradient(to right, #3498db, #2ecc71); " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 18px; " +
        "-fx-font-weight: bold; " +
        "-fx-padding: 20px;"
    );
 // Wrap table in ScrollPane
    ScrollPane scrollPane = new ScrollPane(tableView);
    scrollPane.setFitToWidth(true);
    scrollPane.setPrefHeight(250);
    scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

    // Replace tableView with scrollPane in the content
    content.getChildren().clear();
    content.getChildren().addAll(scrollPane, summaryBox, buttonBox);
    // Style buttons
    Button checkoutButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
    checkoutButton.setText("🛒 Proceed to Checkout");
    checkoutButton.setStyle(buttonBaseStyle + "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14px;");
    
    Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
    cancelButton.setText("Continue Shopping");
    cancelButton.setStyle(buttonBaseStyle + "-fx-background-color: #ecf0f1; -fx-text-fill: #7f8c8d; -fx-border-color: #bdc3c7; -fx-border-width: 1px;");
    
    // Add hover effects to dialog buttons
    addButtonHoverEffect(checkoutButton, "#27ae60");
    cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(buttonBaseStyle + "-fx-background-color: #3498db; -fx-text-fill: white; -fx-border-color: transparent;"));
    cancelButton.setOnMouseExited(e -> cancelButton.setStyle(buttonBaseStyle + "-fx-background-color: #ecf0f1; -fx-text-fill: #7f8c8d; -fx-border-color: #bdc3c7; -fx-border-width: 1px;"));
    
    dialog.setResultConverter(buttonType -> {
        if (buttonType == ButtonType.OK) {
            return buttonType;
        }
        return null;
    });
    
    Optional<ButtonType> result = dialog.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
        handleCheckout();
    }
}

// Helper method for button hover effects
private void addButtonHoverEffect(Button button, String hoverColor) {
    String originalStyle = button.getStyle();
    button.setOnMouseEntered(e -> button.setStyle(originalStyle + "-fx-background-color: " + hoverColor + "; " +
                                                  "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);"));
    button.setOnMouseExited(e -> button.setStyle(originalStyle + "-fx-effect: none;"));
}
    
    @FXML

	private void viewCart(ActionEvent event) {
	    showCartDialog(event);
	}
    
    // Update your addToCart method to use CartItem
    void addToCart(Product product,ActionEvent event) {
    	int currentUserId = UserSession.getUserId();
    	if(currentUserId<1) {
            showAlert("Not Logged In", "Please login to view your orders");
        	openCustomerLogin(event);
        	return;
        }
    // Check if product already in cart
    for (CartItem item : cartItems) {
        if (item.getProductId() == product.getId()) {
            item.setQuantity(item.getQuantity() + 1);
            updateCartCount();
            showAlert("✅ Cart Updated", 
                product.getName() + " quantity updated to " + item.getQuantity());
            return;
        }
    }
    
    // Create new cart item from product
    CartItem newItem = new CartItem(product);
    cartItems.add(newItem);
    
    updateCartCount();
    showAlert("✅ Added to Cart", 
        product.getName() + " has been added to your cart!\n\n" +
        "Current cart total: $" + String.format("%.2f", calculateCartTotal()) + "\n" +
        "Items in cart: " + getTotalItemCount());
}
    
    // Update buyNow method to go directly to checkout with this product
   public void buyNow(Product product,ActionEvent event) {
	   int currentUserId = UserSession.getUserId();
   	if(currentUserId<1) {
           showAlert("Not Logged In", "Please login to view your orders");
       	openCustomerLogin(event);
       	return;
       }
    if (product == null) {
        showAlert("Error", "Product not found!");
        return;
    }
    
    // Clear cart and add this product
    cartItems.clear();
    
    // Create cart item with all required data
    CartItem item = new CartItem();
    item.setProductId(product.getId());
    item.setProductName(product.getName());
    
    // Use getPrice() instead of getFinalPrice() if getFinalPrice() returns 0
    double price = product.getFinalPrice() > 0 ? product.getFinalPrice() : product.getPrice();
    item.setPrice(price);
    item.setFinalPrice(price);  // Make sure this is set
    item.setQuantity(1);
    
    // DEBUG: Print product details
    System.out.println("=== DEBUG: Buy Now Product ===");
    System.out.println("Product ID: " + product.getId());
    System.out.println("Product Name: " + product.getName());
    System.out.println("Original Price: " + product.getPrice());
    System.out.println("Final Price: " + product.getFinalPrice());
    System.out.println("Cart Item Price: " + item.getPrice());
    System.out.println("Cart Item Final Price: " + item.getFinalPrice());
    System.out.println("=== END DEBUG ===");
    
    cartItems.add(item);
    
    updateCartCount();
    handleCheckout();
}

    
    private void handleCheckout() {
        if (cartItems.isEmpty()) {
            showAlert("Warning", "Your cart is empty!");
            return;
        }
        
        try {
            // Step 1: Show payment method selection
            String paymentMethod = showPaymentMethodDialog();
            if (paymentMethod == null) {
                return; // User cancelled
            }
            
            // Step 2: Show checkout dialog
            boolean orderConfirmed = showCheckoutDialog(paymentMethod);
            if (orderConfirmed) {
                // Clear cart and show success
                cartItems.clear();
                updateCartCount();
                showAlert("Success", "Order placed successfully!\nYou can view your orders in the Orders section.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Checkout failed: " + e.getMessage());
        }
    }
    
	private String showPaymentMethodDialog() {
	    Dialog<String> dialog = new Dialog<>();
	    dialog.setTitle("Payment Method");
	    dialog.setHeaderText("Select your payment method");
	    dialog.initOwner(cartButton.getScene().getWindow());
	    
	    // Set dialog size
	    dialog.getDialogPane().setPrefSize(450, 350);
	    
	    // Create radio buttons for payment methods
	    ToggleGroup group = new ToggleGroup();
	    RadioButton telebirRadio = new RadioButton("TeleBirr");
	    RadioButton bankTransferRadio = new RadioButton("Bank Transfer");
	    RadioButton creditCardRadio = new RadioButton("Credit Card");
	    RadioButton paypalRadio = new RadioButton("PayPal");
	    
	    telebirRadio.setToggleGroup(group);
	    bankTransferRadio.setToggleGroup(group);
	    creditCardRadio.setToggleGroup(group);
	    paypalRadio.setToggleGroup(group);
	    
	    telebirRadio.setSelected(true);
	    
	    VBox content = new VBox(15);
	    content.setPadding(new Insets(25));
	    content.setAlignment(Pos.CENTER_LEFT);
	    
	    Label titleLabel = new Label("Choose Payment Method:");
	    titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
	    
	    // Create styled radio buttons
	    String radioStyle = "-fx-font-size: 14px; -fx-padding: 8px 12px; -fx-text-fill: #34495e; " +
	                       "-fx-background-color: white; -fx-background-radius: 8px; -fx-border-radius: 8px; " +
	                       "-fx-border-color: #ddd; -fx-border-width: 1px;";
	    
	    String selectedRadioStyle = "-fx-background-color: #e8f4fd; -fx-border-color: #3498db; " +
	                               "-fx-text-fill: #2980b9;";
	    
	    telebirRadio.setStyle(radioStyle);
	    bankTransferRadio.setStyle(radioStyle);
	    creditCardRadio.setStyle(radioStyle);
	    paypalRadio.setStyle(radioStyle);
	    
	    // Add hover effect
	    for (RadioButton radio : new RadioButton[]{telebirRadio, bankTransferRadio, creditCardRadio, paypalRadio}) {
	        radio.setOnMouseEntered(e -> {
	            if (!radio.isSelected()) {
	                radio.setStyle(radioStyle + "-fx-background-color: #f8f9fa; -fx-border-color: #bbb;");
	            }
	        });
	        radio.setOnMouseExited(e -> {
	            if (!radio.isSelected()) {
	                radio.setStyle(radioStyle);
	            }
	        });
	    }
	    
	    // Update style when selected
	    group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
	        if (oldToggle != null) {
	            RadioButton oldRadio = (RadioButton) oldToggle;
	            oldRadio.setStyle(radioStyle);
	        }
	        if (newToggle != null) {
	            RadioButton newRadio = (RadioButton) newToggle;
	            newRadio.setStyle(radioStyle + selectedRadioStyle);
	        }
	    });
	    
	    // Initialize first selected radio style
	    telebirRadio.setStyle(radioStyle + selectedRadioStyle);
	    
	    content.getChildren().addAll(titleLabel, telebirRadio, bankTransferRadio, creditCardRadio, paypalRadio);
	    
	    // Set dialog pane styling
	    dialog.getDialogPane().setStyle(
	        "-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9fa); " +
	        "-fx-border-color: #e0e0e0; " +
	        "-fx-border-width: 1px; " +
	        "-fx-border-radius: 12px; " +
	        "-fx-background-radius: 12px;"
	    );
	    
	    content.setStyle(
	        "-fx-background-color: transparent;" +
	        "-fx-border-color: transparent;"
	    );
	    
	    dialog.getDialogPane().setContent(content);
	    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
	    
	    // Style header
	    dialog.getDialogPane().lookup(".header-panel").setStyle(
	        "-fx-background-color: #3498db; " +
	        "-fx-text-fill: white;"
	    );
	    
	    // Style buttons
	    String buttonBaseStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10px 24px; " +
	                           "-fx-background-radius: 8px; -fx-border-radius: 8px; " +
	                           "-fx-cursor: hand;";
	    
	    Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
	    okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #3498db; " +
	                     "-fx-text-fill: white;");
	    
	    Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
	    cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;");
	    
	    // Add button hover effects
	    okButton.setOnMouseEntered(e -> okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #2980b9; " +
	                     "-fx-text-fill: white;"));
	    okButton.setOnMouseExited(e -> okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #3498db; " +
	                     "-fx-text-fill: white;"));
	    
	    cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #e74c3c; " +
	                         "-fx-text-fill: white; " +
	                         "-fx-border-color: transparent;"));
	    cancelButton.setOnMouseExited(e -> cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;"));
	    
	    // Set result converter to return selected method
	    dialog.setResultConverter(buttonType -> {
	        if (buttonType == ButtonType.OK) {
	            RadioButton selected = (RadioButton) group.getSelectedToggle();
	            if (selected != null) {
	                String method = selected.getText();
	                
	                // Show validation dialog for selected method
	                boolean isValid = showPaymentValidationDialog(method);
	                
	                if (isValid) {
	                    return method;
	                }
	            }
	        }
	        return null;
	    });
	    
	    Optional<String> result = dialog.showAndWait();
	    return result.orElse(null);
	}
	
	private boolean showPaymentValidationDialog(String paymentMethod) {
	    Dialog<Boolean> dialog = new Dialog<>();
	    dialog.setTitle(paymentMethod + " Payment");
	    dialog.setHeaderText("Enter " + paymentMethod + " details");
	    dialog.initOwner(cartButton.getScene().getWindow());
	    
	    GridPane grid = new GridPane();
	    grid.setHgap(10);
	    grid.setVgap(10);
	    grid.setPadding(new Insets(20));
	    
	    switch (paymentMethod) {
	        case "TeleBirr":
	            return showTeleBirrDialog(grid, dialog);
	        case "Bank Transfer":
	            return showBankTransferDialog(grid, dialog);
	        case "Credit Card":
	            return showCreditCardDialog(grid, dialog);
	        case "PayPal":
	            return showPayPalDialog(grid, dialog);
	        default:
	            return false;
	    }
	}
	
	private boolean showTeleBirrDialog(GridPane grid, Dialog<Boolean> dialog) {
	    // Set dialog size and styling
	    dialog.getDialogPane().setPrefSize(800, 500);
	    
	    grid.setVgap(20);
	    grid.setHgap(20);
	    grid.setPadding(new Insets(40));
	    grid.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fafc, #e8f4fd);" +
	                  "-fx-border-radius: 15px;" +
	                  "-fx-background-radius: 15px;");
	    
	    // Create styled labels
	    Label phoneLabel = new Label("Phone Number:");
	    phoneLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
	    
	    Label pinLabel = new Label("PIN:");
	    pinLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
	    
	    // Create styled text fields
	    TextField phoneField = new TextField();
	    phoneField.setPromptText("+2519XXXXXXXX or 09XXXXXXXX");
	    phoneField.setStyle("-fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 8px; " +
	                       "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;");
	    phoneField.setPrefWidth(300);
	    phoneField.setPrefHeight(40);
	    
	    PasswordField pinField = new PasswordField();
	    pinField.setPromptText("6-digit PIN");
	    pinField.setStyle("-fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 8px; " +
	                     "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;");
	    pinField.setPrefWidth(300);
	    pinField.setPrefHeight(40);
	    
	    // Add focus effects
	    phoneField.focusedProperty().addListener((obs, oldVal, newVal) -> {
	        if (newVal) {
	            phoneField.setStyle("-fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 8px; " +
	                              "-fx-border-radius: 8px; -fx-border-color: #3498db; -fx-border-width: 2px;");
	        } else {
	            phoneField.setStyle("-fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 8px; " +
	                              "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;");
	        }
	    });
	    
	    pinField.focusedProperty().addListener((obs, oldVal, newVal) -> {
	        if (newVal) {
	            pinField.setStyle("-fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 8px; " +
	                            "-fx-border-radius: 8px; -fx-border-color: #3498db; -fx-border-width: 2px;");
	        } else {
	            pinField.setStyle("-fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 8px; " +
	                            "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;");
	        }
	    });
	    
	    grid.add(phoneLabel, 0, 0);
	    grid.add(phoneField, 1, 0);
	    grid.add(pinLabel, 0, 1);
	    grid.add(pinField, 1, 1);
	    
	    // Add TeleBirr logo/icon if available
	    Label telebirrIcon = new Label("📱");
	    telebirrIcon.setStyle("-fx-font-size: 48px; -fx-padding: 20px;");
	    grid.add(telebirrIcon, 2, 0, 1, 2);
	    
	    // Create instruction label
	    Label instructionLabel = new Label("Enter your TeleBirr phone number and 6-digit PIN");
	    instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 10px 0;");
	    grid.add(instructionLabel, 0, 2, 3, 1);
	    
	    // Style the dialog pane
	    dialog.getDialogPane().setStyle(
	        "-fx-background-color: linear-gradient(to bottom right, #ffffff, #f0f9ff); " +
	        "-fx-border-color: #e0e0e0; " +
	        "-fx-border-width: 1px; " +
	        "-fx-border-radius: 20px; " +
	        "-fx-background-radius: 20px; " +
	        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);"
	    );
	    
	    dialog.getDialogPane().setContent(grid);
	    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
	    
	    // Style header
	    dialog.setTitle("TeleBirr Payment");
	    dialog.setHeaderText("TeleBirr Payment Information");
	    dialog.getDialogPane().lookup(".header-panel").setStyle(
	        "-fx-background-color: linear-gradient(to right, #3498db, #2ecc71); " +
	        "-fx-text-fill: white; " +
	        "-fx-font-size: 18px; " +
	        "-fx-font-weight: bold; " +
	        "-fx-padding: 20px;"
	    );
	    
	    // Style buttons with TeleBirr theme
	    String buttonBaseStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 30px; " +
	                           "-fx-background-radius: 8px; -fx-border-radius: 8px; " +
	                           "-fx-cursor: hand; -fx-min-width: 120px;";
	    
	    Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
	    okButton.setText("Pay Now");
	    okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #2ecc71; " +
	                     "-fx-text-fill: white;");
	    
	    Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
	    cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;");
	    
	    // Add button hover effects
	    okButton.setOnMouseEntered(e -> okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #27ae60; " +
	                     "-fx-text-fill: white; " +
	                     "-fx-effect: dropshadow(gaussian, rgba(46, 204, 113, 0.4), 10, 0, 0, 3);"));
	    okButton.setOnMouseExited(e -> okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #2ecc71; " +
	                     "-fx-text-fill: white; " +
	                     "-fx-effect: none;"));
	    
	    cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #e74c3c; " +
	                         "-fx-text-fill: white; " +
	                         "-fx-border-color: transparent;"));
	    cancelButton.setOnMouseExited(e -> cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;"));
	    
	    // Center align content
	    GridPane.setHalignment(phoneLabel, HPos.RIGHT);
	    GridPane.setHalignment(pinLabel, HPos.RIGHT);
	    GridPane.setHalignment(telebirrIcon, HPos.CENTER);
	    GridPane.setHalignment(instructionLabel, HPos.CENTER);
	    
	    // Validation
	    dialog.setResultConverter(buttonType -> {
	        if (buttonType == ButtonType.OK) {
	            String phone = phoneField.getText().trim();
	            String pin = pinField.getText().trim();
	            
	            // Normalize phone number
	            String normalizedPhone = normalizeEthiopianPhoneNumber(phone);
	            
	            // Phone validation for Ethiopian numbers
	            if (!isValidEthiopianPhoneNumber(phone)) {
	                showAlert("Validation Error", 
	                    "Phone number must be in Ethiopian format:\n" +
	                    "- Start with +2519 followed by 8 digits (e.g., +251911223344)\n" +
	                    "- OR start with 09 followed by 8 digits (e.g., 0911223344)");
	                return false;
	            }
	            
	            // PIN validation (6 digits)
	            if (!pin.matches("\\d{6}")) {
	                showAlert("Validation Error", 
	                    "PIN must be exactly 6 digits");
	                return false;
	            }
	            
	            // Save payment info
	            System.out.println("TeleBirr Payment:");
	            System.out.println("Phone: " + normalizedPhone);
	            System.out.println("PIN: ********");
	            
	            return true;
	        }
	        return false;
	    });
	    
	    return dialog.showAndWait().orElse(false);
	}
	
	// Helper method to validate Ethiopian phone numbers
	private boolean isValidEthiopianPhoneNumber(String phone) {
	    if (phone == null || phone.trim().isEmpty()) {
	        return false;
	    }
	    
	    String normalized = phone.trim();
	    
	    // Check for +2519 format (+2519 followed by 8 digits)
	    if (normalized.matches("\\+2519\\d{8}")) {
	        return true;
	    }
	    
	    // Check for 09 format (09 followed by 8 digits)
	    if (normalized.matches("09\\d{8}")) {
	        return true;
	    }
	    
	    return false;
	}
	
	// Helper method to normalize Ethiopian phone number to +251 format
	private String normalizeEthiopianPhoneNumber(String phone) {
	    if (phone == null || phone.trim().isEmpty()) {
	        return phone;
	    }
	    
	    String normalized = phone.trim();
	    
	    // If starts with 09, convert to +2519
	    if (normalized.matches("09\\d{8}")) {
	        return "+251" + normalized.substring(1);
	    }
	    
	    // If starts with +2519, keep as is
	    if (normalized.matches("\\+2519\\d{8}")) {
	        return normalized;
	    }
	    
	    // Return original if doesn't match patterns
	    return phone;
	}
	
	private boolean showBankTransferDialog(GridPane grid, Dialog<Boolean> dialog) {
    // Set dialog size and styling
    dialog.getDialogPane().setPrefSize(800, 500);
    
    grid.setVgap(20);
    grid.setHgap(20);
    grid.setPadding(new Insets(40));
    grid.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fafc, #e8f4fd);" +
                  "-fx-border-radius: 15px;" +
                  "-fx-background-radius: 15px;");
    
    // Create styled labels
    Label bankLabel = new Label("Bank:");
    Label accountLabel = new Label("Account Number:");
    Label holderLabel = new Label("Account Holder:");
    Label branchLabel = new Label("Branch (Optional):");
    
    String labelStyle = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;";
    bankLabel.setStyle(labelStyle);
    accountLabel.setStyle(labelStyle);
    holderLabel.setStyle(labelStyle);
    branchLabel.setStyle(labelStyle);
    
    // Create styled ComboBox
    ComboBox<String> bankCombo = new ComboBox<>();
    bankCombo.getItems().addAll(
        "Commercial Bank of Ethiopia (CBE)",
        "Awash International Bank",
        "Dashen Bank",
        "Bank of Abyssinia",
        "Wegagen Bank",
        "Oromia International Bank"
    );
    bankCombo.setPromptText("Select Bank");
    bankCombo.setStyle("-fx-font-size: 14px; -fx-padding: 10px; -fx-background-radius: 8px; " +
                      "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;");
    bankCombo.setPrefWidth(350);
    bankCombo.setPrefHeight(40);
    
    // Create styled text fields
    String fieldStyle = "-fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 8px; " +
                       "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;";
    
    TextField accountField = new TextField();
    accountField.setPromptText("Account Number");
    accountField.setStyle(fieldStyle);
    accountField.setPrefWidth(350);
    accountField.setPrefHeight(40);
    
    TextField holderField = new TextField();
    holderField.setPromptText("Account Holder Name");
    holderField.setStyle(fieldStyle);
    holderField.setPrefWidth(350);
    holderField.setPrefHeight(40);
    
    TextField branchField = new TextField();
    branchField.setPromptText("Branch (Optional)");
    branchField.setStyle(fieldStyle);
    branchField.setPrefWidth(350);
    branchField.setPrefHeight(40);
    
    // Add focus effects
    addFocusEffect(accountField, fieldStyle);
    addFocusEffect(holderField, fieldStyle);
    addFocusEffect(branchField, fieldStyle);
    
    // Add bank icon
    Label bankIcon = new Label("🏦");
    bankIcon.setStyle("-fx-font-size: 48px; -fx-padding: 20px;");
    
    // Add instruction label
    Label instructionLabel = new Label("Enter your bank transfer details");
    instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 10px 0;");
    
    // Add to grid
    grid.add(bankLabel, 0, 0);
    grid.add(bankCombo, 1, 0);
    grid.add(bankIcon, 2, 0, 1, 2);
    
    grid.add(accountLabel, 0, 1);
    grid.add(accountField, 1, 1);
    
    grid.add(holderLabel, 0, 2);
    grid.add(holderField, 1, 2);
    
    grid.add(branchLabel, 0, 3);
    grid.add(branchField, 1, 3);
    
    grid.add(instructionLabel, 0, 4, 3, 1);
    
    // Style the dialog pane
    dialog.getDialogPane().setStyle(
        "-fx-background-color: linear-gradient(to bottom right, #ffffff, #f0f9ff); " +
        "-fx-border-color: #e0e0e0; " +
        "-fx-border-width: 1px; " +
        "-fx-border-radius: 20px; " +
        "-fx-background-radius: 20px; " +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);"
    );
    
    dialog.getDialogPane().setContent(grid);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    
    // Style header
    dialog.setTitle("Bank Transfer");
    dialog.setHeaderText("Bank Transfer Information");
    dialog.getDialogPane().lookup(".header-panel").setStyle(
        "-fx-background-color: linear-gradient(to right, #3498db, #9b59b6); " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 18px; " +
        "-fx-font-weight: bold; " +
        "-fx-padding: 20px;"
    );
    
    // Style buttons
    String buttonBaseStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 30px; " +
                           "-fx-background-radius: 8px; -fx-border-radius: 8px; " +
                           "-fx-cursor: hand; -fx-min-width: 120px;";
    
    Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
    okButton.setText("Transfer");
    okButton.setStyle(buttonBaseStyle + 
                     "-fx-background-color: #9b59b6; " +
                     "-fx-text-fill: white;");
    
    Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
    cancelButton.setStyle(buttonBaseStyle + 
                         "-fx-background-color: #ecf0f1; " +
                         "-fx-text-fill: #7f8c8d; " +
                         "-fx-border-color: #bdc3c7; " +
                         "-fx-border-width: 1px;");
    
    // Add button hover effects
    okButton.setOnMouseEntered(e -> okButton.setStyle(buttonBaseStyle + 
                     "-fx-background-color: #8e44ad; " +
                     "-fx-text-fill: white; " +
                     "-fx-effect: dropshadow(gaussian, rgba(155, 89, 182, 0.4), 10, 0, 0, 3);"));
    okButton.setOnMouseExited(e -> okButton.setStyle(buttonBaseStyle + 
                     "-fx-background-color: #9b59b6; " +
                     "-fx-text-fill: white; " +
                     "-fx-effect: none;"));
    
    cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(buttonBaseStyle + 
                         "-fx-background-color: #e74c3c; " +
                         "-fx-text-fill: white; " +
                         "-fx-border-color: transparent;"));
    cancelButton.setOnMouseExited(e -> cancelButton.setStyle(buttonBaseStyle + 
                         "-fx-background-color: #ecf0f1; " +
                         "-fx-text-fill: #7f8c8d; " +
                         "-fx-border-color: #bdc3c7; " +
                         "-fx-border-width: 1px;"));
    
    // Center align content
    GridPane.setHalignment(bankLabel, HPos.RIGHT);
    GridPane.setHalignment(accountLabel, HPos.RIGHT);
    GridPane.setHalignment(holderLabel, HPos.RIGHT);
    GridPane.setHalignment(branchLabel, HPos.RIGHT);
    GridPane.setHalignment(bankIcon, HPos.CENTER);
    GridPane.setHalignment(instructionLabel, HPos.CENTER);
    
    // Validation
    dialog.setResultConverter(buttonType -> {
        if (buttonType == ButtonType.OK) {
            if (bankCombo.getValue() == null || bankCombo.getValue().isEmpty()) {
                showAlert("Validation Error", "Please select a bank");
                return false;
            }
            
            String account = accountField.getText().trim();
            String holder = holderField.getText().trim();
            
            if (account.isEmpty() || !account.matches("\\d{8,16}")) {
                showAlert("Validation Error", 
                    "Account number must be 8-16 digits");
                return false;
            }
            
            if (holder.isEmpty() || !holder.matches("[a-zA-Z\\s]+")) {
                showAlert("Validation Error", 
                    "Valid account holder name required");
                return false;
            }
            
            // Save payment info
            System.out.println("Bank Transfer Details:");
            System.out.println("Bank: " + bankCombo.getValue());
            System.out.println("Account: " + account);
            System.out.println("Holder: " + holder);
            System.out.println("Branch: " + branchField.getText().trim());
            
            return true;
        }
        return false;
    });
    
    return dialog.showAndWait().orElse(false);
	}
		
		private boolean showCreditCardDialog(GridPane grid, Dialog<Boolean> dialog) {
	    // Set dialog size and styling
	    dialog.getDialogPane().setPrefSize(800, 500);
	    
	    grid.setVgap(20);
	    grid.setHgap(20);
	    grid.setPadding(new Insets(40));
	    grid.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fafc, #e8f4fd);" +
	                  "-fx-border-radius: 15px;" +
	                  "-fx-background-radius: 15px;");
	    
	    // Create styled labels
	    Label cardLabel = new Label("Card Number:");
	    Label holderLabel = new Label("Card Holder:");
	    Label expiryLabel = new Label("Expiry Date:");
	    Label cvvLabel = new Label("CVV:");
	    
	    String labelStyle = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;";
	    cardLabel.setStyle(labelStyle);
	    holderLabel.setStyle(labelStyle);
	    expiryLabel.setStyle(labelStyle);
	    cvvLabel.setStyle(labelStyle);
	    
	    // Create styled text fields
	    String fieldStyle = "-fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 8px; " +
	                       "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;";
	    
	    TextField cardNumberField = new TextField();
	    cardNumberField.setPromptText("1234 5678 9012 3456");
	    cardNumberField.setStyle(fieldStyle);
	    cardNumberField.setPrefWidth(350);
	    cardNumberField.setPrefHeight(40);
	    
	    TextField holderField = new TextField();
	    holderField.setPromptText("Card Holder Name");
	    holderField.setStyle(fieldStyle);
	    holderField.setPrefWidth(350);
	    holderField.setPrefHeight(40);
	    
	    PasswordField cvvField = new PasswordField();
	    cvvField.setPromptText("CVV");
	    cvvField.setStyle(fieldStyle);
	    cvvField.setPrefWidth(150);
	    cvvField.setPrefHeight(40);
	    
	    // Create styled ComboBoxes
	    HBox expiryBox = new HBox(10);
	    expiryBox.setAlignment(Pos.CENTER_LEFT);
	    
	    ComboBox<String> monthCombo = new ComboBox<>();
	    monthCombo.getItems().addAll("01", "02", "03", "04", "05", "06", 
	                                "07", "08", "09", "10", "11", "12");
	    monthCombo.setPromptText("MM");
	    monthCombo.setStyle("-fx-font-size: 14px; -fx-padding: 10px; -fx-background-radius: 8px; " +
	                       "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;");
	    monthCombo.setPrefWidth(120);
	    monthCombo.setPrefHeight(40);
	    
	    ComboBox<String> yearCombo = new ComboBox<>();
	    int currentYear = java.time.Year.now().getValue();
	    for (int i = currentYear; i <= currentYear + 10; i++) {
	        yearCombo.getItems().add(String.valueOf(i));
	    }
	    yearCombo.setPromptText("YYYY");
	    yearCombo.setStyle("-fx-font-size: 14px; -fx-padding: 10px; -fx-background-radius: 8px; " +
	                      "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;");
	    yearCombo.setPrefWidth(120);
	    yearCombo.setPrefHeight(40);
	    
	    Label slashLabel = new Label("/");
	    slashLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 5px;");
	    
	    expiryBox.getChildren().addAll(monthCombo, slashLabel, yearCombo);
	    
	    // Add focus effects
	    addFocusEffect(cardNumberField, fieldStyle);
	    addFocusEffect(holderField, fieldStyle);
	    addFocusEffect(cvvField, fieldStyle);
	    
	    // Add credit card icon
	    Label cardIcon = new Label("💳");
	    cardIcon.setStyle("-fx-font-size: 48px; -fx-padding: 20px;");
	    
	    // Add instruction label
	    Label instructionLabel = new Label("Enter your credit card details");
	    instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 10px 0;");
	    
	    // Add to grid
	    grid.add(cardLabel, 0, 0);
	    grid.add(cardNumberField, 1, 0);
	    grid.add(cardIcon, 2, 0, 1, 2);
	    
	    grid.add(holderLabel, 0, 1);
	    grid.add(holderField, 1, 1);
	    
	    grid.add(expiryLabel, 0, 2);
	    grid.add(expiryBox, 1, 2);
	    
	    grid.add(cvvLabel, 0, 3);
	    grid.add(cvvField, 1, 3);
	    
	    grid.add(instructionLabel, 0, 4, 3, 1);
	    
	    // Style the dialog pane
	    dialog.getDialogPane().setStyle(
	        "-fx-background-color: linear-gradient(to bottom right, #ffffff, #f0f9ff); " +
	        "-fx-border-color: #e0e0e0; " +
	        "-fx-border-width: 1px; " +
	        "-fx-border-radius: 20px; " +
	        "-fx-background-radius: 20px; " +
	        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);"
	    );
	    
	    dialog.getDialogPane().setContent(grid);
	    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
	    
	    // Style header
	    dialog.setTitle("Credit Card Payment");
	    dialog.setHeaderText("Credit Card Information");
	    dialog.getDialogPane().lookup(".header-panel").setStyle(
	        "-fx-background-color: linear-gradient(to right, #3498db, #e74c3c); " +
	        "-fx-text-fill: white; " +
	        "-fx-font-size: 18px; " +
	        "-fx-font-weight: bold; " +
	        "-fx-padding: 20px;"
	    );
	    
	    // Style buttons
	    String buttonBaseStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 30px; " +
	                           "-fx-background-radius: 8px; -fx-border-radius: 8px; " +
	                           "-fx-cursor: hand; -fx-min-width: 120px;";
	    
	    Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
	    okButton.setText("Pay Now");
	    okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #e74c3c; " +
	                     "-fx-text-fill: white;");
	    
	    Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
	    cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;");
	    
	    // Add button hover effects
	    okButton.setOnMouseEntered(e -> okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #c0392b; " +
	                     "-fx-text-fill: white; " +
	                     "-fx-effect: dropshadow(gaussian, rgba(231, 76, 60, 0.4), 10, 0, 0, 3);"));
	    okButton.setOnMouseExited(e -> okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #e74c3c; " +
	                     "-fx-text-fill: white; " +
	                     "-fx-effect: none;"));
	    
	    cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #95a5a6; " +
	                         "-fx-text-fill: white; " +
	                         "-fx-border-color: transparent;"));
	    cancelButton.setOnMouseExited(e -> cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;"));
	    
	    // Center align content
	    GridPane.setHalignment(cardLabel, HPos.RIGHT);
	    GridPane.setHalignment(holderLabel, HPos.RIGHT);
	    GridPane.setHalignment(expiryLabel, HPos.RIGHT);
	    GridPane.setHalignment(cvvLabel, HPos.RIGHT);
	    GridPane.setHalignment(cardIcon, HPos.CENTER);
	    GridPane.setHalignment(instructionLabel, HPos.CENTER);
	    
	    // Validation
	    dialog.setResultConverter(buttonType -> {
	        if (buttonType == ButtonType.OK) {
	            String cardNumber = cardNumberField.getText().replaceAll("\\s+", "");
	            String holder = holderField.getText().trim();
	            String cvv = cvvField.getText().trim();
	            
	            // Card number validation (Luhn algorithm simplified)
	            if (!cardNumber.matches("\\d{16}")) {
	                showAlert("Validation Error", 
	                    "Card number must be 16 digits");
	                return false;
	            }
	            
	            if (holder.isEmpty() || !holder.matches("[a-zA-Z\\s]+")) {
	                showAlert("Validation Error", 
	                    "Valid card holder name required");
	                return false;
	            }
	            
	            if (monthCombo.getValue() == null || yearCombo.getValue() == null) {
	                showAlert("Validation Error", 
	                    "Please select expiry date");
	                return false;
	            }
	            
	            // Check if card is expired
	            int expiryMonth = Integer.parseInt(monthCombo.getValue());
	            int expiryYear = Integer.parseInt(yearCombo.getValue());
	            int currentMonth = java.time.LocalDate.now().getMonthValue();
	            int currentYearVal = java.time.Year.now().getValue();
	            
	            if (expiryYear < currentYearVal || (expiryYear == currentYearVal && expiryMonth < currentMonth)) {
	                showAlert("Validation Error", 
	                    "Card is expired");
	                return false;
	            }
	            
	            if (!cvv.matches("\\d{3,4}")) {
	                showAlert("Validation Error", 
	                    "CVV must be 3 or 4 digits");
	                return false;
	            }
	            
	            // Save payment info (in real app, use tokenization!)
	            System.out.println("Credit Card Details:");
	            System.out.println("Card: **** **** **** " + cardNumber.substring(cardNumber.length() - 4));
	            System.out.println("Holder: " + holder);
	            System.out.println("Expiry: " + monthCombo.getValue() + "/" + yearCombo.getValue());
	            
	            return true;
	        }
	        return false;
	    });
	    
	    return dialog.showAndWait().orElse(false);
	}
		
		private boolean showPayPalDialog(GridPane grid, Dialog<Boolean> dialog) {
	    // Set dialog size and styling
	    dialog.getDialogPane().setPrefSize(800, 500);
	    
	    grid.setVgap(20);
	    grid.setHgap(20);
	    grid.setPadding(new Insets(40));
	    grid.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fafc, #e8f4fd);" +
	                  "-fx-border-radius: 15px;" +
	                  "-fx-background-radius: 15px;");
	    
	    // Create styled labels
	    Label emailLabel = new Label("PayPal Email:");
	    Label passwordLabel = new Label("Password:");
	    
	    String labelStyle = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;";
	    emailLabel.setStyle(labelStyle);
	    passwordLabel.setStyle(labelStyle);
	    
	    // Create styled text fields
	    String fieldStyle = "-fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 8px; " +
	                       "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;";
	    
	    TextField emailField = new TextField();
	    emailField.setPromptText("paypal@example.com");
	    emailField.setStyle(fieldStyle);
	    emailField.setPrefWidth(350);
	    emailField.setPrefHeight(40);
	    
	    PasswordField passwordField = new PasswordField();
	    passwordField.setPromptText("PayPal Password");
	    passwordField.setStyle(fieldStyle);
	    passwordField.setPrefWidth(350);
	    passwordField.setPrefHeight(40);
	    
	    // Create styled CheckBox
	    CheckBox saveCheckbox = new CheckBox("Save payment method for future purchases");
	    saveCheckbox.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-padding: 15px 0;");
	    saveCheckbox.setSelected(true);
	    
	    // Add focus effects
	    addFocusEffect(emailField, fieldStyle);
	    addFocusEffect(passwordField, fieldStyle);
	    
	    // Add PayPal icon
	    Label paypalIcon = new Label("💰");
	    paypalIcon.setStyle("-fx-font-size: 48px; -fx-padding: 20px;");
	    
	    // Add instruction label
	    Label instructionLabel = new Label("Enter your PayPal credentials");
	    instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 10px 0;");
	    
	    // Add to grid
	    grid.add(emailLabel, 0, 0);
	    grid.add(emailField, 1, 0);
	    grid.add(paypalIcon, 2, 0, 1, 2);
	    
	    grid.add(passwordLabel, 0, 1);
	    grid.add(passwordField, 1, 1);
	    
	    grid.add(saveCheckbox, 0, 2, 3, 1);
	    
	    grid.add(instructionLabel, 0, 3, 3, 1);
	    
	    // Style the dialog pane
	    dialog.getDialogPane().setStyle(
	        "-fx-background-color: linear-gradient(to bottom right, #ffffff, #f0f9ff); " +
	        "-fx-border-color: #e0e0e0; " +
	        "-fx-border-width: 1px; " +
	        "-fx-border-radius: 20px; " +
	        "-fx-background-radius: 20px; " +
	        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);"
	    );
	    
	    dialog.getDialogPane().setContent(grid);
	    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
	    
	    // Style header with PayPal colors
	    dialog.setTitle("PayPal Payment");
	    dialog.setHeaderText("PayPal Login");
	    dialog.getDialogPane().lookup(".header-panel").setStyle(
	        "-fx-background-color: linear-gradient(to right, #003087, #009cde); " +
	        "-fx-text-fill: white; " +
	        "-fx-font-size: 18px; " +
	        "-fx-font-weight: bold; " +
	        "-fx-padding: 20px;"
	    );
	    
	    // Style buttons with PayPal colors
	    String buttonBaseStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 30px; " +
	                           "-fx-background-radius: 8px; -fx-border-radius: 8px; " +
	                           "-fx-cursor: hand; -fx-min-width: 120px;";
	    
	    Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
	    okButton.setText("Login & Pay");
	    okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #003087; " +
	                     "-fx-text-fill: white;");
	    
	    Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
	    cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;");
	    
	    // Add button hover effects
	    okButton.setOnMouseEntered(e -> okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #00205b; " +
	                     "-fx-text-fill: white; " +
	                     "-fx-effect: dropshadow(gaussian, rgba(0, 48, 135, 0.4), 10, 0, 0, 3);"));
	    okButton.setOnMouseExited(e -> okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #003087; " +
	                     "-fx-text-fill: white; " +
	                     "-fx-effect: none;"));
	    
	    cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #009cde; " +
	                         "-fx-text-fill: white; " +
	                         "-fx-border-color: transparent;"));
	    cancelButton.setOnMouseExited(e -> cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;"));
	    
	    // Center align content
	    GridPane.setHalignment(emailLabel, HPos.RIGHT);
	    GridPane.setHalignment(passwordLabel, HPos.RIGHT);
	    GridPane.setHalignment(paypalIcon, HPos.CENTER);
	    GridPane.setHalignment(saveCheckbox, HPos.CENTER);
	    GridPane.setHalignment(instructionLabel, HPos.CENTER);
	    
	    // Validation
	    dialog.setResultConverter(buttonType -> {
	        if (buttonType == ButtonType.OK) {
	            String email = emailField.getText().trim();
	            String password = passwordField.getText().trim();
	            
	            if (!email.matches("^[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) {
	                showAlert("Validation Error", 
	                    "Please enter a valid email address");
	                return false;
	            }
	            
	            if (password.length() < 6) {
	                showAlert("Validation Error", 
	                    "Password must be at least 6 characters");
	                return false;
	            }
	            
	            // Save payment info
	            System.out.println("PayPal Details:");
	            System.out.println("Email: " + email);
	            System.out.println("Password: ********");
	            System.out.println("Save for future: " + saveCheckbox.isSelected());
	            
	            return true;
	        }
	        return false;
	    });
	    
	    return dialog.showAndWait().orElse(false);
	}
	
	// Helper method for focus effects (add this to your class)
	private void addFocusEffect(TextField field, String baseStyle) {
	    field.focusedProperty().addListener((obs, oldVal, newVal) -> {
	        if (newVal) {
	            field.setStyle(baseStyle + "-fx-border-color: #3498db; -fx-border-width: 2px;");
	        } else {
	            field.setStyle(baseStyle);
	        }
	    });
	}
	private boolean showCheckoutDialog(String paymentMethod) {
	    // Calculate order summary
	    double total = calculateCartTotal();
	    
	    StringBuilder orderSummary = new StringBuilder();
	    orderSummary.append("ORDER SUMMARY\n\n");
	    
	    for (CartItem item : cartItems) {
	        orderSummary.append(String.format("%s x%d: $%.2f\n", 
	            item.getProductName(), item.getQuantity(), item.getSubtotal()));
	    }
	    
	    orderSummary.append(String.format("\nTotal: $%.2f\n", total));
	    orderSummary.append(String.format("Payment Method: %s\n\n", paymentMethod));
	    
	    // Create a simple checkout form
	    Dialog<Boolean> dialog = new Dialog<>();
	    dialog.setTitle("Checkout");
	    dialog.setHeaderText("Complete Your Order");
	    dialog.initOwner(cartButton.getScene().getWindow());
	    
	    // Set dialog size
	    dialog.getDialogPane().setPrefSize(600, 500);
	 // After creating the dialog but before showing it
	    dialog.setX(400);
	    dialog.setY(0);
	    // Create form
	    GridPane grid = new GridPane();
	    grid.setHgap(20);
	    grid.setVgap(15);
	    grid.setPadding(new Insets(25));
	    grid.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fafc, #e8f4fd);" +
	                  "-fx-border-radius: 12px;" +
	                  "-fx-background-radius: 12px;");
	    
	    String email = userSession.getEmail();
	    currentCustomerName = userSession.getFullName();
	    
	    // Create styled labels
	    String labelStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;";
	    Label nameLabel = new Label("Full Name:");
	    Label emailLabel = new Label("Email:");
	    Label phoneLabel = new Label("Phone:");
	    Label addressLabel = new Label("Shipping Address:");
	    
	    nameLabel.setStyle(labelStyle);
	    emailLabel.setStyle(labelStyle);
	    phoneLabel.setStyle(labelStyle);
	    addressLabel.setStyle(labelStyle);
	    
	    // Shipping information fields
	    String fieldStyle = "-fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 8px; " +
	                       "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;";
	    
	    TextField nameField = new TextField(currentCustomerName);
	    nameField.setStyle(fieldStyle);
	    nameField.setPrefWidth(300);
	    nameField.setPrefHeight(40);
	    
	    TextField emailField = new TextField(email);
	    emailField.setStyle(fieldStyle);
	    emailField.setPrefWidth(300);
	    emailField.setPrefHeight(40);
	    
	    TextField phoneField = new TextField("+2519123456789");
	    phoneField.setStyle(fieldStyle);
	    phoneField.setPrefWidth(300);
	    phoneField.setPrefHeight(40);
	    
	    TextArea addressArea = new TextArea();
	    addressArea.setPromptText("Enter your shipping address");
	    addressArea.setPrefRowCount(3);
	    addressArea.setPrefWidth(300);
	    addressArea.setStyle(fieldStyle);
	    addressArea.setWrapText(true);
	    
	    // Add focus effects
	    addFocusEffect(nameField, fieldStyle);
	    addFocusEffect(emailField, fieldStyle);
	    addFocusEffect(phoneField, fieldStyle);
	    
	    addressArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
	        if (newVal) {
	            addressArea.setStyle(fieldStyle + "-fx-border-color: #3498db; -fx-border-width: 2px;");
	        } else {
	            addressArea.setStyle(fieldStyle);
	        }
	    });
	    
	    // Add labels and fields to grid
	    grid.add(nameLabel, 0, 0);
	    grid.add(nameField, 1, 0);
	    grid.add(emailLabel, 0, 1);
	    grid.add(emailField, 1, 1);
	    grid.add(phoneLabel, 0, 2);
	    grid.add(phoneField, 1, 2);
	    grid.add(addressLabel, 0, 3);
	    grid.add(addressArea, 1, 3);
	    grid.setAlignment(Pos.TOP_CENTER);
	    GridPane.setHalignment(nameLabel, HPos.RIGHT);
	    GridPane.setHalignment(emailLabel, HPos.RIGHT);
	    GridPane.setHalignment(phoneLabel, HPos.RIGHT);
	    GridPane.setHalignment(addressLabel, HPos.RIGHT);
	    
	    // Order summary label with beautiful styling
	    Label summaryLabel = new Label(orderSummary.toString());
	    summaryLabel.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
	                         "-fx-font-size: 14px; " +
	                         "-fx-font-weight: bold; " +
	                         "-fx-padding: 20px; " +
	                         "-fx-background-color: linear-gradient(to right, #ffffff, #f8f9fa); " +
	                         "-fx-border-color: #3498db; " +
	                         "-fx-border-width: 2px; " +
	                         "-fx-border-radius: 10px; " +
	                         "-fx-background-radius: 10px; " +
	                         "-fx-text-fill: #2c3e50; " +
	                         "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.2), 10, 0, 0, 3);");
	    summaryLabel.setMaxWidth(600);
	    summaryLabel.setWrapText(true);
	    
	    // Shipping section header
	    Label shippingHeader = new Label("Shipping Information");
	    shippingHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; " +
	                           "-fx-padding: 10px 0; -fx-border-color: #3498db; -fx-border-width: 0 0 2px 0;");
	    
	    // Cart icon
	    Label cartIcon = new Label("🛒");
	    cartIcon.setStyle("-fx-font-size: 36px; -fx-padding: 15px;");
	    
	    HBox headerBox = new HBox(15, cartIcon, shippingHeader);
	    headerBox.setAlignment(Pos.CENTER_LEFT);
	    headerBox.setPadding(new Insets(0, 0, 10, 0));
	    
	    VBox content = new VBox(20, summaryLabel, headerBox, grid);
	    content.setAlignment(Pos.TOP_CENTER);
	    content.setPadding(new Insets(20));
	    content.setStyle("-fx-background-color: transparent;");
	    
	    // Style the dialog pane
	    dialog.getDialogPane().setStyle(
	        "-fx-background-color: linear-gradient(to bottom right, #ffffff, #f0f9ff); " +
	        "-fx-border-color: #e0e0e0; " +
	        "-fx-border-width: 1px; " +
	        "-fx-border-radius: 20px; " +
	        "-fx-background-radius: 20px; " +
	        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);"
	    );
	    
	    dialog.getDialogPane().setContent(content);
	    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
	    
	    // Style header
	    dialog.getDialogPane().lookup(".header-panel").setStyle(
	        "-fx-background-color: linear-gradient(to right, #2ecc71, #3498db); " +
	        "-fx-text-fill: white; " +
	        "-fx-font-size: 20px; " +
	        "-fx-font-weight: bold; " +
	        "-fx-padding: 25px;"
	    );
	    
	    // Style buttons
	    String buttonBaseStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 30px; " +
	                           "-fx-background-radius: 8px; -fx-border-radius: 8px; " +
	                           "-fx-cursor: hand; -fx-min-width: 120px;";
	    
	    // Rename OK button
	    Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
	    okButton.setText("Place Order ✓");
	    okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #2ecc71; " +
	                     "-fx-text-fill: white;");
	    
	    Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
	    cancelButton.setText("Cancel Order");
	    cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;");
	    
	    // Add button hover effects
	    okButton.setOnMouseEntered(e -> okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #27ae60; " +
	                     "-fx-text-fill: white; " +
	                     "-fx-effect: dropshadow(gaussian, rgba(46, 204, 113, 0.4), 10, 0, 0, 3);"));
	    okButton.setOnMouseExited(e -> okButton.setStyle(buttonBaseStyle + 
	                     "-fx-background-color: #2ecc71; " +
	                     "-fx-text-fill: white; " +
	                     "-fx-effect: none;"));
	    
	    cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #e74c3c; " +
	                         "-fx-text-fill: white; " +
	                         "-fx-border-color: transparent;"));
	    cancelButton.setOnMouseExited(e -> cancelButton.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;"));
	    
	    // Center align labels
	    GridPane.setHalignment(nameLabel, HPos.RIGHT);
	    GridPane.setHalignment(emailLabel, HPos.RIGHT);
	    GridPane.setHalignment(phoneLabel, HPos.RIGHT);
	    GridPane.setHalignment(addressLabel, HPos.RIGHT);
	    
	    // Set result converter
	    dialog.setResultConverter(buttonType -> {
	        if (buttonType == ButtonType.OK) {
	            // Validate inputs
	            if (nameField.getText().trim().isEmpty()) {
	                showAlert("Error", "Please enter your name.");
	                return false;
	            }
	            if (emailField.getText().trim().isEmpty() || !emailField.getText().contains("@")) {
	                showAlert("Error", "Please enter a valid email address.");
	                return false;
	            }
	            if (phoneField.getText().trim().isEmpty()) {
	                showAlert("Error", "Please enter a valid phone number.");
	                return false;
	            }
	            if (addressArea.getText().trim().isEmpty()) {
	                showAlert("Error", "Please enter your shipping address.");
	                return false;
	            }
	            
	            // Save order to database
	            boolean orderSaved = saveOrderToDatabase(
	                nameField.getText(),
	                emailField.getText(),
	                phoneField.getText(),
	                addressArea.getText(),
	                paymentMethod,
	                total
	            );
	            
	            return orderSaved;
	        }
	        return false;
	    });
	    
	    Optional<Boolean> result = dialog.showAndWait();
	    return result.orElse(false);
	}
    // Or create a new method to convert CartItems to Products
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(cartButton.getScene().getWindow());
        alert.showAndWait();
    }
    
}