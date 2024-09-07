package org.shawn.games.Serendipity.Search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.shawn.games.Serendipity.NNUE.NNUE;
import org.shawn.games.Serendipity.Search.Listener.ISearchListener;
import org.shawn.games.Serendipity.UCI.UCIListener;

import com.github.bhlangonijr.chesslib.Board;

public class ThreadManager
{
	List<ThreadData> threadData;
	List<AlphaBeta> threads;
	int threadsCount;

	CyclicBarrier startBarrier;
	CyclicBarrier endBarrier;
	ExecutorService pool;

	TranspositionTable tt;
	NNUE network;

	public void reinit(int threadsCount)
	{
		init(threadsCount, tt, network);
	}

	public void reinit(NNUE network)
	{
		init(threadsCount, tt, network);
	}

	public void init(int threadsCount, TranspositionTable tt, NNUE network)
	{
		if (this.threads != null)
		{
			this.shutdownAll();
		}
		
		this.threadsCount = threadsCount;
		this.startBarrier = new CyclicBarrier(this.threadsCount + 1);
		this.endBarrier = new CyclicBarrier(this.threadsCount + 1);
		this.threadData = new ArrayList<>();
		this.threads = new ArrayList<>();

		this.tt = tt;
		this.network = network;

		pool = Executors.newFixedThreadPool(this.threadsCount);

		final AtomicBoolean stopped = new AtomicBoolean(false);
		final ArrayList<ISearchListener> listeners = new ArrayList<>();

		startBarrier = new CyclicBarrier(this.threadsCount + 1);
		endBarrier = new CyclicBarrier(this.threadsCount + 1);

		final SharedThreadData sharedData = new SharedThreadData(tt, startBarrier, endBarrier, network, stopped);
		final ThreadData.MainThreadData mainThreadData = new ThreadData.MainThreadData(null, listeners, threads);

		threadData.add(new ThreadData(0, mainThreadData));

		for (int i = 1; i < threadsCount; i++)
		{
			threadData.add(new ThreadData(i));
		}

		listeners.add(new UCIListener());

		for (int i = 0; i < this.threadsCount; i++)
		{
			threads.add(new AlphaBeta(sharedData, threadData.get(i)));
			pool.execute(threads.get(i));
		}
	}
	
	public void initThreads(Board board, Limits limits)
	{
		for (AlphaBeta thread : threads)
		{
			thread.setBoard(board);
		}

		threads.get(0).updateTM(limits);
	}

	public void nextMove(Board board, Limits limits)
	{
		try
		{
			this.endBarrier.await();
		}
		catch (InterruptedException | BrokenBarrierException e)
		{
			e.printStackTrace();
			return;
		}
		
		initThreads(board, limits);

		try
		{
			this.startBarrier.await();
		}
		catch (InterruptedException | BrokenBarrierException e)
		{
			e.printStackTrace();
		}
	}

	public void clearData()
	{
		for (AlphaBeta thread : threads)
		{
			thread.reset();
		}
	}

	public long getNodes()
	{
		long nodes = 0;

		for (AlphaBeta thread : threads)
		{
			nodes += thread.getNodesCount();
		}

		return nodes;
	}
	
	public AlphaBeta getMainThread()
	{
		return threads.get(0);
	}
	
	public void shutdownAll()
	{
		pool.shutdownNow();
	}
}
