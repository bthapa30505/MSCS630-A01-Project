package org.shellassignment;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Shell {
    private final CommandParser parser = new CommandParser();
    private final JobManager jobs = new JobManager();
    private File currentDirectory = new File(System.getProperty("user.dir"));
    public File getCurrentDirectory() { return currentDirectory; }
    public void setCurrentDirectory(File dir) { this.currentDirectory = dir; }

    public static void main(String[] args) {
        new Shell().run();
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("mysh> ");
                String line = in.readLine();
                if (line == null || line.trim().isEmpty()) continue;
                CommandParser.ParsedCommand cmd = parser.parse(line);

                if (BuiltInFeatures.isBuiltIn(cmd.name)) {
                    BuiltInFeatures.execute(cmd, this, jobs);
                } else {
                    launchExternal(cmd);
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