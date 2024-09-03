package org.shawn.games.Serendipity.Search.History;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.Arrays;

public class CaptureHistory implements History {
    private final int[][][] history;

    static final int MAX_BONUS = 16384;

    public CaptureHistory() {
        history = new int[Piece.values().length][Square.values().length][PieceType.values().length];
    }

    public int get(Piece piece, Square to, PieceType captured) {
        return history[piece.ordinal()][to.ordinal()][captured.ordinal()];
    }

    private static int clamp(int v) {
        return v >= CaptureHistory.MAX_BONUS ? CaptureHistory.MAX_BONUS : (Math.max(v, -16384));
    }

    public void register(Piece piece, Square to, PieceType captured, int value) {
        int clampedValue = clamp(value);

        if (captured == null) {
            captured = PieceType.NONE;
        }

        history[piece.ordinal()][to.ordinal()][captured.ordinal()] += clampedValue
                - history[piece.ordinal()][to.ordinal()][captured.ordinal()] * Math.abs(clampedValue) / MAX_BONUS;
    }

    @Override
    public int get(Board board, Move move) {
        Piece movedPiece = board.getPiece(move.getFrom());

        if (move.getTo().equals(board.getEnPassant()) && movedPiece.getPieceType().equals(PieceType.PAWN)) {
            return get(movedPiece, board.getEnPassant(), PieceType.PAWN);
        }

        return get(movedPiece, move.getTo(), board.getPiece(move.getTo()).getPieceType());
    }

    @Override
    public void register(Board board, Move move, int value) {
        Piece movedPiece = board.getPiece(move.getFrom());

        if (move.getTo().equals(board.getEnPassant()) && movedPiece.getPieceType().equals(PieceType.PAWN)) {
            register(movedPiece, board.getEnPassant(), PieceType.PAWN, value);
            return;
        }

        register(movedPiece, move.getTo(), board.getPiece(move.getTo()).getPieceType(), value);
    }

    @Override
    public void fill(int x) {
        for (int[][] x1 : history) {
            for (int[] x2 : x1) {
                Arrays.fill(x2, x);
            }
        }
    }
}