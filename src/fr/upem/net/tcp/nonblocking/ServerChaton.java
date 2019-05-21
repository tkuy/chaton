package fr.upem.net.tcp.nonblocking;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.upem.net.tcp.nonblocking.frame.*;
import fr.upem.net.tcp.nonblocking.frame.reader.FrameLoginPrivateConnectionReader;
import fr.upem.net.tcp.nonblocking.frame.reader.FrameLoginReader;

public class ServerChaton {

	static private class Pair {
		private Optional<PrivateContext> ctx1;
		private Optional<PrivateContext> ctx2;

		public Pair() {
			this.ctx1 = Optional.empty();
			this.ctx2 = Optional.empty();
		}

		public Optional<PrivateContext> getCtx1() {
			return ctx1;
		}

		public void setCtx1(Optional<PrivateContext> ctx1) {
			this.ctx1 = ctx1;
		}

		public Optional<PrivateContext> getCtx2() {
			return ctx2;
		}

		public void setCtx2(Optional<PrivateContext> ctx2) {
			this.ctx2 = ctx2;
		}
	}
    static private class Context implements FrameVisitor{

        final private SelectionKey key;
        final private SocketChannel sc;
        final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
        final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
        final private Queue<Frame> queue = new LinkedList<>();
        final private ServerChaton server;
        private boolean closed = false;
        private String login;
        private State state;

      
        private static Charset UTF8 = StandardCharsets.UTF_8;
        private FrameReader frameReader = new FrameReader(bbin);
        private FrameLoginReader frameLoginReader = new FrameLoginReader(bbin);
        private IntReader opLoginReader = new IntReader(bbin);
        private FrameLoginPrivateConnectionReader frameLoginPrivateConnectionReader = new FrameLoginPrivateConnectionReader(bbin);
        private Context(ServerChaton server, SelectionKey key){
            this.key = key;
            this.sc = (SocketChannel) key.channel();
            this.server = server;
            this.state = State.WAITING_OP;
        }

        private enum State {
            WAITING_OP, WAITING_FRAME_LOGIN, WAITING_FRAME_PRIVATE_LOGIN, AUTHENTICATED
        }
        
