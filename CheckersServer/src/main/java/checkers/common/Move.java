package checkers.common;

import java.io.Serializable;

public class Move implements Serializable {
    public int fr, fc, tr, tc, cr, cc;
    public boolean capture;

    public Move(int fr, int fc, int tr, int tc) {
        this.fr = fr;
        this.fc = fc;
        this.tr = tr;
        this.tc = tc;
    }

    public Move(int fr, int fc, int tr, int tc, int cr, int cc) {
        this(fr, fc, tr, tc);
        this.cr = cr;
        this.cc = cc;
        this.capture = true;
    }
}
