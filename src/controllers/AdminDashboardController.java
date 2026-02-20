package controllers;

import services.DatabaseService;
import models.Product;
import models.Category;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.util.Callback;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

public class AdminDashboardController {
    
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, Integer> idColumn;
    @FXML private TableColumn<Product, String> imageColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Double> discountColumn;
    @FXML private TableColumn<Product, Double> finalPriceColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, Integer> stockColumn;
    @FXML private TableColumn<Product, String> descriptionColumn;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> sortComboBox;
    
    private ObservableList<Product> productList;
    private DatabaseService databaseService;
    
    @FXML
    public void initialize() {
        System.out.println("AdminDashboardController initialized!");
        
        try {
            // Initialize database service
            databaseService = new DatabaseService();
            
            // Load products from database
            loadProductsFromDatabase();
            
            // Configure table columns
            configureTableColumns();
            
            // Initialize filters and comboboxes
            initializeComboBoxes();
            
            // Setup search functionality
            setupSearchFunctionality();
            
            // Debug: Check table data
            debugTableData();
            
            System.out.println("Dashboard setup complete!");
            
        } catch (Exception e) {
            System.err.println("Error in initialize: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to initialize dashboard: " + e.getMessage());
        }
    }
    
    private void loadProductsFromDatabase() {
        try {
            List<Product> products = databaseService.getAllProducts();
            productList = FXCollections.observableArrayList(products);
            productsTable.setItems(productList);
            System.out.println("Loaded " + products.size() + " products from database");
        } catch (Exception e) {
            System.err.println("Error loading products: " + e.getMessage());
            productList = FXCollections.observableArrayList();
            productsTable.setItems(productList);
        }
    }
    
    private void configureTableColumns() {
        // Set column resize policy to unconstrained so all columns are visible
        productsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Set specific column widths
        idColumn.setPrefWidth(60);
        idColumn.setMinWidth(60);
        imageColumn.setPrefWidth(120);
        imageColumn.setMinWidth(120);
        nameColumn.setPrefWidth(180);
        nameColumn.setMinWidth(150);
        priceColumn.setPrefWidth(100);
        priceColumn.setMinWidth(80);
        discountColumn.setPrefWidth(90);
        discountColumn.setMinWidth(80);
        finalPriceColumn.setPrefWidth(110);
        finalPriceColumn.setMinWidth(100);
        categoryColumn.setPrefWidth(130);
        categoryColumn.setMinWidth(120);
        stockColumn.setPrefWidth(90);
        stockColumn.setMinWidth(80);
        descriptionColumn.setPrefWidth(250);
        descriptionColumn.setMinWidth(200);
        
        // Configure cell value factories - THIS IS THE KEY FIX
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        // Image column - simplified to show text first
        imageColumn.setCellValueFactory(new PropertyValueFactory<>("imageUrl"));
        imageColumn.setCellFactory(col -> new TableCell<Product, String>() {
            private final ImageView imageView = new ImageView();
            private final Label textLabel = new Label();
            
            {
                imageView.setFitHeight(50);
                imageView.setFitWidth(50);
                imageView.setPreserveRatio(true);
                textLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            }
            
            @Override
            protected void updateItem(String imageUrl, boolean empty) {
                super.updateItem(imageUrl, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Product product = getTableView().getItems().get(getIndex());
                    if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                        try {
                            File imageFile = new File(product.getImageUrl());
                            if (imageFile.exists()) {
                                Image image = new Image(imageFile.toURI().toString());
                                imageView.setImage(image);
                                setGraphic(imageView);
                                setText(null);
                            } else {
                                // Try URL
                                try {
                                    Image image = new Image(product.getImageUrl());
                                    imageView.setImage(image);
                                    setGraphic(imageView);
                                    setText(null);
                                } catch (Exception e) {
                                    showTextLabel("Invalid URL");
                                }
                            }
                        } catch (Exception e) {
                            showTextLabel("No Image");
                        }
                    } else {
                        showTextLabel("No Image");
                    }
                    setAlignment(Pos.CENTER);
                }
            }
            
            private void showTextLabel(String text) {
                textLabel.setText(text);
                setGraphic(textLabel);
                imageView.setImage(null);
            }
        });
        
