package org.shawn.games.Serendipity;

import java.util.*;

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
	private final int ASPIRATION_DELTA = 600;

	private final TranspositionTable tt;

	private int nodesCount;
	private long timeLimit;

	private Move[][] pv;
	private Move[] killers;
	private Move[][] counterMoves;

	public AlphaBeta()
	{
		this.tt = new TranspositionTable(8388608);
		this.nodesCount = 0;
		this.pv = new Move[MAX_PLY][MAX_PLY];
		this.killers = new Move[MAX_PLY];
		this.counterMoves = new Move[13][65];
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

	private List<Move> sortMoves(List<Move> moves, Board board, int ply)
	{
		List<Move> captures = new ArrayList<Move>();
//		List<Move> promotions = new ArrayList<Move>();
		List<Move> quiets = new ArrayList<Move>();
		List<Move> killers = new ArrayList<Move>();
		List<Move> ttMoves = new ArrayList<Move>();

		MoveBackup lastMove = board.getBackup().peekLast();
		Move counterMove = null;
		boolean hasCounterMove = false;

		if (lastMove != null)
			counterMove = counterMoves[board.getPiece(lastMove.getMove().getTo())
					.ordinal()][lastMove.getMove().getTo().ordinal()];

		for (Move move : moves)
		{
			board.doMove(move);
			TranspositionTable.Entry ttResult = tt.probe(board.getIncrementalHashKey());
			long boardSignature = board.getIncrementalHashKey();
			board.undoMove();

			if (ttResult != null && ttResult.getSignature() == boardSignature
					&& ttResult.getType() == TranspositionTable.NodeType.EXACT)
			{
				ttMoves.add(move);
			}

//			else if (!move.getPromotion().equals(Piece.NONE))
//			{
//				promotions.add(move);
//			}

			else if (move.equals(this.killers[ply]))
			{
				killers.add(move);
			}

			else if (board.getPiece(move.getTo()).equals(Piece.NONE)
					|| board.getPiece(move.getTo()) == null)
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

//		promotions.sort(new Comparator<Move>() {
//
//			@Override
//			public int compare(Move m1, Move m2)
//			{
//				return pieceValue(m2.getPromotion()) - pieceValue(m1.getPromotion());
//			}
//
//		});

		captures.sort(new Comparator<Move>() {

			@Override
			public int compare(Move m1, Move m2)
			{
				return pieceValue(board.getPiece(m2.getTo()))
						- pieceValue(board.getPiece(m2.getFrom()))

						- (pieceValue(board.getPiece(m1.getTo()))
								- pieceValue(board.getPiece(m1.getFrom())));
			}

		});

		moves.clear();

		moves.addAll(ttMoves);
		moves.addAll(captures);

//		if(!promotions.isEmpty())
//		{
//			moves.add(promotions.get(0));
//		}

		moves.addAll(killers);

		if (hasCounterMove)
		{
			moves.add(counterMove);
		}

		moves.addAll(quiets);

//		if(!promotions.isEmpty())
//		{
//			moves.addAll(1, promotions);
//		}

		return moves;
	}

	private List<Move> sortCaptures(List<Move> moves, Board board)
	{
		moves.sort(new Comparator<Move>() {

			@Override
			public int compare(Move m1, Move m2)
			{
				return pieceValue(board.getPiece(m2.getTo()))
						- pieceValue(board.getPiece(m2.getFrom()))

						- (pieceValue(board.getPiece(m1.getTo()))
								- pieceValue(board.getPiece(m1.getFrom())));
			}

		});

		return moves;
	}

	private int quiesce(Board board, int alpha, int beta, int ply) throws TimeOutException
	{
		this.nodesCount++;

		if (board.isDraw())
		{
			return -DRAW_EVAL;
		}

		if (board.isMated())
		{
			return -MATE_EVAL + ply;
		}

		int standPat = evaluate(board);

		alpha = Math.max(alpha, standPat);

		if (alpha >= beta)
		{
			return beta;
		}

		final List<Move> pseudoLegalCaptures = board.pseudoLegalCaptures();

		sortCaptures(pseudoLegalCaptures, board);

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

	private int mainSearch(Board board, int depth, int alpha, int beta, int ply,
			boolean nullAllowed) throws TimeOutException
	{
		this.nodesCount++;
		this.pv[ply][0] = null;
		this.killers[ply + 2] = null;
		int moveCount = 0;
		boolean isPV = beta - alpha > 1;

		if ((nodesCount & 1023) == 0 && isTimeUp())
		{
			throw new TimeOutException();
		}

		if ((board.isRepetition(2) && ply > 0) || board.isRepetition(3) || board.getHalfMoveCounter() >= 100)
		{
			return -DRAW_EVAL;
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
				return -DRAW_EVAL;
			}
		}

		if (depth <= 0 || ply >= MAX_PLY)
		{
			return quiesce(board, alpha, beta, ply + 1);
		}

		TranspositionTable.Entry currentMoveEntry = tt.probe(board.getIncrementalHashKey());

		if ((!isPV || ply > 2) && currentMoveEntry != null && currentMoveEntry.getDepth() >= depth
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
								.getBitboard(board.getSideToMove())
				&& PeSTO.evaluate(board) >= beta && ply > 0)
		{
			board.doNullMove();
			int nullEval = -mainSearch(board, depth - 3, -beta, -beta + 1, ply + 1, false);
			board.undoMove();

			if (nullEval >= beta)
			{
				tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.LOWERBOUND,
						depth, nullEval);
				return nullEval;
			}
		}

		int oldAlpha = alpha;

		sortMoves(legalMoves, board, ply);

		for (Move move : legalMoves)
		{
			moveCount++;
			int newdepth = depth - 1;

			boolean inCheck = board.isKingAttacked();

			board.doMove(move);

			if (board.isKingAttacked())
			{
				newdepth++;
			}

			int thisMoveEval = MIN_EVAL;

			if (moveCount > 3 && depth > 3 && !inCheck && !board.isKingAttacked())
			{
				thisMoveEval = -mainSearch(board, newdepth - 1, -(alpha + 1), -alpha, ply + 1,
						true);

				if (thisMoveEval > alpha)
				{
					thisMoveEval = -mainSearch(board, newdepth, -(alpha + 1), -alpha, ply + 1, true);
				}
			}

			else if(!isPV || moveCount > 1)
			{
				thisMoveEval = -mainSearch(board, newdepth, -(alpha + 1), -alpha, ply + 1, true);
			}
			
			if(isPV && (moveCount == 1 || thisMoveEval > alpha))
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
					tt.write(board.getIncrementalHashKey(), TranspositionTable.NodeType.LOWERBOUND,
							depth, alpha);
					if (move.getPromotion().equals(Piece.NONE)
							&& board.getPiece(move.getTo()).equals(Piece.NONE))
					{
						killers[ply] = move;

						MoveBackup lastMove = board.getBackup().peekLast();
						if (lastMove != null)
						{
							counterMoves[board.getPiece(lastMove.getMove().getTo())
									.ordinal()][lastMove.getMove().getTo().ordinal()] = move;
						}
					}

					return beta;
				}
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
		int currentScore = MIN_EVAL;
		killers = new Move[MAX_PLY];
		counterMoves = new Move[13][65];
		clearPV();
		Move[] lastCompletePV = null;
		this.nodesCount = 0;
		long startTime = System.nanoTime();
		this.timeLimit = System.nanoTime() + msLeft * 1000000L;

		try
		{
			for (int i = 1; i <= targetDepth; i++)
			{
				if (i > 3)
				{
					int newScore = mainSearch(board, i, currentScore - ASPIRATION_DELTA,
							currentScore + ASPIRATION_DELTA, 0, false);
					if (newScore > currentScore - ASPIRATION_DELTA
							&& newScore < currentScore + ASPIRATION_DELTA)
					{
						currentScore = newScore;
						lastCompletePV = pv[0].clone();
						UCI.report(i, nodesCount, currentScore / PeSTO.MAX_PHASE,
								(System.nanoTime() - startTime) / 1000000, lastCompletePV);
						continue;
					}
				}

				currentScore = mainSearch(board, i, MIN_EVAL, MAX_EVAL, 0, false);

				lastCompletePV = pv[0].clone();
				UCI.report(i, nodesCount, currentScore / PeSTO.MAX_PHASE,
						(System.nanoTime() - startTime) / 1000000, lastCompletePV);
			}
		}

		catch (TimeOutException e)
		{
		}

		UCI.reportBestMove(lastCompletePV[0]);

		return lastCompletePV[0];
	}
}
