package models;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class OrderTracking {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty orderId = new SimpleIntegerProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty notes = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> updateTime = new SimpleObjectProperty<>();
    
    public OrderTracking() {}
    
    public OrderTracking(int orderId, String status, String notes) {
        setOrderId(orderId);
        setStatus(status);
        setNotes(notes);
        setUpdateTime(LocalDateTime.now());
    }
    
    // Getters and Setters
    public IntegerProperty idProperty() { return id; }
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    
    public IntegerProperty orderIdProperty() { return orderId; }
    public int getOrderId() { return orderId.get(); }
    public void setOrderId(int orderId) { this.orderId.set(orderId); }
    
    public StringProperty statusProperty() { return status; }
    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    
    public StringProperty notesProperty() { return notes; }
    public String getNotes() { return notes.get(); }
    public void setNotes(String notes) { this.notes.set(notes); }
    
    public ObjectProperty<LocalDateTime> updateTimeProperty() { return updateTime; }
    public LocalDateTime getUpdateTime() { return updateTime.get(); }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime.set(updateTime); }
    
    public String getFormattedUpdateTime() {
        if (updateTime.get() != null) {
            return updateTime.get().toString().replace("T", " ");
        }
        return "";
    }
    
    @Override
    public String toString() {
        return getStatus() + " - " + getFormattedUpdateTime();
    }
}