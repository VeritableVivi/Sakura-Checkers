package checkers.common;

import java.io.Serializable;

public class Message implements Serializable {
    public static final String CREATE_ACCOUNT = "CREATE_ACCOUNT";
    public static final String LOGIN = "LOGIN";
    public static final String AUTH_OK = "AUTH_OK";
    public static final String AUTH_FAIL = "AUTH_FAIL";
    public static final String PROFILE = "PROFILE";
    public static final String PROFILE_DATA = "PROFILE_DATA";
    public static final String QUEUE = "QUEUE";
    public static final String QUEUE_STATUS = "QUEUE_STATUS";
    public static final String MATCH_FOUND = "MATCH_FOUND";
    public static final String MOVE = "MOVE";
    public static final String GAME_STATE = "GAME_STATE";
    public static final String GAME_OVER = "GAME_OVER";
    public static final String CHAT = "CHAT";
    public static final String INFO = "INFO";
    public static final String ERROR = "ERROR";
    public static final String REMATCH = "REMATCH";
    public static final String LEAVE = "LEAVE";
    public static final String LOGOUT = "LOGOUT";
    public static final String OPPONENT_LEFT = "OPPONENT_LEFT";

    public String type;
    public String sender;
    public String text;
    public String username;
    public String password;
    public String color;
    public String opponent;
    public String result;
    public boolean yourTurn;
    public boolean accepted;
    public int fr, fc, tr, tc;
    public CheckersGame game;
    public UserStats stats;

    public Message(String type) {
        this.type = type;
    }
}
