package fr.upem.net.tcp.nonblocking.frame;

import fr.upem.net.tcp.nonblocking.FrameVisitor;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FrameLogin implements Frame{
    private static final int OP_CODE = 0;
    private final String login;
    private final static Charset UTF8 = StandardCharsets.UTF_8;
    private ByteBuffer bb;
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
        if(bb==null) {
            ByteBuffer login = UTF8.encode(this.login);
            login.flip();
            this.bb = ByteBuffer.allocate(login.remaining() + Integer.BYTES);
        }
        return this.bb;
    }

    public String getLogin() {
        return login;
    }
}