        // Name column
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setCellFactory(col -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(name);
                    setTooltip(new Tooltip(name));
                }
            }
        });
        
        // Price column
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceColumn.setCellFactory(col -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", price));
                    setAlignment(Pos.CENTER_RIGHT);
                    setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
                }
            }
        });
        
        // Discount column
        discountColumn.setCellValueFactory(new PropertyValueFactory<>("discountPercentage"));
        discountColumn.setCellFactory(col -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double discount, boolean empty) {
                super.updateItem(discount, empty);
                if (empty || discount == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f%%", discount));
                    setAlignment(Pos.CENTER);
                    if (discount > 0) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #95a5a6;");
                    }
                }
            }
        });
        
        // Final price column
        finalPriceColumn.setCellValueFactory(new PropertyValueFactory<>("finalPrice"));
        finalPriceColumn.setCellFactory(col -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double finalPrice, boolean empty) {
                super.updateItem(finalPrice, empty);
                if (empty || finalPrice == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", finalPrice));
                    setAlignment(Pos.CENTER_RIGHT);
                    setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                }
            }
        });
        
        // Category column
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        categoryColumn.setCellFactory(col -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setText(null);
                } else {
                    setText(category);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        
        // Stock column
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        stockColumn.setCellFactory(col -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(stock));
                    setAlignment(Pos.CENTER);
                    if (stock == 0) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (stock < 10) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #27ae60;");
                    }
                }
            }
        });
        
        // Description column
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setCellFactory(col -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String description, boolean empty) {
                super.updateItem(description, empty);
                if (empty || description == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    // Limit description length
                    String shortDesc = description.length() > 50 ? 
                        description.substring(0, 47) + "..." : description;
                    setText(shortDesc);
                    setTooltip(new Tooltip(description));
                }
            }
        });
        
        // Ensure all columns are in the table
        productsTable.getColumns().setAll(
            idColumn, imageColumn, nameColumn, priceColumn, 
            discountColumn, finalPriceColumn, categoryColumn, 
            stockColumn, descriptionColumn
        );
        
        // Force table refresh
        Platform.runLater(() -> {
            productsTable.refresh();
        });
    }
    
    private void debugTableData() {
        Platform.runLater(() -> {
            System.out.println("\n=== DEBUG TABLE INFO ===");
            System.out.println("Table items count: " + productsTable.getItems().size());
            System.out.println("Table column count: " + productsTable.getColumns().size());
            
            if (!productList.isEmpty()) {
                Product first = productList.get(0);
                System.out.println("First product:");
                System.out.println("  ID: " + first.getId());
                System.out.println("  Name: " + first.getName());
                System.out.println("  Price: " + first.getPrice());
                System.out.println("  Image URL: " + first.getImageUrl());
                System.out.println("  Category: " + first.getCategoryName());
                System.out.println("  Stock: " + first.getStockQuantity());
            }
            
            // Check column widths
            for (TableColumn<?, ?> col : productsTable.getColumns()) {
                System.out.println("Column '" + col.getText() + "': width=" + col.getWidth() + 
                                 ", visible=" + col.isVisible());
            }
            System.out.println("========================\n");
        });
    }
    
    private void initializeComboBoxes() {
        try {
            // Load categories from database
            List<Category> categories = databaseService.getAllCategories();
            ObservableList<String> categoryOptions = FXCollections.observableArrayList("All Categories");
            
            for (Category category : categories) {
                categoryOptions.add(category.getName());
            }
            
            categoryFilter.setItems(categoryOptions);
            categoryFilter.setValue("All Categories");
            
            // Sort options
            ObservableList<String> sortOptions = FXCollections.observableArrayList(
                "Name A-Z", "Name Z-A", 
                "Price Low-High", "Price High-Low",
                "Final Price Low-High", "Final Price High-Low",
                "Discount High-Low", "Stock High-Low"
            );
            sortComboBox.setItems(sortOptions);
            sortComboBox.setValue("Name A-Z");
            
            System.out.println("ComboBoxes initialized successfully");
            
        } catch (Exception e) {
            System.err.println("Error initializing comboboxes: " + e.getMessage());
            e.printStackTrace();
        }
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
        
        ObservableList<Product> filtered = FXCollections.observableArrayList();
        
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
        
        // Apply current sort
        applySort(filtered);
        productsTable.setItems(filtered);
    }
    
    private void applySort(ObservableList<Product> list) {
        String sortOption = sortComboBox.getValue();
        if (sortOption == null) return;
        
        Comparator<Product> comparator = switch (sortOption) {
            case "Name A-Z" -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            case "Name Z-A" -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER).reversed();
            case "Price Low-High" -> Comparator.comparingDouble(Product::getPrice);
            case "Price High-Low" -> Comparator.comparingDouble(Product::getPrice).reversed();
            case "Final Price Low-High" -> Comparator.comparingDouble(Product::getFinalPrice);
            case "Final Price High-Low" -> Comparator.comparingDouble(Product::getFinalPrice).reversed();
            case "Discount High-Low" -> Comparator.comparingDouble(Product::getDiscountPercentage).reversed();
            case "Stock High-Low" -> Comparator.comparingInt(Product::getStockQuantity).reversed();
            default -> null;
        };
        
        if (comparator != null) {
            FXCollections.sort(list, comparator);
        }
    }
    
    @FXML
    private void handleSearch() {
        filterProducts();
    }
    
    @FXML
    private void addCategory() {
	    try {
	        // Create dialog for adding category
	        Dialog<Category> dialog = new Dialog<>();
	        dialog.setTitle("Add New Category");
	        dialog.setHeaderText("Enter category details:");
	        dialog.getDialogPane().setPrefSize(500, 400); // Set dialog size
	        
	        // Set button types
	        ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
	        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);
	        
	        // Create form
	        GridPane grid = new GridPane();
	        grid.setHgap(20);
	        grid.setVgap(20);
	        grid.setPadding(new javafx.geometry.Insets(30));
	        grid.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fafc, #e8f4fd);" +
	                      "-fx-border-radius: 15px;" +
	                      "-fx-background-radius: 15px;");
	        
	        // Create styled labels
	        Label nameLabel = new Label("Category Name:*");
	        Label descLabel = new Label("Description:");
	        
	        String labelStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;";
	        nameLabel.setStyle(labelStyle);
	        descLabel.setStyle(labelStyle);
	        
	        // Create styled text fields
	        String fieldStyle = "-fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 8px; " +
	                           "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;";
	        
	        TextField nameField = new TextField();
	        nameField.setPromptText("Enter category name");
	        nameField.setStyle(fieldStyle);
	        nameField.setPrefWidth(250);
	        nameField.setPrefHeight(40);
	        
	        TextArea descriptionArea = new TextArea();
	        descriptionArea.setPromptText("Enter category description");
	        descriptionArea.setPrefRowCount(4);
	        descriptionArea.setPrefWidth(250);
	        descriptionArea.setWrapText(true);
	        descriptionArea.setStyle(fieldStyle);
	        
	        // Add focus effects
	        nameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
	            if (newVal) {
	                nameField.setStyle(fieldStyle + "-fx-border-color: #3498db; -fx-border-width: 2px;");
	            } else {
	                nameField.setStyle(fieldStyle);
	            }
	        });
	        
	        descriptionArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
	            if (newVal) {
	                descriptionArea.setStyle(fieldStyle + "-fx-border-color: #3498db; -fx-border-width: 2px;");
	            } else {
	                descriptionArea.setStyle(fieldStyle);
	            }
	        });
	        
	        // Add category icon
	        Label categoryIcon = new Label("📁");
	        categoryIcon.setStyle("-fx-font-size: 48px; -fx-padding: 20px;");
	        
	        // Add instruction label
	        Label instructionLabel = new Label("Enter a unique category name and optional description");
	        instructionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d; -fx-padding: 10px 0;");
	        
	        // Add to grid
	        grid.add(nameLabel, 0, 0);
	        grid.add(nameField, 1, 0);
	        grid.add(categoryIcon, 2, 0, 1, 2);
	        
	        grid.add(descLabel, 0, 1);
	        grid.add(descriptionArea, 1, 1);
	        
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
	        
	        // Style header
	        dialog.getDialogPane().lookup(".header-panel").setStyle(
	            "-fx-background-color: linear-gradient(to right, #9b59b6, #3498db); " +
	            "-fx-text-fill: white; " +
	            "-fx-font-size: 18px; " +
	            "-fx-font-weight: bold; " +
	            "-fx-padding: 20px;"
	        );
	        
	        // Style buttons
	        String buttonBaseStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 30px; " +
	                               "-fx-background-radius: 8px; -fx-border-radius: 8px; " +
	                               "-fx-cursor: hand; -fx-min-width: 100px;";
	        
	        Button addBtn = (Button) dialog.getDialogPane().lookupButton(addButton);
	        addBtn.setText("➕ Add Category");
	        addBtn.setStyle(buttonBaseStyle + 
	                       "-fx-background-color: #9b59b6; " +
	                       "-fx-text-fill: white;");
	        
	        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
	        cancelBtn.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;");
	        
	        // Add button hover effects
	        addBtn.setOnMouseEntered(e -> addBtn.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #8e44ad; " +
	                         "-fx-text-fill: white; " +
	                         "-fx-effect: dropshadow(gaussian, rgba(155, 89, 182, 0.4), 10, 0, 0, 3);"));
	        addBtn.setOnMouseExited(e -> addBtn.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #9b59b6; " +
	                         "-fx-text-fill: white; " +
	                         "-fx-effect: none;"));
	        
	        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #e74c3c; " +
	                         "-fx-text-fill: white; " +
	                         "-fx-border-color: transparent;"));
	        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;"));
	        
	        // Center align labels
	        GridPane.setHalignment(nameLabel, HPos.RIGHT);
	        GridPane.setHalignment(descLabel, HPos.RIGHT);
	        GridPane.setHalignment(categoryIcon, HPos.CENTER);
	        GridPane.setHalignment(instructionLabel, HPos.CENTER);
	        
	        // Request focus on name field
	        Platform.runLater(() -> nameField.requestFocus());
	        
	        // Convert result
	        dialog.setResultConverter(dialogButton -> {
	            if (dialogButton == addButton) {
	                String name = nameField.getText().trim();
	                String description = descriptionArea.getText().trim();
	                if (!name.isEmpty()) {
	                    return new Category(0, name, description);
	                }else {
		                showAlert("Failed", "Please fill name of category correctly! " );

	                }
	            }
	            return null;
	        });
	        
	        dialog.showAndWait().ifPresent(newCategory -> {
	            boolean success = databaseService.addCategory(newCategory);
	            if (success) {
	                showAlert("Success", "Category added: " + newCategory.getName());
	                // Refresh categories in filter
	                initializeComboBoxes();
	            } else {
	                showAlert("Error", "Failed to add category. It might already exist.");
	            }
	        });
	    } catch (Exception e) {
	        showAlert("Error", "Failed to add category: " + e.getMessage());
	    }
	}
    
    @FXML
	private void addProduct() {
	    try {
	        // Create dialog
	        Dialog<Product> dialog = new Dialog<>();
	        dialog.setTitle("Add New Product");
	        dialog.setHeaderText("Enter product details:");
	        dialog.getDialogPane().setPrefSize(650, 700); // Set dialog size
	        
	        ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
	        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);
	        
	        // Create form
	        GridPane grid = new GridPane();
	        grid.setHgap(20);
	        grid.setVgap(15);
	        grid.setPadding(new Insets(30));
	        grid.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fafc, #e8f4fd);" +
	                      "-fx-border-radius: 15px;" +
	                      "-fx-background-radius: 15px;");
	        
	        // Create styled labels
	        String labelStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;";
	        Label nameLabel = new Label("Product Name:*");
	        Label priceLabel = new Label("Price:*");
	        Label categoryLabel = new Label("Category:*");
	        Label stockLabel = new Label("Stock Quantity:*");
	        Label discountLabel = new Label("Discount %:");
	        Label descLabel = new Label("Description:");
	        Label imageLabel = new Label("Image:");
	        
	        nameLabel.setStyle(labelStyle);
	        priceLabel.setStyle(labelStyle);
	        categoryLabel.setStyle(labelStyle);
	        stockLabel.setStyle(labelStyle);
	        discountLabel.setStyle(labelStyle);
	        descLabel.setStyle(labelStyle);
	        imageLabel.setStyle(labelStyle);
	        
	        // Create styled text fields
	        String fieldStyle = "-fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 8px; " +
	                           "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;";
	        
	        TextField nameField = new TextField();
	        nameField.setPromptText("Enter product name");
	        nameField.setStyle(fieldStyle);
	        nameField.setPrefWidth(300);
	        nameField.setPrefHeight(40);
	        
	        TextField priceField = new TextField();
	        priceField.setPromptText("e.g., 99.99");
	        priceField.setStyle(fieldStyle);
	        priceField.setPrefWidth(300);
	        priceField.setPrefHeight(40);
	        
	        TextField stockField = new TextField("10");
	        stockField.setPromptText("Stock quantity");
	        stockField.setStyle(fieldStyle);
	        stockField.setPrefWidth(300);
	        stockField.setPrefHeight(40);
	        
	        TextField discountField = new TextField("0");
	        discountField.setPromptText("0-100 %");
	        discountField.setStyle(fieldStyle);
	        discountField.setPrefWidth(300);
	        discountField.setPrefHeight(40);
	        
	        TextArea descriptionArea = new TextArea();
	        descriptionArea.setPromptText("Enter product description");
	        descriptionArea.setPrefRowCount(4);
	        descriptionArea.setPrefWidth(300);
	        descriptionArea.setWrapText(true);
	        descriptionArea.setStyle(fieldStyle);
	        
	        TextField imageUrlField = new TextField();
	        imageUrlField.setPromptText("Image URL or file path");
	        imageUrlField.setStyle(fieldStyle);
	        imageUrlField.setPrefWidth(250);
	        imageUrlField.setPrefHeight(40);
	        
	        // Create styled combo box
	        ComboBox<Category> categoryCombo = new ComboBox<>();
	        List<Category> categories = databaseService.getAllCategories();
	        categoryCombo.getItems().addAll(categories);
	        
	        categoryCombo.setCellFactory(param -> new ListCell<Category>() {
	            @Override
	            protected void updateItem(Category category, boolean empty) {
	                super.updateItem(category, empty);
	                if (empty || category == null) {
	                    setText(null);
	                    setStyle("");
	                } else {
	                    setText(category.getName());
	                    setStyle("-fx-font-size: 14px; -fx-padding: 8px;");
	                }
	            }
	        });
	        categoryCombo.setButtonCell(new ListCell<Category>() {
	            @Override
	            protected void updateItem(Category category, boolean empty) {
	                super.updateItem(category, empty);
	                if (empty || category == null) {
	                    setText(null);
	                    setStyle("");
	                } else {
	                    setText(category.getName());
	                    setStyle("-fx-font-size: 14px;");
	                }
	            }
	        });
	        
	        categoryCombo.setStyle(fieldStyle);
	        categoryCombo.setPrefWidth(300);
	        categoryCombo.setPrefHeight(40);
	        
	        if (!categories.isEmpty()) {
	            categoryCombo.setValue(categories.get(0));
	        }
	        
	        // Create styled browse button
	        Button browseButton = new Button("📁 Browse...");
	        browseButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 20px; " +
	                             "-fx-background-color: #3498db; -fx-text-fill: white; " +
	                             "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;");
	        
	        browseButton.setOnMouseEntered(e -> browseButton.setStyle(
	            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 20px; " +
	            "-fx-background-color: #2980b9; -fx-text-fill: white; " +
	            "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"));
	        browseButton.setOnMouseExited(e -> browseButton.setStyle(
	            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 20px; " +
	            "-fx-background-color: #3498db; -fx-text-fill: white; " +
	            "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"));
	        
	        browseButton.setOnAction(e -> {
	            FileChooser fileChooser = new FileChooser();
	            fileChooser.setTitle("Select Product Image");
	            fileChooser.getExtensionFilters().addAll(
	                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
	                new FileChooser.ExtensionFilter("All Files", "*.*")
	            );
	            File selectedFile = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
	            if (selectedFile != null) {
	                imageUrlField.setText(selectedFile.getAbsolutePath());
	            }
	        });
	        
	        HBox imageBox = new HBox(10, imageUrlField, browseButton);
	        imageBox.setAlignment(Pos.CENTER_LEFT);
	        
	        // Add product icon
	        Label productIcon = new Label("📦");
	        productIcon.setStyle("-fx-font-size: 48px; -fx-padding: 20px;");
	        
	        // Add instruction label
	        Label instructionLabel = new Label("Fill in the product details. Fields marked with * are required.");
	        instructionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d; -fx-padding: 10px 0; -fx-font-style: italic;");
	        
	        // Add to grid
	        grid.add(nameLabel, 0, 0);
	        grid.add(nameField, 1, 0);
	        grid.add(productIcon, 2, 0, 1, 4);
	        
	        grid.add(priceLabel, 0, 1);
	        grid.add(priceField, 1, 1);
	        
	        grid.add(categoryLabel, 0, 2);
	        grid.add(categoryCombo, 1, 2);
	        
	        grid.add(stockLabel, 0, 3);
	        grid.add(stockField, 1, 3);
	        
	        grid.add(discountLabel, 0, 4);
	        grid.add(discountField, 1, 4);
	        
	        grid.add(descLabel, 0, 5);
	        grid.add(descriptionArea, 1, 5);
	        
	        grid.add(imageLabel, 0, 6);
	        grid.add(imageBox, 1, 6);
	        
	        grid.add(instructionLabel, 0, 7, 3, 1);
	        
	        // Add focus effects
	        addFocusEffect(nameField, fieldStyle);
	        addFocusEffect(priceField, fieldStyle);
	        addFocusEffect(stockField, fieldStyle);
	        addFocusEffect(discountField, fieldStyle);
	        addFocusEffect(imageUrlField, fieldStyle);
	        
	        descriptionArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
	            if (newVal) {
	                descriptionArea.setStyle(fieldStyle + "-fx-border-color: #3498db; -fx-border-width: 2px;");
	            } else {
	                descriptionArea.setStyle(fieldStyle);
	            }
	        });
	        
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
	        
	        // Style header
	        dialog.getDialogPane().lookup(".header-panel").setStyle(
	            "-fx-background-color: linear-gradient(to right, #e74c3c, #3498db); " +
	            "-fx-text-fill: white; " +
	            "-fx-font-size: 18px; " +
	            "-fx-font-weight: bold; " +
	            "-fx-padding: 20px;"
	        );
	        
	        // Style buttons
	        String buttonBaseStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 30px; " +
	                               "-fx-background-radius: 8px; -fx-border-radius: 8px; " +
	                               "-fx-cursor: hand; -fx-min-width: 120px;";
	        
	        Button addBtn = (Button) dialog.getDialogPane().lookupButton(addButton);
	        addBtn.setText("➕ Add Product");
	        addBtn.setStyle(buttonBaseStyle + 
	                       "-fx-background-color: #e74c3c; " +
	                       "-fx-text-fill: white;");
	        
	        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
	        cancelBtn.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;");
	        
	        // Add button hover effects
	        addBtn.setOnMouseEntered(e -> addBtn.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #c0392b; " +
	                         "-fx-text-fill: white; " +
	                         "-fx-effect: dropshadow(gaussian, rgba(231, 76, 60, 0.4), 10, 0, 0, 3);"));
	        addBtn.setOnMouseExited(e -> addBtn.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #e74c3c; " +
	                         "-fx-text-fill: white; " +
	                         "-fx-effect: none;"));
	        
	        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #95a5a6; " +
	                         "-fx-text-fill: white; " +
	                         "-fx-border-color: transparent;"));
	        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(buttonBaseStyle + 
	                         "-fx-background-color: #ecf0f1; " +
	                         "-fx-text-fill: #7f8c8d; " +
	                         "-fx-border-color: #bdc3c7; " +
	                         "-fx-border-width: 1px;"));
	        
	        // Center align labels
	        GridPane.setHalignment(nameLabel, HPos.RIGHT);
	        GridPane.setHalignment(priceLabel, HPos.RIGHT);
	        GridPane.setHalignment(categoryLabel, HPos.RIGHT);
	        GridPane.setHalignment(stockLabel, HPos.RIGHT);
	        GridPane.setHalignment(discountLabel, HPos.RIGHT);
	        GridPane.setHalignment(descLabel, HPos.RIGHT);
	        GridPane.setHalignment(imageLabel, HPos.RIGHT);
	        GridPane.setHalignment(productIcon, HPos.CENTER);
	        GridPane.setHalignment(instructionLabel, HPos.CENTER);
	        
	        // Request focus on name field
	        Platform.runLater(() -> nameField.requestFocus());
	        
	        // Convert result
	        dialog.setResultConverter(dialogButton -> {
	            if (dialogButton == addButton) {
	                try {
	                    String name = nameField.getText().trim();
	                    double price = Double.parseDouble(priceField.getText().trim());
	                    Category selectedCategory = categoryCombo.getValue();
	                    int stock = Integer.parseInt(stockField.getText().trim());
	                    double discount = Double.parseDouble(discountField.getText().trim());
	                    String description = descriptionArea.getText().trim();
	                    String imageUrl = imageUrlField.getText().trim();
	                    
	                    if (name.isEmpty() || selectedCategory == null) {
	                        showAlert("Error", "Please fill all required fields (*)!");
	                        return null;
	                    }
	                    
	                    if (price <= 0) {
	                        showAlert("Error", "Price must be greater than 0!");
	                        return null;
	                    }
	                    
	                    if (stock < 0) {
	                        showAlert("Error", "Stock quantity cannot be negative!");
	                        return null;
	                    }
	                    
	                    if (discount < 0 || discount > 100) {
	                        showAlert("Error", "Discount must be between 0-100%!");
	                        return null;
	                    }
	                    if(imageUrl.isEmpty()) {
	                    	showAlert("Error", "Please choose the image!");
	                    	return null;
	                    }
	                    // Create product
	                    Product newProduct = new Product(
	                        0, // ID will be assigned by database
	                        name,
	                        price,
	                        selectedCategory.getId(),
	                        selectedCategory.getName(),
	                        description,
	                        imageUrl.isEmpty() ? null : imageUrl,
	                        stock,
	                        discount
	                    );
	                    
	                    // Add to database
	                    boolean success = databaseService.addProduct(newProduct);
	                    if (success) {
	                        return newProduct;
	                    } else {
	                        showAlert("Error", "Failed to add product to database.");
	                        return null;
	                    }
	                    
	                } catch (NumberFormatException e) {
	                    showAlert("Error", "Please enter valid numbers for price, stock, and discount!");
	                    return null;
	                }
	            }
	            return null;
	        });
	        
	        dialog.showAndWait().ifPresent(newProduct -> {
	            // Refresh product list from database
	            loadProductsFromDatabase();
	            showAlert("Success", "Product added successfully!\n\n" +
	                "Name: " + newProduct.getName() + "\n" +
	                "Price: $" + String.format("%.2f", newProduct.getPrice()) + "\n" +
	                "Final Price: $" + String.format("%.2f", newProduct.getFinalPrice()) + "\n" +
	                "Category: " + newProduct.getCategoryName());
	        });
	    } catch (Exception e) {
	        showAlert("Error", "Failed to add product: " + e.getMessage());
	    }
	}

