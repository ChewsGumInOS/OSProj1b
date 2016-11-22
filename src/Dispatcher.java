
public class Dispatcher {

    //The Dispatcher method assigns a process to the CPU.
    // It is also responsible for context switching of jobs when necessary (more on this later!).
    // For now, the dispatcher will extract parameter data from the PCB and accordingly set the CPU’s PC,
    // and other registers, before the OS calls the CPU to execute the job.


    public static synchronized void dispatch(CPU cpu) {
/*
        PCB currJob = Queues.readyQueue.getFirst();

        System.arraycopy(currJob.registers, 0, cpu.reg, 0, currJob.registers.length);

        cpu.jobId = currJob.jobId;
        cpu.pc = currJob.pc;

        cpu.base_reg = currJob.memories.base_register;
        cpu.codeSize = currJob.codeSize;
        cpu.inputBufferSize = currJob.inputBufferSize;
        cpu.outputBufferSize = currJob.outputBufferSize;
        cpu.tempBufferSize = currJob.tempBufferSize;
        cpu.jobSize = currJob.getJobSizeInMemory();
        cpu.goodFinish = currJob.goodFinish;  //set to true when job completes properly (HLT) - this variable may not be necessary.
        cpu.ioCounter = currJob.trackingInfo.ioCounter;

        currJob.trackingInfo.runStartTime = System.nanoTime();

        //copy job into the cache
        cpu.cache.clearCache();
        System.arraycopy(MemorySystem.memory.memArray, cpu.base_reg, cpu.cache.arr, 0, currJob.getJobSizeInMemory());

        Queues.runningQueues[cpu.cpuId].push(Queues.readyQueue.pop());

        try {
            Queues.cpuActiveQueue[cpu.cpuId].put(1);
        } catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }

*/
    }

    //save PCB info from CPU back into PCB
    public static synchronized void save(CPU cpu) {

        PCB currJob = Queues.runningQueues[cpu.cpuId].getFirst();

        System.arraycopy (cpu.reg, 0, currJob.registers, 0, currJob.registers.length);

        currJob.cpuId = cpu.cpuId;

        currJob.pc = cpu.pc;
        currJob.goodFinish = cpu.goodFinish;   //set to true when job completes properly (HLT) - this variable may not be necessary.
        currJob.trackingInfo.ioCounter = cpu.ioCounter;

        if (currJob.goodFinish) { //job successfully completed

            if (Driver.logging)
                currJob.trackingInfo.buffers = cpu.outputResults();
            currJob.trackingInfo.runEndTime = System.nanoTime();

            Queues.runningQueues[cpu.cpuId].pop();
            Queues.doneQueue.add(currJob);
            try {
                Queues.freeCpuQueue.put(cpu.cpuId);

            } catch (InterruptedException ie) {
                System.err.println(ie.toString());
            }

        }
        else {
            System.err.println ("Unexpected end of program.");
            System.exit(-1);
        }

    }

}
