package org.shawn.games.Serendipity;

import java.util.*;

import org.shawn.games.Serendipity.NNUE.*;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

public class AlphaBeta
{
	private final int PAWN_VALUE = 82;
	private final int KNIGHT_VALUE = 337;
	private final int BISHOP_VALUE = 365;
	private final int ROOK_VALUE = 477;
	private final int QUEEN_VALUE = 1025;

	public static final int MAX_EVAL = 1000000000;
	public static final int MIN_EVAL = -1000000000;
	public static final int MATE_EVAL = 500000000;
	public static final int DRAW_EVAL = 0;

	public static final int MAX_PLY = 256;

	private final int ASPIRATION_DELTA = 603;

	private final TranspositionTable tt;

	private TimeManager timeManager;

	private int nodesCount;
	private int nodesLimit;

	private Move[][] pv;
	private Move[][] counterMoves;
	private int[][] history;

	private int rootDepth;
	private int selDepth;

	private NNUE network;
	private AccumulatorManager accumulators;

	private class SearchState
	{
		public boolean inCheck;
		public boolean ttHit;
		public int moveCount;
		public Move killer;
		public Move excludedMove;
	}

	private SearchState[] searchStack;

	public AlphaBeta(NNUE network)
	{
		this(8, network);
	}

	public AlphaBeta(int n, NNUE network)
	{
		this.tt = new TranspositionTable(1048576 * n);
		this.nodesCount = 0;
		this.nodesLimit = -1;
		this.pv = new Move[MAX_PLY][MAX_PLY];
		this.counterMoves = new Move[13][65];
		this.history = new int[13][65];
		this.rootDepth = 0;
		this.searchStack = newSearchStack();

		this.network = network;
	}

	private SearchState[] newSearchStack()
	{
		SearchState[] newss = new SearchState[MAX_PLY + 10];

		for (int i = 0; i < newss.length; i++)
		{
			newss[i] = new SearchState();
		}

		return newss;
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
		int v = (Side.WHITE.equals(board.getSideToMove())
				? NNUE.evaluate(network, accumulators.getWhiteAccumulator(), accumulators.getBlackAccumulator(),
						NNUE.chooseOutputBucket(board))
				: NNUE.evaluate(network, accumulators.getBlackAccumulator(), accumulators.getWhiteAccumulator(),
						NNUE.chooseOutputBucket(board)))
				* 24;
		return v;
	}

	private void sortMoves(List<Move> moves, Board board, int ply)
	{
		TranspositionTable.Entry currentMoveEntry = tt.probe(board.getIncrementalHashKey());

		Move ttMove = currentMoveEntry == null ? null : currentMoveEntry.getMove();
		MoveSort.sortMoves(moves, ttMove, null, null, history, board);
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

		int bestValue;

		if (board.isRepetition() || board.getHalfMoveCounter() > 100)
		{
			return DRAW_EVAL;
		}

		boolean isPV = beta - alpha > 1;
		boolean inCheck = searchStack[ply].inCheck = board.isKingAttacked();

		TranspositionTable.Entry currentMoveEntry = tt.probe(board.getIncrementalHashKey());
		int ttDepth = inCheck ? 0 : -1;
		Move ttMove = currentMoveEntry == null ? null : currentMoveEntry.getMove();

		if (!isPV && currentMoveEntry != null && currentMoveEntry.getDepth() >= ttDepth
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

		int futilityBase;
		final List<Move> moves;
		Move bestMove;

		if (inCheck)
		{
			bestValue = futilityBase = MIN_EVAL;
			moves = board.legalMoves();
			sortMoves(moves, board, ply);
		}

		else
		{
			int standPat = bestValue = evaluate(board);

			alpha = Math.max(alpha, standPat);

			if (alpha >= beta)
			{
				return alpha;
			}

			futilityBase = standPat + 4932;
			moves = board.pseudoLegalCaptures();
			sortCaptures(moves, board);
		}

		for (Move move : moves)
		{
			if (!inCheck && !board.isMoveLegal(move, false))
			{
				continue;
			}

			if (bestValue > -MATE_EVAL + 1024 && futilityBase < alpha && !SEE.staticExchangeEvaluation(board, move, 1)
					&& (board.getBitboard(Piece.make(board.getSideToMove(), PieceType.KING))
							| board.getBitboard(Piece.make(board.getSideToMove(), PieceType.PAWN))) != board
									.getBitboard(board.getSideToMove()))
			{
				bestValue = Math.max(bestValue, futilityBase);
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

			bestValue = Math.max(bestValue, score);

			if (score > bestValue)
			{
				bestValue = score;

				if (score > alpha)
				{
					alpha = score;
					bestMove = move;

					if (alpha >= beta)
					{
						tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.LOWERBOUND, ttDepth,
								bestValue, bestMove);
					}
				}
			}
		}

		if (bestValue == MIN_EVAL && inCheck)
		{
			return -MATE_EVAL + ply;
		}

		tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.LOWERBOUND, ttDepth,
				bestValue, ttMove);

		return bestValue;
	}

