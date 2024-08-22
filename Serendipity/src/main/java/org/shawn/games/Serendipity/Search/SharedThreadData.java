package org.shawn.games.Serendipity.Search;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import org.shawn.games.Serendipity.NNUE.NNUE;

public class SharedThreadData
{
	final NNUE network;
	final AtomicBoolean stopped;
	final TranspositionTable tt;
	final CyclicBarrier endBarrier;
	final CyclicBarrier startBarrier;

	public SharedThreadData(TranspositionTable tt, CyclicBarrier startBarrier, CyclicBarrier endBarrier, NNUE network,
			AtomicBoolean stopped)
	{
		this.tt = tt;
		this.network = network;
		this.stopped = stopped;
		this.endBarrier = endBarrier;
		this.startBarrier = startBarrier;
	}
}
