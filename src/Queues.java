import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class Queues {

    final static int NUM_JOBS = 30;     //used for sizing priority queues.

    final static Object FrameFreerShutDownLock = new Object();
    final static Object PageManagerShutDownLock = new Object();


    static LinkedList<PCB> diskQueue;
    static PriorityBlockingQueue<PCB> readyQueue;
    static PriorityBlockingQueue<PCB> waitingQueue;
    static SynchronousQueue<PCB> [] runningQueues;
    static LinkedBlockingQueue<PCB> doneQueue;       //for reporting purposes, to track complete jobs

    static LinkedBlockingQueue<Integer> freeCpuQueue;

    static LinkedBlockingQueue<PCB> freeFrameRequestQueue;

    static SynchronousQueue<PCB> ioWaitQueue;
    static SynchronousQueue<Integer> [] ioDoneQueue;  //signals CPU that IO is done, and can continue.



    static public void initQueues () {

        diskQueue = new LinkedList<>();
        doneQueue = new LinkedBlockingQueue<>();

        //runningQueues = array of 4 queues, of size 1.
        //cpu wait for entry, then runs.
        //Driver loads with -1 to shutdown the CPU's.
        runningQueues = new SynchronousQueue[CPU.CPU_COUNT];

        ioWaitQueue = new SynchronousQueue();
        ioDoneQueue = new SynchronousQueue[CPU.CPU_COUNT];

        //freeCpuQueue: 4 free CPU's initially.
        //scheduler keeps removing free cpu's as they are assigned - scheduler blocks at 0.
        freeCpuQueue = new LinkedBlockingQueue<>();

        freeFrameRequestQueue = new LinkedBlockingQueue<>();

        for (int i = 0 ; i < CPU.CPU_COUNT; i++) {
            freeCpuQueue.add(i);
            runningQueues[i] = new SynchronousQueue();
            ioDoneQueue[i] = new SynchronousQueue<>();
        }

        Comparator<PCB> comparatorPCB;
        switch (Driver.policy) {
            case (Driver.PRIORITY):
                comparatorPCB = (PCB o1, PCB o2)-> o2.priority - o1.priority;
            break;
            case (Driver.SJF):
                comparatorPCB = (PCB o1, PCB o2) -> o1.jobSizeInMemory - o2.jobSizeInMemory;
            break;
            default:     //FIFO
                comparatorPCB = (PCB o1, PCB o2) -> o1.order - o2.order;
            break;
        }
        readyQueue = new PriorityBlockingQueue(NUM_JOBS, comparatorPCB);
        waitingQueue = new PriorityBlockingQueue(NUM_JOBS, comparatorPCB);

    }
}
