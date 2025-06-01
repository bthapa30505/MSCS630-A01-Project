package org.shellassignment;

public class ScheduledProcess implements Comparable<ScheduledProcess> {
    private final String processId;
    private final int burstTime;  // Total time needed to complete the process
    private int remainingTime;    // Remaining time to complete the process
    private int waitingTime;      // Time spent waiting in the ready queue
    private int turnaroundTime;   // Total time from arrival to completion
    private final int priority;   // Process priority (higher number = higher priority)
    private final long arrivalTime; // Time when process was added to the queue

    public ScheduledProcess(String processId, int burstTime, int priority) {
        this.processId = processId;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.waitingTime = 0;
        this.turnaroundTime = 0;
        this.priority = priority;
        this.arrivalTime = System.currentTimeMillis();
    }

    // For backward compatibility with round-robin scheduling
    public ScheduledProcess(String processId, int burstTime) {
        this(processId, burstTime, 0);
    }

    public String getProcessId() {
        return processId;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(int remainingTime) {
        this.remainingTime = remainingTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void incrementWaitingTime(int time) {
        this.waitingTime += time;
    }

    public int getTurnaroundTime() {
        return turnaroundTime;
    }

    public void setTurnaroundTime(int turnaroundTime) {
        this.turnaroundTime = turnaroundTime;
    }

    public int getPriority() {
        return priority;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public boolean isCompleted() {
        return remainingTime <= 0;
    }

    @Override
    public int compareTo(ScheduledProcess other) {
        // First compare by priority (higher priority first)
        int priorityCompare = Integer.compare(other.priority, this.priority);
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        // If priorities are equal, compare by arrival time (FCFS)
        return Long.compare(this.arrivalTime, other.arrivalTime);
    }

    @Override
    public String toString() {
        return "ScheduledProcess{" +
                "processId='" + processId + '\'' +
                ", burstTime=" + burstTime +
                ", remainingTime=" + remainingTime +
                ", waitingTime=" + waitingTime +
                ", turnaroundTime=" + turnaroundTime +
                ", priority=" + priority +
                '}';
    }
} 