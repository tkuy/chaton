package test;

import fr.upem.net.tcp.nonblocking.Message;
import fr.upem.net.tcp.nonblocking.MessageReader;
import fr.upem.net.tcp.nonblocking.frame.FrameLogin;
import fr.upem.net.tcp.nonblocking.frame.reader.FrameLoginReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class FrameReaderTest {
    private final static Charset UTF8 = StandardCharsets.UTF_8;
    @Test
    public void loginReader() {
        ByteBuffer bb = ByteBuffer.allocate(256);
        FrameLogin frameLogin = new FrameLogin("Pasteur");
        ByteBuffer buffer = frameLogin.toByteBuffer();
        buffer.flip();
        bb.put(buffer).put(UTF8.encode("Shouldnt appear")).flip();
        int size = bb.getInt();
        String login = UTF8.decode(bb.limit(bb.position()+size)).toString();
        assertEquals("Pasteur",login);
    }
}
