import java.util.concurrent.TimeUnit;

public class PageManager implements Runnable {

    public void run () {

        PCB currJob;
        int diskCounter;
        int freeFrame;
        long currTime;

        try {
            while (!Thread.currentThread().isInterrupted()) {

                currJob = Queues.waitingQueue.take();

                //this is how the Driver shuts down the PageManager after all jobs are processed.
                if (currJob.jobId == -1) {
                    synchronized (Queues.PageManagerShutDownLock) {
                        Queues.PageManagerShutDownLock.notify();  //let the driver know PageManager is done.
                    }
                    return;
                }

                //get a free frame of memory.
                freeFrame = MemorySystem.memory.freeFrameList.take();

                currJob.trackingInfo.currStartFaultServiceTime = System.currentTimeMillis();

                diskCounter = currJob.memories.disk_base_register;
                //copy the desired page from the disk to memory.
                System.arraycopy(MemorySystem.disk.diskArray[diskCounter + currJob.pageFaultFrame], 0, MemorySystem.memory.memArray[freeFrame], 0, 4);
                TimeUnit.NANOSECONDS.sleep(CPU.PAGE_FAULT_DELAY);  //delay to simulate disk access.

                currJob.memories.pageTable[currJob.pageFaultFrame][PCB.PAGE_NUM] = freeFrame;      //update page table
                currJob.memories.pageTable[currJob.pageFaultFrame][PCB.VALID] = 1;               //set to valid.

                //now that the page has been loaded into memory,
                //move the job out of the waitingQueue, and to the top of the readyQueue.
                Queues.readyQueue.put(currJob);

                currJob.trackingInfo.pageFaults++;
                currTime = System.currentTimeMillis();
                currJob.trackingInfo.totalFaultServiceTime += (currTime -
                        currJob.trackingInfo.currStartFaultServiceTime);
                currJob.trackingInfo.totalTimeOnWaitingQueue += (currTime -
                        currJob.trackingInfo.startedWaitingAgainTime);
                currJob.trackingInfo.addStopTime(currTime);
            }
        }
        catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }

    }
}
