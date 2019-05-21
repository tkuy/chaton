package fr.upem.net.tcp.nonblocking.frame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FrameLogin implements Frame{
    private static final int OP_CODE = 0;
    private final String login;
    private final static Charset UTF8 = StandardCharsets.UTF_8;
    public FrameLogin(String login) {
        this.login = login;
    }

    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    public void accept(FrameVisitor visitor) {
        visitor.visitLoginFrame(this);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        ByteBuffer login = UTF8.encode(this.login);
        ByteBuffer bb = ByteBuffer.allocate(login.remaining() + Integer.BYTES);
        bb.putInt(login.remaining()).put(login);
        return bb;
    }

    public String getLogin() {
        return login;
    }
}
