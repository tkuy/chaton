package fr.upem.net.tcp.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FramePrivateConnection implements Frame {
    private final static int OP_CODE = 5;
    private final static Charset UTF8 = StandardCharsets.UTF_8;
    private final String requester;
    private final String target;

    public FramePrivateConnection(String requester, String target) {
        this.requester = requester;
        this.target = target;
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    public String getRequester() {
        return requester;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public void accept(FrameVisitor visitor) {
        visitor.visitPrivateConnection(this);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        ByteBuffer sender = UTF8.encode(this.requester);
        ByteBuffer target = UTF8.encode(this.target);

        ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES * 3 + sender.remaining() + target.remaining());
        bb.putInt(OP_CODE).putInt(sender.remaining()).put(sender).putInt(target.remaining()).put(target);
        return bb;
    }
}
