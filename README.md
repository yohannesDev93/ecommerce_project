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
