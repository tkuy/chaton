package fr.upem.net.tcp.nonblocking.frame;

import fr.upem.net.tcp.nonblocking.FrameVisitor;

import java.nio.ByteBuffer;

public class FrameLogin implements Frame{
    private static final int OP_CODE = 0;
    private final String login;
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
        //TODO
        return null;
    }

    public String getLogin() {
        return login;
    }
}
