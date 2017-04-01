import java.util.concurrent.TimeUnit;

public class DMA implements Runnable {

    public void run() {
        handleDMA();
    }

    public DMA() {}

    public void handleDMA() {
        try {
            int currPage;
            int currIOcount = 0;
            PCB currJob;
            CPU currCpu;

            while (!Thread.currentThread().isInterrupted()) {

                currJob = Queues.ioWaitQueue.take();
                //this is how the Driver shuts down the DMA thread after all jobs are processed.
                if (currJob.jobId == -1) {
                    return;
                }

                currCpu = Driver.cpu[currJob.cpuId];
                for (int i = 0; i < CPU.CACHE_SIZE; i++) {
                    if (currCpu.cache.valid[i]) {
                        for (int j = 0; j < MemorySystem.PAGE_SIZE; j++) {
                            if (currCpu.cache.modified[i][j]) {
                                currPage = currJob.memories.pageTable[i][PCB.PAGE_NUM];
                                MemorySystem.memory.writeMemoryAddress(currPage, j, currCpu.cache.arr[i][j]);
                                currJob.trackingInfo.ioCounter++;       //update *total* IO count.
                                currIOcount++;
                                currJob.memories.pageTable[i][PCB.MODIFIED] = 1;   //set pagetable "dirty" indicator; must be written back to disk.
                            }
                        }
                    }
                }
                TimeUnit.NANOSECONDS.sleep(CPU.DMA_DELAY * currIOcount);

                Queues.ioDoneQueue[currJob.cpuId].put(1); //signal the CPU that IO to memory is done; proceed.
            }
        }
        catch (InterruptedException ie) {
            System.err.println("Invalid DMA handler interruption: " + ie.toString());
        }
    }
}

