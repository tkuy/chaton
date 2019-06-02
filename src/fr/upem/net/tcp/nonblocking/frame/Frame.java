package fr.upem.net.tcp.nonblocking.frame;

import java.nio.ByteBuffer;

public interface Frame {
	/**
	 *	To get the opCode of the frame
	 * @return the opCode of the frame
	 */
	public int getOpCode();

	/**
	 * Use the method of the visitor
	 * @param visitor
	 */
	public void accept(FrameVisitor visitor);

	/**
	 * Take the fields of the frame and return a ByteBuffer
	 * @return the byteBuffer in write mode
	 */
	public ByteBuffer toByteBuffer();
}
