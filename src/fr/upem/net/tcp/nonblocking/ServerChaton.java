package fr.upem.net.tcp.nonblocking;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.upem.net.tcp.nonblocking.frame.*;

public class ServerChaton {

	static class Pair {
		Optional<Context> ctx1;
		Optional<Context> ctx2;

		public Pair() {
			this.ctx1 = Optional.empty();
			this.ctx2 = Optional.empty();
		}

		public Optional<Context> getCtx1() {
			return ctx1;
		}

		public void setCtx1(Optional<Context> ctx1) {
			this.ctx1 = ctx1;
		}

		public Optional<Context> getCtx2() {
			return ctx2;
		}

		public void setCtx2(Optional<Context> ctx2) {
			this.ctx2 = ctx2;
		}
	}
	static class PairCtxPrivate {
		Optional<PrivateContext> ctx1;
		Optional<PrivateContext> ctx2;

		public PairCtxPrivate() {
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



    static private int BUFFER_SIZE = 1_024;
    static private Logger logger = Logger.getLogger(ServerChaton.class.getName());
	final Map<String, Context> pseudos = new HashMap<>();
	final Map<String, ArrayList<String>> requests = new HashMap<>();
    final ArrayList<Long> ids = new ArrayList<>();
    private final ServerSocketChannel serverSocketChannel;
    final Selector selector;
    final Map<Long, Pair> connectionsId = new HashMap<>();
    final Map<SocketChannel, PrivateContext> connections = new HashMap<>();

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
				//((Context) key.attachment()).doWrite();
				
				var tmp = key.attachment();
				System.out.println("TREAT KEY : "+ tmp);
				if(tmp instanceof Context) {
					((Context) tmp).doWrite();
				}else if(tmp instanceof PrivateContext){
					((PrivateContext) tmp).doWrite();
				} else {
					throw new IllegalArgumentException("Not a context and private context");
				}
			}
			if (key.isValid() && key.isReadable()) {
				System.out.println("Key of treatKey "  +key);
				var tmp = key.attachment();
				if(tmp instanceof Context) {
					((Context) tmp).doRead();
				}else if(tmp instanceof PrivateContext){
					((PrivateContext) tmp).doRead();
				} else {
					throw new IllegalArgumentException("Not a context and private context");
				}
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
	void broadcast(Frame frame) {
		for (SelectionKey key : selector.keys()) {
			Object attachment = key.attachment();
			if(attachment!=null && attachment instanceof Context) {
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