        /**
         * Process the content of bbin
         *
         * The convention is that bbin is in write-mode before the call
         * to process and after the call
         *
         */
        private void processIn() {
        	for(;;){
        		switch (state) {
					case WAITING_OP:
						//Waiting OP = 0 to start the connection
						switch (opLoginReader.process()) {
							case DONE:
								int op = (int) opLoginReader.get();
								if (op == 0) {
									System.out.println("GET OP 0");
									this.state = State.WAITING_FRAME_LOGIN;
									opLoginReader.reset();
									processIn();
								} else if(op == 9) {
									this.state = State.WAITING_FRAME_PRIVATE_LOGIN;
									opLoginReader.reset();
									processIn();
								}
							case REFILL:
								return;
							case ERROR:
								silentlyClose();
								return;
						}
					case WAITING_FRAME_LOGIN:
						//Waiting login
						switch (frameLoginReader.process()) {
							case DONE:
								Frame frame = (Frame) frameLoginReader.get();
								frame.accept(this);
								state = State.AUTHENTICATED;
								frameLoginReader.reset();
								logger.info("The user is now authenticated as "+ login);
								processIn();
							case REFILL:
								return;
							case ERROR:
								silentlyClose();
								System.out.println("Error in processIn");
								return;
						}
					case WAITING_FRAME_PRIVATE_LOGIN :
						//Recoit ACCEPT
						switch (frameLoginPrivateConnectionReader.process()) {
							case DONE:
								Frame frame = (Frame) frameLoginPrivateConnectionReader.get();
								frame.accept(this);
								frameLoginPrivateConnectionReader.reset();
								logger.info("Started a private connection ");
								processIn();
							case REFILL:
								return;
							case ERROR:
								silentlyClose();
								System.out.println("Error in processIn");
								return;
						}
                    case AUTHENTICATED:
						//Use the FrameReader that read only allowed frame when user is authenticated
                        switch (frameReader.process()) {
                            case DONE:
                                Frame frame = (Frame) frameReader.get();
                                if(frame==null) {
                                	System.out.println("frame is null");
                                }
                                frame.accept(this);
                                frameReader.reset();
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
        }

        /**
         * Add a message to the message queue, tries to fill bbOut and updateInterestOps
         *
         * @param frame
         */
        private void queueFrame(Frame frame) {
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
        private void doRead() throws IOException {

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

        private void doWrite() throws IOException {
			sc.write(bbout.flip());
			bbout.compact();
			processOut();
			updateInterestOps();
        }

		@Override
		public void visitLoginFrame(FrameLogin frame) {
			String login = frame.getLogin();
			logger.info("Received request to connect for the login : "+ login);
			FrameLoginResponse frameResponse;
			if(!server.pseudos.containsKey(login)) {
				frameResponse = new FrameLoginResponse(FrameLoginResponse.LOGIN_ACCEPTED);
				server.pseudos.put(login, this);
				this.login = login;
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
                    server.connectionsId.put(id, new Pair());
                }
                targets.remove(frame.getTarget());
                if(targets.isEmpty()) {
                    server.requests.remove(frame.getRequester());
                }
            }
		}

		@Override
		public void visitLoginPrivateConnection(FrameLoginPrivateConnection frame) {
			Pair pair = server.connectionsId.get(frame.getId());
			PrivateContext privateContext = new PrivateContext(server, key);
			if(pair.ctx1.isEmpty()) {
				pair.ctx1=Optional.of(privateContext);
				key.attach(privateContext);
			} else if (pair.ctx2.isEmpty()){
				pair.ctx2=Optional.of(privateContext);
				key.attach(privateContext);
				server.connectionsId.remove(frame.getId());
				server.connections.put(pair.getCtx1().get().sc, pair.getCtx2().get());
				server.connections.put(pair.getCtx2().get().sc, pair.getCtx1().get());
				pair.getCtx1().get().queueFrame(new FramePrivateEstablished());
				pair.getCtx2().get().queueFrame(new FramePrivateEstablished());
				logger.info("Connection Established");
			} else {
				throw new IllegalStateException("Connection already established");
			}
		}
	}

	static private class PrivateContext implements FramePrivateVisitor{
		final private SelectionKey key;
		final private SocketChannel sc;
		final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
		final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
		final private Queue<Frame> queue = new LinkedList<>();
		final private ServerChaton server;
		private boolean closed = false;
		private String login;
		private Context.State state;


		private static Charset UTF8 = StandardCharsets.UTF_8;
		private FrameReader frameReader = new FrameReader(bbin);
		private FrameLoginReader frameLoginReader = new FrameLoginReader(bbin);
		private IntReader opLoginReader = new IntReader(bbin);
		private PrivateContext(ServerChaton server, SelectionKey key){
			this.key = key;
			this.sc = (SocketChannel) key.channel();
			this.server = server;
			this.state = Context.State.WAITING_OP;
		}

		@Override
		public void visit(FrameLoginPrivateConnection frame) {

		}

		private enum State {
			WAITING_OP, WAITING_FRAME_LOGIN, AUTHENTICATED
		}

		/**
		 * Process the content of bbin
		 *
		 * The convention is that bbin is in write-mode before the call
		 * to process and after the call
		 *
		 */
		private void processIn() {
			for(;;){
				switch (state) {
					case WAITING_OP:
						//Waiting OP = 0 to start the connection
						switch (opLoginReader.process()) {
							case DONE:
								int op = (int) opLoginReader.get();
								if (op == 0) {
									System.out.println("GET OP 0");
									this.state = Context.State.WAITING_FRAME_LOGIN;
									opLoginReader.reset();
									processIn();
								}
							case REFILL:
								return;
							case ERROR:
								silentlyClose();
								return;
						}
				}
			}
		}

		/**
		 * Add a message to the message queue, tries to fill bbOut and updateInterestOps
		 *
		 * @param frame
		 */
		private void queueFrame(Frame frame) {
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
		private void doRead() throws IOException {

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

		private void doWrite() throws IOException {
			sc.write(bbout.flip());
			bbout.compact();
			processOut();
			updateInterestOps();
		}

	}

    static private int BUFFER_SIZE = 1_024;
    static private Logger logger = Logger.getLogger(ServerChaton.class.getName());
	private final Map<String, Context> pseudos = new HashMap<>();
	private final Map<String, ArrayList<String>> requests = new HashMap<>();
    private final ArrayList<Long> ids = new ArrayList<>();
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final Map<Long, Pair> connectionsId = new HashMap<>();
    private final Map<SocketChannel, PrivateContext> connections = new HashMap<>();

    public ServerChaton(int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        selector = Selector.open();
    }

    public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		while(!Thread.interrupted()) {
			printKeys(); // for debug
			System.out.println("Starting select");
			try {
				selector.select(this::treatKey);
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
			System.out.println("Select finished");
		}
    }

	private void treatKey(SelectionKey key) {
		printSelectedKey(key); // for debug
		try {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
		} catch(IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
		try {
			if (key.isValid() && key.isWritable()) {
				((Context) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((Context) key.attachment()).doRead();
			}
		} catch (IOException e) {
			logger.log(Level.INFO,"Connection closed with client due to IOException",e);
			silentlyClose(key);
		}
	}

    private void doAccept(SelectionKey key) throws IOException {
		SocketChannel sc = serverSocketChannel.accept();
		if(sc == null) {
			return;
		}
		sc.configureBlocking(false);
		SelectionKey newKey = sc.register(selector, SelectionKey.OP_READ);
		newKey.attach(new Context(this, newKey));
    }

    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    /**
     * Add a message to all connected clients queue
     *
     * @param frame
     */
    private void broadcast(Frame frame) {
		for (SelectionKey key : selector.keys()) {
			Object attachment = key.attachment();
			if(attachment!=null) {
				Context ctx = (Context) key.attachment();
				ctx.queueFrame(frame);
			}
		}
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length!=1){
            usage();
            return;
        }
        new ServerChaton(Integer.parseInt(args[0])).launch();
    }

    private static void usage(){
        System.out.println("Usage : ServerSumBetter port");
    }

	/***
	 *  Theses methods are here to help understanding the behavior of the selector
	 ***/

	private String interestOpsToString(SelectionKey key){
		if (!key.isValid()) {
			return "CANCELLED";
		}
		int interestOps = key.interestOps();
		ArrayList<String> list = new ArrayList<>();
		if ((interestOps&SelectionKey.OP_ACCEPT)!=0) list.add("OP_ACCEPT");
		if ((interestOps&SelectionKey.OP_READ)!=0) list.add("OP_READ");
		if ((interestOps&SelectionKey.OP_WRITE)!=0) list.add("OP_WRITE");
		return String.join("|",list);
	}

	public void printKeys() {
		Set<SelectionKey> selectionKeySet = selector.keys();
		if (selectionKeySet.isEmpty()) {
			System.out.println("The selector contains no key : this should not happen!");
			return;
		}
		System.out.println("The selector contains:");
		for (SelectionKey key : selectionKeySet){
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				System.out.println("\tKey for ServerSocketChannel : "+ interestOpsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				System.out.println("\tKey for Client "+ remoteAddressToString(sc) +" : "+ interestOpsToString(key));
			}
		}
	}

	private String remoteAddressToString(SocketChannel sc) {
		try {
			return sc.getRemoteAddress().toString();
		} catch (IOException e){
			return "???";
		}
	}

	public void printSelectedKey(SelectionKey key) {
		SelectableChannel channel = key.channel();
		if (channel instanceof ServerSocketChannel) {
			System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
		} else {
			SocketChannel sc = (SocketChannel) channel;
			System.out.println("\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
		}
	}

	private String possibleActionsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		ArrayList<String> list = new ArrayList<>();
		if (key.isAcceptable()) list.add("ACCEPT");
		if (key.isReadable()) list.add("READ");
		if (key.isWritable()) list.add("WRITE");
		return String.join(" and ",list);
	}
}
