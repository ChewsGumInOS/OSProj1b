//Process Control Block
public class PCB {

    public static final int TABLE_SIZE = 20;

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

    //int cpuId;

    Memories memories;
    TrackingInfo trackingInfo;

    public enum state {NEW, READY, RUNNING, WAITING, COMPLETE};
    state status;  //note: this variable can be safely deleted, and is here for convenience - simulator uses the queues to track the status of the job.


    public PCB() {
        memories = new Memories();
        registers = new int [CPU.NUM_REGISTERS];
        trackingInfo = new TrackingInfo();
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

    long waitStartTime;             //time entered Ready Queue (set by Long Term Scheduler)
    long runStartTime;              //time first started executing (entered Running Queue, set by Dispatcher)
    long runEndTime;                //Completion Time = runEndTime - waitStartTime?


    //Execution Time: runEndTime - runStartTime?
    /*
    //below fields necessary for context-switching - each time we go from Running->Ready->Running etc, we
    //need to track the times.
    long startedWaitingAgainTime;   //time entered Ready Queue again
    long startedRunningAgainTime;   //time starting Executing again

    long totalTimeWaiting;          //sum of the periods spent waiting in the ready queue
                                    //(timeWaitingInReadyQueue += startingRunningAgainTime - startedWaitingAgainTime)
    */

}