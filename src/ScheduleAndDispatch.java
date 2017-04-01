public class ScheduleAndDispatch {

    //to schedule a job:
    //grab a job from the ready queue.
    //grab a free CPU from the free CPU queue.
    //context switch
    //place the job on the CPU's running queue.

    public static boolean scheduleAndDispatch () {
        try {
            PCB currJob = Queues.readyQueue.take();
            //if all jobs are done - cpu will send a signal that it's time to stop.
            if (currJob.jobId == -1) {
                return false;
            }

            //wait for a free CPU, then dispatch it to do stuff.
            Integer currFreeCPU = Queues.freeCpuQueue.take();
            CPU cpu = Driver.cpu[currFreeCPU];
            currJob.cpuId = cpu.cpuId;  //record which CPU is being assigned the job in the PCB.

            /////////////////////////////////////////////////////////////////////////////////
            //                        Begin Loading CPU with job context info
            /////////////////////////////////////////////////////////////////////////////////

            cpu.pc = currJob.pc;
            System.arraycopy(currJob.registers, 0, cpu.reg, 0, currJob.registers.length);

            //copy the job's loaded pages into the cache
            cpu.cache.clearCache();
            int currPage;
            for (int i = 0; i < PCB.TABLE_SIZE; i++) {
                if (currJob.memories.pageTable[i][PCB.VALID] == 1) {
                    currPage = currJob.memories.pageTable[i][PCB.PAGE_NUM];
                    System.arraycopy(MemorySystem.memory.memArray[currPage], 0, cpu.cache.arr[i], 0, MemorySystem.PAGE_SIZE);
                    cpu.cache.valid[i] = true;
                }
            }
            /////////////////////////////////////////////////////////////////////////////////
            //                        End Loading CPU with job context info
            /////////////////////////////////////////////////////////////////////////////////

            Queues.runningQueues[cpu.cpuId].put(currJob);
        } catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }
        return true;
    }


}
