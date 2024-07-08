package org.shawn.games.Serendipity;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

public class ContinuationHistories
{
	private PieceToHistory[][] continuationHistories;

	public ContinuationHistories()
	{
		continuationHistories = new PieceToHistory[Piece.values().length][Square.values().length];
	}

	public History get(Piece piece, Square to)
	{
		return continuationHistories[piece.ordinal()][to.ordinal()];
	}

	public History get(Board board, Move move)
	{
		return get(board.getPiece(move.getFrom()), move.getTo());
	}
}
