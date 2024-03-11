package org.shawn.games.Serendipity;

import org.shawn.games.Serendipity.NNUE.NNUE;
import org.shawn.games.Serendipity.NNUE.NNUE.NNUEAccumulator;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.CastleRight;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class AccumulatorTest
{
	NNUEAccumulator whiteAccumulator;
	NNUEAccumulator blackAccumulator;
	NNUE network;
	Board board;

	public AccumulatorTest() throws IOException
	{
		network = new NNUE("/simple.nnue");
		whiteAccumulator = new NNUEAccumulator(network);
		blackAccumulator = new NNUEAccumulator(network);
		board = new Board();
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

	private int evaluate(Board board)
	{
		return (Side.WHITE.equals(board.getSideToMove()) ? NNUE.evaluate(network, whiteAccumulator, blackAccumulator)
				: NNUE.evaluate(network, blackAccumulator, whiteAccumulator)) * 24;
	}

	@Test
	public void testAccumulators()
	{
		// Initialize Accumulators
		for (Square sq : Square.values())
		{
			if (!board.getPiece(sq).equals(Piece.NONE))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.WHITE), network);
				blackAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.BLACK), network);
			}
		}

		int eval1 = evaluate(board);

		// 1. e4 d5 2. e5 f5 3. exf6 e5 4. fxg7 Bxg7 5. Ne2 Ne7 6. d3 O-O 7. Be3 c6 8.
		// Qd2 Nd7 9. Nbc3 e4 10. O-O-O Kh8 11. g3 Re8 12. Bg2 Rg8 13. Qe1 Re8 14. Qg1
		Move[] testGame = { new Move("e2e4", Side.WHITE), new Move("d7d5", Side.BLACK), new Move("e4e5", Side.WHITE),
				new Move("f7f5", Side.BLACK), new Move("e5f6", Side.WHITE), new Move("e7e5", Side.BLACK),
				new Move("f6g7", Side.WHITE), new Move("f8g7", Side.BLACK), new Move("g1e2", Side.WHITE),
				new Move("g8e7", Side.BLACK), new Move("d2d3", Side.WHITE), new Move("e8g8", Side.BLACK),
				new Move("c1e3", Side.WHITE), new Move("c7c6", Side.BLACK), new Move("d1d2", Side.WHITE),
				new Move("b8d7", Side.BLACK), new Move("b1c3", Side.WHITE), new Move("e5e4", Side.BLACK),
				new Move("e1c1", Side.WHITE), new Move("g8h8", Side.BLACK), new Move("g2g3", Side.WHITE),
				new Move("f8e8", Side.BLACK), new Move("f1g2", Side.WHITE), new Move("e8g8", Side.BLACK),
				new Move("d2e1", Side.WHITE), new Move("g8e8", Side.BLACK), new Move("e1g1", Side.WHITE), };

		for (Move move : testGame)
		{
			updateAccumulators(board, move, false);
			board.doMove(move);
		}

		int eval2 = evaluate(board);

		whiteAccumulator = new NNUEAccumulator(network);
		blackAccumulator = new NNUEAccumulator(network);

		// Initialize Accumulators
		for (Square sq : Square.values())
		{
			if (!board.getPiece(sq).equals(Piece.NONE))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.WHITE), network);
				blackAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.BLACK), network);
			}
		}

		int eval3 = evaluate(board);

		for (int i = testGame.length - 1; i >= 0; i--)
		{
			board.undoMove();
			updateAccumulators(board, testGame[i], true);
		}

		int eval4 = evaluate(board);

		assertEquals(eval1, eval4);
		assertEquals(eval2, eval3);
	}
}
