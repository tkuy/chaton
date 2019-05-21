package test;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ByteBufferTest {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    @Test
    public void forwardBb() {

        ByteBuffer bbin = ByteBuffer.allocate(1024);
        bbin.put(StandardCharsets.UTF_8.encode("Une longue chaine de caracteres qui devraient etre trop longues"));
        ByteBuffer bbout = ByteBuffer.allocate(10);
        //Met 3 Bytes
        bbout.put(UTF8.encode("hel"));
        System.out.println(bbout.remaining());
        //pos =
        bbin.flip();
        int oldLimit = bbin.limit();
        int tailleAExtraire = bbout.remaining();
        bbin.limit(tailleAExtraire);
        bbout.put(bbin);
        System.out.println(UTF8.decode(bbin.flip()));
        System.out.println(UTF8.decode(bbout.flip()));
        System.out.println("\n");
        bbin.position(tailleAExtraire);
        bbin.limit(oldLimit);
        bbin.compact();
        bbin.put(UTF8.encode("popo"));
        System.out.println(UTF8.decode(bbin.flip()));
        /*bbin.position(0);
        int oldLimit = bbin.limit();
        int tailleAExtraire = bbout.remaining();
        bbin.limit(tailleAExtraire);
        bbout.put(bbin);
        System.out.println(UTF8.decode(bbout.flip()));

        bbin.compact();
        //bbin.position(bbin.limit() - tailleAExtraire +1);
        //bbin.limit(oldLimit);
        System.out.println(UTF8.decode(bbin));*/
    }
}
