# 🏪 Java E-Commerce Desktop Application

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-005C84?style=for-the-badge&logo=mysql&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop-App-00C7B7?style=for-the-badge)

> **A full-stack desktop e-commerce solution** with complete order management, admin dashboard, and real-time inventory control

## 📋 Overview

This is a **fully functional** desktop e-commerce application built with Java and MySQL. It provides a complete shopping experience for customers and a powerful administration panel for store management. The application handles everything from user authentication to order processing with real-time inventory updates.

## ✨ Key Features

### 👤 User Features
- **User Authentication** - Secure login and registration system
- **Product Browsing** - View products with details, prices, and availability
- **Shopping Cart** - Add/remove items, update quantities
- **Order Management** - Place orders and view order history
- **User Profile** - Manage personal information

### 👑 Admin Dashboard Features
- **Dashboard Analytics** - Overview of sales, orders, and user statistics
- **Product Management** 
  - ➕ Add new products with images and details
  - ✏️ Edit existing product information
  - 🗑️ Remove products from inventory
  - 🔍 Search and filter products
  
- **User Management**
  - 👥 View all registered users
  - ✏️ Edit user details and roles
  - 🗑️ Remove users from system
  - 🔒 Manage user permissions

- **Order Management**
  - 📦 View all orders across users
  - 🔍 Filter orders by status, date, or user
  - 👀 Track individual user order history
  - 📊 Generate order reports

### 🛒 Shopping Features
- **Product Catalog** - Categorized product listing
- **Search Functionality** - Find products by name or category
- **Stock Management** - Real-time inventory updates
- **Order Processing** - Seamless checkout experience

## 🏗️ Architecture
┌─────────────────────────────────────┐


│ Presentation Layer │
│ (Java Swing/JavaFX UI) │


├─────────────────────────────────────┤


│ Business Logic Layer │
│ (Services, Validators, Helpers) │


├─────────────────────────────────────┤


│ Data Access Layer │
│ (DAO Pattern, JDBC) │


├─────────────────────────────────────┤


│ Database Layer │
│ (MySQL Server) │

└─────────────────────────────────────┘


## 💻 Technology Stack

| Component | Technology |
|-----------|------------|
| **Frontend** | Java Swing / JavaFX |
| **Backend** | Core Java (JDK 8+) |
| **Database** | MySQL |
| **Connectivity** | JDBC |
| **Build Tool** | Maven / Gradle |
| **IDE Support** | Eclipse, IntelliJ IDEA, NetBeans |

## 📊 Database Schema

The application uses a normalized database structure with the following main tables:
- `users` - Customer and admin information
- `products` - Product catalog with pricing and stock
- `orders` - Order header information
- `order_items` - Line items for each order
- `categories` - Product categorization
- `cart` - Temporary shopping cart storage

## 🚀 Installation Guide

### Prerequisites
- ✅ Java JDK 8 or higher
- ✅ MySQL Server 5.7 or higher
- ✅ MySQL Connector/J (included in lib/)
- ✅ Any Java IDE (optional)

### Step-by-Step Setup

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/ecommerce-desktop-app.git
cd ecommerce-desktop-app 
```

# 📖 How to Use the E-Commerce Desktop Application

## 🔧 Initial Setup

### Database Configuration
Before running the application, you need to configure your database connection:

1. **Locate the Database Configuration File**
   - Navigate to: `src/database/Database.java`
   
2. **Update Your Database Credentials**
   ```java
   // Replace these values with your actual MySQL database details
   private static final String URL = "jdbc:mysql://localhost:3306/your_database_name";
   private static final String USERNAME = "your_username";
   private static final String PASSWORD = "your_password";
   ```

3. **Create the Database**
   - Open MySQL and create a new database with the name you specified in URL
   
   ```sql
   CREATE DATABASE your_database_name;
   ```

## 🚀 Running the Application

### Start the Application
- Run the `Main.java` or `ECommerceApp.java` file
- The login screen will appear

### Login or Register
- **New User**: Click "Register" and fill in your details
- **Existing User**: Enter your email and password
- **Admin Access**: Use admin credentials (if pre-configured)

---

## 👤 Customer Functionality (For Regular Users)

### 1. Browse Products
- View all available products in the catalog
- See product details: name, price, description, stock status
- Filter products by category or search by name

### 2. Shopping Cart
- **Add to Cart**: Click "Add to Cart" on any product
- **Update Quantity**: Change the number of items
- **Remove Items**: Delete products from your cart
- **View Cart**: See all selected items and total price

### 3. Place Orders
- **Checkout**: Proceed to payment from your cart
- **Confirm Order**: Review items and confirm purchase
- **Order Success**: Get order confirmation with ID

### 4. Order History
- View all your past orders
- Check order status (Processing, Shipped, Delivered)
- See order details and total amount

### 5. Profile Management
- Update your personal information
- Change password
- View account details

---

## 👑 Admin Functionality (Dashboard Access)

### 1. Dashboard Overview
- View statistics: total sales, total orders, total users
- See recent activities and alerts
- Monitor low stock products

### 2. Product Management

#### ➕ Add New Product
```
1. Click "Products" → "Add New Product"
2. Fill in product details:
   - Product Name
   - Description
   - Price
   - Stock Quantity
   - Category
   - Image (if applicable)
