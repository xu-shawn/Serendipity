package org.shawn.games.Serendipity.NNUE;

import java.lang.reflect.Array;
import java.util.Arrays;

public class AccumulatorStack
{
	NNUE network;

	private static enum Color
	{
		WHITE, BLACK
	}

	private class Accumulator
	{
		short[] values;
		Color color;
		boolean needsRefresh;

		public Accumulator(Accumulator pred)
		{
			this.values = pred.values.clone();
			this.color = pred.color;
			this.needsRefresh = true;
		}

		public Accumulator(NNUE network, Color color)
		{
			this.values = network.L1Biases.clone();
			this.color = color;
			this.needsRefresh = false;
		}
	}

	private class AccumulatorPair
	{
		Accumulator[] accumulators = new Accumulator[] { new Accumulator(network, Color.WHITE),
				new Accumulator(network, Color.BLACK) };
	}
}