// Add this helper method for focus effects
	private void addFocusEffect(TextField field, String baseStyle) {
	    field.focusedProperty().addListener((obs, oldVal, newVal) -> {
	        if (newVal) {
	            field.setStyle(baseStyle + "-fx-border-color: #3498db; -fx-border-width: 2px;");
	        } else {
	            field.setStyle(baseStyle);
	        }
	    });
	}
    
    @FXML
	private void editProduct() {
	    Product selected = productsTable.getSelectionModel().getSelectedItem();
	    if (selected != null) {
	        try {
	            // Create edit dialog
	            Dialog<Product> dialog = new Dialog<>();
	            dialog.setTitle("Edit Product");
	            dialog.setHeaderText("Edit: " + selected.getName());
	            dialog.getDialogPane().setPrefSize(650, 700); // Set dialog size
	            
	            ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
	            dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
	            
	            // Create form
	            GridPane grid = new GridPane();
	            grid.setHgap(20);
	            grid.setVgap(15);
	            grid.setPadding(new Insets(30));
	            grid.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fafc, #e8f4fd);" +
	                          "-fx-border-radius: 15px;" +
	                          "-fx-background-radius: 15px;");
	            
	            // Create styled labels
	            String labelStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;";
	            Label nameLabel = new Label("Product Name:*");
	            Label priceLabel = new Label("Price:*");
	            Label categoryLabel = new Label("Category:*");
	            Label stockLabel = new Label("Stock Quantity:*");
	            Label discountLabel = new Label("Discount %:");
	            Label descLabel = new Label("Description:");
	            Label imageLabel = new Label("Image URL:");
	            
	            nameLabel.setStyle(labelStyle);
	            priceLabel.setStyle(labelStyle);
	            categoryLabel.setStyle(labelStyle);
	            stockLabel.setStyle(labelStyle);
	            discountLabel.setStyle(labelStyle);
	            descLabel.setStyle(labelStyle);
	            imageLabel.setStyle(labelStyle);
	            
	            // Create styled text fields
	            String fieldStyle = "-fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 8px; " +
	                               "-fx-border-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-width: 1.5px;";
	            
	            TextField nameField = new TextField(selected.getName());
	            nameField.setStyle(fieldStyle);
	            nameField.setPrefWidth(300);
	            nameField.setPrefHeight(40);
	            
	            TextField priceField = new TextField(String.valueOf(selected.getPrice()));
	            priceField.setStyle(fieldStyle);
	            priceField.setPrefWidth(300);
	            priceField.setPrefHeight(40);
	            
	            TextField stockField = new TextField(String.valueOf(selected.getStockQuantity()));
	            stockField.setStyle(fieldStyle);
	            stockField.setPrefWidth(300);
	            stockField.setPrefHeight(40);
	            
	            TextField discountField = new TextField(String.valueOf(selected.getDiscountPercentage()));
	            discountField.setStyle(fieldStyle);
	            discountField.setPrefWidth(300);
	            discountField.setPrefHeight(40);
	            
	            TextArea descriptionArea = new TextArea(selected.getDescription() != null ? selected.getDescription() : "");
	            descriptionArea.setPrefRowCount(4);
	            descriptionArea.setPrefWidth(300);
	            descriptionArea.setWrapText(true);
	            descriptionArea.setStyle(fieldStyle);
	            
	            TextField imageUrlField = new TextField(selected.getImageUrl() != null ? selected.getImageUrl() : "");
	            imageUrlField.setPromptText("Image URL or file path");
	            imageUrlField.setStyle(fieldStyle);
	            imageUrlField.setPrefWidth(250);
	            imageUrlField.setPrefHeight(40);
	            
	            // Create styled combo box
	            ComboBox<Category> categoryCombo = new ComboBox<>();
	            List<Category> categories = databaseService.getAllCategories();
	            categoryCombo.getItems().addAll(categories);
	            
	            categoryCombo.setCellFactory(param -> new ListCell<Category>() {
	                @Override
	                protected void updateItem(Category category, boolean empty) {
	                    super.updateItem(category, empty);
	                    if (empty || category == null) {
	                        setText(null);
	                        setStyle("");
	                    } else {
	                        setText(category.getName());
	                        setStyle("-fx-font-size: 14px; -fx-padding: 8px;");
	                    }
	                }
	            });
	            categoryCombo.setButtonCell(new ListCell<Category>() {
	                @Override
	                protected void updateItem(Category category, boolean empty) {
	                    super.updateItem(category, empty);
	                    if (empty || category == null) {
	                        setText(null);
	                        setStyle("");
	                    } else {
	                        setText(category.getName());
	                        setStyle("-fx-font-size: 14px;");
	                    }
	                }
	            });
	            
	            categoryCombo.setStyle(fieldStyle);
	            categoryCombo.setPrefWidth(300);
	            categoryCombo.setPrefHeight(40);
	            
	            // Set current category
	            Category currentCategory = null;
	            for (Category cat : categories) {
	                if (cat.getId() == selected.getCategoryId()) {
	                    currentCategory = cat;
	                    break;
	                }
	            }
	            categoryCombo.setValue(currentCategory != null ? currentCategory : 
	                                  (categories.isEmpty() ? null : categories.get(0)));
	            
	            // Create styled browse button
	            Button browseButton = new Button("📁 Browse...");
	            browseButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 20px; " +
	                                 "-fx-background-color: #3498db; -fx-text-fill: white; " +
	                                 "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;");
	            
	            browseButton.setOnMouseEntered(e -> browseButton.setStyle(
	                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 20px; " +
	                "-fx-background-color: #2980b9; -fx-text-fill: white; " +
	                "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"));
	            browseButton.setOnMouseExited(e -> browseButton.setStyle(
	                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 20px; " +
	                "-fx-background-color: #3498db; -fx-text-fill: white; " +
	                "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"));
	            
	            browseButton.setOnAction(e -> {
	                FileChooser fileChooser = new FileChooser();
	                fileChooser.setTitle("Select Product Image");
	                fileChooser.getExtensionFilters().addAll(
	                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
	                    new FileChooser.ExtensionFilter("All Files", "*.*")
	                );
	                File selectedFile = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
	                if (selectedFile != null) {
	                    imageUrlField.setText(selectedFile.getAbsolutePath());
	                }
	            });
	            
	            HBox imageBox = new HBox(10, imageUrlField, browseButton);
	            imageBox.setAlignment(Pos.CENTER_LEFT);
	            
	            // Add edit icon
	            Label editIcon = new Label("✏️");
	            editIcon.setStyle("-fx-font-size: 48px; -fx-padding: 20px;");
	            
	            // Add product info box
	            Label productInfoLabel = new Label("Editing Product ID: " + selected.getId());
	            productInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-font-style: italic;");
	            
	            VBox infoBox = new VBox(5, editIcon, productInfoLabel);
	            infoBox.setAlignment(Pos.CENTER);
	            
	            // Add instruction label
	            Label instructionLabel = new Label("Update the product details. Fields marked with * are required.");
	            instructionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d; -fx-padding: 10px 0; -fx-font-style: italic;");
	            
	            // Add to grid
	            grid.add(nameLabel, 0, 0);
	            grid.add(nameField, 1, 0);
	            grid.add(infoBox, 2, 0, 1, 4);
	            
	            grid.add(priceLabel, 0, 1);
	            grid.add(priceField, 1, 1);
	            
	            grid.add(categoryLabel, 0, 2);
	            grid.add(categoryCombo, 1, 2);
	            
	            grid.add(stockLabel, 0, 3);
	            grid.add(stockField, 1, 3);
	            
	            grid.add(discountLabel, 0, 4);
	            grid.add(discountField, 1, 4);
	            
	            grid.add(descLabel, 0, 5);
	            grid.add(descriptionArea, 1, 5);
	            
	            grid.add(imageLabel, 0, 6);
	            grid.add(imageBox, 1, 6);
	            
	            grid.add(instructionLabel, 0, 7, 3, 1);
	            
	            // Add focus effects
	            addFocusEffect(nameField, fieldStyle);
	            addFocusEffect(priceField, fieldStyle);
	            addFocusEffect(stockField, fieldStyle);
	            addFocusEffect(discountField, fieldStyle);
	            addFocusEffect(imageUrlField, fieldStyle);
	            
	            descriptionArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
	                if (newVal) {
	                    descriptionArea.setStyle(fieldStyle + "-fx-border-color: #3498db; -fx-border-width: 2px;");
	                } else {
	                    descriptionArea.setStyle(fieldStyle);
	                }
	            });
	            
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
	            
	            // Style header with different gradient for edit mode
	            dialog.getDialogPane().lookup(".header-panel").setStyle(
	                "-fx-background-color: linear-gradient(to right, #f39c12, #3498db); " +
	                "-fx-text-fill: white; " +
	                "-fx-font-size: 18px; " +
	                "-fx-font-weight: bold; " +
	                "-fx-padding: 20px;"
	            );
	            
	            // Style buttons
	            String buttonBaseStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px 30px; " +
	                                   "-fx-background-radius: 8px; -fx-border-radius: 8px; " +
	                                   "-fx-cursor: hand; -fx-min-width: 120px;";
	            
	            Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveButton);
	            saveBtn.setText("💾 Save Changes");
	            saveBtn.setStyle(buttonBaseStyle + 
	                           "-fx-background-color: #f39c12; " +
	                           "-fx-text-fill: white;");
	            
	            Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
	            cancelBtn.setStyle(buttonBaseStyle + 
	                             "-fx-background-color: #ecf0f1; " +
	                             "-fx-text-fill: #7f8c8d; " +
	                             "-fx-border-color: #bdc3c7; " +
	                             "-fx-border-width: 1px;");
	            
	            // Add button hover effects
	            saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(buttonBaseStyle + 
	                             "-fx-background-color: #d68910; " +
	                             "-fx-text-flavor: white; " +
	                             "-fx-effect: dropshadow(gaussian, rgba(243, 156, 18, 0.4), 10, 0, 0, 3);"));
	            saveBtn.setOnMouseExited(e -> saveBtn.setStyle(buttonBaseStyle + 
	                             "-fx-background-color: #f39c12; " +
	                             "-fx-text-fill: white; " +
	                             "-fx-effect: none;"));
	            
	            cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(buttonBaseStyle + 
	                             "-fx-background-color: #95a5a6; " +
	                             "-fx-text-fill: white; " +
	                             "-fx-border-color: transparent;"));
	            cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(buttonBaseStyle + 
	                             "-fx-background-color: #ecf0f1; " +
	                             "-fx-text-fill: #7f8c8d; " +
	                             "-fx-border-color: #bdc3c7; " +
	                             "-fx-border-width: 1px;"));
	            
	            // Center align labels
	            GridPane.setHalignment(nameLabel, HPos.RIGHT);
	            GridPane.setHalignment(priceLabel, HPos.RIGHT);
	            GridPane.setHalignment(categoryLabel, HPos.RIGHT);
	            GridPane.setHalignment(stockLabel, HPos.RIGHT);
	            GridPane.setHalignment(discountLabel, HPos.RIGHT);
	            GridPane.setHalignment(descLabel, HPos.RIGHT);
	            GridPane.setHalignment(imageLabel, HPos.RIGHT);
	            GridPane.setHalignment(infoBox, HPos.CENTER);
	            GridPane.setHalignment(instructionLabel, HPos.CENTER);
	            
	            // Request focus on name field
	            Platform.runLater(() -> nameField.requestFocus());
	            
	            // Convert result
	            dialog.setResultConverter(dialogButton -> {
	                if (dialogButton == saveButton) {
	                    try {
	                        String name = nameField.getText().trim();
	                        double price = Double.parseDouble(priceField.getText().trim());
	                        Category selectedCategory = categoryCombo.getValue();
	                        int stock = Integer.parseInt(stockField.getText().trim());
	                        double discount = Double.parseDouble(discountField.getText().trim());
	                        String description = descriptionArea.getText().trim();
	                        String imageUrl = imageUrlField.getText().trim();
	                        
	                        if (name.isEmpty() || selectedCategory == null) {
	                            showAlert("Error", "Please fill all required fields (*)!");
	                            return null;
	                        }
	                        
	                        if (price <= 0) {
	                            showAlert("Error", "Price must be greater than 0!");
	                            return null;
	                        }
	                        
	                        if (stock < 0) {
	                            showAlert("Error", "Stock quantity cannot be negative!");
	                            return null;
	                        }
	                        
	                        if (discount < 0 || discount > 100) {
	                            showAlert("Error", "Discount must be between 0-100%!");
	                            return null;
	                        }
	                        
	                        // Update product
	                        selected.setName(name);
	                        selected.setPrice(price);
	                        selected.setCategoryId(selectedCategory.getId());
	                        selected.setCategoryName(selectedCategory.getName());
	                        selected.setStockQuantity(stock);
	                        selected.setDiscountPercentage(discount);
	                        selected.setDescription(description);
	                        selected.setImageUrl(imageUrl.isEmpty() ? null : imageUrl);
	                        
	                        // Update in database
	                        boolean success = databaseService.updateProduct(selected);
	                        if (success) {
	                            return selected;
	                        } else {
	                            showAlert("Error", "Failed to update product in database.");
	                            return null;
	                        }
	                        
	                    } catch (NumberFormatException e) {
	                        showAlert("Error", "Please enter valid numbers for price, stock, and discount!");
	                        return null;
	                    }
	                }
	                return null;
	            });
	            
	            dialog.showAndWait().ifPresent(updatedProduct -> {
	                // Refresh table
	                productsTable.refresh();
	                showAlert("Success", "Product updated successfully!\n\n" +
	                    "Name: " + updatedProduct.getName() + "\n" +
	                    "Price: $" + String.format("%.2f", updatedProduct.getPrice()) + "\n" +
	                    "Final Price: $" + String.format("%.2f", updatedProduct.getFinalPrice()) + "\n" +
	                    "Category: " + updatedProduct.getCategoryName());
	            });
	        } catch (Exception e) {
	            showAlert("Error", "Failed to edit product: " + e.getMessage());
	        }
	    } else {
	        showAlert("Error", "Please select a product to edit");
	    }
	}
    
    @FXML
    private void applyDiscount() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                TextInputDialog dialog = new TextInputDialog(String.valueOf(selected.getDiscountPercentage()));
                dialog.setTitle("Apply Discount");
                dialog.setHeaderText("Apply discount to: " + selected.getName());
                dialog.setContentText("Enter discount percentage (0-100):");
                
                dialog.showAndWait().ifPresent(percentage -> {
                    try {
                        double discount = Double.parseDouble(percentage);
                        if (discount >= 0 && discount <= 100) {
                            selected.setDiscountPercentage(discount);
                            // Update in database
                            boolean success = databaseService.updateProduct(selected);
                            if (success) {
                                productsTable.refresh();
                                showAlert("Success", String.format(
                                    "✅ Discount Applied!\n\n" +
                                    "Product: %s\n" +
                                    "Original Price: $%.2f\n" +
                                    "Discount: %.1f%%\n" +
                                    "Final Price: $%.2f\n" +
                                    "Savings: $%.2f",
                                    selected.getName(),
                                    selected.getPrice() / (1 - discount/100),
                                    discount,
                                    selected.getFinalPrice(),
                                    selected.getPrice() / (1 - discount/100) - selected.getFinalPrice()
                                ));
                            } else {
                                showAlert("Error", "Failed to update discount in database.");
                            }
                        } else {
                            showAlert("Error", "Discount must be between 0-100%");
                        }
                    } catch (NumberFormatException e) {
                        showAlert("Error", "Please enter a valid number");
                    }
                });
            } catch (Exception e) {
                showAlert("Error", "Failed to apply discount: " + e.getMessage());
            }
        } else {
            showAlert("Error", "Please select a product to apply discount");
        }
    }
    
    @FXML
    private void increasePrice() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                TextInputDialog dialog = new TextInputDialog("10");
                dialog.setTitle("Increase Price");
                dialog.setHeaderText("Increase price for: " + selected.getName());
                dialog.setContentText("Enter increase percentage:");
                
                dialog.showAndWait().ifPresent(percentage -> {
                    try {
                        double increase = Double.parseDouble(percentage);
                        double newPrice = selected.getPrice() * (1 + increase/100);
                        selected.setPrice(newPrice);
                        // Update in database
                        boolean success = databaseService.updateProduct(selected);
                        if (success) {
                            productsTable.refresh();
                            showAlert("Success", String.format(
                                "📈 Price Increased!\n\n" +
                                "Product: %s\n" +
                                "Original Price: $%.2f\n" +
                                "Increase: %.1f%%\n" +
                                "New Price: $%.2f\n" +
                                "Increase Amount: $%.2f",
                                selected.getName(),
                                selected.getPrice() / (1 + increase/100),
                                increase,
                                selected.getPrice(),
                                selected.getPrice() - (selected.getPrice() / (1 + increase/100))
                            ));
                        } else {
                            showAlert("Error", "Failed to update price in database.");
                        }
                    } catch (NumberFormatException e) {
                        showAlert("Error", "Please enter a valid number");
                    }
                });
            } catch (Exception e) {
                showAlert("Error", "Failed to increase price: " + e.getMessage());
            }
        } else {
            showAlert("Error", "Please select a product to increase price");
        }
    }
    
    @FXML
    private void updateImage() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Product Image");
                fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
                );
                
                File selectedFile = fileChooser.showOpenDialog(productsTable.getScene().getWindow());
                if (selectedFile != null) {
                    // Store file path as image URL
                    String imageUrl = selectedFile.getAbsolutePath();
                    selected.setImageUrl(imageUrl);
                    
                    // Update in database
                    boolean success = databaseService.updateProduct(selected);
                    if (success) {
                        productsTable.refresh();
                        showAlert("Success", "Image updated for: " + selected.getName() + 
                            "\n\nImage path: " + imageUrl);
                    } else {
                        showAlert("Error", "Failed to update image in database.");
                    }
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to update image: " + e.getMessage());
            }
        } else {
            showAlert("Error", "Please select a product to update image");
        }
    }
    
    @FXML
    private void removeProduct() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Delete");
                confirm.setHeaderText("Delete Product");
                confirm.setContentText(String.format(
                    "Are you sure you want to delete this product?\n\n" +
                    "Product: %s\n" +
                    "Price: $%.2f\n" +
                    "Category: %s\n" +
                    "Stock: %d\n\n" +
                    "This action cannot be undone!",
                    selected.getName(), selected.getPrice(), 
                    selected.getCategoryName(), selected.getStockQuantity()
                ));
                
                if (confirm.showAndWait().get() == ButtonType.OK) {
                    // Delete from database
                    boolean success = databaseService.deleteProduct(selected.getId());
                    if (success) {
                        // Refresh from database
                        loadProductsFromDatabase();
                        showAlert("Success", "Product deleted successfully!");
                    } else {
                        showAlert("Error", "Failed to delete product from database.");
                    }
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to delete product: " + e.getMessage());
            }
        } else {
            showAlert("Error", "Please select a product to remove");
        }
    }
    
   
    @FXML
    private void viewOrders(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminOrdersDialog.fxml"));
            Parent root = loader.load();
            
            Stage ordersStage = new Stage();
            ordersStage.setTitle("Order Management - Admin Panel");
            ordersStage.initModality(Modality.APPLICATION_MODAL);
            
            // Set owner window
            if (event.getSource() instanceof Node) {
                Node source = (Node) event.getSource();
                ordersStage.initOwner(source.getScene().getWindow());
            }
            
            Scene scene = new Scene(root);
            ordersStage.setScene(scene);
            
            // Set minimum size
            ordersStage.setMinWidth(800);
            ordersStage.setMinHeight(600);
            
            ordersStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Could not load order management: " + e.getMessage());
            alert.show();
        }
    }
    
    
    @FXML
    private void viewMessages(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminMessagesDialog.fxml"));
            Parent root = loader.load();
            
            AdminMessagesController controller = loader.getController();
            
            Stage messagesStage = new Stage();
            messagesStage.setTitle("Customer Messages - Admin Panel");
            messagesStage.initModality(Modality.APPLICATION_MODAL);
            messagesStage.initOwner(((Node) event.getSource()).getScene().getWindow());
            
            Scene scene = new Scene(root, 900, 700);
            messagesStage.setScene(scene);
            
            messagesStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Could not load messages dialog: " + e.getMessage());
            alert.show();
        }
    }
        
    @FXML
