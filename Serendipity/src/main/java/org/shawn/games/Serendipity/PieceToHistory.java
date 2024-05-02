package org.shawn.games.Serendipity;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;

public class PieceToHistory implements History
{
	private int[][] history;

	static final int MAX_BONUS = 16384;

	public PieceToHistory()
	{
		history = new int[Piece.values().length][Square.values().length];
	}

	public int get(Piece piece, Square to)
	{
		return history[piece.ordinal()][to.ordinal()];
	}

	public int get(Piece piece, Move move)
	{
		return get(piece, move.getTo());
	}

	private static int clamp(int v, int max, int min)
	{
		return v >= max ? max : (v <= min ? min : v);
	}

	public void register(Piece piece, Square to, int value)
	{
		int clampedValue = clamp(value, MAX_BONUS, -MAX_BONUS);

		history[piece.ordinal()][to.ordinal()] += clampedValue
				- history[piece.ordinal()][to.ordinal()] * Math.abs(clampedValue) / MAX_BONUS;
	}

	@Override
	public int get(Board board, Move move)
	{
		return get(board.getPiece(move.getFrom()), move.getTo());
	}

	@Override
	public void register(Board board, Move move, int value)
	{
		register(board.getPiece(move.getFrom()), move.getTo(), value);
	}
}
