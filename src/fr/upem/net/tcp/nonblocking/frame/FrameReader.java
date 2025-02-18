package fr.upem.net.tcp.nonblocking.frame;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.logging.Logger;

import fr.upem.net.tcp.nonblocking.frame.reader.IntReader;
import fr.upem.net.tcp.nonblocking.Reader;
import fr.upem.net.tcp.nonblocking.frame.reader.*;

public class FrameReader implements Reader {

	private enum FrameType{
		BROADCAST, PRIVATE_MESSAGE, PRIVATE_CONNECTION, PRIVATE_CONNECTION_RESPONSE, LOGIN, LOGIN_PRIVATE_CONNECTION, NONE
	}
	private enum State{
		DONE,WAITING_OP,WAITING_FRAME,ERROR
	}
	
	private State state = State.WAITING_OP;
	
	private final IntReader opCodeReader;
	private FrameType frameType;
	private Reader currentReader;
	private Optional<Integer> currentOpCode = Optional.empty();

	private final FrameBroadcastReader frameBroadcastReader;
	private final FramePrivateMessageReader privateMessageReader;
	private final FramePrivateConnectionReader privateConnectionReader;
	private final FrameOKPrivateConnectionResponseReader privateOKConnectionResponseReader;
	private final FrameKOPrivateConnectionResponseReader privateKOConnectionResponseReader;
	private final FrameLoginReader frameLoginReader;
	private FrameLoginPrivateConnectionReader privateLoginConnectionReader;

	 private final ByteBuffer bbin;
	 private final static Logger logger = Logger.getLogger(FrameReader.class.toString());
	 
	private Frame result;
	public FrameReader(ByteBuffer bbin) {
		super();
		this.bbin = bbin;
		this.opCodeReader = new IntReader(bbin);
		this.frameBroadcastReader = new FrameBroadcastReader(bbin);
		this.privateMessageReader = new FramePrivateMessageReader(bbin);
		this.privateConnectionReader = new FramePrivateConnectionReader(bbin);
		this.privateOKConnectionResponseReader = new FrameOKPrivateConnectionResponseReader(bbin);
		this.privateKOConnectionResponseReader = new FrameKOPrivateConnectionResponseReader(bbin);
		this.frameLoginReader = new FrameLoginReader(bbin);
		this.privateLoginConnectionReader = new FrameLoginPrivateConnectionReader(bbin);
	}

	@Override
	public ProcessStatus process() {
		switch(state) {
		
		case WAITING_OP:
			if(opCodeReader.process() == ProcessStatus.DONE) {
				int opCode =(int) opCodeReader.get();
				state = State.WAITING_FRAME;
				if(!updateFrameType(opCode)) {
					reset();
					return ProcessStatus.ERROR;
				}
				opCodeReader.reset();
			
			}else {
				return ProcessStatus.REFILL;
			}
			
		case WAITING_FRAME:
			if(currentReader == null) {
				return ProcessStatus.ERROR;
			}
			if(currentReader.process() == ProcessStatus.DONE) {
				result = (Frame) currentReader.get();
				logger.info("Read frame : "+ frameType);
				state=State.DONE;
				currentReader.reset();
				return ProcessStatus.DONE;
			} else {
				return ProcessStatus.REFILL;
			}
		}
		return ProcessStatus.ERROR;
			
	}

	private boolean updateFrameType(int opCode) {
		switch(opCode) {
			case 0:
				currentReader = this.frameLoginReader;
				frameType = FrameType.LOGIN;
				break;
			case 3:
				currentReader = this.frameBroadcastReader;
				frameType = FrameType.BROADCAST;
				break;
			case 4:
				currentReader = this.privateMessageReader;
				frameType = FrameType.PRIVATE_MESSAGE;
				break;
			case 5:
				currentReader = this.privateConnectionReader;
				frameType = FrameType.PRIVATE_CONNECTION;
				break;
			case 6:
				currentReader = this.privateOKConnectionResponseReader;
				frameType = FrameType.PRIVATE_CONNECTION_RESPONSE;
				break;
			case 7:
				currentReader = this.privateKOConnectionResponseReader;
				frameType = FrameType.PRIVATE_CONNECTION_RESPONSE;
				break;
			case 9:
				currentReader = this.privateLoginConnectionReader;
				frameType = FrameType.LOGIN_PRIVATE_CONNECTION;
				break;
			default:
				frameType = FrameType.NONE;
				return false;
		}
		return true;
	}
		
	

	@Override
	public Object get() {
		if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return result;
	}

	@Override
	public void reset() {
		frameType = FrameType.NONE;
		state = State.WAITING_OP;
		currentReader = null;
	}
	
		
	
}
