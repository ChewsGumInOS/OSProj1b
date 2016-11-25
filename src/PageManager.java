import java.util.ListIterator;

public class PageManager implements Runnable {

    public void run () {

        PageRequest pageRequest;
        int shutdownCheck;

        int jobFrame;
        int diskCounter;

        int freeFrame;

        try {
            while (!Thread.currentThread().isInterrupted()) {
                pageRequest = Queues.pageRequestQueue.take();

                //this is how the Driver shuts down the PageManager after all jobs are processed.
                shutdownCheck = pageRequest.pageNumber;
                if (shutdownCheck == -1) {
                    return;
                }

                //load the requested page into memory.
                synchronized (Queues.frameListLock) {
                    freeFrame = MemorySystem.memory.freeFramesList.pop();
                }

                diskCounter = pageRequest.jobPCB.memories.disk_base_register;
                jobFrame = pageRequest.pageNumber;

                System.arraycopy(MemorySystem.disk.diskArray[diskCounter+jobFrame], 0, MemorySystem.memory.memArray[freeFrame], 0, 4);
                pageRequest.jobPCB.memories.pageTable[jobFrame][0] = freeFrame;      //update page table
                pageRequest.jobPCB.memories.pageTable[jobFrame][1] = 1;               //set to valid.

                //now that the page has been loaded into memory,
                //move the job out of the waiting queue, and to the top of the ready queue.
                synchronized (Queues.queueLock) {
                    Queues.waitingQueue.remove(pageRequest.jobPCB);

                    if (Queues.readyQueue.isEmpty()) {
                        Queues.readyQueue.addFirst(pageRequest.jobPCB);
                        synchronized (Queues.waitForPageManagerLock) {      //notify Scheduler that job it was waiting for, is now available on the readyQueue.
                            Queues.waitForPageManagerLock.notify();
                        }
                    }
                    else {
                        boolean inserted = false;
                        int insertIndex = 0;

                        switch (Driver.policy) {
                            case Driver.PRIORITY:
                                //insert job into proper position in readyQueue.
                                for (PCB thisPCB : Queues.readyQueue) {
                                    if (pageRequest.jobPCB.priority > thisPCB.priority) {
                                        Queues.readyQueue.add(insertIndex, pageRequest.jobPCB);
                                        inserted = true;
                                        break;
                                    }
                                    else
                                        insertIndex++;
                                }
                                if (!inserted) {
                                    Queues.readyQueue.addLast(pageRequest.jobPCB);
                                }
                                break;
                            case Driver.SJF:
                                //insert job into proper position in readyQueue.
                                for (PCB thisPCB : Queues.readyQueue) {
                                    if (pageRequest.jobPCB.getJobSizeInMemory() < thisPCB.getJobSizeInMemory()) {
                                        Queues.readyQueue.add(insertIndex, pageRequest.jobPCB);
                                        inserted = true;
                                        break;
                                    }
                                    else
                                        insertIndex++;
                                }
                                if (!inserted) {
                                    Queues.readyQueue.addLast(pageRequest.jobPCB);
                                }
                                break;
                            default:  //FIFO
                                //insert job into proper position in readyQueue.
                                for (PCB thisPCB : Queues.readyQueue) {
                                    if (pageRequest.jobPCB.order < thisPCB.order) {
                                        Queues.readyQueue.add(insertIndex, pageRequest.jobPCB);
                                        inserted = true;
                                        break;
                                    }
                                    else
                                        insertIndex++;
                                }
                                if (!inserted) {
                                    Queues.readyQueue.addLast(pageRequest.jobPCB);
                                }
                                break;
                        }
                    }

                }
            }
        }

        catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }

    }
}
