public class PageManager implements Runnable {

    public void run () {

        PageRequest pageRequest;
        int shutdownCheck;

        int jobFrame;
        int diskCounter;

        int freeFrame;

        try {
            while (!Thread.currentThread().isInterrupted()) {
                //this is how the Driver shuts down the PageManager after all jobs are processed.
                pageRequest = Queues.pageRequestQueue.take();

                shutdownCheck = pageRequest.address;
                if (shutdownCheck == -1) {
                    return;
                }

                //load the requested page into memory.
                freeFrame = MemorySystem.memory.freeFramesList.pop();

                diskCounter = pageRequest.jobPCB.memories.disk_base_register;
                jobFrame = pageRequest.address/4;

                System.arraycopy(MemorySystem.disk.diskArray[diskCounter+jobFrame], 0, MemorySystem.memory.memArray[freeFrame], 0, 4);

                //find the first empty slot in the page table
                int i=0;
                while (pageRequest.jobPCB.memories.pageTable[i][1] != 0){
                    i++;
                }
                pageRequest.jobPCB.memories.pageTable[i][0] = freeFrame;      //update page table
                pageRequest.jobPCB.memories.pageTable[i][1] = 1;               //set to valid.

                //now that the page has been loaded into memory,
                //move the job out of the waiting queue, and to the top of the ready queue.

                Queues.waitingQueue.remove(pageRequest.jobPCB);
                Queues.readyQueue.addFirst(pageRequest.jobPCB);
            }
        }

        catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }

    }
}
