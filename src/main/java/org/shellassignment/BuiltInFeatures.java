package org.shellassignment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class BuiltInFeatures {
    private static RoundRobinScheduler scheduler;
    private static PriorityScheduler priorityScheduler;

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
            target = target.getCanonicalFile();  // resolves “..” and symlinks
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
        Files.list(shell.getCurrentDirectory().toPath())
                .map(Path::getFileName)
                .forEach(System.out::println);
    }

    private static void cat(final String[] args, final Shell shell) throws IOException {
        for (String name : args) {
            Path p = shell.getCurrentDirectory().toPath().resolve(name);
            Files.lines(p).forEach(System.out::println);
        }
    }

    private static void mkdir(final String[] args, final Shell shell) throws IOException {
        for (String name : args) {
            Path p = shell.getCurrentDirectory().toPath().resolve(name);
            Files.createDirectory(p);
        }
    }

    private static void rmdir(final String[] args, final Shell shell) throws IOException {
        for (String name : args) {
            Path p = shell.getCurrentDirectory().toPath().resolve(name);
            Files.delete(p);
        }
    }

    private static void rm(final String[] args, final Shell shell) throws IOException {
        for (String name : args) {
            Path p = shell.getCurrentDirectory().toPath().resolve(name);
            Files.delete(p);
        }
    }

    private static void touch(final String[] args, final Shell shell) throws IOException {
        for (String name : args) {
            Path p = shell.getCurrentDirectory().toPath().resolve(name);
            if (Files.exists(p)) {
                Files.setLastModifiedTime(p, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
            } else {
                Files.write(p, new byte[0], StandardOpenOption.CREATE);
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
                jobs.removeByPid(pid);  // you’d need a remove-by-pid or remove-by-jobId helper
            } catch (IOException | InterruptedException e) {
                System.err.println("kill: unable to terminate " + pid + ": " + e.getMessage());
            }
        }
    }
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
}


