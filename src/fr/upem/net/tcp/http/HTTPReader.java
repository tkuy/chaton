package fr.upem.net.tcp.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HTTPReader {

    private final Charset ASCII_CHARSET = Charset.forName("ASCII");
    private final SocketChannel sc;
    private final ByteBuffer buff;
    private static final byte CR = (byte) '\r';
    private static final byte LF = (byte) '\n';
    private boolean crBefore;

    public HTTPReader(SocketChannel sc, ByteBuffer buff) {
        this.sc = sc;
        this.buff = buff;
    }

    /**
     * @return The ASCII string terminated by CRLF
     * <p>
     * The method assume that buff is in write mode and leave it in write-mode
     * The method never reads from the socket as long as the buffer is not empty
     * @throws IOException HTTPException if the connection is closed before a line could be read
     */
    public String readLineCRLF() throws IOException {
        crBefore = false;
        StringBuilder stb = new StringBuilder();
        buff.flip();
        for(;;) {
            if (!buff.hasRemaining()) {
                if(sc.read(buff.clear()) == -1) {
                    throw new HTTPException();
                }
                buff.flip();
            }
            byte b = buff.get();
            stb.append((char) b);
            if (b == LF && crBefore) {
                break;
            }
            crBefore = (b==CR);
        }
        stb.setLength(stb.length() - 2);
        buff.compact();
        return stb.toString();
    }

    /**
     * @return The HTTPHeader object corresponding to the header read
     * @throws IOException HTTPException if the connection is closed before a header could be read
     *                     if the header is ill-formed
     */
    public HTTPHeader readHeader() throws IOException {
        Map<String, String> map = new HashMap();
        String header;
        String first=null;
        while(!(header = readLineCRLF()).isEmpty()) {
            if(first == null) {
                first = header;
            }
            String[] tab = header.split(":");
            if(tab.length == 2) {
                map.merge(tab[0], tab[1], (a, b) -> a+";"+b);
            }
        }
        return HTTPHeader.create(first, map);
    }

    /**
     * @param size
     * @return a ByteBuffer in write-mode containing size bytes read on the socket
     * @throws IOException HTTPException is the connection is closed before all bytes could be read
     */
    public ByteBuffer readBytes(int size) throws IOException {
        ByteBuffer buff = ByteBuffer.allocate(size);
        buff.put(this.buff.flip());
        if(!readFully(sc, buff)) {
            throw new HTTPException();
        }
        return buff;
    }

    /**
     * @return a ByteBuffer in write-mode containing a content read in chunks mode
     * @throws IOException HTTPException if the connection is closed before the end of the chunks
     *                     if chunks are ill-formed
     */

    public ByteBuffer readChunks() throws IOException {
        int size;
        ByteBuffer bb = ByteBuffer.allocate(1024);
        for(;;) {
            size = Integer.parseInt(readLineCRLF(), 16);
            if(size == 0) {
                break;
            }
            if(bb.remaining()<size) {
                ByteBuffer tmp = ByteBuffer.allocate(bb.capacity() + size).put(bb.flip());
                tmp.put(readBytes(size).flip());
                readLineCRLF();
                bb=tmp;
            }
        }
        return bb;
    }

    /**
     * Fill the workspace of the Bytebuffer with bytes read from sc.
     *
     * @param sc
     * @param bb
     * @return false if read returned -1 at some point and true otherwise
     * @throws IOException
     */
    static boolean readFully(SocketChannel sc, ByteBuffer bb) throws IOException {
        while (bb.hasRemaining()) {
            if (sc.read(bb) == -1) {
                return false;
            }
        }
        return true;
    }


    public static void main(String[] args) throws IOException {
        Charset charsetASCII = Charset.forName("ASCII");
        String request = "GET / HTTP/1.1\r\n"
                + "Host: www.w3.org\r\n"
                + "\r\n";
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("www.w3.org", 80));
        sc.write(charsetASCII.encode(request));
        ByteBuffer bb = ByteBuffer.allocate(50);
        HTTPReader reader = new HTTPReader(sc, bb);
        System.out.println(reader.readLineCRLF());
        System.out.println(reader.readLineCRLF());
        System.out.println(reader.readLineCRLF());
        sc.close();

        bb = ByteBuffer.allocate(50);
        sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("www.w3.org", 80));
        reader = new HTTPReader(sc, bb);
        sc.write(charsetASCII.encode(request));
        System.out.println(reader.readHeader());
        sc.close();

        bb = ByteBuffer.allocate(50);
        sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("www.w3.org", 80));
        reader = new HTTPReader(sc, bb);
        sc.write(charsetASCII.encode(request));
        HTTPHeader header = reader.readHeader();
        System.out.println(header);
        ByteBuffer content = reader.readBytes(header.getContentLength());
        content.flip();
        System.out.println(header.getCharset().decode(content));
        sc.close();

        bb = ByteBuffer.allocate(50);
        System.out.println("\n\n\n\n");
        request = "GET / HTTP/1.1\r\n"
                + "Host: www.u-pem.fr\r\n"
                + "\r\n";
        sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("www.u-pem.fr", 80));
        reader = new HTTPReader(sc, bb);
        sc.write(charsetASCII.encode(request));
        header = reader.readHeader();
        System.out.println(header);
        content = reader.readChunks();
        content.flip();
        System.out.println(header.getCharset().decode(content));
        sc.close();
    }
}
