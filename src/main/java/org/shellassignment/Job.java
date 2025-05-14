package org.shellassignment;

public class Job {
    //Need to add other status as needed.
    public enum Status { RUNNING }

    private final int id;
    private final Process process;
    private final String command;
    private Status status;

    public Job(int id, Process process, String command) {
        this.id = id;
        this.process = process;
        this.command = command;
        this.status = Status.RUNNING;
    }

    public int getId() { return id; }
    public Process getProcess() { return process; }
    public String getCommand() { return command; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}