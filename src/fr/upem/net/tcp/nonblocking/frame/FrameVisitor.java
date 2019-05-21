package fr.upem.net.tcp.nonblocking.frame;

import fr.upem.net.tcp.nonblocking.frame.*;

public interface FrameVisitor {
    public void visitLoginFrame(FrameLogin frame);
    public void visitResponseLoginFrame(FrameLoginResponse frame);

    public void visitBroadcastFrame(FrameBroadcast frame);

    void visitPrivateMessage(FramePrivateMessage frame);
    void visitPrivateConnection(FramePrivateConnection frame);

    void visitPrivateConnectionResponse(FramePrivateConnectionResponse frame);
}
