package fr.upem.net.tcp.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FramePrivateConnectionResponse implements Frame {
    public final static int OK_PRIVATE = 6;
    public final static int KO_PRIVATE = 7;
    private final static Charset UTF8 = StandardCharsets.UTF_8;
    private final String requester;
    private final String target;
    private int opCode;

    public FramePrivateConnectionResponse(String requester, String target, int op) {
        this.requester = requester;
        this.target = target;
        this.opCode = op;
    }

    @Override
    public int getOpCode() {
        if(opCode!=OK_PRIVATE && opCode!=KO_PRIVATE) {
            throw new IllegalArgumentException("OK_PRIVATE or KO_PRIVATE is allowed");
        }
        return opCode;
    }

    public String getRequester() {
        return requester;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public void accept(FrameVisitor visitor) {
        visitor.visitPrivateConnectionResponse(this);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        ByteBuffer requester = UTF8.encode(this.requester);
        ByteBuffer target = UTF8.encode(this.target);

        ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES * 3 + requester.remaining() + target.remaining());
        bb.putInt(opCode).putInt(requester.remaining()).put(requester).putInt(target.remaining()).put(target);
        return bb;
    }
}