3. Click "Save" to add to inventory
```

#### ✏️ Edit Existing Product
```
1. Find product in product list
2. Click "Edit" button
3. Modify any details:
   - Name
   - Description
   - Price
   - Stock Quantity
   - Category
4. Click "Update" to save changes
```

#### 🗑️ Remove Product
```
1. Locate product in list
2. Click "Delete" button
3. Confirm deletion
```

### 3. User Management

#### 👥 View All Users
```
- Click "Users" section
- See complete list of registered customers
- View user details: name, email, join date
```

#### ✏️ Edit User Details
```
1. Select a user from the list
2. Click "Edit User"
3. Update information:
   - Name
   - Email
   - User role (Customer/Admin)
   - Account status (Active/Inactive)
4. Click "Save Changes"
```

#### 🗑️ Remove User
```
1. Find user in the list
2. Click "Delete User"
3. Confirm removal
```

### 4. Order Management

#### 📦 View All Orders
```
- Click "Orders" section
- See complete list of all customer orders
- View order details: ID, customer name, date, total, status
```

#### 🔍 Filter Orders by User
```
1. Select a specific user from user list
2. Click "View User Orders"
3. See complete order history for that customer
```

#### 📝 Update Order Status
```
1. Select an order from the list
2. Click "Update Status"
3. Choose new status:
   - Pending
   - Processing
   - Shipped
   - Delivered
   - Cancelled
4. Click "Save" to confirm changes
```

---

## 📊 Quick Reference

| Feature | Customer | Admin |
|---------|----------|-------|
| Browse Products | ✅ | ✅ |
| Shopping Cart | ✅ | ❌ |
| Place Orders | ✅ | ❌ |
| Order History | ✅ | ✅ |
| Profile Management | ✅ | ✅ |
| Dashboard | ❌ | ✅ |
| Add/Edit/Remove Products | ❌ | ✅ |
| User Management | ❌ | ✅ |
| View All Orders | ❌ | ✅ |

---

## ⚙️ Database Schema

The application uses the following main tables:
- `users` - Stores customer and admin information
- `products` - Product catalog with pricing and stock
- `orders` - Order header information
- `order_items` - Individual items in each order
- `categories` - Product categories
- `cart` - Temporary shopping cart storage

---

## 🔧 Troubleshooting

### Common Issues and Solutions

| Issue | Solution |
|-------|----------|
| Cannot connect to database | Check MySQL is running and credentials in DatabaseConfig.java |
| Login fails | Verify email and password, check if account is active |
| Products not showing | Ensure database has products and connection is working |
| Admin menu not visible | Login with admin credentials only |

---

## 📝 Tips for Best Experience

1. **First-time users**: Always register before attempting to login
2. **Admins**: Backup your database regularly
3. **Customers**: Check order status in "My Orders" section
4. **Everyone**: Logout after each session for security
5. **Database**: Keep your MySQL server running while using the app

---

## 🆘 Need Help?

If you encounter any issues:
1. Check your database connection settings
2. Verify MySQL server is running
3. Ensure all Java dependencies are properly installed
4. Contact the system administrator for account-related issues

---

*Happy Shopping! 🛍️*
