package org.shawn.games.Serendipity.Search;

import java.util.*;
import java.util.concurrent.BrokenBarrierException;

import org.shawn.games.Serendipity.NNUE.*;
import org.shawn.games.Serendipity.Search.History.History;
import org.shawn.games.Serendipity.Search.Listener.FinalReport;
import org.shawn.games.Serendipity.Search.Listener.ISearchListener;
import org.shawn.games.Serendipity.Search.Listener.SearchReport;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

public class AlphaBeta implements Runnable
{
	public static final int VALUE_NONE = 30002;
	public static final int MAX_EVAL = 32767;
	public static final int MIN_EVAL = -32767;
	public static final int MATE_EVAL = 32700;
	public static final int DRAW_EVAL = 0;
	public static final int MAX_PLY = 256;

	public final int[][] reduction = new int[MAX_PLY + 1][MAX_PLY + 1];

	private Move[] lastCompletePV;
	private int nmpMinPly;

	private AccumulatorStack accumulators;

	private ThreadData threadData;
	private SharedThreadData sharedThreadData;
	private SearchStack ss;
	private TimeManager timeManager;

	private Board internalBoard;

	private Move bestMove;

	public AlphaBeta(SharedThreadData sharedThreadData, ThreadData threadData)
	{
		this.ss = new SearchStack(MAX_PLY);

		this.sharedThreadData = sharedThreadData;
		this.threadData = threadData;

		if (threadData.id == 0)
		{
			this.timeManager = new TimeManager();
		}

		for (int i = 0; i < reduction.length; i++)
		{
			for (int j = 0; j < reduction[0].length; j++)
			{
				reduction[i][j] = (int) (1.60 + Math.log(i) * Math.log(j) / 2.17);
			}
		}
	}

	private void updatePV(Move move, int ply)
	{
		threadData.pv[ply][0] = move;
		System.arraycopy(threadData.pv[ply + 1], 0, threadData.pv[ply], 1, MAX_PLY);
	}

	private void clearPV()
	{
		this.threadData.pv = new Move[MAX_PLY + 1][MAX_PLY + 1];
	}

	private static int stat_bonus(int depth)
	{
		return depth * 300 - 300;
	}

	private static int stat_malus(int depth)
	{
		return -stat_bonus(depth);
	}

	public static boolean isQuiet(Move move, Board board)
	{
		return Piece.NONE.equals(move.getPromotion()) && Piece.NONE.equals(board.getPiece(move.getTo()))
				&& !(PieceType.PAWN.equals(board.getPiece(move.getFrom()).getPieceType())
						&& move.getTo() == board.getEnPassant());
	}

	private boolean shouldStop()
	{
		return (this.threadData.id == 0 && this.threadData.mainThreadData.limits.getNodes() > 0
				&& this.threadData.nodes.get() > threadData.mainThreadData.limits.getNodes())
				|| sharedThreadData.stopped.get() || (this.threadData.id == 0 && this.timeManager.shouldStop());
	}

	public int evaluate(Board board)
	{
		int v = NNUE.evaluate(sharedThreadData.network, accumulators, board.getSideToMove(),
				NNUE.chooseOutputBucket(board));

		final int material = Long
				.bitCount(board.getBitboard(Piece.WHITE_BISHOP) | board.getBitboard(Piece.BLACK_BISHOP)) * 3
				+ Long.bitCount(board.getBitboard(Piece.WHITE_KNIGHT) | board.getBitboard(Piece.BLACK_KNIGHT)) * 3
				+ Long.bitCount(board.getBitboard(Piece.WHITE_ROOK) | board.getBitboard(Piece.BLACK_ROOK)) * 5
				+ Long.bitCount(board.getBitboard(Piece.WHITE_QUEEN) | board.getBitboard(Piece.BLACK_QUEEN)) * 10;

		v = v * (206 + material) / 256;

		return v;
	}

	private void sortMoves(List<Move> moves, Board board, int ply)
	{
		TranspositionTable.Entry currentMoveEntry = sharedThreadData.tt.probe(board.getIncrementalHashKey());

		Move ttMove = currentMoveEntry == null ? null : currentMoveEntry.getMove();

		History[] currentContinuationHistories = new History[] { ss.get(ply - 1).continuationHistory,
				ss.get(ply - 2).continuationHistory, null, ss.get(ply - 4).continuationHistory, null,
				ss.get(ply - 6).continuationHistory };

		MoveSort.sortMoves(moves, ttMove, null, threadData.history, threadData.captureHistory,
				currentContinuationHistories, board);
	}

