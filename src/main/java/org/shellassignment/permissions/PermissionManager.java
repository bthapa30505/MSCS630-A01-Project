package org.shellassignment.permissions;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.EnumSet;

public class PermissionManager {
    private final Map<String, FilePermission> filePermissions;
    private final Map<String, String> userGroups; // username -> group mapping

    public PermissionManager() {
        this.filePermissions = new HashMap<>();
        this.userGroups = new HashMap<>();
        initializeDefaultGroups();
    }

    private void initializeDefaultGroups() {
        // Default group assignments
        userGroups.put("admin", "admin");
        userGroups.put("root", "admin");
    }

    public void setUserGroup(String username, String group) {
        userGroups.put(username, group);
    }

    public String getUserGroup(String username) {
        return userGroups.getOrDefault(username, "users");
    }

    public FilePermission getFilePermission(String filePath) {
        return filePermissions.get(filePath);
    }

    public void setFilePermission(String filePath, FilePermission permission) {
        filePermissions.put(filePath, permission);
    }

    public FilePermission createDefaultPermission(String owner) {
        String group = getUserGroup(owner);
        return new FilePermission(owner, group);
    }

    public FilePermission createSystemFilePermission() {
        // System files have restricted permissions
        Set<FilePermission.Permission> ownerPerms = EnumSet.of(FilePermission.Permission.READ, FilePermission.Permission.WRITE);
        Set<FilePermission.Permission> groupPerms = EnumSet.of(FilePermission.Permission.READ);
        Set<FilePermission.Permission> otherPerms = EnumSet.of(FilePermission.Permission.READ);
        
        return new FilePermission("admin", "admin", ownerPerms, groupPerms, otherPerms);
    }

    public FilePermission createExecutablePermission(String owner) {
        String group = getUserGroup(owner);
        Set<FilePermission.Permission> ownerPerms = EnumSet.of(FilePermission.Permission.READ, FilePermission.Permission.WRITE, FilePermission.Permission.EXECUTE);
        Set<FilePermission.Permission> groupPerms = EnumSet.of(FilePermission.Permission.READ, FilePermission.Permission.EXECUTE);
        Set<FilePermission.Permission> otherPerms = EnumSet.of(FilePermission.Permission.READ, FilePermission.Permission.EXECUTE);
        
        return new FilePermission(owner, group, ownerPerms, groupPerms, otherPerms);
    }

    public boolean canRead(String filePath, String username) {
        FilePermission permission = filePermissions.get(filePath);
        if (permission == null) {
            // Default permission if not set
            permission = createDefaultPermission("admin");
            filePermissions.put(filePath, permission);
        }
        
        String userGroup = getUserGroup(username);
        return permission.canRead(username, userGroup);
    }

    public boolean canWrite(String filePath, String username) {
        FilePermission permission = filePermissions.get(filePath);
        if (permission == null) {
            // Default permission if not set
            permission = createDefaultPermission("admin");
            filePermissions.put(filePath, permission);
        }
        
        String userGroup = getUserGroup(username);
        return permission.canWrite(username, userGroup);
    }

    public boolean canExecute(String filePath, String username) {
        FilePermission permission = filePermissions.get(filePath);
        if (permission == null) {
            // Default permission if not set
            permission = createDefaultPermission("admin");
            filePermissions.put(filePath, permission);
        }
        
        String userGroup = getUserGroup(username);
        return permission.canExecute(username, userGroup);
    }

    public void setFilePermissions(String filePath, String owner, String group, 
                                 Set<FilePermission.Permission> ownerPerms,
                                 Set<FilePermission.Permission> groupPerms,
                                 Set<FilePermission.Permission> otherPerms) {
        FilePermission permission = new FilePermission(owner, group, ownerPerms, groupPerms, otherPerms);
        filePermissions.put(filePath, permission);
    }

    public void chmod(String filePath, String mode, String username) {
        FilePermission permission = filePermissions.get(filePath);
        if (permission == null) {
            System.err.println("Error: File permission not found for " + filePath);
            return;
        }

        // Check if user can modify permissions (only owner or admin can)
        if (!permission.getOwner().equals(username) && !"admin".equals(username)) {
            System.err.println("Error: Permission denied. Only owner or admin can change file permissions.");
            return;
        }

        if (mode.length() != 9) {
            System.err.println("Error: Invalid permission mode. Use format: rwxrwxrwx");
            return;
        }

        // Parse owner permissions
        Set<FilePermission.Permission> ownerPerms = parsePermissions(mode.substring(0, 3));
        Set<FilePermission.Permission> groupPerms = parsePermissions(mode.substring(3, 6));
        Set<FilePermission.Permission> otherPerms = parsePermissions(mode.substring(6, 9));

        permission.setOwnerPermissions(ownerPerms);
        permission.setGroupPermissions(groupPerms);
        permission.setOtherPermissions(otherPerms);

        System.out.println("Permissions changed for " + filePath + ": " + permission.toString());
    }

    private Set<FilePermission.Permission> parsePermissions(String permString) {
        Set<FilePermission.Permission> permissions = EnumSet.noneOf(FilePermission.Permission.class);
        
        if (permString.charAt(0) == 'r') permissions.add(FilePermission.Permission.READ);
        if (permString.charAt(1) == 'w') permissions.add(FilePermission.Permission.WRITE);
        if (permString.charAt(2) == 'x') permissions.add(FilePermission.Permission.EXECUTE);
        
        return permissions;
    }

    public void chown(String filePath, String newOwner, String username) {
        FilePermission permission = filePermissions.get(filePath);
        if (permission == null) {
            System.err.println("Error: File permission not found for " + filePath);
            return;
        }

        // Only admin can change ownership
        if (!"admin".equals(username)) {
            System.err.println("Error: Permission denied. Only admin can change file ownership.");
            return;
        }

        // Create new permission with new owner
        FilePermission newPermission = new FilePermission(newOwner, permission.getGroup(),
                permission.getOwnerPermissions(), permission.getGroupPermissions(), permission.getOtherPermissions());
        
        filePermissions.put(filePath, newPermission);
        System.out.println("Ownership changed for " + filePath + " to " + newOwner);
    }

    public void lsPermissions(String filePath) {
        FilePermission permission = filePermissions.get(filePath);
        if (permission == null) {
            System.out.println("No permission information available for " + filePath);
            return;
        }

        System.out.println(permission.toDetailedString() + " " + filePath);
    }

    public void listAllPermissions() {
        System.out.println("=== File Permissions ===");
        for (Map.Entry<String, FilePermission> entry : filePermissions.entrySet()) {
            System.out.println(entry.getValue().toDetailedString() + " " + entry.getKey());
        }
    }
} 