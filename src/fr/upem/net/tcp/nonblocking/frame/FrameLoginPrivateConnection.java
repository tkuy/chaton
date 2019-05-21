package fr.upem.net.tcp.nonblocking.frame;

import java.nio.ByteBuffer;

public class FrameLoginPrivateConnection implements Frame {
    private static final int OP_CODE = 9;
    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    public void accept(FrameVisitor visitor) {
        //TODO
    }

    @Override
    public ByteBuffer toByteBuffer() {
        return null;
    }

    @Override
    public void accept(FramePrivateVisitor visitor) {

    }
}
