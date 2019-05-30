package fr.upem.net.tcp.nonblocking;

import fr.upem.net.tcp.nonblocking.frame.*;
import fr.upem.net.tcp.nonblocking.frame.reader.FrameLoginPrivateConnectionReader;
import fr.upem.net.tcp.nonblocking.frame.reader.FrameLoginReader;
import fr.upem.net.tcp.nonblocking.frame.reader.IntReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

class Context implements FrameVisitor {
    static private Logger logger = Logger.getLogger(Context.class.getName());
    static private int BUFFER_SIZE = 1_024;
    final private SelectionKey key;
    final private SocketChannel sc;
    final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
    final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
    final private Queue<Frame> queue = new LinkedList<>();
    final private ServerChaton server;
    private boolean closed = false;
    private String login;
    private boolean authenticated;
    private static Charset UTF8 = StandardCharsets.UTF_8;
    private FrameReader frameReader = new FrameReader(bbin);
    final private FrameLogReader frameLogReader = new FrameLogReader(bbin);
    Context(ServerChaton server, SelectionKey key){
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.server = server;
        //this.state = State.WAITING_OP;
    }

    /*private enum State {
        AUTHENTICATED
    }*/

    /**
     * Process the content of bbin
     *
     * The convention is that bbin is in write-mode before the call
     * to process and after the call
     *
     */
    private void processIn() {
        for(;;){
            //Use the FrameReader that read only allowed frame when user is authenticated
            Reader reader;
            if(!authenticated) {
                reader = frameLogReader;
            } else {
                reader = frameReader;
            }
            switch (reader.process()) {
                case DONE:
                    Frame frame = (Frame) reader.get();
                    if(frame==null) {
                        System.out.println("frame is null");
                    }
                    frame.accept(this);
                    reader.reset();
                    break;
                case REFILL:
                    return;
                case ERROR:
                    silentlyClose();
                    System.out.println("Error in processIn");
                    return;
            }
        }
    }

    /**
     * Add a message to the message queue, tries to fill bbOut and updateInterestOps
     *
     * @param frame
     */
    void queueFrame(Frame frame) {
        logger.info("Type of frame : "+frame.getOpCode() + " from "+login);
        queue.add(frame);
        processOut();
        updateInterestOps();
    }

