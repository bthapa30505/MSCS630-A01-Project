package org.shellassignment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.shellassignment.sync.Mutex;
import org.shellassignment.sync.Semaphore;
import org.shellassignment.sync.ProducerConsumer;
import org.shellassignment.memory.MemoryManager;
import org.shellassignment.memory.Page;
import org.shellassignment.memory.PageReplacementAlgorithm;
import org.shellassignment.auth.AuthenticationManager;
import org.shellassignment.auth.User;
import org.shellassignment.permissions.PermissionManager;
import org.shellassignment.permissions.FilePermission;

import java.util.concurrent.atomic.AtomicInteger;

public class BuiltInFeatures {
    private static RoundRobinScheduler scheduler;
    private static PriorityScheduler priorityScheduler;
    private static final AtomicInteger jobCounter = new AtomicInteger(1);
    private static MemoryManager memoryManager = new MemoryManager(10, PageReplacementAlgorithm.FIFO); // 10 page frames
    private static AuthenticationManager authManager;
    private static PermissionManager permissionManager;

    public static void setAuthenticationManager(AuthenticationManager manager) {
        authManager = manager;
    }

    public static void setPermissionManager(PermissionManager manager) {
        permissionManager = manager;
    }

    public static boolean isBuiltIn(String cmd) {
        switch (cmd) {
            case "cd":
            case "pwd":
            case "exit":
            case "echo":
            case "clear":
            case "ls":
            case "cat":
            case "mkdir":
            case "rmdir":
            case "rm":
            case "touch":
            case "kill":
            case "jobs":
            case "fg":
            case "bg":
            case "schedule":
            case "priority":
            case "mutex":
            case "semaphore":
            case "producer-consumer":
            case "memory-status":
            case "allocate-memory":
            case "access-page":
            case "free-memory":
            case "set-replacement":
            case "logout":
            case "whoami":
            case "create-user":
            case "delete-user":
            case "change-password":
            case "list-users":
            case "chmod":
            case "chown":
            case "ls-l":
            case "set-permissions":
            case "list-permissions":
                return true;
            default:
                return false;
        }
    }

    public static void execute(final CommandParser.ParsedCommand cmd, final Shell shell, final JobManager jobs) {
        try {
            switch (cmd.name) {
                case "cd":
                    cd(cmd.args, shell);
                    break;
                case "pwd":
                    pwd(shell);
                    break;
                case "exit":
                    System.exit(0);
                    break;
                case "echo":
                    echo(cmd.args);
                    break;
                case "clear":
                    clear();
                    break;
                case "ls":
                    ls(shell);
                    break;
                case "cat":
                    cat(cmd.args, shell);
                    break;
                case "mkdir":
                    mkdir(cmd.args, shell);
                    break;
                case "rmdir":
                    rmdir(cmd.args, shell);
                    break;
                case "rm":
                    rm(cmd.args, shell);
                    break;
                case "touch":
                    touch(cmd.args, shell);
                    break;
                case "kill":
                    kill(cmd.args, jobs);
                    break;
                case "jobs":
                    jobs.list();
                    break;
                case "fg":
                    jobs.bringToForeground(cmd.args);
                    break;
                case "bg":
                    jobs.resumeInBackground(cmd.args);
                    break;
                case "schedule":
                    schedule(cmd.args);
                    break;
                case "priority":
                    priority(cmd.args);
                    break;
                case "mutex":
                    testMutex();
                    break;
                case "semaphore":
                    testSemaphore();
                    break;
                case "producer-consumer":
                    testProducerConsumer();
                    break;
                case "memory-status":
                    memoryStatus();
                    break;
                case "allocate-memory":
                    allocateMemory(cmd.args);
                    break;
                case "access-page":
                    accessPage(cmd.args);
                    break;
                case "free-memory":
                    freeMemory(cmd.args);
                    break;
                case "set-replacement":
                    setReplacementAlgorithm(cmd.args);
                    break;
                case "logout":
                    logout();
                    break;
                case "whoami":
                    whoami();
                    break;
                case "create-user":
                    createUser(cmd.args);
                    break;
                case "delete-user":
                    deleteUser(cmd.args);
                    break;
                case "change-password":
                    changePassword(cmd.args);
                    break;
                case "list-users":
                    listUsers();
                    break;
                case "chmod":
                    chmod(cmd.args, shell);
                    break;
                case "chown":
                    chown(cmd.args, shell);
                    break;
                case "ls-l":
                    lsWithPermissions(shell);
                    break;
                case "set-permissions":
                    setPermissions(cmd.args, shell);
                    break;
                case "list-permissions":
                    listPermissions();
                    break;
            }
        } catch (Exception e) {
            System.err.println("Token parse error: " + e.getMessage());
        }
    }

