package org.shellassignment.memory;

import java.util.*;

public class MemoryManager {
    private final int totalFrames;
    private PageReplacementAlgorithm replacementAlgorithm;
    private final Map<Integer, List<Page>> processPages; // processId -> list of pages
    private final Page[] frames; // physical memory frames
    private int pageFaults;
    private final Queue<Integer> fifoQueue; // for FIFO algorithm
    private final Map<Integer, Long> pageAccessTimes; // for LRU algorithm

    public MemoryManager(int totalFrames, PageReplacementAlgorithm algorithm) {
        this.totalFrames = totalFrames;
        this.replacementAlgorithm = algorithm;
        this.processPages = new HashMap<>();
        this.frames = new Page[totalFrames];
        this.pageFaults = 0;
        this.fifoQueue = new LinkedList<>();
        this.pageAccessTimes = new HashMap<>();
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public int getAvailableFrames() {
        int used = 0;
        for (Page frame : frames) {
            if (frame != null) used++;
        }
        return totalFrames - used;
    }

    public int getPageFaults() {
        return pageFaults;
    }

    public PageReplacementAlgorithm getReplacementAlgorithm() {
        return replacementAlgorithm;
    }

    public void setReplacementAlgorithm(PageReplacementAlgorithm algorithm) {
        this.replacementAlgorithm = algorithm;
    }

    public boolean allocatePages(int processId, int numPages) {
        if (getAvailableFrames() < numPages) {
            return false;
        }

        List<Page> pages = new ArrayList<>();
        for (int i = 0; i < numPages; i++) {
            Page page = new Page(processId, i);
            pages.add(page);
            // Don't allocate frames immediately, let accessPage handle it
            page.setFrameNumber(-1);
        }
        processPages.put(processId, pages);
        return true;
    }

    public boolean accessPage(int processId, int pageNumber) {
        List<Page> pages = processPages.get(processId);
        if (pages == null || pageNumber >= pages.size()) {
            return false;
        }

        Page page = pages.get(pageNumber);
        
        // If page is not in memory, it's a page fault
        if (page.getFrameNumber() == -1) {
            pageFaults++;
            System.out.println("Page fault occurred for Process " + processId + ", Page " + pageNumber);
            
            if (getAvailableFrames() == 0) {
                // Need to replace a page
                int frameToReplace = selectFrameToReplace();
                if (frameToReplace != -1) {
                    // Remove old page from its frame
                    if (frames[frameToReplace] != null) {
                        frames[frameToReplace].setFrameNumber(-1);
                    }
                    frames[frameToReplace] = page;
                    page.setFrameNumber(frameToReplace);
                    page.updateAccessTime();
                    updateReplacementData(page);
                }
            } else {
                // Find first available frame
                for (int i = 0; i < frames.length; i++) {
                    if (frames[i] == null) {
                        frames[i] = page;
                        page.setFrameNumber(i);
                        page.updateAccessTime();
                        updateReplacementData(page);
                        break;
                    }
                }
            }
        } else {
            // Page is already in memory
            page.updateAccessTime();
            updateReplacementData(page);
        }
        return true;
    }

    public boolean freeProcessPages(int processId) {
        List<Page> pages = processPages.remove(processId);
        if (pages == null) {
            return false;
        }

        for (Page page : pages) {
            int frameNumber = page.getFrameNumber();
            if (frameNumber != -1) {
                frames[frameNumber] = null;
                fifoQueue.remove(frameNumber);
                pageAccessTimes.remove(frameNumber);
            }
        }
        return true;
    }

    private void allocateFrame(Page page) {
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] == null) {
                frames[i] = page;
                page.setFrameNumber(i);
                updateReplacementData(page);
                break;
            }
        }
    }

    private int selectFrameToReplace() {
        switch (replacementAlgorithm) {
            case FIFO:
                return fifoQueue.poll();
            case LRU:
                return findLRUFrame();
            default:
                return -1;
        }
    }

    private int findLRUFrame() {
        long oldestTime = Long.MAX_VALUE;
        int oldestFrame = -1;
        for (Map.Entry<Integer, Long> entry : pageAccessTimes.entrySet()) {
            if (entry.getValue() < oldestTime) {
                oldestTime = entry.getValue();
                oldestFrame = entry.getKey();
            }
        }
        return oldestFrame;
    }

    private void updateReplacementData(Page page) {
        int frameNumber = page.getFrameNumber();
        if (replacementAlgorithm == PageReplacementAlgorithm.FIFO) {
            if (!fifoQueue.contains(frameNumber)) {
                fifoQueue.add(frameNumber);
            }
        } else if (replacementAlgorithm == PageReplacementAlgorithm.LRU) {
            pageAccessTimes.put(frameNumber, page.getLastAccessTime());
        }
    }

    public void printMemoryStatus() {
        System.out.println("\nPhysical Memory Frames:");
        for (int i = 0; i < frames.length; i++) {
            System.out.printf("Frame %d: %s%n", i, frames[i] != null ? frames[i] : "Empty");
        }

        System.out.println("\nProcess Pages:");
        for (Map.Entry<Integer, List<Page>> entry : processPages.entrySet()) {
            System.out.printf("Process %d:%n", entry.getKey());
            for (Page page : entry.getValue()) {
                System.out.printf("  %s%n", page);
            }
        }
    }
} 