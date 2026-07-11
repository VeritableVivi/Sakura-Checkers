package checkers.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CheckersGame implements Serializable {
    public int[][] board = new int[8][8];
    public PieceColor turn = PieceColor.RED;
    public PieceColor winner = null;
    public boolean draw = false;
    public Integer chainR = null;
    public Integer chainC = null;
    public int quiet = 0;

    public CheckersGame() {
        reset();
    }

    public void reset() {
        board = new int[8][8];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r + c) % 2 == 1) board[r][c] = -1;
            }
        }
        for (int r = 5; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r + c) % 2 == 1) board[r][c] = 1;
            }
        }
        turn = PieceColor.RED;
        winner = null;
        draw = false;
        chainR = null;
        chainC = null;
        quiet = 0;
    }

    public CheckersGame copy() {
        CheckersGame g = new CheckersGame();
        g.board = new int[8][8];
        for (int i = 0; i < 8; i++) {
            g.board[i] = Arrays.copyOf(board[i], 8);
        }
        g.turn = turn;
        g.winner = winner;
        g.draw = draw;
        g.chainR = chainR;
        g.chainC = chainC;
        g.quiet = quiet;
        return g;
    }

    public String applyMove(PieceColor p, int fr, int fc, int tr, int tc) {
        if (winner != null || draw) return "ERROR:Game already finished.";
        if (p != turn) return "ERROR:Not your turn.";
        List<Move> legal = legalMoves(p);
        Move chosen = null;
        for (Move m : legal) {
            if (m.fr == fr && m.fc == fc && m.tr == tr && m.tc == tc) {
                chosen = m;
                break;
            }
        }
        if (chosen == null) return "ERROR:Illegal move.";

        int piece = board[fr][fc];
        board[fr][fc] = 0;
        board[tr][tc] = piece;

        if (chosen.capture) {
            board[chosen.cr][chosen.cc] = 0;
            quiet = 0;
        } else {
            quiet++;
        }

        if (piece == 1 && tr == 0) board[tr][tc] = 2;
        if (piece == -1 && tr == 7) board[tr][tc] = -2;

        if (chosen.capture) {
            List<Move> next = capturesFor(p, tr, tc);
            if (!next.isEmpty()) {
                chainR = tr;
                chainC = tc;
                return "Capture made. Continue jumping.";
            }
        }

        chainR = null;
        chainC = null;
        turn = turn.opposite();

        if (count(turn) == 0 || legalMoves(turn).isEmpty()) {
            winner = p;
        } else if (quiet >= 80) {
            draw = true;
        }

        return chosen.capture ? "Move complete after capture." : "Move complete.";
    }

    public int count(PieceColor p) {
        int n = 0;
        for (int[] row : board) {
            for (int x : row) {
                if (p == PieceColor.RED && x > 0) n++;
                if (p == PieceColor.BLACK && x < 0) n++;
            }
        }
        return n;
    }

    public List<Move> legalMoves(PieceColor p) {
        List<Move> captures = new ArrayList<Move>();
        List<Move> regular = new ArrayList<Move>();

        if (chainR != null && chainC != null) {
            return capturesFor(p, chainR.intValue(), chainC.intValue());
        }

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (own(p, board[r][c])) captures.addAll(capturesFor(p, r, c));
            }
        }
        if (!captures.isEmpty()) return captures;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (own(p, board[r][c])) regular.addAll(normalsFor(r, c));
            }
        }
        return regular;
    }

    public List<Move> normalsFor(int r, int c) {
        List<Move> out = new ArrayList<Move>();
        for (int[] d : dirs(board[r][c])) {
            int nr = r + d[0];
            int nc = c + d[1];
            if (in(nr, nc) && board[nr][nc] == 0) out.add(new Move(r, c, nr, nc));
        }
        return out;
    }

    public List<Move> capturesFor(PieceColor p, int r, int c) {
        List<Move> out = new ArrayList<Move>();
        for (int[] d : dirs(board[r][c])) {
            int mr = r + d[0];
            int mc = c + d[1];
            int tr = r + 2 * d[0];
            int tc = c + 2 * d[1];
            if (in(mr, mc) && in(tr, tc) && board[tr][tc] == 0 && opp(p, board[mr][mc])) {
                out.add(new Move(r, c, tr, tc, mr, mc));
            }
        }
        return out;
    }

    private int[][] dirs(int piece) {
        if (Math.abs(piece) == 2) {
            return new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        }
        return piece > 0 ? new int[][]{{-1, 1}, {-1, -1}} : new int[][]{{1, 1}, {1, -1}};
    }

    private boolean own(PieceColor p, int piece) {
        return p == PieceColor.RED ? piece > 0 : piece < 0;
    }

    private boolean opp(PieceColor p, int piece) {
        return p == PieceColor.RED ? piece < 0 : piece > 0;
    }

    private boolean in(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }
}
