package services;

import models.Product;
import models.User;
import utils.PasswordHasher;
import models.Category;
import models.Message;
import models.Order;
import models.OrderItem;
import models.OrderTracking;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = ""; // Your password here
    
    private Connection connection;
    
    public DatabaseService() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Database connected successfully!");
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }
    
    private void initializeDatabase() {
        try {
            Statement stmt = connection.createStatement();
            
            // Create tables if they don't exist (simplified version)
            String[] createTables = {
                // Users table
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "username VARCHAR(50) UNIQUE NOT NULL," +
                "password VARCHAR(100) NOT NULL," +
                "email VARCHAR(100) UNIQUE NOT NULL," +
                "role VARCHAR(20) DEFAULT 'CUSTOMER'," +
                "full_name VARCHAR(100)," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")",
                
                // Categories table
                "CREATE TABLE IF NOT EXISTS categories (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "name VARCHAR(100) NOT NULL UNIQUE," +
                "description TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")",
                
                // Products table with category_id foreign key
                "CREATE TABLE IF NOT EXISTS products (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "name VARCHAR(200) NOT NULL," +
                "description TEXT," +
                "price DECIMAL(10,2) NOT NULL," +
                "category_id INT NOT NULL," +
                "image_url VARCHAR(500)," +
                "stock_quantity INT DEFAULT 0," +
                "discount_percentage DECIMAL(5,2) DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE" +
                ")",
             // Orders table
                "CREATE TABLE IF NOT EXISTS orders (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "customer_id INT NOT NULL," +
                "order_number VARCHAR(50) UNIQUE NOT NULL," +
                "order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "total_amount DECIMAL(10,2) NOT NULL," +
                "shipping_address TEXT NOT NULL," +
                "payment_method VARCHAR(50) NOT NULL," +
                "payment_status VARCHAR(20) DEFAULT 'PENDING'," +
                "order_status VARCHAR(20) DEFAULT 'PENDING'," +
                "notes TEXT," +
                "FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")",

                // Order items table
                "CREATE TABLE IF NOT EXISTS order_items (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "order_id INT NOT NULL," +
                "product_id INT NOT NULL," +
                "quantity INT NOT NULL," +
                "unit_price DECIMAL(10,2) NOT NULL," +
                "total_price DECIMAL(10,2) NOT NULL," +
                "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE," +
                "FOREIGN KEY (product_id) REFERENCES products(id)" +
                ")",

                // Order tracking table
                "CREATE TABLE IF NOT EXISTS order_tracking (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "order_id INT NOT NULL," +
                "status VARCHAR(50) NOT NULL," +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "notes TEXT," +
                "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE" +
                ")",

                // Messages table for customer support
                "CREATE TABLE IF NOT EXISTS messages (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "customer_id INT NOT NULL," +
                "subject VARCHAR(200) NOT NULL," +
                "message TEXT NOT NULL," +
                "admin_reply TEXT," +
                "status VARCHAR(20) DEFAULT 'UNREAD'," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "replied_at TIMESTAMP NULL," +
                "FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")",

                // Payment methods table
                "CREATE TABLE IF NOT EXISTS payment_methods (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "name VARCHAR(50) NOT NULL UNIQUE," +
                "description TEXT," +
                "is_active BOOLEAN DEFAULT true" +
                ")"
            };
            
            for (String sql : createTables) {
                stmt.execute(sql);
            }
            
            // Insert default data
            insertDefaultData();
            
            migratePlainPasswordsToHashed();
            
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }
    
 // Add this getter method to DatabaseService class
    public Connection getConnection() {
        return this.connection;
    }
    private void insertDefaultData() {
        try {
            Statement stmt = connection.createStatement();
            
            // Insert categories if empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories");
            rs.next();
            if (rs.getInt(1) == 0) {
                String[] categories = {
                    "INSERT INTO categories (name, description) VALUES ('Electronics', 'Electronic devices and gadgets')",
                    "INSERT INTO categories (name, description) VALUES ('Clothing', 'Fashion and apparel')",
                    "INSERT INTO categories (name, description) VALUES ('Books', 'Books and educational materials')",
                    "INSERT INTO categories (name, description) VALUES ('Home & Garden', 'Home improvement supplies')",
                    "INSERT INTO categories (name, description) VALUES ('Accessories', 'Fashion accessories')"
                };
                
                for (String sql : categories) {
                    stmt.executeUpdate(sql);
                }
            }
            
            // Insert admin user if not exists
            rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'admin'");
            rs.next();
            if (rs.getInt(1) == 0) {
                // Hash the admin password
                String hashedPassword = PasswordHasher.hashPassword("admin123");
                
                stmt.executeUpdate("INSERT INTO users (username, password, email, role, full_name) " +
                                   "VALUES ('admin', '" + hashedPassword + "', 'admin@store.com', 'ADMIN', 'System Administrator')");
                System.out.println("✓ Default admin created with hashed password");
            }
            
            // Insert sample products if empty
            rs = stmt.executeQuery("SELECT COUNT(*) FROM products");
            rs.next();
            if (rs.getInt(1) == 0) {
                insertSampleProducts();
            }
            
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error inserting default data: " + e.getMessage());
        }
    }
    /**
     * Migrate existing plain passwords to hashed passwords
     */
    private void migratePlainPasswordsToHashed() {
        try {
            String query = "SELECT id, password FROM users";
            PreparedStatement selectStmt = connection.prepareStatement(query);
            ResultSet rs = selectStmt.executeQuery();
            
            int migratedCount = 0;
            while (rs.next()) {
                String password = rs.getString("password");
                int userId = rs.getInt("id");
                
                // Check if password is not hashed (short password = likely plain text)
                if (password != null && password.length() < 50) {
                    // Hash the plain password
                    String hashedPassword = PasswordHasher.hashPassword(password);
                    
                    String updateQuery = "UPDATE users SET password = ? WHERE id = ?";
                    PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                    updateStmt.setString(1, hashedPassword);
                    updateStmt.setInt(2, userId);
                    updateStmt.executeUpdate();
                    updateStmt.close();
                    
                    migratedCount++;
                    System.out.println("Migrated password for user ID: " + userId);
                }
            }
            
            rs.close();
            selectStmt.close();
            if (migratedCount > 0) {
                System.out.println("✓ Migrated " + migratedCount + " passwords to hashed format");
            }
            
        } catch (SQLException e) {
            System.err.println("Error migrating passwords: " + e.getMessage());
        }
    }
    private void insertSampleProducts() {
        try {
            // Get category IDs
            Statement stmt = connection.createStatement();
            
            // Create map of category names to IDs
            ResultSet categoryRs = stmt.executeQuery("SELECT id, name FROM categories");
            java.util.Map<String, Integer> categoryMap = new java.util.HashMap<>();
            while (categoryRs.next()) {
                categoryMap.put(categoryRs.getString("name"), categoryRs.getInt("id"));
            }
            
            // Sample products with category IDs
            String[][] sampleProducts = {
                // name, price, category_id, description, stock
                {"iPhone 15 Pro", "999.99", String.valueOf(categoryMap.get("Electronics")), 
                 "Latest iPhone with advanced camera", "50"},
                {"MacBook Pro", "2399.99", String.valueOf(categoryMap.get("Electronics")), 
                 "Apple laptop for professionals", "30"},
                {"Nike Air Max", "149.99", String.valueOf(categoryMap.get("Clothing")), 
                 "Comfortable running shoes", "100"},
                {"Levis 501 Jeans", "79.99", String.valueOf(categoryMap.get("Clothing")), 
                 "Classic straight fit jeans", "120"},
                {"The Psychology of Money", "19.99", String.valueOf(categoryMap.get("Books")), 
                 "Wealth and happiness lessons", "200"},
                {"Atomic Habits", "16.99", String.valueOf(categoryMap.get("Books")), 
                 "Build good habits", "180"},
                {"Dyson Vacuum", "699.99", String.valueOf(categoryMap.get("Home & Garden")), 
                 "Cordless vacuum cleaner", "40"},
                {"KitchenAid Mixer", "429.99", String.valueOf(categoryMap.get("Home & Garden")), 
                 "Stand mixer for baking", "35"},
                {"Ray-Ban Sunglasses", "159.99", String.valueOf(categoryMap.get("Accessories")), 
                 "Classic aviator sunglasses", "80"},
                {"Fossil Watch", "129.99", String.valueOf(categoryMap.get("Accessories")), 
                 "Leather strap watch", "60"}
            };
            
            for (String[] product : sampleProducts) {
                String sql = String.format(
                    "INSERT INTO products (name, price, category_id, description, stock_quantity) " +
                    "VALUES ('%s', %s, %s, '%s', %s)",
                    product[0], product[1], product[2], product[3], product[4]
                );
                stmt.executeUpdate(sql);
            }
            
            System.out.println("Sample products inserted successfully!");
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error inserting sample products: " + e.getMessage());
        }
    }
    
    // PRODUCT METHODS
