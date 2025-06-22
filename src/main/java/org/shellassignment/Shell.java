package org.shellassignment;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.shellassignment.auth.AuthenticationManager;
import org.shellassignment.permissions.PermissionManager;

public class Shell {
    private final CommandParser parser = new CommandParser();
    private final JobManager jobs = new JobManager();
    private final AuthenticationManager authManager;
    private final PermissionManager permissionManager;
    private File currentDirectory = new File(System.getProperty("user.dir"));
    
    public File getCurrentDirectory() { return currentDirectory; }
    public void setCurrentDirectory(File dir) { this.currentDirectory = dir; }

    public Shell() {
        this.authManager = new AuthenticationManager();
        this.permissionManager = new PermissionManager();
        BuiltInFeatures.setAuthenticationManager(authManager);
        BuiltInFeatures.setPermissionManager(permissionManager);
        initializeSystemPermissions();
    }

    private void initializeSystemPermissions() {
        // Set up some system files with restricted permissions
        String currentDir = currentDirectory.getAbsolutePath();
        
        // System files that only admin can modify
        permissionManager.setFilePermission(currentDir + "/pom.xml", 
            permissionManager.createSystemFilePermission());
        permissionManager.setFilePermission(currentDir + "/README.md", 
            permissionManager.createSystemFilePermission());
        
        // Set user groups for authentication users
        permissionManager.setUserGroup("admin", "admin");
        permissionManager.setUserGroup("user1", "users");
        permissionManager.setUserGroup("user2", "users");
    }

    public static void main(String[] args) {
        new Shell().run();
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                // Check if user is logged in, if not, require authentication
                if (!authManager.isLoggedIn()) {
                    if (!authManager.login()) {
                        System.out.println("Authentication failed. Exiting...");
                        return;
                    }
                }

                // Show current user in prompt
                String prompt = authManager.getCurrentUser().getUsername() + "@mysh> ";
                System.out.print(prompt);
                
                String line = in.readLine();
                if (line == null || line.trim().isEmpty()) continue;
                
                // Check if the command contains pipes
                if (line.contains("|")) {
                    CommandParser.PipedCommands pipedCommands = parser.parsePipedCommands(line);
                    PipeManager.executePipedCommands(pipedCommands, this, jobs);
                } else {
                    CommandParser.ParsedCommand cmd = parser.parse(line);

                    if (BuiltInFeatures.isBuiltIn(cmd.name)) {
                        BuiltInFeatures.execute(cmd, this, jobs);
                    } else {
                        launchExternal(cmd);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
    }

    private void launchExternal(CommandParser.ParsedCommand cmd) {
        ProcessBuilder pb = new ProcessBuilder(cmd.tokens());
        pb.directory(this.getCurrentDirectory());
        try {
            Process proc = pb.start();
            if (cmd.background) {
                int jobId = jobs.addJob(proc, cmd.original);
                long pid = JobManager.getPid(proc);
                System.out.printf("[%d] %d%n", jobId, pid);
            } else {
                proc.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error launching: " + e.getMessage());
        }
    }
}