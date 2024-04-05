package org.shawn.games.Serendipity.NNUE;

import org.shawn.games.Serendipity.NNUE.NNUE.NNUEAccumulator;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.CastleRight;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

public class AccumulatorManager
{
	private final NNUE.NNUEAccumulator whiteAccumulator;
	private final NNUE.NNUEAccumulator blackAccumulator;

	public AccumulatorManager(NNUE network)
	{
		whiteAccumulator = new NNUE.NNUEAccumulator(network, 0);
		blackAccumulator = new NNUE.NNUEAccumulator(network, 0);
	}

	public AccumulatorManager(NNUE network, Board board)
	{
		whiteAccumulator = new NNUEAccumulator(network, NNUE.chooseInputBucket(board, Side.WHITE));
		blackAccumulator = new NNUEAccumulator(network, NNUE.chooseInputBucket(board, Side.BLACK));

		for (Square sq : Square.values())
		{
			if (!board.getPiece(sq).equals(Piece.NONE))
			{
				whiteAccumulator.add(NNUE.getIndex(sq, board.getPiece(sq), Side.WHITE));
				blackAccumulator.add(NNUE.getIndex(sq, board.getPiece(sq), Side.BLACK));
			}
		}
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
					whiteAccumulator.add(NNUE.getIndex(sq, board.getPiece(sq), Side.WHITE));
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
					blackAccumulator.add(NNUE.getIndex(sq, board.getPiece(sq), Side.BLACK));
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
				whiteAccumulator.add(NNUE.getIndex(sq, board.getPiece(sq), Side.WHITE));
				blackAccumulator.add(NNUE.getIndex(sq, board.getPiece(sq), Side.BLACK));
			}
		}
	}

	public void updateAccumulators(Board board, Move move, boolean undo)
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
			efficientlyUpdate(board, move, Side.WHITE, undo);
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
			efficientlyUpdate(board, move, Side.BLACK, undo);
		}
	}

	private void efficientlyUpdate(Board board, Move move, Side side, boolean undo)
	{

		NNUE.NNUEAccumulator accumulator = Side.WHITE.equals(side) ? whiteAccumulator : blackAccumulator;

		if (undo)
		{
			if (board.getContext().isKingSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				accumulator.addaddsubsub(
						NNUE.getIndex(board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE)
								.getFrom(), Piece.make(board.getSideToMove(), PieceType.KING), side),

						NNUE.getIndex(board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE)
								.getFrom(), Piece.make(board.getSideToMove(), PieceType.ROOK), side),

						NNUE.getIndex(board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE)
								.getTo(), Piece.make(board.getSideToMove(), PieceType.KING), side),

						NNUE.getIndex(board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE)
								.getTo(), Piece.make(board.getSideToMove(), PieceType.ROOK), side));

				return;
			}

			if (board.getContext().isQueenSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.QUEEN_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{

				accumulator.addaddsubsub(
						NNUE.getIndex(board.getContext()
								.getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
								Piece.make(board.getSideToMove(), PieceType.KING), side),

						NNUE.getIndex(board.getContext()
								.getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
								Piece.make(board.getSideToMove(), PieceType.ROOK), side),

						NNUE.getIndex(board.getContext()
								.getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
								Piece.make(board.getSideToMove(), PieceType.KING), side),

						NNUE.getIndex(board.getContext()
								.getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
								Piece.make(board.getSideToMove(), PieceType.ROOK), side));

				return;
			}

			if (move.getPromotion().equals(Piece.NONE))
			{
				if (!board.getPiece(move.getTo()).equals(Piece.NONE))
				{
					accumulator.addaddsub(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), side),
							NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), side),
							NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), side));

					return;
				}

				if (move.getTo().equals(board.getEnPassant())
						&& board.getPiece(move.getFrom()).getPieceType().equals(PieceType.PAWN))
				{
					accumulator.addaddsub(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), side),
							NNUE.getIndex(board.getEnPassantTarget(), board.getPiece(board.getEnPassantTarget()), side),
							NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), side));

					return;
				}

				accumulator.addsub(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), side),
						NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), side));

				return;
			}

			else
			{
				if (!board.getPiece(move.getTo()).equals(Piece.NONE))
				{
					accumulator.addaddsub(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), side),
							NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), side),
							NNUE.getIndex(move.getTo(), move.getPromotion(), side));

					return;
				}

				accumulator.addsub(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), side),
						(NNUE.getIndex(move.getTo(), move.getPromotion(), side)));

				return;
			}
		}

		else
		{
			if (board.getContext().isKingSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{

				accumulator.addaddsubsub(
						NNUE.getIndex(board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE)
								.getTo(), Piece.make(board.getSideToMove(), PieceType.ROOK), side),

						NNUE.getIndex(board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE)
								.getTo(), Piece.make(board.getSideToMove(), PieceType.KING), side),

						NNUE.getIndex(board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE)
								.getFrom(), Piece.make(board.getSideToMove(), PieceType.ROOK), side),

						NNUE.getIndex(board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE)
								.getFrom(), Piece.make(board.getSideToMove(), PieceType.KING), side));

				return;
			}

			if (board.getContext().isQueenSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.QUEEN_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{

				accumulator.addaddsubsub(
						NNUE.getIndex(board.getContext()
								.getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
								Piece.make(board.getSideToMove(), PieceType.ROOK), side),

						NNUE.getIndex(board.getContext()
								.getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
								Piece.make(board.getSideToMove(), PieceType.KING), side),

						NNUE.getIndex(board.getContext()
								.getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
								Piece.make(board.getSideToMove(), PieceType.ROOK), side),

						NNUE.getIndex(board.getContext()
								.getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
								Piece.make(board.getSideToMove(), PieceType.KING), side));

				return;
			}

			if (move.getPromotion().equals(Piece.NONE))
			{
				if (!board.getPiece(move.getTo()).equals(Piece.NONE))
				{
					accumulator.addsubsub(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), side),
							NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), side),
							NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), side));
					return;
				}

				if (move.getTo().equals(board.getEnPassant())
						&& board.getPiece(move.getFrom()).getPieceType().equals(PieceType.PAWN))
				{
					accumulator.addsubsub(

							NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), side),
							NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), side),

							NNUE.getIndex(board.getEnPassantTarget(), board.getPiece(board.getEnPassantTarget()),
									side));

					return;
				}

				accumulator.addsub(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), side),
						NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), side));

				return;
			}

			else
			{
				if (!board.getPiece(move.getTo()).equals(Piece.NONE))
				{
					accumulator.addsubsub(NNUE.getIndex(move.getTo(), move.getPromotion(), side),
							NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), side),
							NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), side));

					return;
				}

				accumulator.addsub(NNUE.getIndex(move.getTo(), move.getPromotion(), side),
						NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), side));

				return;
			}
		}
	}

	public NNUE.NNUEAccumulator getWhiteAccumulator()
	{
		return whiteAccumulator;
	}

	public NNUE.NNUEAccumulator getBlackAccumulator()
	{
		return blackAccumulator;
	}
}
