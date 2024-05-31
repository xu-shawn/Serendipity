package org.shawn.games.Serendipity;

import java.util.*;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

public class MoveSort
{
	private static int pieceValue(Piece p)
	{
		if (p.getPieceType() == null)
		{
			return 0;
		}

		return p.getPieceType().ordinal() + 1;
	}

	public static int moveValue(Move move, Move ttMove, Move killer, Move counterMove, History history,
			History captureHistory, Board board)
	{
		if (move.equals(ttMove))
		{
			return Integer.MAX_VALUE;
		}

		if (!move.getPromotion().equals(Piece.NONE))
		{
			return switch (move.getPromotion().getPieceType())
			{
				case QUEEN -> 2000000001;
				case KNIGHT -> 2000000000;
				default -> -2000000001;
			};
		}

		if (!board.getPiece(move.getTo()).equals(Piece.NONE)
				|| (PieceType.PAWN.equals(board.getPiece(move.getFrom()).getPieceType())
						&& move.getTo() == board.getEnPassant()))
		{
			int score = SEE.staticExchangeEvaluation(board, move, -20) ? 900000000 : -1000000;
			score += pieceValue(board.getPiece(move.getTo())) * 100000 + captureHistory.get(board, move);
			return score;
		}

		if (move.equals(killer))
		{
			return 800000000;
		}

		if (move.equals(counterMove))
		{
			return 700000000;
		}

		return history.get(board, move);
	}

	public static void sortMoves(List<Move> moves, Move ttMove, Move killer, Move counterMove, History history,
			History captureHistory, Board board)
	{
		moves.sort(new Comparator<Move>() {
			@Override
			public int compare(Move m1, Move m2)
			{
				return Integer.compare(moveValue(m2, ttMove, killer, counterMove, history, captureHistory, board),
						moveValue(m1, ttMove, killer, counterMove, history, captureHistory, board));
			}
		});
	}
}