	private int mainSearch(Board board, int depth, int alpha, int beta, int ply, boolean nullAllowed)
			throws TimeOutException
	{
		SearchState ss = searchStack[ply];
		this.nodesCount++;
		this.pv[ply][0] = null;
		this.searchStack[ply + 2].killer = null;
		ss.moveCount = 0;
		boolean isPV = beta - alpha > 1;
		boolean inCheck = ss.inCheck = board.isKingAttacked();
		boolean givesCheck;
		Move bestMove = null;
		int bestValue = MIN_EVAL;
		this.selDepth = Math.max(this.selDepth, ply);

		if ((nodesCount & 1023) == 0 && (timeManager.stop() || (nodesLimit > 0 && nodesCount > nodesLimit)))
		{
			throw new TimeOutException();
		}

		if ((board.isRepetition(2) && ply > 0) || board.isRepetition(3) || board.getHalfMoveCounter() > 100)
		{
			return DRAW_EVAL;
		}

		if (depth <= 0 || ply >= MAX_PLY)
		{
			this.nodesCount--;
			return quiesce(board, alpha, beta, ply + 1);
		}

		TranspositionTable.Entry currentMoveEntry = tt.probe(board.getIncrementalHashKey());

		ss.ttHit = currentMoveEntry != null;

		if (!isPV && currentMoveEntry != null && currentMoveEntry.getDepth() >= depth
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
			staticEval = evaluate(board);
		}

		if (!isPV && !inCheck && depth < 7 && staticEval > beta && staticEval - depth * 1687 > beta)
		{
			return beta;
		}

		if (nullAllowed && beta < MATE_EVAL - 1024 && !inCheck
				&& (board.getBitboard(Piece.make(board.getSideToMove(), PieceType.KING))
						| board.getBitboard(Piece.make(board.getSideToMove(), PieceType.PAWN))) != board
								.getBitboard(board.getSideToMove())
				&& staticEval >= beta && ply > 0)
		{
			int r = depth / 3 + 4;

			board.doNullMove();
			int nullEval = -mainSearch(board, depth - r, -beta, -beta + 1, ply + 1, false);
			board.undoMove();

			if (nullEval >= beta && nullEval < MATE_EVAL - 1024)
			{
				return nullEval;
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

//		Move ttMove = sortMoves(legalMoves, board, ply);

		Move ttMove = currentMoveEntry == null ? null : currentMoveEntry.getMove();

		MoveBackup lastMove = board.getBackup().peekLast();
		Move counterMove = null;

		if (lastMove != null)
			counterMove = counterMoves[board.getPiece(lastMove.getMove().getFrom()).ordinal()][lastMove.getMove()
					.getTo().ordinal()];

		MoveSort.sortMoves(legalMoves, ttMove, ss.killer, counterMove, history, board);

		List<Move> quietMovesFailBeta = new ArrayList<>();

		if (isPV && ttMove == null && rootDepth > 1 && depth > 5)
		{
			depth -= 2;
		}

		for (Move move : legalMoves)
		{
			ss.moveCount++;
			int newdepth = depth - 1;
			board.doMove(move);
			givesCheck = board.isKingAttacked();
			board.undoMove();
			boolean isQuiet = Piece.NONE.equals(move.getPromotion()) && Piece.NONE.equals(board.getPiece(move.getTo()))
					&& !(PieceType.PAWN.equals(board.getPiece(move.getFrom()).getPieceType())
							&& move.getTo() == board.getEnPassant());

			if (isQuiet && !isPV && !givesCheck && depth <= 6 && ss.moveCount > 3 + depth * depth
					&& alpha > -MATE_EVAL + 1024)
			{
				continue;
			}

			if (alpha > -MATE_EVAL + 1024 && depth < 9
					&& !SEE.staticExchangeEvaluation(board, move, isQuiet ? -65 * depth : -38 * depth * depth))
			{
				continue;
			}

			accumulators.updateAccumulators(board, move, false);
			board.doMove(move);

			if (givesCheck)
			{
				newdepth++;
			}

			int thisMoveEval = MIN_EVAL;

			if (ss.moveCount > 3 + (ply == 0 ? 1 : 0) && depth > 2)
			{
				int r = (int) (1.60 + Math.log(depth) * Math.log(ss.moveCount) / 2.17);

//				r += isPV ? 0 : 1;
				r -= givesCheck ? 1 : 0;
//
//				r = Math.max(0, Math.min(depth - 1, r));

				thisMoveEval = -mainSearch(board, depth - r, -(alpha + 1), -alpha, ply + 1, true);

				if (thisMoveEval > alpha)
				{
					thisMoveEval = -mainSearch(board, newdepth, -(alpha + 1), -alpha, ply + 1, true);
				}
			}

			else if (!isPV || ss.moveCount > 1)
			{
				thisMoveEval = -mainSearch(board, newdepth, -(alpha + 1), -alpha, ply + 1, true);
			}

			if (isPV && (ss.moveCount == 1 || thisMoveEval > alpha))
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
							bestMove);

					for (Move quietMove : quietMovesFailBeta)
					{
						history[board.getPiece(quietMove.getFrom()).ordinal()][quietMove.getTo().ordinal()] = Math
								.max(history[board.getPiece(quietMove.getFrom()).ordinal()][quietMove.getTo().ordinal()]
										- depth * depth, -32768);
					}

					if (isQuiet)
					{
						ss.killer = move;

						history[board.getPiece(move.getFrom()).ordinal()][move.getTo().ordinal()] = Math
								.min(history[board.getPiece(move.getFrom()).ordinal()][move.getTo().ordinal()]
										+ depth * depth, 32767);

						if (lastMove != null)
						{
							counterMoves[board.getPiece(lastMove.getMove().getFrom()).ordinal()][lastMove.getMove()
									.getTo().ordinal()] = move;
						}
					}

					return bestValue;
				}
			}

			if (isQuiet)
			{
				quietMovesFailBeta.add(move);
			}
		}

