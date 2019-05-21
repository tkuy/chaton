package test;

import fr.upem.net.tcp.nonblocking.frame.FrameLoginResponse;
import fr.upem.net.tcp.nonblocking.Message;
import fr.upem.net.tcp.nonblocking.MessageReader;
import fr.upem.net.tcp.nonblocking.frame.FrameBroadcast;
import fr.upem.net.tcp.nonblocking.frame.FrameLogin;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class FrameTest {
    private final static Charset UTF8 = StandardCharsets.UTF_8;
    @Test
    public void loginToByteBuffer() {
        ByteBuffer bb = ByteBuffer.allocate(256);
        FrameLogin frameLogin = new FrameLogin("Pasteur");
        ByteBuffer buffer = frameLogin.toByteBuffer();
        buffer.flip();
        bb.put(buffer).put(UTF8.encode("Shouldnt appear")).flip();
        int size = bb.getInt();
        String login = UTF8.decode(bb.limit(bb.position()+size)).toString();
        assertEquals("Pasteur",login);
    }
    @Test
    public void frameBroadcastToByteBuffer() {
        FrameBroadcast frameBroadcast = new FrameBroadcast("Login", "hello");
        ByteBuffer buffer = frameBroadcast.toByteBuffer();
        buffer.flip().getInt();
        buffer.compact();
        MessageReader messageReader = new MessageReader(buffer);
        messageReader.process();
        Message m = (Message) messageReader.get();
        assertEquals(m.getLogin(), "Login");
        assertEquals(m.getText(), "hello");
    }

    @Test
    public void frameLoginResponseReader() {
        FrameLoginResponse frameLoginResponse = new FrameLoginResponse(FrameLoginResponse.LOGIN_ACCEPTED);
        ByteBuffer buffer = frameLoginResponse.toByteBuffer();
        assertEquals(buffer.flip().getInt(), FrameLoginResponse.LOGIN_ACCEPTED);
        ByteBuffer buffer2 = new FrameLoginResponse(FrameLoginResponse.LOGIN_REFUSED).toByteBuffer();
        assertEquals(buffer2.flip().getInt(), FrameLoginResponse.LOGIN_REFUSED);
    }
}
