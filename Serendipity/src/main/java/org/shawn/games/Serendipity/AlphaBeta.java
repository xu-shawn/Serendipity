package org.shawn.games.Serendipity;

import java.util.*;

import org.shawn.games.Serendipity.TranspositionTable.NodeType;
import org.shawn.games.Serendipity.NNUE.*;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

public class AlphaBeta
{
	private static final int PAWN_VALUE = 82;
	private static final int KNIGHT_VALUE = 337;
	private static final int BISHOP_VALUE = 365;
	private static final int ROOK_VALUE = 477;
	private static final int QUEEN_VALUE = 1025;

	public static final int VALUE_NONE = 30002;
	public static final int MAX_EVAL = 32767;
	public static final int MIN_EVAL = -32767;
	public static final int MATE_EVAL = 32700;
	public static final int DRAW_EVAL = 0;

	public static final int MAX_PLY = 256;

	private final TranspositionTable tt;

	private TimeManager timeManager;

	private int nodesCount;
	private int nodesLimit;

	private Move[][] pv;
	private Move[] lastCompletePV;
	private Move[][] counterMoves;
	private History history;
	private History captureHistory;
	private int nmpMinPly;

	private int rootDepth;
	private int selDepth;

	private NNUE network;
	private AccumulatorManager accumulators;

	private SearchStack ss;

	private Board internalBoard;
	private Limits limits;

	public AlphaBeta(NNUE network)
	{
		this(1, network);
	}

