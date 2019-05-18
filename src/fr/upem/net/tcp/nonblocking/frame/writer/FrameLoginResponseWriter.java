package fr.upem.net.tcp.nonblocking.frame.writer;

import fr.upem.net.tcp.nonblocking.FrameLoginResponse;
import fr.upem.net.tcp.nonblocking.Writer;
import fr.upem.net.tcp.nonblocking.frame.Frame;
import fr.upem.net.tcp.nonblocking.frame.FrameLogin;

import java.nio.ByteBuffer;

public class FrameLoginResponseWriter {
    private final ByteBuffer bbout;
    private final static int size = Integer.BYTES;
    public FrameLoginResponseWriter(ByteBuffer bbout) {
        this.bbout = bbout;
    }
    public ByteBuffer process(Frame frame) {
        FrameLoginResponse frameLogin = (FrameLoginResponse) frame;
        ByteBuffer bb = ByteBuffer.allocate(size);
        bb.putInt(frameLogin.getResponse());
        return bb;
    }
}
