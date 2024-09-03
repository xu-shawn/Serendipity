package org.shawn.games.Serendipity.Search.History;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

public interface History {
    int get(Board board, Move move);

    void register(Board board, Move move, int value);

    void fill(int x);
}
