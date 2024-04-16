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

		if (p.getPieceType().equals(PieceType.PAWN))
		{
			return 1;
		}

		if (p.getPieceType().equals(PieceType.KNIGHT))
		{
			return 2;
		}

		if (p.getPieceType().equals(PieceType.BISHOP))
		{
			return 3;
		}

		if (p.getPieceType().equals(PieceType.ROOK))
		{
			return 4;
		}

		if (p.getPieceType().equals(PieceType.QUEEN))
		{
			return 5;
		}

		return 0;
	}

	public static int moveValue(Move move, Move ttMove, Move killer, Move counterMove, int[][] history, Board board)
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
			int score = SEE.staticExchangeEvaluation(board, move, -1) ? 900000000 : -1000000;
			score += pieceValue(board.getPiece(move.getTo())) * 100000 - pieceValue(board.getPiece(move.getFrom()));
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

		return history[board.getPiece(move.getFrom()).ordinal()][move.getTo().ordinal()];
	}

	public static void sortMoves(List<Move> moves, Move ttMove, Move killer, Move counterMove, int[][] history,
			Board board)
	{
		moves.sort(new Comparator<Move>() {
			@Override
			public int compare(Move m1, Move m2)
			{
				return Integer.compare(moveValue(m2, ttMove, killer, counterMove, history, board),
						moveValue(m1, ttMove, killer, counterMove, history, board));
			}
		});
	}
}
