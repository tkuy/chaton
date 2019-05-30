package fr.upem.net.tcp.nonblocking.frame;

import fr.upem.net.tcp.nonblocking.Reader;
import fr.upem.net.tcp.nonblocking.frame.reader.*;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.logging.Logger;

public class FrameLogReader implements Reader {

	private enum FrameType{
		LOGIN, LOGIN_PRIVATE, NONE
	}
	private enum State{
		DONE,WAITING_OP,WAITING_FRAME,ERROR
	}

	private State state = State.WAITING_OP;

	private final IntReader opCodeReader;
	private FrameType frameType;
	private Reader currentReader;
	private Optional<Integer> currentOpCode = Optional.empty();

	private final FrameLoginReader frameLoginReader;
	private FrameLoginPrivateConnectionReader framePrivateLoginConnectionReader;

	 private final ByteBuffer bbin;
	 private final static Logger logger = Logger.getLogger(FrameLogReader.class.toString());

	private Frame result;
	public FrameLogReader(ByteBuffer bbin) {
		super();
		this.bbin = bbin;
		this.opCodeReader = new IntReader(bbin);
		this.frameLoginReader = new FrameLoginReader(bbin);
		this.framePrivateLoginConnectionReader = new FrameLoginPrivateConnectionReader(bbin);
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
				state= State.DONE;
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
			case 9:
				currentReader = this.framePrivateLoginConnectionReader;
				frameType = FrameType.LOGIN_PRIVATE;
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
