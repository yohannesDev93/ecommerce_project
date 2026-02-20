package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

public class StageManager {
    
    public static void changeScene(ActionEvent event, String fxmlPath, String title, int width, int height) {
        try {
            Parent root = FXMLLoader.load(StageManager.class.getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, width, height));
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void changeScene(Stage stage, String fxmlPath, String title, int width, int height) {
        try {
            Parent root = FXMLLoader.load(StageManager.class.getResource(fxmlPath));
            stage.setScene(new Scene(root, width, height));
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}