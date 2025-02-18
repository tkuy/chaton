package fr.upem.net.tcp.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FrameBroadcast implements Frame {
    private static final int OP_CODE = 3;
    private final String sender;
    private final String message;
    private final static Charset UTF8 = StandardCharsets.UTF_8;
    public FrameBroadcast(String login, String message) {
        this.sender = login;
        this.message = message;
    }

    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    public void accept(FrameVisitor visitor) {
        visitor.visitBroadcastFrame(this);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        ByteBuffer login = UTF8.encode(this.sender);
        ByteBuffer message = UTF8.encode(this.message);
        ByteBuffer bb = ByteBuffer.allocate(login.remaining() + message.remaining() + Integer.BYTES * 3);
        bb.putInt(3).putInt(login.remaining()).put(login).putInt(message.remaining()).put(message);
        return bb;
    }

    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }
}
