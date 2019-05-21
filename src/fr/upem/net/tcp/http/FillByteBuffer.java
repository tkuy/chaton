package fr.upem.net.tcp.http;

import java.nio.ByteBuffer;

public class FillByteBuffer {
    public static void fill(ByteBuffer bbin, ByteBuffer bbout) {
        //mode ecriture

        //mode lecture
        bbin.flip();

        //Saisie la bonne proportion de bbin
        int oldLimit = bbin.limit();
        int sizeToExtract = bbout.remaining();
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
