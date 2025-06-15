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

import java.util.concurrent.atomic.AtomicInteger;

public class BuiltInFeatures {
    private static RoundRobinScheduler scheduler;
    private static PriorityScheduler priorityScheduler;
    private static final AtomicInteger jobCounter = new AtomicInteger(1);
    private static MemoryManager memoryManager = new MemoryManager(10, PageReplacementAlgorithm.FIFO); // 10 page frames

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
}


