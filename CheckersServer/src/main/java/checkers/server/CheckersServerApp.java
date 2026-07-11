package checkers.server;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Random;

public class CheckersServerApp extends JFrame {
    private final JTextArea logArea = new JTextArea();
    private final CheckersServer server;

    public CheckersServerApp() {
        super("Hello Kitty Checkers Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 680);
        setMinimumSize(new Dimension(860, 560));
        setLocationRelativeTo(null);

        ThemePanel root = new ThemePanel();
        root.setLayout(new BorderLayout(18, 18));
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel hero = shellPanel();
        hero.setLayout(new BorderLayout(16, 16));
        hero.setBorder(new EmptyBorder(20, 22, 20, 22));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("HELLO KITTY CHECKERS SERVER");
        title.setFont(Theme.titleFont());
        title.setForeground(Theme.TEXT_DARK);

        JLabel subtitle = new JLabel("Live activity and matchmaking log");
        subtitle.setFont(Theme.subtitleFont());
        subtitle.setForeground(Theme.TEXT_MID);

        JLabel helper = new JLabel("<html><div style='width:420px;'>Keep this server window open while clients connect. The server tracks accounts, queue events, matches, moves, rematches, chat, and disconnects.</div></html>");
        helper.setFont(Theme.bodyFont());
        helper.setForeground(Theme.TEXT_DARK);

        left.add(title);
        left.add(Box.createVerticalStrut(8));
        left.add(subtitle);
        left.add(Box.createVerticalStrut(10));
        left.add(helper);

        hero.add(left, BorderLayout.CENTER);
        hero.add(heroBadge("Server\nOnline"), BorderLayout.EAST);

        root.add(hero, BorderLayout.NORTH);

        JPanel centerCard = shellPanel();
        centerCard.setLayout(new BorderLayout(12, 12));
        centerCard.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel topBar = new JPanel(new GridLayout(1, 3, 10, 10));
        topBar.setOpaque(false);
        topBar.add(infoPill("Port 5555"));
        topBar.add(infoPill("Client/Server Mode"));
        topBar.add(infoPill("Themed Log Panel"));
        centerCard.add(topBar, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        logArea.setForeground(new Color(95, 58, 82));
        logArea.setBackground(new Color(255, 249, 252));
        logArea.setMargin(new Insets(14, 14, 14, 14));
        logArea.setBorder(new RoundedBorder(26, new Color(236, 201, 223), 2));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(255, 249, 252));
        centerCard.add(scrollPane, BorderLayout.CENTER);

        JLabel footer = new JLabel("Server log updates in real time as players create accounts, queue, chat, and play.", SwingConstants.CENTER);
        footer.setFont(Theme.smallFont());
        footer.setForeground(Theme.TEXT_SOFT);
        centerCard.add(footer, BorderLayout.SOUTH);

        root.add(centerCard, BorderLayout.CENTER);

        setContentPane(root);
        server = new CheckersServer(5555, this::appendLog);
        server.start();
        appendLog("Server started on port 5555.");
    }

    private JLabel infoPill(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(new Color(255, 242, 248));
        label.setForeground(Theme.TEXT_DARK);
        label.setFont(Theme.bodyFont().deriveFont(Font.BOLD));
        label.setBorder(new CompoundRoundedBorder());
        return label;
    }

    private JComponent heroBadge(String text) {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 228, 240));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(238, 170, 203));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(150, 150));
        panel.setLayout(new GridBagLayout());
        JLabel label = new JLabel("<html><div style='text-align:center;'>" + text.replace("\n", "<br>") + "</div></html>");
        label.setFont(Theme.subtitleFont());
        label.setForeground(Theme.TEXT_DARK);
        panel.add(label);
        return panel;
    }

    private JPanel shellPanel() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
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
        return p;
    }

    private void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CheckersServerApp().setVisible(true));
    }

    static class ThemePanel extends JPanel {
        ThemePanel() {
            setBackground(Theme.BG_MAIN);
        }

        @Override
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
            g2.setColor(new Color(77, 46, 58, 160));
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D path1 = new Path2D.Double();
            path1.moveTo(32, 88);
            path1.curveTo(160, 10, 240, 120, 310, 70);
            g2.draw(path1);
            g2.drawLine(110, 54, 84, 18);
            g2.drawLine(165, 52, 190, 22);
            g2.drawLine(222, 73, 248, 40);
            Path2D path2 = new Path2D.Double();
            path2.moveTo(w - 270, h - 120);
            path2.curveTo(w - 180, h - 180, w - 100, h - 20, w - 28, h - 86);
            g2.draw(path2);
            g2.drawLine(w - 200, h - 132, w - 232, h - 168);
            g2.drawLine(w - 150, h - 116, w - 122, h - 150);
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
            drawBow(g2, w - 86, 54, 0.75);
            drawBow(g2, 64, h - 78, 0.68);
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

    static class RoundedBorder extends AbstractBorder {
        private final int arc;
        private final Color color;
        private final int thickness;

        RoundedBorder(int arc, Color color, int thickness) {
            this.arc = arc;
            this.color = color;
            this.thickness = thickness;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(10, 14, 10, 14);
        }

        @Override
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
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(10, 16, 10, 16);
        }

        @Override
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
        static final Color TEXT_DARK = new Color(111, 62, 92);
        static final Color TEXT_MID = new Color(137, 90, 118);
        static final Color TEXT_SOFT = new Color(156, 117, 142);

        static Font titleFont() { return new Font("Serif", Font.BOLD, 34); }
        static Font subtitleFont() { return new Font("SansSerif", Font.BOLD, 22); }
        static Font bodyFont() { return new Font("SansSerif", Font.PLAIN, 16); }
        static Font smallFont() { return new Font("SansSerif", Font.PLAIN, 13); }
    }
}
