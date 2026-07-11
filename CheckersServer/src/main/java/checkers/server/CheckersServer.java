package checkers.server;

import checkers.common.CheckersGame;
import checkers.common.Message;
import checkers.common.PieceColor;
import checkers.common.UserStats;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class CheckersServer extends Thread {
    private final int port;
    private final Consumer<String> log;
    private final AccountStore store;
    private final Map<String, ClientHandler> onlineUsers = new ConcurrentHashMap<String, ClientHandler>();
    private final Queue<ClientHandler> waitingQueue = new ConcurrentLinkedQueue<ClientHandler>();
    private final Map<ClientHandler, GameSession> sessions = new ConcurrentHashMap<ClientHandler, GameSession>();
    private ServerSocket serverSocket;

    public CheckersServer(int port, Consumer<String> log) {
        this.port = port;
        this.log = log;
        this.store = new AccountStore(new File("server-data/accounts.ser"));
        setName("CheckersServer");
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            log.accept("Server listening on port " + port);
            while (!isInterrupted()) {
                Socket socket = serverSocket.accept();
                log.accept("Client connected: " + socket.getInetAddress());
                new ClientHandler(socket, this).start();
            }
        } catch (IOException e) {
            log.accept("Server stopped: " + e.getMessage());
        }
    }

    public void shutdown() {
        interrupt();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    public void onDisconnect(ClientHandler client) {
        removeFromQueue(client);
        if (client.username != null) {
            onlineUsers.remove(client.username);
            log.accept(client.username + " disconnected");
        }
        GameSession session = sessions.remove(client);
        if (session != null) {
            ClientHandler other = session.other(client);
            sessions.remove(other);
            if (other != null) {
                Message m = new Message(Message.OPPONENT_LEFT);
                m.text = "Your opponent left the game.";
                other.send(m);
            }
        }
    }

    private void removeFromQueue(ClientHandler client) {
        waitingQueue.remove(client);
    }

    public void handleMessage(ClientHandler client, Message m) {
        if (Message.CREATE_ACCOUNT.equals(m.type)) {
            handleCreateAccount(client, m);
        } else if (Message.LOGIN.equals(m.type)) {
            handleLogin(client, m);
        } else if (Message.PROFILE.equals(m.type)) {
            handleProfile(client);
        } else if (Message.QUEUE.equals(m.type)) {
            handleQueue(client);
        } else if (Message.MOVE.equals(m.type)) {
            handleMove(client, m);
        } else if (Message.CHAT.equals(m.type)) {
            handleChat(client, m);
        } else if (Message.REMATCH.equals(m.type)) {
            handleRematch(client);
        } else if (Message.LEAVE.equals(m.type) || Message.LOGOUT.equals(m.type)) {
            client.closeHandler();
        }
    }

    private void handleCreateAccount(ClientHandler client, Message m) {
        boolean ok = store.createAccount(m.username, m.password);
        Message reply = new Message(ok ? Message.AUTH_OK : Message.AUTH_FAIL);
        if (ok) {
            client.username = m.username;
            onlineUsers.put(client.username, client);
            reply.text = "Account created successfully.";
            reply.stats = store.getStats(m.username);
            log.accept("Account created: " + m.username);
        } else {
            reply.text = "Could not create account. Username may already exist or fields are empty.";
        }
        client.send(reply);
    }

    private void handleLogin(ClientHandler client, Message m) {
        if (!store.validateLogin(m.username, m.password)) {
            Message fail = new Message(Message.AUTH_FAIL);
            fail.text = "Invalid username or password.";
            client.send(fail);
            return;
        }
        if (onlineUsers.containsKey(m.username)) {
            Message fail = new Message(Message.AUTH_FAIL);
            fail.text = "That username is already online.";
            client.send(fail);
            return;
        }
        client.username = m.username;
        onlineUsers.put(client.username, client);
        Message ok = new Message(Message.AUTH_OK);
        ok.text = "Login successful.";
        ok.stats = store.getStats(m.username);
        client.send(ok);
        log.accept("Login: " + m.username);
    }

    private void handleProfile(ClientHandler client) {
        if (client.username == null) return;
        Message profile = new Message(Message.PROFILE_DATA);
        profile.stats = store.getStats(client.username);
        client.send(profile);
    }

    private void handleQueue(ClientHandler client) {
        if (client.username == null) {
            Message err = new Message(Message.ERROR);
            err.text = "Please log in first.";
            client.send(err);
            return;
        }
        if (sessions.containsKey(client)) return;
        if (!waitingQueue.contains(client)) {
            waitingQueue.add(client);
        }
        Message status = new Message(Message.QUEUE_STATUS);
        status.text = "Waiting for an opponent...";
        client.send(status);
        matchPlayers();
    }

    private void matchPlayers() {
        while (waitingQueue.size() >= 2) {
            ClientHandler a = waitingQueue.poll();
            ClientHandler b = waitingQueue.poll();
            if (a == null || b == null || a == b) break;
            GameSession session = new GameSession(a, b);
            sessions.put(a, session);
            sessions.put(b, session);
            log.accept("Match found: " + a.username + " vs " + b.username);
            sendMatchFound(a, b, session, PieceColor.RED);
            sendMatchFound(b, a, session, PieceColor.BLACK);
        }
    }

    private void sendMatchFound(ClientHandler player, ClientHandler opponent, GameSession session, PieceColor color) {
        Message m = new Message(Message.MATCH_FOUND);
        m.color = color.name();
        m.opponent = opponent.username;
        m.game = session.game.copy();
        m.yourTurn = session.game.turn == color;
        m.text = "Game started.";
        player.send(m);
    }

    private void handleMove(ClientHandler client, Message m) {
        GameSession session = sessions.get(client);
        if (session == null) return;
        PieceColor color = session.colorOf(client);
        String result = session.game.applyMove(color, m.fr, m.fc, m.tr, m.tc);
        if (result.startsWith("ERROR:")) {
            Message err = new Message(Message.ERROR);
            err.text = result;
            client.send(err);
            return;
        }
        sendGameState(session, result);
        if (session.game.winner != null || session.game.draw) {
            finishSession(session);
        }
    }

    private void sendGameState(GameSession session, String text) {
        Message a = new Message(Message.GAME_STATE);
        a.game = session.game.copy();
        a.yourTurn = session.game.turn == session.colorOf(session.redPlayer);
        a.text = text;
        session.redPlayer.send(a);

        Message b = new Message(Message.GAME_STATE);
        b.game = session.game.copy();
        b.yourTurn = session.game.turn == session.colorOf(session.blackPlayer);
        b.text = text;
        session.blackPlayer.send(b);
    }

    private void finishSession(GameSession session) {
        if (session.game.draw) {
            store.recordDraw(session.redPlayer.username, session.blackPlayer.username);
            sendGameOver(session.redPlayer, session, "DRAW", "Game ended in a draw.");
            sendGameOver(session.blackPlayer, session, "DRAW", "Game ended in a draw.");
        } else if (session.game.winner == PieceColor.RED) {
            store.recordWin(session.redPlayer.username, session.blackPlayer.username);
            sendGameOver(session.redPlayer, session, "WIN", "You won the game.");
            sendGameOver(session.blackPlayer, session, "LOSS", "You lost the game.");
        } else {
            store.recordWin(session.blackPlayer.username, session.redPlayer.username);
            sendGameOver(session.redPlayer, session, "LOSS", "You lost the game.");
            sendGameOver(session.blackPlayer, session, "WIN", "You won the game.");
        }
        session.finished = true;
        session.redRematch = false;
        session.blackRematch = false;
    }

    private void sendGameOver(ClientHandler player, GameSession session, String result, String text) {
        Message m = new Message(Message.GAME_OVER);
        m.result = result;
        m.text = text;
        m.game = session.game.copy();
        player.send(m);
    }

    private void handleChat(ClientHandler client, Message m) {
        GameSession session = sessions.get(client);
        if (session == null) return;
        Message chat = new Message(Message.CHAT);
        chat.sender = client.username;
        chat.text = m.text;
        session.redPlayer.send(chat);
        if (session.blackPlayer != session.redPlayer) {
            session.blackPlayer.send(chat);
        }
    }

    private void handleRematch(ClientHandler client) {
        GameSession session = sessions.get(client);
        if (session == null || !session.finished) return;
        if (client == session.redPlayer) session.redRematch = true;
        if (client == session.blackPlayer) session.blackRematch = true;
        if (session.redRematch && session.blackRematch) {
            session.game = new CheckersGame();
            session.finished = false;
            sendMatchFound(session.redPlayer, session.blackPlayer, session, PieceColor.RED);
            sendMatchFound(session.blackPlayer, session.redPlayer, session, PieceColor.BLACK);
        } else {
            Message info = new Message(Message.INFO);
            info.text = "Rematch requested. Waiting for opponent.";
            client.send(info);
        }
    }

    static class GameSession {
        ClientHandler redPlayer;
        ClientHandler blackPlayer;
        CheckersGame game = new CheckersGame();
        boolean finished = false;
        boolean redRematch = false;
        boolean blackRematch = false;

        GameSession(ClientHandler a, ClientHandler b) {
            redPlayer = a;
            blackPlayer = b;
        }

        PieceColor colorOf(ClientHandler c) {
            return c == redPlayer ? PieceColor.RED : PieceColor.BLACK;
        }

        ClientHandler other(ClientHandler c) {
            return c == redPlayer ? blackPlayer : redPlayer;
        }
    }
}