		if (alpha == oldAlpha)
		{
			tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.UPPERBOUND, depth, bestValue, ttMove);
		}

		else if (alpha > oldAlpha)
		{
			tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.EXACT, depth, bestValue, bestMove);
		}

		return bestValue;
	}

	public Move nextMove(Board board, Limits limits)
	{
		return nextMove(board, limits, false);
	}

	public Move nextMove(Board board, Limits limits, boolean supressOutput)
	{
		int currentScore = MIN_EVAL;
		counterMoves = new Move[13][65];
		clearPV();
		Move[] lastCompletePV = null;
		this.nodesCount = 0;
		this.nodesLimit = limits.getNodes();
		this.timeManager = new TimeManager(limits.getTime(), limits.getIncrement(), limits.getMovesToGo(), 100,
				board.getMoveCounter());
		this.searchStack = newSearchStack();
		this.accumulators = new AccumulatorManager(network, board);

		for (int i = 0; i < 13; i++)
		{
			for (int j = 0; j < 65; j++)
			{
				history[i][j] /= 5;
			}
		}

		try
		{
			for (int i = 1; i <= limits.getDepth() && (i < 4 || !timeManager.stopIterativeDeepening()); i++)
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
							UCI.report(i, selDepth, nodesCount, currentScore, timeManager.timePassed(), lastCompletePV);
						}
						continue;
					}
				}

				currentScore = mainSearch(board, i, MIN_EVAL, MAX_EVAL, 0, false);

				lastCompletePV = pv[0].clone();

				if (!supressOutput)
				{
					UCI.report(i, selDepth, nodesCount, currentScore, timeManager.timePassed(), lastCompletePV);
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
		this.nodesLimit = -1;
		this.pv = new Move[MAX_PLY][MAX_PLY];
		this.searchStack = newSearchStack();
		this.counterMoves = new Move[13][65];
		this.history = new int[13][65];
		this.rootDepth = 0;
		this.selDepth = 0;
	}
}
