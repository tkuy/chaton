package fr.upem.net.tcp.nonblocking.frame.reader;

import fr.upem.net.tcp.nonblocking.MessageReader;
import fr.upem.net.tcp.nonblocking.Reader;
import fr.upem.net.tcp.nonblocking.StringReader;
import fr.upem.net.tcp.nonblocking.frame.FrameLogin;

import java.nio.ByteBuffer;

public class FrameLoginReader implements Reader {
    private enum State {DONE, WAITING_LOGIN, ERROR}

    private State state = State.WAITING_LOGIN;
    private String login;
    private StringReader stringReader;

    public FrameLoginReader(ByteBuffer bb) {
        this.stringReader = new StringReader(bb);
    }

    @Override
    public Reader.ProcessStatus process() {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state) {
            case WAITING_LOGIN:
                ProcessStatus status = stringReader.process();
                System.out.println(status);
                if (status == ProcessStatus.DONE) {
                    login = (String) stringReader.get();
                    System.out.println(login);
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
        return new FrameLogin(login);
    }

    @Override
    public void reset() {
        state = State.WAITING_LOGIN;
        login = null;
    }

    public static void main(String[] args) {
        /*var utf8 = Charset.forName("UTF-8");
        var bb = ByteBuffer.allocate(1_024);
        var login = utf8.encode("coline");
        var content = utf8.encode("cc");
        bb.putInt(login.remaining()).put(login).putInt(content.remaining()).put(content)
                .putInt(login.flip().remaining()).put(utf8.encode("co"));
        var messageReader = new MessageReader(bb);
        System.out.println("test : " + bb.position());
        System.out.println("status : " + process());
        var message = (Message) get();
        System.out.println(message.getLogin() + " : " + message.getText() + "\n");
        reset();

        System.out.println("=======");
        System.out.println("status : " + process());
        bb.put(utf8.encode("line")).putInt(utf8.encode("test").remaining()).put(utf8.encode("test"));
        System.out.println("status : " + process());
        message = (Message) get();
        System.out.println(message.getLogin() + " :: " + message.getText() + "\n");*/
    }
}
