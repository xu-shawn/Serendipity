package org.shawn.games.Serendipity.Search.Listener;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

public class SearchReport {
    public final int depth;
    public final int selDepth;
    public final long nodes;
    public final int hashfull;
    public final int score;
    public final long ms;
    public final Board board;
    public final Move[] pv;

    public SearchReport(int depth, int selDepth, long nodes, int hashfull, int score, long ms, Board board, Move[] pv) {
        this.depth = depth;
        this.selDepth = selDepth;
        this.nodes = nodes;
        this.hashfull = hashfull;
        this.score = score;
        this.ms = ms;
        this.board = board;
        this.pv = pv;
    }
}
