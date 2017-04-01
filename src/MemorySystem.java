import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class MemorySystem {

    static public final int PAGE_SIZE = 4;

    static public DiskClass disk;
    static public MemoryClass memory;

    static public void initMemSystem() {
        disk = new DiskClass();
        memory = new MemoryClass();
    }
}

//disk: 2048 words.  1 word = 4 bytes (or 8 hex characters).
class DiskClass {

    public static final int DISK_SIZE = 2048/MemorySystem.PAGE_SIZE;
    int[][] diskArray;

    public DiskClass() {
        diskArray = new int[DISK_SIZE][MemorySystem.PAGE_SIZE];
    }

    public void writeDisk(int diskCounter, String line) {
        int hexInt = Long.decode(line).intValue();
        diskArray[diskCounter/MemorySystem.PAGE_SIZE][diskCounter%MemorySystem.PAGE_SIZE] = hexInt;
    }

    //returns a line of code from the disk (as an int)
    public int readDisk(int address) {
        int frameNumber = address/MemorySystem.PAGE_SIZE;
        int offset = address%MemorySystem.PAGE_SIZE;
        return diskArray[frameNumber][offset];
    }
}

//memory: 1024 words.  1 word = 4 bytes (or 8 hex characters).
class MemoryClass {
    public static final int MEM_SIZE = 1024/MemorySystem.PAGE_SIZE;

    int[][] memArray;

    LinkedBlockingQueue<Integer> freeFrameList;
    //List freeFramesList = Collections.synchronizedList(new LinkedList<Integer>());

    public MemoryClass() {
        memArray = new int [MEM_SIZE][MemorySystem.PAGE_SIZE];
        freeFrameList = new LinkedBlockingQueue<>();
        try {
            for (int i = 0; i < MEM_SIZE; i++) {
                freeFrameList.put(i);
            }
        }
        catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }

    }


    public void writeMemoryAddress(int frame, int offset, int data) {
        if ((frame < 0) || (frame > MEM_SIZE - 1))
            System.out.println ("Error, attempting to write to invalid memory frame: " + frame);
        else
            memArray[frame][offset] = data;
    }

    public int readMemoryAddress(int frame, int offset, int data) {
        if ((frame < 0) || (frame > MEM_SIZE - 1)) {
            System.out.println("Error, attempting to write to invalid memory frame: " + frame);
            return -1;
        }
        else
            return (memArray[frame][offset]);
    }
}

