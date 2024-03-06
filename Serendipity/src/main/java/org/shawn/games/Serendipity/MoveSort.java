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

	public static void sortMoves(List<Move> moves, Move ttMove, Move killer, Move counterMove, int[][] history,
			Board board)
	{
		HashMap<Move, Integer> moveScore = new HashMap<>();

		for (Move move : moves)
		{
			if (move.equals(ttMove))
			{
				moveScore.put(move, Integer.MAX_VALUE / 2);
				continue;
			}

			if (!move.getPromotion().equals(Piece.NONE))
			{
				moveScore.put(move, switch (move.getPromotion().getPieceType())
				{
					case QUEEN -> 2000000001;
					case KNIGHT -> 2000000000;
					default -> -2000000001;
				});

				continue;
			}

			if (!board.getPiece(move.getTo()).equals(Piece.NONE))
			{
				int score = SEE.staticExchangeEvaluation(board, move, -20) ? 900000000 : -1000000;
				score += pieceValue(board.getPiece(move.getTo())) * 100000 - pieceValue(board.getPiece(move.getFrom()));
				moveScore.put(move, score);
				continue;
			}

			if (move.equals(killer))
			{
				moveScore.put(move, 800000000);
				continue;
			}
			
			if (move.equals(counterMove))
			{
				moveScore.put(move, 700000000);
			}

			moveScore.put(move, history[board.getPiece(move.getFrom()).ordinal()][move.getTo().ordinal()]);
		}

		moves.sort(new Comparator<Move>() {
			@Override
			public int compare(Move m1, Move m2)
			{
				return moveScore.get(m2) - moveScore.get(m1);
			}
		});
	}
}
