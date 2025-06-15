package org.shellassignment.sync;

public class Semaphore {
    private int permits;
    private final Object lock = new Object();

    public Semaphore(int initialPermits) {
        if (initialPermits < 0) {
            throw new IllegalArgumentException("Initial permits cannot be negative");
        }
        this.permits = initialPermits;
    }

    public void acquire() {
        synchronized (lock) {
            while (permits <= 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Acquire interrupted", e);
                }
            }
            permits--;
        }
    }

    public void release() {
        synchronized (lock) {
            permits++;
            lock.notify();
        }
    }

    public int availablePermits() {
        synchronized (lock) {
            return permits;
        }
    }
} 