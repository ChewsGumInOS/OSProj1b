//Long-Term Scheduler.
//Loads first 4 frames from all 30 jobs into memory.

public class LongScheduler {

    //load processes into memory from disk.
    //these processes are "Ready" to be run.
    public static void schedule () {
        //FIFO      - readyQueue is first-in, first-out.
        //PRIORITY  - readyQueue sorted by priority (16 = highest priority, 0 = lowest)
        //SJF       - readyQueue sorted by jobSize: shortest job first.
        //if (Driver.policy == Driver.PRIORITY) {
        //    Queues.diskQueue.sort((PCB o1, PCB o2) -> o2.priority - o1.priority);
        //} else if (Driver.policy == Driver.SJF) {
        //    Queues.diskQueue.sort((PCB o1, PCB o2) -> o1.getJobSizeInMemory() - o2.getJobSizeInMemory());
        //}

        int memCounter = 0;  //current mem location being written to
        int diskCounter; //current disk location being read
        PCB currPCB;

        try {
            //keep reading jobs into memory until no more jobs to load.
            while (!Queues.diskQueue.isEmpty()) {

                currPCB = Queues.diskQueue.pop();  //read the top-most job on the disk table.
                Queues.readyQueue.add(currPCB);
                currPCB.status = PCB.state.READY;
                diskCounter = currPCB.memories.disk_base_register;

                for (int i = 0; i < 4; i++) {  //copy first 4 frames of job into memory, from disk.
                    System.arraycopy(MemorySystem.disk.diskArray[diskCounter], 0, MemorySystem.memory.memArray[memCounter], 0, 4);
                    //MemorySystem.memory.freeFramesList.pop();           //update freeFramesList (frame of memory has been filled)
                    MemorySystem.memory.freeFrameList.take();           //update freeFramesList (frame of memory has been filled)
                    currPCB.memories.pageTable[i][PCB.PAGE_NUM] = memCounter;      //update page table
                    currPCB.memories.pageTable[i][PCB.VALID] = 1;               //set to valid.
                    diskCounter++;
                    memCounter++;
                }
                currPCB.trackingInfo.waitStartTime = System.currentTimeMillis();
            }
        }
        catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }

        //for (PCB thisPCB : Queues.readyQueue) {
        //    System.out.println(Arrays.deepToString(thisPCB.memories.pageTable));
        //}
    }
}
