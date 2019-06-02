package test;

import fr.upem.net.tcp.nonblocking.Reader;
import fr.upem.net.tcp.nonblocking.frame.FramePrivateConnection;
import fr.upem.net.tcp.nonblocking.frame.FramePrivateMessage;
import fr.upem.net.tcp.nonblocking.frame.reader.FrameBroadcastReader;
import fr.upem.net.tcp.nonblocking.frame.reader.FrameLoginReader;
import fr.upem.net.tcp.nonblocking.frame.reader.FramePrivateConnectionReader;
import fr.upem.net.tcp.nonblocking.frame.reader.FramePrivateMessageReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The frame reader
 */
public class FrameReaderTest {
    private static Charset UTF8 = StandardCharsets.UTF_8;
    @Test
    public void loginFrameReader() {
        ByteBuffer bbin = ByteBuffer.allocate(256);
        FrameLoginReader frameLoginReader = new FrameLoginReader(bbin);
        Reader.ProcessStatus status = frameLoginReader.process();
        assertEquals(status, Reader.ProcessStatus.REFILL);
        ByteBuffer login = UTF8.encode("Login");
        bbin.putInt(login.remaining());
        status = frameLoginReader.process();
        assertEquals(status, Reader.ProcessStatus.REFILL);
        bbin.put(login);
        status = frameLoginReader.process();
        assertEquals(Reader.ProcessStatus.DONE, status);
    }

    @Test
    public void broadcastFrameReader() {
        ByteBuffer bbin = ByteBuffer.allocate(256);
        FrameBroadcastReader frameBroadcastReader = new FrameBroadcastReader(bbin);
        Reader.ProcessStatus status = frameBroadcastReader.process();
        assertEquals(status, Reader.ProcessStatus.REFILL);
        ByteBuffer login = UTF8.encode("Login");
        bbin.putInt(login.remaining());
        status = frameBroadcastReader.process();
        assertEquals(status, Reader.ProcessStatus.REFILL);
        bbin.put(login);
        status = frameBroadcastReader.process();
        assertEquals(Reader.ProcessStatus.REFILL, status);

        ByteBuffer message = UTF8.encode("The message");
        bbin.putInt(message.remaining()).put(message);
        status = frameBroadcastReader.process();
        assertEquals(Reader.ProcessStatus.DONE, status);
    }

    @Test
    public void privateMessageFrameReader() {
        ByteBuffer bbin = ByteBuffer.allocate(256);
        FramePrivateMessageReader framePrivateMessageReader = new FramePrivateMessageReader(bbin);
        Reader.ProcessStatus status = framePrivateMessageReader.process();
        assertEquals(status, Reader.ProcessStatus.REFILL);
        ByteBuffer login = UTF8.encode("sender");
        bbin.putInt(login.remaining());
        status = framePrivateMessageReader.process();
        assertEquals(status, Reader.ProcessStatus.REFILL);
        bbin.put(login);
        status = framePrivateMessageReader.process();
        assertEquals(Reader.ProcessStatus.REFILL, status);

        ByteBuffer target = UTF8.encode("The_target");
        bbin.putInt(target.remaining()).put(target);
        status = framePrivateMessageReader.process();
        assertEquals(Reader.ProcessStatus.REFILL, status);

        ByteBuffer message = UTF8.encode("The message");
        bbin.putInt(message.remaining()).put(message);
        status = framePrivateMessageReader.process();
        assertEquals(Reader.ProcessStatus.DONE, status);
    }

    @Test
    public void privateConnectionReader() {
        ByteBuffer bbin = ByteBuffer.allocate(256);
        FramePrivateConnectionReader framePrivateConnectionReader = new FramePrivateConnectionReader(bbin);
        Reader.ProcessStatus status = framePrivateConnectionReader.process();
        assertEquals(status, Reader.ProcessStatus.REFILL);
        ByteBuffer login = UTF8.encode("sender");
        bbin.putInt(login.remaining());
        status = framePrivateConnectionReader.process();
        assertEquals(status, Reader.ProcessStatus.REFILL);
        bbin.put(login);
        status = framePrivateConnectionReader.process();
        assertEquals(Reader.ProcessStatus.REFILL, status);

        ByteBuffer target = UTF8.encode("The_target");
        bbin.putInt(target.remaining()).put(target);
        status = framePrivateConnectionReader.process();
        assertEquals(Reader.ProcessStatus.DONE, status);
    }
}
