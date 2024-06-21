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

	public static int moveValue(Move move, Move ttMove, Move killer, Move counterMove, History history, Board board)
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

		return history.get(board, move);
	}

	public static void sortMoves(List<Move> moves, Move ttMove, Move killer, Move counterMove, History history,
			Board board)
	{

		for (int i = 0; i < moves.size(); i++)
		{
			Move move = moves.get(i);
			int value = moveValue(move, ttMove, killer, counterMove, history, board);
			moves.set(i, new ScoredMove(move, value));
		}

		moves.sort(new Comparator<Move>() {
			@Override
			public int compare(Move m1, Move m2)
			{
				if (((ScoredMove) m2).getScore() > ((ScoredMove) m1).getScore())
				{
					return 1;
				}
				
				else if (((ScoredMove) m2).getScore() == ((ScoredMove) m1).getScore())
				{
					return 0;
				}
				
				return -1;
			}
		});
	}

	public static List<Move> sortCaptures(List<Move> moves, Board board)
	{
		moves.sort(new Comparator<Move>() {

			@Override
			public int compare(Move m1, Move m2)
			{
				return pieceValue(board.getPiece(m2.getTo())) * 100 - pieceValue(board.getPiece(m2.getFrom()))
						- (pieceValue(board.getPiece(m1.getTo())) * 100 - pieceValue(board.getPiece(m1.getFrom())));
			}

		});

		return moves;
	}
}
