package fr.upem.net.tcp.nonblocking;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerEchoWithConsole {

	static private class Context {

		final private SelectionKey key;
		final private SocketChannel sc;
		final private ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
		private boolean closed = false;
		private boolean activeSinceLastTimeoutCheck;
		private Context(SelectionKey key){
			this.key = key;
			this.sc = (SocketChannel) key.channel();
			this.activeSinceLastTimeoutCheck = true;
		}

		/**
		 * Update the interestOps of the key looking
		 * only at values of the boolean closed and
		 * the ByteBuffer buff.
		 *
		 * The convention is that buff is in write-mode.
		 */
		private void updateInterestOps() {
			int interestOps = 0;
			if(!closed && bb.hasRemaining()) {
				interestOps |= SelectionKey.OP_READ;
			}
			if(bb.position()!=0) {
				interestOps |= SelectionKey.OP_WRITE;
			}
			if(interestOps == 0) {
				silentlyClose();
			} else {
				key.interestOps(interestOps);
			}
		}

		/**
		 * Performs the read action on sc
		 *
		 * The convention is that buff is in write-mode before calling doRead
		 * and is in write-mode after calling doRead
		 *
		 * @throws IOException
		 */
		private void doRead() throws IOException {
			SocketChannel sc = (SocketChannel) key.channel();
			if(sc.read(bb) == -1) {
				closed = true;
			}
			activeSinceLastTimeoutCheck = true;
			updateInterestOps();
		}

		/**
		 * Performs the write action on sc
		 *
		 * The convention is that buff is in write-mode before calling doWrite
		 * and is in write-mode after calling doWrite
		 *
		 * @throws IOException
		 */
		private void doWrite() throws IOException {
			SocketChannel sc = (SocketChannel) key.channel();
			if(sc == null) {
				return;
			}
			sc.write(bb.flip());
			bb.compact();
			activeSinceLastTimeoutCheck = true;
			updateInterestOps();
		}

		private void silentlyClose() {
			try {
				sc.close();
			} catch (IOException e) {
				// ignore exception
			}
		}
	}

	static private int BUFFER_SIZE = 1_024;
	static private Logger logger = Logger.getLogger(ServerEchoWithConsole.class.getName());
	static private enum  Command {INFO, SHUTDOWN, SHUTDOWNNOW}
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final BlockingQueue<Command> consoleQueue;
	private static final int TIMEOUT = 500;
	public ServerEchoWithConsole(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		consoleQueue = new ArrayBlockingQueue(1);
	}

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		Thread consoleThread = new Thread(() -> {
			while (!Thread.interrupted()) {
				try(Scanner sc = new Scanner(System.in)){
					while (sc.hasNextLine()) {
						switch (sc.nextLine().trim().toUpperCase()) {
							case "INFO" :
								consoleQueue.offer(Command.INFO);
								selector.wakeup();
								break;
							case "SHUTDOWN":
								consoleQueue.offer(Command.SHUTDOWN);
								selector.wakeup();
								break;

							case "SHUTDOWNNOW":
								consoleQueue.offer(Command.SHUTDOWNNOW);
								selector.wakeup();
								break;
							default:
								System.out.println("The command line is not valid, your fingers are too big");
						}
					}
				}
			}
		});
		consoleThread.start();
		boolean shutdown = false;
		while(!Thread.interrupted()) {
			printKeys(); // for debug
			System.out.println("Starting select");
			try {
				if(!shutdown) {
					selector.select(this::treatKey);
					Command command = consoleQueue.poll();
					if (command != null) {
						switch (command.toString()) {
							case "INFO":
								long count = selector.keys().stream().filter(key -> !key.isAcceptable() && key.isValid()).count();
								System.out.println("Nombre de clients connectés : " + count);
								break;
							case "SHUTDOWN":
								selector.keys().stream().filter(k -> k.isAcceptable() && k.isValid()).forEach(this::silentlyClose);
								break;
							case "SHUTDOWNNOW":
								serverSocketChannel.close();
								selector.keys().forEach(key -> {
									Context ctx = (Context) key.attachment();
									if (ctx != null)
										ctx.silentlyClose();
								});
								shutdown = true;
								break;
							default:
								System.out.println("The command line is not valid, your fingers are too big");
						}
					}
				}
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
		newKey.attach(new Context(newKey));
	}

	private void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length!=1){
			usage();
			return;
		}
		new ServerEchoWithConsole(Integer.parseInt(args[0])).launch();
	}

	private static void usage(){
		System.out.println("Usage : ServerEcho port");
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
