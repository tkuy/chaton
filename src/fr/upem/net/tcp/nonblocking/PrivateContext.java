package fr.upem.net.tcp.nonblocking;

import fr.upem.net.tcp.FillByteBuffer;
import fr.upem.net.tcp.nonblocking.frame.reader.IntReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * Treat the private connections. It only forwards the bytes from the sender to the target.
 */
class PrivateContext {
    private static int BUFFER_SIZE = 1024;
    final private SelectionKey key;
    final SocketChannel sc;
    final private ByteBuffer bbin;
    final private ByteBuffer bbout;
    final private Queue<ByteBuffer> queue = new LinkedList<>();
    final private ServerChaton server;
    private boolean closed = false;
    private IntReader intReader;
    static private Logger logger = Logger.getLogger(PrivateContext.class.getName());

    private static Charset UTF8 = StandardCharsets.UTF_8;
    PrivateContext(ServerChaton server, SelectionKey key, ByteBuffer bbin, ByteBuffer bbout){
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.server = server;
        this.bbin = bbin;
        this.bbout = bbout;
        this.intReader = new IntReader(bbin);
    }
    /**1
     * Process the content of bbin
     *
     * The convention is that bbin is in write-mode before the call
     * to process and after the call
     *
     */
    private void processIn() {
        for(;;){
            PrivateContext target = server.connections.get(sc);
            bbin.flip();
            int size = bbin.remaining();
            if(size!=0) {
                ByteBuffer tmpBbin = ByteBuffer.allocate(size).put(bbin);
                target.queueByteBuffer(tmpBbin);
                bbin.compact();
            }
            return;
        }
    }

    /**
     * Add a message to the bytebuffer queue, tries to fill bbOut and updateInterestOps
     *
     * @param byteBuffer
     */
    void queueByteBuffer(ByteBuffer byteBuffer) {
        logger.info(this +" " + "QueueByteBuffer send to "+this);
        queue.add(byteBuffer);
        processOut();
        updateInterestOps();
    }

    /**
     * Try to fill bbout from the message queue
     *
     */
    private void processOut() {
			while (!queue.isEmpty()) {
				ByteBuffer bb = queue.element();
                System.out.println("==>"+StandardCharsets.US_ASCII.decode(bb.flip()));
                bb.flip();
				if(bbout.remaining()>=bb.limit()) {
					bbout.put(bb);
					queue.poll();
				} else {
					//Not enough space. do not poll.
					//Write mode
					bb.compact();
					FillByteBuffer.fill(bb, bbout);
					//FillByteBuffer let the bbin bbout on write mode, compact is not useful.
					return;
				}
			}
    }

    /**
     * Update the interestOps of the key looking
     * only at values of the boolean closed and
     * of both ByteBuffers.
     *
     * The convention is that both buffers are in write-mode before the call
     * to updateInterestOps and after the call.
     * Also it is assumed that process has been be called just
     * before updateInterestOps.
     */

    void updateInterestOps() {
        int interestOps = 0;
        if(!closed && bbin.hasRemaining()) {
            interestOps |= SelectionKey.OP_READ;
        }
        if(bbout.position()!=0) {
            interestOps |= SelectionKey.OP_WRITE;
        }
        if(interestOps == 0) {
            silentlyClose();
        } else {
            key.interestOps(interestOps);
        }
    }

    private void silentlyClose() {
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    /**
     * Performs the read action on sc
     *
     * The convention is that both buffers are in write-mode before the call
     * to doRead and after the call
     *
     * @throws IOException
     */
    void doRead() throws IOException {

        if(sc.read(bbin) == -1) {
            closed = true;
        }
        processIn();
        updateInterestOps();
    }

    /**
     * Performs the write action on sc
     *
     * The convention is that both buffers are in write-mode before the call
     * to doWrite and after the call
     *
     * @throws IOException
     */

    void doWrite() throws IOException {
        sc.write(bbout.flip());
        bbout.compact();
        processOut();
        updateInterestOps();
    }

}