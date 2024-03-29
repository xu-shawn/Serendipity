package org.shawn.games.Serendipity;

import org.shawn.games.Serendipity.NNUE.NNUE;
import org.shawn.games.Serendipity.NNUE.NNUE.NNUEAccumulator;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

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
		board = new Board();
		whiteAccumulator = new NNUEAccumulator(network, NNUE.chooseInputBucket(board, Side.WHITE));
		blackAccumulator = new NNUEAccumulator(network, NNUE.chooseInputBucket(board, Side.BLACK));
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
		if (undo)
		{
			if (board.getPiece(move.getFrom()).getPieceType().equals(PieceType.KING))
			{
				fullAccumulatorUpdate(board);
				return;
			}

			whiteAccumulator.addFeature(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), Side.WHITE));
			blackAccumulator.addFeature(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), Side.BLACK));

			if (move.getPromotion().equals(Piece.NONE))
			{
				whiteAccumulator
						.subtractFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), Side.WHITE));
				blackAccumulator
						.subtractFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), Side.BLACK));
			}

			else
			{
				whiteAccumulator.subtractFeature(NNUE.getIndex(move.getTo(), move.getPromotion(), Side.WHITE));
				blackAccumulator.subtractFeature(NNUE.getIndex(move.getTo(), move.getPromotion(), Side.BLACK));
			}

			if (!board.getPiece(move.getTo()).equals(Piece.NONE))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), Side.WHITE));
				blackAccumulator.addFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), Side.BLACK));
			}

			else if (move.getTo().equals(board.getEnPassant())
					&& board.getPiece(move.getFrom()).getPieceType().equals(PieceType.PAWN))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(board.getEnPassantTarget(),
						board.getPiece(board.getEnPassantTarget()), Side.WHITE));
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

			whiteAccumulator.subtractFeature(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), Side.WHITE));
			blackAccumulator.subtractFeature(NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), Side.BLACK));

			if (move.getPromotion().equals(Piece.NONE))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), Side.WHITE));
				blackAccumulator.addFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), Side.BLACK));
			}

			else
			{
				whiteAccumulator.addFeature(NNUE.getIndex(move.getTo(), move.getPromotion(), Side.WHITE));
				blackAccumulator.addFeature(NNUE.getIndex(move.getTo(), move.getPromotion(), Side.BLACK));
			}

			if (!board.getPiece(move.getTo()).equals(Piece.NONE))
			{
				whiteAccumulator.subtractFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), Side.WHITE));
				blackAccumulator.subtractFeature(NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), Side.BLACK));
			}

			else if (move.getTo().equals(board.getEnPassant())
					&& board.getPiece(move.getFrom()).getPieceType().equals(PieceType.PAWN))
			{
				whiteAccumulator.subtractFeature(NNUE.getIndex(board.getEnPassantTarget(),
						board.getPiece(board.getEnPassantTarget()), Side.WHITE));
				blackAccumulator.subtractFeature(NNUE.getIndex(board.getEnPassantTarget(),
						board.getPiece(board.getEnPassantTarget()), Side.BLACK));
			}
		}
	}

	private int evaluate(Board board)
	{
		int v = (Side.WHITE.equals(board.getSideToMove())
				? NNUE.evaluate(network, whiteAccumulator, blackAccumulator, NNUE.chooseOutputBucket(board))
				: NNUE.evaluate(network, blackAccumulator, whiteAccumulator, NNUE.chooseOutputBucket(board))) * 24;
		return v;
	}

	@Test
	public void testAccumulators()
	{
		// Initialize Accumulators
		for (Square sq : Square.values())
		{
			if (!board.getPiece(sq).equals(Piece.NONE))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.WHITE));
				blackAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.BLACK));
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

		whiteAccumulator = new NNUEAccumulator(network, NNUE.chooseInputBucket(board, Side.WHITE));
		blackAccumulator = new NNUEAccumulator(network, NNUE.chooseInputBucket(board, Side.BLACK));

		// Initialize Accumulators
		for (Square sq : Square.values())
		{
			if (!board.getPiece(sq).equals(Piece.NONE))
			{
				whiteAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.WHITE));
				blackAccumulator.addFeature(NNUE.getIndex(sq, board.getPiece(sq), Side.BLACK));
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
