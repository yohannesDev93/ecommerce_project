package controllers;

import services.DatabaseService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;

public class AdminLoginController {
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Button backBtn;
    @FXML private Label errorLabel;
    
    private DatabaseService databaseService;
    
    @FXML
    public void initialize() {
        databaseService = new DatabaseService();
    }
    
    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }
        
        // Validate against database
        boolean isValid = databaseService.validateAdmin(username, password);
        
        if (isValid) {
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
        } else {
            showError("Invalid username or password");
        }
    }
    
    @FXML
    private void handleBack(ActionEvent event) {
        try {
            // Close database connection
            if (databaseService != null) {
                databaseService.close();
            }
            
            // Load role selection screen
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/RoleSelection.fxml"));
            
            // Get current stage
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            
            // Set new scene
            Scene scene = new Scene(root, 800, 500);
            stage.setScene(scene);
            stage.setTitle("E-Commerce Store");
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error going back: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}