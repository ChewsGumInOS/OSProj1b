import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class Queues {

    static LinkedList<PCB> diskQueue;
    static LinkedList<PCB> readyQueue;
    static LinkedList<PCB> waitingQueue;    //not used yet
    static LinkedList<PCB> doneQueue;       //for reporting purposes, to track complete jobs


    static LinkedBlockingQueue<Integer> freeCpuQueue;

    static SynchronousQueue<Integer>[] cpuActiveQueue;

    static LinkedBlockingQueue<PageRequest> pageRequestQueue;


    static public void initQueues () {
        diskQueue = new LinkedList<>();
        readyQueue = new LinkedList<>();
        waitingQueue = new LinkedList<>();
        doneQueue = new LinkedList<>();
        //while running, job's PCB is "held" by the CPU it is running on (cpu.currPCB)

        //freeCpuQueue: initialized with size 4, because 4 free CPU's.
        //scheduler keeps removing free cpu's as they are assigned - scheduler blocks at 0.
        freeCpuQueue = new LinkedBlockingQueue<>(CPU.CPU_COUNT);

        //cpuActiveQueue: array of 4 activeQueues, of size 1.
        //dispatcher fills it to signal CPU.  cpu waits for it and takes, then runs.
        //Driver loads with -1 to shutdown the CPU's.
        cpuActiveQueue = new SynchronousQueue[CPU.CPU_COUNT];


        for (int i = 0 ; i < CPU.CPU_COUNT; i++) {
            freeCpuQueue.add(i);
            cpuActiveQueue[i] = new SynchronousQueue();
        }
    }
}
