package fr.upem.net.tcp.nonblocking;

import fr.upem.net.tcp.nonblocking.frame.FrameBroadcast;
import fr.upem.net.tcp.nonblocking.frame.FrameLogin;
import fr.upem.net.tcp.nonblocking.frame.FramePrivateMessage;

public interface FrameVisitor {
    public void visitLoginFrame(FrameLogin frame);
    public void visitResponseLoginFrame(FrameLoginResponse frame);

    public void visitBroadcastFrame(FrameBroadcast frame);

    void visitPrivateMessage(FramePrivateMessage framePrivateMessage);
}
