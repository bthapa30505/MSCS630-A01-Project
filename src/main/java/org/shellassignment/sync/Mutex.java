package org.shellassignment.sync;

import java.util.concurrent.atomic.AtomicBoolean;

public class Mutex {
    private final AtomicBoolean locked = new AtomicBoolean(false);
    private Thread owner = null;

    public synchronized void lock() {
        while (locked.get()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Lock interrupted", e);
            }
        }
        locked.set(true);
        owner = Thread.currentThread();
    }

    public synchronized void unlock() {
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("Only the owner can unlock the mutex");
        }
        locked.set(false);
        owner = null;
        notify();
    }

    public boolean isLocked() {
        return locked.get();
    }

    public Thread getOwner() {
        return owner;
    }
} 