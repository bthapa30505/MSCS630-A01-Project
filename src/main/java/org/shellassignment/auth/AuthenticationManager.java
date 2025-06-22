package org.shellassignment.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class AuthenticationManager {
    private final Map<String, User> users;
    private User currentUser;
    private final Scanner scanner;

    public AuthenticationManager() {
        this.users = new HashMap<>();
        this.scanner = new Scanner(System.in);
        initializeDefaultUsers();
    }

    private void initializeDefaultUsers() {
        // Create default admin user
        User adminUser = new User("admin", "1234", User.UserRole.ADMIN);
        users.put("admin", adminUser);
    }

    public boolean login() {
        System.out.println("=== Shell Authentication ===");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        User user = users.get(username);
        if (user != null && user.getPassword().equals(password) && user.isActive()) {
            currentUser = user;
            System.out.println("Welcome, " + username + " (" + user.getRole().getRoleName() + ")!");
            return true;
        } else {
            System.out.println("Invalid username or password!");
            return false;
        }
    }

    public void logout() {
        if (currentUser != null) {
            System.out.println("Goodbye, " + currentUser.getUsername() + "!");
            currentUser = null;
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public boolean createUser(String username, String password, User.UserRole role) {
        if (!isAdmin()) {
            System.out.println("Error: Only admin users can create new users.");
            return false;
        }

        if (username == null || username.trim().isEmpty()) {
            System.out.println("Error: Username cannot be empty.");
            return false;
        }

        if (password == null || password.trim().isEmpty()) {
            System.out.println("Error: Password cannot be empty.");
            return false;
        }

        if (users.containsKey(username)) {
            System.out.println("Error: User '" + username + "' already exists.");
            return false;
        }

        User newUser = new User(username.trim(), password.trim(), role);
        users.put(username.trim(), newUser);
        System.out.println("User '" + username + "' created successfully with role: " + role.getRoleName());
        return true;
    }

    public boolean deleteUser(String username) {
        if (!isAdmin()) {
            System.out.println("Error: Only admin users can delete users.");
            return false;
        }

        if (username == null || username.trim().isEmpty()) {
            System.out.println("Error: Username cannot be empty.");
            return false;
        }

        if (!users.containsKey(username)) {
            System.out.println("Error: User '" + username + "' does not exist.");
            return false;
        }

        if (username.equals(currentUser.getUsername())) {
            System.out.println("Error: Cannot delete your own account.");
            return false;
        }

        User deletedUser = users.remove(username);
        System.out.println("User '" + username + "' deleted successfully.");
        return true;
    }

    public boolean changePassword(String username, String newPassword) {
        if (!isAdmin() && !username.equals(currentUser.getUsername())) {
            System.out.println("Error: You can only change your own password.");
            return false;
        }

        if (username == null || username.trim().isEmpty()) {
            System.out.println("Error: Username cannot be empty.");
            return false;
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            System.out.println("Error: Password cannot be empty.");
            return false;
        }

        User user = users.get(username);
        if (user == null) {
            System.out.println("Error: User '" + username + "' does not exist.");
            return false;
        }

        user.setPassword(newPassword.trim());
        System.out.println("Password for user '" + username + "' changed successfully.");
        return true;
    }

    public void listUsers() {
        if (!isAdmin()) {
            System.out.println("Error: Only admin users can list users.");
            return;
        }

        System.out.println("=== User List ===");
        System.out.printf("%-15s %-10s %-8s%n", "Username", "Role", "Status");
        System.out.println("--------------------------------");
        
        for (User user : users.values()) {
            String status = user.isActive() ? "Active" : "Inactive";
            System.out.printf("%-15s %-10s %-8s%n", 
                user.getUsername(), 
                user.getRole().getRoleName(), 
                status);
        }
    }

    public void showCurrentUser() {
        if (currentUser != null) {
            System.out.println("Current User: " + currentUser.getUsername());
            System.out.println("Role: " + currentUser.getRole().getRoleName());
            System.out.println("Status: " + (currentUser.isActive() ? "Active" : "Inactive"));
        } else {
            System.out.println("No user currently logged in.");
        }
    }
} 