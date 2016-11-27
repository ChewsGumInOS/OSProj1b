import java.io.FileNotFoundException;

public class TimingData {

    int iterations;
    int numJobs;

    static final int FACTOR = 1;  //data is in ms already
    static final String FACTOR_STRING = "Data is in milliseconds (ms)";

    static final String TITLE_STRING = "Job#,NumPageFaults,AvgWaitTime,AvgCompletionTime,FirstWait,2ndWait,FaultServiceTotal,";

    static final int NUM_FIELDS = 8;    //4 columns: jobId, number of page faults, Waiting Time, Completion Time, WaitStart

    static final int JOBID = 0;
    static final int NUM_FAULTS = 1;
    static final int WAITING_TIME = 2;
    static final int COMPLETION_TIME = 3;
    static final int FIRST_WAIT = 4;
    static final int SECOND_WAIT = 5;
    static final int FAULT_SERVICE = 6;

    long[][][] timingArray;
    long[][] avgTimingArray;   //just Waiting Time(avg) and Completion Time(avg)


    public TimingData (int iterations, int numJobs) {
        this.iterations = iterations;
        this.numJobs = numJobs;
        //create timing array (if not already created)
        timingArray = new long[iterations][numJobs][NUM_FIELDS];
        avgTimingArray = new long [numJobs][NUM_FIELDS];
    }

    // save Timing Data for current iteration.
    public void saveTimingDataForThisIteration(int i) {
        int j=0; //j = job counter
        for (PCB thisPCB: Queues.doneQueue) {
            //i=iteration counter
            //waitStartTime - time entered Ready Queue (set by Long Term Scheduler)
            //runStartTime  - time first started executing (set by CPU)
            //runEndTime    - Completion Time = runEndTime - waitStartTime?
            timingArray[i][j][JOBID] = thisPCB.jobId;
            timingArray[i][j][NUM_FAULTS] = thisPCB.trackingInfo.pageFaults;
            timingArray[i][j][WAITING_TIME] = Math.round((thisPCB.trackingInfo.runStartTime - thisPCB.trackingInfo.waitStartTime
                    + thisPCB.trackingInfo.totalTimeOnWaitingQueue) / (double) FACTOR);
            timingArray[i][j][COMPLETION_TIME] = Math.round((thisPCB.trackingInfo.runEndTime -
                    thisPCB.trackingInfo.waitStartTime) / (double) FACTOR);
            timingArray[i][j][FIRST_WAIT] = Math.round((thisPCB.trackingInfo.runStartTime - thisPCB.trackingInfo.waitStartTime) / (double) FACTOR);
            timingArray[i][j][SECOND_WAIT] = Math.round(thisPCB.trackingInfo.waitTimes.get(0).diff / (double) FACTOR);
            timingArray[i][j][FAULT_SERVICE] = Math.round((thisPCB.trackingInfo.totalFaultServiceTime) / (double) FACTOR);
            j++;
        }
    }

    // Process and Output Timing Data
    public void outputTimingData() {

        String fileName = "timing.csv";
        java.io.File timingFile = new java.io.File(fileName);

        try {
            java.io.PrintWriter timing = new java.io.PrintWriter(timingFile);

            timing.println(TITLE_STRING + FACTOR_STRING);
            for (int i = 0; i < numJobs; i++) {
                for (int j = 0; j < iterations; j++) {
                    avgTimingArray[i][NUM_FAULTS] += timingArray[j][i][NUM_FAULTS];   //sum page faults times
                    avgTimingArray[i][WAITING_TIME] += timingArray[j][i][WAITING_TIME];   //sum wait times
                    avgTimingArray[i][COMPLETION_TIME] += timingArray[j][i][COMPLETION_TIME];   //sum completion times
                    avgTimingArray[i][FIRST_WAIT] += timingArray[j][i][FIRST_WAIT];   //first wait
                    avgTimingArray[i][SECOND_WAIT] += timingArray[j][i][SECOND_WAIT];   //second wait
                    avgTimingArray[i][FAULT_SERVICE] += timingArray[j][i][FAULT_SERVICE];   //fault service time
                }

                avgTimingArray[i][JOBID] = timingArray[0][i][JOBID];        //job number
                timing.print(avgTimingArray[i][JOBID] + ",");

                avgTimingArray[i][NUM_FAULTS] = Math.round((double) avgTimingArray[i][NUM_FAULTS] / (double) iterations);  //take avg of page faults across the iterations
                timing.print(avgTimingArray[i][NUM_FAULTS] + ",");

                avgTimingArray[i][WAITING_TIME] = Math.round((double) avgTimingArray[i][WAITING_TIME] / (double) iterations);  //take avg of wait times across the iterations
                timing.print(avgTimingArray[i][WAITING_TIME] + ",");

                avgTimingArray[i][COMPLETION_TIME] = Math.round((double) avgTimingArray[i][COMPLETION_TIME] / (double) iterations);  //take avg of completion times across the iterations
                timing.print(avgTimingArray[i][COMPLETION_TIME] + ",");

                avgTimingArray[i][FIRST_WAIT] = Math.round((double) avgTimingArray[i][FIRST_WAIT] / (double) iterations);  //take avg of first wait times across iterations
                timing.print(avgTimingArray[i][FIRST_WAIT] + ",");

                avgTimingArray[i][SECOND_WAIT] = Math.round((double) avgTimingArray[i][SECOND_WAIT] / (double) iterations);
                timing.print(avgTimingArray[i][SECOND_WAIT] + ",");

                avgTimingArray[i][FAULT_SERVICE] = Math.round((double) avgTimingArray[i][FAULT_SERVICE] / (double) iterations);
                timing.print(avgTimingArray[i][FAULT_SERVICE] + ",");

                timing.println();
            }

            //finally calculate avg Wait Time, Completion Time across all jobs.
            timing.println("NumFaults, AvgWaitTime, AvgCompletionTimeAvg, For All Jobs After running " + iterations + " iterations:");

            double avgNumFaults = 0;
            double avgWaitTime = 0;
            double avgCompletionTime = 0;
            //double avgNumTimesInWaitingQueue = 0;

            for (int i = 0; i < numJobs; i++) {
                avgNumFaults+= avgTimingArray[i][NUM_FAULTS];
                avgWaitTime += avgTimingArray[i][WAITING_TIME];
                avgCompletionTime += avgTimingArray[i][COMPLETION_TIME];

            }
            avgNumFaults = avgNumFaults / (double) numJobs;
            avgWaitTime = avgWaitTime / (double) numJobs;
            avgCompletionTime = avgCompletionTime / (double) numJobs;
            timing.println(avgNumFaults + "  ," + Math.round(avgWaitTime) + "," + Math.round(avgCompletionTime) + ",");
            timing.close();

        }
        catch (FileNotFoundException ex){
            System.err.println("Could not open the " + fileName + "file.");
            System.err.println("Please close the " + fileName + "file and try again.");
            //ex.printStackTrace();
        }
    }

}
