package org.shawn.games.Serendipity.NNUE;

import com.github.bhlangonijr.chesslib.Side;

public interface Inference
{
	int forward(final AccumulatorStack accumulators, Side side, final short[] weights, final short bias);
}
