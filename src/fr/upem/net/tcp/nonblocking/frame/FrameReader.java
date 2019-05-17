package fr.upem.net.tcp.nonblocking.frame;

import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.IntReader;
import fr.upem.net.tcp.nonblocking.Reader;
import fr.upem.net.tcp.nonblocking.frame.reader.FrameBroadcastReader;
import fr.upem.net.tcp.nonblocking.frame.reader.FrameLoginReader;

public class FrameReader implements Reader {

	private enum FrameType{
		LOGIN, BROADCAST, NONE
	}
	private enum State{
		DONE,WAITING_OP,WAITING_FRAME,ERROR
	}
	
	private State state = State.WAITING_OP;
	
	private final IntReader opCodeReader;
	private FrameType frameType;
	private Reader currentReader;
	
	private final FrameLoginReader frameLoginReader;
	private final FrameBroadcastReader frameBroadcastReader;
	
	 private final ByteBuffer bbin;
	 
	 
	private Frame result;
	public FrameReader(ByteBuffer bbin) {
		super();
		this.bbin = bbin;
		this.opCodeReader = new IntReader(bbin);
		this.frameLoginReader = new FrameLoginReader(bbin);
		this.frameBroadcastReader = new FrameBroadcastReader(bbin);
		
	}

	@Override
	public ProcessStatus process() {
		switch(state) {
		
		case WAITING_OP:
			if(opCodeReader.process() == ProcessStatus.DONE) {
				int opCode =(int) opCodeReader.get();
				state = State.WAITING_FRAME;
				if(updateFrameType(opCode)) {
					reset();
					return ProcessStatus.ERROR;
				}
				opCodeReader.reset();
			
			}else {
				return ProcessStatus.REFILL;
			}
			break;
		case WAITING_FRAME:
			if(currentReader == null) {
				return ProcessStatus.ERROR;
			}
			if(currentReader.process() == ProcessStatus.DONE) {
				result = (Frame) frameLoginReader.get();
				state=State.DONE;
				currentReader.reset();
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
