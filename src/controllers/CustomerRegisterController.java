package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.DatabaseService;
import utils.PasswordValidator;
import javafx.event.ActionEvent;
import javafx.scene.Node;

public class CustomerRegisterController {
	private DatabaseService databaseService;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressField;
    @FXML private CheckBox termsCheckBox;
    @FXML private CheckBox newsletterCheckBox;
    @FXML private Button registerBtn;
    @FXML private Button backBtn;
    @FXML private Label errorLabel;
    
   
    
	private boolean validateInputs() {
	    StringBuilder errors = new StringBuilder();
	    
	    // Check required fields
	    if (firstNameField.getText().trim().isEmpty()) {
	        errors.append("• First name is required\n");
	    }
	    
	    if (lastNameField.getText().trim().isEmpty()) {
	        errors.append("• Last name is required\n");
	    }
	    
	    if (emailField.getText().trim().isEmpty()) {
	        errors.append("• Email is required\n");
	    } else if (!isValidEmail(emailField.getText())) {
	        errors.append("• Invalid email format\n");
	    }
	    
	    if (usernameField.getText().trim().isEmpty()) {
	        errors.append("• Username is required\n");
	    } else if (usernameField.getText().length() < 3) {
	        errors.append("• Username must be at least 3 characters\n");
	    }
	    
	    // Password complexity validation
	    String password = passwordField.getText();
	    if (password.isEmpty()) {
	        errors.append("• Password is required\n");
	    } else {
	        PasswordValidator.ValidationResult passwordResult = PasswordValidator.validatePassword(password);
	        if (!passwordResult.isSuccess()) {
	            errors.append("• Password: " + passwordResult.getErrorMessage() + "\n");
	        }
	    }
	    
	    if (!passwordField.getText().equals(confirmPasswordField.getText())) {
	        errors.append("• Passwords do not match\n");
	    }
	    
	    if (!termsCheckBox.isSelected()) {
	        errors.append("• You must agree to terms and conditions\n");
	    }
	    
	    if (errors.length() > 0) {
	        errorLabel.setText(errors.toString());
	        errorLabel.setVisible(true);
	        return false;
	    }
	    
	    return true;
	}

	@FXML
	private void handleRegister(ActionEvent event) {
	    // Reset error
	    errorLabel.setVisible(false);
	    
	    // Validate inputs
	    if (!validateInputs()) {
	        return;
	    }
	    
	    // Register customer with hashed password
	    String fullName = firstNameField.getText() + " " + lastNameField.getText();
	    databaseService = new DatabaseService();
	    
	    // Password will be hashed inside registerCustomer method
	    databaseService.registerCustomer(usernameField.getText(), 
	                                    emailField.getText(), 
	                                    confirmPasswordField.getText(), 
	                                    fullName);
	    
	    showAlert("Registration Successful", 
	        "Welcome to our store, " + firstNameField.getText() + "!\n\n" +
	        "Your account has been created successfully.\n" +
	        "You will receive a confirmation email shortly.\n\n" +
	        "Use coupon code: WELCOME10 for 10% off your first order!");
	    
	    // Go to login page
	    handleBack(event);
	}
    
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
    
    @FXML
    private void handleBack(ActionEvent event) {
        try {
        	Node source = (Node) event.getSource();
	        Stage currentStage = (Stage) source.getScene().getWindow();
	        
	        // Load role selection screen
	        Parent root = FXMLLoader.load(getClass().getResource("/fxml/CustomerLogin.fxml"));
	        Stage newStage = new Stage();
	        newStage.setScene(new Scene(root, 800, 500));
	        newStage.setTitle("Welcom Back");
	        newStage.setMaximized(true);
	        // Optional: Show logout confirmation
//	        System.out.println("User '" + username + "' logged out successfully");
	        
	        // Close current and show new
	        currentStage.close();
	        newStage.show();
        } catch (Exception e) {
            showAlert("Error", "Error going back: " + e.getMessage());
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