	private void updateContinuationHistories(int ply, int depth, Board board, Move move, List<Move> quietsSearched)
	{
		History conthist;
		int bonus = stat_bonus(depth);
		int malus = stat_malus(depth);

		for (int i : new int[] { 1, 2, 4, 6 })
		{
			conthist = ss.get(ply - i).continuationHistory;

			conthist.register(board, move, (i == 6) ? bonus / 2 : bonus);

			for (Move quietMove : quietsSearched)
			{
				conthist.register(board, quietMove, (i == 6) ? malus / 2 : malus);
			}
		}
	}

	private int quiesce(Board board, int alpha, int beta, int ply) throws TimeOutException
	{
		this.threadData.nodes.incrementAndGet();

		this.threadData.selDepth = Math.max(this.threadData.selDepth, ply);

		SearchStack.SearchState sse = ss.get(ply);

		int bestScore;

		if (board.isRepetition() || board.getHalfMoveCounter() > 100)
		{
			return DRAW_EVAL;
		}

		if ((this.threadData.nodes.get() & 1023) == 0 && this.shouldStop())
		{
			if (threadData.id == 0)
			{
				sharedThreadData.stopped.set(true);
			}

			throw new TimeOutException();
		}

		boolean isPV = beta - alpha > 1;

		TranspositionTable.Entry currentMoveEntry = sharedThreadData.tt.probe(board.getIncrementalHashKey());

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
					if (eval >= beta)
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
			MoveSort.sortCaptures(moves, board, threadData.captureHistory);
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

			accumulators.push(board, move);
			board.doMove(move);
			sse.move = move;
			sse.continuationHistory = threadData.continuationHistories.get(board, sse.move);

			int score = -quiesce(board, -beta, -alpha, ply + 1);

			board.undoMove();
			accumulators.pop();

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

	private int mainSearch(Board board, int depth, int alpha, int beta, int ply, boolean cutNode)
			throws TimeOutException
	{
		this.threadData.nodes.incrementAndGet();
		this.ss.get(ply + 2).killer = null;
		this.threadData.selDepth = Math.max(this.threadData.selDepth, ply);

		boolean improving, isPV, inCheck, givesCheck, inSingularSearch, ttCapture;
		Move bestMove, ttMove;
		int bestValue;
		int eval;

		SearchStack.SearchState sse = ss.get(ply);

		bestValue = MIN_EVAL;
		bestMove = null;
		sse.moveCount = 0;
		isPV = beta - alpha > 1;
		inCheck = sse.inCheck = board.isKingAttacked();
		inSingularSearch = sse.excludedMove != null;

		if (isPV)
		{
			threadData.pv[ply][0] = null;
		}

		if ((this.threadData.nodes.get() & 1023) == 0 && this.shouldStop())
		{
			if (threadData.id == 0)
			{
				sharedThreadData.stopped.set(true);
			}

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
			this.threadData.nodes.decrementAndGet();
			return quiesce(board, alpha, beta, ply);
		}

		if (depth >= MAX_PLY)
		{
			depth = MAX_PLY - 1;
		}

		TranspositionTable.Entry currentMoveEntry = sharedThreadData.tt.probe(board.getIncrementalHashKey());

		sse.ttHit = currentMoveEntry != null && currentMoveEntry.verifySignature(board.getIncrementalHashKey());

		if (!inSingularSearch && !isPV && sse.ttHit && currentMoveEntry.getDepth() >= depth)
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
					if (eval >= beta)
					{
						return eval;
					}
					break;
				default:
					throw new IllegalArgumentException("Unexpected value: " + currentMoveEntry.getType());
			}
		}

		ttMove = sse.ttHit ? currentMoveEntry.getMove() : null;
		ttCapture = ttMove != null && !isQuiet(ttMove, board);

