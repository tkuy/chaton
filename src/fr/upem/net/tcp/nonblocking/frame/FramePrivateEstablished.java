package fr.upem.net.tcp.nonblocking.frame;

import fr.upem.net.tcp.nonblocking.frame.Frame;

import java.nio.ByteBuffer;

public class FramePrivateEstablished implements Frame {
    private final static int OP_CODE = 10;
    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    public void accept(FrameVisitor visitor) {
        //Do nothing
    }

    @Override
    public ByteBuffer toByteBuffer() {
        return ByteBuffer.allocate(Integer.BYTES).putInt(OP_CODE);
    }
}
