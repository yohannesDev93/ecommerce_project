package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class RoleSelectionController {
    
    @FXML
    private void openAdminLogin(ActionEvent event) {
        try {
            System.out.println("Loading AdminLogin.fxml...");
            
            // Load the FXML
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AdminLogin.fxml"));
            root.getStylesheets().add(getClass().getResource("/styles/customer-login.css").toExternalForm());
            // Get the current stage from the event source
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            
            // Set new scene
            Scene scene = new Scene(root, 800, 500);
            stage.setScene(scene);
            stage.setTitle("Admin Login");
           
            
            System.out.println("Admin login screen loaded successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load admin login: " + e.getMessage());
        }
    }
    
    @FXML
    private void openCustomerLogin(ActionEvent event) {
        try {
        	Node source = (Node) event.getSource();
	        Stage currentStage = (Stage) source.getScene().getWindow();
	        
	        // Load role selection screen
	        Parent root = FXMLLoader.load(getClass().getResource("/fxml/CustomerLogin.fxml"));
            root.getStylesheets().add(getClass().getResource("/styles/customer-login.css").toExternalForm());

	        Stage newStage = new Stage();
	        newStage.setScene(new Scene(root, 800, 500));
	        newStage.setTitle("Welcome back to login page");
	        newStage.setMaximized(true);
	        // Optional: Show logout confirmation
//	        System.out.println("User '" + username + "' logged out successfully");
	        
	        // Close current and show new
	        currentStage.close();
	        newStage.show();
        } catch (Exception e) {
            showAlert("Error", "Cannot load customer login: " + e.getMessage());
            System.out.println(e);
        }
    }
    
    @FXML
    private void openGuestStore(ActionEvent event) {
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