package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

public class PaymentMethodController {
    @FXML private ToggleGroup paymentGroup;
    @FXML private RadioButton telebirrRadio;
    @FXML private RadioButton bankRadio;
    @FXML private RadioButton creditCardRadio;
    @FXML private RadioButton paypalRadio;
    
    private String selectedPaymentMethod;
    
    @FXML
    public void initialize() {
        // Select first option by default
        telebirrRadio.setSelected(true);
        selectedPaymentMethod = "Tele Birr";
        
        // Add listeners
        telebirrRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) selectedPaymentMethod = "Tele Birr";
        });
        bankRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) selectedPaymentMethod = "Bank Transfer";
        });
        creditCardRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) selectedPaymentMethod = "Credit Card";
        });
        paypalRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) selectedPaymentMethod = "PayPal";
        });
    }
    
    public String getSelectedPaymentMethod() {
        return selectedPaymentMethod;
    }
}