package org.shawn.games.Serendipity;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.shawn.games.Serendipity.TranspositionTable.NodeType;

import com.github.bhlangonijr.chesslib.*;

public class TranspositionTableTest
{
	@Test
	public void testTT()
	{
		Board board = new Board();
		TranspositionTable tt = new TranspositionTable(128);
		tt.write(board.getIncrementalHashKey(), NodeType.EXACT, 12, 2, null, 0);
		assertTrue(tt.probe(board.getIncrementalHashKey()).getDepth() == 12);
		assertTrue(tt.probe(board.getIncrementalHashKey()).getEvaluation() == 2);
		assertTrue(tt.probe(board.getIncrementalHashKey()).getSignature() == board.getIncrementalHashKey());
	}
}
