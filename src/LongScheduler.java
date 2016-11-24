//Long-Term Scheduler.

//Loads first 4 frames from all 30 jobs into memory.

import java.util.Arrays;

public class LongScheduler {


    final static int FIFO = 1;
    final static int PRIORITY = 2;
    final static int SJF = 3;

    int algorithm = FIFO;                       //current scheduling algorithm; FIFO is default.

    public LongScheduler (int policy) {
        this.algorithm = policy;
    }

    //load processes into memory from disk.
    //these processes are "Ready" to be run.
    public void schedule () {
        //FIFO      - readyQueue is first-in, first-out.
        //PRIORITY  - readyQueue sorted by priority (16 = highest priority, 0 = lowest)
        //SJF       - readyQueue sorted by jobSize: shortest job first.
        if (algorithm == PRIORITY) {
            Queues.diskQueue.sort((PCB o1, PCB o2) -> o2.priority - o1.priority);
        } else if (algorithm == SJF) {
            Queues.diskQueue.sort((PCB o1, PCB o2) -> o1.getJobSizeInMemory() - o2.getJobSizeInMemory());
        }

        int memCounter = 0;  //current mem location being written to
        int diskCounter; //current disk location being read

        PCB currPCB;

        //keep reading jobs into memory until no more jobs to load.
        while (!Queues.diskQueue.isEmpty()) {

            currPCB = Queues.diskQueue.pop();  //read the top-most job on the disk table.
            Queues.readyQueue.add(currPCB);

            diskCounter = currPCB.memories.disk_base_register;

            for (int i = 0; i < 4; i++) {  //copy first 4 frames of job into memory, from disk.
                System.arraycopy(MemorySystem.disk.diskArray[diskCounter], 0, MemorySystem.memory.memArray[memCounter], 0, 4);
                MemorySystem.memory.freeFramesList.pop();           //update freeFramesList (frame of memory has been filled)
                currPCB.memories.pageTable[i][0] = memCounter;      //update page table
                currPCB.memories.pageTable[i][1] = 1;               //set to valid.
                diskCounter++;
                memCounter++;
            }
            currPCB.trackingInfo.waitStartTime = System.nanoTime();
        }

        //for (PCB thisPCB : Queues.readyQueue) {
        //    System.out.println(Arrays.deepToString(thisPCB.memories.pageTable));
        //}
    }


    //sum up memory usage of all the jobs currently in memory (readyQueue and waitingQueue).
    public int calcMemUsage () {
        int counter = 0;
        for (PCB thisPCB : Queues.readyQueue) {
            counter += thisPCB.getJobSizeInMemory();
        }
        for (PCB thisPCB : Queues.waitingQueue) {
            counter += thisPCB.getJobSizeInMemory();
        }
        return counter;
    }
}
