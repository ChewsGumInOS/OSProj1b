import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class Queues {

    final static Object queueLock = new Object();
    final static Object frameListLock = new Object();
    final static Object waitForPageManagerLock = new Object();
    final static Object waitForFrameFreerLock = new Object();

    static LinkedList<PCB> diskQueue;
    static LinkedList<PCB> readyQueue;
    static LinkedList<PCB> waitingQueue;
    static LinkedList<PCB> doneQueue;       //for reporting purposes, to track complete jobs

    //static List readyQueue = Collections.synchronizedList(new LinkedList<PCB>());
    //static List waitingQueue = Collections.synchronizedList(new LinkedList<PCB>());
    //static LinkedList<PCB>[] runningQueues;

    static LinkedBlockingQueue<Integer> freeCpuQueue;
    static SynchronousQueue<Integer>[] cpuActiveQueue;
    static LinkedBlockingQueue<PageRequest> pageRequestQueue;
    static LinkedBlockingQueue<PCB> freeFrameRequestQueue;

    //static SynchronousQueue<Integer>[] cpuDone;

    static public void initQueues () {

        diskQueue = new LinkedList<>();
        readyQueue = new LinkedList<>();
        waitingQueue = new LinkedList<>();
        doneQueue = new LinkedList<>();
        //runningQueues = new LinkedList[CPU.CPU_COUNT];

        //freeCpuQueue: initialized with size 4, because 4 free CPU's.
        //scheduler keeps removing free cpu's as they are assigned - scheduler blocks at 0.
        freeCpuQueue = new LinkedBlockingQueue<>(CPU.CPU_COUNT);

        //cpuActiveQueue: array of 4 activeQueues, of size 1.
        //dispatcher fills it to signal CPU.  cpu waits for it and takes, then runs.
        //Driver loads with -1 to shutdown the CPU's.
        cpuActiveQueue = new SynchronousQueue[CPU.CPU_COUNT];

        //cpuDone = new SynchronousQueue[CPU.CPU_COUNT];

        pageRequestQueue = new LinkedBlockingQueue<>();
        freeFrameRequestQueue = new LinkedBlockingQueue<>();

        for (int i = 0 ; i < CPU.CPU_COUNT; i++) {
            //runningQueues[i] = new LinkedList<>();
            freeCpuQueue.add(i);
            cpuActiveQueue[i] = new SynchronousQueue();
            //cpuDone[i] = new SynchronousQueue();
        }
    }
}
