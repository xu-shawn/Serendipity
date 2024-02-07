package org.shawn.games.Serendipity;

import java.util.*;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

public class AlphaBeta
{
	private final static int PAWN_VALUE = 82;
	private final static int KNIGHT_VALUE = 337;
	private final static int BISHOP_VALUE = 365;
	private final static int ROOK_VALUE = 477;
	private final static int QUEEN_VALUE = 1025;
	private final static int MAX_EVAL = 1000000000;
	private final static int MIN_EVAL = -1000000000;
	private final static int MATE_EVAL = 1000000;
	private final static int DRAW_EVAL = 0;

	private final static int MAX_PLY = 256;
	private final static int ASPIRATION_DELTA = 600;

	private final TranspositionTable tt;

	private int nodesCount;
	private long timeLimit;

	private Move[][] pv;
	private Move[] killers;
	private Move[][] counterMoves;
	private int[][] history;

	private int rootDepth;
	private int selDepth;

	public AlphaBeta()
	{
		this(8);
	}

	public AlphaBeta(int n)
	{
		this.tt = new TranspositionTable(1048576 * n);
		this.nodesCount = 0;
		this.timeLimit = 0;
		this.pv = new Move[MAX_PLY][MAX_PLY];
		this.killers = new Move[MAX_PLY];
		this.counterMoves = new Move[13][65];
		this.history = new int[13][65];
		this.rootDepth = 0;
	}

	private void updatePV(Move move, int ply)
	{
		pv[ply][0] = move;
		System.arraycopy(pv[ply + 1], 0, pv[ply], 1, MAX_PLY - 1);
	}

	private void clearPV()
	{
		this.pv = new Move[MAX_PLY][MAX_PLY];
	}

	public static int pieceValue(Piece p)
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

	private List<Move> sortCaptures(List<Move> moves, Board board)
	{
		moves.sort(new Comparator<Move>() {

			@Override
			public int compare(Move m1, Move m2)
			{
				return pieceValue(board.getPiece(m2.getTo())) - pieceValue(board.getPiece(m2.getFrom()))

						- (pieceValue(board.getPiece(m1.getTo())) - pieceValue(board.getPiece(m1.getFrom())));
			}

		});

		return moves;
	}

	private int quiesce(Board board, int alpha, int beta, int ply) throws TimeOutException
	{
		this.nodesCount++;

		this.selDepth = Math.max(this.selDepth, ply);

		int bestScore;

		if (board.isRepetition() || board.getHalfMoveCounter() >= 100)
		{
			return DRAW_EVAL;
		}
		
		int futilityBase;
		boolean inCheck = false;
		final List<Move> moves;
		
		if(board.isKingAttacked())
		{
			bestScore = futilityBase = MIN_EVAL;
			moves = board.legalMoves();
			MoveSort.sortMoves(moves, board, tt, killers, counterMoves, history, ply);
			inCheck = true;
		}
		
		else
		{
			int standPat = bestScore = evaluate(board);
	
			alpha = Math.max(alpha, standPat);
	
			if (alpha >= beta)
			{
				return beta;
			}
			
			futilityBase = standPat + 4896;
			moves = board.pseudoLegalCaptures();
			sortCaptures(moves, board);
		}

		for (Move move : moves)
		{
			if (!inCheck && !board.isMoveLegal(move, false))
			{
				continue;
			}

			if (bestScore > -MATE_EVAL + 1024 && futilityBase < alpha && !SEE.staticExchangeEvaluation(board, move, 1)
					&& (board.getBitboard(Piece.make(board.getSideToMove(), PieceType.KING))
							| board.getBitboard(Piece.make(board.getSideToMove(), PieceType.PAWN))) != board
									.getBitboard(board.getSideToMove()))
			{
				bestScore = Math.max(bestScore, futilityBase);
				continue;
			}

			board.doMove(move);

			int score = -quiesce(board, -beta, -alpha, ply + 1);

			board.undoMove();

			bestScore = Math.max(bestScore, score);
			alpha = Math.max(alpha, bestScore);

			if (alpha >= beta)
			{
				break;
			}
		}
		
		if (bestScore == MIN_EVAL && inCheck)
		{
			return -MATE_EVAL + ply;
		}

		return bestScore;
	}

