package org.shawn.games.Serendipity.Search.Listeners;

import com.github.bhlangonijr.chesslib.move.Move;

public class FinalReport
{
	final Move bestMove;

	public FinalReport(Move bestMove)
	{
		this.bestMove = bestMove;
	}
}
