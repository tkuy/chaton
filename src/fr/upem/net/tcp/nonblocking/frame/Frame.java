package fr.upem.net.tcp.nonblocking.frame;

import java.nio.ByteBuffer;

public interface Frame {
	public int getOpCode();
	public void accept(FrameVisitor visitor);
	public ByteBuffer toByteBuffer();
}
