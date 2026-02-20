import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/RoleSelection.fxml"));
            
            
            Scene scene = new Scene(root, 800, 500);
            scene.getStylesheets().add(getClass().getResource("/styles/store.css").toExternalForm());

            primaryStage.setTitle("Welcome to E-Commerce Store");
            primaryStage.setMaximized(true);
            primaryStage.setScene(scene);
            primaryStage.show();
            
        } catch (Exception e) {
            System.out.println("Error loading FXML: " + e.getMessage());
            e.printStackTrace();
  // Fallback UI
        }
    }
    
   
    
    public static void main(String[] args) {
        System.setProperty("prism.verbose", "true");
        launch(args);
    }
}