package models;

import javafx.beans.property.*;

public class Category {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    
    public Category() {}
    
    public Category(int id, String name, String description) {
        setId(id);
        setName(name);
        setDescription(description);
    }
    
    // Getters and Setters
    public IntegerProperty idProperty() { return id; }
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    
    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
    
    @Override
    public String toString() {
        return getName();
    }
}