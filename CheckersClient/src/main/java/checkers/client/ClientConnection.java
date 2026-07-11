package checkers.client;

import checkers.common.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientConnection extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final Consumer<Message> callback;
    private final String host;
    private final int port;

    public ClientConnection(String host, int port, Consumer<Message> callback) {
        this.host = host;
        this.port = port;
        this.callback = callback;
        setName("CheckersClientConnection");
    }

    public void run() {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            socket.setTcpNoDelay(true);
        } catch (Exception e) {
            Message m = new Message(Message.ERROR);
            m.text = "Could not connect to server on port " + port + ".";
            callback.accept(m);
            return;
        }

        while (!isInterrupted()) {
            try {
                Object obj = in.readObject();
                if (obj instanceof Message) {
                    callback.accept((Message) obj);
                }
            } catch (Exception e) {
                Message m = new Message(Message.ERROR);
                m.text = "Disconnected from server.";
                callback.accept(m);
                break;
            }
        }
    }

    public synchronized void send(Message msg) {
        try {
            if (out == null) {
                Message m = new Message(Message.ERROR);
                m.text = "Not connected to server yet.";
                callback.accept(m);
                return;
            }
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            Message m = new Message(Message.ERROR);
            m.text = "Failed to send message.";
            callback.accept(m);
        }
    }

    public void closeConnection() {
        interrupt();
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }
}
