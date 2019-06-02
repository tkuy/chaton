package fr.upem.net.tcp.nonblocking.frame.reader;

import fr.upem.net.tcp.nonblocking.Reader;
import fr.upem.net.tcp.nonblocking.StringReader;
import fr.upem.net.tcp.nonblocking.frame.FrameLogin;
import fr.upem.net.tcp.nonblocking.frame.FramePrivateMessage;
import fr.upem.net.tcp.nonblocking.frame.FrameReader;

import java.nio.ByteBuffer;

public class FramePrivateMessageReader implements Reader {
    private enum State {DONE, WAITING_SENDER, WAITING_TARGET, WAITING_CONTENT, ERROR}

    private State state = State.WAITING_SENDER;
    private String sender;
    private String target;
    private String content;
    private StringReader stringReader;
    //MESSAGE_PRIVATE(4) = 4 (OPCODE) login_sender (STRING) login_target (STRING) msg (STRING)
    public FramePrivateMessageReader(ByteBuffer bb) {
        this.stringReader = new StringReader(bb);
    }

    @Override
    public Reader.ProcessStatus process() {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state) {
            case WAITING_SENDER:
                if (stringReader.process() == ProcessStatus.DONE) {
                    sender = (String) stringReader.get();
                    stringReader.reset();
                    state = State.WAITING_TARGET;
                } else {
                    return ProcessStatus.REFILL;
                }
            case WAITING_TARGET:
                if (stringReader.process() == ProcessStatus.DONE) {
                    target = (String) stringReader.get();
                    stringReader.reset();
                    state = State.WAITING_CONTENT;
                } else {
                    return ProcessStatus.REFILL;
                }
            case WAITING_CONTENT:
                if (stringReader.process() == ProcessStatus.DONE) {
                    content = (String) stringReader.get();
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
        return new FramePrivateMessage(sender,target,content);
    }

    @Override
    public void reset() {
        state = State.WAITING_SENDER;
        sender = null;
    }
}
