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

	public static final int MAX_EVAL = 1000000000;
	public static final int MIN_EVAL = -1000000000;
	public static final int MATE_EVAL = 500000000;
	public static final int DRAW_EVAL = 0;

	public static final int MAX_PLY = 256;

	private final int ASPIRATION_DELTA = 601;

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
	private NNUEAccumulator blackAccumulator;
	private NNUEAccumulator whiteAccumulator;

	private static final IntegerOption a1 = new IntegerOption(3, 1, 8, "a2"); // Step: 0.5
	private static final IntegerOption a2 = new IntegerOption(4, -2, 6, "a2"); // Step: 0.5

	private static final IntegerOption b1 = new IntegerOption(7, 1, 15, "b1"); // Step: 0.5
	private static final IntegerOption b2 = new IntegerOption(1683, 0, 3000, "b2"); // Step: 6

	private static final IntegerOption c1 = new IntegerOption(1, -1, 15, "c1"); // Step: 0.5
	private static final IntegerOption c2 = new IntegerOption(4, 0, 15, "c2"); // Step: 0.5
	private static final IntegerOption c3 = new IntegerOption(2, 0, 9, "c3"); // Step: 0.5

	private static final IntegerOption d1 = new IntegerOption(9, 0, 15, "d1"); // Step: 0.5
	private static final IntegerOption d2 = new IntegerOption(66, 0, 300, "d2"); // Step: 5
	private static final IntegerOption d3 = new IntegerOption(21, 0, 150, "d3"); // Step: 5

	private static final IntegerOption e1 = new IntegerOption(3, 1, 8, "e1"); // Step: 0.5
	private static final IntegerOption e2 = new IntegerOption(1, 0, 10, "e2"); // Step: 0.5
	private static final IntegerOption e3 = new IntegerOption(1, -1, 10, "e3"); // Step: 0.5

	private static final IntegerOption f1 = new IntegerOption(158, -300, 300, "f1"); // Step: 5
	private static final IntegerOption f2 = new IntegerOption(219, -800, 800, "f2"); // Step: 5

	private static final IntegerOption g1 = new IntegerOption(1, 1, 3, "g1"); // Step: 0.5

	private static final IntegerOption h1 = new IntegerOption(38, 10, 500, "h1"); // Step: 5

	private static final IntegerOption i1 = new IntegerOption(4986, 0, 20000, "i1"); // Step: 50

	private static final IntegerOption asp = new IntegerOption(601, 12, 2400, "asp"); // Step: 6

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
				? NNUE.evaluate(network, whiteAccumulator, blackAccumulator, NNUE.chooseOutputBucket(board))
				: NNUE.evaluate(network, blackAccumulator, whiteAccumulator, NNUE.chooseOutputBucket(board))) * 24;
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

	private void fullAccumulatorUpdate(Board board, Side side)
	{
		if (side.equals(Side.WHITE))
		{
			whiteAccumulator.reset();
			whiteAccumulator.setBucketIndex(NNUE.chooseInputBucket(board.getKingSquare(Side.WHITE).ordinal()));

			for (Square sq : Square.values())
			{
				if (!board.getPiece(sq).equals(Piece.NONE))
				{
					whiteAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.WHITE));
				}
			}
		}
		else
		{
			blackAccumulator.reset();
			blackAccumulator
					.setBucketIndex(NNUE.chooseInputBucket(board.getKingSquare(Side.BLACK).ordinal() ^ 0b111000));

			for (Square sq : Square.values())
			{
				if (!board.getPiece(sq).equals(Piece.NONE))
				{
					blackAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.BLACK));
				}
			}
		}
	}

	private void fullAccumulatorUpdate(Board board)
	{
		whiteAccumulator.reset();
		blackAccumulator.reset();
		whiteAccumulator.setBucketIndex(NNUE.chooseInputBucket(board.getKingSquare(Side.WHITE).ordinal()));
		blackAccumulator.setBucketIndex(NNUE.chooseInputBucket(board.getKingSquare(Side.BLACK).ordinal() ^ 0b111000));

		for (Square sq : Square.values())
		{
			if (!board.getPiece(sq).equals(Piece.NONE))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.WHITE));
				blackAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.BLACK));
			}
		}
	}

	private void updateAccumulators(Board board, Move move, boolean undo)
	{
		if (board.getSideToMove().equals(Side.WHITE) && board.getPiece(move.getFrom()).equals(Piece.WHITE_KING)
				&& NNUE.chooseInputBucket(move.getFrom().ordinal()) != NNUE.chooseInputBucket(move.getTo().ordinal()))
		{
			if (undo)
			{
				fullAccumulatorUpdate(board, Side.WHITE);
			}
			else
			{
				board.doMove(move);
				fullAccumulatorUpdate(board, Side.WHITE);
				board.undoMove();
			}
		}
		else
		{
			updateWhiteAccumulators(board, move, undo);
		}

		if (board.getSideToMove().equals(Side.BLACK) && board.getPiece(move.getFrom()).equals(Piece.BLACK_KING)
				&& NNUE.chooseInputBucket(move.getFrom().ordinal() ^ 0b111000) != NNUE
						.chooseInputBucket(move.getTo().ordinal() ^ 0b111000))
		{
			if (undo)
			{
				fullAccumulatorUpdate(board, Side.BLACK);
			}
			else
			{
				board.doMove(move);
				fullAccumulatorUpdate(board, Side.BLACK);
				board.undoMove();
			}
		}
		else
		{
			updateBlackAccumulators(board, move, undo);
		}
	}

	private void updateWhiteAccumulators(Board board, Move move, boolean undo)
	{
		if (undo)
		{
			if (board.getContext().isKingSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE));
				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE));
				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE));
				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE));

				return;
			}

			if (board.getContext().isQueenSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.QUEEN_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{

				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE));
				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE));
				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE));
				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE));

				return;
			}

			whiteAccumulator.addFeature(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), Side.WHITE));

			if (move.getPromotion().equals(Piece.NONE))
			{
				whiteAccumulator
						.subtractFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), Side.WHITE));
			}

			else
			{
				whiteAccumulator.subtractFeature(NNUE.getIndex(move.getTo(), move.getPromotion(), Side.WHITE));
			}

			if (!board.getPiece(move.getTo()).equals(Piece.NONE))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), Side.WHITE));
			}

			else if (move.getTo().equals(board.getEnPassant())
					&& board.getPiece(move.getFrom()).getPieceType().equals(PieceType.PAWN))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(board.getEnPassantTarget(),
						board.getPiece(board.getEnPassantTarget()), Side.WHITE));
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
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE));
				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE));
				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE));
				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE));

				return;
			}

			if (board.getContext().isQueenSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.QUEEN_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE));
				whiteAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE));
				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.WHITE));
				whiteAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.WHITE));

				return;
			}

			whiteAccumulator.subtractFeature(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), Side.WHITE));

			if (move.getPromotion().equals(Piece.NONE))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), Side.WHITE));
			}

			else
			{
				whiteAccumulator.addFeature(NNUE.getIndex(move.getTo(), move.getPromotion(), Side.WHITE));
			}

			if (!board.getPiece(move.getTo()).equals(Piece.NONE))
			{
				whiteAccumulator.subtractFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), Side.WHITE));
			}

			else if (move.getTo().equals(board.getEnPassant())
					&& board.getPiece(move.getFrom()).getPieceType().equals(PieceType.PAWN))
			{
				whiteAccumulator.subtractFeature(NNUE.getIndex(board.getEnPassantTarget(),
						board.getPiece(board.getEnPassantTarget()), Side.WHITE));
			}
		}
	}

	private void updateBlackAccumulators(Board board, Move move, boolean undo)
	{
		if (undo)
		{
			if (board.getContext().isKingSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK));
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK));
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK));
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK));

				return;
			}

			if (board.getContext().isQueenSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.QUEEN_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK));
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK));
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK));
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK));

				return;
			}

			blackAccumulator.addFeature(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), Side.BLACK));

			if (move.getPromotion().equals(Piece.NONE))
			{
				blackAccumulator
						.subtractFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), Side.BLACK));
			}

			else
			{
				blackAccumulator.subtractFeature(NNUE.getIndex(move.getTo(), move.getPromotion(), Side.BLACK));
			}

			if (!board.getPiece(move.getTo()).equals(Piece.NONE))
			{
				blackAccumulator.addFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), Side.BLACK));
			}

			else if (move.getTo().equals(board.getEnPassant())
					&& board.getPiece(move.getFrom()).getPieceType().equals(PieceType.PAWN))
			{
				blackAccumulator.addFeature(NNUE.getIndex(board.getEnPassantTarget(),
						board.getPiece(board.getEnPassantTarget()), Side.BLACK));
			}
		}

		else
		{
			if (board.getPiece(move.getFrom()).getPieceType().equals(PieceType.KING))
			{
				board.doMove(move);
				fullAccumulatorUpdate(board);
				board.undoMove();
				return;
			}
			if (board.getContext().isKingSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK));
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK));
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK));
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK));

				return;
			}

			if (board.getContext().isQueenSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.QUEEN_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK));
				blackAccumulator.addFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK));
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.ROOK), Side.BLACK));
				blackAccumulator.subtractFeature(NNUE.getIndex(
						board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
						Piece.make(board.getSideToMove(), PieceType.KING), Side.BLACK));

				return;
			}

			blackAccumulator.subtractFeature(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), Side.BLACK));

			if (move.getPromotion().equals(Piece.NONE))
			{
				blackAccumulator.addFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), Side.BLACK));
			}

			else
			{
				blackAccumulator.addFeature(NNUE.getIndex(move.getTo(), move.getPromotion(), Side.BLACK));
			}

			if (!board.getPiece(move.getTo()).equals(Piece.NONE))
			{
				blackAccumulator.subtractFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), Side.BLACK));
			}

			else if (move.getTo().equals(board.getEnPassant())
					&& board.getPiece(move.getFrom()).getPieceType().equals(PieceType.PAWN))
			{
				blackAccumulator.subtractFeature(NNUE.getIndex(board.getEnPassantTarget(),
						board.getPiece(board.getEnPassantTarget()), Side.BLACK));
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

		boolean isPV = beta - alpha > 1;

		TranspositionTable.Entry currentMoveEntry = tt.probe(board.getIncrementalHashKey());

		if (!isPV && currentMoveEntry != null && currentMoveEntry.getSignature() == board.getIncrementalHashKey())
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
		boolean inCheck = searchStack[ply].inCheck = board.isKingAttacked();
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
		SearchState ss = searchStack[ply];
		this.nodesCount++;
		this.pv[ply][0] = null;
		this.searchStack[ply + 2].killer = null;
		ss.moveCount = 0;
		boolean isPV = beta - alpha > 1;
		boolean inCheck = ss.inCheck = board.isKingAttacked();
		Move bestMove = null;
		int bestValue = MIN_EVAL;
		this.selDepth = Math.max(this.selDepth, ply);

		if ((nodesCount & 1023) == 0 && (timeManager.stop() || (nodesLimit > 0 && nodesCount > nodesLimit)))
		{
			throw new TimeOutException();
		}

		if ((board.isRepetition(2) && ply > 0) || board.isRepetition(3) || board.getHalfMoveCounter() >= 100)
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

		if (!isPV && !inCheck && depth < b1.get() && staticEval > beta && staticEval - depth * b2.get() > beta)
		{
			return beta;
		}

		if (nullAllowed && beta < MATE_EVAL - 1024 && !inCheck
				&& (board.getBitboard(Piece.make(board.getSideToMove(), PieceType.KING))
						| board.getBitboard(Piece.make(board.getSideToMove(), PieceType.PAWN))) != board
								.getBitboard(board.getSideToMove())
				&& staticEval >= beta && ply > 0)
		{
			int r = depth / a1.get() + a2.get();

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

		if (isPV && ttMove == null && rootDepth > c1.get() && depth > c2.get())
		{
			depth -= c3.get();
		}

		for (Move move : legalMoves)
		{
			ss.moveCount++;
			int newdepth = depth - 1;
			boolean isQuiet = Piece.NONE.equals(move.getPromotion()) && Piece.NONE.equals(board.getPiece(move.getTo()));

			if (alpha > -MATE_EVAL + 1024 && depth < d1.get() && !SEE.staticExchangeEvaluation(board, move,
					isQuiet ? -d2.get() * depth : -d3.get() * depth * depth))
			{
				continue;
			}

			updateAccumulators(board, move, false);
			board.doMove(move);

			inCheck = board.isKingAttacked();

			if (inCheck)
			{
				newdepth++;
			}

			int thisMoveEval = MIN_EVAL;

			if (ss.moveCount > e1.get() + (ply == 0 ? e2.get() : 0) && depth > e3.get())
			{
				int r = (int) ((double) f1.get() / 100f
						+ Math.log(depth) * Math.log(ss.moveCount) / ((double) f2.get() / 100f));

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

			else if (!isPV || ss.moveCount > 1)
			{
				thisMoveEval = -mainSearch(board, newdepth, -(alpha + 1), -alpha, ply + 1, true);
			}

			if (isPV && (ss.moveCount == 1 || thisMoveEval > alpha))
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
								* depth * h1.get() / 100;
					}

					if (isQuiet)
					{
						ss.killer = move;

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
		this.history = new int[13][65];
		this.whiteAccumulator = new NNUEAccumulator(network, NNUE.chooseInputBucket(board, Side.WHITE));
		this.blackAccumulator = new NNUEAccumulator(network, NNUE.chooseInputBucket(board, Side.BLACK));
		this.searchStack = newSearchStack();

		// Initialize Accumulators
		for (Square sq : Square.values())
		{
			if (!board.getPiece(sq).equals(Piece.NONE))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.WHITE));
				blackAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.BLACK));
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
					int newScore = mainSearch(board, i, currentScore - asp.get(), currentScore + asp.get(), 0, false);
					if (newScore > currentScore - asp.get() && newScore < currentScore + asp.get())
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
