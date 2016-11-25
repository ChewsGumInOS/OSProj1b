public class PageRequest {
    PCB jobPCB;
    int pageNumber;

    public PageRequest (PCB jobPCB, int pageNumber) {
        this.jobPCB = jobPCB;
        this.pageNumber = pageNumber;
    }
}

