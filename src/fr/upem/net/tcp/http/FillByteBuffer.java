package fr.upem.net.tcp.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class FillByteBuffer {
    /**
     * Try to fill bbout with bbin. Take and returns bbin and bbout on write mode.
     * @param bbin
     * @param bbout
     */
    public static void fill(ByteBuffer bbin, ByteBuffer bbout) {
        //mode ecriture

        //mode lecture
        bbin.flip();
        //System.out.println("refill with this : " + StandardCharsets.UTF_8.decode(bbin).toString());
        //Saisie la bonne proportion de bbin
        int oldLimit = bbin.limit();
        int sizeToExtract = bbout.remaining();
        System.out.println("Size to Extract : " + sizeToExtract);
        bbin.limit(sizeToExtract);
        //Remplie le bbout
        bbout.put(bbin);
        //Prepare pour le compact. position apres la selection inclue dans bbout
        //Remet la limite
        bbin.position(sizeToExtract);
        bbin.limit(oldLimit);
        //Mode ecriture
        bbin.compact();
    }
}
