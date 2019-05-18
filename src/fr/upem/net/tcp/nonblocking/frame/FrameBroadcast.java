package fr.upem.net.tcp.nonblocking.frame;

import fr.upem.net.tcp.nonblocking.FrameVisitor;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FrameBroadcast implements Frame {
    private static final int OP_CODE = 3;
    private final String login;
    private final String message;
    private ByteBuffer bb;
    private final static Charset UTF8 = StandardCharsets.UTF_8;
    public FrameBroadcast(String login, String message) {
        this.login = login;
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
        if(this.bb==null) {
            ByteBuffer login = UTF8.encode(this.login);
            ByteBuffer message = UTF8.encode(this.message);
            login.flip();
            message.flip();
            ByteBuffer bb = ByteBuffer.allocate(login.remaining() + message.remaining() + Integer.BYTES * 3);
            bb.putInt(3).putInt(login.limit()).put(login).putInt(message.limit()).put(message);
            this.bb = bb;
        }
        return this.bb;
    }

    public String getMessage() {
        return message;
    }

    public String getLogin() {
        return login;
    }

}
