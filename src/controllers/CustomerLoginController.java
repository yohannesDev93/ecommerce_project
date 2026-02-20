package controllers;

import services.DatabaseService;
import models.User;
import models.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;

public class CustomerLoginController {
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Button registerBtn;
    @FXML private Button backBtn;
    @FXML private Label errorLabel;
    
    private DatabaseService databaseService;
    
    @FXML
    public void initialize() {
        databaseService = new DatabaseService();
    }
    private void Admin(ActionEvent event) {
    	try {
            // Load admin dashboard
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AdminDashboard.fxml"));
            
            // Get current stage
            Node source = (Node) event.getSource();
            Stage currentStage = (Stage) source.getScene().getWindow();
            
            // Create new stage for dashboard
            Stage dashboardStage = new Stage();
            Scene scene = new Scene(root);
            dashboardStage.setScene(scene);
            dashboardStage.setTitle("Admin Dashboard");
            dashboardStage.setMaximized(true);
            
            // Close login window and show dashboard
            currentStage.close();
            dashboardStage.show();
            
            // Close database connection from login controller
            databaseService.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading dashboard: " + e.getMessage());
        }
    }
    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username/email and password");
            return;
        }
        
        // Validate customer against database
        boolean isCustomer = databaseService.customerLogin(username, password);
        boolean isAdmin = databaseService.validateAdmin(username, password);
        
        if (isCustomer) {
            try {
                // Get full user details from database
                User user = databaseService.getUserByUsername(username);
                
                if (user != null) {
                    // Initialize UserSession
                    UserSession session = UserSession.getInstance();
                    session.initSession(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getRole()
                    );
                    
                    
                	System.out.println("User logged in: " + user.getUsername() + " (ID: " + user.getId() + ")");
                    System.out.println("Full name: " + user.getFullName());
                    System.out.println("Role: " + user.getRole());
                    
                    // Load customer store
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerStore.fxml"));
                    Parent root = loader.load();
                    root.getStylesheets().add(getClass().getResource("/styles/store.css").toExternalForm());

                    
                    // Get controller and pass user data
                    CustomerStoreController controller = loader.getController();
                    controller.setCurrentUser(session); // Add this method to CustomerStoreController
                    
                    // Get current stage
                    Node source = (Node) event.getSource();
                    Stage currentStage = (Stage) source.getScene().getWindow();
                    
                    // Create new stage for store
                    Stage storeStage = new Stage();
                    storeStage.setScene(new Scene(root, 1200, 800));
                    storeStage.setTitle("E-Commerce Store - Welcome " + user.getFullName());
                    storeStage.setMaximized(true);
                    
                    // Close login window and show store
                    currentStage.close();
                    storeStage.show();
                
                    
                } else {
                    showError("User details not found in database");
                    return;
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error loading store: " + e.getMessage());
            }
        } else if(isAdmin) {
        	Admin(event);
        }
        else {
            showError("Invalid username or password. Please try again.");
        }
    }
    
    @FXML
    private void openRegister(ActionEvent event) {
        try {
        	Node source = (Node) event.getSource();
	        Stage currentStage = (Stage) source.getScene().getWindow();
	        
	        // Load role selection screen
	        Parent root = FXMLLoader.load(getClass().getResource("/fxml/CustomerRegister.fxml"));
	        Stage newStage = new Stage();
	        newStage.setScene(new Scene(root, 800, 500));
	        newStage.setTitle("Welcome  to create account");
	        newStage.setMaximized(true);
	        // Optional: Show logout confirmation
//	        System.out.println("User '" + username + "' logged out successfully");
	        
	        // Close current and show new
	        currentStage.close();
	        newStage.show();
        } catch (Exception e) {
            showError("Error loading registration: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleBack(ActionEvent event) {
        try {
        	Node source = (Node) event.getSource();
	        Stage currentStage = (Stage) source.getScene().getWindow();
	        
	        // Load role selection screen
	        Parent root = FXMLLoader.load(getClass().getResource("/fxml/RoleSelection.fxml"));
	        Stage newStage = new Stage();
	        newStage.setScene(new Scene(root, 800, 500));
	        newStage.setTitle("E-Commerce Store - Select Role");
	        newStage.setMaximized(true);
	        // Optional: Show logout confirmation
//	        System.out.println("User '" + username + "' logged out successfully");
	        
	        // Close current and show new
	        currentStage.close();
	        newStage.show();
        } catch (Exception e) {
            showError("Error going back: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleForgotPassword(ActionEvent event) {
        showAlert("Forgot Password", "Please contact customer support at support@store.com");
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    public void shutdown() {
        if (databaseService != null) {
            databaseService.close();
        }
    }
}