import java.util.LinkedList;

//Process Control Block
public class PCB {

    public static final int TABLE_SIZE = 20;
    public static final int PAGE_NUM = 0;
    public static final int VALID = 1;
    public static final int MODIFIED = 2;

    int order;  //used by FIFO - tracks who came in first.

    //job card info
    int jobId;
    int codeSize;
    int priority;

    //data control card info
    int inputBufferSize;
    int outputBufferSize;
    int tempBufferSize;

    int pc;     // the job’s pc holds the address of the instruction to fetch
    int [] registers;

    boolean goodFinish;
    boolean firstRun = true;
    int pageFaultFrame;     //frame that pageManager needs to load.


    Memories memories;
    TrackingInfo trackingInfo;

    public enum state {NEW, READY, RUNNING, WAITING, COMPLETE};
    state status;  //note: this variable can be safely deleted, and is here for convenience - simulator uses the queues to track the status of the job.


    public PCB() {
        memories = new Memories();
        registers = new int [CPU.NUM_REGISTERS];
        trackingInfo = new TrackingInfo();
    }

    public PCB(int jobId) { //used to quickly construct a dummy PCB with a jobId of -1.
        this.jobId = jobId;
    }


    public String toString() {
        String record = "jobID: " + jobId + ".\t "
                + "codeSize: " + codeSize + ".\t"
                + "priority: " + priority + ".\t"
                + "inputBufferSize:" + inputBufferSize + ".\t"
                + "outputBufferSize:" + outputBufferSize + ".\t"
                + "tempBufferSize:" + tempBufferSize + ".\t"
                + "pc:" + pc + ".\t";
        return record;
    }

    public int getJobSizeInMemory() {
        return codeSize + inputBufferSize + outputBufferSize + tempBufferSize;
    }

}

class Memories {

    int disk_base_register;  //starting page of the job's code on disk
    int disk_data_base_reg;  //starting page of the job's data on disk.

    int[][] pageTable;      //pageTable column 0: pageNumber.  column 1: valid (1) or invalid (0).
                            //column 2: modified.

    public Memories() {
        pageTable = new int[PCB.TABLE_SIZE][3];
    }
}

class TrackingInfo {


    volatile int ioCounter;                  //number of io operations each process made
    String buffers;                 //at job completion, output buffers written to this String.

    int pageFaults;


    long waitStartTime;             //time entered Ready Queue (set by Long Term Scheduler)
    long runStartTime;              //runStartTime  - time first started executing (set by CPU)
    long runEndTime;                //Completion Time = runEndTime - waitStartTime?

    long currStartFaultServiceTime;
    long totalFaultServiceTime;  //sum of currStartFaultServiceTime - currEndFaultServiceTime

    //waiting again?
    long startedWaitingAgainTime;   //time entered Waiting Queue.
    long totalTimeOnWaitingQueue;   //+= System.time() time exited Waiting Queue -startedWaitingAgainTime
    //Execution Time: runEndTime - runStartTime - timesWaiting?

    LinkedList<WaitTimes> waitTimes;

    public TrackingInfo() {
        waitTimes = new LinkedList<>();
    }

    public void addStartTime(long start) {
        waitTimes.add(new WaitTimes(start));
    }

    public void addStopTime(long stop) {
        waitTimes.getLast().stop = stop;
        waitTimes.getLast().diff = stop - waitTimes.getLast().start;
    }

    class WaitTimes {
        long start;
        long stop;
        long diff;

        WaitTimes(long start) {
            this.start = start;
        }
    }
}


