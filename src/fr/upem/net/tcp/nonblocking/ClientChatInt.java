package fr.upem.net.tcp.nonblocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

public class ClientChatInt {

    static private int BUFFER_SIZE = 1_024;
    static private Logger logger = Logger.getLogger(ClientChatInt.class.getName());

    private final SocketChannel sc;
    private final Selector selector;
    private final InetSocketAddress serverAddress;
	private SelectionKey uniqueKey;
	private boolean closed;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	private IntReader intReader = new IntReader(bbin);
	final private Queue<Integer> blockingQueue = new ArrayBlockingQueue(10);
	final private LinkedList<Integer> queue = new LinkedList<>();
	public ClientChatInt(int port, String address) throws IOException {
        sc = SocketChannel.open();
		this.serverAddress = new InetSocketAddress(address, port);
        sc.configureBlocking(false);
        selector = Selector.open();
    }

    public void launch() throws IOException {
		sc.configureBlocking(false);
		sc.connect(serverAddress);
		uniqueKey=sc.register(selector, SelectionKey.OP_CONNECT);
		Set selectedKeys = selector.selectedKeys();
		while (!Thread.interrupted()) {
			selector.select();
			processSelectedKeys();
			selectedKeys.clear();
			Integer value;
			while((value=blockingQueue.poll()) != null) {
				queueMessage(value);
			}
		}
    }

	private void doConnect() throws IOException {
		System.out.println("Do connect");
		if (!sc.finishConnect()){
			return;
		}
		updateInterestsOps();
		printKeys();
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
		logger.info("Do read");
		if(sc.read(bbin) == -1) {
			closed = true;
		}
		processIn();
		updateInterestsOps();
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

	private void updateInterestsOps() {
		int interestOps = 0;
		if(!closed && bbin.hasRemaining()) {
			interestOps |= SelectionKey.OP_READ;
		}
		if(bbout.position()!=0) {
			interestOps |= SelectionKey.OP_WRITE;
		}
		if(interestOps == 0) {
			silentlyClose(uniqueKey);
		} else {
			uniqueKey.interestOps(interestOps);
		}
	}

	private void processSelectedKeys() throws IOException {
		for (SelectionKey key : selector.selectedKeys()) {
			if (key.isValid() && key.isConnectable()) {
				doConnect();
			}
			if (key.isValid() && key.isWritable()) {
				doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				doRead();
			}
		}
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
			Reader.ProcessStatus status = intReader.process();
			switch (status){
				case DONE:
					Integer value = (Integer) intReader.get();
					System.out.println("Value : " + value);
					intReader.reset();
					break;
				case REFILL:
					return;
				case ERROR:
					silentlyClose(uniqueKey);
					return;
			}
		}
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
		logger.info("Do write");
		sc.write(bbout.flip());
		bbout.compact();
		processOut();
		updateInterestsOps();
	}
	/**
	 * Try to fill bbout from the message queue
	 *
	 */
	private void processOut() {
		while (bbout.remaining() >= Integer.BYTES && !queue.isEmpty()) {
			bbout.putInt(queue.poll());
		}
	}

	/**
	 * Add a message to the message queue, tries to fill bbOut and updateInterestOps
	 *
	 * @param msg
	 */
	private void queueMessage(Integer msg) {
		queue.add(msg);
		processOut();
		updateInterestsOps();
	}
    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    public void console() {
		System.out.println("Start write process");
		new Thread(() -> {
			String line;
			try(Scanner sc = new Scanner(System.in)){
				while(sc.hasNextLine()) {
					line = sc.nextLine();
					blockingQueue.offer(Integer.parseInt(line));
					selector.wakeup();
				}
			}
		}).start();
	}

    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length!=2){
            usage();
            return;
        }
		ClientChatInt clientChatInt = new ClientChatInt(Integer.parseInt(args[0]), args[1]);
		clientChatInt.console();
        clientChatInt.launch();
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

	private String remoteAddressToString(SocketChannel sc) {
		try {
			return sc.getRemoteAddress().toString();
		} catch (IOException e){
			return "???";
		}
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
