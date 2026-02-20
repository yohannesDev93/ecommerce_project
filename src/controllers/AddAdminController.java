package controllers;

import models.User;
import services.DatabaseService;
import utils.PasswordValidator;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddAdminController {
    
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField searchField;
    @FXML private Label messageLabel;
    @FXML private Label statusLabel;
    @FXML private Label totalAdminsLabel;
    @FXML private Label recentAdminsLabel;
    @FXML private Label primaryAdminLabel;
    @FXML private Label noAdminsLabel;
    @FXML private VBox adminListContainer;
    
    private DatabaseService databaseService;
    private ObservableList<User> allAdmins;
    private FilteredList<User> filteredAdmins;
    private ExecutorService executorService;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private final String PRIMARY_ADMIN_USERNAME = "admin";
    
    @FXML
    public void initialize() {
        // If databaseService wasn't set via setter, create a new one
        if (databaseService == null) {
            databaseService = new DatabaseService();
        }
        executorService = Executors.newCachedThreadPool();
        allAdmins = FXCollections.observableArrayList();
        filteredAdmins = new FilteredList<>(allAdmins);
        
        // Initialize UI
        messageLabel.setText("");
        messageLabel.setVisible(false);
        statusLabel.setText("Ready");
        noAdminsLabel.setVisible(false);
        primaryAdminLabel.setText("0");
        
        // Setup search field listener
        setupSearchListener();
        
        // Load admins asynchronously
        loadAdminsAsync();
        
        // Add real-time validation
        setupValidation();
    }
    
    // Add this method back - it's being called from AdminDashboardController
    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }
	    
	private void setupValidation() {
	    // Password strength indicator
	    passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
	        if (newVal.isEmpty()) {
	            passwordField.setStyle("");
	            statusLabel.setText("Enter password");
	        } else {
	            // Simple validation feedback
	            String style = "-fx-border-color: ";
	            String message = "Password: ";
	            
	            if (newVal.length() < 8) {
	                style += "#e74c3c; -fx-border-width: 2;";
	                message += "Too short (min 8)";
	            } else if (!newVal.matches(".*[A-Z].*")) {
	                style += "#e74c3c; -fx-border-width: 2;";
	                message += "Need uppercase";
	            } else if (!newVal.matches(".*[a-z].*")) {
	                style += "#e74c3c; -fx-border-width: 2;";
	                message += "Need lowercase";
	            } else if (!newVal.matches(".*[!@#$%^&*].*")) {
	                style += "#f39c12; -fx-border-width: 2;";
	                message += "Add special char";
	            } else {
	                style += "#2ecc71; -fx-border-width: 2;";
	                message += "✓ Good password";
	            }
	            
	            passwordField.setStyle(style);
	            statusLabel.setText(message);
	            
	            // Check confirmation
	            if (!confirmPasswordField.getText().isEmpty()) {
	                if (!newVal.equals(confirmPasswordField.getText())) {
	                    confirmPasswordField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
	                } else {
	                    confirmPasswordField.setStyle("-fx-border-color: #2ecc71; -fx-border-width: 2;");
	                }
	            }
	        }
	    });
	    
	    // Confirm password listener
	    confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
	        if (newVal.isEmpty()) {
	            confirmPasswordField.setStyle("");
	        } else if (!newVal.equals(passwordField.getText())) {
	            confirmPasswordField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
	            statusLabel.setText("Passwords don't match!");
	        } else {
	            confirmPasswordField.setStyle("-fx-border-color: #2ecc71; -fx-border-width: 2;");
	            statusLabel.setText("✓ Passwords match!");
	        }
	    });
	}
    
    private void setupSearchListener() {
        PauseTransition pause = new PauseTransition(Duration.millis(300));
        pause.setOnFinished(event -> filterAdmins(searchField.getText()));
        
        searchField.textProperty().addListener((obs, old, newVal) -> {
            pause.playFromStart();
        });
    }
    
    private void loadAdminsAsync() {
        statusLabel.setText("Loading admins...");
        
        executorService.execute(() -> {
            try {
                List<User> admins = databaseService.getAllUsers().stream()
                    .filter(user -> "ADMIN".equals(user.getRole()))
                    .toList();
                
                Platform.runLater(() -> {
                    allAdmins.setAll(admins);
                    updateAdminList();
                    updateStats();
                    statusLabel.setText("Loaded " + admins.size() + " admins");
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showMessage("Error loading admins: " + e.getMessage(), "error");
                    statusLabel.setText("Error loading admins");
                });
            }
        });
    }
    
    private void updateAdminList() {
        adminListContainer.getChildren().clear();
        
        if (filteredAdmins.isEmpty()) {
            noAdminsLabel.setVisible(true);
            adminListContainer.getChildren().add(noAdminsLabel);
            return;
        }
        
        noAdminsLabel.setVisible(false);
        
        for (User admin : filteredAdmins) {
            HBox adminCard = createAdminCard(admin);
            adminListContainer.getChildren().add(adminCard);
        }
    }
    
    private HBox createAdminCard(User admin) {
        HBox card = new HBox(15);
        card.setPrefHeight(100);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                     "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 10; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 1);");
        
        // Check if this is primary admin
        boolean isPrimaryAdmin = PRIMARY_ADMIN_USERNAME.equals(admin.getUsername());
        
        // Avatar Circle with gradient
        Circle avatarCircle = new Circle(30);
        if (isPrimaryAdmin) {
            avatarCircle.setFill(LinearGradient.valueOf("linear-gradient(to bottom right, #e74c3c, #c0392b)"));
        } else {
            avatarCircle.setFill(LinearGradient.valueOf("linear-gradient(to bottom right, #3498db, #2980b9)"));
        }
        
        // User Initial
        String initial = admin.getFullName().substring(0, 1).toUpperCase();
        Label initialLabel = new Label(initial);
        initialLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        initialLabel.setTextFill(Color.WHITE);
        
        StackPane avatarContainer = new StackPane(avatarCircle, initialLabel);
        avatarContainer.setAlignment(Pos.CENTER);
        
        // Admin Info
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        
        HBox nameBox = new HBox(10);
        Label nameLabel = new Label(admin.getFullName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLabel.setTextFill(Color.web("#2c3e50"));
        
        // Role badge
        Label roleBadge = new Label();
        if (isPrimaryAdmin) {
            roleBadge.setText("PRIMARY ADMIN");
            roleBadge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                              "-fx-font-size: 10; -fx-font-weight: bold; " +
                              "-fx-padding: 3 8; -fx-background-radius: 10;");
        } else {
            roleBadge.setText("ADMIN");
            roleBadge.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                              "-fx-font-size: 10; -fx-font-weight: bold; " +
                              "-fx-padding: 3 8; -fx-background-radius: 10;");
        }
        
        nameBox.getChildren().addAll(nameLabel, roleBadge);
        
        // Contact info
        HBox contactBox = new HBox(15);
        contactBox.setAlignment(Pos.CENTER_LEFT);
        
        Label emailLabel = new Label("📧 " + admin.getEmail());
        emailLabel.setFont(Font.font("System", 12));
        emailLabel.setTextFill(Color.web("#7f8c8d"));
        
        Label usernameLabel = new Label("👤 " + admin.getUsername());
        usernameLabel.setFont(Font.font("System", 12));
        usernameLabel.setTextFill(Color.web("#7f8c8d"));
        
        contactBox.getChildren().addAll(emailLabel, usernameLabel);
        
        // Date and ID
        HBox metaBox = new HBox(10);
        metaBox.setAlignment(Pos.CENTER_LEFT);
        
        Label dateLabel = new Label();
        if (admin.getCreatedAt() != null) {
            dateLabel.setText("🗓️ Joined: " + admin.getCreatedAt().format(dateFormatter));
        }
        dateLabel.setFont(Font.font("System", 11));
        dateLabel.setTextFill(Color.web("#95a5a6"));
        
        Label idLabel = new Label("🆔 ID: " + admin.getId());
        idLabel.setFont(Font.font("System", 11));
        idLabel.setTextFill(Color.web("#95a5a6"));
        
        metaBox.getChildren().addAll(dateLabel, idLabel);
        
        infoBox.getChildren().addAll(nameBox, contactBox, metaBox);
        
        // Action Buttons (Right side)
        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        
        // View Details Button
        Button viewBtn = new Button("👁️ View");
        viewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-background-radius: 8; " +
                        "-fx-padding: 8 15; -fx-cursor: hand;");
        viewBtn.setOnAction(e -> showAdminDetails(admin));
        
        // Remove Admin Button (disabled for primary admin)
        Button removeBtn = new Button("🗑️ Remove");
        removeBtn.setStyle("-fx-background-color: " + (isPrimaryAdmin ? "#95a5a6" : "#e74c3c") + "; " +
                          "-fx-text-fill: white; -fx-font-weight: bold; " +
                          "-fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
        removeBtn.setDisable(isPrimaryAdmin);
        removeBtn.setOnAction(e -> removeAdmin(admin));
        
        actionBox.getChildren().addAll(viewBtn, removeBtn);
        
        // Layout
        card.getChildren().addAll(avatarContainer, infoBox, new Region(), actionBox);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        HBox.setHgrow(new Region(), Priority.ALWAYS);
        
        return card;
    }
    
    private void filterAdmins(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredAdmins.setPredicate(null);
        } else {
            String lowerCaseFilter = searchText.toLowerCase();
            filteredAdmins.setPredicate(admin -> {
                return admin.getFullName().toLowerCase().contains(lowerCaseFilter) ||
                       admin.getEmail().toLowerCase().contains(lowerCaseFilter) ||
                       admin.getUsername().toLowerCase().contains(lowerCaseFilter);
            });
        }
        updateAdminList();
    }
    
    private void updateStats() {
        int total = allAdmins.size();
        totalAdminsLabel.setText(String.valueOf(total));
        
        long recent = allAdmins.stream()
            .filter(admin -> admin.getCreatedAt() != null &&
                    admin.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
            .count();
        recentAdminsLabel.setText(String.valueOf(recent));
        
        long primaryAdminCount = allAdmins.stream()
            .filter(admin -> PRIMARY_ADMIN_USERNAME.equals(admin.getUsername()))
            .count();
        primaryAdminLabel.setText(String.valueOf(primaryAdminCount));
    }
    
    @FXML
   
private void handleAddAdmin() {
    // Clear previous messages
    messageLabel.setText("");
    messageLabel.setVisible(false);
    
    // Validate inputs
    if (!validateInputs()) {
        return;
    }
    
    String fullName = fullNameField.getText().trim();
    String email = emailField.getText().trim();
    String username = usernameField.getText().trim();
    String password = passwordField.getText();
    
    statusLabel.setText("Processing...");
    
    executorService.execute(() -> {
        try {
            // FIRST: Check if username already exists in database
            User existingUserByUsername = databaseService.getUserByUsername(username);
            
            Platform.runLater(() -> {
                if (existingUserByUsername != null) {
                    // Username exists - show promotion dialog
                    showUsernameExistsDialog(existingUserByUsername);
                    return;
                }
                
                // If username doesn't exist, check if email exists
                User existingUserByEmail = databaseService.getUserByEmail(email);
                
                if (existingUserByEmail != null) {
                    // User exists with this email, check if they're already admin
                    if ("ADMIN".equals(existingUserByEmail.getRole())) {
                        showMessage("User '" + existingUserByEmail.getFullName() + "' is already an administrator!", "warning");
                        statusLabel.setText("User already admin");
                    } else {
                        // User exists with email but not admin - show promotion dialog
                        showPromoteConfirmationDialog(existingUserByEmail);
                    }
                } else {
                    // Neither username nor email exists - register new admin
                    registerNewAdmin(fullName, email, username, password);
                }
            });
            
        } catch (Exception e) {
            Platform.runLater(() -> {
                showMessage("Error: " + e.getMessage(), "error");
                statusLabel.setText("Error occurred");
            });
        }
    });
}

private void showUsernameExistsDialog(User existingUser) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Username Already Exists");
    alert.setHeaderText("Username '" + existingUser.getUsername() + "' is already taken!");
    
    String content = String.format(
        "User with username '%s' already exists.\n\n" +
        "👤 Existing User: %s\n" +
        "📧 Email: %s\n" +
        "🎯 Current Role: %s\n\n" +
        "Do you want to promote this existing user to Administrator?\n\n" +
        "Note: If you choose 'Cancel', you'll need to use a different username.",
        existingUser.getUsername(),
        existingUser.getFullName(),
        existingUser.getEmail(),
        existingUser.getRole()
    );
    
    alert.setContentText(content);
    
    // Custom buttons
    ButtonType promoteButton = new ButtonType("Promote to Admin", ButtonBar.ButtonData.OK_DONE);
    ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    alert.getButtonTypes().setAll(promoteButton, cancelButton);
    
    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == promoteButton) {
        // Check if user is already admin
        if ("ADMIN".equals(existingUser.getRole())) {
            showMessage("User '" + existingUser.getFullName() + "' is already an administrator!", "warning");
            statusLabel.setText("User already admin");
        } else {
            promoteToAdmin(existingUser.getEmail());
        }
    } else {
        statusLabel.setText("Please choose a different username");
        usernameField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        usernameField.requestFocus();
    }
}
    
    private void showPromoteConfirmationDialog(User existingUser) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Promote Existing User");
        alert.setHeaderText("Promote to Administrator?");
        
        String content = String.format(
            "User '%s' already exists as a %s.\n\n" +
            "📧 Email: %s\n" +
            "👤 Username: %s\n\n" +
            "Would you like to promote this user to Administrator?\n" +
            "They will gain full administrator privileges.",
            existingUser.getFullName(),
            existingUser.getRole(),
            existingUser.getEmail(),
            existingUser.getUsername()
        );
        
        alert.setContentText(content);
        
        // Custom buttons
        ButtonType promoteButton = new ButtonType("Promote to Admin", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(promoteButton, cancelButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == promoteButton) {
            promoteToAdmin(existingUser.getEmail());
        } else {
            statusLabel.setText("Operation cancelled");
        }
    }
    
    private void promoteToAdmin(String email) {
        executorService.execute(() -> {
            try {
                boolean success = databaseService.updateUserRole(email, "ADMIN");
                
                Platform.runLater(() -> {
                    if (success) {
                        showMessage("✓ User promoted to administrator successfully!", "success");
                        statusLabel.setText("User promoted successfully");
                        clearFields();
                        loadAdminsAsync();
                    } else {
                        showMessage("Failed to promote user to administrator.", "error");
                        statusLabel.setText("Promotion failed");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showMessage("Error promoting user: " + e.getMessage(), "error");
                    statusLabel.setText("Promotion error");
                });
            }
        });
    }
    
    private void registerNewAdmin(String fullName, String email, String username, String password) {
        executorService.execute(() -> {
            try {
                // Register new user as ADMIN
                boolean success = databaseService.registerUser(username, email, password, fullName, "ADMIN");
                
                Platform.runLater(() -> {
                    if (success) {
                        showMessage("✓ New administrator added successfully!", "success");
                        statusLabel.setText("Admin added successfully");
                        clearFields();
                        loadAdminsAsync();
                    } else {
                        showMessage("Failed to add new administrator.", "error");
                        statusLabel.setText("Addition failed");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showMessage("Error adding admin: " + e.getMessage(), "error");
                    statusLabel.setText("Addition error");
                });
            }
        });
    }
    
	private boolean validateInputs() {
	    // ... other validations ...
	    
	    // Check password using PasswordValidator
	    String password = passwordField.getText();
	    PasswordValidator.ValidationResult result = PasswordValidator.validatePassword(password);
	    
	    if (!result.isSuccess()) {
	        showMessage(result.getErrorMessage(), "error");
	        return false;
	    }
	    
	    // Check password match
	    if (!passwordField.getText().equals(confirmPasswordField.getText())) {
	        showMessage("Passwords do not match!", "error");
	        return false;
	    }
	    
	    return true;
	}
    
    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        
        switch (type) {
            case "error":
                messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14; " +
                                      "-fx-background-color: #ffeaea; -fx-background-radius: 10; " +
                                      "-fx-padding: 12; -fx-border-color: #e74c3c; " +
                                      "-fx-border-radius: 10; -fx-border-width: 1;");
                break;
            case "success":
                messageLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 14; " +
                                      "-fx-background-color: #eafaf1; -fx-background-radius: 10; " +
                                      "-fx-padding: 12; -fx-border-color: #27ae60; " +
                                      "-fx-border-radius: 10; -fx-border-width: 1;");
                break;
            case "warning":
                messageLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 14; " +
                                      "-fx-background-color: #fff3cd; -fx-background-radius: 10; " +
                                      "-fx-padding: 12; -fx-border-color: #f39c12; " +
                                      "-fx-border-radius: 10; -fx-border-width: 1;");
                break;
        }
    }
    
    private void removeAdmin(User admin) {
        // Double-check if trying to remove primary admin
        if (PRIMARY_ADMIN_USERNAME.equals(admin.getUsername())) {
            showMessage("Cannot remove primary admin account!", "error");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Administrator");
        alert.setHeaderText("Remove " + admin.getFullName() + " as administrator?");
        alert.setContentText("This will change their role to CUSTOMER.\n\n" +
                           "They will lose administrator privileges but can still login as a regular customer.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                executeAdminRemoval(admin);
            }
        });
    }
    
    private void executeAdminRemoval(User admin) {
        statusLabel.setText("Removing admin privileges...");
        
        executorService.execute(() -> {
            try {
                // Demote admin to customer
                boolean success = databaseService.updateUserRole(admin.getEmail(), "CUSTOMER");
                
                Platform.runLater(() -> {
                    if (success) {
                        showMessage("✓ " + admin.getFullName() + " has been removed as administrator.", "success");
                        statusLabel.setText("Admin removed successfully");
                        
                        // Remove from local list
                        allAdmins.remove(admin);
                        updateAdminList();
                        updateStats();
                    } else {
                        showMessage("Failed to remove admin privileges.", "error");
                        statusLabel.setText("Removal failed");
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showMessage("Error removing admin: " + e.getMessage(), "error");
                    statusLabel.setText("Error occurred");
                });
            }
        });
    }
    
    private void showAdminDetails(User admin) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Admin Details");
        alert.setHeaderText("👑 " + admin.getFullName());
        
        String roleText = PRIMARY_ADMIN_USERNAME.equals(admin.getUsername()) ? 
            "PRIMARY ADMIN" : "ADMIN";
        
        String content = String.format(
            "📧 Email: %s\n" +
            "👤 Username: %s\n" +
            "🎯 Role: %s\n" +
            "📅 Joined: %s\n" +
            "\nUser ID: %d",
            admin.getEmail(),
            admin.getUsername(),
            roleText,
            admin.getCreatedAt() != null ? 
                admin.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm")) : "N/A",
            admin.getId()
        );
        
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    @FXML
    private void handleClear() {
        clearFields();
        messageLabel.setText("");
        messageLabel.setVisible(false);
        statusLabel.setText("Ready");
        
        // Reset field styles
        fullNameField.setStyle("");
        emailField.setStyle("");
        usernameField.setStyle("");
        passwordField.setStyle("");
        confirmPasswordField.setStyle("");
    }
    
    @FXML
    private void handleRefreshAdmins() {
        loadAdminsAsync();
        statusLabel.setText("Refreshing...");
    }
    
    @FXML
    private void handleSearchAdmins() {
        filterAdmins(searchField.getText());
    }
    
    private void clearFields() {
        fullNameField.clear();
        emailField.clear();
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }
    
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (databaseService != null) {
            databaseService.close();
        }
    }
}