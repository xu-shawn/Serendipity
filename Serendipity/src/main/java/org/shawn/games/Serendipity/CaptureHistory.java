package org.shawn.games.Serendipity;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;

public class CaptureHistory implements History
{
	private int[][][] history;

	static final int MAX_BONUS = 16384;

	public CaptureHistory()
	{
		history = new int[Piece.values().length][Square.values().length][PieceType.values().length];
	}

	public int get(Piece piece, Square to, PieceType captured)
	{
		return history[piece.ordinal()][to.ordinal()][captured.ordinal()];
	}

	private static int clamp(int v, int max, int min)
	{
		return v >= max ? max : (v <= min ? min : v);
	}

	public void register(Piece piece, Square to, PieceType captured, int value)
	{
		int clampedValue = clamp(value, MAX_BONUS, -MAX_BONUS);

		history[piece.ordinal()][to.ordinal()][captured.ordinal()] += clampedValue
				- history[piece.ordinal()][to.ordinal()][captured.ordinal()] * Math.abs(clampedValue) / MAX_BONUS;
	}

	@Override
	public int get(Board board, Move move)
	{
		if (move.getTo().equals(board.getEnPassant()))
		{
			return get(board.getPiece(move.getFrom()), move.getTo(), PieceType.PAWN);
		}

		else if (board.getPiece(move.getTo()) == Piece.NONE)
		{
			return get(board.getPiece(move.getFrom()), move.getTo(), PieceType.NONE);
		}
		
		return get(board.getPiece(move.getFrom()), move.getTo(), board.getPiece(move.getTo()).getPieceType());
	}

	@Override
	public void register(Board board, Move move, int value)
	{
		if (move.getTo().equals(board.getEnPassant()))
		{
			register(board.getPiece(move.getFrom()), move.getTo(), PieceType.PAWN, value);
			return;
		}

		else if (board.getPiece(move.getTo()) == Piece.NONE)
		{
			register(board.getPiece(move.getFrom()), move.getTo(), PieceType.NONE, value);
			return;
		}

		register(board.getPiece(move.getFrom()), move.getTo(), board.getPiece(move.getTo()).getPieceType(), value);
	}
}
