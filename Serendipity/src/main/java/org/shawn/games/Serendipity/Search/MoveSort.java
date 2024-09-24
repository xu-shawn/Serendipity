package org.shawn.games.Serendipity.Search;

import java.util.*;

import org.shawn.games.Serendipity.Search.History.History;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

public class MoveSort
{
	private static final int[] promoValue = { -2000000001, 2000000000, -2000000001, -2000000001, 2000000001 };

	private static int pieceValue(Piece p)
	{
		if (p.getPieceType() == null)
		{
			return 0;
		}

		return p.getPieceType().ordinal() + 1;
	}

	private static int captureValue(Move move, Board board, History captureHistory)
	{
		return pieceValue(board.getPiece(move.getTo())) * 100 + captureHistory.get(board, move) / 128;
	}

	private static int qSearchValue(Move move, Board board, History captureHistory)
	{
		return captureValue(move, board, captureHistory);
	}

	public static int moveValue(Move move, Move ttMove, Move killer, History history, History captureHistory,
			History[] continuationHistories, Board board)
	{
		if (move.equals(ttMove))
		{
			return Integer.MAX_VALUE;
		}

		if (!move.getPromotion().equals(Piece.NONE))
		{
			return promoValue[move.getPromotion().getPieceType().ordinal()];
		}

		if (!AlphaBeta.isQuiet(move, board))
		{
			int score = SEE.staticExchangeEvaluation(board, move, -20) ? 900000000 : -1000000;
			score += captureValue(move, board, captureHistory);
			return score;
		}

		if (move.equals(killer))
		{
			return 800000000;
		}

		int moveValue = history.get(board, move);

		moveValue += continuationHistories[0].get(board, move);
		moveValue += continuationHistories[1].get(board, move);
		moveValue += continuationHistories[3].get(board, move);
		moveValue += continuationHistories[5].get(board, move) / 2;

		return moveValue;
	}

	public static void sortMoves(List<Move> moves, Move ttMove, Move killer, History history, History captureHistory,
			History[] continuationHistories, Board board)
	{

		for (int i = 0; i < moves.size(); i++)
		{
			Move move = moves.get(i);
			int value = moveValue(move, ttMove, killer, history, captureHistory, continuationHistories, board);
			moves.set(i, new ScoredMove(move, value));
		}

		moves.sort((m1, m2) -> {
			if (((ScoredMove) m2).getScore() > ((ScoredMove) m1).getScore())
			{
				return 1;
			}

			else if (((ScoredMove) m2).getScore() == ((ScoredMove) m1).getScore())
			{
				return 0;
			}

			return -1;
		});
	}

	public static void sortCaptures(List<Move> moves, Move ttMove, Board board, History captureHistory)
	{
		for (int i = 0; i < moves.size(); i++)
		{
			Move move = moves.get(i);
			int value = (move.equals(ttMove)) ? 2000000000 : qSearchValue(move, board, captureHistory);
			moves.set(i, new ScoredMove(move, value));
		}

		moves.sort((m1, m2) -> ((ScoredMove) m2).getScore() - ((ScoredMove) m1).getScore());

	}
}
