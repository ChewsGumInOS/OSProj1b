import java.util.Arrays;

public class ScheduleAndDispatch {

    //to schedule a job:

    //1) wait for a free CPU.
    //2) grab a job off the readyQueue.
    //3) but, if the readyQueue is empty, wait for the pageManager to release a job from waitingQueue.
    //4) but, if BOTH readyQueue and waitingQueue are empty,
    //      don't let the CPU run again!!!  (doneFlag checks this.)


    //CPUs: takes from activeCpuQueue.  CPUs wait till this signal sent to begin processing.

    //scheduler/driver: takes from freeCpuQueue.  waits until a CPU is free.
    //but should the scheduler just take a free CPU, no matter what??
    //shouldn't it check if there are jobs to run first, before taking from the cpuQueue?



    public static void schedule () {
        try {
            //wait for a free CPU, then dispatch it to do stuff.
            Integer currFreeCPU = Queues.freeCpuQueue.take();
            dispatch(Driver.cpu[currFreeCPU]);

        } catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }
    }

    public static void dispatch (CPU cpu) {

        PCB currJob = null;
        boolean waitForPageManager = false;
        boolean doneFlag = false;

        try {
            synchronized (Queues.queueLock) {
                if (!Queues.readyQueue.isEmpty())
                    currJob = Queues.readyQueue.pop();
                else {
                    if (!Queues.waitingQueue.isEmpty()) {
                        waitForPageManager = true;
                    }
                    else {
                        //both readyQueue and waitingQueue are empty.
                        //there are no more jobs to process.
                        doneFlag = true;
                    }
                }
            }

            //wait for the PageManager to release a job to the readyQueue.
            if (waitForPageManager) {
                synchronized (Queues.waitForPageManagerLock) {
                    Queues.waitForPageManagerLock.wait();
                    synchronized (Queues.queueLock) {
                        currJob = Queues.readyQueue.pop();
                    }
                }
            }

            if (!doneFlag) {
                ////////perform context switch - load the CPU with the next job./////////////////////////

                cpu.currPCB = currJob;
                cpu.pc = currJob.pc;
                System.arraycopy(currJob.registers, 0, cpu.reg, 0, currJob.registers.length);

                currJob.trackingInfo.runStartTime = System.nanoTime();

                //copy the job's loaded pages into the cache
                cpu.cache.clearCache();
                int currPage;
                for (int i = 0; i < PCB.TABLE_SIZE; i++) {
                    if (currJob.memories.pageTable[i][1] == 1) {
                        currPage = currJob.memories.pageTable[i][0];
                        System.arraycopy(MemorySystem.memory.memArray[currPage], 0, cpu.cache.arr[i], 0, 4);
                        cpu.cache.valid[i] = true;
                    }
                }
                Queues.cpuActiveQueue[cpu.cpuId].put(1);
            }
            else {
                //Queues.cpuDone[cpu.cpuId].put(1);
            }
        } catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }
    }

    //save PCB info from CPU back into PCB
    public static synchronized void save(CPU cpu) {

        PCB currJob = cpu.currPCB;
        currJob.pc = cpu.pc;
        System.arraycopy (cpu.reg, 0, currJob.registers, 0, currJob.registers.length);


        if (currJob.goodFinish) {
            /////////////////////////////////////////////////////////////////////////////////
            //  Job successfully completed - so free frames, add to DoneQueue, save output buffers.
            /////////////////////////////////////////////////////////////////////////////////
            if (Driver.logging)
                currJob.trackingInfo.buffers = cpu.outputResults();

            //free frames
            synchronized (Queues.frameListLock) {
                int frameToFree;
                for (int i=0; i<PCB.TABLE_SIZE; i++) {
                    if (currJob.memories.pageTable[i][1] == 1) {
                        currJob.memories.pageTable[i][1] = 0;
                        frameToFree = currJob.memories.pageTable[i][0];
                        Arrays.fill(MemorySystem.memory.memArray[frameToFree],0);
                        MemorySystem.memory.freeFramesList.addLast(frameToFree);
                    }
                }
            }
            currJob.trackingInfo.runEndTime = System.nanoTime();

            synchronized (Queues.doneQueue) {
                if (!Queues.doneQueue.contains(currJob))
                    Queues.doneQueue.add(currJob);
                else {
                    System.err.println("Warning: Trying to add a job back into the done Queue again.");
                }
            }
        }

        //if job was not successfully completed, but it on the waiting queue for the PageManager.
        else {
            synchronized (Queues.queueLock) {
                Queues.waitingQueue.add(currJob);
            }
        }
    }
}
