package fr.upem.net.tcp.nonblocking;

public class Message {
    private String login;
    private String text;

    public Message(String login, String text) {
        this.login = login;
        this.text = text;
    }

    public String getLogin() {
        return login;
    }

    public String getText() {
        return text;
    }
}
