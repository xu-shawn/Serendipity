package org.shawn.games.Serendipity;

import java.util.*;

import org.shawn.games.Serendipity.TranspositionTable.NodeType;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

public class AlphaBeta
{
	private final int PAWN_VALUE = 100;
	private final int KNIGHT_VALUE = 300;
	private final int BISHOP_VALUE = 300;
	private final int ROOK_VALUE = 500;
	private final int QUEEN_VALUE = 900;
	private final int MAX_EVAL = 1000000000;
	private final int MIN_EVAL = -1000000000;
	private final int MATE_EVAL = 500000000;
	private final int DRAW_EVAL = 0;
	
	private final int MAX_PLY = 256;

	private final TranspositionTable tt;

	private int nodesCount;
	private long timeLimit;
	
	private Move[][] pv;

	private class MoveWithScore
	{
		public final Move move;
		public final int score;

		public MoveWithScore(Move move, int score)
		{
			this.move = move;
			this.score = score;
		}
	}

	public AlphaBeta()
	{
		this.tt = new TranspositionTable(8388608);
		this.nodesCount = 0;
		this.pv = new Move[MAX_PLY][MAX_PLY];
	}
	
	private void updatePV(Move move, int ply)
	{
		pv[ply][0] = move;
		System.arraycopy(pv[ply + 1], 0, pv[ply], 1, MAX_PLY - 1);
	}

	private int pieceValue(Piece p)
	{
		if (p.getPieceType() == null)
		{
			return 0;
		}

		if (p.getPieceType().equals(PieceType.PAWN))
		{
			return PAWN_VALUE;
		}

		if (p.getPieceType().equals(PieceType.KNIGHT))
		{
			return KNIGHT_VALUE;
		}

		if (p.getPieceType().equals(PieceType.BISHOP))
		{
			return BISHOP_VALUE;
		}

		if (p.getPieceType().equals(PieceType.ROOK))
		{
			return ROOK_VALUE;
		}

		if (p.getPieceType().equals(PieceType.QUEEN))
		{
			return QUEEN_VALUE;
		}

		return 0;
	}

	public int evaluate(Board board)
	{
		return PeSTO.evaluate(board);
	}

	public boolean isTimeUp()
	{
		return System.nanoTime() > this.timeLimit;
	}

	private List<Move> sortMoves(List<Move> moves, Board board, boolean useTT)
	{
		moves.sort(new Comparator<Move>() {

			@Override
			public int compare(Move m1, Move m2)
			{
				int cmp = 0;

				cmp += pieceValue(board.getPiece(m2.getTo()))
						- pieceValue(board.getPiece(m2.getFrom()))
						- (pieceValue(board.getPiece(m1.getTo()))
								- pieceValue(board.getPiece(m1.getFrom())));
				if (useTT)
				{
					board.doMove(m2);
					cmp += (tt.probe(board.getIncrementalHashKey()) != null
							&& tt.probe(board.getIncrementalHashKey())
									.getType() == TranspositionTable.NodeType.EXACT) ? 10000 : 0;
					board.undoMove();

					board.doMove(m1);
					cmp -= (tt.probe(board.getIncrementalHashKey()) != null
							&& tt.probe(board.getIncrementalHashKey())
									.getType() == TranspositionTable.NodeType.EXACT) ? 10000 : 0;
					board.undoMove();
				}

				return cmp;
			}

		});
		return moves;
	}

	private int quiesce(Board board, int alpha, int beta, int ply) throws TimeOutException
	{
		this.nodesCount++;

		if (board.isMated())
		{
			return -MATE_EVAL + ply;
		}

		if (board.isDraw())
		{
			return -DRAW_EVAL;
		}

		int standPat = evaluate(board);

		alpha = Math.max(alpha, standPat);

		if (alpha >= beta)
		{
			return beta;
		}

		final List<Move> pseudoLegalCaptures = board.pseudoLegalCaptures();

		sortMoves(pseudoLegalCaptures, board, false);

		for (Move move : pseudoLegalCaptures)
		{
			if (!board.isMoveLegal(move, true))
			{
				continue;
			}

			board.doMove(move);

			int score = -quiesce(board, -beta, -alpha, ply + 1);

			board.undoMove();

			alpha = Math.max(alpha, score);

			if (alpha >= beta)
			{
				return beta;
			}
		}

		return alpha;
	}

