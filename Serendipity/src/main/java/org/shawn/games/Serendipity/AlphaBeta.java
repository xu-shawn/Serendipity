package org.shawn.games.Serendipity;

import java.util.*;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

public class AlphaBeta
{
	private final int PAWN_VALUE = 82;
	private final int KNIGHT_VALUE = 337;
	private final int BISHOP_VALUE = 365;
	private final int ROOK_VALUE = 477;
	private final int QUEEN_VALUE = 1025;
	private final int MAX_EVAL = 1000000000;
	private final int MIN_EVAL = -1000000000;
	private final int MATE_EVAL = 500000000;
	private final int DRAW_EVAL = 0;

	private final int MAX_PLY = 256;
	private final int ASPIRATION_DELTA = 600;

	private final TranspositionTable tt;

	private int nodesCount;
	private long timeLimit;

	private Move[][] pv;
	private Move[] killers;
	private Move[][] counterMoves;
	private int[][] history;

	private int rootDepth;
	private int selDepth;

	private IntegerOption a1 = new IntegerOption(3, 2, 6, "a1"); // Step: 1
	private IntegerOption a2 = new IntegerOption(3, -2, 6, "a2"); // Step: 1

	private IntegerOption b1 = new IntegerOption(7, 1, 15, "b1"); // Step: 1
	private IntegerOption b2 = new IntegerOption(1680, 0, 3000, "b2"); // Step: 10

	private IntegerOption c1 = new IntegerOption(2, -1, 15, "c1"); // Step: 1
	private IntegerOption c2 = new IntegerOption(5, 0, 15, "c2"); // Step: 1
	private IntegerOption c3 = new IntegerOption(2, 0, 9, "c3"); // Step: 1
	
	private IntegerOption d1 = new IntegerOption(8, 0, 15, "d1"); // Step: 1
	private IntegerOption d2 = new IntegerOption(64, 0, 300, "d2"); // Step: 5
	private IntegerOption d3 = new IntegerOption(20, 0, 150, "d3"); // Step: 5
	
	private IntegerOption e1 = new IntegerOption(3, 1, 8, "e1"); // Step: 1
	private IntegerOption e2 = new IntegerOption(3, -1, 10, "e2"); // Step: 1

	private IntegerOption f1 = new IntegerOption(135, -300, 300, "f1"); // Step: 15
	private IntegerOption f2 = new IntegerOption(275, -800, 800, "f2"); // Step: 25
	
	private IntegerOption g1 = new IntegerOption(1, 1, 3, "g1"); // Step: 1

	private IntegerOption h1 = new IntegerOption(100, 10, 500, "h1"); // Step: 25
	
	private IntegerOption i1 = new IntegerOption(4896, 0, 20000, "i1"); // Step: 100
	
	private IntegerOption asp = new IntegerOption(600, 12, 2400, "asp"); // Step: 12

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

