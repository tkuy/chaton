package test;

import fr.upem.net.tcp.nonblocking.Reader;
import fr.upem.net.tcp.nonblocking.frame.reader.FrameBroadcastReader;
import fr.upem.net.tcp.nonblocking.frame.reader.FrameLoginReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

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
}
