package models;

public class CartItem {
    private int productId;
    private String productName;
    private double price;
    private double finalPrice;
    private int quantity;
    
    // Constructor
    public CartItem(Product product) {
        this.productId = product.getId();
        this.productName = product.getName();
        this.price = product.getPrice();
        this.finalPrice = product.getFinalPrice();
        this.quantity = 1;
    }
    
    // Default constructor
    public CartItem() {
        this.quantity = 1;
    }
    
    // Getters and Setters
    public int getProductId() {
        return productId;
    }
    
    public void setProductId(int productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public double getFinalPrice() {
        return finalPrice;
    }
    
    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    // Calculate subtotal (price * quantity)
    public double getSubtotal() {
        return finalPrice * quantity;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CartItem cartItem = (CartItem) obj;
        return productId == cartItem.productId;
    }
    
    @Override
    public int hashCode() {
        return productId;
    }
}