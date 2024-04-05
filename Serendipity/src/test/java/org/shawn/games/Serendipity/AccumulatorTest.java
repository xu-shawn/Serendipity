package org.shawn.games.Serendipity;

import org.shawn.games.Serendipity.NNUE.*;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class AccumulatorTest
{
	AccumulatorManager accumulators;
	NNUE network;
	Board board;

	public AccumulatorTest() throws IOException
	{
		network = new NNUE("/simple.nnue");
		board = new Board();
		accumulators = new AccumulatorManager(network, board);
	}

	private int evaluate(Board board)
	{
		int v = (Side.WHITE.equals(board.getSideToMove())
				? NNUE.evaluate(network, accumulators.getWhiteAccumulator(), accumulators.getBlackAccumulator(),
						NNUE.chooseOutputBucket(board))
				: NNUE.evaluate(network, accumulators.getBlackAccumulator(), accumulators.getWhiteAccumulator(),
						NNUE.chooseOutputBucket(board)))
				* 24;
		return v;
	}

	@Test
	public void testAccumulators()
	{
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
			accumulators.updateAccumulators(board, move, false);
			board.doMove(move);
		}

		int eval2 = evaluate(board);
		
		accumulators = new AccumulatorManager(network, board);

		int eval3 = evaluate(board);

		for (int i = testGame.length - 1; i >= 0; i--)
		{
			board.undoMove();
			accumulators.updateAccumulators(board, testGame[i], true);
		}

		int eval4 = evaluate(board);

		assertEquals(eval1, eval4);
		assertEquals(eval2, eval3);
	}
}
