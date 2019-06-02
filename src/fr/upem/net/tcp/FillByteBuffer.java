package fr.upem.net.tcp;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class FillByteBuffer {
    /**
     * Try to fill bbout with bbin. Take and returns bbin and bbout on write mode.
     * @param bbin must be in write mode
     * @param bbout must be in write mode
     */
    public static void fill(ByteBuffer bbin, ByteBuffer bbout) {
        //mode ecriture

        //mode lecture
        bbin.flip();
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
