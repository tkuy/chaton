package fr.upem.net.tcp.nonblocking.frame;

public interface FramePrivateVisitor {
    void visit(FrameLoginPrivateConnection frame);
}
