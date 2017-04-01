import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.exit;

public class Driver {


    final static int FIFO = 1;
    final static int PRIORITY = 2;
    final static int SJF = 3;

    static int policy = FIFO;                       //current scheduling algorithm; FIFO is default.

    public static final boolean CHECK_OUTPUT_MODE = false;      //to check output when complete.
    public static boolean logging;       //set to true if we want to output the results(buffers) to a file.

    //static ExecutorService executor;
    static CPU[] cpu;
    static Thread [] cpuThread;

    static int numJobs = 0;

    public static void main(String[] args) throws FileNotFoundException {

        Scanner userInput = new Scanner(System.in);
        int iterations;
        TimingData timingData = null;

        LinkedList<String> queueWatch = new LinkedList<>();
        /////////////////////////////////////////////////////////////////////////////////
        //                  Get User Input
        /////////////////////////////////////////////////////////////////////////////////

        System.out.println("Welcome to the SimpleOS Simulator.\t");
        System.out.print("Enter Scheduling Policy (1 for FIFO, 2 for Priority, 3 for SJF:)\t");
        policy = userInput.nextInt();

        System.out.print("Log output for 1 iteration?(y/n)\t");
        String logChoice = userInput.next();

        if (logChoice.equals("y") || logChoice.equals("Y")) {
            logging = true;
            iterations = 1;
        } else {
            logging = false;
            System.out.print("Enter Number of iterations:\t");
            iterations = userInput.nextInt();
        }

        /////////////////////////////////////////////////////////////////////////////////
        //                  Begin Outer Loop
        /////////////////////////////////////////////////////////////////////////////////

        for (int i = 0; i < iterations; i++) {

            java.io.File file = new java.io.File("Program-File.txt");

            try ( //try with resources (auto-closes Scanner)
                Scanner input = new Scanner(file);
            ) {
                Queues.initQueues();
                MemorySystem.initMemSystem();
                Loader loader = new Loader();

                PageManager pageManager = new PageManager();
                Thread pageManThread = new Thread(pageManager);
                pageManThread.start();

                FrameFreer frameFreer = new FrameFreer();
                Thread frameFreerThread = new Thread(frameFreer);
                frameFreerThread.start();

                DMA dma = new DMA();
                Thread dmaThread = new Thread(dma);
                dmaThread.start();

                cpu = new CPU[CPU.CPU_COUNT];
                //executor = Executors.newFixedThreadPool(CPU.CPU_COUNT);
                cpuThread = new Thread[CPU.CPU_COUNT];

                for (int j = 0; j < CPU.CPU_COUNT; j++) {
                    cpu[j] = new CPU(j);
                    cpuThread[j] = new Thread(cpu[j]);
                    cpuThread[j].start();
                    //executor.execute(cpu[j]);
                }

                ///coreDumpArray = new ArrayList();

                loader.load(input);
                //outputDiskToFile();  //debugging method to check if the Loader loaded the disk properly.

                numJobs = Queues.diskQueue.size();

                LongScheduler.schedule();       //load processes into memory from disk.
                //outputMemToFile("memDump.txt");  //to check if the LTS loaded the disk properly.

                /////////////////////////////////////////////////////////////////////////////////
                //                        Begin Main Driver Loop
                /////////////////////////////////////////////////////////////////////////////////

                if (i == 0) {
                    queueWatch.clear();
                    queueWatch.add("ready:, waiting:, done:, free frames:");
                }
                while (ScheduleAndDispatch.scheduleAndDispatch()) {
                    //after page fault or job completion:
                    if (i==0) {//output the queue progress for first iteration.
                        //System.out.println("Ready: " + Queues.readyQueue.size() + ", " +
                        //        "Waiting: " + Queues.waitingQueue.size() + ", " +
                        //        "Done: " + Queues.doneQueue.size() + ", " +
                        //        "Free Frames: " + MemorySystem.memory.freeFrameList.size());
                        queueWatch.add(Queues.readyQueue.size() + ", " +
                                Queues.waitingQueue.size() + ", " +
                                Queues.doneQueue.size() + ", " +
                                MemorySystem.memory.freeFrameList.size());
                    }
                }

                /////////////////////////////////////////////////////////////////////////////////
                //                          END Main Driver Loop
                /////////////////////////////////////////////////////////////////////////////////

                try {
                    PCB shutDownRequest = new PCB(-1);    //create a dummy PCB we use to shut down the threads.

                    //shutdown the CPU's.
                    for (int k = 0; k < CPU.CPU_COUNT; k++)
                        Queues.runningQueues[k].put(shutDownRequest);

                    //shutdown the Page Manager.
                    Queues.waitingQueue.put(shutDownRequest);

                    //shut down freeFrameManager.
                    Queues.freeFrameRequestQueue.put(shutDownRequest);

                    //shut down DMA  thread.
                    Queues.ioWaitQueue.put(shutDownRequest);

                    if (frameFreerThread.isAlive()) {
                        //System.out.println ("Warning: Frame Freer is still active.");
                        //wait for the FrameFreer to finish running.
                        synchronized (Queues.FrameFreerShutDownLock) {
                            Queues.FrameFreerShutDownLock.wait();
                        }
                    }
                    if (pageManThread.isAlive()) {
                        //System.out.println ("Warning: Page Manager is still active.");
                        //wait for the PageManager to finish running.
                        synchronized (Queues.PageManagerShutDownLock) {
                            Queues.PageManagerShutDownLock.wait();
                        }
                    }

                } catch (InterruptedException ie) {
                    System.err.println(ie.toString());
                }


                if (logging) {
                    writeOutputFile();
                }
                if (CHECK_OUTPUT_MODE) {
                    //compare output to gold standard
                    checkOutputIsCorrect();
                }

                if (i==0) {//output the final state of the queues, for first iteration.
                    //System.out.println("Ready: " + Queues.readyQueue.size() + ", " +
                    //        "Waiting: " + Queues.waitingQueue.size() + ", " +
                    //        "Done: " + Queues.doneQueue.size() + ", " +
                    //        "Free Frames: " + MemorySystem.memory.freeFrameList.size());
                    queueWatch.add(Queues.readyQueue.size() + ", " +
                            Queues.waitingQueue.size() + ", " +
                            Queues.doneQueue.size() + ", " +
                            MemorySystem.memory.freeFrameList.size());
                }

                outputQueueWatchToFile("queueWatch.csv", queueWatch);

                //outputMemToFile("coredump.txt");
                outputDiskToFile();  //debugging method; outputs the contents of the disk to a file.

                //create timing data (if not already created))
                if (timingData == null) {
                    timingData = new TimingData(iterations, Queues.doneQueue.size());
                }
                timingData.saveTimingDataForThisIteration(i);
            }
        }
        /////////////////////////////////////////////////////////////////////////////////
        //                  End Outer Loop
        /////////////////////////////////////////////////////////////////////////////////


        timingData.outputTimingData();
    }