// In getAllProducts method, ensure image_url is included:
	public List<Product> getAllProducts() {
	    List<Product> products = new ArrayList<>();
	    
	    try {
	        // Join with categories to get category name
	        String query = "SELECT p.*, c.name as category_name " +
	                      "FROM products p " +
	                      "JOIN categories c ON p.category_id = c.id " +
	                      "ORDER BY p.id DESC";
	        
	        PreparedStatement pstmt = connection.prepareStatement(query);
	        ResultSet rs = pstmt.executeQuery();
	        
	        while (rs.next()) {
	            Product product = new Product(
	                rs.getInt("id"),
	                rs.getString("name"),
	                rs.getDouble("price"),
	                rs.getInt("category_id"),
	                rs.getString("category_name"),
	                rs.getString("description"),
	                rs.getString("image_url"), // This might be null
	                rs.getInt("stock_quantity"),
	                rs.getDouble("discount_percentage")
	            );
	            products.add(product);
	        }
	        
	        rs.close();
	        pstmt.close();
	        
	        
	    } catch (SQLException e) {
	        System.err.println("Error fetching products: " + e.getMessage());
	        e.printStackTrace();
	    }
	    
	    return products;
	}
	
	
    
    public List<Product> getProductsByCategory(int categoryId) {
        List<Product> products = new ArrayList<>();
        
        try {
            String query = "SELECT p.*, c.name as category_name " +
                          "FROM products p " +
                          "JOIN categories c ON p.category_id = c.id " +
                          "WHERE p.category_id = ? " +
                          "ORDER BY p.id";
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Product product = new Product(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getInt("category_id"),
                    rs.getString("category_name"),
                    rs.getString("description"),
                    rs.getString("image_url"),
                    rs.getInt("stock_quantity"),
                    rs.getDouble("discount_percentage")
                );
                products.add(product);
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error fetching products by category: " + e.getMessage());
        }
        
        return products;
    }
 
//    // Order Methods
//	public boolean createOrder(Order order) {
//	    try {
//	        connection.setAutoCommit(false);
//	        
//	        // Insert order
//	        String orderSql = "INSERT INTO orders (order_number, customer_id, total_amount, " +
//	                         "shipping_address, shipping_city, shipping_phone, shipping_email, " +
//	                         "payment_method, payment_status, order_status, notes) " +
//	                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
//	        
//	        PreparedStatement orderStmt = connection.prepareStatement(orderSql, 
//	            Statement.RETURN_GENERATED_KEYS);
//	        
//	        orderStmt.setString(1, order.getOrderId());
//	        orderStmt.setInt(2, order.getCustomerId());
//	        orderStmt.setDouble(3, order.getTotalAmount());
//	        orderStmt.setString(4, order.getShippingAddress());
//	        orderStmt.setString(5, order.getShippingAddress());
//	        orderStmt.setString(6, order.getCustomerPhone());
//	        orderStmt.setString(7, order.getCustomerEmail());
//	        orderStmt.setString(8, order.getPaymentMethod());
////	        orderStmt.setString(9, order.getPaymentStatus());
//	        orderStmt.setString(10, order.getStatus());
////	        orderStmt.setString(11, order.getNotes());
//	        
//	        int orderRows = orderStmt.executeUpdate();
//	        ResultSet rs = orderStmt.getGeneratedKeys();
//	        int orderId = 0;
//	        if (rs.next()) {
//	            orderId = orderId;
//	        }
//	        orderStmt.close();
//	        
//	        // Insert order items
//	        String itemSql = "INSERT INTO order_items (order_id, product_id, product_name, " +
//	                        "quantity, unit_price, original_price, discount_percentage, subtotal) " +
//	                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
//	        PreparedStatement itemStmt = connection.prepareStatement(itemSql);
//	        
//	        for (OrderItem item : order.getItems()) {
//	            itemStmt.setInt(1, orderId);
//	            itemStmt.setInt(2, item.getProductId());
//	            itemStmt.setString(3, item.getProductName());
//	            itemStmt.setInt(4, item.getQuantity());
//	            itemStmt.setDouble(5, item.getUnitPrice());
//	            itemStmt.setDouble(6, item.getOriginalPrice());
//	            itemStmt.setDouble(7, item.getDiscountPercentage());
//	            itemStmt.setDouble(8, item.getSubtotal());
//	            itemStmt.addBatch();
//	            
//	            // Update product stock
//	            updateProductStock(item.getProductId(), -item.getQuantity());
//	        }
//	        
//	        int[] itemRows = itemStmt.executeBatch();
//	        itemStmt.close();
//	        
//	        // Insert initial tracking
//	        String trackingSql = "INSERT INTO order_tracking (order_id, status, notes) VALUES (?, ?, ?)";
//	        PreparedStatement trackingStmt = connection.prepareStatement(trackingSql);
//	        trackingStmt.setInt(1, orderId);
//	        trackingStmt.setString(2, "ORDER_PLACED");
//	        trackingStmt.setString(3, "Order placed successfully");
//	        trackingStmt.executeUpdate();
//	        trackingStmt.close();
//	        
//	        connection.commit();
//	        return true;
//	        
//	    } catch (SQLException e) {
//	        try {
//	            connection.rollback();
//	        } catch (SQLException ex) {
//	            ex.printStackTrace();
//	        }
//	        e.printStackTrace();
//	        return false;
//	    } finally {
//	        try {
//	            connection.setAutoCommit(true);
//	        } catch (SQLException e) {
//	            e.printStackTrace();
//	        }
//	    }
//	}
//	
	public User getUserByUsername(String username) {
	    try {
	        String query = "SELECT id, username, email, full_name, role FROM users WHERE username = ? OR email = ?";
	        PreparedStatement pstmt = connection.prepareStatement(query);
	        pstmt.setString(1, username);
	        pstmt.setString(2, username);
	        
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            User user = new User();
	            user.setId(rs.getInt("id"));
	            user.setUsername(rs.getString("username"));
	            user.setEmail(rs.getString("email"));
	            user.setFullName(rs.getString("full_name"));
	            user.setRole(rs.getString("role"));
	            
	            rs.close();
	            pstmt.close();
	            return user;
	        }
	        
	        rs.close();
	        pstmt.close();
	        
	    } catch (SQLException e) {
	        System.err.println("Error getting user: " + e.getMessage());
	    }
	    
	    return null;
	}

	public int getUserIdByUsername(String username) {
	    try {
	        String query = "SELECT id FROM users WHERE username = ? OR email = ?";
	        PreparedStatement pstmt = connection.prepareStatement(query);
	        pstmt.setString(1, username);
	        pstmt.setString(2, username);
	        
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            int userId = rs.getInt("id");
	            rs.close();
	            pstmt.close();
	            return userId;
	        }
	        
	        rs.close();
	        pstmt.close();
	        
	    } catch (SQLException e) {
	        System.err.println("Error getting user ID: " + e.getMessage());
	    }
	    
	    return 0;
	}