    private static void cd(final String[] args, final Shell shell) {
        File base = shell.getCurrentDirectory();
        String targetArg = args.length > 0 ? args[0] : System.getProperty("user.home");
        File target = new File(base, targetArg);
        try {
            target = target.getCanonicalFile();  // resolves ".." and symlinks
            if (target.isDirectory()) {
                shell.setCurrentDirectory(target);
            } else {
                System.err.println("cd: no such directory: " + targetArg);
            }
        } catch (IOException e) {
            System.err.println("cd: error resolving path: " + e.getMessage());
        }
    }

    private static void pwd(final Shell shell) {
        System.out.println(shell.getCurrentDirectory().getAbsolutePath());
    }

    private static void echo(final String[] args) {
        System.out.println(String.join(" ", args));
    }

    private static void clear() {
        System.out.print("\033[H\033[J");
    }

    private static void ls(final Shell shell) throws IOException {
        if (permissionManager == null) {
            Files.list(shell.getCurrentDirectory().toPath())
                    .map(Path::getFileName)
                    .forEach(System.out::println);
            return;
        }

        String currentUser = authManager != null ? authManager.getCurrentUser().getUsername() : "admin";
        Files.list(shell.getCurrentDirectory().toPath())
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String filePath = path.toString();
                    
                    // Check read permission
                    if (permissionManager.canRead(filePath, currentUser)) {
                        System.out.println(fileName);
                    } else {
                        System.out.println(fileName + " (Permission denied)");
                    }
                });
    }

    private static void cat(final String[] args, final Shell shell) throws IOException {
        if (permissionManager == null) {
            for (String name : args) {
                Path p = shell.getCurrentDirectory().toPath().resolve(name);
                Files.lines(p).forEach(System.out::println);
            }
            return;
        }

        String currentUser = authManager != null ? authManager.getCurrentUser().getUsername() : "admin";
        for (String name : args) {
            Path p = shell.getCurrentDirectory().toPath().resolve(name);
            String filePath = p.toString();
            
            if (!permissionManager.canRead(filePath, currentUser)) {
                System.err.println("cat: Permission denied: " + name);
                continue;
            }
            
            Files.lines(p).forEach(System.out::println);
        }
    }

    private static void mkdir(final String[] args, final Shell shell) throws IOException {
        if (permissionManager == null) {
            for (String name : args) {
                Path p = shell.getCurrentDirectory().toPath().resolve(name);
                Files.createDirectory(p);
            }
            return;
        }

        String currentUser = authManager != null ? authManager.getCurrentUser().getUsername() : "admin";
        for (String name : args) {
            Path p = shell.getCurrentDirectory().toPath().resolve(name);
            String filePath = p.toString();
            
            // Check write permission on parent directory
            String parentPath = p.getParent().toString();
            if (!permissionManager.canWrite(parentPath, currentUser)) {
                System.err.println("mkdir: Permission denied: " + name);
                continue;
            }
            
            Files.createDirectory(p);
            
            // Set default permissions for new directory
            FilePermission permission = permissionManager.createDefaultPermission(currentUser);
            permissionManager.setFilePermission(filePath, permission);
        }
    }

    private static void rmdir(final String[] args, final Shell shell) throws IOException {
        for (String name : args) {
            Path p = shell.getCurrentDirectory().toPath().resolve(name);
            Files.delete(p);
        }
    }

    private static void rm(final String[] args, final Shell shell) throws IOException {
        if (permissionManager == null) {
            for (String name : args) {
                Path p = shell.getCurrentDirectory().toPath().resolve(name);
                Files.delete(p);
            }
            return;
        }

        String currentUser = authManager != null ? authManager.getCurrentUser().getUsername() : "admin";
        for (String name : args) {
            Path p = shell.getCurrentDirectory().toPath().resolve(name);
            String filePath = p.toString();
            
            if (!permissionManager.canWrite(filePath, currentUser)) {
                System.err.println("rm: Permission denied: " + name);
                continue;
            }
            
            Files.delete(p);
            permissionManager.setFilePermission(filePath, null); // Remove permission entry
        }
    }

    private static void touch(final String[] args, final Shell shell) throws IOException {
        if (permissionManager == null) {
            for (String name : args) {
                Path p = shell.getCurrentDirectory().toPath().resolve(name);
                if (Files.exists(p)) {
                    Files.setLastModifiedTime(p, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
                } else {
                    Files.write(p, new byte[0], StandardOpenOption.CREATE);
                }
            }
            return;
        }

        String currentUser = authManager != null ? authManager.getCurrentUser().getUsername() : "admin";
        for (String name : args) {
            Path p = shell.getCurrentDirectory().toPath().resolve(name);
            String filePath = p.toString();
            
            if (Files.exists(p)) {
                // Check write permission for existing file
                if (!permissionManager.canWrite(filePath, currentUser)) {
                    System.err.println("touch: Permission denied: " + name);
                    continue;
                }
                Files.setLastModifiedTime(p, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
            } else {
                // Check write permission on parent directory for new file
                String parentPath = p.getParent().toString();
                if (!permissionManager.canWrite(parentPath, currentUser)) {
                    System.err.println("touch: Permission denied: " + name);
                    continue;
                }
                Files.write(p, new byte[0], StandardOpenOption.CREATE);
                
                // Set default permissions for new file
                FilePermission permission = permissionManager.createDefaultPermission(currentUser);
                permissionManager.setFilePermission(filePath, permission);
            }
        }
    }

    private static void kill(final String[] args, JobManager jobs) {
        for (String token : args) {
            String raw = token.startsWith("%") ? token.substring(1) : token;
            long pid;
            if (token.startsWith("%")) {
                int jobId = Integer.parseInt(raw);
                Job job = jobs.getJobById(jobId);
                if (job == null) {
                    System.err.println("kill: no such job: " + token);
                    continue;
                }
                pid = JobManager.getPid(job.getProcess());
            } else {
                pid = Long.parseLong(raw);
            }
            try {
                Runtime.getRuntime()
                        .exec(new String[]{"kill", "-9", Long.toString(pid)})
                        .waitFor();
                jobs.removeByPid(pid);  // you'd need a remove-by-pid or remove-by-jobId helper
            } catch (IOException | InterruptedException e) {
                System.err.println("kill: unable to terminate " + pid + ": " + e.getMessage());
            }
        }
    }

    private static void schedule(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: schedule <time_quantum> <burst_time1> <burst_time2> ... [time_unit_ms]");
            return;
        }

        try {
            int timeQuantum = Integer.parseInt(args[0]);
            int timeUnitMillis = 1000;
            int burstArgsEnd = args.length;
            // If last argument is a number and not a burst time, treat as time unit
            if (args.length > 2) {
                try {
                    int possibleTimeUnit = Integer.parseInt(args[args.length - 1]);
                    // If the number of burst times is at least 1, and the last arg is not a burst time
                    if (args.length - 2 >= 1 && possibleTimeUnit != Integer.parseInt(args[1])) {
                        timeUnitMillis = possibleTimeUnit;
                        burstArgsEnd = args.length - 1;
                    }
                } catch (NumberFormatException ignored) {}
            }
            scheduler = new RoundRobinScheduler(timeQuantum, timeUnitMillis);

            // Add processes with burst times
            for (int i = 1; i < burstArgsEnd; i++) {
                int burstTime = Integer.parseInt(args[i]);
                scheduler.addProcess(new ScheduledProcess("P" + i, burstTime));
            }

            // Start scheduling
            scheduler.schedule();
            scheduler.printStatistics();
        } catch (NumberFormatException e) {
            System.err.println("Error: All arguments must be numbers");
        }
    }

    private static void priority(String[] args) {
        if (args.length < 2 || (args.length % 2 != 0 && args.length % 2 != 1)) {
            System.err.println("Usage: priority <burst_time1> <priority1> <burst_time2> <priority2> ... [time_unit_ms]");
            return;
        }

        try {
            int timeUnitMillis = 1000;
            int pairArgsEnd = args.length;
            // If last argument is a number and not a burst/priority, treat as time unit
            if (args.length > 2) {
                try {
                    int possibleTimeUnit = Integer.parseInt(args[args.length - 1]);
                    if ((args.length - 1) % 2 == 0) {
                        timeUnitMillis = possibleTimeUnit;
                        pairArgsEnd = args.length - 1;
                    }
                } catch (NumberFormatException ignored) {}
            }
            priorityScheduler = new PriorityScheduler(timeUnitMillis);

            // Add processes with burst times and priorities
            for (int i = 0; i < pairArgsEnd; i += 2) {
                int burstTime = Integer.parseInt(args[i]);
                int priority = Integer.parseInt(args[i + 1]);
                priorityScheduler.addProcess(new ScheduledProcess("P" + (i/2 + 1), burstTime, priority));
            }

            // Start scheduling
            priorityScheduler.schedule();
            priorityScheduler.printStatistics();
        } catch (NumberFormatException e) {
            System.err.println("Error: All arguments must be numbers");
        }
    }

    private static void testMutex() {
        System.out.println("=== Testing Mutex ===");
        Mutex mutex = new Mutex();
        
        // Create two threads that will compete for the mutex
        Thread t1 = new Thread(() -> {
            System.out.println("Thread 1 trying to acquire mutex...");
            mutex.lock();
            System.out.println("Thread 1 acquired mutex");
            try {
                Thread.sleep(2000); // Hold the lock for 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            mutex.unlock();
            System.out.println("Thread 1 released mutex");
        });

        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(500); // Wait a bit before trying to acquire
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Thread 2 trying to acquire mutex...");
            mutex.lock();
            System.out.println("Thread 2 acquired mutex");
            mutex.unlock();
            System.out.println("Thread 2 released mutex");
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void testSemaphore() {
        System.out.println("=== Testing Semaphore ===");
        Semaphore sem = new Semaphore(2);
        
        // Create three threads that will compete for semaphore permits
        for (int i = 1; i <= 3; i++) {
            final int threadId = i;
            new Thread(() -> {
                System.out.println("Thread " + threadId + " trying to acquire permit...");
                sem.acquire();
                System.out.println("Thread " + threadId + " acquired permit");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                sem.release();
                System.out.println("Thread " + threadId + " released permit");
            }).start();
        }
    }

    private static void testProducerConsumer() {
        System.out.println("=== Testing Producer-Consumer ===");
        ProducerConsumer pc = new ProducerConsumer(3);
        
        // Create producer thread
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                pc.produce(i);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        // Create consumer thread
        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                pc.consume();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        producer.start();
        consumer.start();

        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void memoryStatus() {
        System.out.println("\n=== Memory Status ===");
        System.out.println("Total Page Frames: " + memoryManager.getTotalFrames());
        System.out.println("Available Frames: " + memoryManager.getAvailableFrames());
        System.out.println("Page Faults: " + memoryManager.getPageFaults());
        System.out.println("Current Replacement Algorithm: " + memoryManager.getReplacementAlgorithm());
        System.out.println("\nAllocated Pages:");
        memoryManager.printMemoryStatus();
    }

    private static void allocateMemory(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: allocate-memory <process_id> <num_pages>");
            return;
        }

        try {
            int processId = Integer.parseInt(args[0]);
            int numPages = Integer.parseInt(args[1]);
            
            System.out.println("Allocating " + numPages + " pages for process " + processId);
            boolean success = memoryManager.allocatePages(processId, numPages);
            
            if (success) {
                System.out.println("Memory allocation successful");
            } else {
                System.out.println("Memory allocation failed - not enough frames available");
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Process ID and number of pages must be integers");
        }
    }

    private static void accessPage(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: access-page <process_id> <page_number>");
            return;
        }

        try {
            int processId = Integer.parseInt(args[0]);
            int pageNumber = Integer.parseInt(args[1]);
            
            System.out.println("Accessing page " + pageNumber + " for process " + processId);
            boolean success = memoryManager.accessPage(processId, pageNumber);
            
            if (success) {
                System.out.println("Page access successful");
            } else {
                System.out.println("Page access failed - page not allocated to process");
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Process ID and page number must be integers");
        }
    }

    private static void freeMemory(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: free-memory <process_id>");
            return;
        }

        try {
            int processId = Integer.parseInt(args[0]);
            
            System.out.println("Freeing memory for process " + processId);
            boolean success = memoryManager.freeProcessPages(processId);
            
            if (success) {
                System.out.println("Memory freed successfully");
            } else {
                System.out.println("No memory allocated for this process");
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Process ID must be an integer");
        }
    }

    private static void setReplacementAlgorithm(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: set-replacement <algorithm>");
            System.err.println("Available algorithms: FIFO, LRU");
            return;
        }

        try {
            PageReplacementAlgorithm algorithm = PageReplacementAlgorithm.valueOf(args[0].toUpperCase());
            memoryManager.setReplacementAlgorithm(algorithm);
            System.out.println("Replacement algorithm set to " + algorithm);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: Invalid algorithm. Use FIFO or LRU");
        }
    }

    private static void logout() {
        if (authManager != null) {
            authManager.logout();
        } else {
            System.out.println("Authentication system not initialized.");
        }
    }

    private static void whoami() {
        if (authManager != null) {
            authManager.showCurrentUser();
        } else {
            System.out.println("Authentication system not initialized.");
        }
    }

    private static void createUser(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: create-user <username> <password> <role>");
            System.err.println("Roles: admin, standard");
            return;
        }

        if (authManager != null) {
            String username = args[0];
            String password = args[1];
            String roleStr = args[2].toLowerCase();
            
            User.UserRole role;
            try {
                role = User.UserRole.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Error: Invalid role. Use 'admin' or 'standard'");
                return;
            }
            
            authManager.createUser(username, password, role);
        } else {
            System.out.println("Authentication system not initialized.");
        }
    }

    private static void deleteUser(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: delete-user <username>");
            return;
        }

        if (authManager != null) {
            String username = args[0];
            authManager.deleteUser(username);
        } else {
            System.out.println("Authentication system not initialized.");
        }
    }

    private static void changePassword(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: change-password <username> <new_password>");
            return;
        }

        if (authManager != null) {
            String username = args[0];
            String newPassword = args[1];
            authManager.changePassword(username, newPassword);
        } else {
            System.out.println("Authentication system not initialized.");
        }
    }

    private static void listUsers() {
        if (authManager != null) {
            authManager.listUsers();
        } else {
            System.out.println("Authentication system not initialized.");
        }
    }

    private static void chmod(String[] args, Shell shell) {
        if (permissionManager == null) {
            System.err.println("Permission system not initialized.");
            return;
        }

        if (args.length < 2) {
            System.err.println("Usage: chmod <mode> <file>");
            System.err.println("Example: chmod rwxr-xr-x myfile.txt");
            return;
        }

        String mode = args[0];
        String fileName = args[1];
        String currentUser = authManager != null ? authManager.getCurrentUser().getUsername() : "admin";
        
        permissionManager.chmod(shell.getCurrentDirectory().toPath().resolve(fileName).toString(), mode, currentUser);
    }

    private static void chown(String[] args, Shell shell) {
        if (permissionManager == null) {
            System.err.println("Permission system not initialized.");
            return;
        }

        if (args.length < 2) {
            System.err.println("Usage: chown <new_owner> <file>");
            System.err.println("Example: chown john myfile.txt");
            return;
        }

        String newOwner = args[0];
        String fileName = args[1];
        String currentUser = authManager != null ? authManager.getCurrentUser().getUsername() : "admin";
        
        permissionManager.chown(shell.getCurrentDirectory().toPath().resolve(fileName).toString(), newOwner, currentUser);
    }

    private static void lsWithPermissions(Shell shell) {
        if (permissionManager == null) {
            System.err.println("Permission system not initialized.");
            return;
        }

        try {
            String currentUser = authManager != null ? authManager.getCurrentUser().getUsername() : "admin";
            Files.list(shell.getCurrentDirectory().toPath())
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String filePath = path.toString();
                        
                        FilePermission permission = permissionManager.getFilePermission(filePath);
                        if (permission != null) {
                            System.out.println(permission.toDetailedString() + " " + fileName);
                        } else {
                            // Default permission display
                            System.out.println("rw-r--r-- " + currentUser + ":users " + fileName);
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error listing files: " + e.getMessage());
        }
    }

    private static void setPermissions(String[] args, Shell shell) {
        if (permissionManager == null) {
            System.err.println("Permission system not initialized.");
            return;
        }

        if (args.length < 4) {
            System.err.println("Usage: set-permissions <file> <owner> <group> <mode>");
            System.err.println("Example: set-permissions myfile.txt john users rw-r--r--");
            return;
        }

        String fileName = args[0];
        String owner = args[1];
        String group = args[2];
        String mode = args[3];
        
        // Resolve file path relative to current directory
        String filePath = shell.getCurrentDirectory().toPath().resolve(fileName).toString();
        
        if (mode.length() != 9) {
            System.err.println("Error: Invalid permission mode. Use format: rwxrwxrwx");
            return;
        }

        // Parse permissions
        java.util.Set<FilePermission.Permission> ownerPerms = parsePermissions(mode.substring(0, 3));
        java.util.Set<FilePermission.Permission> groupPerms = parsePermissions(mode.substring(3, 6));
        java.util.Set<FilePermission.Permission> otherPerms = parsePermissions(mode.substring(6, 9));

        permissionManager.setFilePermissions(filePath, owner, group, ownerPerms, groupPerms, otherPerms);
        System.out.println("Permissions set for " + fileName + ": " + mode);
    }

    private static java.util.Set<FilePermission.Permission> parsePermissions(String permString) {
        java.util.Set<FilePermission.Permission> permissions = java.util.EnumSet.noneOf(FilePermission.Permission.class);
        
        if (permString.charAt(0) == 'r') permissions.add(FilePermission.Permission.READ);
        if (permString.charAt(1) == 'w') permissions.add(FilePermission.Permission.WRITE);
        if (permString.charAt(2) == 'x') permissions.add(FilePermission.Permission.EXECUTE);
        
        return permissions;
    }

    private static void listPermissions() {
        if (permissionManager == null) {
            System.err.println("Permission system not initialized.");
            return;
        }

        permissionManager.listAllPermissions();
    }
}


