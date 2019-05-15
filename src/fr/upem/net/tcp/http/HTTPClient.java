package fr.upem.net.tcp.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class HTTPClient {
    public static void main(String[] args) {
        int port = Integer.parseInt(args[1]);
        String address = args[0];
        try(SocketChannel sc = SocketChannel.open()) {
            sc.connect(new InetSocketAddress(address, port));
            String request = "GET / HTTP/1.1\r\n"
                    + "Host:" + address + "\r\n"
                    + "\r\n";
            sc.write(StandardCharsets.US_ASCII.encode(request));
            HTTPReader httpReader = new HTTPReader(sc, ByteBuffer.allocate(50));
            HTTPHeader httpHeader = httpReader.readHeader();
            ByteBuffer byteBuffer = httpReader.readBytes(httpHeader.getContentLength());
            String contentType = httpHeader.getContentType();
            System.out.println(contentType);
            if(contentType.contains("html")) {
                System.out.println(httpHeader.getCharset().decode(byteBuffer.flip()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
