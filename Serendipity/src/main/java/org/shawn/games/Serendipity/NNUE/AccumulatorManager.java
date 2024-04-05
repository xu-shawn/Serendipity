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
				whiteAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.WHITE));
				blackAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.BLACK));
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

	public NNUE.NNUEAccumulator getWhiteAccumulator()
	{
		return whiteAccumulator;
	}

	public NNUE.NNUEAccumulator getBlackAccumulator()
	{
		return blackAccumulator;
	}
}