//	public List<Order> getOrdersByUserId(int userId) {
//	    List<Order> orders = new ArrayList<>();
//	    
//	    try {
//	        String query = "SELECT o.*, u.full_name as user_name " +
//	                      "FROM orders o " +
//	                      "JOIN users u ON o.customer_id = u.id " +
//	                      "WHERE o.customer_id = ? " +
//	                      "ORDER BY o.order_date DESC";
//	        
//	        PreparedStatement pstmt = connection.prepareStatement(query);
//	        pstmt.setInt(1, userId);
//	        ResultSet rs = pstmt.executeQuery();
//	        
//	        while (rs.next()) {
//	            Order order = new Order(
//	                rs.getString("order_number"),
//	                rs.getInt("customer_id"),
//	                rs.getString("user_name"),
//	                rs.getDouble("total_amount")
//	            );
//	            
//	            order.setId(rs.getInt("id"));
//	            order.setShippingAddress(rs.getString("shipping_address"));
//	            order.setShippingCity(rs.getString("shipping_city"));
//	            order.setShippingPhone(rs.getString("shipping_phone"));
//	            order.setShippingEmail(rs.getString("shipping_email"));
//	            order.setPaymentMethod(rs.getString("payment_method"));
//	            order.setPaymentStatus(rs.getString("payment_status"));
//	            order.setStatus(rs.getString("order_status"));
//	            order.setNotes(rs.getString("notes"));
//	            order.setOrderDate(rs.getTimestamp("order_date").toLocalDateTime());
//	            
//	            // Load order items
//	            order.setItems(getOrderItems(order.getId()));
//	            
//	            // Load tracking history
//	            order.setTrackingHistory(getOrderTracking(order.getId()));
//	            
//	            orders.add(order);
//	        }
//	        
//	        rs.close();
//	        pstmt.close();
//	        
//	    } catch (SQLException e) {
//	        e.printStackTrace();
//	    }
//	    
//	    return orders;
//	}
//
////	

	private List<OrderItem> getOrderItems(int orderId) {
	    List<OrderItem> items = new ArrayList<>();
	    
	    try {
	        String query = "SELECT * FROM order_items WHERE order_id = ?";
	        PreparedStatement pstmt = connection.prepareStatement(query);
	        pstmt.setInt(1, orderId);
	        ResultSet rs = pstmt.executeQuery();
	        
	        while (rs.next()) {
	            OrderItem item = new OrderItem(
	            		rs.getString("order_id"),
		                rs.getInt("product_id"),
		                rs.getString("product_name"),
		                rs.getInt("quantity"),
		                rs.getDouble("unit_price"),
		                rs.getDouble("subtotal")
	            );
	            
	            item.setId(rs.getInt("id"));
	            item.setOrderId(rs.getString("order_id"));
	            item.setUnitPrice(rs.getDouble("original_price"));
//	            item.setDiscountPercentage(rs.getDouble("discount_percentage"));
	            
	            items.add(item);
	        }
	        
	        rs.close();
	        pstmt.close();
	        
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    
	    return items;
	}
	
	
	public boolean saveOrder(Order order, List<OrderItem> orderItems) {
        try {
            connection.setAutoCommit(false);
            
            // Insert order
            String orderSql = "INSERT INTO orders (order_id, customer_id, customer_name, " +
                             "customer_email, customer_phone, shipping_address, total_amount, " +
                             "currency, payment_method, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement orderStmt = connection.prepareStatement(orderSql);
            orderStmt.setString(1, order.getOrderId());
            orderStmt.setInt(2, order.getCustomerId());
            orderStmt.setString(3, order.getCustomerName());
            orderStmt.setString(4, order.getCustomerEmail());
            orderStmt.setString(5, order.getCustomerPhone());
            orderStmt.setString(6, order.getShippingAddress());
            orderStmt.setDouble(7, order.getTotalAmount());
            orderStmt.setString(8, order.getCurrency());
            orderStmt.setString(9, order.getPaymentMethod());
            orderStmt.setString(10, order.getStatus());
            
            int orderResult = orderStmt.executeUpdate();
            System.out.println("Order saved: " + orderResult + " rows affected");
            
            // Insert order items
            String itemSql = "INSERT INTO order_items (order_id, product_id, product_name, " +
                            "quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?, ?)";
            
            PreparedStatement itemStmt = connection.prepareStatement(itemSql);
            int itemCount = 0;
            
            for (OrderItem item : orderItems) {
                itemStmt.setString(1, item.getOrderId());
                itemStmt.setInt(2, item.getProductId());
                itemStmt.setString(3, item.getProductName());
                itemStmt.setInt(4, item.getQuantity());
                itemStmt.setDouble(5, item.getUnitPrice());
                itemStmt.setDouble(6, item.getSubtotal());
                itemStmt.addBatch();
                itemCount++;
            }
            
            int[] itemResults = itemStmt.executeBatch();
            System.out.println("Order items saved: " + itemCount + " items");
            
            connection.commit();
            return true;
            
        } catch (SQLException e) {
            try {
                connection.rollback();
                System.err.println("Order save failed, rolling back: " + e.getMessage());
            } catch (SQLException ex) {
                System.err.println("Rollback failed: " + ex.getMessage());
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Get orders by customer ID
    public List<Order> getCustomerOrders(int customerId) throws SQLException {
    List<Order> orders = new ArrayList<>();
    
    String query = "SELECT * FROM orders WHERE customer_id = ? ORDER BY order_date DESC";
    
    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
        pstmt.setInt(1, customerId); // Filter by customer ID
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            Order order = new Order();
            order.setOrderId(rs.getString("order_id"));
            order.setCustomerId(rs.getInt("customer_id"));
            order.setCustomerName(rs.getString("customer_name"));
            order.setCustomerEmail(rs.getString("customer_email"));
            order.setCustomerPhone(rs.getString("customer_phone"));
            order.setShippingAddress(rs.getString("shipping_address"));
            order.setTotalAmount(rs.getDouble("total_amount"));
            order.setCurrency(rs.getString("currency"));
            order.setPaymentMethod(rs.getString("payment_method"));
            order.setStatus(rs.getString("status"));
            order.setOrderDate(rs.getTimestamp("order_date"));
            
            // Load order items for this order
            order.setItems(getOrderItems(order.getOrderId()));
            
            orders.add(order);
        }
    }
    
    return orders;
}
    
    // Get all orders (for admin)
	   public List<Order> getAllOrders() {
	    List<Order> orders = new ArrayList<>();
	    
	    try {
	        String query = "SELECT o.*, u.full_name as customer_name, u.email as customer_email " +
	                      "FROM orders o " +
	                      "JOIN users u ON o.customer_id = u.id " +
	                      "ORDER BY o.order_date DESC";
	        
	        PreparedStatement pstmt = connection.prepareStatement(query);
	        ResultSet rs = pstmt.executeQuery();
	        
	        while (rs.next()) {
	            Order order = new Order();
	            order.setId(rs.getInt("id"));
	            order.setOrderId(rs.getString("order_id"));
	            order.setCustomerId(rs.getInt("customer_id"));
	            order.setCustomerName(rs.getString("customer_name"));
	            order.setCustomerEmail(rs.getString("customer_email"));
	            order.setShippingAddress(rs.getString("shipping_address"));
	            order.setTotalAmount(rs.getDouble("total_amount"));
	            order.setCurrency(rs.getString("currency"));
	            order.setPaymentMethod(rs.getString("payment_method"));
	            order.setStatus(rs.getString("status"));
	            order.setOrderDate(rs.getTimestamp("order_date"));
	            
	            // Load order items
	            String itemsQuery = "SELECT * FROM order_items WHERE order_id = ?";
	            try (PreparedStatement itemsStmt = connection.prepareStatement(itemsQuery)) {
	                itemsStmt.setString(1, order.getOrderId());
	                ResultSet itemsRs = itemsStmt.executeQuery();
	                List<OrderItem> items = new ArrayList<>();
	                while (itemsRs.next()) {
	                    OrderItem item = new OrderItem();
	                    item.setProductName(itemsRs.getString("product_name"));
	                    item.setQuantity(itemsRs.getInt("quantity"));
	                    item.setUnitPrice(itemsRs.getDouble("unit_price"));
	                    item.setSubtotal(itemsRs.getDouble("subtotal"));
	                    items.add(item);
	                }
	                order.setItems(items);
	            }
	            orders.add(order);
	        }
	        
	        rs.close();
	        pstmt.close();
	        
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    
	    return orders;
		}
    
   

    private List<OrderItem> getOrderItems(String orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        
        String query = "SELECT * FROM order_items WHERE order_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                OrderItem item = new OrderItem();
                item.setId(rs.getInt("id"));
                item.setOrderId(rs.getString("order_id"));
                item.setProductId(rs.getInt("product_id"));
                item.setProductName(rs.getString("product_name"));
                item.setQuantity(rs.getInt("quantity"));
                item.setUnitPrice(rs.getDouble("unit_price"));
                item.setSubtotal(rs.getDouble("subtotal"));
                
                items.add(item);
            }
        }
        
        return items;
    }

    public boolean updateOrderStatus(String orderId, String newStatus) throws SQLException {
        String query = "UPDATE orders SET status = ? WHERE order_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, orderId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
	
	private List<OrderTracking> getOrderTracking(int orderId) {
	    List<OrderTracking> tracking = new ArrayList<>();
	    
	    try {
	        String query = "SELECT * FROM order_tracking WHERE order_id = ? ORDER BY update_time";
	        PreparedStatement pstmt = connection.prepareStatement(query);
	        pstmt.setInt(1, orderId);
	        ResultSet rs = pstmt.executeQuery();
	        
	        while (rs.next()) {
	            OrderTracking track = new OrderTracking(
	                rs.getInt("order_id"),
	                rs.getString("status"),
	                rs.getString("notes")
	            );
	            
	            track.setId(rs.getInt("id"));
	            track.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
	            
	            tracking.add(track);
	        }
	        
	        rs.close();
	        pstmt.close();
	        
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    
	    return tracking;
	}

	public boolean updateOrderStatus(int orderId, String status, String notes) {
	    try {
	        // Update main order status
	        String orderSql = "UPDATE orders SET order_status = ? WHERE id = ?";
	        PreparedStatement orderStmt = connection.prepareStatement(orderSql);
	        orderStmt.setString(1, status);
	        orderStmt.setInt(2, orderId);
	        orderStmt.executeUpdate();
	        orderStmt.close();
	        
	        // Add tracking record
	        String trackingSql = "INSERT INTO order_tracking (order_id, status, notes) VALUES (?, ?, ?)";
	        PreparedStatement trackingStmt = connection.prepareStatement(trackingSql);
	        trackingStmt.setInt(1, orderId);
	        trackingStmt.setString(2, status);
	        trackingStmt.setString(3, notes);
	        trackingStmt.executeUpdate();
	        trackingStmt.close();
	        
	        return true;
	        
	    } catch (SQLException e) {
	        e.printStackTrace();
	        return false;
	    }
	}
	
	



	
	
	// ========== MESSAGE METHODS ==========

	/**
	 * Save a new message from customer to admin
	 * 
	 */
	
	// Get all customers (non-admin users)
	// Get all customers (non-admin users) - Fixed query
// Get all customers (non-admin users)
	
	public User getCustomerById(int customerId) throws SQLException {
	    String query = "SELECT id, username, email FROM users WHERE id = ?";
	    
	    try (PreparedStatement stmt = connection.prepareStatement(query)) {
	        stmt.setInt(1, customerId);
	        ResultSet rs = stmt.executeQuery();
	        
	        if (rs.next()) {
	            User user = new User();
	            user.setId(rs.getInt("id"));
	            user.setUsername(rs.getString("username"));
	            user.setEmail(rs.getString("email"));
	            return user;
	        }
	    }
	    return null;
	}

	// FIXED: Update sendMessageFromAdmin to include customer details
	public boolean sendMessageFromAdmin(int customerId, String subject, String message) throws SQLException {
	    // Get customer details first
	    User customer = getCustomerById(customerId);
	    if (customer == null) {
	        return false;
	    }
	    
	    String query = "INSERT INTO messages (customer_id, customer_name, customer_email, " +
	                   "subject, message, admin_reply, status, replied_at) " +
	                   "VALUES (?, ?, ?, ?, ?, ?, 'REPLIED', NOW())";
	    
	    try (PreparedStatement stmt = connection.prepareStatement(query)) {
	        stmt.setInt(1, customerId);
	        stmt.setString(2, customer.getUsername());
	        stmt.setString(3, customer.getEmail());
	        stmt.setString(4, subject);
	        stmt.setString(5, message);  // Original message (could be empty for admin-initiated)
	        stmt.setString(6, message);  // Admin's reply
	        
	        int rowsAffected = stmt.executeUpdate();
	        return rowsAffected > 0;
	    }
	}

	// FIXED: Get unread message count for customer
	public int getCustomerUnreadMessageCount(int customerId) {
	    try {
	        // FIX: Changed from 'REPLIED' to 'UNREAD'
	        String query = "SELECT COUNT(*) FROM messages WHERE customer_id = ? AND status = 'UNREAD'";
	        
	        PreparedStatement pstmt = connection.prepareStatement(query);
	        pstmt.setInt(1, customerId);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            return rs.getInt(1);
	        }
	        
	        rs.close();
	        pstmt.close();
	        
	    } catch (SQLException e) {
	        System.err.println("Error getting customer unread message count: " + e.getMessage());
	        e.printStackTrace();
	    }
	    
	    return 0;
	}
	
	
public List<User> getAllCustomers() throws SQLException {
    List<User> customers = new ArrayList<>();
    
    // Check your users table structure first
    System.out.println("DEBUG: Getting all customers...");
    
    // Try this query first - gets all users
    String query = "SELECT id, username, email, created_at FROM users ORDER BY username ASC";
    
    try (PreparedStatement stmt = connection.prepareStatement(query);
         ResultSet rs = stmt.executeQuery()) {
        
        int count = 0;
        while (rs.next()) {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                user.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            // Assuming all are customers for now
            user.setRole("CUSTOMER");
            customers.add(user);
            count++;
        }
        System.out.println("DEBUG: Found " + count + " customers");
    }
    return customers;
}

	// Send message from admin to customer
//	public boolean sendMessageFromAdmin(int customerId, String subject, String message) throws SQLException {
//	    String query = "INSERT INTO messages (customer_id, subject, message, status, admin_reply, replied_at) VALUES (?, ?, ?, 'REPLIED', ?, NOW())";
//	    
//	    try (PreparedStatement stmt = connection.prepareStatement(query)) {
//	        stmt.setInt(1, customerId);
//	        stmt.setString(2, subject);
//	        stmt.setString(3, message);
//	        stmt.setString(4, message); // Store the message as admin_reply
//	        
//	        int rowsAffected = stmt.executeUpdate();
//	        return rowsAffected > 0;
//	    }
//	}

	// Delete a message
	public boolean deleteMessage(int messageId) throws SQLException {
	    String query = "DELETE FROM messages WHERE id = ?";
	    
	    try (PreparedStatement stmt = connection.prepareStatement(query)) {
	        stmt.setInt(1, messageId);
	        
	        int rowsAffected = stmt.executeUpdate();
	        return rowsAffected > 0;
	    }
	}
	public boolean saveCustomerMessage(int customerId, String customerName, String customerEmail, 
	                                   String subject, String message) {
	    try {
	        String sql = "INSERT INTO messages (customer_id, customer_name, customer_email, subject, message, status) " +
	                    "VALUES (?, ?, ?, ?, ?, 'UNREAD')";
	        
	        PreparedStatement pstmt = connection.prepareStatement(sql);
	        pstmt.setInt(1, customerId);
	        pstmt.setString(2, customerName);
	        pstmt.setString(3, customerEmail);
	        pstmt.setString(4, subject);
	        pstmt.setString(5, message);
	        
	        int rows = pstmt.executeUpdate();
	        pstmt.close();
	        
	        return rows > 0;
	        
	    } catch (SQLException e) {
	        System.err.println("Error saving message: " + e.getMessage());
	        e.printStackTrace();
	        return false;
	    }
	}

	/**
	 * Get all messages for a specific customer
	 */
	/**
 * Get all messages for a specific customer
 */
public List<Message> getCustomerMessages(int customerId) {
    List<Message> messages = new ArrayList<>();
    
    try {
        String query = "SELECT * FROM messages WHERE customer_id = ? ORDER BY created_at ASC";
        
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, customerId);
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            Message msg = new Message();
            msg.setId(rs.getInt("id"));
            msg.setCustomerId(rs.getInt("customer_id"));
            msg.setCustomerName(rs.getString("customer_name"));
            msg.setCustomerEmail(rs.getString("customer_email"));
            msg.setSubject(rs.getString("subject"));
            msg.setMessage(rs.getString("message"));
            msg.setAdminReply(rs.getString("admin_reply"));
            msg.setStatus(rs.getString("status"));
            
            // FIX: Convert Timestamp to LocalDateTime
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                msg.setCreatedAt(createdAt.toLocalDateTime());  // This is the fix
            }
            
            Timestamp repliedAt = rs.getTimestamp("replied_at");
            if (repliedAt != null) {
                msg.setRepliedAt(repliedAt.toLocalDateTime());  // This is the fix
            }
            
            messages.add(msg);
        }
        
        rs.close();
        pstmt.close();
        
    } catch (SQLException e) {
        System.err.println("Error fetching customer messages: " + e.getMessage());
        e.printStackTrace();
    }
    
    return messages;
}

	/**
	 * Get all messages for admin (all customers)
	 */
	public List<Message> getAllMessages() {
    List<Message> messages = new ArrayList<>();
    
    try {
        String query = "SELECT * FROM messages ORDER BY " +
                      "CASE status WHEN 'UNREAD' THEN 1 WHEN 'READ' THEN 2 ELSE 3 END, " +
                      "created_at DESC";
        
        PreparedStatement pstmt = connection.prepareStatement(query);
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            Message msg = new Message();
            msg.setId(rs.getInt("id"));
            msg.setCustomerId(rs.getInt("customer_id"));
            msg.setCustomerName(rs.getString("customer_name"));
            msg.setCustomerEmail(rs.getString("customer_email"));
            msg.setSubject(rs.getString("subject"));
            msg.setMessage(rs.getString("message"));
            msg.setAdminReply(rs.getString("admin_reply"));
            msg.setStatus(rs.getString("status"));
            
            // FIX: Convert Timestamp to LocalDateTime
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                msg.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            Timestamp repliedAt = rs.getTimestamp("replied_at");
            if (repliedAt != null) {
                msg.setRepliedAt(repliedAt.toLocalDateTime());
            }
            
            messages.add(msg);
        }
        
        rs.close();
        pstmt.close();
        
    } catch (SQLException e) {
        System.err.println("Error fetching all messages: " + e.getMessage());
        e.printStackTrace();
    }
    
    return messages;
}

	/**
	 * Admin replies to a message
	 */
	public boolean adminReplyToMessage(int messageId, String adminReply) {
	    try {
	        String sql = "UPDATE messages SET admin_reply = ?, status = 'REPLIED', " +
	                    "replied_at = CURRENT_TIMESTAMP WHERE id = ?";
	        
	        PreparedStatement pstmt = connection.prepareStatement(sql);
	        pstmt.setString(1, adminReply);
	        pstmt.setInt(2, messageId);
	        
	        int rows = pstmt.executeUpdate();
	        pstmt.close();
	        
	        return rows > 0;
	        
	    } catch (SQLException e) {
	        System.err.println("Error replying to message: " + e.getMessage());
	        e.printStackTrace();
	        return false;
	    }
	}

	/**
	 * Mark message as read
	 */
	public boolean markMessageAsRead(int messageId) {
	    try {
	        String sql = "UPDATE messages SET status = 'READ' WHERE id = ? AND status = 'UNREAD'";
	        
	        PreparedStatement pstmt = connection.prepareStatement(sql);
	        pstmt.setInt(1, messageId);
	        
	        int rows = pstmt.executeUpdate();
	        pstmt.close();
	        
	        return rows > 0;
	        
	    } catch (SQLException e) {
	        System.err.println("Error marking message as read: " + e.getMessage());
	        e.printStackTrace();
	        return false;
	    }
	}

	/**
	 * Get count of unread messages for admin
	 */
	public int getUnreadMessageCount() {
	    try {
	        String query = "SELECT COUNT(*) FROM messages WHERE status = 'UNREAD'";
	        
	        PreparedStatement pstmt = connection.prepareStatement(query);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            return rs.getInt(1);
	        }
	        
	        rs.close();
	        pstmt.close();
	        
	    } catch (SQLException e) {
	        System.err.println("Error getting unread message count: " + e.getMessage());
	        e.printStackTrace();
	    }
	    
	    return 0;
	}

	/**
	 * Get count of unread messages for a specific customer
	 */