	public AlphaBeta(int n, NNUE network)
	{
		this.tt = new TranspositionTable(1048576 * n);
		this.nodesCount = 0;
		this.nodesLimit = -1;
		this.pv = new Move[MAX_PLY][MAX_PLY];
		this.counterMoves = new Move[13][65];
		this.history = new FromToHistory();
		this.captureHistory = new CaptureHistory();
		this.rootDepth = 0;
		this.ss = new SearchStack(MAX_PLY);

		this.network = network;
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

	private int stat_bonus(int depth)
	{
		return depth * 300 - 300;
	}

	private int stat_malus(int depth)
	{
		return -stat_bonus(depth);
	}

	public int evaluate(Board board)
	{
		int v = (Side.WHITE.equals(board.getSideToMove())
				? NNUE.evaluate(network, accumulators.getWhiteAccumulator(), accumulators.getBlackAccumulator(),
						NNUE.chooseOutputBucket(board))
				: NNUE.evaluate(network, accumulators.getBlackAccumulator(), accumulators.getWhiteAccumulator(),
						NNUE.chooseOutputBucket(board)));

		return v;
	}

	private void sortMoves(List<Move> moves, Board board, int ply)
	{
		TranspositionTable.Entry currentMoveEntry = tt.probe(board.getIncrementalHashKey());

		Move ttMove = currentMoveEntry == null ? null : currentMoveEntry.getMove();
		MoveSort.sortMoves(moves, ttMove, null, null, history, captureHistory, board);
	}

	private int quiesce(Board board, int alpha, int beta, int ply) throws TimeOutException
	{
		this.nodesCount++;

		this.selDepth = Math.max(this.selDepth, ply);

		int bestScore;

		if (board.isRepetition() || board.getHalfMoveCounter() > 100)
		{
			return DRAW_EVAL;
		}

		boolean isPV = beta - alpha > 1;

		TranspositionTable.Entry currentMoveEntry = tt.probe(board.getIncrementalHashKey());

		if (!isPV && currentMoveEntry != null && currentMoveEntry.verifySignature(board.getIncrementalHashKey()))
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

		if (ply >= MAX_PLY)
		{
			return evaluate(board);
		}

		int futilityBase;
		boolean inCheck = ss.get(ply).inCheck = board.isKingAttacked();
		final List<Move> moves;

		if (inCheck)
		{
			bestScore = futilityBase = MIN_EVAL;
			moves = board.legalMoves();
			sortMoves(moves, board, ply);
		}

		else
		{
			int standPat = bestScore = evaluate(board);

			alpha = Math.max(alpha, standPat);

			if (alpha >= beta)
			{
				return alpha;
			}

			futilityBase = standPat + 205;
			moves = board.pseudoLegalCaptures();
			MoveSort.sortCaptures(moves, board, captureHistory);
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

			if (!inCheck && !SEE.staticExchangeEvaluation(board, move, -20))
			{
				continue;
			}

			accumulators.updateAccumulators(board, move, false);
			board.doMove(move);

			int score = -quiesce(board, -beta, -alpha, ply + 1);

			board.undoMove();
			accumulators.updateAccumulators(board, move, true);

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
		this.ss.get(ply + 2).killer = null;
		this.selDepth = Math.max(this.selDepth, ply);

		boolean improving, isPV, inCheck, givesCheck, inSingularSearch;
		Move bestMove;
		int bestValue;
		int eval;

		var sse = ss.get(ply);

		bestValue = MIN_EVAL;
		bestMove = null;
		sse.moveCount = 0;
		isPV = beta - alpha > 1;
		inCheck = sse.inCheck = board.isKingAttacked();
		inSingularSearch = sse.excludedMove != null;

		if ((nodesCount & 1023) == 0 && (timeManager.stop() || (nodesLimit > 0 && nodesCount > nodesLimit)))
		{
			throw new TimeOutException();
		}

		if (ply > 0 && (board.isRepetition(2) || board.getHalfMoveCounter() > 100 || board.isInsufficientMaterial()))
		{
			return DRAW_EVAL;
		}

		if (ply > 0)
		{
			alpha = Math.max(alpha, -MATE_EVAL + ply);
			beta = Math.min(beta, MATE_EVAL - ply - 1);

			if (alpha >= beta)
			{
				return alpha;
			}
		}

		if (depth <= 0 || ply >= MAX_PLY)
		{
			this.nodesCount--;
			return quiesce(board, alpha, beta, ply + 1);
		}

		TranspositionTable.Entry currentMoveEntry = tt.probe(board.getIncrementalHashKey());

		sse.ttHit = currentMoveEntry != null;

		if (!inSingularSearch && !isPV && currentMoveEntry != null && currentMoveEntry.getDepth() >= depth
				&& currentMoveEntry.verifySignature(board.getIncrementalHashKey()))
		{
			eval = currentMoveEntry.getEvaluation();
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

		if (inCheck)
		{
			eval = sse.staticEval = VALUE_NONE;
		}
		else
		{

			if (currentMoveEntry != null && currentMoveEntry.verifySignature(board.getIncrementalHashKey()))
			{
				sse.staticEval = currentMoveEntry.getStaticEval();
				eval = currentMoveEntry.getEvaluation();
				switch (currentMoveEntry.getType())
				{
					case EXACT:
						break;
					case UPPERBOUND:
						if (eval > sse.staticEval)
						{
							eval = sse.staticEval;
						}
						break;
					case LOWERBOUND:
						if (eval < sse.staticEval)
						{
							eval = sse.staticEval;
						}
						break;
					default:
						throw new IllegalArgumentException("Unexpected value: " + currentMoveEntry.getType());
				}
			}
			else
			{
				eval = sse.staticEval = evaluate(board);
			}
		}

		improving = false;

		if (!inCheck)
		{
			if (ss.get(-2).staticEval != VALUE_NONE)
			{
				improving = ss.get(-2).staticEval < sse.staticEval;
			}

			else if (ss.get(-4).staticEval != VALUE_NONE)
			{
				improving = ss.get(-4).staticEval < sse.staticEval;
			}
			else
			{
				improving = true;
			}
		}

		if (!inSingularSearch && !isPV && !inCheck && depth < 7 && eval > beta && eval - depth * 70 > beta)
		{
			return beta > -MATE_EVAL + 1024 ? beta + (eval - beta) / 3 : eval;
		}

		if (!inSingularSearch && nullAllowed && beta < MATE_EVAL - 1024 && !inCheck
				&& (board.getBitboard(Piece.make(board.getSideToMove(), PieceType.KING))
						| board.getBitboard(Piece.make(board.getSideToMove(), PieceType.PAWN))) != board
								.getBitboard(board.getSideToMove())
				&& eval >= beta && ply > 0)
		{
			int r = depth / 3 + 4 + Math.min((eval - beta) / 200, 3);

			board.doNullMove();
			sse.move = Constants.emptyMove;
			int nullEval = -mainSearch(board, depth - r, -beta, -beta + 1, ply + 1, false);
			board.undoMove();

			if (nullEval >= beta && nullEval < MATE_EVAL - 1024)
			{
				if (this.nmpMinPly != 0 || depth < 12)
				{
					return nullEval;
				}

				this.nmpMinPly = ply + 3 * (depth - r) / 4;

				int v = mainSearch(board, depth - r, -beta, -beta + 1, ply, false);

				this.nmpMinPly = 0;

				if (v > beta)
				{
					return nullEval;
				}
			}
		}

		if (depth <= 5 && eval + 256 * depth < alpha)
		{
			int razorValue = quiesce(board, alpha, alpha + 1, ply);

			if (razorValue <= alpha)
			{
				return razorValue;
			}
		}

		final List<Move> legalMoves = board.legalMoves();

		if (legalMoves.isEmpty())
		{
			if (inCheck)
			{
				return -MATE_EVAL + ply;
			}
			else
			{
				return DRAW_EVAL;
			}
		}

		int oldAlpha = alpha;

		Move ttMove = currentMoveEntry == null ? null : currentMoveEntry.getMove();
		Move lastMove = ss.get(ply - 1).move;
		Move counterMove = null;

		if (lastMove != null)
			counterMove = counterMoves[board.getPiece(lastMove.getFrom()).ordinal()][lastMove.getTo().ordinal()];

		MoveSort.sortMoves(legalMoves, ttMove, sse.killer, counterMove, history, captureHistory, board);

		List<Move> quietsSearched = new ArrayList<>();
		List<Move> capturesSearched = new ArrayList<>();

		if (isPV && ttMove == null && rootDepth > 1 && depth > 5)
		{
			depth -= 2;
		}

		for (Move move : legalMoves)
		{
			if (move.equals(sse.excludedMove))
			{
				continue;
			}

			sse.moveCount++;
			int newdepth = depth - 1;
			board.doMove(move);
			givesCheck = board.isKingAttacked();
			board.undoMove();
			boolean isQuiet = Piece.NONE.equals(move.getPromotion()) && Piece.NONE.equals(board.getPiece(move.getTo()))
					&& !(PieceType.PAWN.equals(board.getPiece(move.getFrom()).getPieceType())
							&& move.getTo() == board.getEnPassant());

			int r = (int) (1.60 + Math.log(depth) * Math.log(sse.moveCount) / 2.17);
			int lmrDepth = depth - r;

			if (isQuiet && !isPV && !givesCheck && sse.moveCount > 3 + depth * depth / (improving ? 1 : 2)
					&& alpha > -MATE_EVAL + 1024)
			{
				continue;
			}

			if (bestValue > -MATE_EVAL + 1024 && ply > 0
					&& (board.getBitboard(Piece.make(board.getSideToMove(), PieceType.KING))
							| board.getBitboard(Piece.make(board.getSideToMove(), PieceType.PAWN))) != board
									.getBitboard(board.getSideToMove()))
			{
				if (!inCheck && isQuiet && lmrDepth <= 8 && sse.staticEval + lmrDepth * 150 + 150 <= alpha)
				{
					continue;
				}

				if (depth < 9
						&& !SEE.staticExchangeEvaluation(board, move, isQuiet ? -65 * depth : -38 * depth * depth))
				{
					continue;
				}
			}

			int extension = 0;

			if (!inSingularSearch && ply > 0 && move.equals(ttMove) && depth >= 4
					&& Math.abs(currentMoveEntry.getEvaluation()) < MATE_EVAL - 1024
					&& (currentMoveEntry.getType().equals(TranspositionTable.NodeType.EXACT)
							|| currentMoveEntry.getType().equals(TranspositionTable.NodeType.LOWERBOUND))
					&& currentMoveEntry.getDepth() > depth - 4)
			{
				int singularBeta = currentMoveEntry.getEvaluation() - 2 * depth;
				int singularDepth = depth / 2;
				int moveCountBackup = sse.moveCount;

				sse.excludedMove = move;
				int singularValue = mainSearch(board, singularDepth, singularBeta - 1, singularBeta, ply, false);
				sse.excludedMove = null;
				sse.moveCount = moveCountBackup;

				if (singularValue < singularBeta)
				{
					extension = 1;
					
					if (!isPV)
					{
						extension = 2;
					}
				}
				
				else if (singularValue > beta)
				{
					return singularValue;
				}

			}

			else if (givesCheck)
			{
				extension = 1;
			}

			newdepth += extension;

			accumulators.updateAccumulators(board, move, false);
			board.doMove(move);
			sse.move = move;

			int thisMoveEval = MIN_EVAL;

			if (sse.moveCount > 3 + (ply == 0 ? 1 : 0) && depth > 2)
			{
				r += isPV ? 0 : 1;
				r -= givesCheck ? 1 : 0;
//
//				r = Math.max(0, Math.min(depth - 1, r));

				thisMoveEval = -mainSearch(board, depth - r, -(alpha + 1), -alpha, ply + 1, true);

				if (thisMoveEval > alpha)
				{
					thisMoveEval = -mainSearch(board, newdepth, -(alpha + 1), -alpha, ply + 1, true);
				}
			}

			else if (!isPV || sse.moveCount > 1)
			{
				thisMoveEval = -mainSearch(board, newdepth, -(alpha + 1), -alpha, ply + 1, true);
			}

			if (isPV && (sse.moveCount == 1 || thisMoveEval > alpha))
			{
				thisMoveEval = -mainSearch(board, newdepth, -beta, -alpha, ply + 1, true);
			}

			board.undoMove();
			accumulators.updateAccumulators(board, move, true);

			if (thisMoveEval > bestValue)
			{
				bestValue = thisMoveEval;
				bestMove = move;

				if (isPV)
					updatePV(move, ply);
			}

			if (thisMoveEval > alpha)
			{
				alpha = thisMoveEval;

				if (alpha >= beta)
				{
					tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.LOWERBOUND, depth, bestValue,
							bestMove, sse.staticEval);

					if (isQuiet)
					{
						sse.killer = move;

						history.register(board, move, stat_bonus(depth));

						for (Move quietMove : quietsSearched)
						{
							history.register(board, quietMove, stat_malus(depth));
						}

						if (lastMove != null)
						{
							counterMoves[board.getPiece(lastMove.getFrom()).ordinal()][lastMove.getTo()
									.ordinal()] = move;
						}
					}
					else
					{
						captureHistory.register(board, move, stat_bonus(depth));

						for (Move capture : capturesSearched)
						{
							captureHistory.register(board, capture, stat_malus(depth));
						}
					}

					return bestValue;
				}
			}

			if (isQuiet)
			{
				quietsSearched.add(move);
			}
			else
			{
				capturesSearched.add(move);
			}
		}

		if (sse.moveCount == 0)
		{
			return -MATE_EVAL + ply;
		}

		if (alpha == oldAlpha)
		{
			tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.UPPERBOUND, depth, bestValue, ttMove,
					sse.staticEval);
		}

		else if (alpha > oldAlpha)
		{
			tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.EXACT, depth, bestValue, bestMove,
					sse.staticEval);
		}

		return bestValue;
	}

