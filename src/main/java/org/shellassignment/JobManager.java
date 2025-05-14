package org.shellassignment;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class JobManager {
    private final Map<Integer, Job> jobs = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    //method to get the process id.
    public static long getPid(Process process) {
        try {
            Field f = process.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            return f.getLong(process);
        } catch (Exception e) {
            return -1;
        }
    }

    //method to add job.
    public int addJob(Process p, String cmd) {
        int id = nextId.getAndIncrement();
        jobs.put(id, new Job(id, p, cmd));
        return id;
    }

    // Retrieve a job by its job ID.
    public Job getJobById(int id) {
        return jobs.get(id);
    }

    // Retrieve a job by its process ID.
    public Job getJobByPid(long pid) {
        for (Job job : jobs.values()) {
            if (getPid(job.getProcess()) == pid) {
                return job;
            }
        }
        return null;
    }

    // Remove a job by its job ID.
    public boolean removeById(int id) {
        return jobs.remove(id) != null;
    }

    // Remove a job by its process ID.
    public boolean removeByPid(long pid) {
        Integer targetId = null;
        for (Map.Entry<Integer, Job> entry : jobs.entrySet()) {
            if (getPid(entry.getValue().getProcess()) == pid) {
                targetId = entry.getKey();
                break;
            }
        }
        if (targetId != null) {
            jobs.remove(targetId);
            return true;
        }
        return false;
    }


    //lists all processes/jobs
    public void list() {
        jobs.forEach((id, job) -> {
            long pid = getPid(job.getProcess());
            System.out.printf("[%d] (%d) %s %s%n",
                    id, pid, job.getStatus(), job.getCommand());
        });
    }

    public void bringToForeground(String[] args) throws InterruptedException {
        // Allow both "1" and "%1"
        String raw = args[0].startsWith("%") ? args[0].substring(1) : args[0];
        int id = Integer.parseInt(raw);
        Job job = jobs.get(id);
        if (job == null) {
            System.err.println("fg: no such job");
            return;
        }
        Process p = job.getProcess();
        p.waitFor();
        jobs.remove(id);
    }

    public void resumeInBackground(String[] args) {
        String raw = args[0].startsWith("%") ? args[0].substring(1) : args[0];
        int id = Integer.parseInt(raw);
        Job job = jobs.get(id);
        if (job == null) {
            System.err.println("bg: no such job");
            return;
        }
        long pid = getPid(job.getProcess());
        System.out.printf("Resuming job [%d] (%d) in background: %s%n",
                id, pid, job.getCommand());
    }
}