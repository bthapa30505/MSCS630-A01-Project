package org.shellassignment.sync;

import java.util.LinkedList;
import java.util.Queue;

public class ProducerConsumer {
    private final Queue<Integer> buffer;
    private final int capacity;
    private final Mutex mutex;
    private final Semaphore empty;
    private final Semaphore full;

    public ProducerConsumer(int capacity) {
        this.buffer = new LinkedList<>();
        this.capacity = capacity;
        this.mutex = new Mutex();
        this.empty = new Semaphore(capacity);
        this.full = new Semaphore(0);
    }

    public void produce(int item) {
        try {
            empty.acquire();  // Wait if buffer is full
            mutex.lock();     // Get exclusive access to buffer
            
            buffer.add(item);
            System.out.println("Produced: " + item + ", Buffer size: " + buffer.size());
            
            mutex.unlock();   // Release buffer access
            full.release();   // Signal that buffer is not empty
        } catch (Exception e) {
            System.err.println("Error in producer: " + e.getMessage());
        }
    }

    public int consume() {
        try {
            full.acquire();   // Wait if buffer is empty
            mutex.lock();     // Get exclusive access to buffer
            
            int item = buffer.remove();
            System.out.println("Consumed: " + item + ", Buffer size: " + buffer.size());
            
            mutex.unlock();   // Release buffer access
            empty.release();  // Signal that buffer is not full
            return item;
        } catch (Exception e) {
            System.err.println("Error in consumer: " + e.getMessage());
            return -1;
        }
    }

    public static void main(String[] args) {
        ProducerConsumer pc = new ProducerConsumer(5);
        
        // Create producer thread
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                pc.produce(i);
                try {
                    Thread.sleep(100); // Simulate production time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        // Create consumer thread
        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                pc.consume();
                try {
                    Thread.sleep(200); // Simulate consumption time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        // Start threads
        producer.start();
        consumer.start();

        // Wait for threads to complete
        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
} 