	private Move sortMoves(List<Move> moves, Board board, int ply)
	{
		List<Move> captures = new ArrayList<Move>();
		List<Move> quiets = new ArrayList<Move>();
		List<Move> killers = new ArrayList<Move>();
		List<Move> ttMoves = new ArrayList<Move>();

		MoveBackup lastMove = board.getBackup().peekLast();
		Move counterMove = null;
		boolean hasCounterMove = false;

		if (lastMove != null)
			counterMove = counterMoves[board.getPiece(lastMove.getMove().getFrom()).ordinal()][lastMove.getMove()
					.getTo().ordinal()];

		for (Move move : moves)
		{
			board.doMove(move);
			TranspositionTable.Entry ttResult = tt.probe(board.getIncrementalHashKey());
			long boardSignature = board.getIncrementalHashKey();
			board.undoMove();

			if (ttResult != null && ttResult.getSignature() == boardSignature
					&& (ttResult.getType() == TranspositionTable.NodeType.EXACT
							|| ttResult.getType() == TranspositionTable.NodeType.UPPERBOUND))
			{
				ttMoves.add(move);
			}

			else if (move.equals(this.killers[ply]))
			{
				killers.add(move);
			}

			else if (board.getPiece(move.getTo()).equals(Piece.NONE) || board.getPiece(move.getTo()) == null)
			{
				if (!hasCounterMove && counterMove != null && move.equals(counterMove))
				{
					hasCounterMove = true;
				}
				else
				{
					quiets.add(move);
				}
			}

			else
			{
				captures.add(move);
			}
		}

		ttMoves.sort(new Comparator<Move>() {

			@Override
			public int compare(Move m1, Move m2)
			{
				int cmp = 0;

				board.doMove(m2);
				cmp += tt.probe(board.getIncrementalHashKey()).getEvaluation();
				board.undoMove();

				board.doMove(m1);
				cmp -= tt.probe(board.getIncrementalHashKey()).getEvaluation();
				board.undoMove();

				return cmp;
			}

		});

		captures.sort(new Comparator<Move>() {

			@Override
			public int compare(Move m1, Move m2)
			{
				return pieceValue(board.getPiece(m2.getTo())) - pieceValue(board.getPiece(m2.getFrom()))

						- (pieceValue(board.getPiece(m1.getTo())) - pieceValue(board.getPiece(m1.getFrom())));
			}

		});

		quiets.sort(new Comparator<Move>() {

			@Override
			public int compare(Move m1, Move m2)
			{
				return history[board.getPiece(m2.getFrom()).ordinal()][m2.getTo().ordinal()]
						- history[board.getPiece(m1.getFrom()).ordinal()][m1.getTo().ordinal()];
			}

		});

		moves.clear();

		moves.addAll(ttMoves);
		moves.addAll(captures);

		moves.addAll(killers);

		if (hasCounterMove)
		{
			moves.add(counterMove);
		}

		moves.addAll(quiets);

		return ttMoves.isEmpty() ? null : ttMoves.get(0);
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

		if (board.isKingAttacked())
		{
			bestScore = futilityBase = MIN_EVAL;
			moves = board.legalMoves();
			sortMoves(moves, board, ply);
			inCheck = true;
		}

		else
		{
			int standPat = bestScore = evaluate(board);

			alpha = Math.max(alpha, standPat);

			if (alpha >= beta)
			{
				return alpha;
			}

			futilityBase = standPat + i1.get();
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
		Move bestMove = null;
		int bestValue = MIN_EVAL;
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

		if (!isPV && !board.isKingAttacked() && depth < b1.get() && staticEval > beta && staticEval - depth * b2.get() > beta)
		{
			return beta;
		}

		if (nullAllowed && beta < MATE_EVAL - 1024 && !board.isKingAttacked()
				&& (board.getBitboard(Piece.make(board.getSideToMove(), PieceType.KING))
						| board.getBitboard(Piece.make(board.getSideToMove(), PieceType.PAWN))) != board
								.getBitboard(board.getSideToMove())
				&& PeSTO.evaluate(board) >= beta && ply > 0 && staticEval >= beta)
		{
			int r = depth / a1.get() + a2.get();

			board.doNullMove();
			int nullEval = -mainSearch(board, Math.min(depth - r, depth - 1), -beta, -beta + 1, ply + 1, false);
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

		Move ttMove = currentMoveEntry == null ? null : currentMoveEntry.getMove();

		MoveBackup lastMove = board.getBackup().peekLast();
		Move counterMove = null;

		if (lastMove != null)
			counterMove = counterMoves[board.getPiece(lastMove.getMove().getFrom()).ordinal()][lastMove.getMove()
					.getTo().ordinal()];

		MoveSort.sortMoves(legalMoves, ttMove, killers[ply], counterMove, history, board);

		List<Move> quietMovesFailBeta = new ArrayList<>();

		if (isPV && ttMove == null && rootDepth > c1.get() && depth > c2.get())
		{
			depth -= c3.get();
		}

		for (Move move : legalMoves)
		{
			moveCount++;
			int newdepth = depth - 1;
			boolean isQuiet = Piece.NONE.equals(move.getPromotion()) && Piece.NONE.equals(board.getPiece(move.getTo()));

			if (alpha > -MATE_EVAL + 1024 && depth < d1.get()
					&& !SEE.staticExchangeEvaluation(board, move, isQuiet ? -d2.get() * depth : -d3.get() * depth * depth))
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

			if (moveCount > e1.get() && depth > e2.get())
			{
				int r = (int) (f1.get() / 100 + Math.log(depth) * Math.log(moveCount) * f2.get() / 100);

//				r += isPV ? 0 : 1;
				r -= inCheck ? g1.get() : 0;
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

			if (thisMoveEval > bestValue)
			{
				bestValue = thisMoveEval;
				bestMove = move;
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
						history[board.getPiece(quietMove.getFrom()).ordinal()][quietMove.getTo().ordinal()] -= h1.get() * depth
								* depth / 100;
					}

					if (isQuiet)
					{
						killers[ply] = move;

						history[board.getPiece(move.getFrom()).ordinal()][move.getTo().ordinal()] += depth * depth;
            
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
			tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.UPPERBOUND, depth, bestValue, bestMove);
		}

		else if (alpha > oldAlpha)
		{
			tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.EXACT, depth, bestValue, bestMove);
		}

		return bestValue;
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
					int newScore = mainSearch(board, i, currentScore - asp.get(),
							currentScore + asp.get(), 0, false);
					if (newScore > currentScore - asp.get() && newScore < currentScore + asp.get())
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