private void viewStatistics() {
    try {
        // Calculate statistics
        int totalProducts = productList.size();
        double totalValue = 0;
        double totalFinalValue = 0;
        int outOfStock = 0;
        int lowStock = 0;
        int inStock = 0;
        int discountedProducts = 0;
        double totalDiscount = 0;
        Product mostExpensive = null;
        Product cheapest = null;
        Product highestDiscountProduct = null;
        double highestPrice = 0;
        double lowestPrice = Double.MAX_VALUE;
        double highestDiscount = 0;
        int totalStockQuantity = 0;
        
        for (Product p : productList) {
            double price = p.getPrice();
            double finalPrice = p.getFinalPrice();
            totalValue += price;
            totalFinalValue += finalPrice;
            totalDiscount += (price - finalPrice);
            totalStockQuantity += p.getStockQuantity();
            
            // Stock status
            if (p.getStockQuantity() == 0) {
                outOfStock++;
            } else if (p.getStockQuantity() < 10) {
                lowStock++;
            } else {
                inStock++;
            }
            
            // Discount info
            if (p.getDiscountPercentage() > 0) {
                discountedProducts++;
                if (p.getDiscountPercentage() > highestDiscount) {
                    highestDiscount = p.getDiscountPercentage();
                    highestDiscountProduct = p;
                }
            }
            
            // Price extremes
            if (price > highestPrice) {
                highestPrice = price;
                mostExpensive = p;
            }
            if (price < lowestPrice) {
                lowestPrice = price;
                cheapest = p;
            }
        }
        
        // Calculate averages
        double avgPrice = totalProducts > 0 ? totalValue / totalProducts : 0;
        double avgFinalPrice = totalProducts > 0 ? totalFinalValue / totalProducts : 0;
        double avgDiscount = discountedProducts > 0 ? totalDiscount / (totalValue + totalDiscount) * 100 : 0;
        
        // Create a custom dialog for beautiful statistics
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("📊 Store Analytics Dashboard");
        dialog.initStyle(StageStyle.UTILITY);
        
        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom, #f8f9fa, #e9ecef); -fx-padding: 25;");
        mainContainer.setPrefSize(900, 500);
        
        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER);
        
        Label title = new Label("📊 STORE ANALYTICS DASHBOARD");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; " +
                      "-fx-text-fill: linear-gradient(to right, #2c3e50, #4a6491); " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0.3, 0, 2);");
        header.getChildren().add(title);
        
        // Create statistics cards in a grid
        GridPane cardsGrid = new GridPane();
        cardsGrid.setHgap(20);
        cardsGrid.setVgap(20);
        cardsGrid.setPadding(new Insets(20));
        
        // Row 1: Overview Cards
        VBox totalProductsCard = createStatCard("📦", "TOTAL PRODUCTS", 
            String.valueOf(totalProducts), "#3498db", 
            String.format("%d categories", getCategoryCount()));
        
        VBox inventoryValueCard = createStatCard("💰", "INVENTORY VALUE", 
            String.format("$%.2f", totalValue), "#2ecc71", 
            String.format("Avg: $%.2f", avgPrice));
        
        VBox totalStockCard = createStatCard("📊", "TOTAL STOCK", 
            String.valueOf(totalStockQuantity), "#9b59b6", 
            String.format("Units available"));
        
        VBox averagePriceCard = createStatCard("💎", "AVERAGE PRICE", 
            String.format("$%.2f", avgFinalPrice), "#e74c3c", 
            String.format("Original: $%.2f", avgPrice));
        
        cardsGrid.add(totalProductsCard, 0, 0);
        cardsGrid.add(inventoryValueCard, 1, 0);
        cardsGrid.add(totalStockCard, 2, 0);
        cardsGrid.add(averagePriceCard, 3, 0);
        
        // Row 2: Discount & Savings Cards
        VBox totalDiscountCard = createStatCard("🏷️", "TOTAL DISCOUNT", 
            String.format("$%.2f", totalDiscount), "#f39c12", 
            String.format("%.1f%% average", avgDiscount));
        
        VBox discountedProductsCard = createStatCard("📉", "DISCOUNTED ITEMS", 
            String.valueOf(discountedProducts), "#1abc9c", 
            String.format("%.1f%% of products", (totalProducts > 0 ? (discountedProducts * 100.0 / totalProducts) : 0)));
        
        VBox savingsCard = createStatCard("💸", "CUSTOMER SAVINGS", 
            String.format("$%.2f", totalDiscount), "#e91e63", 
            "Total amount saved");
        
        VBox discountRateCard = createStatCard("🎯", "DISCOUNT RATE", 
            String.format("%.1f%%", avgDiscount), "#673ab7", 
            "Average discount percentage");
        
        cardsGrid.add(totalDiscountCard, 0, 1);
        cardsGrid.add(discountedProductsCard, 1, 1);
        cardsGrid.add(savingsCard, 2, 1);
        cardsGrid.add(discountRateCard, 3, 1);
        
        // Stock Status Section
        VBox stockSection = createSection("📈 STOCK ANALYSIS", "#3498db");
        
        HBox stockCardsContainer = new HBox(20);
        stockCardsContainer.setAlignment(Pos.CENTER);
        stockCardsContainer.setPadding(new Insets(20, 0, 20, 0));
        
        VBox outOfStockCard = createStockStatusCard("🔴", "OUT OF STOCK", 
            String.valueOf(outOfStock), "#e74c3c",
            String.format("%.1f%% of products", (totalProducts > 0 ? (outOfStock * 100.0 / totalProducts) : 0)));
        
        VBox lowStockCard = createStockStatusCard("🟡", "LOW STOCK", 
            String.valueOf(lowStock), "#f39c12",
            "Less than 10 units");
        
        VBox inStockCard = createStockStatusCard("🟢", "IN STOCK", 
            String.valueOf(inStock), "#2ecc71",
            "10+ units available");
        
        stockCardsContainer.getChildren().addAll(outOfStockCard, lowStockCard, inStockCard);
        stockSection.getChildren().add(stockCardsContainer);
        
        // Top Products Section
        VBox topProductsSection = createSection("🏆 TOP PRODUCTS", "#9b59b6");
        
        GridPane topProductsGrid = new GridPane();
        topProductsGrid.setHgap(20);
        topProductsGrid.setVgap(20);
        topProductsGrid.setPadding(new Insets(20, 0, 20, 0));
        
        // Most Expensive Product
        VBox mostExpensiveCard = createProductCard("💎", "MOST EXPENSIVE", 
            mostExpensive != null ? mostExpensive.getName() : "N/A",
            mostExpensive != null ? String.format("$%.2f", mostExpensive.getPrice()) : "$0.00",
            mostExpensive != null ? String.format("Stock: %d", mostExpensive.getStockQuantity()) : "N/A",
            "#ff5722");
        
        // Cheapest Product
        VBox cheapestCard = createProductCard("💰", "BEST VALUE", 
            cheapest != null ? cheapest.getName() : "N/A",
            cheapest != null ? String.format("$%.2f", cheapest.getPrice()) : "$0.00",
            cheapest != null ? String.format("Stock: %d", cheapest.getStockQuantity()) : "N/A",
            "#4caf50");
        
        // Highest Discount Product
        VBox highestDiscountCard = createProductCard("🎯", "BIGGEST DISCOUNT", 
            highestDiscountProduct != null ? highestDiscountProduct.getName() : "N/A",
            highestDiscountProduct != null ? 
                String.format("%.1f%% OFF", highestDiscountProduct.getDiscountPercentage()) : "0%",
            highestDiscountProduct != null ? 
                String.format("$%.2f → $%.2f", 
                    highestDiscountProduct.getPrice(),
                    highestDiscountProduct.getFinalPrice()) : "N/A",
            "#9c27b0");
        
        // Most Stock Product
        Product mostStockProduct = getMostStockProduct();
        VBox mostStockCard = createProductCard("📦", "HIGHEST STOCK", 
            mostStockProduct != null ? mostStockProduct.getName() : "N/A",
            mostStockProduct != null ? String.valueOf(mostStockProduct.getStockQuantity()) : "0",
            mostStockProduct != null ? 
                String.format("$%.2f each", mostStockProduct.getPrice()) : "N/A",
            "#2196f3");
        
        topProductsGrid.add(mostExpensiveCard, 0, 0);
        topProductsGrid.add(cheapestCard, 1, 0);
        topProductsGrid.add(highestDiscountCard, 0, 1);
        topProductsGrid.add(mostStockCard, 1, 1);
        
        topProductsSection.getChildren().add(topProductsGrid);
        
        // Price Range Section
        VBox priceRangeSection = createSection("📊 PRICE DISTRIBUTION", "#e74c3c");
        
        HBox priceRangeContainer = new HBox(15);
        priceRangeContainer.setAlignment(Pos.CENTER);
        priceRangeContainer.setPadding(new Insets(20, 0, 20, 0));
        
        // Calculate price ranges
        int[] priceRanges = new int[5];
        for (Product p : productList) {
            double price = p.getPrice();
            if (price <= 50) priceRanges[0]++;
            else if (price <= 100) priceRanges[1]++;
            else if (price <= 200) priceRanges[2]++;
            else if (price <= 500) priceRanges[3]++;
            else priceRanges[4]++;
        }
        
        String[] rangeLabels = {"$0-50", "$51-100", "$101-200", "$201-500", "$500+"};
        String[] rangeColors = {"#2ecc71", "#3498db", "#9b59b6", "#f39c12", "#e74c3c"};
        
        for (int i = 0; i < 5; i++) {
            VBox priceRangeCard = createPriceRangeCard(rangeLabels[i], 
                String.valueOf(priceRanges[i]), 
                String.format("%.1f%%", (totalProducts > 0 ? (priceRanges[i] * 100.0 / totalProducts) : 0)),
                rangeColors[i]);
            priceRangeContainer.getChildren().add(priceRangeCard);
        }
        
        priceRangeSection.getChildren().add(priceRangeContainer);
        
        // Summary Section
        VBox summarySection = createSection("📋 QUICK SUMMARY", "#1abc9c");
        
        VBox summaryBox = new VBox(10);
        summaryBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                          "-fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.5, 0, 2);");
        summaryBox.setPadding(new Insets(20));
        
        Label summaryLabel = new Label("📌 Store Performance Summary");
        summaryLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        String summaryText = String.format("""
            • Total Products: %d items worth $%.2f
            • Stock Status: %d in stock, %d low stock, %d out of stock
            • Discounts: %d products discounted (%.1f%%)
            • Customer Savings: $%.2f total discount value
            • Average Price: $%.2f (Discounted: $%.2f)
            """, 
            totalProducts, totalValue,
            inStock, lowStock, outOfStock,
            discountedProducts, (totalProducts > 0 ? (discountedProducts * 100.0 / totalProducts) : 0),
            totalDiscount,
            avgPrice, avgFinalPrice
        );
        
        Label summaryContent = new Label(summaryText);
        summaryContent.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e; -fx-line-spacing: 5px;");
        summaryContent.setWrapText(true);
        
        summaryBox.getChildren().addAll(summaryLabel, new Separator(), summaryContent);
        summarySection.getChildren().add(summaryBox);
        
        // Add all sections to main container
        mainContainer.getChildren().addAll(
            header, cardsGrid, stockSection, 
            topProductsSection, priceRangeSection, summarySection
        );
        
        // Create scroll pane for content
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        // Set dialog content
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Style the close button
        Button closeButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setText("Close Dashboard");
        closeButton.setStyle("-fx-background-color: linear-gradient(to right, #e74c3c, #c0392b); " +
                            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                            "-fx-padding: 8 20; -fx-border-radius: 20;");
        
        // Add footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10));
        
        Label timestamp = new Label("📅 Report generated: " + 
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timestamp.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        
        footer.getChildren().add(timestamp);
        mainContainer.getChildren().add(footer);
        
        // Show dialog
        dialog.showAndWait();
        
    } catch (Exception e) {
        showAlert("Error", "Failed to generate statistics: " + e.getMessage());
        e.printStackTrace();
    }
}