		if (inCheck)
		{
			eval = sse.staticEval = VALUE_NONE;
		}
		else
		{
			if (sse.ttHit)
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

		if (!inSingularSearch && !isPV && !inCheck && (ttMove == null || ttCapture) && depth < 7 && eval >= beta
				&& eval - depth * 70 >= beta)
		{
			return beta > -MATE_EVAL + 1024 ? beta + (eval - beta) / 3 : eval;
		}

		if (!inSingularSearch && ply > 0 && eval >= beta && beta < MATE_EVAL - 1024 && !inCheck
				&& (ss.get(-1).move == null || !ss.get(-1).move.equals(Constants.emptyMove))
				&& (board.getBitboard(Piece.make(board.getSideToMove(), PieceType.KING))
						| board.getBitboard(Piece.make(board.getSideToMove(), PieceType.PAWN))) != board
								.getBitboard(board.getSideToMove()))
		{
			int r = depth / 3 + 4 + Math.min((eval - beta) / 200, 3);

			board.doNullMove();
			sse.move = Constants.emptyMove;
			sse.continuationHistory = threadData.continuationHistories.get(board, sse.move);
			int nullEval = -mainSearch(board, depth - r, -beta, -beta + 1, ply + 1, !cutNode);
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

				if (v >= beta)
				{
					return nullEval;
				}
			}
		}

		if (!inCheck && depth <= 5 && eval + 256 * depth < alpha)
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

		History[] currentContinuationHistories = new History[] { ss.get(ply - 1).continuationHistory,
				ss.get(ply - 2).continuationHistory, null, ss.get(ply - 4).continuationHistory, null,
				ss.get(ply - 6).continuationHistory };

		MoveSort.sortMoves(legalMoves, ttMove, sse.killer, threadData.history, threadData.captureHistory,
				currentContinuationHistories, board);

		List<Move> quietsSearched = new ArrayList<>();
		List<Move> capturesSearched = new ArrayList<>();

		if (isPV && ttMove == null && threadData.rootDepth > 1 && depth > 5)
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
			boolean isQuiet = isQuiet(move, board);

			int r = reduction[depth][sse.moveCount];
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
				if (!inCheck && !givesCheck && isQuiet && lmrDepth <= 8
						&& sse.staticEval + lmrDepth * 150 + 150 <= alpha)
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
				int singularValue = mainSearch(board, singularDepth, singularBeta - 1, singularBeta, ply, cutNode);
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

