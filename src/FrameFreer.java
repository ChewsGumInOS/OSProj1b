import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class FrameFreer implements Runnable {

    //public void run () {
    //}


    public void run () {

        PCB currJob;
        int diskCounter;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                currJob = Queues.freeFrameRequestQueue.take();

                //this is how the Driver shuts down the FrameFreer after all jobs are processed.
                if (currJob.jobId == -1) {
                    synchronized (Queues.FrameFreerShutDownLock) {
                        Queues.FrameFreerShutDownLock.notify();  //let the driver know FrameFreer is done.
                    }
                    return;
                }

                //job is done. copy frames back to disk if modified, and free frames.
                int frameToFree;
                for (int i=0; i<PCB.TABLE_SIZE; i++) {
                    if (currJob.memories.pageTable[i][PCB.VALID] == 1) {
                        frameToFree = currJob.memories.pageTable[i][PCB.PAGE_NUM];

                        if (currJob.memories.pageTable[i][PCB.MODIFIED] == 1) { //check frame was modified
                            diskCounter = currJob.memories.disk_base_register + i;
                            //copy the page from memory, back to disk.
                            System.arraycopy(MemorySystem.memory.memArray[frameToFree], 0, MemorySystem.disk.diskArray[diskCounter], 0, MemorySystem.PAGE_SIZE);
                            TimeUnit.NANOSECONDS.sleep(CPU.DISK_ACCESS_DELAY);  //delay to simulate disk access.
                        }
                        currJob.memories.pageTable[i][PCB.VALID] = 0;
                        Arrays.fill(MemorySystem.memory.memArray[frameToFree],0);
                        MemorySystem.memory.freeFrameList.put(frameToFree);
                    }
                }
            }
        } catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }
    }
}