// Helper method to create beautiful stat cards
private VBox createStatCard(String emoji, String title, String value, String color, String subtitle) {
    VBox card = new VBox(12);
    card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                 "-fx-padding: 25; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0.5, 0, 4); " +
                 "-fx-border-radius: 15; -fx-border-color: " + color + "20; -fx-border-width: 1;");
    card.setPrefSize(180, 140);
    card.setAlignment(Pos.CENTER);
    
    HBox emojiBox = new HBox();
    emojiBox.setAlignment(Pos.CENTER);
    
    Label emojiLabel = new Label(emoji);
    emojiLabel.setStyle("-fx-font-size: 28px; -fx-padding: 0 0 5 0;");
    
    Label titleLabel = new Label(title);
    titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d; -fx-font-weight: bold; " +
                       "-fx-letter-spacing: 1px; -fx-text-transform: uppercase;");
    
    Label valueLabel = new Label(value);
    valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " +
                       "-fx-text-fill: " + color + "; -fx-effect: dropshadow(gaussian, " + color + "40, 5, 0.3, 0, 1);");
    
    Label subtitleLabel = new Label(subtitle);
    subtitleLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #95a5a6; -fx-wrap-text: true;");
    subtitleLabel.setMaxWidth(150);
    subtitleLabel.setAlignment(Pos.CENTER);
    
    card.getChildren().addAll(emojiLabel, titleLabel, valueLabel, subtitleLabel);
    return card;
}

