package checkers.common;

import java.io.Serializable;

public class UserStats implements Serializable {
    public String username;
    public int wins;
    public int losses;
    public int draws;

    public UserStats() {}

    public UserStats(String username) {
        this.username = username;
    }
}