				else if (singularValue >= beta)
				{
					return singularValue;
				}

			}

			else if (givesCheck)
			{
				extension = 1;
			}

			newdepth += extension;

			accumulators.push(board, move);
			board.doMove(move);
			sse.move = move;
			sse.continuationHistory = threadData.continuationHistories.get(board, sse.move);

			int thisMoveEval = MIN_EVAL;

			if (sse.moveCount > 2 + (ply == 0 ? 1 : 0) && depth > 2)
			{
				r -= isPV ? 1 : 0;
				r -= givesCheck ? 1 : 0;
				r -= !isQuiet ? 1 : 0;
				r += cutNode ? 1 : 0;

				if (isQuiet)
				{
					final int history = threadData.history.get(board, move) + 4500;
					r -= history / 5000;
				}

				int d = newdepth - r;
				d = Math.min(newdepth, newdepth - r);

				thisMoveEval = -mainSearch(board, d, -(alpha + 1), -alpha, ply + 1, true);

				if (thisMoveEval > alpha)
				{
					thisMoveEval = -mainSearch(board, newdepth, -(alpha + 1), -alpha, ply + 1, !cutNode);
				}
			}

			else if (!isPV || sse.moveCount > 1)
			{
				thisMoveEval = -mainSearch(board, newdepth, -(alpha + 1), -alpha, ply + 1, !cutNode);
			}

			if (isPV && (sse.moveCount == 1 || thisMoveEval > alpha))
			{
				thisMoveEval = -mainSearch(board, newdepth, -beta, -alpha, ply + 1, false);
			}

			board.undoMove();
			accumulators.pop();

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

				if (ply == 0)
					this.bestMove = move;

				if (alpha >= beta)
				{
					if (isQuiet)
					{
						sse.killer = move;

						threadData.history.register(board, move, stat_bonus(depth));

						for (Move quietMove : quietsSearched)
						{
							threadData.history.register(board, quietMove, stat_malus(depth));
						}

						updateContinuationHistories(ply, depth, board, move, quietsSearched);
					}
					else
					{
						threadData.captureHistory.register(board, move, stat_bonus(depth));

						for (Move capture : capturesSearched)
						{
							threadData.captureHistory.register(board, capture, stat_malus(depth));
						}
					}

					break;
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
			return inSingularSearch ? alpha : -MATE_EVAL + ply;
		}

		if (!inSingularSearch)
		{
			if (alpha >= beta)
			{
				sharedThreadData.tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.LOWERBOUND, depth,
						bestValue, bestMove, sse.staticEval);
			}

			else if (alpha == oldAlpha)
			{
				sharedThreadData.tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.UPPERBOUND, depth,
						bestValue, ttMove, sse.staticEval);
			}

			else if (alpha > oldAlpha)
			{
				sharedThreadData.tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.EXACT, depth,
						bestValue, bestMove, sse.staticEval);
			}
		}

		return bestValue;
	}

	public void iterativeDeepening(boolean suppressOutput)
	{
		int currentScore;
		int alpha, beta;
		int delta;

		currentScore = MIN_EVAL;
		lastCompletePV = null;
		alpha = MIN_EVAL;
		beta = MAX_EVAL;
		delta = 25;
		clearPV();

		try
		{
			for (int i = 1; i < MAX_PLY; i++)
			{
				if (this.threadData.id == 0 && (i > this.threadData.mainThreadData.limits.getDepth()
						|| this.timeManager.shouldStopIterativeDeepening()))
				{
					break;
				}

				threadData.rootDepth = i;
				threadData.selDepth = 0;
				this.bestMove = null;

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
						this.lastCompletePV = threadData.pv[0].clone();

						if (!suppressOutput && threadData.id == 0)
						{
							long totalNodes = 0;

							for (AlphaBeta thread : this.threadData.mainThreadData.threads)
							{
								totalNodes += thread.getNodesCount();
							}

							SearchReport report = new SearchReport(i, threadData.selDepth, totalNodes,
									sharedThreadData.tt.hashfull(), currentScore, this.timeManager.timePassed(),
									this.internalBoard, this.lastCompletePV);

							for (ISearchListener listener : threadData.mainThreadData.listeners)
							{
								listener.notify(report);
							}
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

		if (threadData.id == 0)
		{
			sharedThreadData.stopped.set(true);
		}

		if (!suppressOutput && threadData.id == 0)
		{
			FinalReport report = new FinalReport(this.bestMove == null ? lastCompletePV[0] : this.bestMove);

			for (ISearchListener listener : threadData.mainThreadData.listeners)
			{
				listener.notify(report);
			}
		}
	}

	public long getNodesCount()
	{
		return this.threadData.nodes.get();
	}

	public void reset()
	{
		this.ss = new SearchStack(MAX_PLY);

		this.threadData.nodes.set(0);
		this.threadData.rootDepth = 0;
		this.threadData.selDepth = 0;
		this.threadData.history.fill(0);
		this.threadData.captureHistory.fill(0);
		this.threadData.continuationHistories.fill(0);
		this.threadData.pv = new Move[MAX_PLY + 1][MAX_PLY + 1];

		for (int i = 0; i < reduction.length; i++)
		{
			for (int j = 0; j < reduction[0].length; j++)
			{
				reduction[i][j] = (int) (1.60 + Math.log(i) * Math.log(j) / 2.17);
			}
		}
	}

	public void setBoard(Board board)
	{
		this.internalBoard = board.clone();
	}

	public void updateTM(Limits limits)
	{
		this.threadData.mainThreadData.limits = limits.clone();
		this.timeManager.set(limits);
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				this.sharedThreadData.endBarrier.await();
				this.sharedThreadData.startBarrier.await();
			}

			catch (InterruptedException | BrokenBarrierException e)
			{
				break;
			}

			prepareThreadAndDoIterativeDeepening();
		}
	}

	public void prepareThreadAndDoIterativeDeepening()
	{
		this.nmpMinPly = 0;
		this.threadData.nodes.set(0);
		this.ss = new SearchStack(MAX_PLY);
		this.sharedThreadData.stopped.set(false);
		this.accumulators = new AccumulatorStack(sharedThreadData.network);
		this.accumulators.init(this.internalBoard);

		iterativeDeepening(false);
	}
}
