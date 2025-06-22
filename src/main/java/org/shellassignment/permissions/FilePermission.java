package org.shellassignment.permissions;

import java.util.EnumSet;
import java.util.Set;

public class FilePermission {
    public enum Permission {
        READ("r"),
        WRITE("w"),
        EXECUTE("x");

        private final String symbol;

        Permission(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    private final String owner;
    private final Set<Permission> ownerPermissions;
    private final Set<Permission> groupPermissions;
    private final Set<Permission> otherPermissions;
    private final String group;

    public FilePermission(String owner, String group) {
        this.owner = owner;
        this.group = group;
        this.ownerPermissions = EnumSet.of(Permission.READ, Permission.WRITE);
        this.groupPermissions = EnumSet.of(Permission.READ);
        this.otherPermissions = EnumSet.of(Permission.READ);
    }

    public FilePermission(String owner, String group, Set<Permission> ownerPerms, 
                         Set<Permission> groupPerms, Set<Permission> otherPerms) {
        this.owner = owner;
        this.group = group;
        this.ownerPermissions = EnumSet.copyOf(ownerPerms);
        this.groupPermissions = EnumSet.copyOf(groupPerms);
        this.otherPermissions = EnumSet.copyOf(otherPerms);
    }

    public boolean canRead(String username, String userGroup) {
        if (owner.equals(username)) {
            return ownerPermissions.contains(Permission.READ);
        } else if (group.equals(userGroup)) {
            return groupPermissions.contains(Permission.READ);
        } else {
            return otherPermissions.contains(Permission.READ);
        }
    }

    public boolean canWrite(String username, String userGroup) {
        if (owner.equals(username)) {
            return ownerPermissions.contains(Permission.WRITE);
        } else if (group.equals(userGroup)) {
            return groupPermissions.contains(Permission.WRITE);
        } else {
            return otherPermissions.contains(Permission.WRITE);
        }
    }

    public boolean canExecute(String username, String userGroup) {
        if (owner.equals(username)) {
            return ownerPermissions.contains(Permission.EXECUTE);
        } else if (group.equals(userGroup)) {
            return groupPermissions.contains(Permission.EXECUTE);
        } else {
            return otherPermissions.contains(Permission.EXECUTE);
        }
    }

    public void setOwnerPermissions(Set<Permission> permissions) {
        this.ownerPermissions.clear();
        this.ownerPermissions.addAll(permissions);
    }

    public void setGroupPermissions(Set<Permission> permissions) {
        this.groupPermissions.clear();
        this.groupPermissions.addAll(permissions);
    }

    public void setOtherPermissions(Set<Permission> permissions) {
        this.otherPermissions.clear();
        this.otherPermissions.addAll(permissions);
    }

    public String getOwner() {
        return owner;
    }

    public String getGroup() {
        return group;
    }

    public Set<Permission> getOwnerPermissions() {
        return EnumSet.copyOf(ownerPermissions);
    }

    public Set<Permission> getGroupPermissions() {
        return EnumSet.copyOf(groupPermissions);
    }

    public Set<Permission> getOtherPermissions() {
        return EnumSet.copyOf(otherPermissions);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        // Owner permissions
        sb.append(ownerPermissions.contains(Permission.READ) ? "r" : "-");
        sb.append(ownerPermissions.contains(Permission.WRITE) ? "w" : "-");
        sb.append(ownerPermissions.contains(Permission.EXECUTE) ? "x" : "-");
        
        // Group permissions
        sb.append(groupPermissions.contains(Permission.READ) ? "r" : "-");
        sb.append(groupPermissions.contains(Permission.WRITE) ? "w" : "-");
        sb.append(groupPermissions.contains(Permission.EXECUTE) ? "x" : "-");
        
        // Other permissions
        sb.append(otherPermissions.contains(Permission.READ) ? "r" : "-");
        sb.append(otherPermissions.contains(Permission.WRITE) ? "w" : "-");
        sb.append(otherPermissions.contains(Permission.EXECUTE) ? "x" : "-");
        
        return sb.toString();
    }

    public String toDetailedString() {
        return String.format("%s %s:%s", toString(), owner, group);
    }
} 