package fr.upem.net.tcp.nonblocking.frame.reader;

import fr.upem.net.tcp.nonblocking.Reader;
import fr.upem.net.tcp.nonblocking.StringReader;
import fr.upem.net.tcp.nonblocking.frame.FramePrivateConnection;
import fr.upem.net.tcp.nonblocking.frame.FramePrivateConnectionResponse;

import java.nio.ByteBuffer;
import java.util.Optional;

public class FramePrivateConnectionResponseReader implements Reader{
    private enum State {DONE, WAITING_SENDER, WAITING_TARGET, ERROR}

    private State state = State.WAITING_SENDER;
    private String requester;
    private String target;
    private int opCode;
    private StringReader stringReader;
    public FramePrivateConnectionResponseReader(ByteBuffer bb) {
        this.stringReader = new StringReader(bb);
    }
    //OK_PRIVATE(6) = 6 (OPCODE) login_requester (STRING) login_target (STRING)
    //KO_PRIVATE(7) = 7 (OPCODE) login_requester (STRING) login_target (STRING)
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
        return new FramePrivateConnectionResponse(requester, target, opCode);
    }
    public void setResponse(int opCode) {
        this.opCode = opCode;
    }

    @Override
    public void reset() {
        state = State.WAITING_SENDER;
        opCode=0;
        requester = null;
        target=null;
    }
}
