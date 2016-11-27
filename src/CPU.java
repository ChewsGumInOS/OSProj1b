import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class CPU implements Runnable {

    public static final int NUM_REGISTERS = 16;
    public static final int CACHE_SIZE = PCB.TABLE_SIZE; //=20; code is simplified by having the cache mirror page table.
    public static final int CPU_COUNT = 4;

    public static final int DMA_DELAY = 10;            //delay DMA by X nanoseconds to simulate IO to/from RAM.
    public static final int PAGE_FAULT_DELAY = 100;    //delay for loading a page from disk to memory.
    public static final int DISK_ACCESS_DELAY = 100;   //delay for writing page back to disk.


    class Cache {
        int[][] arr;
        boolean[][] modified;
        boolean[] valid;
        boolean changed;

        public void clearCache() {
            for (int i=0; i < CACHE_SIZE; i++) {
                Arrays.fill(arr[i], 0);
                Arrays.fill(modified[i], false);
            }
            Arrays.fill(valid, false);
            changed = false;
        }

        public Cache() {
            arr = new int[CACHE_SIZE][4];
            modified = new boolean[CACHE_SIZE][4];
            valid = new boolean[CACHE_SIZE];
        }
    }

    class CacheResult {
        boolean valid;
        int page;
        int offset;
        int data;
    }

    int pageFaultNumber = 0;   //set by execute, in the event of a page fault.

    int cpuId;
    Cache cache;
    PCB currJob;

    /////////////////////////////////////////////////////////////////////////////////
    //                  BEGIN variables set/preserved by context switcher
    /////////////////////////////////////////////////////////////////////////////////
    int [] reg;             //16 registers.
                            //reg0 = Accumulator.
                            //reg1 = Zero register (0).
    int pc;                 //program counter.  (logical address)
    /////////////////////////////////////////////////////////////////////////////////
    //                  END variables set/preserved by context switcher
    /////////////////////////////////////////////////////////////////////////////////


    public CPU (int cpuId) {
        reg = new int[NUM_REGISTERS];
        cache = new Cache();
        this.cpuId = cpuId;
    }


    public void run() {

        Instruction currInstruction;
        CacheResult cacheResult;
        boolean pageFault;
        long currTime;

        try {
            while (!Thread.currentThread().isInterrupted()) {
                currJob = Queues.runningQueues[cpuId].take();
                //this is how the Driver shuts down the CPU's after all jobs are processed.
                if (currJob.jobId == -1) {
                    return;
                }

                currJob.status = PCB.state.RUNNING;

                if (currJob.firstRun) {
                    currJob.firstRun = false;
                    currJob.trackingInfo.runStartTime = System.currentTimeMillis();
                }

                /////////////////////////////////////////////////////////////////////////////////
                //                  BEGIN  Fetch-Decode-Execute and pageFault detection
                /////////////////////////////////////////////////////////////////////////////////
                pageFault = false;
                while ((!pageFault) && (pc < currJob.codeSize)) {
                    cacheResult = fetchFromCache(pc);
                    if (cacheResult.valid) {
                        currInstruction = decode(cacheResult.data);
                        if (execute(currInstruction))
                            pc++;
                        else
                            pageFault = true;   //page fault from within IO instruction being executed.
                    }
                    else {                      //page fault from accessing next line of code.
                        pageFault = true;
                        pageFaultNumber = pc/4;
                    }
                }
                /////////////////////////////////////////////////////////////////////////////////
                //                  END  Fetch-Decode-Execute and pageFault detection
                /////////////////////////////////////////////////////////////////////////////////



                //if cache was modified, copy modified words from cache back into memory.
                //(because the cache is going to be erased upon context switch)
                if (cache.changed) {
                    DMA dma = new DMA();
                    Thread dmaThread = new Thread(dma);
                    dmaThread.start();
                    dmaThread.join();
                }

                //save context information.
                currJob.pc = pc;
                System.arraycopy(reg, 0, currJob.registers, 0, currJob.registers.length);



                if (!pageFault) {
                    //Job successfully completed - so free frames, add to DoneQueue, save output buffers.
                    Queues.freeFrameRequestQueue.put(currJob);

                    currJob.trackingInfo.buffers = outputResults();
                    currJob.trackingInfo.runEndTime = System.currentTimeMillis();
                    currJob.status = PCB.state.COMPLETE;

                    Queues.doneQueue.add(currJob);
                    if (Queues.doneQueue.size() == Driver.numJobs) {
                        //*All* jobs are done - signal the scheduler it's time to stop.
                        PCB shutDownRequest = new PCB(-1);    //create a dummy PCB to tell the scheduler to stop.
                        Queues.readyQueue.put(shutDownRequest);
                    }
                }
                else {
                    //page fault - so put it on the waiting queue, for the PageManager.
                    currJob.pageFaultFrame = pageFaultNumber;
                    Queues.waitingQueue.put(currJob);
                    currTime = System.currentTimeMillis();
                    currJob.trackingInfo.startedWaitingAgainTime = currTime;
                    currJob.trackingInfo.addStartTime(currTime);

                    currJob.status = PCB.state.WAITING;

                    //System.out.println("Ready Queue size: " + Queues.readyQueue.size());
                    //System.out.println("Page Request Queue size:" + Queues.pageRequestQueue.size());
                }
                Queues.freeCpuQueue.put(cpuId);

            }
        } catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }

    }

    // The Decode method is a part of the CPU. Its function is to completely decode a fetched instruction –
    // using the different kinds of address translation schemes of the CPU architecture.
    // (See the supplementary information in the file: Instruction Format.)
    // On decoding, the needed parameters must be loaded into the appropriate registers or data structures
    // pertaining to the program/job and readied for the Execute method to function properly.
    public Instruction decode (int currLine) {

        Instruction currInst;
        int reg1, reg2, reg3;

        int temp = (currLine >> 24);
        // why & 0b111111 in the code below?  because java int is signed.
        // we need to remove the sign from the starting 111111's (if they exist)
        int opcode = temp & 0b111111;

        // & 0b11 to remove the sign, if it exists (the starting 111111's)
        int type = (temp >> 6) & 0b11;

        switch (type) {
            case Instruction.ARITHMETIC:
                currLine >>= 12;  //remove last 12 empty bits
                reg3 = currLine & 0b1111;
                currLine >>= 4;
                reg2 = currLine & 0b1111;
                currLine >>= 4;
                reg1 = currLine & 0b1111;
                currInst = new Instruction(opcode, reg1, reg2, reg3);
                break;

            case Instruction.CBI:
                reg3 = currLine & 0xFFFF; //address
                currLine >>= 16;
                reg2 = currLine & 0b1111;
                currLine >>= 4;
                reg1 = currLine & 0b1111;
                currInst = new Instruction(opcode, reg1, reg2, reg3);
                break;

            case Instruction.JUMP:
                reg1 = currLine & 0xFFFFFF; //24-bit address
                currInst = new Instruction(opcode, reg1, 0, 0);
                break;

            case Instruction.IO:
                reg3 = currLine & 0xFFFF; //address
                currLine >>= 16;
                reg2 = currLine & 0b1111;
                currLine >>= 4;
                reg1 = currLine & 0b1111;
                currInst = new Instruction(opcode, reg1, reg2, reg3);
                break;

            default:
                System.err.println("Invalid opcode: " + type);
                currInst = null;
                break;
        }

        return currInst;
    }

    //Execute
    //This method is essentially a switch-loop of the CPU.
    // One of its key functions is to increment the PC value on ‘successful’ execution of the current instruction.
    // Note also that if an I/O operation is done via an interrupt, or due to any other preemptive instruction,
    // the job is suspended until the DMA-Channel method completes the read/write operation, or the interrupt is serviced.
    public boolean execute(Instruction instruction) {

        boolean result = true;
        CacheResult cacheResult;

        switch (instruction.opcode) {
            //case Instruction.IO:
            case Instruction.RD:    //Reads content of I/P buffer into a accumulator
                cacheResult = readCache(instruction.reg3 + reg[instruction.reg2]);
                if (cacheResult.valid)
                    reg[instruction.reg1] = cacheResult.data;
                else {
                    result = false;
                    pageFaultNumber = cacheResult.page;
                }
                break;

            //case Instruction.IO:
            case Instruction.WR:    //Writes the content of accumulator into O/P buffer
                if (instruction.reg3 != 0) {
                    cacheResult = writeCache(instruction.reg3, reg[instruction.reg1]);
                    pageFaultNumber = cacheResult.page;
                }
                else {
                    cacheResult = writeCache(reg[instruction.reg2], reg[instruction.reg1]);
                    pageFaultNumber = cacheResult.page;
                }
                if (cacheResult.valid) {
                    cache.changed = true;
                }
                else {
                    result = false;
                }
                break;

            //case Instruction.CBI:
            case Instruction.LW: //Loads the content of an address into a reg.
                cacheResult = readCache(instruction.reg3 + reg[instruction.reg1]);
                if (cacheResult.valid)
                    reg[instruction.reg2] = cacheResult.data;
                else {
                    result = false;
                    pageFaultNumber = cacheResult.page;
                }
                break;

            //case Instruction.CBI:
            case Instruction.ST: //Stores content of a reg. into an address
                cacheResult = writeCache(reg[instruction.reg2] + instruction.reg3, reg[instruction.reg1]);
                if (cacheResult.valid) {
                    cache.changed = true;
                }
                else {
                    result = false;
                    pageFaultNumber = cacheResult.page;
                }
                break;



            //case Instruction.ARITHMETIC:
            case Instruction.ADD: //Adds content of two S-regs into D-reg
                reg[instruction.reg3] = reg[instruction.reg1] + reg[instruction.reg2];
                break;

            case Instruction.SUB: //Subtracts content of two S-regs into D-reg
                reg[instruction.reg3] = reg[instruction.reg1] - reg[instruction.reg2];
                break;

            case Instruction.MUL: //Multiplies content of two S-regs into D-reg
                reg[instruction.reg3] = reg[instruction.reg1] * reg[instruction.reg2];
                break;

            case Instruction.DIV: //Divides content of two S-regs into D-reg
                reg[instruction.reg3] = reg[instruction.reg1] / reg[instruction.reg2];
                break;

            case Instruction.AND: //Logical AND of two S-regs into D-reg
                reg[instruction.reg3] = reg[instruction.reg1] & reg[instruction.reg2];
                break;

            case Instruction.OR: //Logical OR of two S-regs into D-reg
                reg[instruction.reg3] = reg[instruction.reg1] | reg[instruction.reg2];
                break;

            case Instruction.MOV: //Transfers the content of one register into another
                //reg[rCommand.regD] = reg[rCommand.regS2];
                reg[instruction.reg3 + instruction.reg1] = reg[instruction.reg2];
                break;

            case Instruction.SLT: //Sets the D-reg to 1 if  first S-reg is less than second B-reg,
                // and 0 otherwise
                reg[instruction.reg3] = (reg[instruction.reg1] < reg[instruction.reg2] ? 1 : 0);
                break;

            case Instruction.NOP: //Does nothing and moves to next instruction
                break;

            case Instruction.MOVI://Transfers address/data directly into a register
                //transfers the contents of one register to another
                reg[instruction.reg2] = instruction.reg3;
                break;

            case Instruction.ADDI: //Adds a data directly to the content of a register
                reg[instruction.reg2] += instruction.reg3;
                break;

            case Instruction.MULI: //Multiplies a data directly to the content of a register
                reg[instruction.reg2] *= instruction.reg3;
                break;

            case Instruction.DIVI: //Divides a data directly to the content of a register
                reg[instruction.reg2] /= instruction.reg3;
                break;

            case Instruction.LDI: //Loads a data/address directly to the content of a register
                reg[instruction.reg2] = instruction.reg3 + reg[instruction.reg1];
                break;

            case Instruction.SLTI: //Sets the D-reg to 1 if  first S-reg is less than a data, and 0 otherwise
                reg[instruction.reg2] = (reg[instruction.reg3] < instruction.reg1 ? 1 : 0);
                break;

            case Instruction.BEQ: //Branches to an address when content of B-reg = D-reg
                if (reg[instruction.reg1] == reg[instruction.reg2])
                    // divide by 4 to convert from byte to word.
                    // - 1 because pc counter is incremented at end of Execute.
                    pc = (instruction.reg3 / 4) - 1;
                break;

            case Instruction.BNE: //Branches to an address when content of B-reg <> D-reg
                if (reg[instruction.reg1] != reg[instruction.reg2])
                    // divide by 4 to convert from byte to word.
                    // - 1 because pc counter is incremented at end of Execute.
                    pc = (instruction.reg3 / 4) - 1;
                break;

            case Instruction.BEZ: //Branches to an address when content of B-reg = 0
                if (reg[instruction.reg1] == 0)
                    // divide by 4 to convert from byte to word.
                    // - 1 because pc counter is incremented at end of Execute.
                    pc = (instruction.reg3 / 4) - 1;
                break;

            case Instruction.BNZ: //Branches to an address when content of B-reg <> 0
                if (reg[instruction.reg1] != 0)
                    // divide by 4 to convert from byte to word.
                    // - 1 because pc counter is incremented at end of Execute.
                    pc = (instruction.reg3 / 4) - 1;
                break;

            case Instruction.BGZ: //Branches to an address when content of B-reg > 0
                if (reg[instruction.reg1] > 0)
                    // divide by 4 to convert from byte to word.
                    // - 1 because pc counter is incremented at end of Execute.
                    pc = (instruction.reg3 / 4) - 1;
                break;

            case Instruction.BLZ: //Branches to an address when content of B-reg < 0
                if (reg[instruction.reg1] < 0)
                    // divide by 4 to convert from byte to word.
                    // - 1 because pc counter is incremented at end of Execute.
                    pc = (instruction.reg3 / 4) - 1;
                break;

            //case Instruction.JUMP:
            case Instruction.HLT: //Logical end of program
                currJob.goodFinish = true;
                break;

            case Instruction.JMP: //Jumps to a specified location
                pc = (instruction.reg1 / 4) - 1;
                break;

            default:
                System.err.println("Invalid opcode in execute: " + instruction.type);
                break;
        }
        return result;
    }

    public CacheResult checkAndGetCacheAddress(int address) {
        CacheResult result = new CacheResult();
        result.page = address / 4;
        result.offset = address % 4;
        result.valid = cache.valid[result.page];
        return result;
    }

    //fetch from the cache.
    public CacheResult fetchFromCache(int address) {
        //check that the logical code being fetched is inside the job's code section
        if ((address >= 0) && (address < currJob.codeSize)) {
            CacheResult result = checkAndGetCacheAddress(address);
            if (result.valid) {
                result.data = cache.arr[result.page][result.offset];
            }
            return result;
        }
        else {
            System.err.println ("Error.  Invalid fetch at address: " + address);
            System.exit(-1);
            return null;
        }
    }

    //read the specified memory address - but from the cache.
    public CacheResult readCache(int address) {
        //Convert from bytes to words - the instruction addresses go by bytes.
        //e.g. pc = pc + 4, we need pc = pc + 1.  if address, we need address/4.
        address = address/4;

        //check if logical address is in bounds of the current job's DATA section.
        if ((address >= currJob.codeSize) && (address < currJob.getJobSizeInMemory())) {
            CacheResult result = checkAndGetCacheAddress(address);
            if (result.valid) {
                result.data = cache.arr[result.page][result.offset];
            }
            return result;
        }
        else {
            System.err.println ("Error.  Invalid memory write at address: " + address);
            System.exit(-1);
            return null;
        }
    }

    //write the specified memory address - but to the cache
    public CacheResult writeCache(int address, int data) {
        //Convert from bytes to words - the instruction addresses go by bytes.
        //e.g. pc = pc + 4, we need pc = pc + 1.  if address, we need address/4.
        address = address/4;

        //check if logical address is in bounds of the current job's DATA section.
        if ((address >= currJob.codeSize) && (address < currJob.getJobSizeInMemory())) {
            CacheResult result = checkAndGetCacheAddress(address);
            if (result.valid) {
                cache.arr[result.page][result.offset] = data;
                cache.modified[result.page][result.offset] = true;
            }
            return result;
        }
        else {
            System.err.println ("Error.  Invalid memory read at address: " + address);
            System.exit(-1);
            return null;
        }
    }

    //for printing the results of the job to a file.
    public String outputResults() {

        StringBuilder results = new StringBuilder();
        results.append("Job ID:\t");
        results.append(currJob.jobId);
        results.append("\r\nInput:\t");
        for (int i = 0; i < currJob.inputBufferSize; i++) {
            results.append(readCache((currJob.codeSize + i)*4).data);
            results.append(" ");
        }
        results.append("\r\nOutput:\t");
        for (int i = 0; i < currJob.outputBufferSize; i++) {
            results.append(readCache((currJob.codeSize + currJob.inputBufferSize + i)*4).data);
            results.append(" ");
        }
        results.append("\r\nTemp:\t");
        for (int i = 0; i < currJob.tempBufferSize; i++) {
            results.append(readCache((currJob.codeSize + currJob.inputBufferSize + currJob.outputBufferSize + i)*4).data);
            results.append(" ");
        }
        results.append("\r\n");

        return results.toString();
    }

    //debugging method - prints current instruction.
    public String printInstruction(Instruction instruction) {

        String [] opStrings = {"RD", "WR", "ST", "LW", "MOV", "ADD", "SUB", "MUL", "DIV", "AND", "OR", "MOVI", "ADDI",
                "MULI", "DIVI", "LDI", "SLT", "SLTI", "HLT", "NOP", "JMP", "BEQ", "BNE", "BEZ", "BNZ", "BGZ", "BLZ"};

        String result = "";

        switch (instruction.type) {
            case Instruction.ARITHMETIC:
                result= "Rtype. Opcode: " + instruction.opcode + " " + opStrings[instruction.opcode] + "\t" +
                         "S1: " + instruction.reg1 + "  S2: " + instruction.reg2 + "  D: " + instruction.reg3;
                break;
            case Instruction.CBI:
                result= "CBI.   Opcode: " + instruction.opcode + " " + opStrings[instruction.opcode] + "\t" +
                        "B: " + instruction.reg1 + "  D: " + instruction.reg2 + "  Ad: " + instruction.reg3;
                break;
            case Instruction.JUMP:
                result= "Jump.  Opcode: " + instruction.opcode + " " + opStrings[instruction.opcode] + "\t" +
                        "Ad: " + instruction.reg1;
                break;
            case Instruction.IO:
                result= "IO.    Opcode: " + instruction.opcode + " " + opStrings[instruction.opcode] + "\t" +
                        "Reg1: " + instruction.reg1 + "  Reg2: " + instruction.reg2 + "  Ad: " + instruction.reg3;
                break;
        }
        return result;
    }

    /*
    //debugging method - prints registers and buffers to screen.
    public void printDataBuffers() {

        //System.out.print("Regs:\t");
        for (int i = 0; i < reg.length; i++) {
            System.out.print("R" + i + ":" + reg[i] + "\t");
            if (i==7)
                System.out.println();
        }
        System.out.println("PC:" + pc);

        System.out.print("Input:\t");
        for (int i = 0; i < inputBufferSize; i++) {
            System.out.print(readCache((codeSize + i)*4) + " ");  //*4 to convert from words to bytes
        }
        System.out.println();

        System.out.print("Output:\t");
        for (int i = 0; i < outputBufferSize; i++) {
            System.out.print(readCache((codeSize + inputBufferSize + i)*4) + " ");  //*4 to convert from words to bytes
        }

        System.out.println();

        System.out.print("Temp:\t");
        for (int i = 0; i < tempBufferSize; i++) {
            System.out.print(readCache((codeSize + inputBufferSize + outputBufferSize + i)*4) + " ");  //*4 to convert from words to bytes
        }

        System.out.println();
        System.out.println();
    }

*/

    //separate thread to handle DMA transfer
    class DMA implements Runnable {

        public void run() {
            handleDMA();
        }

        public DMA() {}

        public void handleDMA() {
            try {
                int currPage;
                int currIOcount = 0;
                for (int i = 0; i < CACHE_SIZE; i++) {
                    if (cache.valid[i]) {
                        for (int j=0; j <4; j++) {
                            if (cache.modified[i][j]) {
                                currPage = currJob.memories.pageTable[i][PCB.PAGE_NUM];
                                MemorySystem.memory.writeMemoryAddress(currPage, j, cache.arr[i][j]);
                                currJob.trackingInfo.ioCounter++;       //update *total* IO count.
                                currIOcount++;
                                currJob.memories.pageTable[i][PCB.MODIFIED] = 1;   //set pagetable "dirty" indicator; must be written back to disk.
                            }
                        }
                    }
                }
                TimeUnit.NANOSECONDS.sleep(DMA_DELAY*currIOcount);
            }
            catch (InterruptedException ie) {
                System.err.println("Invalid DMA handler interruption: " + ie.toString());
            }
        }
    }
}
