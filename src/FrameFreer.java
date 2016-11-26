import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class FrameFreer implements Runnable {

    public void run () {

        PCB currJob;
        int diskCounter;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                currJob = Queues.freeFrameRequestQueue.take();

                //this is how the Driver shuts down the PageManager after all jobs are processed.
                if (currJob.jobId == -1) {
                    synchronized (Queues.waitForFrameFreerLock) {
                        Queues.waitForFrameFreerLock.notify();  //let the driver know FrameFreer is done.
                    }
                    return;
                }

                //copy frames back to disk if modified, and free frames.
                synchronized (Queues.frameListLock) {
                    int frameToFree;
                    for (int i=0; i<PCB.TABLE_SIZE; i++) {
                        if (currJob.memories.pageTable[i][1] == 1) {
                            frameToFree = currJob.memories.pageTable[i][0];

                            if (currJob.memories.pageTable[i][2] == 1) { //modified/dirty set
                                diskCounter = currJob.memories.disk_base_register + i;
                                //copy the page from memory, back to disk.
                                System.arraycopy(MemorySystem.memory.memArray[frameToFree], 0, MemorySystem.disk.diskArray[diskCounter], 0, 4);
                                TimeUnit.NANOSECONDS.sleep(CPU.DISK_ACCESS_DELAY);  //delay to simulate disk access.
                            }
                            currJob.memories.pageTable[i][1] = 0;

                            Arrays.fill(MemorySystem.memory.memArray[frameToFree],0);
                            //MemorySystem.memory.freeFramesList.addLast(frameToFree);
                            MemorySystem.memory.freeFrameList.put(frameToFree);
                        }
                    }
                }
            }
        } catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }
    }
}
