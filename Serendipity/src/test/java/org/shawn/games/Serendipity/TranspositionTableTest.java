package org.shawn.games.Serendipity;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.shawn.games.Serendipity.Search.TranspositionTable;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;

public class TranspositionTableTest
{
	@Test
	public void testTT()
	{
		Board board = new Board();
		TranspositionTable tt = new TranspositionTable(4);

		assertFalse(tt.probe(0).hit());

		tt.write(null, board.getIncrementalHashKey(), TranspositionTable.NODETYPE_EXACT, 12, 2, null, 0);
		assertEquals(12, tt.probe(board.getIncrementalHashKey()).getDepth());
		assertEquals(2, tt.probe(board.getIncrementalHashKey()).getEvaluation());
		assertTrue(tt.probe(board.getIncrementalHashKey()).verifySignature(board.getIncrementalHashKey()));

		tt.write(null, board.getIncrementalHashKey(), TranspositionTable.NODETYPE_NONE, TranspositionTable.DEPTH_QS,
				-6900, new Move(Square.E2, Square.E4), -200);
		assertEquals(TranspositionTable.NODETYPE_NONE, tt.probe(board.getIncrementalHashKey()).getNodeType());
		assertEquals(TranspositionTable.DEPTH_QS, tt.probe(board.getIncrementalHashKey()).getDepth());
		assertEquals(-6900, tt.probe(board.getIncrementalHashKey()).getEvaluation());
		assertEquals(-200, tt.probe(board.getIncrementalHashKey()).getStaticEval());
		assertTrue(tt.probe(board.getIncrementalHashKey()).getMove().equals(new Move(Square.E2, Square.E4)));
		assertTrue(tt.probe(board.getIncrementalHashKey()).verifySignature(board.getIncrementalHashKey()));
	}
}
