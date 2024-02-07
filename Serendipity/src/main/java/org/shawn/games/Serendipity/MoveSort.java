package org.shawn.games.Serendipity;

import java.util.*;

import com.github.bhlangonijr.chesslib.move.*;
import com.github.bhlangonijr.chesslib.*;

public class MoveSort
{
	public static Move sortMoves(List<Move> moves, Board board, TranspositionTable tt, Move[] killers,
			Move[][] counterMoves, int[][] history, int ply)
	{
		if (moves.size() == 0)
		{
			return null;
		}

		List<ScoredMove> scoredMoves = new LinkedList<ScoredMove>();
		MoveBackup lastMove = board.getBackup().peekLast();
		Move killer = killers[ply];
		Move counterMove = lastMove != null
				? counterMoves[board.getPiece(lastMove.getMove().getTo()).ordinal()][lastMove.getMove().getTo()
						.ordinal()]
				: null;

		int bestEval = Integer.MIN_VALUE;
		Move ttMove = null;

		for (Move move : moves)
		{
			board.doMove(move);
			TranspositionTable.Entry ttResult = tt.probe(board.getIncrementalHashKey());
			long boardSignature = board.getIncrementalHashKey();
			board.undoMove();

			if (ttResult != null && ttResult.getSignature() == boardSignature
					&& (ttResult.getType() == TranspositionTable.NodeType.EXACT
							|| ttResult.getType() == TranspositionTable.NodeType.UPPERBOUND)
					&& ttResult.getEvaluation() > bestEval)
			{
				ttMove = move;
				bestEval = ttResult.getEvaluation();
			}

			insert(scoredMoves, new ScoredMove(move, moveScore(move, board, tt, killer, counterMove, history, ply)));
		}

		moves.clear();

		if (ttMove != null)
		{
			moves.add(ttMove);
		}

		for (ScoredMove move : scoredMoves)
		{
			if (move.getMove().equals(ttMove))
			{
				continue;
			}
			
			moves.add(move.getMove());
		}

		return scoredMoves.get(0).getScore() > 90000000 ? moves.get(0) : null;
	}

	private static int moveScore(Move move, Board board, TranspositionTable tt, Move killer, Move counterMove,
			int[][] history, int ply)
	{

//		else if (!move.getPromotion().equals(Piece.NONE))
//		{
//			promotions.add(move);
//		}

		if (move.equals(killer))
		{
			return 70000000;
		}

		else if (move.equals(counterMove))
		{
			return 69000000;
		}

		else if (board.getPiece(move.getTo()).equals(Piece.NONE) || board.getPiece(move.getTo()) == null)
		{
			return history[board.getPiece(move.getTo()).ordinal()][move.getTo().ordinal()];
		}

		else
		{
			return 80000000 + AlphaBeta.pieceValue(board.getPiece(move.getTo()))
							- AlphaBeta.pieceValue(board.getPiece(move.getFrom()));
		}
	}

	private static void insert(List<ScoredMove> scoredMoves, ScoredMove move)
	{
		int targetIndex = 0;

		for (ScoredMove curMove : scoredMoves)
		{
			if (curMove.getScore() <= move.getScore())
			{
				break;
			}
			targetIndex++;
		}

		scoredMoves.add(targetIndex, move);
	}
}
