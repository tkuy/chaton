package fr.upem.net.tcp.nonblocking.frame.reader;

import fr.upem.net.tcp.nonblocking.Reader;
import fr.upem.net.tcp.nonblocking.StringReader;
import fr.upem.net.tcp.nonblocking.frame.FrameLogin;

import java.nio.ByteBuffer;

public class FrameLoginReader implements Reader {
    private enum State {DONE, WAITING_LOGIN, ERROR}

    private State state = State.WAITING_LOGIN;
    private String login;
    private StringReader stringReader;

    public FrameLoginReader(ByteBuffer bb) {
        this.stringReader = new StringReader(bb);
    }

    @Override
    public Reader.ProcessStatus process() {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state) {
            case WAITING_LOGIN:
                ProcessStatus status = stringReader.process();
                if (status == ProcessStatus.DONE) {
                    login = (String) stringReader.get();
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
        return new FrameLogin(login);
    }

    @Override
    public void reset() {
        state = State.WAITING_LOGIN;
        login = null;
    }
}
