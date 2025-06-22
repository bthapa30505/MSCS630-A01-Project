package org.shellassignment;

import java.io.*;
import java.util.List;

public class PipeManager {
    
    public static void executePipedCommands(CommandParser.PipedCommands pipedCommands, Shell shell, JobManager jobs) {
        List<CommandParser.ParsedCommand> commands = pipedCommands.commands;
        
        if (commands.size() < 2) {
            // Single command, execute normally
            CommandParser.ParsedCommand cmd = commands.get(0);
            if (BuiltInFeatures.isBuiltIn(cmd.name)) {
                BuiltInFeatures.execute(cmd, shell, jobs);
            } else {
                launchExternal(cmd, shell, jobs);
            }
            return;
        }
        
        try {
            // Create and start all processes
            Process[] processes = new Process[commands.size()];
            
            // Start the first process
            processes[0] = createProcess(commands.get(0), shell);
            
            // Start remaining processes and connect them with pipes
            for (int i = 1; i < commands.size(); i++) {
                ProcessBuilder pb = createProcessBuilder(commands.get(i), shell);
                processes[i] = pb.start();
                
                // Connect the output of previous process to input of current process
                connectProcesses(processes[i-1], processes[i]);
            }
            
            // Handle background execution
            if (pipedCommands.background) {
                int jobId = jobs.addJob(processes[processes.length - 1], pipedCommands.original);
                long pid = JobManager.getPid(processes[processes.length - 1]);
                System.out.printf("[%d] %d%n", jobId, pid);
            } else {
                // Capture and display output from the final process
                captureAndDisplayOutput(processes[processes.length - 1]);
                
                // Wait for all processes to complete
                for (Process process : processes) {
                    process.waitFor();
                }
            }
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing piped commands: " + e.getMessage());
        }
    }
    
    private static void captureAndDisplayOutput(Process process) {
        // Create a thread to read and display the output from the final process
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                System.err.println("Error reading process output: " + e.getMessage());
            }
        });
        outputThread.start();
    }
    
    private static void connectProcesses(Process source, Process destination) {
        // Create a thread to pipe data from source to destination
        Thread pipeThread = new Thread(() -> {
            try (InputStream sourceOutput = source.getInputStream();
                 OutputStream destInput = destination.getOutputStream()) {
                
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = sourceOutput.read(buffer)) != -1) {
                    destInput.write(buffer, 0, bytesRead);
                }
                destInput.close();
            } catch (IOException e) {
                System.err.println("Error in pipe: " + e.getMessage());
            }
        });
        pipeThread.start();
    }
    
    private static Process createProcess(CommandParser.ParsedCommand cmd, Shell shell) throws IOException {
        ProcessBuilder pb = createProcessBuilder(cmd, shell);
        return pb.start();
    }
    
    private static ProcessBuilder createProcessBuilder(CommandParser.ParsedCommand cmd, Shell shell) {
        ProcessBuilder pb = new ProcessBuilder(cmd.tokens());
        pb.directory(shell.getCurrentDirectory());
        return pb;
    }
    
    private static void launchExternal(CommandParser.ParsedCommand cmd, Shell shell, JobManager jobs) {
        ProcessBuilder pb = new ProcessBuilder(cmd.tokens());
        pb.directory(shell.getCurrentDirectory());
        try {
            Process proc = pb.start();
            if (cmd.background) {
                int jobId = jobs.addJob(proc, cmd.original);
                long pid = JobManager.getPid(proc);
                System.out.printf("[%d] %d%n", jobId, pid);
            } else {
                // Capture and display output for single commands too
                captureAndDisplayOutput(proc);
                proc.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error launching: " + e.getMessage());
        }
    }
} 