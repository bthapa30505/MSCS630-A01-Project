package org.shellassignment;

import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;

public class RoundRobinScheduler {
    private final Queue<ScheduledProcess> readyQueue;
    private final int timeQuantum;
    private int currentTime;
    private final List<ScheduledProcess> completedProcesses;
    private final int timeUnitMillis;

    public RoundRobinScheduler(int timeQuantum, int timeUnitMillis) {
        this.readyQueue = new LinkedList<>();
        this.timeQuantum = timeQuantum;
        this.currentTime = 0;
        this.completedProcesses = new ArrayList<>();
        this.timeUnitMillis = timeUnitMillis;
    }

    public void addProcess(ScheduledProcess process) {
        readyQueue.add(process);
    }

    public void schedule() {
        while (!readyQueue.isEmpty()) {
            ScheduledProcess currentProcess = readyQueue.poll();
            
            // Calculate the actual time slice for this process
            int timeSlice = Math.min(timeQuantum, currentProcess.getRemainingTime());
            
            // Simulate process execution
            try {
                System.out.println("Executing process " + currentProcess.getProcessId() + 
                                 " for " + timeSlice + " time units");
                Thread.sleep(timeSlice * timeUnitMillis); // Use configurable time unit
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Process execution interrupted");
                return;
            }

            // Update process state
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - timeSlice);
            currentTime += timeSlice;

            // Update waiting time for processes in the queue
            for (ScheduledProcess p : readyQueue) {
                p.incrementWaitingTime(timeSlice);
            }

            if (currentProcess.isCompleted()) {
                // Process is complete
                currentProcess.setTurnaroundTime(currentTime);
                completedProcesses.add(currentProcess);
                System.out.println("Process " + currentProcess.getProcessId() + " completed");
            } else {
                // Process needs more time, add it back to the queue
                readyQueue.add(currentProcess);
            }
        }
    }

    public void printStatistics() {
        System.out.println("\nScheduling Statistics:");
        System.out.println("=====================");
        
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