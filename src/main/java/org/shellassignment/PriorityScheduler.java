package org.shellassignment;

import java.util.PriorityQueue;
import java.util.List;
import java.util.ArrayList;

public class PriorityScheduler {
    private final PriorityQueue<ScheduledProcess> readyQueue;
    private ScheduledProcess currentProcess;
    private int currentTime;
    private final List<ScheduledProcess> completedProcesses;
    private final int timeUnitMillis;

    public PriorityScheduler(int timeUnitMillis) {
        this.readyQueue = new PriorityQueue<>();
        this.currentProcess = null;
        this.currentTime = 0;
        this.completedProcesses = new ArrayList<>();
        this.timeUnitMillis = timeUnitMillis;
    }

    public void addProcess(ScheduledProcess process) {
        if (currentProcess != null && process.getPriority() > currentProcess.getPriority()) {
            // Preempt current process if new process has higher priority
            System.out.println("Preempting process " + currentProcess.getProcessId() + 
                             " for higher priority process " + process.getProcessId());
            readyQueue.add(currentProcess);
            currentProcess = process;
        } else {
            readyQueue.add(process);
        }
    }

    public void schedule() {
        while (!readyQueue.isEmpty() || currentProcess != null) {
            // If no process is running, get the highest priority process
            if (currentProcess == null) {
                currentProcess = readyQueue.poll();
            }

            // Execute current process for 1 time unit
            try {
                System.out.println("Executing process " + currentProcess.getProcessId() + 
                                 " (Priority: " + currentProcess.getPriority() + ")");
                Thread.sleep(timeUnitMillis); // Use configurable time unit
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Process execution interrupted");
                return;
            }

            // Update process state
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - 1);
            currentTime++;

            // Update waiting time for processes in the queue
            for (ScheduledProcess p : readyQueue) {
                p.incrementWaitingTime(1);
            }

            // Check if current process is completed
            if (currentProcess.isCompleted()) {
                currentProcess.setTurnaroundTime(currentTime);
                completedProcesses.add(currentProcess);
                System.out.println("Process " + currentProcess.getProcessId() + " completed");
                currentProcess = null;
            }
        }
    }

    public void printStatistics() {
        System.out.println("\nPriority Scheduling Statistics:");
        System.out.println("=============================");
        
        double totalWaitingTime = 0;
        double totalTurnaroundTime = 0;
        
        for (ScheduledProcess process : completedProcesses) {
            System.out.println(process);
            totalWaitingTime += process.getWaitingTime();
            totalTurnaroundTime += process.getTurnaroundTime();
        }
        
        double avgWaitingTime = totalWaitingTime / completedProcesses.size();
        double avgTurnaroundTime = totalTurnaroundTime / completedProcesses.size();
        
        System.out.println("\nAverage Waiting Time: " + avgWaitingTime);
        System.out.println("Average Turnaround Time: " + avgTurnaroundTime);
    }
} 