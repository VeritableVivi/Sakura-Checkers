package checkers.server;

import checkers.common.UserStats;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AccountStore {
    private final File file;
    private Map<String, String> passwords = new HashMap<String, String>();
    private Map<String, UserStats> stats = new HashMap<String, UserStats>();

    public AccountStore(File file) {
        this.file = file;
        load();
    }

    public synchronized boolean createAccount(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) return false;
        if (passwords.containsKey(username)) return false;
        passwords.put(username, password);
        stats.put(username, new UserStats(username));
        save();
        return true;
    }

    public synchronized boolean validateLogin(String username, String password) {
        return passwords.containsKey(username) && passwords.get(username).equals(password);
    }

    public synchronized UserStats getStats(String username) {
        UserStats s = stats.get(username);
        if (s == null) {
            s = new UserStats(username);
            stats.put(username, s);
            save();
        }
        UserStats copy = new UserStats(s.username);
        copy.wins = s.wins;
        copy.losses = s.losses;
        copy.draws = s.draws;
        return copy;
    }

    public synchronized void recordWin(String winner, String loser) {
        if (winner != null) {
            UserStats w = stats.computeIfAbsent(winner, UserStats::new);
            w.wins++;
        }
        if (loser != null) {
            UserStats l = stats.computeIfAbsent(loser, UserStats::new);
            l.losses++;
        }
        save();
    }

    public synchronized void recordDraw(String a, String b) {
        if (a != null) stats.computeIfAbsent(a, UserStats::new).draws++;
        if (b != null) stats.computeIfAbsent(b, UserStats::new).draws++;
        save();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!file.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            passwords = (Map<String, String>) in.readObject();
            stats = (Map<String, UserStats>) in.readObject();
        } catch (Exception ignored) {
            passwords = new HashMap<String, String>();
            stats = new HashMap<String, UserStats>();
        }
    }

    private void save() {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(passwords);
            out.writeObject(stats);
        } catch (IOException ignored) {
        }
    }
}
