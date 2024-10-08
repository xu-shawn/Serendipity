package org.shawn.games.Serendipity.Search.History;

import java.util.Arrays;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

public class ContinuationHistories
{
	private final PieceToHistory[][] continuationHistories;

	public ContinuationHistories()
	{
		continuationHistories = new PieceToHistory[Piece.values().length][Square.values().length];

		for (int i = 0; i < Piece.values().length; i++)
		{
			for (int j = 0; j < Square.values().length; j++)
			{
				continuationHistories[i][j] = new PieceToHistory();
			}
		}
	}

	public History get(Piece piece, Square to)
	{
		return continuationHistories[piece.ordinal()][to.ordinal()];
	}

	public History get(Board board, Move move)
	{
		return get(board.getPiece(move.getFrom()), move.getTo());
	}

	public void fill(int x)
	{
		for (PieceToHistory[] row : continuationHistories)
		{
			Arrays.stream(row).forEach(history -> history.fill(x));
		}
	}
}
