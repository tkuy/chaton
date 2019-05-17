package fr.upem.net.tcp.nonblocking.frame.reader;

import fr.upem.net.tcp.nonblocking.Message;
import fr.upem.net.tcp.nonblocking.MessageReader;
import fr.upem.net.tcp.nonblocking.Reader;
import fr.upem.net.tcp.nonblocking.StringReader;
import fr.upem.net.tcp.nonblocking.frame.FrameBroadcast;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class FrameBroadcastReader implements Reader {
    private enum State {DONE, WAITING_LOGIN, WAITING_TEXT, ERROR}

    private final ByteBuffer bb;
    private State state = State.WAITING_LOGIN;
    private String login;
    private String text;
    private StringReader stringReader;

    public FrameBroadcastReader(ByteBuffer bb) {
        this.bb = bb;
        this.stringReader = new StringReader(bb);
    }

    @Override
    public ProcessStatus process() {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state) {
            case WAITING_LOGIN:
                if (stringReader.process() == ProcessStatus.DONE) {
                    login = (String) stringReader.get();
                    stringReader.reset();
                    state = State.WAITING_TEXT;
                } else {
                    return ProcessStatus.REFILL;
                }
            case WAITING_TEXT:
                if (stringReader.process() == ProcessStatus.DONE) {
                    text = (String) stringReader.get();
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
        return new FrameBroadcast(login, text);
    }

    @Override
    public void reset() {
        state = State.WAITING_LOGIN;
        login = null;
        text = null;
    }
}