//	public int getCustomerUnreadMessageCount(int customerId) {
//	    try {
//	        String query = "SELECT COUNT(*) FROM messages WHERE customer_id = ? AND status = 'REPLIED'";
//	        
//	        PreparedStatement pstmt = connection.prepareStatement(query);
//	        pstmt.setInt(1, customerId);
//	        ResultSet rs = pstmt.executeQuery();
//	        
//	        if (rs.next()) {
//	            return rs.getInt(1);
//	        }
//	        
//	        rs.close();
//	        pstmt.close();
//	        
//	    } catch (SQLException e) {
//	        System.err.println("Error getting customer unread message count: " + e.getMessage());
//	        e.printStackTrace();
//	    }
//	    
//	    return 0;
//	}
	// Add this method to DatabaseService.java to debug messages table
	public void debugMessagesTable() {
	    try {
	        System.out.println("=== DEBUG MESSAGES TABLE ===");
	        
	        // Check if table exists
	        String checkTableSql = "SHOW TABLES LIKE 'messages'";
	        PreparedStatement checkStmt = connection.prepareStatement(checkTableSql);
	        ResultSet tableRs = checkStmt.executeQuery();
	        
	        if (tableRs.next()) {
	            System.out.println("✓ Messages table exists");
	            
	            // Count messages
	            String countSql = "SELECT COUNT(*) as message_count FROM messages";
	            PreparedStatement countStmt = connection.prepareStatement(countSql);
	            ResultSet countRs = countStmt.executeQuery();
	            
	            if (countRs.next()) {
	                int count = countRs.getInt("message_count");
	                System.out.println("Total messages in database: " + count);
	                
	                // Show sample messages
	                if (count > 0) {
	                    System.out.println("Sample messages:");
	                    String sampleSql = "SELECT id, customer_name, subject, status, created_at FROM messages LIMIT 5";
	                    PreparedStatement sampleStmt = connection.prepareStatement(sampleSql);
	                    ResultSet sampleRs = sampleStmt.executeQuery();
	                    
	                    while (sampleRs.next()) {
	                        System.out.println("  ID: " + sampleRs.getInt("id") +
	                                         " | Customer: " + sampleRs.getString("customer_name") +
	                                         " | Subject: " + sampleRs.getString("subject") +
	                                         " | Status: " + sampleRs.getString("status") +
	                                         " | Date: " + sampleRs.getTimestamp("created_at"));
	                    }
	                    sampleRs.close();
	                    sampleStmt.close();
	                }
	            }
	            countRs.close();
	            countStmt.close();
	        } else {
	            System.out.println("✗ Messages table does NOT exist!");
	            
	            // Create the table
	            System.out.println("Creating messages table...");
	            String createTableSql = 
	                "CREATE TABLE messages (" +
	                "id INT PRIMARY KEY AUTO_INCREMENT," +
	                "customer_id INT NOT NULL," +
	                "customer_name VARCHAR(100)," +
	                "customer_email VARCHAR(100)," +
	                "subject VARCHAR(200) NOT NULL," +
	                "message TEXT NOT NULL," +
	                "admin_reply TEXT," +
	                "status VARCHAR(20) DEFAULT 'UNREAD'," +
	                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
	                "replied_at TIMESTAMP NULL," +
	                "FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE" +
	                ")";
	            
	            Statement stmt = connection.createStatement();
	            stmt.execute(createTableSql);
	            stmt.close();
	            System.out.println("✓ Messages table created successfully!");
	            
	            // Insert test data
	            System.out.println("Inserting test messages...");
	            String insertTestSql = 
	                "INSERT INTO messages (customer_id, customer_name, customer_email, subject, message, status) VALUES " +
	                "(2, 'yohan wega', 'jo123@example.com', 'Order Issue', 'My order has not arrived yet. Can you check?', 'UNREAD'), " +
	                "(2, 'yohan wega', 'jo123@example.com', 'Product Quality', 'The product arrived damaged.', 'REPLIED'), " +
	                "(2, 'yohan wega', 'jo123@example.com', 'Payment Problem', 'My payment was declined but money deducted.', 'READ')";
	            
	            stmt = connection.createStatement();
	            stmt.executeUpdate(insertTestSql);
	            stmt.close();
	            System.out.println("✓ Test messages inserted!");
	        }
	        
	        tableRs.close();
	        checkStmt.close();
	        
	        System.out.println("===========================");
	        
	    } catch (SQLException e) {
	        System.err.println("Error debugging messages table: " + e.getMessage());
	        e.printStackTrace();
	    }
	}
    public boolean updateOrderStatus(int orderId, String status) {
        try {
            String sql = "INSERT INTO order_tracking (order_id, status) VALUES (?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, orderId);
            pstmt.setString(2, status);
            
            int rows = pstmt.executeUpdate();
            pstmt.close();
            
            // Update main order status
            String updateSql = "UPDATE orders SET order_status = ? WHERE id = ?";
            PreparedStatement updateStmt = connection.prepareStatement(updateSql);
            updateStmt.setString(1, status);
            updateStmt.setInt(2, orderId);
            updateStmt.executeUpdate();
            updateStmt.close();
            
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    
 // Method to check if user is the only admin
    public boolean isOnlyAdmin(String email) {
        try {
            String query = "SELECT COUNT(*) as admin_count FROM users WHERE role = 'ADMIN'";
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int adminCount = rs.getInt("admin_count");
                return adminCount <= 1; // If only one admin exists
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error checking admin count: " + e.getMessage());
        }
        
        return false;
    }

    // Method to get admin count
    public int getAdminCount() {
        try {
            String query = "SELECT COUNT(*) as count FROM users WHERE role = 'ADMIN'";
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting admin count: " + e.getMessage());
        }
        
        return 0;
    }

    // Method to check if user is primary admin
    public boolean isPrimaryAdmin(String username) {
        return "admin".equals(username); // Your primary admin username
    }
    
 // Add this method to check if user exists by email
    public User getUserByEmail(String email) {
        try {
            String query = "SELECT * FROM users WHERE email = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, email);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setFullName(rs.getString("full_name"));
                user.setRole(rs.getString("role"));
                
                rs.close();
                pstmt.close();
                return user;
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting user by email: " + e.getMessage());
        }
        
        return null;
    }

    // Add this method to register a new user
   
    
    public boolean registerUser(String username, String email, String password, String fullName, String role) {
        try {
            // Hash the password before storing
            String hashedPassword = PasswordHasher.hashPassword(password);
            
            String query = "INSERT INTO users (username, email, password, full_name, role) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, hashedPassword); // Store hashed password
            pstmt.setString(4, fullName);
            pstmt.setString(5, role);
            
            int rows = pstmt.executeUpdate();
            pstmt.close();
            
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
            return false;
        }
    }

    // Add this method to update user role
    public boolean updateUserRole(String email, String newRole) {
        try {
            String query = "UPDATE users SET role = ? WHERE email = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, newRole);
            pstmt.setString(2, email);
            
            int rows = pstmt.executeUpdate();
            pstmt.close();
            
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating user role: " + e.getMessage());
            return false;
        }
    }

    // Add this method to check if username exists
    public boolean usernameExists(String username) {
        try {
            String query = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, username);
            
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            
            rs.close();
            pstmt.close();
            
            return count > 0;
            
        } catch (SQLException e) {
            System.err.println("Error checking username: " + e.getMessage());
            return false;
        }
    }

    // Add this method to get all users (for admin view)
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        
        try {
            String query = "SELECT id, username, email, full_name, role, created_at FROM users ORDER BY created_at DESC";
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setFullName(rs.getString("full_name"));
                user.setRole(rs.getString("role"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                users.add(user);
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error fetching all users: " + e.getMessage());
        }
        
        return users;
    }

    // Message methods
    public boolean saveMessage(int customerId, String subject, String message) {
        try {
            String sql = "INSERT INTO messages (customer_id, subject, message) VALUES (?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, customerId);
            pstmt.setString(2, subject);
            pstmt.setString(3, message);
            
            int rows = pstmt.executeUpdate();
            pstmt.close();
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String generateOrderNumber() {
        return "ORD" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    private void updateProductStock(int productId, int quantityChange) {
        try {
            String sql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, quantityChange);
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public boolean validateCustomer(String username, String password) {
    try {
        String query = "SELECT password FROM users WHERE (username = ? OR email = ?) AND role = 'CUSTOMER'";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, username);
        pstmt.setString(2, username);
        
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            String storedHash = rs.getString("password");
            // Verify password against stored hash
            boolean isValid = PasswordHasher.verifyPassword(password, storedHash);
            
            rs.close();
            pstmt.close();
            return isValid;
        }
        
        rs.close();
        pstmt.close();
        return false;
        
    } catch (SQLException e) {
        System.err.println("Error validating customer: " + e.getMessage());
        return false;
    }
}
 // Add these methods to get customer data:
 // Add these methods to your DatabaseService class

    /**
     * Validate login credentials (works with email or username)
     */
   

    /**
     * Get user by email or username
     */
    public User getUserByEmailOrUsername(String emailOrUsername) {
        try {
            String query = "SELECT id, email, username, full_name, role, phone, address " +
                          "FROM users WHERE email = ? OR username = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, emailOrUsername);
            pstmt.setString(2, emailOrUsername);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setEmail(rs.getString("email"));
                user.setUsername(rs.getString("username"));
                user.setFullName(rs.getString("full_name"));
                user.setRole(rs.getString("role"));
                user.setPhone(rs.getString("phone"));
                user.setAddress(rs.getString("address"));
                
                rs.close();
                pstmt.close();
                return user;
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting user: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Check if email already exists
     */
    public boolean checkEmailExists(String email) {
        try {
            String query = "SELECT COUNT(*) as count FROM users WHERE email = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, email);
            
            ResultSet rs = pstmt.executeQuery();
            boolean exists = false;
            
            if (rs.next()) {
                exists = rs.getInt("count") > 0;
            }
            
            rs.close();
            pstmt.close();
            
            return exists;
            
        } catch (SQLException e) {
            System.err.println("Error checking email: " + e.getMessage());
            return false;
        }
    }

    /**
     * Register new customer with all required fields
     */
    public boolean registerCustomer(String email, String password, String fullName, 
                                   String phone, String address, boolean newsletter) {
        try {
            // Generate username from email (remove @domain part)
            String username = email.split("@")[0];
            
            String query = "INSERT INTO users (username, email, password, full_name, " +
                          "phone, address, role, newsletter_subscribed, created_at) " +
                          "VALUES (?, ?, ?, ?, ?, ?, 'CUSTOMER', ?, CURRENT_TIMESTAMP)";
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, fullName);
            pstmt.setString(5, phone);
            pstmt.setString(6, address);
            pstmt.setBoolean(7, newsletter);
            
            int rows = pstmt.executeUpdate();
            pstmt.close();
            
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error registering customer: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create users table with all required fields (if not exists)
     * Add this to your initializeDatabase() method
     */
    private void createUsersTable() {
        try {
            String createTableSQL = 
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "username VARCHAR(50) UNIQUE NOT NULL," +
                "email VARCHAR(100) UNIQUE NOT NULL," +
                "password VARCHAR(100) NOT NULL," +
                "full_name VARCHAR(100) NOT NULL," +
                "phone VARCHAR(20)," +
                "address TEXT," +
                "role VARCHAR(20) DEFAULT 'CUSTOMER'," +
                "newsletter_subscribed BOOLEAN DEFAULT false," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
            
            Statement stmt = connection.createStatement();
            stmt.execute(createTableSQL);
            stmt.close();
            
            System.out.println("Users table created/verified");
            
        } catch (SQLException e) {
            System.err.println("Error creating users table: " + e.getMessage());
        }
    }
    public boolean customerLogin(String username, String password) {
    try {
        String query = "SELECT password FROM users WHERE (username = ? OR email = ?) AND role = 'CUSTOMER'";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, username);
        pstmt.setString(2, username);
        
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            String storedHash = rs.getString("password");
            // VERIFY AGAINST HASHED PASSWORD
            boolean isValid = PasswordHasher.verifyPassword(password, storedHash);
            
            rs.close();
            pstmt.close();
            return isValid;
        }
        
        rs.close();
        pstmt.close();
        return false;
        
    } catch (SQLException e) {
        System.err.println("Error in customer login: " + e.getMessage());
        return false;
    }
}

    public String getCustomerName(String username) {
        try {
            String query = "SELECT full_name FROM users WHERE username = ? OR email = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("full_name");
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting customer name: " + e.getMessage());
        }
        
        return "Customer";
    }
public boolean registerCustomer(String username, String email, String password, String fullName) {
    try {
        // MAKE SURE THIS LINE HASHES THE PASSWORD
        String hashedPassword = PasswordHasher.hashPassword(password);
        
        System.out.println("DEBUG: Registering user - Password hashed: " + 
            (hashedPassword != null && hashedPassword.length() > 20));
        
        String query = "INSERT INTO users (username, email, password, role, full_name) VALUES (?, ?, ?, 'CUSTOMER', ?)";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, username);
        pstmt.setString(2, email);
        pstmt.setString(3, hashedPassword); // Make sure this is hashed
        pstmt.setString(4, fullName);
        
        int rows = pstmt.executeUpdate();
        pstmt.close();
        
        return rows > 0;
        
    } catch (SQLException e) {
        System.err.println("Error registering customer: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}
    
    public boolean addProduct(Product product) {
        try {
            String query = "INSERT INTO products (name, price, category_id, description, " +
                          "image_url, stock_quantity, discount_percentage) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, product.getName());
            pstmt.setDouble(2, product.getPrice());
            pstmt.setInt(3, product.getCategoryId());
            pstmt.setString(4, product.getDescription());
            pstmt.setString(5, product.getImageUrl());
            pstmt.setInt(6, product.getStockQuantity());
            pstmt.setDouble(7, product.getDiscountPercentage());
            
            int rows = pstmt.executeUpdate();
            pstmt.close();
            
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error adding product: " + e.getMessage());
            return false;
        }
    }
    
    public boolean updateProduct(Product product) {
        try {
            String query = "UPDATE products SET name = ?, price = ?, category_id = ?, " +
                          "description = ?, image_url = ?, stock_quantity = ?, " +
                          "discount_percentage = ? WHERE id = ?";
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, product.getName());
            pstmt.setDouble(2, product.getPrice());
            pstmt.setInt(3, product.getCategoryId());
            pstmt.setString(4, product.getDescription());
            pstmt.setString(5, product.getImageUrl());
            pstmt.setInt(6, product.getStockQuantity());
            pstmt.setDouble(7, product.getDiscountPercentage());
            pstmt.setInt(8, product.getId());
            
            int rows = pstmt.executeUpdate();
            pstmt.close();
            
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating product: " + e.getMessage());
            return false;
        }
    }
    
    public boolean deleteProduct(int productId) {
        try {
            String query = "DELETE FROM products WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, productId);
            
            int rows = pstmt.executeUpdate();
            pstmt.close();
            
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting product: " + e.getMessage());
            return false;
        }
    }
    
    // CATEGORY METHODS
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        
        try {
            String query = "SELECT * FROM categories ORDER BY name";
            PreparedStatement pstmt = connection.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Category category = new Category(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description")
                );
                categories.add(category);
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error fetching categories: " + e.getMessage());
        }
        
        return categories;
    }
    
    public boolean addCategory(Category category) {
        try {
            String query = "INSERT INTO categories (name, description) VALUES (?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            
            int rows = pstmt.executeUpdate();
            pstmt.close();
            
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error adding category: " + e.getMessage());
            return false;
        }
    }
    
    public boolean updateCategory(Category category) {
        try {
            String query = "UPDATE categories SET name = ?, description = ? WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            pstmt.setInt(3, category.getId());
            
            int rows = pstmt.executeUpdate();
            pstmt.close();
            
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating category: " + e.getMessage());
            return false;
        }
    }
    
    public boolean deleteCategory(int categoryId) {
        try {
            // Note: This will fail if products reference this category due to foreign key
            String query = "DELETE FROM categories WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, categoryId);
            
            int rows = pstmt.executeUpdate();
            pstmt.close();
            
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting category: " + e.getMessage());
            return false;
        }
    }
    
    // USER/AUTH METHODS
    public boolean validateAdmin(String username, String password) {
    try {
        String query = "SELECT password FROM users WHERE username = ? AND role = 'ADMIN'";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, username);
        
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            String storedHash = rs.getString("password");
            // Verify password against stored hash
            boolean isValid = PasswordHasher.verifyPassword(password, storedHash);
            
            rs.close();
            pstmt.close();
            return isValid;
        }
        
        rs.close();
        pstmt.close();
        return false;
        
    } catch (SQLException e) {
        System.err.println("Error validating admin: " + e.getMessage());
        return false;
    }
}
public boolean validateLogin(String emailOrUsername, String password) {
    try {
        String query = "SELECT password FROM users WHERE (email = ? OR username = ?)";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, emailOrUsername);
        pstmt.setString(2, emailOrUsername);
        
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            String storedHash = rs.getString("password");
            // VERIFY AGAINST HASHED PASSWORD
            boolean isValid = PasswordHasher.verifyPassword(password, storedHash);
            
            rs.close();
            pstmt.close();
            return isValid;
        }
        
        rs.close();
        pstmt.close();
        return false;
        
    } catch (SQLException e) {
        System.err.println("Error validating login: " + e.getMessage());
        return false;
    }
}
	
	public User getUserWithPasswordCheck(String username, String password) {
	    try {
	        String query = "SELECT * FROM users WHERE (username = ? OR email = ?)";
	        PreparedStatement pstmt = connection.prepareStatement(query);
	        pstmt.setString(1, username);
	        pstmt.setString(2, username);
	        
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            String storedHash = rs.getString("password");
	            
	            // Verify password
	            if (PasswordHasher.verifyPassword(password, storedHash)) {
	                User user = new User();
	                user.setId(rs.getInt("id"));
	                user.setUsername(rs.getString("username"));
	                user.setEmail(rs.getString("email"));
	                user.setFullName(rs.getString("full_name"));
	                user.setRole(rs.getString("role"));
	                
	                rs.close();
	                pstmt.close();
	                return user;
	            }
	        }
	        
	        rs.close();
	        pstmt.close();
	        
	    } catch (SQLException e) {
	        System.err.println("Error getting user: " + e.getMessage());
	    }
	    
	    return null;
	}
	public boolean updateUserPassword(int userId, String newPassword) {
	    try {
	        // Hash the new password
	        String hashedPassword = PasswordHasher.hashPassword(newPassword);
	        
	        String query = "UPDATE users SET password = ? WHERE id = ?";
	        PreparedStatement pstmt = connection.prepareStatement(query);
	        pstmt.setString(1, hashedPassword);
	        pstmt.setInt(2, userId);
	        
	        int rows = pstmt.executeUpdate();
	        pstmt.close();
	        
	        return rows > 0;
	        
	    } catch (SQLException e) {
	        System.err.println("Error updating password: " + e.getMessage());
	        return false;
	    }
	}
    
    
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}