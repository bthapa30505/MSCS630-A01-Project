package org.shellassignment.auth;

public class User {
    private String username;
    private String password;
    private UserRole role;
    private boolean isActive;

    public enum UserRole {
        ADMIN("admin"),
        STANDARD("standard");

        private final String roleName;

        UserRole(String roleName) {
            this.roleName = roleName;
        }

        public String getRoleName() {
            return roleName;
        }
    }

    public User(String username, String password, UserRole role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.isActive = true;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", role=" + role.getRoleName() +
                ", active=" + isActive +
                '}';
    }
} 