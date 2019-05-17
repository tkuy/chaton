package test;

import fr.upem.net.tcp.nonblocking.Message;
import fr.upem.net.tcp.nonblocking.MessageReader;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class FrameReaderTest {

    @Test
    public void loginReader() {
        var utf8 = Charset.forName("UTF-8");
        var bb = ByteBuffer.allocate(1_024);
        var login = utf8.encode("coline");
        var content = utf8.encode("cc");
        bb.putInt(login.remaining()).put(login).putInt(content.remaining()).put(content)
                .putInt(login.flip().remaining()).put(utf8.encode("co"));
        var messageReader = new MessageReader(bb);
        System.out.println("test : " + bb.position());
        //System.out.println("status : " + process());
        //var message = (Message) get();
        //System.out.println(message.getLogin() + " : " + message.getText() + "\n");
        //reset();
    }
}
