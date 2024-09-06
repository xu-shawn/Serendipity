package org.shawn.games.Serendipity.Search;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.shawn.games.Serendipity.Search.History.CaptureHistory;
import org.shawn.games.Serendipity.Search.History.ContinuationHistories;
import org.shawn.games.Serendipity.Search.History.FromToHistory;
import org.shawn.games.Serendipity.Search.History.History;
import org.shawn.games.Serendipity.Search.Listener.ISearchListener;

import com.github.bhlangonijr.chesslib.move.Move;

public class ThreadData
{
	final History history;
	final History captureHistory;
	final ContinuationHistories continuationHistories;
	final MainThreadData mainThreadData;
	Move[][] pv;

	int rootDepth;
	int selDepth;
	final int id;
	AtomicLong nodes;

	public static class MainThreadData
	{
		final List<ISearchListener> listeners;
		final List<AlphaBeta> threads;
		Limits limits;

		public MainThreadData(Limits limits, List<ISearchListener> listeners, List<AlphaBeta> threads)
		{
			this.limits = limits;
			this.listeners = listeners;
			this.threads = threads;
		}
	}

	public ThreadData(int id, MainThreadData mainThreadData)
	{
		this.id = id;

		this.history = new FromToHistory();
		this.captureHistory = new CaptureHistory();
		this.continuationHistories = new ContinuationHistories();

		this.rootDepth = 0;
		this.selDepth = 0;
		this.pv = new Move[AlphaBeta.MAX_PLY + 1][AlphaBeta.MAX_PLY + 1];
		this.mainThreadData = mainThreadData;
		this.nodes = new AtomicLong(0);
	}

	public ThreadData(int id)
	{
		this(id, null);
	}
}
