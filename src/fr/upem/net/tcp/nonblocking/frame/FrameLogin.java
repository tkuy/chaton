package fr.upem.net.tcp.nonblocking.frame;

public class FrameLogin implements Frame{
    private static final int OP_CODE = 0;
    private final String login;
    public FrameLogin(String login) {
        this.login = login;
    }

    public int getOpCode() {
        return OP_CODE;
    }

    public String getLogin() {
        return login;
    }
}
