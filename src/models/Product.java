package models;

import javafx.beans.property.*;

public class Product {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final DoubleProperty price = new SimpleDoubleProperty(); // Price in USD (base currency)
    private final IntegerProperty categoryId = new SimpleIntegerProperty();
    private final StringProperty categoryName = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty imageUrl = new SimpleStringProperty();
    private final IntegerProperty stockQuantity = new SimpleIntegerProperty();
    private final DoubleProperty discountPercentage = new SimpleDoubleProperty();
    private final DoubleProperty finalPrice = new SimpleDoubleProperty(); // Final price in USD
    
    // Currency conversion properties
    private String currency = "USD"; // Default currency
    private final DoubleProperty displayedPrice = new SimpleDoubleProperty(); // Price in selected currency
    
    public Product() {}
    
    // Constructor for database
    public Product(int id, String name, double price, int categoryId, 
                   String categoryName, String description, String imageUrl, 
                   int stockQuantity, double discountPercentage) {
        setId(id);
        setName(name);
        setPrice(price); // Store price in USD
        setCategoryId(categoryId);
        setCategoryName(categoryName);
        setDescription(description);
        setImageUrl(imageUrl);
        setStockQuantity(stockQuantity);
        setDiscountPercentage(discountPercentage);
        calculateFinalPrice(); // Calculate final price in USD
    }
    
    // Getters and Setters
    public IntegerProperty idProperty() { return id; }
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    
    // Price in USD (base currency)
    public DoubleProperty priceProperty() { return price; }
    public double getPrice() { return price.get(); } // Returns price in USD
    public void setPrice(double price) { 
        this.price.set(price);
        calculateFinalPrice();
    }
    
    public IntegerProperty categoryIdProperty() { return categoryId; }
    public int getCategoryId() { return categoryId.get(); }
    public void setCategoryId(int categoryId) { this.categoryId.set(categoryId); }
    
    public StringProperty categoryNameProperty() { return categoryName; }
    public String getCategoryName() { return categoryName.get(); }
    public void setCategoryName(String categoryName) { this.categoryName.set(categoryName); }
    
    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
    
    public StringProperty imageUrlProperty() { return imageUrl; }
    public String getImageUrl() { return imageUrl.get(); }
    public void setImageUrl(String imageUrl) { this.imageUrl.set(imageUrl); }
    
    public IntegerProperty stockQuantityProperty() { return stockQuantity; }
    public int getStockQuantity() { return stockQuantity.get(); }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity.set(stockQuantity); }
    
    public DoubleProperty discountPercentageProperty() { return discountPercentage; }
    public double getDiscountPercentage() { return discountPercentage.get(); }
    public void setDiscountPercentage(double discountPercentage) { 
        this.discountPercentage.set(discountPercentage);
        calculateFinalPrice();
    }
    
    // Final price in USD (after discount)
    public DoubleProperty finalPriceProperty() { return finalPrice; }
    public double getFinalPrice() { return finalPrice.get(); } // Returns final price in USD
    
    // Get final price in specific currency
    public double getFinalPriceInCurrency(String targetCurrency) {
        double priceInUSD = getFinalPrice();
        return convertToCurrency(priceInUSD, targetCurrency);
    }
    
    // Get original price in specific currency (before discount)
    public double getPriceInCurrency(String targetCurrency) {
        double priceInUSD = getPrice();
        return convertToCurrency(priceInUSD, targetCurrency);
    }
    
    // Currency conversion methods
    public void setCurrency(String currency) {
        this.currency = currency;
        updateDisplayedPrice();
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public DoubleProperty displayedPriceProperty() { 
        updateDisplayedPrice();
        return displayedPrice; 
    }
    
    public double getDisplayedPrice() {
        updateDisplayedPrice();
        return displayedPrice.get();
    }
    
    // Currency conversion rates (USD is base currency)
    private static final java.util.Map<String, Double> CURRENCY_RATES = new java.util.HashMap<>();
    static {
        CURRENCY_RATES.put("USD", 1.0);
        CURRENCY_RATES.put("ETB", 58.5);  // 1 USD = 58.5 ETB
        CURRENCY_RATES.put("EUR", 0.92);  // 1 USD = 0.92 EUR
    }
    
    private void updateDisplayedPrice() {
        double priceInUSD = getFinalPrice();
        double convertedPrice = convertToCurrency(priceInUSD, currency);
        displayedPrice.set(convertedPrice);
    }
    
    private double convertToCurrency(double priceInUSD, String targetCurrency) {
        Double rate = CURRENCY_RATES.get(targetCurrency);
        if (rate == null) {
            rate = 1.0; // Default to USD if currency not found
        }
        return priceInUSD * rate;
    }
    
    // Format price with currency symbol
    public String getFormattedPrice() {
        return getFormattedPrice(currency);
    }
    
    public String getFormattedPrice(String targetCurrency) {
        double price = getFinalPriceInCurrency(targetCurrency);
        
        switch(targetCurrency) {
            case "USD":
                return String.format("$%.2f", price);
            case "ETB":
                return String.format("ETB %.2f", price);
            case "EUR":
                return String.format("€%.2f", price);
            default:
                return String.format("$%.2f", price);
        }
    }
    
    // Get currency symbol
    public String getCurrencySymbol(String currency) {
        switch(currency) {
            case "USD": return "$";
            case "ETB": return "ETB ";
            case "EUR": return "€";
            default: return "$";
        }
    }
    
    private void calculateFinalPrice() {
        double discount = getDiscountPercentage();
        double originalPrice = getPrice(); // Price in USD
        double finalPriceValue = originalPrice * (1 - discount / 100);
        this.finalPrice.set(Math.round(finalPriceValue * 100.0) / 100.0); // Round to 2 decimals
        updateDisplayedPrice();
    }
    
    // Helper method to check if product has discount
    public boolean hasDiscount() {
        return getDiscountPercentage() > 0;
    }
    
    // Helper method to get original price in specific currency
    public String getOriginalFormattedPrice(String currency) {
        double price = getPriceInCurrency(currency);
        
        switch(currency) {
            case "USD":
                return String.format("$%.2f", price);
            case "ETB":
                return String.format("ETB %.2f", price);
            case "EUR":
                return String.format("€%.2f", price);
            default:
                return String.format("$%.2f", price);
        }
    }
    
    @Override
    public String toString() {
        return getName() + " (" + getFormattedPrice() + ")";
    }
}