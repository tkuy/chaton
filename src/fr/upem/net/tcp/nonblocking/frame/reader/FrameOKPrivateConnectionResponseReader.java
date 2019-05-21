package fr.upem.net.tcp.nonblocking.frame.reader;

import fr.upem.net.tcp.nonblocking.Reader;
import fr.upem.net.tcp.nonblocking.StringReader;
import fr.upem.net.tcp.nonblocking.frame.FramePrivateConnectionResponse;

import java.nio.ByteBuffer;

public class FrameOKPrivateConnectionResponseReader implements Reader{
    private enum State {DONE, WAITING_SENDER, WAITING_TARGET, ERROR}

    private State state = State.WAITING_SENDER;
    private String requester;
    private String target;
    private StringReader stringReader;
    public FrameOKPrivateConnectionResponseReader(ByteBuffer bb) {
        this.stringReader = new StringReader(bb);
    }
    @Override
    public ProcessStatus process() {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state) {
            case WAITING_SENDER:
                if (stringReader.process() == ProcessStatus.DONE) {
                    requester = (String) stringReader.get();
                    stringReader.reset();
                    state = State.WAITING_TARGET;
                } else {
                    return ProcessStatus.REFILL;
                }
            case WAITING_TARGET:
                if (stringReader.process() == ProcessStatus.DONE) {
                    target = (String) stringReader.get();
                    stringReader.reset();
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
        System.out.println("Asked by " + requester + " target= " + target);
        return new FramePrivateConnectionResponse(requester, target, FramePrivateConnectionResponse.OK_PRIVATE);
    }

    @Override
    public void reset() {
        state = State.WAITING_SENDER;
        requester = null;
        target=null;
    }
}
