package org.shawn.games.Serendipity;

import java.util.*;

import org.shawn.games.Serendipity.NNUE.NNUE;
import org.shawn.games.Serendipity.NNUE.NNUE.NNUEAccumulator;

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
	private final int ASPIRATION_DELTA = 601;

	private final TranspositionTable tt;

	private int nodesCount;
	private int nodesLimit;
	private long timeLimit;

	private Move[][] pv;
	private Move[] killers;
	private Move[][] counterMoves;
	private int[][] history;

	private int rootDepth;
	private int selDepth;

	private NNUE network;
	private NNUEAccumulator blackAccumulator;
	private NNUEAccumulator whiteAccumulator;

	public AlphaBeta(NNUE network)
	{
		this(8, network);
	}

	public AlphaBeta(int n, NNUE network)
	{
		this.tt = new TranspositionTable(1048576 * n);
		this.nodesCount = 0;
		this.timeLimit = 0;
		this.pv = new Move[MAX_PLY][MAX_PLY];
		this.killers = new Move[MAX_PLY];
		this.counterMoves = new Move[13][65];
		this.history = new int[13][65];
		this.rootDepth = 0;

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
		return (Side.WHITE.equals(board.getSideToMove()) ? NNUE.evaluate(network, whiteAccumulator, blackAccumulator)
				: NNUE.evaluate(network, blackAccumulator, whiteAccumulator)) * 24;
	}

	public boolean isTimeUp()
	{
		return System.nanoTime() > this.timeLimit;
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

	private void updateAccumulators(Board board, Move move, boolean undo)
	{
		if (undo)
		{
			if (board.getContext().isKingSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE), network);
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK), network);

				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE), network);
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK), network);

				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE), network);
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK), network);

				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE), network);
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK), network);

				return;
			}

			if (board.getContext().isQueenSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.QUEEN_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE), network);
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK), network);

				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE), network);
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK), network);

				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE), network);
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK), network);

				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE), network);
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK), network);

				return;
			}

			whiteAccumulator.addFeature(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), Side.WHITE),
					network);
			blackAccumulator.addFeature(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), Side.BLACK),
					network);

			if (move.getPromotion().equals(Piece.NONE))
			{
				whiteAccumulator.subtractFeature(
						NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), Side.WHITE), network);
				blackAccumulator.subtractFeature(
						NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), Side.BLACK), network);
			}

			else
			{
				whiteAccumulator.subtractFeature(NNUE.getIndex(move.getTo(), move.getPromotion(), Side.WHITE), network);
				blackAccumulator.subtractFeature(NNUE.getIndex(move.getTo(), move.getPromotion(), Side.BLACK), network);
			}

			if (!board.getPiece(move.getTo()).equals(Piece.NONE))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), Side.WHITE),
						network);
				blackAccumulator.addFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), Side.BLACK),
						network);
			}

			else if (move.getTo().equals(board.getEnPassant())
					&& board.getPiece(move.getFrom()).getPieceType().equals(PieceType.PAWN))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(board.getEnPassantTarget(),
						board.getPiece(board.getEnPassantTarget()), Side.WHITE), network);
				blackAccumulator.addFeature(NNUE.getIndex(board.getEnPassantTarget(),
						board.getPiece(board.getEnPassantTarget()), Side.BLACK), network);
			}
		}

		else
		{
			if (board.getContext().isKingSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE), network);
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK), network);

				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE), network);
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK), network);

				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE), network);
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK), network);

				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE), network);
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK), network);

				return;
			}

			if (board.getContext().isQueenSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.QUEEN_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE), network);
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK), network);

				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE), network);
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK), network);

				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE), network);
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK), network);

				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE), network);
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK), network);

				return;
			}

			whiteAccumulator.subtractFeature(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), Side.WHITE),
					network);
			blackAccumulator.subtractFeature(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), Side.BLACK),
					network);

			if (move.getPromotion().equals(Piece.NONE))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), Side.WHITE),
						network);
				blackAccumulator.addFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), Side.BLACK),
						network);
			}

			else
			{
				whiteAccumulator.addFeature(NNUE.getIndex(move.getTo(), move.getPromotion(), Side.WHITE), network);
				blackAccumulator.addFeature(NNUE.getIndex(move.getTo(), move.getPromotion(), Side.BLACK), network);
			}

			if (!board.getPiece(move.getTo()).equals(Piece.NONE))
			{
				whiteAccumulator.subtractFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), Side.WHITE),
						network);
				blackAccumulator.subtractFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), Side.BLACK),
						network);
			}

			else if (move.getTo().equals(board.getEnPassant())
					&& board.getPiece(move.getFrom()).getPieceType().equals(PieceType.PAWN))
			{
				whiteAccumulator.subtractFeature(NNUE.getIndex(board.getEnPassantTarget(),
						board.getPiece(board.getEnPassantTarget()), Side.WHITE), network);
				blackAccumulator.subtractFeature(NNUE.getIndex(board.getEnPassantTarget(),
						board.getPiece(board.getEnPassantTarget()), Side.BLACK), network);
			}
		}
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

			futilityBase = standPat + 4986;
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

			if (!inCheck && !SEE.staticExchangeEvaluation(board, move, -20))
			{
				continue;
			}

			updateAccumulators(board, move, false);
			board.doMove(move);

			int score = -quiesce(board, -beta, -alpha, ply + 1);

			board.undoMove();
			updateAccumulators(board, move, true);

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

		if ((nodesCount & 1023) == 0 && (isTimeUp() || (nodesLimit > 0 && nodesCount > nodesLimit)))
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
			staticEval = evaluate(board);
		}

		if (!isPV && !board.isKingAttacked() && depth < 7 && staticEval > beta && staticEval - depth * 1683 > beta)
		{
			return beta;
		}

		if (nullAllowed && beta < MATE_EVAL - 1024 && !board.isKingAttacked()
				&& (board.getBitboard(Piece.make(board.getSideToMove(), PieceType.KING))
						| board.getBitboard(Piece.make(board.getSideToMove(), PieceType.PAWN))) != board
								.getBitboard(board.getSideToMove())
				&& evaluate(board) >= beta && ply > 0 && staticEval >= beta)
		{
//			int r = depth / 3 + 4;

			board.doNullMove();
			int nullEval = -mainSearch(board, depth - 4, -beta, -beta + 1, ply + 1, false);
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

		if (isPV && ttMove == null && rootDepth > 1 && depth > 4)
		{
			depth -= 2;
		}

		for (Move move : legalMoves)
		{
			moveCount++;
			int newdepth = depth - 1;
			boolean isQuiet = Piece.NONE.equals(move.getPromotion()) && Piece.NONE.equals(board.getPiece(move.getTo()));

			if (alpha > -MATE_EVAL + 1024 && depth < 9
					&& !SEE.staticExchangeEvaluation(board, move, isQuiet ? -66 * depth : -21 * depth * depth))
			{
				continue;
			}

			updateAccumulators(board, move, false);
			board.doMove(move);

			boolean inCheck = board.isKingAttacked();

			if (inCheck)
			{
				newdepth++;
			}

			int thisMoveEval = MIN_EVAL;

			if (moveCount > 3 + (ply == 0 ? 1 : 0) && depth > 1)
			{
				int r = (int) (1.58 + Math.log(depth) * Math.log(moveCount) / 2.19);

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
			}

			board.undoMove();
			updateAccumulators(board, move, true);

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
						history[board.getPiece(quietMove.getFrom()).ordinal()][quietMove.getTo().ordinal()] -= depth
								* depth * 38 / 100;
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

	public Move nextMove(Board board, int targetDepth, long msLeft, int nodesLimit)
	{
		return nextMove(board, targetDepth, msLeft, nodesLimit, false);
	}

	public Move nextMove(Board board, int targetDepth, long msLeft, int nodesLimit, boolean supressOutput)
	{
		int currentScore = MIN_EVAL;
		killers = new Move[MAX_PLY];
		counterMoves = new Move[13][65];
		clearPV();
		Move[] lastCompletePV = null;
		this.nodesCount = 0;
		this.nodesLimit = nodesLimit;
		long startTime = System.nanoTime();
		this.timeLimit = System.nanoTime() + msLeft * 1000000L;
		long softTimeLimit = System.nanoTime() + msLeft * 600000L;
		this.history = new int[13][65];
		this.whiteAccumulator = new NNUEAccumulator(network);
		this.blackAccumulator = new NNUEAccumulator(network);

		// Initialize Accumulators
		for (Square sq : Square.values())
		{
			if (!board.getPiece(sq).equals(Piece.NONE))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.WHITE), network);
				blackAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.BLACK), network);
			}
		}

		try
		{
			for (int i = 1; i <= targetDepth && (i < 4 || System.nanoTime() < softTimeLimit); i++)
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
		this.nodesLimit = -1;
		this.pv = new Move[MAX_PLY][MAX_PLY];
		this.killers = new Move[MAX_PLY];
		this.counterMoves = new Move[13][65];
		this.history = new int[13][65];
		this.rootDepth = 0;
		this.selDepth = 0;
	}
}
