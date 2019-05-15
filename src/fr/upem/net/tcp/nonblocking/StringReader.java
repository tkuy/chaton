package fr.upem.net.tcp.nonblocking;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class StringReader implements Reader {
    private enum State {DONE, WAITING_SIZE, WAITING_DATA, ERROR}

    ;

    private final ByteBuffer bb;
    private State state = State.WAITING_SIZE;
    private int size;
    private String decoded;

    public StringReader(ByteBuffer bb) {
        this.bb = bb;
    }

    @Override
    public ProcessStatus process() {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        IntReader intReader = new IntReader(bb);
        switch (state) {
            case WAITING_SIZE:
                if(intReader.process() == ProcessStatus.DONE) {
                    size = (int) intReader.get();
                    state = State.WAITING_DATA;
                } else {
                    return ProcessStatus.REFILL;
                }
            case WAITING_DATA:
                bb.flip();
                try {
                    if (bb.remaining() >= size) {
                        int oldLimit = bb.limit();
                        bb.limit(bb.position()+size);
                        state = State.DONE;
                        decoded = StandardCharsets.UTF_8.decode(bb).toString();
                        bb.limit(oldLimit);
                        System.out.println("DONE String Reader " + decoded);
                        return ProcessStatus.DONE;
                    } else {
                        return ProcessStatus.REFILL;
                    }
                } finally {
                    bb.compact();
                }
        }
        return ProcessStatus.ERROR;
    }

    @Override
    public Object get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return decoded;
    }

    @Override
    public void reset() {
        state = State.WAITING_SIZE;
        decoded=null;
    }

    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        ByteBuffer s = StandardCharsets.UTF_8.encode("Bonjour");
        StringReader stringReader = new StringReader(buffer);
        buffer.putInt(s.capacity());
        if(stringReader.process()==ProcessStatus.REFILL) {
            System.out.println("Normal behavior");
        }
        buffer.put(s).putInt(s.capacity()).put(s);
        if(stringReader.process()==ProcessStatus.DONE) {
            String s1 = (String) stringReader.get();
            System.out.println("=="+s1);
        }
    }
}
