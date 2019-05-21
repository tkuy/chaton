package fr.upem.net.tcp.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FrameIdPrivateConnectionResponse implements Frame {
    private final static int OP_CODE = 8;
    private final Charset UTF8 = StandardCharsets.UTF_8;
    private final String requester;
    private final String target;
    private final long id;

    public FrameIdPrivateConnectionResponse(String requester, String target, long id) {
        this.requester = requester;
        this.target = target;
        this.id = id;
    }

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
        ByteBuffer sender = UTF8.encode(this.requester);
        ByteBuffer target = UTF8.encode(this.target);

        ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES * 3 + sender.remaining() + target.remaining());
        bb.putInt(OP_CODE).putInt(sender.remaining()).put(sender).putInt(target.remaining()).put(target).putLong(id);
        return bb;
    }
}