	private int mainSearch(Board board, int depth, int alpha, int beta, int ply, boolean nullAllowed)
			throws TimeOutException
	{
		this.nodesCount++;
		this.pv[ply][0] = null;

		if ((nodesCount & 1023) == 0 && isTimeUp())
		{
			throw new TimeOutException();
		}

		final List<Move> legalMoves = board.legalMoves();

		if (legalMoves.isEmpty())
		{
			if(board.isKingAttacked())
			{
				return -MATE_EVAL + ply;
			}
			else
			{
				return -DRAW_EVAL;
			}
		}

		if (depth <= 0 || ply >= MAX_PLY)
		{
			return quiesce(board, alpha, beta, ply + 1);
		}

		TranspositionTable.Entry currentMoveEntry = tt.probe(board.getIncrementalHashKey());

		if (currentMoveEntry != null && currentMoveEntry.getDepth() >= depth
				&& currentMoveEntry.getSignature() == board.getIncrementalHashKey())
		{
			int eval = currentMoveEntry.getEvaluation();
			switch (currentMoveEntry.getType())
			{
				case EXACT:
					return eval;
				case UPPERBOUND:
					if (eval <= alpha)
					{
						return eval;
					}
					break;
				case LOWERBOUND:
					if (eval > beta)
					{
						return eval;
					}
					break;
				default:
					throw new IllegalArgumentException(
							"Unexpected value: " + currentMoveEntry.getType());
			}
		}

		if (nullAllowed && beta < MATE_EVAL - 1024 && !board.isKingAttacked()
				&& (board.getBitboard(Piece.make(board.getSideToMove(), PieceType.KING)) | board
						.getBitboard(Piece.make(board.getSideToMove(), PieceType.PAWN))) != board
						.getBitboard(board.getSideToMove()) && PeSTO.evaluate(board) >= beta)
		{
			board.doNullMove();
			int nullEval = -mainSearch(board, depth - 3, -beta, -beta + 1, ply + 1, false);
			board.undoMove();
			
			if(nullEval >= beta)
			{
				tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.LOWERBOUND,
						depth, nullEval);
				return nullEval;
			}
		}

		int oldAlpha = alpha;

		sortMoves(legalMoves, board, true);

		for (Move move : legalMoves)
		{
			int newdepth = depth - 1;
			
			board.doMove(move);

			int thisMoveEval = -mainSearch(board, newdepth, -beta, -alpha, ply + 1, true);

			board.undoMove();

			if(thisMoveEval > alpha)
			{
				alpha = thisMoveEval;
				
				if (alpha >= beta)
				{
					tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.LOWERBOUND,
							depth, alpha);
					return beta;
				}
				
				updatePV(move, ply);
			}
		}

		if (alpha == oldAlpha)
		{
			tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.UPPERBOUND, depth,
					alpha);
		}

		else if (alpha > oldAlpha)
		{
			tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.EXACT, depth,
					alpha);
		}

		return alpha;
	}

	public Move nextMove(Board board, int targetDepth, long msLeft)
	{
		int currentScore;
		Move[] lastCompletePV = null;
		this.nodesCount = 0;
		long startTime = System.nanoTime();
		this.timeLimit = System.nanoTime() + msLeft * 1000000L;

		try
		{
			for (int i = 1; i <= targetDepth; i++)
			{
				currentScore = mainSearch(board, i, MIN_EVAL, MAX_EVAL, 0, true);
				lastCompletePV = pv[0].clone();
				UCI.report(i, nodesCount, currentScore / PeSTO.MAX_PHASE,
						(System.nanoTime() - startTime) / 1000000, lastCompletePV);
			}
		}

		catch (TimeOutException e) {}

		UCI.reportBestMove(lastCompletePV[0]);
		return lastCompletePV[0];
	}
}
