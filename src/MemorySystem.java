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

    public void writeDisk(String line, int frameNumber, int offset) {
        //int hexLong = Long.decode(line).intValue();

        int hexInt = Long.decode(line).intValue();
        diskArray[frameNumber][offset] = hexInt;

        //System.out.println (hexInt);
        //line = line.substring(2);
        //System.out.println (line.substring(2,3));
        //System.out.println (line.substring(3,4));
        //diskArray[diskCounter].word[0] = line.substring(2,3);

    }

    //returns a line of code from the disk (as an int)
    public int readDisk(int frameNumber, int offset) {
          return diskArray[frameNumber][offset];
    }


}

//memory: 1024 words.  1 word = 4 bytes (or 8 hex characters).
class MemoryClass {
    public final int MEM_SIZE = 256;

    int[][] memArray;

    public MemoryClass() {
        memArray = new int [MEM_SIZE][4];
    }

    /*
    public void writeMemoryAddress(int ramAddress, int data) {
        if ((ramAddress < 0) || (ramAddress > MEM_SIZE - 1))
            System.out.println ("Error, attempting to write to invalid memory address: " + ramAddress);
        else
            memArray[ramAddress] = data;
    }

    public int readMemoryAddress(int ramAddress) {
        if ((ramAddress < 0) || (ramAddress > MEM_SIZE - 1)) {
            System.out.println("Error, attempting to write to invalid memory address: " + ramAddress);
            return -1;
        }
        else
            return (memArray[ramAddress]);
    }


    public boolean checkAddressInBounds(int memLoc) {
        return !((memLoc < 0 ) || (memLoc > MEM_SIZE - 1));
    }

    */
}

