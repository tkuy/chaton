package fr.upem.net.tcp.nonblocking.frame;

import fr.upem.net.tcp.nonblocking.FrameVisitor;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FramePrivateMessage implements Frame{
    private final String sender;
    private final String target;
    private final String content;
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    public static final int OP_CODE = 4;
    public FramePrivateMessage(String sender, String target, String content) {
        this.sender = sender;
        this.target = target;
        this.content = content;
    }


    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    public void accept(FrameVisitor visitor) {
        visitor.visitPrivateMessage(this);
    }

    public String getSender() {
        return sender;
    }

    public String getTarget() {
        return target;
    }

    public String getContent() {
        return content;
    }

    @Override
    public ByteBuffer toByteBuffer() {
        //MESSAGE_PRIVATE(4) = 4 (OPCODE) login_sender (STRING) login_target (STRING) msg (STRING)
        ByteBuffer sender = UTF8.encode(this.sender);
        ByteBuffer target = UTF8.encode(this.target);
        ByteBuffer content = UTF8.encode(this.content);
        ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES * 3 + sender.remaining() + target.remaining() + content.remaining());
        bb.putInt(OP_CODE).putInt(sender.remaining()).put(sender)
                .putInt(target.remaining()).put(target)
                .putInt(content.remaining()).put(content);
        return bb;
    }
}
