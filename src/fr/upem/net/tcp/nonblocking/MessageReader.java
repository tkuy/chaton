package fr.upem.net.tcp.nonblocking;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class MessageReader implements Reader {
    private enum State {DONE, WAITING_LOGIN, WAITING_TEXT, ERROR}

    ;

    private final ByteBuffer bb;
    private State state = State.WAITING_LOGIN;
    private String login;
    private String text;
    private StringReader stringReader;

    public MessageReader(ByteBuffer bb) {
        this.bb = bb;
        this.stringReader = new StringReader(bb);
    }

    @Override
    public ProcessStatus process() {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state) {
            case WAITING_LOGIN:
                if (stringReader.process() == ProcessStatus.DONE) {
                    login = (String) stringReader.get();
                    stringReader.reset();
                    state = State.WAITING_TEXT;
                } else {
                    return ProcessStatus.REFILL;
                }
            case WAITING_TEXT:
                if (stringReader.process() == ProcessStatus.DONE) {
                    text = (String) stringReader.get();
                    stringReader.reset();
                    state = State.DONE;
                    return ProcessStatus.DONE;
                } else {
                    return ProcessStatus.REFILL;
                }
        }

        return ProcessStatus.ERROR;
    }

    @Override
    public Object get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return new Message(login, text);
    }

    @Override
    public void reset() {
        state = State.WAITING_LOGIN;
        login = null;
        text = null;
    }

    public static void main(String[] args) {
        var utf8 = Charset.forName("UTF-8");
        var bb = ByteBuffer.allocate(1_024);
        var login = utf8.encode("coline");
        var content = utf8.encode("cc");
        bb.putInt(login.remaining()).put(login).putInt(content.remaining()).put(content)
                .putInt(login.flip().remaining()).put(utf8.encode("co"));
        var messageReader = new MessageReader(bb);
        System.out.println("test : " + bb.position());
        System.out.println("status : " + messageReader.process());
        var message = (Message) messageReader.get();
        System.out.println(message.getLogin() + " : " + message.getText() + "\n");
        messageReader.reset();

        System.out.println("=======");
        System.out.println("status : " + messageReader.process());
        bb.put(utf8.encode("line")).putInt(utf8.encode("test").remaining()).put(utf8.encode("test"));
        System.out.println("status : " + messageReader.process());
        message = (Message) messageReader.get();
        System.out.println(message.getLogin() + " :: " + message.getText() + "\n");
    }
}