// Helper method to create stock status cards
private VBox createStockStatusCard(String emoji, String status, String count, String color, String subtitle) {
    VBox card = new VBox(15);
    card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                 "-fx-padding: 25; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.5, 0, 3);");
    card.setPrefSize(180, 150);
    card.setAlignment(Pos.CENTER);
    
    Label emojiLabel = new Label(emoji);
    emojiLabel.setStyle("-fx-font-size: 36px; -fx-padding: 0 0 5 0;");
    
    Label statusLabel = new Label(status);
    statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    
    Label countLabel = new Label(count);
    countLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    
    Label subtitleLabel = new Label(subtitle);
    subtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d; -fx-wrap-text: true;");
    subtitleLabel.setMaxWidth(150);
    subtitleLabel.setAlignment(Pos.CENTER);
    
    card.getChildren().addAll(emojiLabel, statusLabel, countLabel, subtitleLabel);
    return card;
}

// Helper method to create product cards
private VBox createProductCard(String emoji, String title, String productName, String value, String details, String color) {
    VBox card = new VBox(12);
    card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                 "-fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.5, 0, 3);");
    card.setPrefWidth(250);
    card.setPrefHeight(180);
    card.setAlignment(Pos.CENTER);
    
    HBox headerBox = new HBox(10);
    headerBox.setAlignment(Pos.CENTER);
    
    Label emojiLabel = new Label(emoji);
    emojiLabel.setStyle("-fx-font-size: 24px;");
    
    Label titleLabel = new Label(title);
    titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    
    headerBox.getChildren().addAll(emojiLabel, titleLabel);
    
    Label productLabel = new Label(productName);
    productLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; " +
                         "-fx-wrap-text: true; -fx-text-alignment: center;");
    productLabel.setMaxWidth(210);
    productLabel.setAlignment(Pos.CENTER);
    
    Label valueLabel = new Label(value);
    valueLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    
    Label detailsLabel = new Label(details);
    detailsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d; -fx-wrap-text: true;");
    detailsLabel.setMaxWidth(210);
    detailsLabel.setAlignment(Pos.CENTER);
    
    card.getChildren().addAll(headerBox, productLabel, valueLabel, detailsLabel);
    return card;
}

