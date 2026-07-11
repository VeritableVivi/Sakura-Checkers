package checkers.client;

import checkers.common.CheckersGame;
import checkers.common.Message;
import checkers.common.Move;
import checkers.common.PieceColor;
import checkers.common.UserStats;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;
import java.util.Objects;

public class CheckersClientApp extends JFrame {


    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final Map<String, BufferedImage> imageCache = new HashMap<String, BufferedImage>();

    private final WelcomePanel welcome = new WelcomePanel();
    private final AuthPanel login = new AuthPanel(false);
    private final AuthPanel create = new AuthPanel(true);
    private final HomePanel home = new HomePanel();
    private final ProfilePanel profile = new ProfilePanel();
    private final QueuePanel queue = new QueuePanel();
    private final GamePanel game = new GamePanel();

    private ClientConnection conn;
    private String username = "";
    private String playerColor = "RED";
    private String opponent = "";
    private UserStats stats = new UserStats("");
    private boolean localBotMode = false;
    private CheckersGame localGame;


    public CheckersClientApp() {
        super("Hello Kitty Checkers Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 860);
        setMinimumSize(new Dimension(1180, 780));
        setLocationRelativeTo(null);

        root.add(welcome, "welcome");
        root.add(login, "login");
        root.add(create, "create");
        root.add(home, "home");
        root.add(profile, "profile");
        root.add(queue, "queue");
        root.add(game, "game");
        setContentPane(root);
        showCard("welcome");
    }

    void showCard(String name) {
        cards.show(root, name);
    }

    void send(Message m) {
        if (conn != null) {
            conn.send(m);
        }
    }

    BufferedImage getImage(String resourceName) {
        if (resourceName == null) return null;
        if (imageCache.containsKey(resourceName)) return imageCache.get(resourceName);
        try {
            BufferedImage img = ImageIO.read(Objects.requireNonNull(getClass().getResource("/" + resourceName)));
            imageCache.put(resourceName, img);
            return img;
        } catch (Exception e) {
            imageCache.put(resourceName, null);
            return null;
        }
    }

    Image scaledImage(String resourceName, int w, int h) {
        BufferedImage img = getImage(resourceName);
        if (img == null) return null;
        return img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
    }

    JLabel imageLabel(String resourceName, int w, int h) {
        JLabel label = new JLabel();
        Image img = scaledImage(resourceName, w, h);
        if (img != null) label.setIcon(new ImageIcon(img));
        label.setOpaque(false);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    void ensureConn(String host, int port) {
        if (conn != null && conn.isAlive()) return;
        conn = new ClientConnection(host, port, this::handleMessage);
        conn.start();
    }

    void handleMessage(final Message m) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (Message.ERROR.equals(m.type)) {
                    JOptionPane.showMessageDialog(CheckersClientApp.this, m.text, "Hello Kitty Checkers", JOptionPane.ERROR_MESSAGE);
                } else if (Message.INFO.equals(m.type)) {
                    queue.message.setText(m.text);
                    game.appendSystem(m.text);
                } else if (Message.AUTH_OK.equals(m.type)) {
                    if (m.stats != null) {
                        username = m.stats.username;
                        stats = m.stats;
                    }
                    home.refresh();
                    showCard("home");
                } else if (Message.AUTH_FAIL.equals(m.type)) {
                    JOptionPane.showMessageDialog(CheckersClientApp.this, m.text, "Authentication Error", JOptionPane.ERROR_MESSAGE);
                } else if (Message.PROFILE_DATA.equals(m.type)) {
                    if (m.stats != null) {
                        stats = m.stats;
                    }
                    profile.refresh();
                    showCard("profile");
                } else if (Message.QUEUE_STATUS.equals(m.type)) {
                    queue.message.setText(m.text);
                    showCard("queue");
                } else if (Message.MATCH_FOUND.equals(m.type)) {
                    localBotMode = false;
                    playerColor = m.color == null ? "RED" : m.color;
                    opponent = m.opponent == null ? "" : m.opponent;
                    game.loadRemote(m.game, m.yourTurn, m.text == null ? "Match found." : m.text);
                    showCard("game");
                } else if (Message.GAME_STATE.equals(m.type)) {
                    game.loadRemote(m.game, m.yourTurn, m.text == null ? "Game updated." : m.text);
                } else if (Message.CHAT.equals(m.type)) {
                    String from = m.sender == null ? "Opponent" : m.sender;
                    game.appendChat(from, m.text == null ? "" : m.text, from.equals(username));
                } else if (Message.GAME_OVER.equals(m.type)) {
                    game.loadRemote(m.game, false, m.text == null ? "Game over." : m.text);
                    game.finish(m.result == null ? "DRAW" : m.result, m.text == null ? "Game over." : m.text);
                } else if (Message.OPPONENT_LEFT.equals(m.type)) {
                    JOptionPane.showMessageDialog(CheckersClientApp.this,
                            m.text == null ? "Opponent left the game." : m.text,
                            "Opponent Left",
                            JOptionPane.INFORMATION_MESSAGE);
                    showCard("home");
                }
            }
        });
    }

    private void startBot() {
        localBotMode = true;
        localGame = new CheckersGame();
        game.clearChat();
        game.loadLocal("New solo game vs bot.");
        showCard("game");
    }

    class WelcomePanel extends ThemePanel {
        WelcomePanel() {
            setLayout(new GridBagLayout());
            JPanel card = shellPanel(new Dimension(470, 430));
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            JLabel title = themedTitle("CHECKERS");
            JLabel subtitle = themedSubtitle("WELCOME");
            JLabel helper = themedParagraph("Cute, cozy, and competitive. Log in or create an account to start playing.");
            helper.setAlignmentX(Component.CENTER_ALIGNMENT);
            JPanel mascots = new JPanel(new GridLayout(1, 2, 10, 10));
            mascots.setOpaque(false);
            mascots.add(imageLabel("kitty_welcome.png", 120, 120));
            mascots.add(imageLabel("cinnamoroll.png", 120, 80));
            RoundedButton logIn = primaryButton("LOG IN");
            RoundedButton createAccount = secondaryButton("Create Account");
            RoundedButton offline = secondaryButton("Play Offline vs Bot");
            logIn.addActionListener(e -> showCard("login"));
            createAccount.addActionListener(e -> showCard("create"));
            offline.addActionListener(e -> startBot());
            card.add(title);
            card.add(Box.createVerticalStrut(14));
            card.add(subtitle);
            card.add(Box.createVerticalStrut(8));
            card.add(helper);
            card.add(Box.createVerticalStrut(12));
            card.add(mascots);
            card.add(Box.createVerticalStrut(20));
            card.add(logIn);
            card.add(Box.createVerticalStrut(12));
            card.add(createAccount);
            card.add(Box.createVerticalStrut(12));
            card.add(offline);
            card.add(Box.createVerticalGlue());
            card.add(footerLabel("Wireframe-matched Hello Kitty edition"));
            add(card);
        }
    }

    class AuthPanel extends ThemePanel {
        private final boolean createMode;
        private final JTextField host = themedField("localhost");
        private final JTextField port = themedField("5555");
        private final JTextField user = themedField("");
        private final JPasswordField pass = themedPasswordField();
        private final JLabel status = themedMiniLabel(" ");

        AuthPanel(boolean createMode) {
            this.createMode = createMode;
            setLayout(new GridBagLayout());

            JPanel card = shellPanel(new Dimension(520, 600));
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

            String titleText = createMode ? "Create Account" : "Log In";
            card.add(themedTitle(titleText));
            card.add(Box.createVerticalStrut(10));

            card.add(themedParagraph(
                    createMode
                            ? "Create your cutest competitive profile."
                            : "Sign in to continue your checkers journey."
            ));
            card.add(Box.createVerticalStrut(12));

            JLabel mascot = imageLabel(createMode ? "kitty_pink.png" : "kitty_apples.png", 90, 90);
            mascot.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(mascot);
            card.add(Box.createVerticalStrut(12));

            card.add(labeledField("Server Host", host));
            card.add(Box.createVerticalStrut(12));

            card.add(labeledField("Port", port));
            card.add(Box.createVerticalStrut(12));

            card.add(labeledField("Username", user));
            card.add(Box.createVerticalStrut(12));

            card.add(labeledField("Password", pass));
            card.add(Box.createVerticalStrut(18));

            RoundedButton submit = primaryButton(createMode ? "Submit" : "Log In");
            RoundedButton back = secondaryButton("Back");

            submit.addActionListener(e -> submit());
            back.addActionListener(e -> showCard("welcome"));

            JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
            buttons.setOpaque(false);
            buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            buttons.add(submit);
            buttons.add(back);

            card.add(buttons);
            card.add(Box.createVerticalStrut(16));

            status.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(status);

            add(card);
        }
        private void submit() {
            try {
                String h = host.getText().trim();
                int p = Integer.parseInt(port.getText().trim());
                String u = user.getText().trim();
                String pw = new String(pass.getPassword());
                ensureConn(h, p);
                Message m = new Message(createMode ? Message.CREATE_ACCOUNT : Message.LOGIN);
                m.username = u;
                m.password = pw;
                send(m);
                status.setText(createMode ? "Create account request sent." : "Login request sent.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(CheckersClientApp.this, ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class HomePanel extends ThemePanel {
        private final JLabel welcomeText = themedTitle("Welcome");
        private final JLabel summary = themedSubtitle("");

        HomePanel() {
            setLayout(new BorderLayout(18, 18));
            setBorder(new EmptyBorder(26, 28, 26, 28));
            JPanel hero = shellPanel(new Dimension(0, 0));
            hero.setLayout(new BorderLayout());
            JPanel left = new JPanel();
            left.setOpaque(false);
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            left.add(welcomeText);
            left.add(Box.createVerticalStrut(8));
            left.add(summary);
            left.add(Box.createVerticalStrut(12));
            left.add(themedParagraph("Choose solo bot practice or connect online for a real match."));
            hero.add(left, BorderLayout.CENTER);
            hero.add(heroBadge("kitty_cheer.png"), BorderLayout.EAST);
            add(hero, BorderLayout.NORTH);

            JPanel grid = new JPanel(new GridLayout(1, 2, 18, 18));
            grid.setOpaque(false);
            grid.add(featureCard("Play", "Start a new game.", new String[]{"1 Player vs Bot", "2 Players Online"}, new Runnable[]{
                    CheckersClientApp.this::startBot,
                    new Runnable() {
                        public void run() {
                            Message m = new Message(Message.QUEUE);
                            m.sender = username;
                            send(m);
                        }
                    }
            }));
            grid.add(featureCard("Account", "Your record is stored on the server.", new String[]{"Profile", "Logout"}, new Runnable[]{
                    new Runnable() {
                        public void run() {
                            send(new Message(Message.PROFILE));
                        }
                    },
                    this::logout
            }));
            add(grid, BorderLayout.CENTER);
        }

        private JPanel featureCard(String title, String body, String[] labels, Runnable[] actions) {
            JPanel card = shellPanel(new Dimension(0, 0));
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.add(sectionLabel(title));
            card.add(Box.createVerticalStrut(8));
            card.add(themedParagraph(body));
            card.add(Box.createVerticalStrut(14));
            for (int i = 0; i < labels.length; i++) {
                RoundedButton b = i == 0 ? primaryButton(labels[i]) : secondaryButton(labels[i]);
                Runnable action = actions[i];
                b.addActionListener(e -> action.run());
                card.add(b);
                card.add(Box.createVerticalStrut(10));
            }
            card.add(Box.createVerticalGlue());
            card.add(footerLabel("Online games include chat, rematch, and cute stickers."));
            return card;
        }

        private void logout() {
            if (conn != null) {
                send(new Message(Message.LOGOUT));
                conn.closeConnection();
                conn = null;
            }
            username = "";
            stats = new UserStats("");
            showCard("welcome");
        }

        void refresh() {
            welcomeText.setText("Welcome, " + username + " ♡");
            summary.setText("Wins: " + stats.wins + "   Losses: " + stats.losses + "   Draws: " + stats.draws);
        }
    }

    class ProfilePanel extends ThemePanel {
        private final JLabel player = themedTitle("");
        private final JLabel record = themedSubtitle("");
        private final JTextArea note = new JTextArea();

        ProfilePanel() {
            setLayout(new GridBagLayout());
            JPanel card = shellPanel(new Dimension(560, 500));
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            player.setAlignmentX(Component.CENTER_ALIGNMENT);
            record.setAlignmentX(Component.CENTER_ALIGNMENT);
            note.setEditable(false);
            note.setOpaque(false);
            note.setLineWrap(true);
            note.setWrapStyleWord(true);
            note.setForeground(Theme.TEXT_DARK);
            note.setFont(Theme.bodyFont());
            note.setText("User can look at their profile with game statistics, just like your wireframe design.");
            RoundedButton refresh = secondaryButton("Refresh Stats");
            RoundedButton back = primaryButton("Back Home");
            refresh.addActionListener(e -> send(new Message(Message.PROFILE)));
            back.addActionListener(e -> showCard("home"));
            JPanel avatars = new JPanel(new GridLayout(1, 2, 8, 8));
            avatars.setOpaque(false);
            avatars.add(imageLabel("profile_icon.png", 84, 84));
            avatars.add(imageLabel("melody_kuromi.png", 90, 90));
            card.add(avatars);
            card.add(Box.createVerticalStrut(8));
            card.add(player);
            card.add(Box.createVerticalStrut(8));
            card.add(record);
            card.add(Box.createVerticalStrut(16));
            card.add(new JSeparator());
            card.add(Box.createVerticalStrut(14));
            card.add(note);
            card.add(Box.createVerticalStrut(18));
            card.add(refresh);
            card.add(Box.createVerticalStrut(10));
            card.add(back);
            add(card);
        }

        void refresh() {
            player.setText("Player: " + stats.username);
            record.setText("Record  W " + stats.wins + "   L " + stats.losses + "   D " + stats.draws);
        }
    }

    class QueuePanel extends ThemePanel {
        private final JLabel message = themedTitle("Waiting for an opponent...");

        QueuePanel() {
            setLayout(new GridBagLayout());
            JPanel card = shellPanel(new Dimension(520, 340));
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.add(message);
            card.add(Box.createVerticalStrut(16));
            card.add(themedParagraph("The server matches players in pairs. Stay here until your game appears."));
            card.add(Box.createVerticalStrut(12));
            card.add(imageLabel("dance_kitty.png", 120, 90));
            card.add(Box.createVerticalStrut(16));
            RoundedButton back = secondaryButton("Back Home");
            back.addActionListener(e -> showCard("home"));
            card.add(back);
            add(card);
        }
    }

    class GamePanel extends ThemePanel {
        private final Board board = new Board();
        private final JLabel left = themedInfoPill("User");
        private final JLabel centerStatus = themedInfoPill("Status");
        private final JLabel right = themedInfoPill("Opponent");
        private final JLabel turnBadge = themedInfoPill("Waiting");
        private final JLabel timerBadge = themedInfoPill("Soft timer: --");
        private final JTextPane chatPane = new JTextPane();
        private final JTextField entry = themedField("");
        private final RoundedButton sendButton = miniButton("Send");
        private final RoundedButton leaveButton = miniButton("Quit");
        private final RoundedButton rematchButton = miniButton("Rematch");
        private final RoundedButton homeButton = miniButton("Home");
        private final javax.swing.Timer uiTimer;
        private int softCountdown = 12;

        private CheckersGame state;
        private boolean myTurn = false;
        private int sr = -1, sc = -1;

        GamePanel() {
            setLayout(new BorderLayout(18, 18));
            setBorder(new EmptyBorder(20, 20, 20, 20));
            JPanel top = shellPanel(new Dimension(0, 0));
            top.setLayout(new GridLayout(1, 5, 12, 12));
            top.add(left);
            top.add(centerStatus);
            top.add(turnBadge);
            top.add(timerBadge);
            top.add(right);
            add(top, BorderLayout.NORTH);

            JPanel center = new JPanel(new GridBagLayout());
            center.setOpaque(false);
            center.add(board);
            add(center, BorderLayout.CENTER);

            JPanel side = shellPanel(new Dimension(360, 0));
            side.setPreferredSize(new Dimension(360, 0));
            side.setLayout(new BorderLayout(10, 10));
            JPanel sideTop = new JPanel(new BorderLayout(8, 8));
            sideTop.setOpaque(false);
            JLabel chatTitle = sectionLabel("Chat Box");
            sideTop.add(chatTitle, BorderLayout.WEST);
            sideTop.add(imageLabel("cinnamoroll_big.png", 70, 70), BorderLayout.EAST);
            side.add(sideTop, BorderLayout.NORTH);
            chatPane.setEditable(false);
            chatPane.setFont(Theme.bodyFont());
            chatPane.setBackground(new Color(255, 248, 252));
            chatPane.setBorder(new RoundedBorder(24, new Color(240, 210, 228), 2));
            JScrollPane chatScroll = new JScrollPane(chatPane);
            chatScroll.setBorder(BorderFactory.createEmptyBorder());
            side.add(chatScroll, BorderLayout.CENTER);

            JPanel south = new JPanel(new BorderLayout(10, 10));
            south.setOpaque(false);
            JPanel input = new JPanel(new BorderLayout(8, 8));
            input.setOpaque(false);
            input.add(entry, BorderLayout.CENTER);
            input.add(sendButton, BorderLayout.EAST);
            south.add(input, BorderLayout.NORTH);
            JPanel buttons = new JPanel(new GridLayout(1, 3, 8, 8));
            buttons.setOpaque(false);
            buttons.add(rematchButton);
            buttons.add(leaveButton);
            buttons.add(homeButton);
            south.add(buttons, BorderLayout.SOUTH);
            side.add(south, BorderLayout.SOUTH);
            add(side, BorderLayout.EAST);

            sendButton.addActionListener(e -> sendChat());
            entry.addActionListener(e -> sendChat());
            leaveButton.addActionListener(e -> {
                if (localBotMode) {
                    showCard("home");
                } else {
                    send(new Message(Message.LEAVE));
                    showCard("home");
                }
            });
            rematchButton.addActionListener(e -> {
                if (localBotMode) {
                    startBot();
                } else {
                    Message m = new Message(Message.REMATCH);
                    m.accepted = true;
                    send(m);
                }
            });
            Image homeImg = scaledImage("home_icon.png", 18, 18);
            if (homeImg != null) homeButton.setIcon(new ImageIcon(homeImg));
            Image crownImg = scaledImage("crown_icon.png", 18, 18);
            if (crownImg != null) rematchButton.setIcon(new ImageIcon(crownImg));
            homeButton.addActionListener(e -> {
                sr = sc = -1;
                localBotMode = false;
                showCard("welcome");
            });
            rematchButton.setVisible(false);

            uiTimer = new javax.swing.Timer(1000, e -> {
                if (state == null) {
                    timerBadge.setText("Soft timer: --");
                    return;
                }
                softCountdown--;
                if (softCountdown < 0) softCountdown = 12;
                timerBadge.setText("Soft timer: 0:" + (softCountdown < 10 ? "0" : "") + softCountdown);
            });
            uiTimer.start();
        }

        void clearChat() {
            chatPane.setText("");
        }

        void appendSystem(String text) {
            appendChat("System", text, false);
        }

        void appendChat(String from, String text, boolean mine) {
            String prefix = mine ? "Me" : from;
            chatPane.setText(chatPane.getText() + "\n" + prefix + ": " + text);
            chatPane.setCaretPosition(chatPane.getDocument().getLength());
        }

        void sendChat() {
            String t = entry.getText().trim();
            if (t.isEmpty()) return;
            entry.setText("");
            if (localBotMode) {
                appendChat(username.isEmpty() ? "You" : username, t, true);
                appendChat("Bot", generateBotReply(t), false);
            } else {
                Message m = new Message(Message.CHAT);
                m.sender = username;
                m.text = t;
                send(m);
            }
        }

        String generateBotReply(String text) {
            String msg = text == null ? "" : text.toLowerCase().trim();
            if (msg.contains("hi") || msg.contains("hello") || msg.contains("hey")) return "Hi! Ready for a good game?";
            if (msg.contains("good luck")) return "Good luck to you too!";
            if (msg.contains("your turn")) return "Yep, I am looking for my best move now.";
            if (msg.contains("nice") || msg.contains("good move")) return "Thanks! I am trying my best.";
            if (msg.contains("haha") || msg.contains("lol")) return "Haha, this game is getting fun.";
            if (msg.contains("help")) return "Try controlling the center and watching for forced jumps.";
            return "I saw your message: \"" + text + "\".";
        }

        void loadRemote(CheckersGame s, boolean turn, String msg) {
            state = s;
            myTurn = turn;
            sr = sc = -1;
            left.setText(username + " (" + playerColor + ")");
            right.setText(opponent);
            centerStatus.setText((msg == null ? "Game updated." : msg) + "   |   " + (turn ? "YOUR TURN" : "OPPONENT TURN"));
            turnBadge.setText(turn ? "Your turn ♡" : "Opponent turn");
            turnBadge.setForeground(turn ? new Color(214, 61, 122) : new Color(90, 60, 80));
            softCountdown = 12;
            rematchButton.setVisible(false);
            board.repaint();
        }

        void loadLocal(String msg) {
            state = localGame.copy();
            myTurn = localGame.turn == PieceColor.RED;
            left.setText(username.isEmpty() ? "You (RED)" : username + " (RED)");
            right.setText("Bot (BLACK)");
            centerStatus.setText((msg == null ? "Local game." : msg) + "   |   " + (myTurn ? "YOUR TURN" : "BOT TURN"));
            turnBadge.setText(myTurn ? "Your turn ♡" : "Bot thinking...");
            turnBadge.setForeground(myTurn ? new Color(214, 61, 122) : new Color(90, 60, 80));
            softCountdown = 12;
            rematchButton.setVisible(false);
            board.repaint();
        }

        void finish(String result, String msg) {
            centerStatus.setText(msg);
            rematchButton.setVisible(true);

            String title = "Draw";
            if ("WIN".equals(result)) title = "Winner!";
            else if ("LOSS".equals(result)) title = "Lost";

            Color bg = new Color(240, 240, 240);
            if ("WIN".equals(result)) bg = new Color(230, 212, 248);
            else if ("LOSS".equals(result)) bg = new Color(207, 228, 255);

            JLabel resultLabel = new JLabel(title, SwingConstants.CENTER);
            resultLabel.setFont(Theme.titleFont().deriveFont(34f));
            resultLabel.setForeground(Theme.TEXT_DARK);
            resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            String resultImageName;
            if ("WIN".equals(result)) {
                resultImageName = "kitty_cheer.png";
            } else if ("LOSS".equals(result)) {
                resultImageName = "kitty_cry.png";
            } else {
                resultImageName = "kitty_pink.png";
            }

            JLabel resultImage = imageLabel(resultImageName, 120, 120);
            resultImage.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel detail = new JLabel(msg, SwingConstants.CENTER);
            detail.setFont(Theme.bodyFont());
            detail.setForeground(Theme.TEXT_DARK);
            detail.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel panel = new JPanel();
            panel.setBackground(bg);
            panel.setBorder(new EmptyBorder(20, 24, 20, 24));
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            panel.add(resultLabel);
            panel.add(Box.createVerticalStrut(10));
            panel.add(resultImage);
            panel.add(Box.createVerticalStrut(12));
            panel.add(detail);

            JOptionPane.showMessageDialog(
                    CheckersClientApp.this,
                    panel,
                    "Game Result",
                    JOptionPane.PLAIN_MESSAGE
            );
        }

        class Board extends JPanel {
            Board() {
                setPreferredSize(new Dimension(620, 620));
                setOpaque(false);
                addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (state == null) return;
                        int cell = Math.min(getWidth(), getHeight()) / 8;
                        int ox = (getWidth() - cell * 8) / 2;
                        int oy = (getHeight() - cell * 8) / 2;
                        int x = e.getX() - ox;
                        int y = e.getY() - oy;
                        if (x < 0 || y < 0) return;
                        int c = x / cell;
                        int r = y / cell;
                        if (r < 0 || r > 7 || c < 0 || c > 7) return;
                        if (localBotMode) localClick(r, c); else remoteClick(r, c);
                    }
                });
            }

            void remoteClick(int r, int c) {
                if (!myTurn) {
                    Toolkit.getDefaultToolkit().beep();
                    centerStatus.setText("Wait for your turn.");
                    return;
                }
                int piece = state.board[r][c];
                boolean mine = "RED".equals(playerColor) ? piece > 0 : piece < 0;
                if (sr == -1 && mine) {
                    sr = r;
                    sc = c;
                    repaint();
                } else if (sr != -1) {
                    if (mine) {
                        sr = r;
                        sc = c;
                        repaint();
                    } else {
                        Message m = new Message(Message.MOVE);
                        m.fr = sr;
                        m.fc = sc;
                        m.tr = r;
                        m.tc = c;
                        send(m);
                        sr = sc = -1;
                    }
                }
            }

            void localClick(int r, int c) {
                if (localGame == null || localGame.turn != PieceColor.RED || localGame.winner != null || localGame.draw) return;
                int piece = localGame.board[r][c];
                boolean mine = piece > 0;
                if (sr == -1 && mine) {
                    sr = r;
                    sc = c;
                    repaint();
                    return;
                }
                if (sr != -1) {
                    if (mine) {
                        sr = r;
                        sc = c;
                        repaint();
                    } else {
                        String msg = localGame.applyMove(PieceColor.RED, sr, sc, r, c);
                        if (!msg.startsWith("ERROR:")) {
                            sr = sc = -1;
                            loadLocal(msg);
                            if (localGame.winner == PieceColor.RED) {
                                finish("WIN", "You beat the bot!");
                                return;
                            }
                            if (localGame.draw) {
                                finish("DRAW", "Draw game.");
                                return;
                            }
                            if (localGame.turn == PieceColor.BLACK) {
                                javax.swing.Timer t = new javax.swing.Timer(650, e -> botMove());
                                t.setRepeats(false);
                                t.start();
                            }
                        } else {
                            Toolkit.getDefaultToolkit().beep();
                            centerStatus.setText(msg);
                        }
                    }
                }
            }

            void botMove() {
                List<Move> moves = localGame.legalMoves(PieceColor.BLACK);
                if (moves.isEmpty()) {
                    localGame.winner = PieceColor.RED;
                    finish("WIN", "Bot cannot move.");
                    return;
                }
                Move m = moves.get(ThreadLocalRandom.current().nextInt(moves.size()));
                String msg = localGame.applyMove(PieceColor.BLACK, m.fr, m.fc, m.tr, m.tc);
                loadLocal("Bot moved. " + msg);
                while (localGame.turn == PieceColor.BLACK && localGame.chainR != null) {
                    List<Move> next = localGame.legalMoves(PieceColor.BLACK);
                    if (next.isEmpty()) break;
                    Move again = next.get(ThreadLocalRandom.current().nextInt(next.size()));
                    msg = localGame.applyMove(PieceColor.BLACK, again.fr, again.fc, again.tr, again.tc);
                    loadLocal("Bot continues. " + msg);
                }
                if (localGame.winner == PieceColor.BLACK) finish("LOSS", "Bot won.");
                else if (localGame.winner == PieceColor.RED) finish("WIN", "You beat the bot!");
                else if (localGame.draw) finish("DRAW", "Draw game.");
            }

            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cell = Math.min(getWidth(), getHeight()) / 8;
                int ox = (getWidth() - cell * 8) / 2;
                int oy = (getHeight() - cell * 8) / 2;
                g2.setColor(new Color(255, 245, 251));
                g2.fillRoundRect(ox - 16, oy - 16, cell * 8 + 32, cell * 8 + 32, 32, 32);
                g2.setColor(new Color(235, 193, 216));
                g2.setStroke(new BasicStroke(4f));
                g2.drawRoundRect(ox - 16, oy - 16, cell * 8 + 32, cell * 8 + 32, 32, 32);
                Color light = new Color(255, 236, 246);
                Color dark = new Color(246, 190, 218);
                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < 8; c++) {
                        g2.setColor((r + c) % 2 == 0 ? light : dark);
                        g2.fillRect(ox + c * cell, oy + r * cell, cell, cell);
                    }
                }
                if (state != null && sr >= 0) {
                    g2.setColor(new Color(255, 0, 120, 90));
                    g2.fillRoundRect(ox + sc * cell, oy + sr * cell, cell, cell, 18, 18);
                    g2.setColor(new Color(255, 20, 147));
                    g2.setStroke(new BasicStroke(4f));
                    g2.drawRoundRect(ox + sc * cell + 2, oy + sr * cell + 2, cell - 4, cell - 4, 18, 18);
                }
                if (state != null) {
                    List<Move> hints = Collections.emptyList();
                    if (sr >= 0) {
                        CheckersGame temp = localBotMode ? localGame : state;
                        PieceColor who = localBotMode ? PieceColor.RED : ("RED".equals(playerColor) ? PieceColor.RED : PieceColor.BLACK);
                        hints = temp.legalMoves(who);
                    }
                    for (Move m : hints) {
                        if (m.fr == sr && m.fc == sc) {
                            g2.setColor(new Color(255, 255, 255, 170));
                            g2.fillOval(ox + m.tc * cell + cell / 3, oy + m.tr * cell + cell / 3, cell / 3, cell / 3);
                        }
                    }
                    for (int r = 0; r < 8; r++) {
                        for (int c = 0; c < 8; c++) {
                            int p = state.board[r][c];
                            if (p != 0) paintPiece(g2, ox, oy, cell, r, c, p);
                        }
                    }
                }
                g2.dispose();
            }

            private void paintPiece(Graphics2D g2, int ox, int oy, int cell, int r, int c, int piece) {
                int x = ox + c * cell + cell / 8;
                int y = oy + r * cell + cell / 8;
                int d = cell * 3 / 4;
                boolean redPiece = piece > 0;
                Color base = redPiece ? new Color(255, 212, 232) : new Color(108, 49, 80);
                Color edge = redPiece ? new Color(235, 121, 176) : new Color(66, 23, 50);
                Color shine = redPiece ? new Color(255, 244, 248) : new Color(170, 118, 146);
                g2.setColor(base);
                g2.fillOval(x, y, d, d);
                g2.setColor(edge);
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(x, y, d, d);
                g2.setColor(shine);
                g2.drawOval(x + 6, y + 6, d - 12, d - 12);
                g2.setColor(new Color(255, 255, 255, 110));
                g2.fillOval(x + d / 4, y + d / 5, d / 3, d / 5);
                if (Math.abs(piece) == 2) {
                    g2.setColor(redPiece ? new Color(162, 70, 110) : new Color(255, 214, 234));
                    g2.setFont(Theme.titleFont().deriveFont((float) cell / 2.2f));
                    g2.drawString("♕", x + d / 2 - cell / 6, y + d / 2 + cell / 7);
                }
            }
        }
    }

    abstract class ThemePanel extends JPanel {
        ThemePanel() {
            setBackground(Theme.BG_MAIN);
        }

        JPanel shellPanel(Dimension preferred) {
            JPanel p = new JPanel() {
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255, 249, 252, 232));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 34, 34);
                    g2.setColor(new Color(236, 201, 223));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 34, 34);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            p.setOpaque(false);
            p.setBorder(new EmptyBorder(26, 26, 26, 26));
            if (preferred.width > 0 || preferred.height > 0) p.setPreferredSize(preferred);
            return p;
        }

        JLabel themedTitle(String text) {
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            label.setFont(Theme.titleFont());
            label.setForeground(Theme.TEXT_DARK);
            return label;
        }

        JLabel themedSubtitle(String text) {
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            label.setFont(Theme.subtitleFont());
            label.setForeground(Theme.TEXT_MID);
            return label;
        }

        JLabel themedParagraph(String text) {
            JLabel label = new JLabel("<html><div style='text-align:center; width:340px;'>" + text + "</div></html>", SwingConstants.CENTER);
            label.setFont(Theme.bodyFont());
            label.setForeground(Theme.TEXT_DARK);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            return label;
        }

        JLabel sectionLabel(String text) {
            JLabel label = new JLabel(text);
            label.setFont(Theme.subtitleFont());
            label.setForeground(Theme.TEXT_DARK);
            return label;
        }

        JLabel footerLabel(String text) {
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            label.setFont(Theme.smallFont());
            label.setForeground(Theme.TEXT_SOFT);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            return label;
        }

        JLabel themedMiniLabel(String text) {
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            label.setFont(Theme.smallFont());
            label.setForeground(Theme.TEXT_SOFT);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            return label;
        }

        JLabel themedInfoPill(String text) {
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBackground(new Color(255, 242, 248));
            label.setForeground(Theme.TEXT_DARK);
            label.setFont(Theme.bodyFont().deriveFont(Font.BOLD));
            label.setBorder(new CompoundRoundedBorder());
            return label;
        }

        JTextField themedField(String text) {
            JTextField field = new JTextField(text);
            field.setFont(Theme.bodyFont());
            field.setForeground(Theme.TEXT_DARK);
            field.setBackground(Color.WHITE);
            field.setCaretColor(Theme.TEXT_DARK);
            field.setBorder(new RoundedBorder(20, new Color(234, 199, 220), 2));
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            field.setPreferredSize(new Dimension(0, 44));
            return field;
        }

        JPasswordField themedPasswordField() {
            JPasswordField field = new JPasswordField();
            field.setFont(Theme.bodyFont());
            field.setForeground(Theme.TEXT_DARK);
            field.setBackground(Color.WHITE);
            field.setCaretColor(Theme.TEXT_DARK);
            field.setBorder(new RoundedBorder(20, new Color(234, 199, 220), 2));
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            field.setPreferredSize(new Dimension(0, 44));
            return field;
        }

        JPanel labeledField(String label, JComponent field) {
            JPanel p = new JPanel();
            p.setOpaque(false);
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            JLabel l = new JLabel(label);
            l.setFont(Theme.bodyFont().deriveFont(Font.BOLD));
            l.setForeground(Theme.TEXT_DARK);
            p.add(l);
            p.add(Box.createVerticalStrut(6));
            p.add(field);
            return p;
        }

        RoundedButton primaryButton(String text) {
            return new RoundedButton(text, Theme.BUTTON_PRIMARY, Theme.TEXT_ON_DARK, 22, 48, true);
        }

        RoundedButton secondaryButton(String text) {
            return new RoundedButton(text, Theme.BUTTON_SECONDARY, Theme.TEXT_DARK, 22, 48, false);
        }

        RoundedButton miniButton(String text) {
            return new RoundedButton(text, Theme.BUTTON_SECONDARY, Theme.TEXT_DARK, 18, 40, false);
        }
        JComponent heroBadge(String resourceName) {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setOpaque(false);
            panel.setPreferredSize(new Dimension(160, 160));
            panel.add(imageLabel(resourceName, 140, 140));
            return panel;
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, Theme.BG_MAIN, getWidth(), getHeight(), Theme.BG_SECONDARY);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            paintBranches(g2, getWidth(), getHeight());
            paintPetals(g2, getWidth(), getHeight());
            paintBows(g2, getWidth(), getHeight());
            g2.dispose();
        }
        private void paintBranches(Graphics2D g2, int w, int h) {
            Image branch = scaledImage("branch.png", 320, 240);
            if (branch != null) {
                g2.drawImage(branch, 20, 10, null);
                g2.drawImage(branch, w - 340, h - 220, null);
            }
            Image home = scaledImage("home_icon.png", 36, 36);
            if (home != null) g2.drawImage(home, w - 58, 18, null);
        }

        private void paintPetals(Graphics2D g2, int w, int h) {
            Random rand = new Random(6);
            for (int i = 0; i < 46; i++) {
                int x = rand.nextInt(Math.max(w, 1));
                int y = rand.nextInt(Math.max(h, 1));
                int size = 6 + rand.nextInt(8);
                g2.setColor(i % 2 == 0 ? new Color(255, 145, 190, 130) : new Color(255, 196, 218, 140));
                g2.fillOval(x, y, size, size);
                g2.setColor(new Color(255, 102, 160, 160));
                g2.fillOval(x + size / 3, y + size / 3, size / 4 + 1, size / 4 + 1);
            }
        }
        private void paintBows(Graphics2D g2, int w, int h) {
            Image kitty = scaledImage("kitty_pink.png", 72, 72);
            Image cinna = scaledImage("cinnamoroll.png", 84, 60);
            if (kitty != null) g2.drawImage(kitty, w - 110, 56, null);
            if (cinna != null) g2.drawImage(cinna, 30, h - 95, null);
        }

        private void drawBow(Graphics2D g2, int x, int y, double scale) {
            int s = (int) (40 * scale);
            g2.setColor(new Color(255, 166, 205, 130));
            g2.fillOval(x, y, s, s / 2);
            g2.fillOval(x + s / 2, y, s, s / 2);
            g2.fillOval(x + s / 2 - 6, y + 6, 16, 16);
            g2.setColor(new Color(255, 108, 170, 150));
            g2.drawOval(x, y, s, s / 2);
            g2.drawOval(x + s / 2, y, s, s / 2);
        }
    }

    static class RoundedButton extends JButton {
        private final Color fill;
        private final int arc;
        private final int prefHeight;
        private final boolean shadow;

        RoundedButton(String text, Color fill, Color fg, int arc, int prefHeight, boolean shadow) {
            super(text);
            this.fill = fill;
            this.arc = arc;
            this.prefHeight = prefHeight;
            this.shadow = shadow;
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(fg);
            setFont(Theme.bodyFont().deriveFont(Font.BOLD));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        public Dimension getMaximumSize() {
            Dimension d = super.getMaximumSize();
            return new Dimension(Integer.MAX_VALUE, prefHeight);
        }

        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(170, d.width + 32), prefHeight);
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (shadow) {
                g2.setColor(new Color(173, 116, 146, 70));
                g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, arc, arc);
            }
            g2.setColor(getModel().isPressed() ? fill.darker() : fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(new Color(255, 255, 255, 80));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RoundedBorder extends AbstractBorder {
        private final int arc;
        private final Color color;
        private final int thickness;

        RoundedBorder(int arc, Color color, int thickness) {
            this.arc = arc;
            this.color = color;
            this.thickness = thickness;
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(10, 14, 10, 14);
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x + 1, y + 1, width - 3, height - 3, arc, arc);
            g2.dispose();
        }
    }

    static class CompoundRoundedBorder extends AbstractBorder {
        public Insets getBorderInsets(Component c) {
            return new Insets(10, 16, 10, 16);
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(234, 199, 220));
            g2.drawRoundRect(x, y, width - 1, height - 1, 24, 24);
            g2.setColor(new Color(255, 255, 255, 110));
            g2.drawRoundRect(x + 1, y + 1, width - 3, height - 3, 24, 24);
            g2.dispose();
        }
    }

    static class Theme {
        static final Color BG_MAIN = new Color(236, 219, 242);
        static final Color BG_SECONDARY = new Color(218, 228, 255);
        static final Color BUTTON_PRIMARY = new Color(255, 178, 205);
        static final Color BUTTON_SECONDARY = new Color(255, 227, 238);
        static final Color TEXT_DARK = new Color(111, 62, 92);
        static final Color TEXT_MID = new Color(137, 90, 118);
        static final Color TEXT_SOFT = new Color(156, 117, 142);
        static final Color TEXT_ON_DARK = Color.WHITE;

        static Font titleFont() { return new Font("Serif", Font.BOLD, 38); }
        static Font subtitleFont() { return new Font("SansSerif", Font.BOLD, 22); }
        static Font bodyFont() { return new Font("SansSerif", Font.PLAIN, 17); }
        static Font smallFont() { return new Font("SansSerif", Font.PLAIN, 13); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CheckersClientApp().setVisible(true));
    }
}
