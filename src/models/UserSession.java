package models;

public class UserSession {
    private static UserSession instance;
    private static int userId=0;
    private String username;
    private String email;
    private String fullName;
    private String role;
    
    private UserSession() {
        // Private constructor for singleton
    }
    
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }
    
    // Initialize session with user data
    public void initSession(int userId, String username, String email, String fullName, String role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }
    
    // Clear session (logout)
    public void clearSession() {
        this.userId = 0;
        this.username = null;
        this.email = null;
        this.fullName = null;
        this.role = null;
    }
    
    // Getters
    public static  int getUserId() { 
		return userId;
	 }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    
    // Check if user is logged in
    public boolean isLoggedIn() { 
        return userId > 0 && username != null; 
    }
    
    // Check if user is customer
    public boolean isCustomer() {
        return "CUSTOMER".equals(role);
    }
    
    // Check if user is admin
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}