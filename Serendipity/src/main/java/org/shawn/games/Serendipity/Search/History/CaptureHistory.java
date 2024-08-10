package org.shawn.games.Serendipity.Search.History;

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
		
		if (captured == null)
		{
			captured = PieceType.NONE;
		}

		history[piece.ordinal()][to.ordinal()][captured.ordinal()] += clampedValue
				- history[piece.ordinal()][to.ordinal()][captured.ordinal()] * Math.abs(clampedValue) / MAX_BONUS;
	}

	@Override
	public int get(Board board, Move move)
	{
		Piece movedPiece = board.getPiece(move.getFrom());

		if (move.getTo().equals(board.getEnPassant()) && movedPiece.getPieceType().equals(PieceType.PAWN))
		{
			return get(movedPiece, board.getEnPassant(), PieceType.PAWN);
		}

		return get(movedPiece, move.getTo(), board.getPiece(move.getTo()).getPieceType());
	}

	@Override
	public void register(Board board, Move move, int value)
	{
		Piece movedPiece = board.getPiece(move.getFrom());

		if (move.getTo().equals(board.getEnPassant()) && movedPiece.getPieceType().equals(PieceType.PAWN))
		{
			register(movedPiece, board.getEnPassant(), PieceType.PAWN, value);
			return;
		}

		register(movedPiece, move.getTo(), board.getPiece(move.getTo()).getPieceType(), value);
	}
}