package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;

public class TelebirrController extends BasePaymentController {
    @FXML private TextField phoneField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private Label errorLabel;
    
    @FXML
    public void initialize() {
        // Setup validation
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            validateInput();
        });
    }
    
    @Override
    public boolean validateInput() {
        StringBuilder errors = new StringBuilder();
        
        if (firstNameField.getText().trim().isEmpty()) {
            errors.append("First name is required\n");
        }
        if (lastNameField.getText().trim().isEmpty()) {
            errors.append("Last name is required\n");
        }
        if (!phoneField.getText().matches("^\\+251\\d{9}$")) {
            errors.append("Phone must be in format +251XXXXXXXXX\n");
        }
        
        if (errors.length() > 0) {
            errorLabel.setText(errors.toString());
            errorLabel.setVisible(true);
            return false;
        }
        
        errorLabel.setVisible(false);
        return true;
    }
}