package checkers.server;

import checkers.common.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final CheckersServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    public volatile String username;

    public ClientHandler(Socket socket, CheckersServer server) {
        this.socket = socket;
        this.server = server;
        setName("ClientHandler-" + socket.getPort());
    }

    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            while (!isInterrupted()) {
                Object obj = in.readObject();
                if (obj instanceof Message) {
                    server.handleMessage(this, (Message) obj);
                }
            }
        } catch (Exception ignored) {
        } finally {
            closeHandler();
        }
    }

    public synchronized void send(Message m) {
        try {
            if (out != null) {
                out.writeObject(m);
                out.flush();
            }
        } catch (IOException ignored) {
            closeHandler();
        }
    }

    public void closeHandler() {
        interrupt();
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        server.onDisconnect(this);
    }
}
