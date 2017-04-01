import java.io.FileNotFoundException;

public class TimingData {

    int iterations;
    int numJobs;

    //static final int FACTOR = 1;  //FACTOR is used if we wanted to convert data to nanoseconds, etc
    static final String FACTOR_STRING = "Data is in milliseconds (ms)";

    static final String TITLE_STRING = "Job#,NumPageFaults,AvgWaitTime,AvgCompletionTime,AvgExecutionTime,IOTime,FaultServiceTotal,";

    static final int NUM_FIELDS = 7;

    static final int JOBID = 0;
    static final int NUM_FAULTS = 1;
    static final int WAITING_TIME = 2;
    static final int COMPLETION_TIME = 3;
    static final int EXECUTION_TIME = 4;
    static final int IO_TIME = 5;
    static final int FAULT_SERVICE = 6;

    long[][][] timingArray;
    long[][] avgTimingArray;


    public TimingData (int iterations, int numJobs) {
        this.iterations = iterations;
        this.numJobs = numJobs;
        timingArray = new long[iterations][numJobs][NUM_FIELDS];
        avgTimingArray = new long [numJobs][NUM_FIELDS];
    }

    // save Timing Data for current iteration.
    public void saveTimingDataForThisIteration(int i) {
        int j=0; //j = job counter
        for (PCB thisPCB: Queues.doneQueue) {
            //i=iteration counter
            timingArray[i][j][JOBID] = thisPCB.jobId;
            timingArray[i][j][NUM_FAULTS] = thisPCB.trackingInfo.pageFaults;
            timingArray[i][j][WAITING_TIME] = Math.round(thisPCB.trackingInfo.totalWait);
            timingArray[i][j][COMPLETION_TIME] = Math.round(thisPCB.trackingInfo.completionTime);
            timingArray[i][j][EXECUTION_TIME] = Math.round(thisPCB.trackingInfo.execTotalTime);
            timingArray[i][j][IO_TIME] = Math.round(thisPCB.trackingInfo.ioTotalTime);
            timingArray[i][j][FAULT_SERVICE] = Math.round(thisPCB.trackingInfo.totalFaultServiceTime);
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
                    avgTimingArray[i][EXECUTION_TIME] += timingArray[j][i][EXECUTION_TIME];   //sum completion times
                    avgTimingArray[i][IO_TIME] += timingArray[j][i][IO_TIME];   //sum completion times
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

                avgTimingArray[i][EXECUTION_TIME] = Math.round((double) avgTimingArray[i][EXECUTION_TIME] / (double) iterations);  //take avg of completion times across the iterations
                timing.print(avgTimingArray[i][EXECUTION_TIME] + ",");

                avgTimingArray[i][IO_TIME] = Math.round((double) avgTimingArray[i][IO_TIME] / (double) iterations);  //take avg of completion times across the iterations
                timing.print(avgTimingArray[i][IO_TIME] + ",");

                avgTimingArray[i][FAULT_SERVICE] = Math.round((double) avgTimingArray[i][FAULT_SERVICE] / (double) iterations);
                timing.print(avgTimingArray[i][FAULT_SERVICE] + ",");

                timing.println();
            }

            //finally calculate avg Wait Time, Completion Time across all jobs.
            double avgNumFaults = 0;
            double avgWaitTime = 0;
            double avgCompletionTime = 0;
            double avgIOWaitTime = 0;
            double avgFaultServiceTime = 0;

            for (int i = 0; i < numJobs; i++) {
                avgNumFaults+= avgTimingArray[i][NUM_FAULTS];
                avgWaitTime += avgTimingArray[i][WAITING_TIME];
                avgCompletionTime += avgTimingArray[i][COMPLETION_TIME];
                avgIOWaitTime += avgTimingArray[i][IO_TIME];
                avgFaultServiceTime += avgTimingArray[i][FAULT_SERVICE];

            }
            avgNumFaults = avgNumFaults / (double) numJobs;
            avgWaitTime = avgWaitTime / (double) numJobs;
            avgCompletionTime = avgCompletionTime / (double) numJobs;
            avgIOWaitTime = avgIOWaitTime / (double) numJobs;
            avgFaultServiceTime = avgFaultServiceTime / (double) numJobs;

            timing.println();
            timing.println("For All Jobs After running " + iterations + " iterations:");
            timing.println(",,#Faults, AvgWait, AvgCompletion, AvgIOWait, AvgFaultService");
            timing.println(",," + avgNumFaults + "," + Math.round(avgWaitTime) + "," + Math.round(avgCompletionTime) +
                    "," + avgIOWaitTime + "," + avgFaultServiceTime + ",");
            timing.close();

        }
        catch (FileNotFoundException ex){
            System.err.println("Could not open the " + fileName + "file.");
            System.err.println("Please close the " + fileName + "file and try again.");
            //ex.printStackTrace();
        }
    }

}
