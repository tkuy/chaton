package fr.upem.net.tcp.nonblocking.frame;

import fr.upem.net.tcp.nonblocking.FrameVisitor;

import java.nio.ByteBuffer;

public interface Frame {
	public int getOpCode();
	public void accept(FrameVisitor visitor);
	public ByteBuffer toByteBuffer();
}
