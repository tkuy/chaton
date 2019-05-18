package fr.upem.net.tcp.nonblocking;

import fr.upem.net.tcp.nonblocking.frame.FrameBroadcast;
import fr.upem.net.tcp.nonblocking.frame.FrameLogin;

public interface FrameVisitor {
    public void visitLoginFrame(FrameLogin frame);
    public void visitBroadcastFrame(FrameBroadcast frame);
}
