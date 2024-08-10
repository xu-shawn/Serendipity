package org.shawn.games.Serendipity.Search;

import org.shawn.games.Serendipity.Search.History.CaptureHistory;
import org.shawn.games.Serendipity.Search.History.ContinuationHistories;
import org.shawn.games.Serendipity.Search.History.FromToHistory;
import org.shawn.games.Serendipity.Search.History.History;

import com.github.bhlangonijr.chesslib.move.Move;

public class ThreadData
{
	History history;
	History captureHistory;
	ContinuationHistories continuationHistories;
	Move[][] pv;

	int rootDepth;
	int selDepth;

	public ThreadData()
	{
		this.history = new FromToHistory();
		this.captureHistory = new CaptureHistory();
		this.continuationHistories = new ContinuationHistories();

		this.rootDepth = 0;
		this.pv = new Move[AlphaBeta.MAX_PLY][AlphaBeta.MAX_PLY];
	}
}
