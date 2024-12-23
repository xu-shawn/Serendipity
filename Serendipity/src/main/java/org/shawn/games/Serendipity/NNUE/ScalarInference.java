package org.shawn.games.Serendipity.NNUE;

import com.github.bhlangonijr.chesslib.Side;

public class ScalarInference implements Inference
{
	private final static int[] screlu = new int[Short.MAX_VALUE - Short.MIN_VALUE + 1];

	static
	{
		for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++)
		{
			screlu[i - (int) Short.MIN_VALUE] = screlu((short) (i));
		}
	}

	private static int screlu(short i)
	{
		int v = Math.max(0, Math.min(i, NNUE.QA));
		return v;
	}

	@Override
	public int forward(AccumulatorStack accumulators, Side side, short[] weights, short bias)
	{
		int eval = 0;

		AccumulatorStack.Accumulator us = accumulators.getAccumulator(side);
		AccumulatorStack.Accumulator them = accumulators.getAccumulator(side.flip());

		for (int i = 0; i < NNUE.HIDDEN_SIZE; i++)
		{
			eval += screlu[us.values[i] - (int) Short.MIN_VALUE] * (int) weights[i]
					+ screlu[them.values[i] - (int) Short.MIN_VALUE] * (int) weights[i + NNUE.HIDDEN_SIZE];
		}

		eval += bias;

		eval *= NNUE.SCALE;
		eval /= NNUE.QA * NNUE.QB;

		return eval;
	}
}