    //debugging method: compares the output to an output file that we know is correct.
    public static boolean checkOutputIsCorrect() throws FileNotFoundException {
        java.io.File goodFile = new java.io.File("CorrectOutput.txt");
        try ( //try with resources (auto-closes Scanner)
            Scanner goodInput = new Scanner(goodFile);
        ) {
            ArrayList<String> goodResults = new ArrayList<>();
            while (goodInput.hasNext()) {
                StringBuilder temp = new StringBuilder();
                for (int i = 0; i < 4; i++) {
                    temp.append(goodInput.nextLine());
                    temp.append("\r\n");
                }
                goodResults.add(temp.toString());
            }
            //make a copy of the doneQueue because we are going to sort it - don't want to mess up the original.
            LinkedList<PCB> tempDoneQueue = new LinkedList<>();
            for (PCB thisPCB : Queues.doneQueue) {
                tempDoneQueue.add(thisPCB);
            }
            tempDoneQueue.sort((PCB o1, PCB o2) -> o1.jobId - o2.jobId);
            for (int i = 0; i < goodResults.size(); i++) {
                boolean isMatch = goodResults.get(i).equals(tempDoneQueue.get(i).trackingInfo.buffers);
                if (!isMatch) {
                    System.out.println("Warning: output does not match gold standard.");
                    System.out.println(goodResults.get(i));
                    System.out.println(tempDoneQueue.get(i).trackingInfo.buffers);
                }
            }
            return false;
        }
    }


