package org.shawn.games.Serendipity.Search.Listener;

import com.github.bhlangonijr.chesslib.move.Move;

public class FinalReport {
    public final Move bestMove;

    public FinalReport(Move bestMove) {
        this.bestMove = bestMove;
    }
}
