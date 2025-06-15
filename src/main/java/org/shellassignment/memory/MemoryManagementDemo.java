package org.shellassignment.memory;

import java.util.Scanner;

public class MemoryManagementDemo {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Memory Management System Demo");
        System.out.println("============================");
        
        // Get memory configuration
        System.out.print("Enter total number of frames: ");
        int totalFrames = scanner.nextInt();
        
        System.out.println("\nSelect page replacement algorithm:");
        System.out.println("1. FIFO (First-In-First-Out)");
        System.out.println("2. LRU (Least Recently Used)");
        System.out.print("Enter your choice (1 or 2): ");
        int algorithmChoice = scanner.nextInt();
        
        PageReplacementAlgorithm algorithm = 
            (algorithmChoice == 1) ? PageReplacementAlgorithm.FIFO 
                                 : PageReplacementAlgorithm.LRU;
        
        MemoryManager memoryManager = new MemoryManager(totalFrames, algorithm);
        
        while (true) {
            System.out.println("\nMemory Management Commands:");
            System.out.println("1. Allocate memory for a process");
            System.out.println("2. Access a page");
            System.out.println("3. Deallocate memory for a process");
            System.out.println("4. Show memory status");
            System.out.println("5. Exit");
            System.out.print("Enter your choice (1-5): ");
            
            int choice = scanner.nextInt();
            
            switch (choice) {
                case 1:
                    System.out.print("Enter process ID: ");
                    int processId = scanner.nextInt();
                    System.out.print("Enter number of pages to allocate: ");
                    int numPages = scanner.nextInt();
                    memoryManager.allocatePages(processId, numPages);
                    System.out.println("Memory allocated successfully!");
                    break;
                    
                case 2:
                    System.out.print("Enter process ID: ");
                    processId = scanner.nextInt();
                    System.out.print("Enter page ID to access: ");
                    int pageId = scanner.nextInt();
                    boolean success = memoryManager.accessPage(processId, pageId);
                    if (success) {
                        System.out.println("Page access successful!");
                    } else {
                        System.out.println("Error: Invalid process ID or page ID!");
                    }
                    break;
                    
                case 3:
                    System.out.print("Enter process ID to deallocate: ");
                    processId = scanner.nextInt();
                    memoryManager.freeProcessPages(processId);
                    System.out.println("Memory deallocated successfully!");
                    break;
                    
                case 4:
                    memoryManager.printMemoryStatus();
                    break;
                    
                case 5:
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                    
                default:
                    System.out.println("Invalid choice! Please try again.");
            }
        }
    }
} 