	private int mainSearch(Board board, int depth, int alpha, int beta, int ply, boolean nullAllowed)
			throws TimeOutException
	{
		this.nodesCount++;
		this.pv[ply][0] = null;
		this.killers[ply + 2] = null;
		int moveCount = 0;
		boolean isPV = beta - alpha > 1;
		this.selDepth = Math.max(this.selDepth, ply);

		if ((nodesCount & 1023) == 0 && isTimeUp())
		{
			throw new TimeOutException();
		}

		if ((board.isRepetition(2) && ply > 0) || board.isRepetition(3) || board.getHalfMoveCounter() >= 100)
		{
			return DRAW_EVAL;
		}

		if (depth <= 0 || ply >= MAX_PLY)
		{
			return quiesce(board, alpha, beta, ply + 1);
		}

		TranspositionTable.Entry currentMoveEntry = tt.probe(board.getIncrementalHashKey());

		if ((!isPV || ply > 1) && currentMoveEntry != null && currentMoveEntry.getDepth() >= depth
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
					throw new IllegalArgumentException("Unexpected value: " + currentMoveEntry.getType());
			}
		}

		int staticEval;

		if (currentMoveEntry != null && currentMoveEntry.getSignature() == board.getIncrementalHashKey())
		{
			staticEval = currentMoveEntry.getEvaluation();
		}
		else
		{
			staticEval = PeSTO.evaluate(board);
		}

		if (!isPV && !board.isKingAttacked() && depth < 7 && staticEval > beta && staticEval - depth * 1680 > beta)
		{
			return beta;
		}

		if (nullAllowed && beta < MATE_EVAL - 1024 && !board.isKingAttacked()
				&& (board.getBitboard(Piece.make(board.getSideToMove(), PieceType.KING))
						| board.getBitboard(Piece.make(board.getSideToMove(), PieceType.PAWN))) != board
								.getBitboard(board.getSideToMove())
				&& PeSTO.evaluate(board) >= beta && ply > 0 && staticEval >= beta)
		{
//			int r = depth / 3 + 4;

			board.doNullMove();
			int nullEval = -mainSearch(board, depth - 3, -beta, -beta + 1, ply + 1, false);
			board.undoMove();

			if (nullEval >= beta && nullEval < MATE_EVAL - 1024)
			{
				return nullEval;
			}
		}

		final List<Move> legalMoves = board.legalMoves();

		if (legalMoves.isEmpty())
		{
			if (board.isKingAttacked())
			{
				return -MATE_EVAL + ply;
			}
			else
			{
				return DRAW_EVAL;
			}
		}

		int oldAlpha = alpha;

//		Move ttMove = sortMoves(legalMoves, board, ply);
		Move ttMove = MoveSort.sortMoves(legalMoves, board, tt, killers, counterMoves, history, ply);

		if (isPV && ttMove == null && rootDepth > 2 && depth > 5)
		{
			depth -= 2;
		}

