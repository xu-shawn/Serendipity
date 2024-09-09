package org.shawn.games.Serendipity;

import static org.junit.Assert.assertTrue;
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
		assertTrue(tt.probe(board.getIncrementalHashKey()).getDepth() == 12);
		assertTrue(tt.probe(board.getIncrementalHashKey()).getEvaluation() == 2);
		assertTrue(tt.probe(board.getIncrementalHashKey()).verifySignature(board.getIncrementalHashKey()));

		tt.write(null, board.getIncrementalHashKey(), TranspositionTable.NODETYPE_NONE, TranspositionTable.DEPTH_NONE,
				-100, new Move(Square.E2, Square.E4), 200);
		assertTrue(tt.probe(board.getIncrementalHashKey()).getNodeType() == TranspositionTable.NODETYPE_NONE);
		assertTrue(tt.probe(board.getIncrementalHashKey()).getDepth() == TranspositionTable.DEPTH_NONE);
		assertTrue(tt.probe(board.getIncrementalHashKey()).getEvaluation() == -100);
		assertTrue(tt.probe(board.getIncrementalHashKey()).getStaticEval() == 200);
		assertTrue(tt.probe(board.getIncrementalHashKey()).getMove().equals(new Move(Square.E2, Square.E4)));
		assertTrue(tt.probe(board.getIncrementalHashKey()).verifySignature(board.getIncrementalHashKey()));
	}
}
