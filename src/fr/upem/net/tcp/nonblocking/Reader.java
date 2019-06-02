package fr.upem.net.tcp.nonblocking;


public interface Reader {

    public static enum ProcessStatus {DONE,REFILL,ERROR};

    /**
     * Look if the bytebuffer is filled enough to get the object and return the status
     * @return the ProcessStatus
     */
    public ProcessStatus process();

    public Object get();

    public void reset();

}
