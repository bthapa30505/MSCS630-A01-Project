package org.shellassignment.memory;

public class Page {
    private final int processId;
    private final int pageNumber;
    private long lastAccessTime;
    private int frameNumber;

    public Page(int processId, int pageNumber) {
        this.processId = processId;
        this.pageNumber = pageNumber;
        this.lastAccessTime = System.currentTimeMillis();
        this.frameNumber = -1; // Not yet allocated to a frame
    }

    public int getProcessId() {
        return processId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void updateAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
    }

    @Override
    public String toString() {
        return String.format("Process %d, Page %d, Frame %d", processId, pageNumber, frameNumber);
    }
} 