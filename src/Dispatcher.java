import java.util.Arrays;

public class Dispatcher {

    //The Dispatcher method assigns a process to the CPU.
    // It is also responsible for context switching of jobs when necessary.
    // The dispatcher extracts parameter data from the PCB and accordingly set the CPU’s PC,
    // and other registers, before the OS calls the CPU to execute the job.


    public static synchronized void dispatch(CPU cpu) {

        PCB currJob = Queues.readyQueue.getFirst();

        cpu.currPCB = currJob;
        cpu.pc = currJob.pc;
        System.arraycopy(currJob.registers, 0, cpu.reg, 0, currJob.registers.length);

        currJob.trackingInfo.runStartTime = System.nanoTime();

        //copy the job's loaded pages into the cache
        cpu.cache.clearCache();
        int currPage;
        int cacheCounter = 0;
        for (int i = 0; i < PCB.TABLE_SIZE; i++) {
            if (currJob.memories.pageTable[i][1] == 1) {
                currPage = currJob.memories.pageTable[i][0];
                System.arraycopy(MemorySystem.memory.memArray[currPage], 0, cpu.cache.arr[cacheCounter], 0, 4);
                cpu.cache.valid[cacheCounter] = true;
                cacheCounter++;
            }
        }

        /*
        for (int i = 0; i < CPU.CACHE_SIZE; i++) {
            for (int j = 0; j < 4; j++) {
                System.out.print(Driver.convertIntInstructionToHex(cpu.cache.arr[i][j]) + "\t");
            }
            System.out.println();
        }
        */

        Queues.readyQueue.pop();

        try {
            Queues.cpuActiveQueue[cpu.cpuId].put(1);
        } catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }
    }

    //save PCB info from CPU back into PCB
    public static synchronized void save(CPU cpu) {

        PCB currJob = cpu.currPCB;
        currJob.pc = cpu.pc;
        System.arraycopy (cpu.reg, 0, currJob.registers, 0, currJob.registers.length);

        if (currJob.goodFinish) { //job successfully completed

            /////if (Driver.logging)
                /////currJob.trackingInfo.buffers = cpu.outputResults();
            currJob.trackingInfo.runEndTime = System.nanoTime();
            Queues.doneQueue.add(currJob);
            cpu.currPCB = null;
        }
        else {                  //job being put on waiting queue.
            Queues.waitingQueue.add(currJob);
            cpu.currPCB = null;
        }
        try {
            Queues.freeCpuQueue.put(cpu.cpuId);

        } catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }


    }

}