// Helper method to create price range cards
private VBox createPriceRangeCard(String range, String count, String percentage, String color) {
    VBox card = new VBox(10);
    card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                 "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0.3, 0, 2);");
    card.setPrefWidth(120);
    card.setAlignment(Pos.CENTER);
    
    Label rangeLabel = new Label(range);
    rangeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    
    Label countLabel = new Label(count + " products");
    countLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
    
    Label percentageLabel = new Label(percentage);
    percentageLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
    
    card.getChildren().addAll(rangeLabel, countLabel, percentageLabel);
    return card;
}

// Helper method to create section headers
private VBox createSection(String title, String color) {
    VBox section = new VBox(15);
    section.setPadding(new Insets(20, 0, 10, 0));
    
    HBox titleBox = new HBox(10);
    titleBox.setAlignment(Pos.CENTER_LEFT);
    
    Label titleLabel = new Label(title);
    titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; " +
                       "-fx-text-fill: " + color + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0.3, 0, 1);");
    
    titleBox.getChildren().add(titleLabel);
    
    Separator separator = new Separator();
    separator.setStyle("-fx-background-color: linear-gradient(to right, " + color + ", transparent); " +
                      "-fx-padding: 2px;");
    
    section.getChildren().addAll(titleBox, separator);
    return section;
}

