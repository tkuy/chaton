package fr.upem.net.tcp.nonblocking.frame.reader;

import fr.upem.net.tcp.nonblocking.Reader;
import fr.upem.net.tcp.nonblocking.frame.FrameLogin;
import fr.upem.net.tcp.nonblocking.frame.FrameLoginPrivateConnection;
import fr.upem.net.tcp.nonblocking.frame.FrameReader;

import java.nio.ByteBuffer;

public class FrameLoginPrivateConnectionReader implements Reader {
    private final LongReader longReader;
    private enum State{DONE, WAITING_ID, ERROR}
    private long id;
    private State state;
    public FrameLoginPrivateConnectionReader(ByteBuffer bb) {
        this.longReader = new LongReader(bb);
        this.state = State.WAITING_ID;
    }

    @Override
    public Reader.ProcessStatus process() {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state) {
            case WAITING_ID:
                ProcessStatus status = longReader.process();
                System.out.println(status);
                if (status == ProcessStatus.DONE) {
                    id = (long) longReader.get();
                    longReader.reset();
                    state = State.DONE;
                    return ProcessStatus.DONE;
                } else {
                    return ProcessStatus.REFILL;
                }
        }
        return ProcessStatus.ERROR;
    }

    @Override
    public Object get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return new FrameLoginPrivateConnection(id);
    }

    @Override
    public void reset() {
        state = State.WAITING_ID;
    }
}
