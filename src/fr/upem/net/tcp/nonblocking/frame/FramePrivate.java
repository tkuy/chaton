package fr.upem.net.tcp.nonblocking.frame;

import java.nio.ByteBuffer;

public interface FramePrivate {
    public int getOpCode();
    public void accept(FramePrivateVisitor visitor);
    public ByteBuffer toByteBuffer();
}