	public void iterativeDeepening(boolean suppressOutput)
	{
		int currentScore;
		int alpha, beta;
		int delta;

		currentScore = MIN_EVAL;
		counterMoves = new Move[13][65];
		lastCompletePV = null;
		alpha = MIN_EVAL;
		beta = MAX_EVAL;
		delta = 25;
		clearPV();

		try
		{
			for (int i = 1; i <= limits.getDepth() && (i < 4 || !timeManager.stopIterativeDeepening())
					&& i < MAX_PLY; i++)
			{
				rootDepth = i;
				selDepth = 0;

				if (i > 3)
				{
					delta = 25;
					alpha = currentScore - delta;
					beta = currentScore + delta;
				}

				while (true)
				{
					int newScore = mainSearch(this.internalBoard, i, alpha, beta, 0, false);

					if (newScore > alpha && newScore < beta)
					{
						currentScore = newScore;
						this.lastCompletePV = pv[0].clone();
						if (!suppressOutput)
						{
							UCI.report(i, selDepth, nodesCount, currentScore, timeManager.timePassed(),
									this.lastCompletePV);
						}
						break;
					}

					else if (newScore <= alpha)
					{
						beta = (alpha + beta) / 2;
						alpha = Math.max(alpha - delta, MIN_EVAL);
					}

					else
					{
						beta = Math.min(beta + delta, MAX_EVAL);
					}

					delta += delta * 3;
				}
			}
		}

		catch (TimeOutException e)
		{
		}

		if (!suppressOutput)
		{
			UCI.reportBestMove(lastCompletePV[0]);
		}
	}

	public Move nextMove(Board board, Limits limits)
	{
		return nextMove(board, limits, false);
	}

	public Move nextMove(Board board, Limits limits, boolean suppressOutput)
	{
		this.nodesCount = 0;
		this.nodesLimit = limits.getNodes();
		this.timeManager = new TimeManager(limits.getTime(), limits.getIncrement(), limits.getMovesToGo(), 100,
				board.getMoveCounter());
		this.ss = new SearchStack(MAX_PLY);
		this.accumulators = new AccumulatorManager(network, board);
		this.nmpMinPly = 0;

		this.internalBoard = board.clone();
		this.limits = limits.clone();

		iterativeDeepening(suppressOutput);

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
		this.nodesLimit = -1;
		this.pv = new Move[MAX_PLY][MAX_PLY];
		this.ss = new SearchStack(MAX_PLY);
		this.counterMoves = new Move[13][65];
		this.history = new FromToHistory();
		this.captureHistory = new CaptureHistory();
		this.rootDepth = 0;
		this.selDepth = 0;
	}
}