    // Log Mem Usage, Output and # IO Operations to File
    public static void writeOutputFile() {

        java.io.File outputFile = new java.io.File("output.txt");
        try {
            java.io.PrintWriter output = new java.io.PrintWriter(outputFile);
            for (PCB thisPCB : Queues.doneQueue) {
                output.println("Job:" + thisPCB.jobId + "\tNumber of io operations: " + thisPCB.trackingInfo.ioCounter
                    + "\tJobSize: " + thisPCB.jobSizeInMemory); // + "\tCPUid: " + thisPCB.cpuId);
            }

            for (PCB thisPCB : Queues.doneQueue) {
                output.print(thisPCB.trackingInfo.buffers);
            }
            output.close();
        }
        catch (FileNotFoundException ex){
            //ex.printStackTrace();
            System.err.println("Could not open the output.txt file.");
            System.err.println("Please close the file and try again.");
        }

    }

    public static String convertIntInstructionToHex(int instructionAsInt) {
        String padding = "00000000";
        String unpaddedHex = Integer.toHexString(instructionAsInt).toUpperCase();
        String paddedHex = padding.substring(unpaddedHex.length()) + unpaddedHex;
        paddedHex = "0x" + paddedHex;
        return paddedHex;
    }


    //outputDiskToFile: //debugging method to view the contents of the disk.
    public static void outputDiskToFile() {
        try {
            java.io.File diskDumpFile = new java.io.File("diskDump.txt");
            java.io.PrintWriter diskDump = new java.io.PrintWriter(diskDumpFile);

            for (int i = 0; i < MemorySystem.disk.DISK_SIZE; i++) {
                for (int j = 0; j < MemorySystem.PAGE_SIZE; j++) {
                    diskDump.println(convertIntInstructionToHex(MemorySystem.disk.diskArray[i][j]));
                }
            }
            diskDump.close();
        }
        catch (FileNotFoundException ex){
            //ex.printStackTrace();
            System.err.println("Could not open the diskDump.txt file.");
            System.err.println("Please close the file and try again.");
        }
    }


    //outputMemToFile: debugging method to check if LTS loaded memory properly.
    //Outputs Memory to File.
    public static void outputMemToFile(String fileName) {
        try {
            java.io.File memDumpFile = new java.io.File(fileName);
            java.io.PrintWriter memDump = new java.io.PrintWriter(memDumpFile);

            for (int i = 0; i < MemorySystem.memory.MEM_SIZE; i++) {
                for (int j = 0; j < MemorySystem.PAGE_SIZE; j++) {
                    memDump.println(convertIntInstructionToHex(MemorySystem.memory.memArray[i][j]));
                }
            }
            memDump.close();
        }
        catch (FileNotFoundException ex){
            //ex.printStackTrace();
            System.err.println("Could not open the " + fileName + "file.");
            System.err.println("Please close the " + fileName + "file and try again.");
        }
    }


    //outputMemToFile: debugging method to check if LTS loaded memory properly.
    //Outputs Memory to File.
    public static void outputQueueWatchToFile(String fileName, LinkedList<String> queueWatch) {
        try {
            java.io.File queueFile = new java.io.File(fileName);
            java.io.PrintWriter queueOut = new java.io.PrintWriter(queueFile);

            for (String currLine: queueWatch)
                queueOut.println(currLine);

            queueOut.close();
        }
        catch (FileNotFoundException ex){
            //ex.printStackTrace();
            System.err.println("Could not open the " + fileName + "file.");
            System.err.println("Please close the " + fileName + "file and try again.");
        }
    }

}