// Helper methods for calculations
private int getCategoryCount() {
    Set<String> categories = new HashSet<>();
    for (Product p : productList) {
        categories.add(p.getCategoryName());
    }
    return categories.size();
}

private Product getMostStockProduct() {
    Product mostStock = null;
    int maxStock = 0;
    for (Product p : productList) {
        if (p.getStockQuantity() > maxStock) {
            maxStock = p.getStockQuantity();
            mostStock = p;
        }
    }
    return mostStock;
}
    
    private String getMostExpensiveProduct() {
        if (productList.isEmpty()) return "N/A";
        Product mostExpensive = productList.get(0);
        for (Product p : productList) {
            if (p.getFinalPrice() > mostExpensive.getFinalPrice()) {
                mostExpensive = p;
            }
        }
        return mostExpensive.getName() + " ($" + String.format("%.2f", mostExpensive.getFinalPrice()) + ")";
    }
    
    private String getCheapestProduct() {
        if (productList.isEmpty()) return "N/A";
        Product cheapest = productList.get(0);
        for (Product p : productList) {
            if (p.getFinalPrice() < cheapest.getFinalPrice()) {
                cheapest = p;
            }
        }
        return cheapest.getName() + " ($" + String.format("%.2f", cheapest.getFinalPrice()) + ")";
    }
    
    private String getHighestDiscountProduct() {
        if (productList.isEmpty()) return "N/A";
        Product highestDiscount = null;
        double maxDiscount = 0;
        for (Product p : productList) {
            if (p.getDiscountPercentage() > maxDiscount) {
                maxDiscount = p.getDiscountPercentage();
                highestDiscount = p;
            }
        }
        return highestDiscount != null ? 
            highestDiscount.getName() + " (" + String.format("%.1f%%", maxDiscount) + ")" : "None";
    }
    
    @FXML

    public void addAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add-admin.fxml"));
            Parent root = loader.load();
            
            AddAdminController controller = loader.getController();
            controller.setDatabaseService(databaseService);
            
            Stage stage = new Stage();
            stage.setTitle("Add New Administrator");
            stage.setScene(new Scene(root, 1100, 650));
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load add admin page: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Close database connection
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
            newStage.setTitle("E-Commerce Store");
            newStage.setMaximized(true);
            // Close current and show new
            currentStage.close();
            newStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Logout failed: " + e.getMessage());
        }
    }
    
    @FXML
    void viewStore(ActionEvent event) {
    	try {
    		
    		// Load customer store
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerStore.fxml"));
            Parent root = loader.load();
            root.getStylesheets().add(getClass().getResource("/styles/store.css").toExternalForm());

            // Get controller and pass user data
            CustomerStoreController controller = loader.getController();
            
            // Get current stage
            Node source = (Node) event.getSource();
            Stage currentStage = (Stage) source.getScene().getWindow();
            
            // Create new stage for store
            Stage storeStage = new Stage();
            storeStage.setScene(new Scene(root, 1200, 800));
            storeStage.setTitle("Welcome to E-Commerce Store");
            storeStage.setMaximized(true);
            
            // Close login window and show store
            currentStage.close();
            storeStage.show();
    	}catch(Exception e) {
    		showAlert("Error", e.getMessage());
    	}

    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}