package org.shellassignment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class BuiltInFeatures {
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
                    kill(cmd.args);
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
            }
        } catch (Exception e) {
            System.err.println("builtin error: " + e.getMessage());
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

    private static void kill(final String[] args) {
        for (String pidStr : args) {
            try {
                Runtime.getRuntime().exec(new String[]{"kill", "-9", pidStr}).waitFor();
            } catch (IOException | InterruptedException e) {
                System.err.println("kill: unable to terminate process " + pidStr + ": " + e.getMessage());
            }
        }
    }
}


