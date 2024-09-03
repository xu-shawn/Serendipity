package org.shawn.games.Serendipity.Search.History;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.Arrays;

public class FromToHistory implements History {
    private final int[] history;

    static final int MAX_BONUS = 16384;

    final int DIMENSION = Square.values().length;

    public FromToHistory() {
        history = new int[DIMENSION * DIMENSION];
    }

    public int get(Square from, Square to) {
        return history[from.ordinal() * DIMENSION + to.ordinal()];
    }

    public int get(Move move) {
        return get(move.getFrom(), move.getTo());
    }

    private static int clamp(int v) {
        return v >= FromToHistory.MAX_BONUS ? FromToHistory.MAX_BONUS : (Math.max(v, -16384));
    }

    public void register(Square from, Square to, int value) {
        int clampedValue = clamp(value);

        history[from.ordinal() * DIMENSION + to.ordinal()] += clampedValue
                - history[from.ordinal() * DIMENSION + to.ordinal()] * Math.abs(clampedValue) / MAX_BONUS;
    }

    @Override
    public int get(Board board, Move move) {
        return get(move.getFrom(), move.getTo());
    }

    @Override
    public void register(Board board, Move move, int value) {
        register(move.getFrom(), move.getTo(), value);
    }

    @Override
    public void fill(int x) {
        Arrays.fill(history, x);
    }
}
