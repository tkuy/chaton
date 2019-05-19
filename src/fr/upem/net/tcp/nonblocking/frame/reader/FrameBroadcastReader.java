package fr.upem.net.tcp.nonblocking.frame.reader;

import fr.upem.net.tcp.nonblocking.Message;
import fr.upem.net.tcp.nonblocking.MessageReader;
import fr.upem.net.tcp.nonblocking.Reader;
import fr.upem.net.tcp.nonblocking.frame.FrameBroadcast;

import java.nio.ByteBuffer;

public class FrameBroadcastReader implements Reader {
    private enum State {DONE, WAITING_MESSAGE, ERROR}

    private final ByteBuffer bb;
    private State state = State.WAITING_MESSAGE;
    private Message message;
    private MessageReader messageReader;

    public FrameBroadcastReader(ByteBuffer bb) {
        this.bb = bb;
        this.messageReader = new MessageReader(bb);
    }

    @Override
    public ProcessStatus process() {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state) {
            case WAITING_MESSAGE:
                if (messageReader.process() == ProcessStatus.DONE) {
                    message = (Message) messageReader.get();
                    messageReader.reset();
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
        return new FrameBroadcast(message.getLogin(), message.getText());
    }

    @Override
    public void reset() {
        state = State.WAITING_MESSAGE;
        message=null;
    }
}