    /**
     * Try to fill bbout from the message queue
     *
     */
    private void processOut() {
        while (!queue.isEmpty()) {
            Frame frame = queue.element();
            System.out.println("Send with opCode : "+frame.getOpCode());
            ByteBuffer bb = frame.toByteBuffer();
            bb.flip();
            if(bbout.remaining() >= bb.limit()) {
                bbout.put(bb);
                queue.poll();
            } else {
                bb.compact();
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

    private void updateInterestOps() {
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
            logger.info("Remove login from the map : "+login);
            if(login!=null) {
                server.pseudos.remove(login);
            }
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
    //Connection OP = 1 or 2
    @Override
    public void visitLoginFrame(FrameLogin frame) {
        String login = frame.getLogin();
        logger.info("Received request to connect for the login : "+ login);
        FrameLoginResponse frameResponse;
        if(!server.pseudos.containsKey(login)) {
            frameResponse = new FrameLoginResponse(FrameLoginResponse.LOGIN_ACCEPTED);
            server.pseudos.put(login, this);
            this.login = login;
            this.authenticated=true;
            logger.info("Connection accepted");
        } else {
            frameResponse = new FrameLoginResponse(FrameLoginResponse.LOGIN_REFUSED);
            logger.info("Connection refused");
        }
        queueFrame(frameResponse);
    }
    @Override
    public void visitResponseLoginFrame(FrameLoginResponse frame) {
        queueFrame(frame);
    }
    @Override
    public void visitBroadcastFrame(FrameBroadcast frame) {
        if(frame.getSender().equals(login)) {
            logger.info("Broadcast frame send by "+ login);
            server.broadcast(frame);
        }
    }

    @Override
    public void visitPrivateMessage(FramePrivateMessage frame) {
        Context targetCtx = server.pseudos.get(frame.getTarget());
        if(frame.getSender().equals(login) && targetCtx!=null) {
            logger.info("Private message from "+frame.getSender() + " to "+ frame.getTarget());
            targetCtx.queueFrame(frame);
        } else {
            logger.info("Private message from "+frame.getSender() + " to "+ frame.getTarget() + "failed");
        }
    }
    //OP = 5. Ask for a connection
    @Override
    public void visitPrivateConnection(FramePrivateConnection frame) {
        Context targetCtx = server.pseudos.get(frame.getTarget());
        if(frame.getRequester().equals(login) && targetCtx!=null) {
            targetCtx.queueFrame(frame);
            ArrayList<String> targets = server.requests.computeIfAbsent(frame.getRequester(), k -> new ArrayList());
            targets.add(frame.getTarget());
            logger.info("Private connection request from "+frame.getRequester() + " to "+ frame.getTarget());
        } else {
            logger.info("Private connection request from "+frame.getRequester() + " to "+ frame.getTarget() + "failed");
        }
    }
    //OP 6 OR 7. Accept or refuse the connection
    //Case 6  : Send the ID to the requester and target
    @Override
    public void visitPrivateConnectionResponse(FramePrivateConnectionResponse frame) {
        ArrayList<String> targets = server.requests.get(frame.getRequester());
        System.out.println(targets);
        System.out.println("visitPrivateConnectionResponse");
        //If the requester has made a request and
        if(targets!= null && targets.contains(frame.getTarget())) {
            if(frame.getOpCode() == FramePrivateConnectionResponse.OK_PRIVATE) {
                long id = new Random().nextLong();
                server.ids.add(id);
                FrameIdPrivateConnectionResponse frameIdPrivateConnectionResponse = new FrameIdPrivateConnectionResponse(frame.getRequester(), frame.getTarget(), id);
                this.queueFrame(frameIdPrivateConnectionResponse);
                System.out.println(server.pseudos.get(frame.getRequester()) + frame.getRequester());
                server.pseudos.get(frame.getRequester()).queueFrame(frameIdPrivateConnectionResponse);
                server.connectionsId.put(id, new ServerChaton.Pair());
            }
            targets.remove(frame.getTarget());
            if(targets.isEmpty()) {
                server.requests.remove(frame.getRequester());
            }
        }
    }
    //OP = 8. Receive the frame with the ID. Migrate from Context to ContextPrivate
    @Override
    public void visitLoginPrivateConnection(FrameLoginPrivateConnection frame) {
        ServerChaton.Pair pair = server.connectionsId.get(frame.getId());
        PrivateContext privateContext = new PrivateContext(server, key);
        if(pair.ctx1.isEmpty()) {
            pair.ctx1=Optional.of(privateContext);
            key.attach(privateContext);
            System.out.println("KEY: "+ this.key);
        } else if (pair.ctx2.isEmpty()){
            pair.setCtx2(Optional.of(privateContext));
            this.key.attach(privateContext);
            System.out.println("KEY: "+ this.key);
            server.connectionsId.remove(frame.getId());
            server.connections.put(pair.getCtx1().get().sc, pair.getCtx2().get());
            server.connections.put(pair.getCtx2().get().sc, pair.getCtx1().get());

            System.out.println(pair.getCtx1().get().sc);
            System.out.println(pair.getCtx2().get().sc);
            if(pair.getCtx1().get().sc.equals(pair.getCtx2().get().sc)) {
                throw new IllegalStateException("SocketChannel identique");
            }
            //FIXME: Force en mode bloquant !!!
            try {
                sc.write(ByteBuffer.allocate(Integer.BYTES).putInt(10).flip());
            } catch (IOException e) {
                e.printStackTrace();
            }
            pair.getCtx1().get().queueByteBuffer(new FramePrivateEstablished().toByteBuffer());
            pair.getCtx2().get().queueByteBuffer(new FramePrivateEstablished().toByteBuffer());
            logger.info("Connection Established");
        } else {
            throw new IllegalStateException("Connection already established");
        }
    }
}