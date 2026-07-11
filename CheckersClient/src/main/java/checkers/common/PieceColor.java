package checkers.common;

public enum PieceColor {
    RED, BLACK;

    public PieceColor opposite() {
        return this == RED ? BLACK : RED;
    }
}
