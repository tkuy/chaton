package fr.upem.net.tcp.nonblocking.frame;

public class FrameBroadcast implements Frame {
    private static final int OP_CODE = 3;
    private final String login;
    private final String message;
    public FrameBroadcast(String login, String message) {
        this.login = login;
        this.message = message;
    }

    public int getOpCode() {
        return OP_CODE;
    }

    public String getMessage() {
        return message;
    }

    public String getLogin() {
        return login;
    }
}
