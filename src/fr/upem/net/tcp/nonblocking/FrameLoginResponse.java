package fr.upem.net.tcp.nonblocking;

import fr.upem.net.tcp.nonblocking.frame.Frame;

import java.nio.ByteBuffer;

public class FrameLoginResponse implements Frame {
    public final static int LOGIN_ACCEPTED = 1;
    public final static int LOGIN_REFUSED = 2;
    private final int response;

    public FrameLoginResponse(int response) {
        if(response != LOGIN_ACCEPTED && response != LOGIN_REFUSED) {
            throw new IllegalArgumentException("Use LOGIN_ACCEPTED or LOGIN_REFUSED");
        }
        this.response = response;
    }

    public int getResponse() {
        return response;
    }

    @Override
    public int getOpCode() {
        return 0;
    }

    @Override
    public void accept(FrameVisitor visitor) {
        //TODO
    }

    @Override
    public ByteBuffer toByteBuffer() {
        //TODO
        return null;
    }
}