		for (Move move : legalMoves)
		{
			moveCount++;
			int newdepth = depth - 1;
			boolean isQuiet = Piece.NONE.equals(move.getPromotion()) && Piece.NONE.equals(board.getPiece(move.getTo()));

			if (alpha > -MATE_EVAL + 1024 && depth < 8
					&& !SEE.staticExchangeEvaluation(board, move, isQuiet ? -64 * depth : -20 * depth * depth))
			{
				continue;
			}

			board.doMove(move);

			boolean inCheck = board.isKingAttacked();

			if (inCheck)
			{
				newdepth++;
			}

			int thisMoveEval = MIN_EVAL;

			if (moveCount > 3 && depth > 3)
			{
				int r = (int) (1.35 + Math.log(depth) * Math.log(moveCount) / 2.75);

//				r += isPV ? 0 : 1;
				r -= inCheck ? 1 : 0;
//
//				r = Math.max(0, Math.min(depth - 1, r));

				thisMoveEval = -mainSearch(board, depth - r, -(alpha + 1), -alpha, ply + 1, true);

				if (thisMoveEval > alpha)
				{
					thisMoveEval = -mainSearch(board, newdepth, -(alpha + 1), -alpha, ply + 1, true);
				}
			}

			else if (!isPV || moveCount > 1)
			{
				thisMoveEval = -mainSearch(board, newdepth, -(alpha + 1), -alpha, ply + 1, true);
			}

			if (isPV && (moveCount == 1 || thisMoveEval > alpha))
			{
				thisMoveEval = -mainSearch(board, newdepth, -beta, -alpha, ply + 1, true);
				updatePV(move, ply);
			}

			board.undoMove();

			if (thisMoveEval > alpha)
			{
				alpha = thisMoveEval;

				if (alpha >= beta)
				{
					tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.LOWERBOUND, depth, alpha);
					if (move.getPromotion().equals(Piece.NONE) && board.getPiece(move.getTo()).equals(Piece.NONE))
					{
						killers[ply] = move;
						history[board.getPiece(move.getTo()).ordinal()][move.getTo().ordinal()] += depth * depth;

						if (!board.getBackup().isEmpty())
						{
							Move lastMove = board.getBackup().peekLast().getMove();
							counterMoves[board.getPiece(lastMove.getTo()).ordinal()][lastMove.getTo().ordinal()] = move;
						}
					}

					return beta;
				}
			}
		}

		if (alpha == oldAlpha)
		{
			tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.UPPERBOUND, depth, alpha);
		}

		else if (alpha > oldAlpha)
		{
			tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.EXACT, depth, alpha);
		}

		return alpha;
	}

	public Move nextMove(Board board, int targetDepth, long msLeft)
	{
		return nextMove(board, targetDepth, msLeft, false);
	}

	public Move nextMove(Board board, int targetDepth, long msLeft, boolean supressOutput)
	{
		int currentScore = MIN_EVAL;
		killers = new Move[MAX_PLY];
		counterMoves = new Move[13][65];
		clearPV();
		Move[] lastCompletePV = null;
		this.nodesCount = 0;
		long startTime = System.nanoTime();
		this.timeLimit = System.nanoTime() + msLeft * 1000000L;
		this.history = new int[13][65];

		try
		{
			for (int i = 1; i <= targetDepth; i++)
			{
				rootDepth = i;
				selDepth = 0;
				if (i > 3)
				{
					int newScore = mainSearch(board, i, currentScore - ASPIRATION_DELTA,
							currentScore + ASPIRATION_DELTA, 0, false);
					if (newScore > currentScore - ASPIRATION_DELTA && newScore < currentScore + ASPIRATION_DELTA)
					{
						currentScore = newScore;
						lastCompletePV = pv[0].clone();
						if (!supressOutput)
						{
							UCI.report(i, selDepth, nodesCount, currentScore / PeSTO.MAX_PHASE,
									(System.nanoTime() - startTime) / 1000000, lastCompletePV);
						}
						continue;
					}
				}

				currentScore = mainSearch(board, i, MIN_EVAL, MAX_EVAL, 0, false);

				lastCompletePV = pv[0].clone();

				if (!supressOutput)
				{
					UCI.report(i, selDepth, nodesCount, currentScore / PeSTO.MAX_PHASE,
							(System.nanoTime() - startTime) / 1000000, lastCompletePV);
				}
			}
		}

		catch (TimeOutException e)
		{
		}

		if (!supressOutput)
		{
			UCI.reportBestMove(lastCompletePV[0]);
		}

		return lastCompletePV[0];
	}

	public int getNodesCount()
	{
		return this.nodesCount;
	}

	public void reset()
	{
		this.tt.clear();
		this.nodesCount = 0;
		this.timeLimit = 0;
		this.pv = new Move[MAX_PLY][MAX_PLY];
		this.killers = new Move[MAX_PLY];
		this.counterMoves = new Move[13][65];
		this.history = new int[13][65];
		this.rootDepth = 0;
		this.selDepth = 0;
	}
}
