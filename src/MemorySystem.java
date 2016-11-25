import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MemorySystem {

    //public final int FRAME_SIZE = 4;

    static public DiskClass disk;
    static public MemoryClass memory;

    static public void initMemSystem() {
        disk = new DiskClass();
        memory = new MemoryClass();
    }

}

//disk: 2048 words.  1 word = 4 bytes (or 8 hex characters).
class DiskClass {

    public static final int DISK_SIZE = 512;

    int[][] diskArray;

    public DiskClass() {
        diskArray = new int[DISK_SIZE][4];
    }

    public void writeDisk(int diskCounter, String line) {
        int hexInt = Long.decode(line).intValue();
        diskArray[diskCounter/4][diskCounter%4] = hexInt;
    }

    //returns a line of code from the disk (as an int)
    public int readDisk(int address) {
        int frameNumber = address/4;
        int offset = address%4;
        return diskArray[frameNumber][offset];
    }

}

//memory: 1024 words.  1 word = 4 bytes (or 8 hex characters).
class MemoryClass {
    public static final int MEM_SIZE = 256;

    int[][] memArray;

    LinkedList<Integer> freeFramesList;
    //List freeFramesList = Collections.synchronizedList(new LinkedList<Integer>());

    public MemoryClass() {
        memArray = new int [MEM_SIZE][4];
        freeFramesList = new LinkedList();
        for (int i = 0; i < MEM_SIZE; i++) {
            freeFramesList.add(i);
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


    //public boolean checkAddressInBounds(int memLoc) {
    //    return !((memLoc < 0 ) || (memLoc > MEM_SIZE - 1));
    //}

}

