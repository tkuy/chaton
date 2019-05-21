package fr.upem.net.tcp.nonblocking.frame;

import java.nio.ByteBuffer;

public class FrameLoginPrivateConnection implements Frame {
    private static final int OP_CODE = 9;

    public long getId() {
        return id;
    }

    public FrameLoginPrivateConnection(long id) {
        this.id = id;
    }

    private final long id;
    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    public void accept(FrameVisitor visitor) {
        visitor.visitLoginPrivateConnection(this);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        return ByteBuffer.allocate(Integer.BYTES + Long.BYTES).putInt(OP_CODE).putLong(id);
    }
}
